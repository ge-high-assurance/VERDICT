/* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.

   @author: M. Fareed Arif
*/

package edu.uiowa.clc.verdict.vdm.instrumentor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import verdict.vdm.vdm_data.DataType;
import verdict.vdm.vdm_data.PlainType;
import verdict.vdm.vdm_lustre.BinaryOperation;
import verdict.vdm.vdm_lustre.ConstantDeclaration;
import verdict.vdm.vdm_lustre.ContractItem;
import verdict.vdm.vdm_lustre.ContractSpec;
import verdict.vdm.vdm_lustre.Expression;
import verdict.vdm.vdm_lustre.IfThenElse;
import verdict.vdm.vdm_lustre.LustreProgram;
import verdict.vdm.vdm_lustre.NodeBody;
import verdict.vdm.vdm_lustre.NodeCall;
import verdict.vdm.vdm_lustre.NodeEquation;
import verdict.vdm.vdm_lustre.NodeEquationLHS;
import verdict.vdm.vdm_lustre.SymbolDefinition;
import verdict.vdm.vdm_model.BlockImpl;
import verdict.vdm.vdm_model.CompInstancePort;
import verdict.vdm.vdm_model.ComponentImpl;
import verdict.vdm.vdm_model.ComponentInstance;
import verdict.vdm.vdm_model.ComponentType;
import verdict.vdm.vdm_model.Connection;
import verdict.vdm.vdm_model.ConnectionEnd;
import verdict.vdm.vdm_model.Model;
import verdict.vdm.vdm_model.Port;
import verdict.vdm.vdm_model.PortMode;

public class VDMInstrumentor {

    protected Model vdm_model;

    protected Vector<Port> marked_ports = null;
    protected Vector<ComponentType> marked_types = null;
    protected boolean emptySelection = false;

    public VDMInstrumentor(Model vdm_model) {

        this.vdm_model = vdm_model;

        this.marked_ports = new Vector<Port>();
        this.marked_types = new Vector<ComponentType>();
    }

    // Attacks:
    // IT: Insider Threats
    // OT: Outside User Threats
    // RC: Remote Code Injection
    // * LB: Logic Bomb
    // * LS: Location Spoofing
    // CR: Crash* (Not supproted)
    // SV: virus/malware/worm/trojan
    // * NI: Network Injection modulo Denial-of-Service
    // HT: Hardware Trojan

    // Demo Threat List:
    // (LS)Location Spoofing - Instrument GPS
    // (NI)Network Injection - Instrument GPS & RC_receiver
    // (LB)Logic Bomb - Instrument BatteryHealthCheck, FlightController, GPS,
    // RC_receiver &
    // RC_reciverHealthChecker
    public Model instrument(Model vdm_model, CommandLine cmdLine) {

        Model instrumented_model = null;

        String[] possibleThreats = {"LS", "LB", "NI", "SV", "RI", "OT", "IT", "HT", "BG"};
        List<String> threats =
                Arrays.asList(possibleThreats).stream()
                        .filter(threat -> cmdLine.hasOption(threat))
                        .collect(Collectors.toList());
        boolean blameAssignment = cmdLine.hasOption("B");
        boolean componentLevel = cmdLine.hasOption("C");

        retrieve_component_and_channels(vdm_model, threats, blameAssignment, componentLevel);

        return instrumented_model;
    }

    public Model instrument(
            Model vdm_model,
            List<String> threats,
            boolean blameAssignment,
            boolean componentLevel) {
        Model instrumented_model = null;

        retrieve_component_and_channels(vdm_model, threats, blameAssignment, componentLevel);

        return instrumented_model;
    }

    public Model instrument(Model vdm_model, List<String> threats, boolean blameAssignment) {
        return instrument(vdm_model, threats, blameAssignment, false);
    }

    protected String getComponentID(
            Map<String, HashSet<Connection>> components_map, Connection con) {

        String ComponentID = null;

        for (String cmpID : components_map.keySet()) {
            HashSet<Connection> con_set = components_map.get(cmpID);

            if (con_set.contains(con)) {
                ComponentID = cmpID;
                break;
            }
        }

        return ComponentID;
    }

    protected BlockImpl getBlockID(String componentID) {

        BlockImpl blockImpl = null;

        for (ComponentImpl cmpImpl : vdm_model.getComponentImpl()) {
            if (cmpImpl.getBlockImpl() != null) {
                blockImpl = cmpImpl.getBlockImpl();
                for (ComponentInstance cmpInstance : blockImpl.getSubcomponent()) {
                    ComponentImpl impl = cmpInstance.getImplementation();
                    ComponentType enumType = null;

                    if (impl != null) {
                        enumType = impl.getType();
                    } else {
                        enumType = cmpInstance.getSpecification();
                    }

                    if (componentID.equalsIgnoreCase(enumType.getId())) {
                        return blockImpl;
                    }
                }
            }
        }

        return blockImpl;
    }

    protected boolean isSourceComponent(Connection con) {
        if (con != null) {
            ConnectionEnd src = con.getSource();
            if (src.getSubcomponentPort() == null) {
                return true;
            }
        }
        return false;
    }

    protected boolean isProbePort(Connection con) {

        if (con != null) {

            ConnectionEnd dest_con = con.getDestination();

            Port dstPort = dest_con.getComponentPort();

            if (dstPort == null) {
                CompInstancePort instancePort = dest_con.getSubcomponentPort();
                dstPort = instancePort.getPort();
            }

            if (dstPort.isProbe()) {
                return true;
            }
        }

        return false;
    }

    // Ignore Connection or Marked Ports.
    private boolean ignoreMarkedLink(Connection con) {

        ConnectionEnd srcConnection = con.getSource();
        ComponentType srcType = null;

        ConnectionEnd destConnection = con.getDestination();
        ComponentType destType = null;

        Port srcPort = srcConnection.getComponentPort();

        if (srcPort == null) {
            CompInstancePort compPort = srcConnection.getSubcomponentPort();
            srcPort = compPort.getPort();

            ComponentInstance srcCompInstance = compPort.getSubcomponent();
            srcType = srcCompInstance.getSpecification();
            if (srcType == null) {
                ComponentImpl compImpl = srcCompInstance.getImplementation();
                srcType = compImpl.getType();
            }
        }

        Port destPort = destConnection.getComponentPort();

        if (destPort == null) {
            CompInstancePort compPort = destConnection.getSubcomponentPort();
            destPort = compPort.getPort();

            ComponentInstance destCompInstance = compPort.getSubcomponent();
            destType = destCompInstance.getSpecification();
            if (destType == null) {
                ComponentImpl compImpl = destCompInstance.getImplementation();
                destType = compImpl.getType();
            }
        }

        if (this.marked_ports.contains(srcPort) || this.marked_ports.contains(destPort)) {
            System.out.println("Ignore Port Connection:" + con.getName());
            return true;
        }

        if (this.marked_types.contains(srcType) || this.marked_types.contains(destType)) {
            System.out.println("Ignore Instance Connection:" + con.getName());
            return true;
        }

        return false;
    }

