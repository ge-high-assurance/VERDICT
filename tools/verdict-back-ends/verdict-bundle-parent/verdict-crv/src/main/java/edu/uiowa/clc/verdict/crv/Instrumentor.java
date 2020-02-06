/* See LICENSE in project directory */
package edu.uiowa.clc.verdict.crv;

import edu.uiowa.clc.verdict.vdm.instrumentor.VDMInstrumentor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import verdict.vdm.vdm_model.BlockImpl;
import verdict.vdm.vdm_model.CompInstancePort;
import verdict.vdm.vdm_model.ComponentImpl;
import verdict.vdm.vdm_model.ComponentInstance;
import verdict.vdm.vdm_model.ComponentType;
import verdict.vdm.vdm_model.Connection;
import verdict.vdm.vdm_model.ConnectionEnd;
import verdict.vdm.vdm_model.ConnectionType;
import verdict.vdm.vdm_model.KindOfComponent;
import verdict.vdm.vdm_model.Model;
import verdict.vdm.vdm_model.PedigreeType;
import verdict.vdm.vdm_model.Port;
import verdict.vdm.vdm_model.PortMode;

// Accepts options of instrumentation and perform the operation.
// Also supports PostProcessing Kind2 Results by
// managing Mapping b/t Threats <=> Components <=> Links
// One - Component map Set of Links
// One Threat Maps List of Links
public class Instrumentor extends VDMInstrumentor {

    //
    // // Instrumentor class code:
    // private Model vdm_model;

    // ThreatID, Components/Links
    private HashMap<String, HashSet<String>> attack_cmp_link_map =
            new HashMap<String, HashSet<String>>();

    public Instrumentor(Model vdm_model) {
        super(vdm_model);
    }

    public HashMap<String, HashSet<String>> getAttackMap() {
        return this.attack_cmp_link_map;
    }

    public static Options createOptions() {

        final Options options = new Options();

        Option input_opt = new Option("i", "VDM Model", true, "Input Model File");
        Option output_opt = new Option("o", "Instrumented Model", true, "Instrumented Model File");

        Option bresult_output_opt =
                new Option("r", "Blame Assignment Output", true, "Blame Assignment Result File");

        Option kind2_output_opt = new Option("k", "Kind2 Result Output", true, "Kind2 Result File");

        options.addOption(input_opt);
        options.addOption(output_opt);

        options.addOption(bresult_output_opt);
        options.addOption(kind2_output_opt);

        //
        // "Attacks Library:\n "
        // + "1. LS = Location Spoofing \n"
        // + "2. NI = NetWork Injection \n "
        // + "3. LB = Logic Bomb \n"
        // + "4. IT = Insider Threat \n"
        // + "5. OT = Outside User Threat \n"
        // + "6. RI = Remote Code Injection \n"
        // + "7. SV = Software virus/malware/worm/trojan \n"
        // + "8. HT = Hardware Trojans \n"
        // + "9. BG = Benign (Default) \n");

        // Attack Library Options
        Option ls_opt =
                new Option(
                        "LS",
                        "Location Spoofing",
                        false,
                        "Location Spoofing attack Instrumentation");
        Option ni_opt =
                new Option("NI", "Network Injection", false, "Network Injection Instrumentation");
        Option lb_opt = new Option("LB", "Logic Bomb", false, "Logic Bomb Instrumentation");
        Option ht_opt =
                new Option("HT", "Harware Trojan", false, "Harware Trojans Instrumentation");

        Option sv_opt =
                new Option(
                        "SV",
                        "Software Virus/malware/worm/trojan",
                        false,
                        "Software Virus/malware/worm/trojan Instrumentation");

        Option ri_opt =
                new Option(
                        "RI",
                        "Remotet Code Injection",
                        false,
                        "Remotet Code Injection Instrumentation");

        Option ot_opt =
                new Option("OT", "Outsider Threat", false, "Outsider Threat Instrumentation");

        Option it_opt = new Option("IT", "Insider Threat", false, "Insider Threat Instrumentation");

        Option bn_opt = new Option("BN", "Benign", false, "Benign (Default)");

        Option bm_opt =
                new Option("B", "Blame Assignment", false, "Blame Assignment (Link Level) Default");

        Option bl_opt =
                new Option(
                        "C",
                        "Blame Assignment (Component)",
                        false,
                        "Blame Assignment (Link Level)");

        options.addOption(ls_opt);
        options.addOption(ni_opt);
        options.addOption(lb_opt);
        options.addOption(ht_opt);
        options.addOption(sv_opt);
        options.addOption(ri_opt);
        options.addOption(ot_opt);
        options.addOption(it_opt);
        options.addOption(bn_opt);
        options.addOption(bm_opt);
        options.addOption(bl_opt);

        return options;
    }