    protected void identifyEmptyOutputComponents() {
        for (ComponentType componentType : this.vdm_model.getComponentType()) {
            Port mPort = markPort(componentType);

            if (mPort != null) {
                //                  System.out.println("Ignoring Node:" + componentType.getName());
                System.out.println("Ignoring Port Instrumentation:" + mPort.getName());

                this.marked_types.add(componentType);
                this.marked_ports.add(mPort);
            }
        }
    }

    // Marked NonOutput Ports.
    protected Port markPort(ComponentType componentType) {

        Port markedPort = null;

        for (Port port : componentType.getPort()) {

            PortMode port_mode = port.getMode();

            if (port_mode == PortMode.OUT) {
                return null;
            } else {
                markedPort = port;
            }
        }

        return markedPort;
    }

    protected void retrieve_component_and_channels(
            Model vdm_model,
            List<String> threats,
            boolean blame_assignment,
            boolean component_level) {

        HashSet<ComponentType> vdm_components = new HashSet<ComponentType>();
        HashSet<Connection> vdm_links = new HashSet<Connection>();

        // Initialize Components with Empty Ports and Ignore
        identifyEmptyOutputComponents();

        // Initialize DataFlow for empty Implementations.
        LustreProgram lt = vdm_model.getDataflowCode();

        if (lt == null) {
            lt = new LustreProgram();
            vdm_model.setDataflowCode(lt);
        }

        if (threats.contains("LS")) {
            System.out.println("Location Spoofing Instrumentation");
            locationSpoofing(vdm_components);
        }
        if (threats.contains("LB")) {
            System.out.println("Logic Bomb Instrumentation");
            logicBomb(vdm_components);
        }
        if (threats.contains("SV")) {
            System.out.println("Software Virus/malware/worm/trojan");
            softwareVirus(vdm_components);
        }
        if (threats.contains("RI")) {
            System.out.println("Remote Code Injection");
            remoteCodeInjection(vdm_components);
        }
        if (threats.contains("OT")) {
            System.out.println("Outsider Threat");
            outsiderThreat(vdm_components);
        }
        if (threats.contains("IT")) {
            System.out.println("Insider Threat");
            insiderThreat(vdm_components);
        }
        if (threats.contains("HT")) {
            System.out.println("Hardware Trojans");
            hardwareTrojan(vdm_components);
        }

        if (threats.contains("NI")) {
            System.out.println("Network Injection Instrumentation");

            // Snooze links for component level blame assignment.
            if (!component_level) {
                networkInjection(vdm_links);
            }
        }

        if (threats.contains("BN")) {
            System.out.println("Benign");
            vdm_components.clear();
            vdm_links.clear();
        }

        //        int component_index = 1;

        // Removed Once component Implemtation assumption.
        ComponentImpl componentImpl = retrieve_main_cmp_impl();

        BlockImpl blockImpl = null;

        if (componentImpl != null) {
            blockImpl = componentImpl.getBlockImpl();
        }

        Map<String, HashSet<Connection>> components_map =
                new HashMap<String, HashSet<Connection>>();

        if (vdm_components.size() > 0) {
            //            System.out.println("Selected Components:");

            for (ComponentType component : vdm_components) {

                blockImpl = retrieve_block(component);

                HashSet<Connection> vdm_cmp_links = instrument_component(component, blockImpl);

                for (Connection link_con : vdm_cmp_links) {
                    // Check if connection contains Empty Component on Port Ends.
                    if (!ignoreMarkedLink(link_con)) {
                        // Check if Port is Probe Port
                        if (!isProbePort(link_con)) {
                            vdm_links.add(link_con);
                        }
                    }
                }
                components_map.put(component.getId(), vdm_cmp_links);
            }
        }

        // Snoorzing probe ports and Empty output components
        if (vdm_links.size() > 0) {

            Iterator<Connection> it = vdm_links.iterator();
            while (it.hasNext()) {
                Connection con = it.next();
                if (isProbePort(con)) {
                    it.remove();
                } else if (ignoreMarkedLink(con)) {
                    it.remove();
                }
            }
        }

        HashSet<String> global_constants = new HashSet<String>();

        Map<Connection, String> connections_map = new HashMap<Connection, String>();

        if (vdm_links.size() > 0) {

            //            System.out.println("Selected Links:");

            for (Connection connection : vdm_links) {
                //                System.out.println("(" + connection_index++ + ") " +
                // connection.getName());
                // instrument_link(connection, blockImpl);

                String cmpID = getComponentID(components_map, connection);

                if (cmpID != null) {
                    // Find Block based on Connection

                    blockImpl = getBlockID(cmpID);

                    String constant = instrument_link(cmpID, connection, blockImpl);

                    global_constants.add(constant);

                    connections_map.put(connection, constant);

                } else {
                    // Handle 'NI' as Special Case.
                    ConnectionEnd conDest = connection.getSource();
                    Port dest_port = conDest.getComponentPort();

                    if (dest_port != null) {

                        cmpID = dest_port.getId();

                    } else {
                        CompInstancePort compInstance = conDest.getSubcomponentPort();

                        ComponentInstance compInst = compInstance.getSubcomponent();
                        cmpID = compInst.getId();
                    }

                    blockImpl = retrieve_block(connection);

                    String constant = instrument_link(cmpID, connection, blockImpl);

                    global_constants.add(constant);

                    connections_map.put(connection, constant);
                }
            }
        } else {
            emptySelection = true;
        }

        // Declare Global Constants
        for (String comp_id : global_constants) {

            ConstantDeclaration global_comp_const = new ConstantDeclaration();

            DataType global_comp_dataType = new DataType();
            global_comp_dataType.setPlainType(PlainType.BOOL);
            global_comp_const.setName(comp_id);
            global_comp_const.setDataType(global_comp_dataType);

            // Expression global_expr = new Expression();

            // global_expr.setBoolLiteral(true);
            // global_comp_const.setDefinition(global_expr);

            vdm_model.getDataflowCode().getConstantDeclaration().add(global_comp_const);
            // g_constants.add(global_comp_const);
        }

        Map<String, List<String>> connection_gps_comp_map =
                connection_gps_mapper(connections_map, components_map);

        // Choosing Blame options
        if (threats.contains("LS") && component_level) {
            // Link Level Instrumentation varibales
            dec_var_asmp_const(connection_gps_comp_map, blame_assignment, false);
        } else if (threats.contains("LS") && !component_level) {
            dec_var_asmp_const(connection_gps_comp_map, blame_assignment, true);
        }

        if (blame_assignment && component_level) {

            Map<String, List<String>> connection_comp_map =
                    connection_mapper(connections_map, components_map);

            ComponentImpl compImpl = retrieve_main_cmp_impl();

            //            if (compImpl.getBlockImpl() == null) {
            //            compImpl = retrieve_block(compImpl);
            //            }

            ContractSpec contractSpec = compImpl.getType().getContract();

            for (String key : components_map.keySet()) {
                Expression wk_expr = new Expression();
                wk_expr.setIdentifier(key);

                Expression not_wkexpr = new Expression();
                not_wkexpr.setNot(wk_expr);

                // Adding weakly assume variables

                ContractItem weakly_assume_item = new ContractItem();

                weakly_assume_item.setName(key + " is not instrumented");
                weakly_assume_item.setExpression(not_wkexpr);
                // Checking connection before adding assumption
                HashSet<Connection> empty_connection_check = components_map.get(key);

                if (empty_connection_check.size() > 0) {
                    contractSpec.getWeaklyassume().add(weakly_assume_item);
                }
            }

            dec_var_const(connection_comp_map);

        } else if (blame_assignment && !component_level) {

            ComponentImpl compImpl = retrieve_main_cmp_impl();

            if (compImpl != null) {
                //                if (compImpl.getBlockImpl() == null) {
                //                    compImpl = retrieve_block_impl(compImpl);
                //                }

                ContractSpec contractSpec = compImpl.getType().getContract();

                for (String key : global_constants) {
                    Expression wk_expr = new Expression();
                    wk_expr.setIdentifier(key);

                    Expression not_wkexpr = new Expression();
                    not_wkexpr.setNot(wk_expr);

                    // Adding weakly assume variables
                    ContractItem weakly_assume_item = new ContractItem();
                    weakly_assume_item.setName(link_name(key) + " is not instrumented");
                    weakly_assume_item.setExpression(not_wkexpr);
                    contractSpec.getWeaklyassume().add(weakly_assume_item);
                }
            }
        }
    }

    protected BlockImpl retrieve_block(Connection input_con) {

        BlockImpl blockImpl = null;

        for (ComponentImpl cmpImpl : vdm_model.getComponentImpl()) {
            if (cmpImpl.getBlockImpl() != null) {
                blockImpl = cmpImpl.getBlockImpl();
                for (Connection block_con : blockImpl.getConnection()) {

                    if (block_con == input_con) {
                        return blockImpl;
                    }
                }
            }
        }

        return blockImpl;
    }

    //    protected ComponentImpl retrieve_cmp_impl(ComponentType componentType) {
    //        ComponentImpl componentImpl = null;
    //        for (ComponentImpl cImpl : vdm_model.getComponentImpl()) {
    //            ComponentType cmpType = cImpl.getType();
    //            if (cmpType.getId().equalsIgnoreCase(componentType.getId())) {
    //                componentImpl = cImpl;
    //            }
    //        }
    //        return componentImpl;
    //    }

    protected ComponentImpl retrieve_cmp_impl(String componentID) {

        ComponentImpl componentImpl = null;
        BlockImpl blockImpl = null;

        for (ComponentImpl cImpl : vdm_model.getComponentImpl()) {

            if (cImpl.getBlockImpl() != null) {

                blockImpl = cImpl.getBlockImpl();

                for (ComponentInstance cmpInstance : blockImpl.getSubcomponent()) {
                    ComponentImpl impl = cmpInstance.getImplementation();
                    ComponentType enumType = null;

                    if (impl != null) {
                        enumType = impl.getType();
                    } else {
                        enumType = cmpInstance.getSpecification();
                    }

                    if (componentID.equals(enumType.getId())) {
                        componentImpl = cImpl;
                        return cImpl;
                    }
                }
            }
        }

        return componentImpl;
    }

    protected ComponentImpl retrieve_cmp_impl(ComponentType componentType) {
        ComponentImpl componentImpl = null;
        BlockImpl blockImpl = null;

        for (ComponentImpl cImpl : vdm_model.getComponentImpl()) {

            if (cImpl.getBlockImpl() != null) {

                blockImpl = cImpl.getBlockImpl();

                for (ComponentInstance cmpInstance : blockImpl.getSubcomponent()) {
                    ComponentImpl impl = cmpInstance.getImplementation();
                    ComponentType enumType = null;

                    if (impl != null) {
                        enumType = impl.getType();
                    } else {
                        enumType = cmpInstance.getSpecification();
                    }

                    if (componentType.getId().equals(enumType.getId())) {
                        componentImpl = cImpl;
                        return cImpl;
                    }
                }
            }
        }
        return componentImpl;
    }

    protected ComponentImpl retrieve_main_cmp_impl() {
        ComponentImpl componentImpl = null;
        for (ComponentImpl cImpl : vdm_model.getComponentImpl()) {
            ComponentType cmpType = cImpl.getType();
            if (cmpType.getContract() != null) {
                componentImpl = cImpl;
            }
        }
        return componentImpl;
    }

    protected ComponentImpl retrieve_block_impl(ComponentImpl compImpl) {

        BlockImpl blockImpl = null;
        String cmpID = compImpl.getType().getId();

        for (ComponentImpl cImpl : vdm_model.getComponentImpl()) {
            if (cImpl.getBlockImpl() != null) {
                blockImpl = cImpl.getBlockImpl();

                for (ComponentInstance cmpInstance : blockImpl.getSubcomponent()) {

                    ComponentImpl impl = cmpInstance.getImplementation();
                    ComponentType enumType = null;

                    if (impl != null) {
                        enumType = impl.getType();
                    } else {
                        enumType = cmpInstance.getSpecification();
                    }

                    if (cmpID.equals(enumType.getId())) {
                        //                        System.out.println(cImpl.getId() + " == " +
                        // compImpl.getId());
                        return cImpl;
                    }
                }
            }
        }
        return compImpl;
    }

    protected BlockImpl retrieve_block(ComponentImpl compImpl) {

        BlockImpl blockImpl = null;

        String cmpID = compImpl.getType().getId();

        for (ComponentImpl cmpImpl : vdm_model.getComponentImpl()) {
            if (cmpImpl.getBlockImpl() != null) {
                blockImpl = cmpImpl.getBlockImpl();
                for (ComponentInstance cmpInstance : blockImpl.getSubcomponent()) {
                    ComponentImpl impl = cmpInstance.getImplementation();
                    ComponentType enumType = null;

                    if (impl != null) {
                        enumType = impl.getType();
                    } else {
                        enumType = cmpInstance.getSpecification();
                    }

                    if (cmpID.equals(enumType.getId())) {
                        return blockImpl;
                    }
                }
            }
        }

        return blockImpl;
    }

    protected BlockImpl retrieve_block(ComponentType compType) {

        BlockImpl blockImpl = null;

        for (ComponentImpl cmpImpl : vdm_model.getComponentImpl()) {
            if (cmpImpl.getBlockImpl() != null) {
                blockImpl = cmpImpl.getBlockImpl();
                for (ComponentInstance cmpInstance : blockImpl.getSubcomponent()) {
                    ComponentImpl impl = cmpInstance.getImplementation();
                    ComponentType enumType = null;

                    if (impl != null) {
                        enumType = impl.getType();
                    } else {
                        enumType = cmpInstance.getSpecification();
                    }

                    if (compType.getId().equals(enumType.getId())) {
                        return blockImpl;
                    }
                }
            }
        }

        return blockImpl;
    }

    protected String link_name(String link) {

        String weak_assumption = "(.+)_instrumented$";

        Pattern fml_pattern = Pattern.compile(weak_assumption);

        Matcher m = fml_pattern.matcher(link);

        if (m.find()) {
            weak_assumption = m.group(1);
        } else {
            weak_assumption = "NO NAME!";
        }

        return weak_assumption;
    }