    public Model instrument(Model vdm_model, CommandLine cmdLine) {

        //        Model instrumented_model = null;

        String[] possibleThreats = {"LS", "LB", "NI", "SV", "RI", "OT", "IT", "HT", "BG"};
        List<String> threats =
                Arrays.asList(possibleThreats).stream()
                        .filter(threat -> cmdLine.hasOption(threat))
                        .collect(Collectors.toList());
        boolean blameAssignment = cmdLine.hasOption("B");
        boolean componentLevel = cmdLine.hasOption("C");

        retrieve_component_and_channels(vdm_model, threats, blameAssignment, componentLevel);

        return vdm_model;
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

    // Instrument Link for all outgoing edges
    @Override
    public HashSet<Connection> instrument_component(ComponentType component, BlockImpl blockImpl) {

        HashSet<Connection> vdm_links = new HashSet<Connection>();

        HashSet<String> links = new HashSet<String>();

        for (Port port : component.getPort()) {

            PortMode mode = port.getMode();

            if (mode == PortMode.OUT) {

                //                for (Connection connection : blockImpl.getConnection()) {
                //                    links.add(connection.getName());
                //                    links.add(port.getName());
                //                    links.addAll(get_ports(connection));
                //                }
                //                links.add(port.getName());
            }
            {
                // instrument_link(port, blockImpl);
                if (blockImpl != null) {
                    for (Connection connection : blockImpl.getConnection()) {
                        if (retrieve_links(component, connection, port)) {
                            vdm_links.add(connection);
                            links.add(connection.getName());
                            //                        links.add(get_ports(vdm_links));
                        }
                        //                    links.addAll(get_ports(connection));
                    }
                } else {

                }
            }
        }

        String attack_type = getThreatID(component.getId());

        if (this.attack_cmp_link_map.containsKey(attack_type)) {
            HashSet<String> cmp_links = this.attack_cmp_link_map.get(attack_type);
            for (Connection con : vdm_links) {
                cmp_links.addAll(get_ports(con));
            }
        }
        //        System.out.println(links);
        return vdm_links;
    }

    private String getThreatID(String componentID) {

        String attack_type = "None";

        for (String attack_id : this.attack_cmp_link_map.keySet()) {

            HashSet<String> comps = this.attack_cmp_link_map.get(attack_id);

            if (comps.contains(componentID)) {
                attack_type = attack_id;
                break;
            }
        }

        return attack_type;
    }
    // LS:
    // - Select all components in the model M such that:
    // c.Component-Group = 'GPS' v 'IMU' v 'LIDAR'
    @Override
    public void locationSpoofing(HashSet<ComponentType> vdm_components) {

        HashSet<String> components = new HashSet<String>();

        BlockImpl blockImpl = null;

        for (ComponentImpl componentImpl : vdm_model.getComponentImpl()) {

            blockImpl = componentImpl.getBlockImpl();

            // BlockImpl
            if (blockImpl != null) {

                ComponentType componentType = componentImpl.getType();

                for (ComponentInstance componentInstance : blockImpl.getSubcomponent()) {

                    componentType = componentInstance.getSpecification();
                    ComponentImpl subcomponentImpl = componentInstance.getImplementation();

                    // Option 1) Specification
                    if (componentType != null) {

                    }
                    // Option 2) Implementation
                    else if (subcomponentImpl != null) {

                        componentType = subcomponentImpl.getType();
                    }

                    String component_group = componentInstance.getCategory();
                    if (component_group == null) {
                        component_group = "";
                    }

                    if (component_group.equals("GPS")
                            || component_group.equals("DME_VOR")
                            || component_group.equals("IRU")) {
                        vdm_components.add(componentType);
                        components.add(componentType.getId());
                    }
                }
            }
        }

        this.attack_cmp_link_map.put("LS", components);

        //		return components;
    }

    // NI:
    // - Select all channels ch in the model M such that:
    // ch.ConnectionType = Remote & ch.Connection-Encrypted = False &
    // ch.Connection-Authentication = False
    @Override
    public void networkInjection(HashSet<Connection> vdm_links) {

        HashSet<String> links = new HashSet<String>();

        // ArrayList<Connection> selected_channels = new ArrayList<Connection>();

        boolean data_encryption = false;
        boolean authentication = false;
        BlockImpl blockImpl = null;

        for (ComponentImpl componentImpl : vdm_model.getComponentImpl()) {
            blockImpl = componentImpl.getBlockImpl();
            // BlockImpl
            if (blockImpl != null) {

                // Selection channels (Authentication = OFF & DataEncrypted = OFF)
                for (Connection connection : blockImpl.getConnection()) {
                    // visit(connection, instrumented_channel);
                    ConnectionType con_type = connection.getConnType();

                    System.out.println(
                            "Connection Name: "
                                    + connection.getName()
                                    + " "
                                    + connection.isEncryptedTransmission()
                                    + " "
                                    + connection.isDataEncrypted());

                    if (con_type == ConnectionType.REMOTE
                            && connection.isEncryptedTransmission() == data_encryption
                            && connection.isAuthenticated() == authentication) {

                        // selected_channels.add(connection);
                        // LOGGER.info("(" + connection_index++ + ") " +
                        // connection.getName());
                        vdm_links.add(connection);
                        links.add(connection.getName());
                    }
                }
            }
        }

        for (Connection con : vdm_links) {
            links.addAll(get_ports(con));
        }
        this.attack_cmp_link_map.put("NI", links);

        // return links;
    }

    private HashSet<String> get_ports(Connection link) {

        HashSet<String> ports = new HashSet<String>();

        //        for (Connection con : vdm_links) {

        //        ConnectionEnd con_end = link.getSource();
        //        Port src_port = con_end.getComponentPort();
        //
        //        if (src_port == null) {
        //            CompInstancePort instance_port = con_end.getSubcomponentPort();
        //            src_port = instance_port.getPort();
        //        }
        //
        //        ports.add(src_port.getName());

        ConnectionEnd con_end = link.getSource();
        Port dest_port = con_end.getComponentPort();

        if (dest_port == null) {
            CompInstancePort instance_port = con_end.getSubcomponentPort();
            dest_port = instance_port.getPort();
        }

        ports.add(dest_port.getName());

        con_end = link.getDestination();
        dest_port = con_end.getComponentPort();

        if (dest_port == null) {
            CompInstancePort instance_port = con_end.getSubcomponentPort();
            dest_port = instance_port.getPort();
        }

        ports.add(dest_port.getName());

        //        }

        return ports;
    }

    // LB:
    // - Select components c in the model M such that:
    // c.ComponentType = 'Software' v c.ComponentType = 'Hybrid' & c.Manufacturer =
    // 'ThirdParty'
    @Override
    public void logicBomb(HashSet<ComponentType> vdm_components) {

        HashSet<String> components = new HashSet<String>();

        // Conditions
        KindOfComponent component_kind_cond_1 = KindOfComponent.SOFTWARE;
        KindOfComponent component_kind_cond_2 = KindOfComponent.HYBRID;
        //        ManufacturerType manufacturer_cond = ManufacturerType.THIRD_PARTY;

        PedigreeType pedigree_cond1 = PedigreeType.COTS;
        PedigreeType pedigree_cond2 = PedigreeType.SOURCED;

        BlockImpl blockImpl = null;

        for (ComponentImpl componentImpl : vdm_model.getComponentImpl()) {

            blockImpl = componentImpl.getBlockImpl();

            // BlockImpl
            if (blockImpl != null) {

                ComponentType componentType = componentImpl.getType();

                for (ComponentInstance componentInstance : blockImpl.getSubcomponent()) {

                    componentType = componentInstance.getSpecification();
                    ComponentImpl subcomponentImpl = componentInstance.getImplementation();

                    KindOfComponent kind_of_component = componentInstance.getComponentKind();
                    //                    ManufacturerType manufacturer =
                    // componentInstance.getManufacturer();

                    PedigreeType pedgree = componentInstance.getPedigree();
                    // Option 1) Specification
                    if (componentType != null) {

                    }
                    // Option 2) Implementation
                    else if (subcomponentImpl != null) {

                        componentType = subcomponentImpl.getType();
                    }

                    boolean comp_cond_3 = false;

                    if (componentInstance.isAdversariallyTested() != null) {
                        comp_cond_3 = componentInstance.isAdversariallyTested();
                    }

                    if ((kind_of_component == component_kind_cond_1
                                    || kind_of_component == component_kind_cond_2)
                            && (pedgree == pedigree_cond1 || pedgree == pedigree_cond2)
                            && !comp_cond_3) {
                        // Store component
                        // if (!vdm_components.contains(componentType)) {
                        vdm_components.add(componentType);
                        components.add(componentType.getId());
                        // }
                    }
                }
            }
        }

        this.attack_cmp_link_map.put("LB", components);

        //		return components;
    }

    // SV:
    // - Select components c in the model M such that:
    // c.ComponentType = 'Software' v c.ComponentType = 'Hybrid' & c.Manufacturer =
    // 'ThirdParty'
    // & \exists ch\in M. p\in InputPort(c). ch = p.channel & ch.Connectin-Type =
    // Remote
    @Override
    public void softwareVirus(HashSet<ComponentType> vdm_components) {

        HashSet<String> components = new HashSet<String>();

        // Conditions
        KindOfComponent component_kind_cond_1 = KindOfComponent.SOFTWARE;
        KindOfComponent component_kind_cond_2 = KindOfComponent.HYBRID;
        //        ManufacturerType manufacturer_cond = ManufacturerType.THIRD_PARTY;

        PedigreeType pedigree_cond1 = PedigreeType.COTS;
        PedigreeType pedigree_cond2 = PedigreeType.SOURCED;

        BlockImpl blockImpl = null;

        for (ComponentImpl componentImpl : vdm_model.getComponentImpl()) {

            blockImpl = componentImpl.getBlockImpl();

            // BlockImpl
            if (blockImpl != null) {

                ComponentType componentType = componentImpl.getType();

                for (ComponentInstance componentInstance : blockImpl.getSubcomponent()) {

                    componentType = componentInstance.getSpecification();
                    ComponentImpl subcomponentImpl = componentInstance.getImplementation();

                    KindOfComponent kind_of_component = componentInstance.getComponentKind();
                    //                    ManufacturerType manufacturer =
                    // componentInstance.getManufacturer();

                    PedigreeType pedgree = componentInstance.getPedigree();

                    // Option 1) Specification
                    if (componentType != null) {

                    }
                    // Option 2) Implementation
                    else if (subcomponentImpl != null) {

                        componentType = subcomponentImpl.getType();
                    }

                    if ((kind_of_component == component_kind_cond_1
                                    || kind_of_component == component_kind_cond_2)
                            && (pedgree == pedigree_cond1 || pedgree == pedigree_cond2)) {

                        // Port
                        for (Port port : componentType.getPort()) {
                            // System.out.print("(" + port_index + ") ");

                            PortMode mode = port.getMode();
                            if (mode == PortMode.IN) {
                                // Google code style intent add error here.
                                for (Connection con : blockImpl.getConnection()) {

                                    ConnectionType con_type = con.getConnType();

                                    if (con_type == ConnectionType.REMOTE) {

                                        ConnectionEnd src_con = con.getSource();
                                        Port src_port = src_con.getComponentPort();

                                        if (src_port == null) {
                                            CompInstancePort compPort =
                                                    src_con.getSubcomponentPort();
                                            src_port = compPort.getPort();
                                        }

                                        if (port == src_port) {
                                            vdm_components.add(componentType);
                                            components.add(componentType.getId());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        this.attack_cmp_link_map.put("SV", components);

        //		return components;
    }

    // Remote Code Injection:
    // - Select components c in the model M such that:
    // c.ComponentType = 'Software' v c.ComponentType = 'Hybrid'
    // & \exists ch\in M. p\in InputPort(c). ch = p.channel & ch.Connectin-Type =
    // Remote
    @Override
    public void remoteCodeInjection(HashSet<ComponentType> vdm_components) {

        HashSet<String> components = new HashSet<String>();

        // Conditions
        KindOfComponent component_kind_cond_1 = KindOfComponent.SOFTWARE;
        KindOfComponent component_kind_cond_2 = KindOfComponent.HYBRID;

        BlockImpl blockImpl = null;

        for (ComponentImpl componentImpl : vdm_model.getComponentImpl()) {

            blockImpl = componentImpl.getBlockImpl();

            // BlockImpl
            if (blockImpl != null) {

                ComponentType componentType = componentImpl.getType();

                for (ComponentInstance componentInstance : blockImpl.getSubcomponent()) {

                    componentType = componentInstance.getSpecification();
                    ComponentImpl subcomponentImpl = componentInstance.getImplementation();

                    KindOfComponent kind_of_component = componentInstance.getComponentKind();

                    // Option 1) Specification
                    if (componentType != null) {

                    }
                    // Option 2) Implementation
                    else if (subcomponentImpl != null) {

                        componentType = subcomponentImpl.getType();
                    }

                    if ((kind_of_component == component_kind_cond_1
                            || kind_of_component == component_kind_cond_2)) {

                        // Port
                        for (Port port : componentType.getPort()) {
                            // System.out.print("(" + port_index + ") ");

                            PortMode mode = port.getMode();
                            if (mode == PortMode.IN) {
                                // Google code style add errror in here
                                for (Connection con : blockImpl.getConnection()) {

                                    ConnectionType con_type = con.getConnType();

                                    if (con_type == ConnectionType.REMOTE) {

                                        ConnectionEnd src_con = con.getSource();
                                        Port src_port = src_con.getComponentPort();

                                        if (src_port == null) {
                                            CompInstancePort compPort =
                                                    src_con.getSubcomponentPort();
                                            src_port = compPort.getPort();
                                        }

                                        if (port == src_port) {
                                            vdm_components.add(componentType);
                                            components.add(componentType.getId());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        this.attack_cmp_link_map.put("RI", components);

        //		return components;
    }

    // HT
    // - Select all components c in the model M that meet condition:
    // ComponentKind = Hardware v Hybrid and manufacturer = ThirdParty
    @Override
    public void hardwareTrojan(HashSet<ComponentType> vdm_components) {

        HashSet<String> components = new HashSet<String>();

        KindOfComponent component_kind_cond_1 = KindOfComponent.HARDWARE;
        KindOfComponent component_kind_cond_2 = KindOfComponent.HYBRID;

        //        ManufacturerType manufacturer_cond = ManufacturerType.THIRD_PARTY;

        PedigreeType pedigree_cond1 = PedigreeType.COTS;
        PedigreeType pedigree_cond2 = PedigreeType.SOURCED;

        BlockImpl blockImpl = null;

        for (ComponentImpl componentImpl : vdm_model.getComponentImpl()) {

            blockImpl = componentImpl.getBlockImpl();

            // BlockImpl
            if (blockImpl != null) {

                ComponentType componentType = componentImpl.getType();

                for (ComponentInstance componentInstance : blockImpl.getSubcomponent()) {

                    KindOfComponent kind_of_component = componentInstance.getComponentKind();
                    //                    ManufacturerType manufacturer =
                    // componentInstance.getManufacturer();

                    PedigreeType pedgree = componentInstance.getPedigree();

                    componentType = getType(componentInstance);

                    if ((kind_of_component == component_kind_cond_1
                                    || kind_of_component == component_kind_cond_2)
                            && (pedgree == pedigree_cond1 || pedgree == pedigree_cond2)) {
                        // Store component
                        vdm_components.add(componentType);
                        components.add(componentType.getId());
                        // instrument_component(componentType, blockImpl);
                    }
                }
            }
        }

        this.attack_cmp_link_map.put("HT", components);

        //		return components;
    }

    // OT
    // - Select all components c in the model M that meet condition:
    // ComponentKind = Human and c.InsideTrustedBoundary = False
    @Override
    public void outsiderThreat(HashSet<ComponentType> vdm_components) {

        HashSet<String> components = new HashSet<String>();

        KindOfComponent component_kind_cond_1 = KindOfComponent.HUMAN;
        boolean boundary_cond = false;
        BlockImpl blockImpl = null;

        for (ComponentImpl componentImpl : vdm_model.getComponentImpl()) {

            blockImpl = componentImpl.getBlockImpl();

            // BlockImpl
            if (blockImpl != null) {

                ComponentType componentType = componentImpl.getType();

                for (ComponentInstance componentInstance : blockImpl.getSubcomponent()) {

                    KindOfComponent kind_of_component = componentInstance.getComponentKind();

                    componentType = getType(componentInstance);

                    if (kind_of_component == component_kind_cond_1
                            && componentInstance.isInsideTrustedBoundary() == boundary_cond) {
                        // Store component
                        vdm_components.add(componentType);
                        components.add(componentType.getId());

                        // instrument_component(componentType, blockImpl);
                    }
                }
            }
        }

        this.attack_cmp_link_map.put("OT", components);
    }

    // IT
    // - Select all components c in the model M that meet condition:
    // ComponentKind = Human and c.InsideTrustedBoundary = True
    @Override
    public void insiderThreat(HashSet<ComponentType> vdm_components) {

        HashSet<String> components = new HashSet<String>();

        KindOfComponent component_kind_cond_1 = KindOfComponent.HUMAN;
        boolean boundary_cond = true;
        BlockImpl blockImpl = null;

        for (ComponentImpl componentImpl : vdm_model.getComponentImpl()) {

            blockImpl = componentImpl.getBlockImpl();

            // BlockImpl
            if (blockImpl != null) {

                ComponentType componentType = componentImpl.getType();

                for (ComponentInstance componentInstance : blockImpl.getSubcomponent()) {

                    KindOfComponent kind_of_component = componentInstance.getComponentKind();

                    componentType = getType(componentInstance);

                    if (kind_of_component == component_kind_cond_1
                            && componentInstance.isInsideTrustedBoundary() == boundary_cond) {
                        // Store component
                        vdm_components.add(componentType);
                        components.add(componentType.getId());
                        // instrument_component(componentType, blockImpl);
                    }
                }
            }
        }

        this.attack_cmp_link_map.put("IT", components);
    }
}