    protected Map<String, List<String>> connection_gps_mapper(
            Map<Connection, String> connections, Map<String, HashSet<Connection>> comp_asmp) {

        Map<String, List<String>> comp_link = new HashMap<String, List<String>>();

        for (String key : comp_asmp.keySet()) {

            List<String> constants = new ArrayList<String>();
            for (Connection con : comp_asmp.get(key)) {

                String g_constant = connections.get(con);
                if (!isProbePort(con)) {
                    constants.add(g_constant);
                }
                // System.out.println("COMP: " + key + " ==>" + g_constant);
            }

            if (key.equals("GPS") || key.equals("DME_VOR") || key.equals("IRU")) {
                comp_link.put(key, constants);
            }
        }

        return comp_link;
    }

    protected Map<String, List<String>> connection_mapper(
            Map<Connection, String> connections, Map<String, HashSet<Connection>> comp_asmp) {

        Map<String, List<String>> comp_link = new HashMap<String, List<String>>();

        for (String key : comp_asmp.keySet()) {

            List<String> constants = new ArrayList<String>();
            for (Connection con : comp_asmp.get(key)) {

                if (!isProbePort(con)) {
                    String g_constant = connections.get(con);
                    constants.add(g_constant);
                    comp_link.put(key, constants);
                }
            }
        }

        return comp_link;
    }

    /*
     * Declare and Define weak assumptions for the blame assignment.
     */
    protected void dec_var_asmp_const(
            Map<String, List<String>> connection_comp_map,
            boolean blame_assignment,
            boolean link_level) {

        Set<String> vars = connection_comp_map.keySet();
        List<SymbolDefinition> vars_dec = new ArrayList<SymbolDefinition>();
        //        String default_var = null;

        Set<String> var_links = new HashSet<String>();

        for (String var : vars) {
            // Declaration global variables for instrumented links.
            List<String> connections = connection_comp_map.get(var);
            SymbolDefinition var_dec = add_vars_assume(var, connections);

            if (!connections.isEmpty()) {
                vars_dec.add(var_dec);
                var_links.addAll(connections);
            }
        }

        String vars_assumption[] = new String[vars.size()];
        vars_assumption = vars.toArray(vars_assumption);

        Expression assume_expr = null;

        if (link_level) {
            String links[] = new String[var_links.size()];
            links = var_links.toArray(links);
            assume_expr = add_assume_amo(links);
        } else {

            assume_expr = add_assume_amo(vars_assumption);
        }
        // Adding Xor assumption for components.
        ComponentImpl compImpl = retrieve_main_cmp_impl();

        if (compImpl != null) {

            if (compImpl.getBlockImpl() == null) {
                compImpl = retrieve_block_impl(compImpl);
            }

            ContractSpec contractSpec = compImpl.getType().getContract();
            ContractItem assume_item = new ContractItem();

            if (assume_expr != null) {
                assume_item.setExpression(assume_expr);
                contractSpec.getAssume().add(assume_item);
            }

            if (blame_assignment == false && contractSpec != null) {
                contractSpec.getSymbol().addAll(vars_dec);
            }
        }
    }

    protected void dec_var_const(Map<String, List<String>> connection_comp_map) {

        Set<String> vars = connection_comp_map.keySet();
        List<SymbolDefinition> vars_dec = new ArrayList<SymbolDefinition>();

        for (String var : vars) {
            // Declaration global variables for instrumented links.
            List<String> connections = connection_comp_map.get(var);

            SymbolDefinition var_dec = add_vars_assume(var, connections);
            vars_dec.add(var_dec);
        }

        ComponentImpl compImpl = retrieve_main_cmp_impl();

        //        if (compImpl.getBlockImpl() == null) {
        compImpl = retrieve_block_impl(compImpl);
        //        }

        ContractSpec contractSpec = compImpl.getType().getContract();

        contractSpec.getSymbol().addAll(vars_dec);
    }

    protected SymbolDefinition add_vars_assume(String var, List<String> connections) {

        SymbolDefinition var_dec = new SymbolDefinition();

        Expression var_expr = new Expression();

        Stack<Expression> var_stack = new Stack<Expression>();
        for (String con : connections) {
            Expression expr = new Expression();
            expr.setIdentifier(con);
            var_stack.push(expr);
        }

        if (!var_stack.isEmpty()) {
            var_expr = or_expr(var_stack);
        }

        if (var_expr == null) {
            var_expr = new Expression();
            var_expr.setIdentifier(var);
        }

        var_dec.setDefinition(var_expr);
        var_dec.setName(var);

        DataType dataType = new DataType();
        dataType.setPlainType(PlainType.BOOL);
        var_dec.setDataType(dataType);

        return var_dec;
    }

    protected Expression add_assume_amo(String[] global_constants) {

        Stack<Expression> xor_list = new Stack<Expression>();

        int n = global_constants.length;

        for (int index_i = 0; index_i < n - 1; index_i++) {
            for (int index_j = index_i + 1; index_j < n; index_j++) {

                String x_i = global_constants[index_i];
                String x_j = global_constants[index_j];

                Expression xor = xor_expr(x_i, x_j);
                xor_list.push(xor);
                // xor_list.getExpression().add(xor);
            }
        }

        // Adding assumption
        Expression and_expr = null;

        if (xor_list.size() > 1) {
            and_expr = and_expr(xor_list);
        }

        return and_expr;
    }

    protected Expression and_expr(Stack<Expression> expr_stack) {

        while (expr_stack.size() > 1) {

            Expression left_expr = expr_stack.pop();
            Expression right_expr = expr_stack.pop();

            BinaryOperation and_op = new BinaryOperation();
            and_op.setLhsOperand(left_expr);
            and_op.setRhsOperand(right_expr);

            Expression and_expr = new Expression();
            and_expr.setAnd(and_op);

            expr_stack.push(and_expr);
        }

        return expr_stack.pop();
    }

    protected Expression or_expr(Stack<Expression> expr_stack) {

        while (expr_stack.size() > 1) {

            Expression left_expr = expr_stack.pop();
            Expression right_expr = expr_stack.pop();

            BinaryOperation or_op = new BinaryOperation();
            or_op.setLhsOperand(left_expr);
            or_op.setRhsOperand(right_expr);

            Expression or_expr = new Expression();
            or_expr.setOr(or_op);

            expr_stack.push(or_expr);
        }

        return expr_stack.pop();
    }

    protected Expression xor_expr(String i_id, String j_id) {

        Expression amo_expr = new Expression();

        Expression expr_i = new Expression();
        expr_i.setIdentifier(i_id);
        Expression expr_j = new Expression();
        expr_j.setIdentifier(j_id);

        Expression notexpr_i = new Expression();
        notexpr_i.setNot(expr_i);
        Expression notexpr_j = new Expression();
        notexpr_j.setNot(expr_j);

        BinaryOperation or_op = new BinaryOperation();
        or_op.setLhsOperand(notexpr_i);
        or_op.setRhsOperand(notexpr_j);

        amo_expr.setOr(or_op);

        return amo_expr;
    }

    // LS:
    // - Select all components in the model M such that:
    // c.Component-Group = 'GPS' v 'IMU' v 'LIDAR'
    public void locationSpoofing(HashSet<ComponentType> vdm_components) {

        //        BlockImpl blockImpl = null;
        //
        //        for (ComponentImpl componentImpl : vdm_model.getComponentImpl()) {
        //
        //            blockImpl = componentImpl.getBlockImpl();
        //
        //            // BlockImpl
        //            if (blockImpl != null) {
        //
        //                ComponentType componentType = componentImpl.getType();
        //
        //                for (ComponentInstance componentInstance : blockImpl.getSubcomponent()) {
        //
        //                    componentType = componentInstance.getSpecification();
        //                    ComponentImpl subcomponentImpl =
        // componentInstance.getImplementation();
        //
        //                    // Option 1) Specification
        //                    if (componentType != null) {
        //
        //                    }
        //                    // Option 2) Implementation
        //                    else if (subcomponentImpl != null) {
        //
        //                        componentType = subcomponentImpl.getType();
        //                    }
        //
        //                    String component_group = componentInstance.getCategory();
        //
        //                    if (component_group == null) {
        //                        component_group = "";
        //                    }
        //
        //                    if (component_group.equals("GPS")
        //                            || component_group.equals("DME_VOR")
        //                            || component_group.equals("IRU")) {
        //                        vdm_components.add(componentType);
        //                    }
        //                }
        //            }
        //        }
    }

    // NI:
    // - Select all channels ch in the model M such that:
    // ch.ConnectionType = Remote & ch.Connection-Encrypted = False &
    // ch.Connection-Authentication = False
    public void networkInjection(HashSet<Connection> vdm_links) {

        // ArrayList<Connection> selected_channels = new ArrayList<Connection>();

        //        boolean data_encryption = false;
        //        boolean authentication = false;
        //        BlockImpl blockImpl = null;
        //
        //        for (ComponentImpl componentImpl : vdm_model.getComponentImpl()) {
        //            blockImpl = componentImpl.getBlockImpl();
        //            // BlockImpl
        //            if (blockImpl != null) {
        //
        //                // Selection channels (Authentication = OFF & DataEncrypted = OFF)
        //                for (Connection connection : blockImpl.getConnection()) {
        //                    // visit(connection, instrumented_channel);
        //                    ConnectionType con_type = connection.getConnType();
        //                    if (con_type == ConnectionType.REMOTE
        //                            && connection.isEncryptedTransmission() == data_encryption
        //                            && connection.isAuthenticated() == authentication) {
        //
        //                        // selected_channels.add(connection);
        //                        // LOGGER.info("(" + connection_index++ + ") " +
        //                        // connection.getName());
        //                        vdm_links.add(connection);
        //                    }
        //                }
        //            }
        //        }
    }

    // LB:
    // - Select components c in the model M such that:
    // c.ComponentType = 'Software' v c.ComponentType = 'Hybrid' & c.Manufacturer =
    // 'ThirdParty'
    public void logicBomb(HashSet<ComponentType> vdm_components) {

        //        // Conditions
        //        KindOfComponent component_kind_cond_1 = KindOfComponent.SOFTWARE;
        //        KindOfComponent component_kind_cond_2 = KindOfComponent.HYBRID;
        //
        //        //        ManufacturerType manufacturer_cond = ManufacturerType.THIRD_PARTY;
        //
        //        PedigreeType pedigree_cond1 = PedigreeType.COTS;
        //        PedigreeType pedigree_cond2 = PedigreeType.SOURCED;
        //
        //        BlockImpl blockImpl = null;
        //
        //        for (ComponentImpl componentImpl : vdm_model.getComponentImpl()) {
        //
        //            blockImpl = componentImpl.getBlockImpl();
        //
        //            // BlockImpl
        //            if (blockImpl != null) {
        //
        //                ComponentType componentType = componentImpl.getType();
        //
        //                for (ComponentInstance componentInstance : blockImpl.getSubcomponent()) {
        //
        //                    componentType = componentInstance.getSpecification();
        //                    ComponentImpl subcomponentImpl =
        // componentInstance.getImplementation();
        //
        //                    KindOfComponent kind_of_component =
        // componentInstance.getComponentKind();
        //                    //                    ManufacturerType manufacturer =
        //                    // componentInstance.getManufacturer();
        //                    PedigreeType pedgree = componentInstance.getPedigree();
        //                    // Option 1) Specification
        //                    if (componentType != null) {
        //
        //                    }
        //                    // Option 2) Implementation
        //                    else if (subcomponentImpl != null) {
        //
        //                        componentType = subcomponentImpl.getType();
        //                    }
        //
        //                    boolean comp_cond_3 = false;
        //
        //                    if (componentInstance.isAdversariallyTested() != null) {
        //                        comp_cond_3 = componentInstance.isAdversariallyTested();
        //                    }
        //
        //                    if ((kind_of_component == component_kind_cond_1
        //                                    || kind_of_component == component_kind_cond_2)
        //                            && (pedgree == pedigree_cond1 || pedgree == pedigree_cond2)
        //                            //                            manufacturer ==
        // manufacturer_cond
        //                            && !comp_cond_3) {
        //                        // Store component
        //                        // if (!vdm_components.contains(componentType)) {
        //                        vdm_components.add(componentType);
        //                        // }
        //                    }
        //                }
        //            }
        //        }
    }

    // SV:
    // - Select components c in the model M such that:
    // c.ComponentType = 'Software' v c.ComponentType = 'Hybrid' & c.Manufacturer =
    // 'ThirdParty'
    // & \exists ch\in M. p\in InputPort(c). ch = p.channel & ch.Connectin-Type =
    // Remote
    public void softwareVirus(HashSet<ComponentType> vdm_components) {

        //        // Conditions
        //        KindOfComponent component_kind_cond_1 = KindOfComponent.SOFTWARE;
        //        KindOfComponent component_kind_cond_2 = KindOfComponent.HYBRID;
        //        //        ManufacturerType manufacturer_cond = ManufacturerType.THIRD_PARTY;
        //
        //        PedigreeType pedigree_cond1 = PedigreeType.COTS;
        //        PedigreeType pedigree_cond2 = PedigreeType.SOURCED;
        //
        //        BlockImpl blockImpl = null;
        //
        //        for (ComponentImpl componentImpl : vdm_model.getComponentImpl()) {
        //
        //            blockImpl = componentImpl.getBlockImpl();
        //
        //            // BlockImpl
        //            if (blockImpl != null) {
        //
        //                ComponentType componentType = componentImpl.getType();
        //
        //                for (ComponentInstance componentInstance : blockImpl.getSubcomponent()) {
        //
        //                    componentType = componentInstance.getSpecification();
        //                    ComponentImpl subcomponentImpl =
        // componentInstance.getImplementation();
        //
        //                    KindOfComponent kind_of_component =
        // componentInstance.getComponentKind();
        //                    //                    ManufacturerType manufacturer =
        //                    // componentInstance.getManufacturer();
        //                    PedigreeType pedgree = componentInstance.getPedigree();
        //
        //                    // Option 1) Specification
        //                    if (componentType != null) {
        //
        //                    }
        //                    // Option 2) Implementation
        //                    else if (subcomponentImpl != null) {
        //
        //                        componentType = subcomponentImpl.getType();
        //                    }
        //
        //                    if ((kind_of_component == component_kind_cond_1
        //                                    || kind_of_component == component_kind_cond_2)
        //                            && (pedgree == pedigree_cond1 || pedgree == pedigree_cond2)) {
        //
        //                        // Port
        //                        for (Port port : componentType.getPort()) {
        //                            // System.out.print("(" + port_index + ") ");
        //
        //                            PortMode mode = port.getMode();
        //                            if (mode == PortMode.IN) {;
        //                            }
        //                            {
        //                                for (Connection con : blockImpl.getConnection()) {
        //
        //                                    ConnectionType con_type = con.getConnType();
        //
        //                                    if (con_type == ConnectionType.REMOTE) {
        //
        //                                        ConnectionEnd src_con = con.getSource();
        //                                        Port src_port = src_con.getComponentPort();
        //
        //                                        if (src_port == null) {
        //                                            CompInstancePort compPort =
        //                                                    src_con.getSubcomponentPort();
        //                                            src_port = compPort.getPort();
        //                                        }
        //
        //                                        if (port == src_port) {
        //                                            vdm_components.add(componentType);
        //                                        }
        //                                    }
        //                                }
        //                            }
        //                        }
        //                    }
        //                }
        //            }
        //        }
    }

    // Remote Code Injection:
    // - Select components c in the model M such that:
    // c.ComponentType = 'Software' v c.ComponentType = 'Hybrid'
    // & \exists ch\in M. p\in InputPort(c). ch = p.channel & ch.Connectin-Type =
    // Remote
    public void remoteCodeInjection(HashSet<ComponentType> vdm_components) {

        // Conditions
        //        KindOfComponent component_kind_cond_1 = KindOfComponent.SOFTWARE;
        //        KindOfComponent component_kind_cond_2 = KindOfComponent.HYBRID;
        //
        //        BlockImpl blockImpl = null;
        //
        //        for (ComponentImpl componentImpl : vdm_model.getComponentImpl()) {
        //
        //            blockImpl = componentImpl.getBlockImpl();
        //
        //            // BlockImpl
        //            if (blockImpl != null) {
        //
        //                ComponentType componentType = componentImpl.getType();
        //
        //                for (ComponentInstance componentInstance : blockImpl.getSubcomponent()) {
        //
        //                    componentType = componentInstance.getSpecification();
        //                    ComponentImpl subcomponentImpl =
        // componentInstance.getImplementation();
        //
        //                    KindOfComponent kind_of_component =
        // componentInstance.getComponentKind();
        //
        //                    // Option 1) Specification
        //                    if (componentType != null) {
        //
        //                    }
        //                    // Option 2) Implementation
        //                    else if (subcomponentImpl != null) {
        //
        //                        componentType = subcomponentImpl.getType();
        //                    }
        //
        //                    if ((kind_of_component == component_kind_cond_1
        //                            || kind_of_component == component_kind_cond_2)) {
        //
        //                        // Port
        //                        for (Port port : componentType.getPort()) {
        //                            // System.out.print("(" + port_index + ") ");
        //
        //                            PortMode mode = port.getMode();
        //                            if (mode == PortMode.IN) {;
        //                            }
        //                            {
        //                                for (Connection con : blockImpl.getConnection()) {
        //
        //                                    ConnectionType con_type = con.getConnType();
        //
        //                                    if (con_type == ConnectionType.REMOTE) {
        //
        //                                        ConnectionEnd src_con = con.getSource();
        //                                        Port src_port = src_con.getComponentPort();
        //
        //                                        if (src_port == null) {
        //                                            CompInstancePort compPort =
        //                                                    src_con.getSubcomponentPort();
        //                                            src_port = compPort.getPort();
        //                                        }
        //
        //                                        if (port == src_port) {
        //                                            vdm_components.add(componentType);
        //                                        }
        //                                    }
        //                                }
        //                            }
        //                        }
        //                    }
        //                }
        //            }
        //        }
    }

    // HT
    // - Select all components c in the model M that meet condition:
    // ComponentKind = Hardware v Hybrid and manufacturer = ThirdParty
    public void hardwareTrojan(HashSet<ComponentType> vdm_components) {

        //        KindOfComponent component_kind_cond_1 = KindOfComponent.HARDWARE;
        //        KindOfComponent component_kind_cond_2 = KindOfComponent.HYBRID;
        //
        //        //        ManufacturerType manufacturer_cond = ManufacturerType.THIRD_PARTY;
        //
        //        PedigreeType pedigree_cond1 = PedigreeType.COTS;
        //        PedigreeType pedigree_cond2 = PedigreeType.SOURCED;
        //
        //        BlockImpl blockImpl = null;
        //
        //        for (ComponentImpl componentImpl : vdm_model.getComponentImpl()) {
        //
        //            blockImpl = componentImpl.getBlockImpl();
        //
        //            // BlockImpl
        //            if (blockImpl != null) {
        //
        //                ComponentType componentType = componentImpl.getType();
        //
        //                for (ComponentInstance componentInstance : blockImpl.getSubcomponent()) {
        //
        //                    KindOfComponent kind_of_component =
        // componentInstance.getComponentKind();
        //                    //                    ManufacturerType manufacturer =
        //                    // componentInstance.getManufacturer();
        //                    PedigreeType pedgree = componentInstance.getPedigree();
        //
        //                    componentType = getType(componentInstance);
        //
        //                    if ((kind_of_component == component_kind_cond_1
        //                                    || kind_of_component == component_kind_cond_2)
        //                            && (pedgree == pedigree_cond1 || pedgree == pedigree_cond2)) {
        //
        //                        // Store component
        //                        vdm_components.add(componentType);
        //                        // instrument_component(componentType, blockImpl);
        //                    }
        //                }
        //            }
        //        }
    }

    // OT
    // - Select all components c in the model M that meet condition:
    // ComponentKind = Human and c.InsideTrustedBoundary = False
    public void outsiderThreat(HashSet<ComponentType> vdm_components) {

        //        KindOfComponent component_kind_cond_1 = KindOfComponent.HUMAN;
        //        boolean boundary_cond = false;
        //        BlockImpl blockImpl = null;
        //
        //        for (ComponentImpl componentImpl : vdm_model.getComponentImpl()) {
        //
        //            blockImpl = componentImpl.getBlockImpl();
        //
        //            // BlockImpl
        //            if (blockImpl != null) {
        //
        //                ComponentType componentType = componentImpl.getType();
        //
        //                for (ComponentInstance componentInstance : blockImpl.getSubcomponent()) {
        //
        //                    KindOfComponent kind_of_component =
        // componentInstance.getComponentKind();
        //
        //                    componentType = getType(componentInstance);
        //
        //                    if (kind_of_component == component_kind_cond_1
        //                            && componentInstance.isInsideTrustedBoundary() ==
        // boundary_cond) {
        //                        // Store component
        //                        vdm_components.add(componentType);
        //                        // instrument_component(componentType, blockImpl);
        //                    }
        //                }
        //            }
        //        }
    }

    // IT
    // - Select all components c in the model M that meet condition:
    // ComponentKind = Human and c.InsideTrustedBoundary = True
    public void insiderThreat(HashSet<ComponentType> vdm_components) {

        //        KindOfComponent component_kind_cond_1 = KindOfComponent.HUMAN;
        //        boolean boundary_cond = true;
        //        BlockImpl blockImpl = null;
        //
        //        for (ComponentImpl componentImpl : vdm_model.getComponentImpl()) {
        //
        //            blockImpl = componentImpl.getBlockImpl();
        //
        //            // BlockImpl
        //            if (blockImpl != null) {
        //
        //                ComponentType componentType = componentImpl.getType();
        //
        //                for (ComponentInstance componentInstance : blockImpl.getSubcomponent()) {
        //
        //                    KindOfComponent kind_of_component =
        // componentInstance.getComponentKind();
        //
        //                    componentType = getType(componentInstance);
        //
        //                    if (kind_of_component == component_kind_cond_1
        //                            && componentInstance.isInsideTrustedBoundary() ==
        // boundary_cond) {
        //                        // Store component
        //                        vdm_components.add(componentType);
        //                        // instrument_component(componentType, blockImpl);
        //                    }
        //                }
        //            }
        //        }

        // for (ComponentType component : selected_components) {
        // LOGGER.info("(" + component_index++ + ") " + component.getId());
        // instrument_component(component, blockImpl);
        // }
    }

    protected ComponentType getType(ComponentInstance componentInstance) {

        ComponentType componentType = componentInstance.getSpecification();
        ComponentImpl subcomponentImpl = componentInstance.getImplementation();

        // Option 1) Specification
        if (componentType != null) {

        }
        // Option 2) Implementation
        else if (subcomponentImpl != null) {

            componentType = subcomponentImpl.getType();
        }

        return componentType;
    }

    // Instrument Link for all outgoing edges
    public HashSet<Connection> instrument_component(ComponentType component, BlockImpl blockImpl) {

        HashSet<Connection> vdm_links = new HashSet<Connection>();
        //
        //            for (Port port : component.getPort()) {
        //
        //                PortMode mode = port.getMode();
        //
        //                if (mode == PortMode.OUT) {;
        //                }
        //                {
        //                    // Block Implementation Component Selection.
        //                    if (blockImpl != null) {
        //                        for (Connection connection : blockImpl.getConnection()) {
        //                            if (retrieve_links(component, connection, port)) {
        //                                vdm_links.add(connection);
        //                            }
        //                        }
        //                    } else {
        //                        // DataFlow Implementation Selection.
        //                    }
        //                }
        //            }
        //
        return vdm_links;
    }

    protected boolean retrieve_links(
            ComponentType component, Connection connection, Port instrumented_port) {

        // Default Block Implementation
        ComponentImpl compImpl = retrieve_cmp_impl(component);

        // R.H.S
        ConnectionEnd src = connection.getSource();
        ComponentInstance src_componentInstance = new ComponentInstance();

        // Source Connection
        Port src_port = src.getComponentPort();

        if (src_port != null) {
            String identifier = compImpl.getId();
            //            identifier = identifier.replace(".I", "_I");
            identifier = identifier.replace(".", "_dot_");

            src_componentInstance.setId(identifier);
            src_componentInstance.setName(identifier);
            src_componentInstance.setImplementation(compImpl);
        }

        // if (src_port == instrumented_port) {

        CompInstancePort compInstancePort = src.getSubcomponentPort();

        if (compInstancePort != null) {

            src_componentInstance = compInstancePort.getSubcomponent();
            src_port = compInstancePort.getPort();
        }

        if (instrumented_port == src_port) {
            // System.out.println(
            // "Outgoing Channels to Component: "
            // + src_componentInstance.getName()
            // + " -- "
            // + connection.getName()
            // + " --> "
            // + src_port.getName());

            return true;
        }

        return false;
    }

    // public void instrument_link(Connection connection) {
    // // Connection Source
    // ConnectionEnd src = connection.getSource();
    //
    // // Connection Destination
    // ConnectionEnd dest = connection.getDestination();
    //
    // // Source Component
    // Port src_port = src.getComponentPort();
    // // Destination Component
    // Port dest_port = dest.getComponentPort();
    //
    // if (src_port == null && dest_port == null) {
    // // Both are sub-compon
    // System.out.println("Both are subcomponents.");
    // }
    // if (src_port == null && dest_port != null) {
    // // Only one is Subcomponent
    // System.out.println(dest_port.getId() + " -- " + dest_port.getName());
    // }
    // if (src_port != null && dest_port == null) {
    // // One Subcomponent
    // System.out.println(src_port.getId() + " -- " + src_port.getName());
    // }
    // }

    // public void create_link(Connection old_channel, ComponentInstance
    // src_componentInstance,
    // ComponentInstance dest_componentInstance) {
    //
    // ComponentInstance instrumented_componentInstance = new ComponentInstance();
    //
    // String component_ID = src_componentInstance.getName() + "_Inst_" +
    // dest_componentInstance.getName();
    // instrumented_componentInstance.setId(component_ID + "_Instance");
    // instrumented_componentInstance.setName(component_ID);
    //
    // instrumented_componentInstance.setSpecification(value);
    // instrumented_componentInstance.setImplementation(value);
    //
    // ComponentType instrumented_component = new ComponentType();
    // instrumented_component.setId(component_ID);
    // instrumented_component.setName(component_ID);
    //
    //
    //
    // Connection inst_channel = new Connection();
    //
    // //Update Old connection Destination
    // old_channel.setDestination(value);
    //
    // //Add New Connection Source
    // inst_channel.setSource(value);
    // //Add New Connection Destination
    // inst_channel.setDestination(value);
    //
    //
    // }

    public String instrument_link(String compID, Connection connection, BlockImpl blockImpl) {
        // instrument_link(connection);
        //        System.out.println("Instrumented Link ***" + connection.getName());
        // Default Block Implementation
        ComponentImpl compImpl = null;

        if (compID != null) {
            compImpl = retrieve_cmp_impl(compID);
        }

        // Connections without Components Instrumentation.
        if (compImpl == null) {
            compImpl = retrieve_main_cmp_impl();
        }

        ComponentType instrumented_cmp = new ComponentType();

        // R.H.S
        ConnectionEnd src = connection.getSource();
        ComponentInstance src_componentInstance = new ComponentInstance();

        // Source Connection
        Port src_port = src.getComponentPort();

        if (src_port != null) {
            String identifier = compImpl.getId();
            //            identifier = identifier.replace(".I", "_I");
            identifier = identifier.replace(".", "_dot_");

            src_componentInstance.setId(identifier);
            src_componentInstance.setName(identifier);
            src_componentInstance.setImplementation(compImpl);
        }

        // if (src_port == instrumented_port) {

        CompInstancePort compInstancePort = src.getSubcomponentPort();

        if (compInstancePort != null) {

            src_componentInstance = compInstancePort.getSubcomponent();
            src_port = compInstancePort.getPort();
        }

        // R.H.S
        ConnectionEnd dest = connection.getDestination();
        ComponentInstance dest_componentInstance = new ComponentInstance();

        // Source Connection
        Port dest_port = dest.getComponentPort();

        if (dest_port != null) {
            String identifier = compImpl.getId();
            //            identifier = identifier.replace(".I", "_I");
            identifier = identifier.replace(".", "_dot_");

            dest_componentInstance.setId(identifier);
            dest_componentInstance.setName(identifier);
            dest_componentInstance.setImplementation(compImpl);
        }
        // if (dest_port == instrumented_port) {

        compInstancePort = dest.getSubcomponentPort();

        if (compInstancePort != null) {

            dest_componentInstance = compInstancePort.getSubcomponent();
            dest_port = compInstancePort.getPort();
        }

        String instrument_cmp_Id =
                src_componentInstance.getName()
                        + "_Inst_"
                        + dest_componentInstance.getName()
                        + "_port_"
                        + dest_port.getName();

        // Setting Component IDs
        instrumented_cmp.setId(instrument_cmp_Id);
        instrumented_cmp.setName(instrument_cmp_Id);

        // output port
        Port instrumented_port_dest = new Port();

        instrumented_port_dest.setId(dest_port.getId());

        instrumented_port_dest.setName(dest_port.getName());
        instrumented_port_dest.setMode(dest_port.getMode());

        instrumented_port_dest.setType(dest_port.getType());

        if (dest_port.isEvent() != null && dest_port.isEvent()) {
            instrumented_port_dest.setEvent(true);
        } else {
            instrumented_port_dest.setEvent(false);
        }

        instrumented_cmp.getPort().add(instrumented_port_dest);

        // Input port
        Port instrumented_port_src = new Port();

        instrumented_port_src.setId(src_port.getId());

        instrumented_port_src.setName(src_componentInstance + "_port_" + src_port.getName());
        instrumented_port_src.setMode(src_port.getMode());

        if (src_port.isEvent() != null && src_port.isEvent()) {
            instrumented_port_src.setEvent(true);
        } else {
            instrumented_port_src.setEvent(false);
        }

        String global_constant_Id = src_componentInstance.getName();

        if (instrumented_port_src.getMode() == instrumented_port_dest.getMode()) {
            instrumented_port_src.setName(src_port.getName());

            if (instrumented_port_src.getMode() == PortMode.IN) {
                instrumented_port_src.setMode(PortMode.OUT);
            } else {
                instrumented_port_dest.setMode(PortMode.IN);
            }
        } else {
            instrumented_port_src.setName(src_port.getName());
        }

        if (dest_port.getMode() == PortMode.OUT) {
            global_constant_Id += "_port_" + dest_port.getName() + "_instrumented";
        } else {
            global_constant_Id += "_port_" + src_port.getName() + "_instrumented";
        }

        instrumented_port_src.setType(dest_port.getType());

        instrumented_cmp.getPort().add(instrumented_port_src);

        vdm_model.getComponentType().add(instrumented_cmp);

        // Modify connection.

        ConnectionEnd con_end_inst = new ConnectionEnd();

        // instrumentd_port.setPort(value);
        ComponentInstance instrumented_compInstance = new ComponentInstance();
        instrumented_compInstance.setId(connection.getName());
        instrumented_compInstance.setName(connection.getName());

        instrumented_compInstance.setSpecification(instrumented_cmp);

        // -----------------------------------------
        // Adding Auxiliary Node.
        NodeCall nodeCall = new NodeCall();
        nodeCall.setNodeId(instrumented_cmp.getId());
        Expression callExpr = new Expression();
        callExpr.setCall(nodeCall);

        ContractItem true_guarantee_item = new ContractItem();
        // true_guarantee_item.setName("true");
        Expression true_expr = new Expression();
        Boolean true_lit = Boolean.valueOf("true");
        true_expr.setBoolLiteral(true_lit);
        true_guarantee_item.setExpression(true_expr);

        ContractSpec contractSpec = new ContractSpec();
        contractSpec.getGuarantee().add(true_guarantee_item);

        // ---------------------------------------------

        ComponentImpl instrument_compImpl = new ComponentImpl();

        instrument_compImpl.setId(instrumented_cmp.getId() + "_dot_impl");
        instrument_compImpl.setName(instrumented_cmp.getName() + "_dot_Impl");
        instrument_compImpl.setType(instrumented_cmp);

        IfThenElse ifelse = new IfThenElse();

        // Condition
        Expression cond_expr = new Expression();
        cond_expr.setIdentifier(global_constant_Id);
        ifelse.setCondition(cond_expr);
        // Then
        Expression then_arg = new Expression();
        then_arg.setIdentifier(dest_port.getName());
        ifelse.setThenBranch(callExpr);
        // Else
        Expression else_arg = new Expression();

        else_arg.setIdentifier(dest_port.getName());
        nodeCall.getArgument().add(else_arg);
        ifelse.setElseBranch(then_arg);

        Expression instrumented_expr = new Expression();
        instrumented_expr.setConditionalExpression(ifelse);

        NodeEquation n_eq = new NodeEquation();
        NodeEquationLHS neq_lhs = new NodeEquationLHS();
        neq_lhs.getIdentifier().add(src_port.getName() + "_instrumented");
        n_eq.setLhs(neq_lhs);

        n_eq.setRhs(instrumented_expr);

        NodeBody nodeBody = new NodeBody();

        // VariableDeclaration cond_var = new VariableDeclaration();
        // cond_var.setName(gloabal_constant_Id);
        // DataType dataType = new DataType();
        // dataType.setPlainType(PlainType.BOOL);
        // cond_var.setDataType(dataType);
        // nodeBody.getVariableDeclaration().add(cond_var);

        nodeBody.setIsMain(false);
        nodeBody.getEquation().add(n_eq);

        instrument_compImpl.setDataflowImpl(nodeBody);

        instrumented_compInstance.setImplementation(instrument_compImpl);
        vdm_model.getComponentImpl().add(instrument_compImpl);
        vdm_model.getComponentType().add(instrumented_cmp);
        // -----------------------------------------

        CompInstancePort compInstance_inst_port = new CompInstancePort();

        compInstance_inst_port.setPort(dest_port);
        compInstance_inst_port.setSubcomponent(instrumented_compInstance);
        con_end_inst.setSubcomponentPort(compInstance_inst_port);

        blockImpl.getSubcomponent().add(instrumented_compInstance);

        connection.setDestination(con_end_inst);

        Connection new_con = new Connection();
        // Copying connection related artifacts
        new_con.setName(connection.getName() + "_instrumented_channel");
        //        new_con.setConnType(connection.getConnType());
        //        new_con.setFlowType(connection.getFlowType());
        //
        //        new_con.setDataEncrypted(connection.isEncryptedTransmission());
        //        new_con.setAuthenticated(connection.isAuthenticated());

        new_con.setSource(con_end_inst);

        compInstance_inst_port.setPort(src_port);

        new_con.setDestination(dest);

        blockImpl.getConnection().add(new_con);

        return global_constant_Id;
    }
}
