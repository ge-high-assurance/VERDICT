/* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   Copyright (c) 2019-2020, General Electric Company.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.

    @author: William D. Smith
    @author: M. Fareed Arif
    @author: Daniel Yahyazadeh
*/

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
import verdict.vdm.vdm_data.GenericAttribute;
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

    public boolean emptyIntrumentation() {
        return emptySelection;
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

        Option all_opt = new Option("AT", "ALL Threats", false, "Enable all attacks");

        Option bn_opt = new Option("BN", "Benign", false, "Benign (Default)");

        Option bm_opt =
                new Option("B", "Blame Assignment", false, "Blame Assignment (Link Level) Default");

        Option mt_opt = new Option("M", "Merit Assignment", false, "Merit Assignment");

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
        options.addOption(all_opt);

        options.addOption(bn_opt);
        options.addOption(bm_opt);
        options.addOption(mt_opt);
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
                if (!isProbePort(con)) {
                    cmp_links.addAll(get_ports(con));
                }
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

    protected GenericAttribute getAttributeByName(
            List<GenericAttribute> genericAttributes, String attributeName, String id)
            throws CRVException {
        GenericAttribute genericAttribute = null;

        for (GenericAttribute attribute : genericAttributes) {
            if (attributeName.equalsIgnoreCase(attribute.getName())) {
                genericAttribute = attribute;
                break;
            }
        }

        if (genericAttribute == null) {
            throw new CRVException(
                    "There is no [" + attributeName + "] attribute for [" + id + "] element.",
                    ErrorCode.GENERIC_ATTRIBUTE_NOT_FOUND);
        } else {
            return genericAttribute;
        }
    }

    // LS:
    // - Select all components c in C such that:
    // c.category = GPS or c.category = IMU or c.category = LIDAR or c.category = LOCATION_DEVICE
    @Override
    public void locationSpoofing(HashSet<ComponentType> vdm_components) {

        try {
            HashSet<String> components = new HashSet<String>();

            HashSet<String> locIdentificationDeviceSet =
                    new HashSet<String>(Arrays.asList("gps", "dme_vor", "iru", "lidar", "imu"));

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

                        List<GenericAttribute> attributeList = componentInstance.getAttribute();

                        GenericAttribute componentCategoryAttribute =
                                getAttributeByName(
                                        attributeList, "Category", componentInstance.getName());

                        String componentCategory = componentCategoryAttribute.getValue().toString();

                        if (locIdentificationDeviceSet.contains(componentCategory.toLowerCase())) {
                            vdm_components.add(componentType);
                            components.add(componentType.getId());
                        }
                    }
                }
            }

            this.attack_cmp_link_map.put("LS", components);

        } catch (CRVException e) {
            System.out.println("\tCRV Error " + e.getCode() + " " + e.getMessage());
        }
    }

    // NI:
    // - Select all channels ch in the model M such that:
    // ch.ConnectionType = Remote & ch.Connection-Encrypted = False &
    // ch.Connection-Authentication = False
    //
    // - Select all channels ch in CH such that:
    // (ch.start.insideTrustedBoundary = false and ch.connectionType = Remote)
    // and ((ch.deviceAuthentication = 0 and ch.sessionAuthenticity = 0) or
    // ch.start.strongCryptoAlgorithms = 0)
    @Override
    public void networkInjection(HashSet<Connection> vdm_links) {

        try {
            HashSet<String> links = new HashSet<String>();

            BlockImpl blockImpl = null;

            for (ComponentImpl componentImpl : vdm_model.getComponentImpl()) {
                blockImpl = componentImpl.getBlockImpl();

                // BlockImpl
                if (blockImpl != null) {

                    // Selection channels (Authentication = OFF & DataEncrypted = OFF)
                    for (Connection connection : blockImpl.getConnection()) {

                        boolean insideTrustedBoundary;
                        int strongCryptoAlgorithms;

                        if (connection.getSource().getSubcomponentPort() != null) {
                            ComponentInstance sourceComponent =
                                    connection.getSource().getSubcomponentPort().getSubcomponent();

                            List<GenericAttribute> sourceComponentAttributeList =
                                    sourceComponent.getAttribute();

                            GenericAttribute insideTrustedBoundaryAttribute =
                                    getAttributeByName(
                                            sourceComponentAttributeList,
                                            "InsideTrustedBoundary",
                                            sourceComponent.getName());
                            GenericAttribute strongCryptoAlgorithmsAttribute =
                                    getAttributeByName(
                                            sourceComponentAttributeList,
                                            "StrongCryptoAlgorithms",
                                            sourceComponent.getName());
                            insideTrustedBoundary =
                                    Boolean.parseBoolean(
                                            insideTrustedBoundaryAttribute.getValue().toString());

                            strongCryptoAlgorithms =
                                    Integer.parseInt(
                                            strongCryptoAlgorithmsAttribute.getValue().toString());
                        } else {
                            insideTrustedBoundary = true;
                            strongCryptoAlgorithms = 1;
                        }

                        List<GenericAttribute> connectionAttributeList = connection.getAttribute();

                        GenericAttribute connectionTypeAttribute =
                                getAttributeByName(
                                        connectionAttributeList,
                                        "ConnectionType",
                                        connection.getName());
                        GenericAttribute deviceAuthenticationAttribute =
                                getAttributeByName(
                                        connectionAttributeList,
                                        "DeviceAuthentication",
                                        connection.getName());
                        GenericAttribute sessionAuthenticityAttribute =
                                getAttributeByName(
                                        connectionAttributeList,
                                        "SessionAuthenticity",
                                        connection.getName());

                        String connectionType =
                                connectionTypeAttribute.getValue().toString().toLowerCase();

                        int deviceAuthentication =
                                Integer.parseInt(
                                        deviceAuthenticationAttribute.getValue().toString());

                        int sessionAuthenticity =
                                Integer.parseInt(
                                        sessionAuthenticityAttribute.getValue().toString());

                        if ((!insideTrustedBoundary || connectionType.equalsIgnoreCase("untrusted"))
                                && ((deviceAuthentication == 0 && sessionAuthenticity == 0)
                                        || strongCryptoAlgorithms == 0)) {

                            vdm_links.add(connection);
                            links.add(connection.getName());
                        }
                    }
                }
            }

            for (Connection con : vdm_links) {
                if (!isProbePort(con)) {
                    links.addAll(get_ports(con));
                }
            }
            this.attack_cmp_link_map.put("NI", links);

        } catch (CRVException e) {
            System.out.println("\tCRV Error " + e.getCode() + " " + e.getMessage());
        }
    }

    private HashSet<String> get_ports(Connection link) {

        HashSet<String> ports = new HashSet<String>();

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

        return ports;
    }

    // LB:
    // - Select components c in C such that:
    // c.ComponentType is in {Software, SwHwHybrid, SwHumanHybrid, Hybrid}
    // and (c.pedigree = COTS and (c.pedigree = Sourced and c.supplyChainSecurity = 0 and
    // c.tamperProtection = 0))
    // and (c.adversariallyTestedForTrojanOrLogicBomb = 0 or C.staticCodeAnalysis = 0)
    @Override
    public void logicBomb(HashSet<ComponentType> vdm_components) {

        try {
            HashSet<String> components = new HashSet<String>();

            HashSet<String> lbComponentTypeSet =
                    new HashSet<String>(
                            Arrays.asList("software", "swhwhybrid", "swhumanhybrid", "hybrid"));

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

                        List<GenericAttribute> attributeList = componentInstance.getAttribute();

                        GenericAttribute componentKindAttribute =
                                getAttributeByName(
                                        attributeList,
                                        "ComponentType",
                                        componentInstance.getName());
                        GenericAttribute adversariallyTestedForTrojanOrLogicBombAttribute =
                                getAttributeByName(
                                        attributeList,
                                        "AdversariallyTestedForTrojanOrLogicBomb",
                                        componentInstance.getName());
                        GenericAttribute pedigreeAttribute =
                                getAttributeByName(
                                        attributeList, "Pedigree", componentInstance.getName());
                        GenericAttribute supplyChainSecurityAttribute =
                                getAttributeByName(
                                        attributeList,
                                        "SupplyChainSecurity",
                                        componentInstance.getName());
                        GenericAttribute tamperProtectionAttribute =
                                getAttributeByName(
                                        attributeList,
                                        "TamperProtection",
                                        componentInstance.getName());
                        GenericAttribute staticCodeAnalysisAttribute =
                                getAttributeByName(
                                        attributeList,
                                        "StaticCodeAnalysis",
                                        componentInstance.getName());

                        String componentKind =
                                componentKindAttribute.getValue().toString().toLowerCase();

                        int adversariallyTestedForTrojanOrLogicBomb =
                                Integer.parseInt(
                                        adversariallyTestedForTrojanOrLogicBombAttribute
                                                .getValue()
                                                .toString());

                        String pedigree = pedigreeAttribute.getValue().toString().toLowerCase();

                        int supplyChainSecurity =
                                Integer.parseInt(
                                        supplyChainSecurityAttribute.getValue().toString());

                        int tamperProtection =
                                Integer.parseInt(tamperProtectionAttribute.getValue().toString());

                        int staticCodeAnalysis =
                                Integer.parseInt(staticCodeAnalysisAttribute.getValue().toString());

                        if (lbComponentTypeSet.contains(componentKind)
                                && (pedigree.equalsIgnoreCase("cots")
                                        || (pedigree.equalsIgnoreCase("sourced")
                                                && supplyChainSecurity == 0
                                                && tamperProtection == 0))
                                && (adversariallyTestedForTrojanOrLogicBomb == 0
                                        || staticCodeAnalysis == 0)) {
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

        } catch (CRVException e) {
            System.out.println("\tCRV Error " + e.getCode() + " " + e.getMessage());
        }
    }

    // SV:
    // - Select components c in the model M such that:
    // c.ComponentType = 'Software' v c.ComponentType = 'Hybrid' & c.Manufacturer =
    // 'ThirdParty'
    // & \exists ch\in M. p\in InputPort(c). ch = p.channel & ch.Connectin-Type =
    // Remote
    @Override
    public void softwareVirus(HashSet<ComponentType> vdm_components) {
        try {
            HashSet<String> components = new HashSet<String>();

            HashSet<String> svComponentTypeSet =
                    new HashSet<String>(
                            Arrays.asList("software", "swhwhybrid", "swhumanhybrid", "hybrid"));

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

                        List<GenericAttribute> attributeList = componentInstance.getAttribute();

                        GenericAttribute componentKindAttribute =
                                getAttributeByName(
                                        attributeList,
                                        "ComponentType",
                                        componentInstance.getName());
                        GenericAttribute staticCodeAnalysisAttribute =
                                getAttributeByName(
                                        attributeList,
                                        "StaticCodeAnalysis",
                                        componentInstance.getName());
                        GenericAttribute inputValidationAttribute =
                                getAttributeByName(
                                        attributeList,
                                        "InputValidation",
                                        componentInstance.getName());
                        GenericAttribute memoryProtectionAttribute =
                                getAttributeByName(
                                        attributeList,
                                        "MemoryProtection",
                                        componentInstance.getName());
                        GenericAttribute secureBootAttribute =
                                getAttributeByName(
                                        attributeList, "SecureBoot", componentInstance.getName());

                        String componentKind =
                                componentKindAttribute.getValue().toString().toLowerCase();

                        int staticCodeAnalysis =
                                Integer.parseInt(staticCodeAnalysisAttribute.getValue().toString());

                        int inputValidation =
                                Integer.parseInt(inputValidationAttribute.getValue().toString());

                        int memoryProtection =
                                Integer.parseInt(memoryProtectionAttribute.getValue().toString());

                        int secureBoot =
                                Integer.parseInt(secureBootAttribute.getValue().toString());

                        if (svComponentTypeSet.contains(componentKind.toLowerCase())
                                && (staticCodeAnalysis == 0
                                        || inputValidation == 0
                                        || memoryProtection == 0
                                        || secureBoot == 0)) {

                            Boolean hasEligibleIncomingChannels = false;

                            for (Port port : componentType.getPort()) {

                                PortMode mode = port.getMode();
                                if (mode == PortMode.IN) {

                                    for (Connection connection : blockImpl.getConnection()) {

                                        if (connection.getDestination().getSubcomponentPort()
                                                != null) {
                                            if (connection
                                                            .getDestination()
                                                            .getSubcomponentPort()
                                                            .getPort()
                                                    == port) {

                                                Boolean scInsideTrustedBoundary;
                                                String scComponentKind;
                                                String scPedigree;
                                                int scStrongCryptoAlgorithms;
                                                int scSupplyChainSecurity;
                                                int scTamperProtection;

                                                if (connection.getSource().getSubcomponentPort()
                                                        != null) {
                                                    ComponentInstance sourceComponent =
                                                            connection
                                                                    .getSource()
                                                                    .getSubcomponentPort()
                                                                    .getSubcomponent();

                                                    List<GenericAttribute>
                                                            sourceComponentAttributeList =
                                                                    sourceComponent.getAttribute();

                                                    GenericAttribute
                                                            sourceComponentInsideTrustedBoundaryAttribute =
                                                                    getAttributeByName(
                                                                            sourceComponentAttributeList,
                                                                            "InsideTrustedBoundary",
                                                                            sourceComponent
                                                                                    .getName());
                                                    GenericAttribute
                                                            sourceComponentComponentKindAttribute =
                                                                    getAttributeByName(
                                                                            sourceComponentAttributeList,
                                                                            "ComponentType",
                                                                            sourceComponent
                                                                                    .getName());
                                                    GenericAttribute
                                                            sourceComponentPedigreeAttribute =
                                                                    getAttributeByName(
                                                                            sourceComponentAttributeList,
                                                                            "Pedigree",
                                                                            sourceComponent
                                                                                    .getName());
                                                    GenericAttribute
                                                            sourceComponentStrongCryptoAlgorithmsAttribute =
                                                                    getAttributeByName(
                                                                            sourceComponentAttributeList,
                                                                            "StrongCryptoAlgorithms",
                                                                            sourceComponent
                                                                                    .getName());
                                                    GenericAttribute
                                                            sourceComponentSupplyChainSecurityAttribute =
                                                                    getAttributeByName(
                                                                            sourceComponentAttributeList,
                                                                            "SupplyChainSecurity",
                                                                            sourceComponent
                                                                                    .getName());
                                                    GenericAttribute
                                                            sourceComponentTamperProtectionAttribute =
                                                                    getAttributeByName(
                                                                            sourceComponentAttributeList,
                                                                            "TamperProtection",
                                                                            sourceComponent
                                                                                    .getName());

                                                    scInsideTrustedBoundary =
                                                            Boolean.parseBoolean(
                                                                    sourceComponentInsideTrustedBoundaryAttribute
                                                                            .getValue()
                                                                            .toString());

                                                    scComponentKind =
                                                            sourceComponentComponentKindAttribute
                                                                    .getValue()
                                                                    .toString()
                                                                    .toLowerCase();

                                                    scPedigree =
                                                            sourceComponentPedigreeAttribute
                                                                    .getValue()
                                                                    .toString()
                                                                    .toLowerCase();

                                                    scStrongCryptoAlgorithms =
                                                            Integer.parseInt(
                                                                    sourceComponentStrongCryptoAlgorithmsAttribute
                                                                            .getValue()
                                                                            .toString());

                                                    scSupplyChainSecurity =
                                                            Integer.parseInt(
                                                                    sourceComponentSupplyChainSecurityAttribute
                                                                            .getValue()
                                                                            .toString());

                                                    scTamperProtection =
                                                            Integer.parseInt(
                                                                    sourceComponentTamperProtectionAttribute
                                                                            .getValue()
                                                                            .toString());

                                                } else {
                                                    scInsideTrustedBoundary = true;
                                                    scComponentKind = "";
                                                    scPedigree = "";
                                                    scStrongCryptoAlgorithms = -1;
                                                    scSupplyChainSecurity = -1;
                                                    scTamperProtection = -1;
                                                }

                                                List<GenericAttribute> connectionAttributeList =
                                                        connection.getAttribute();

                                                GenericAttribute connectionTypeAttribute =
                                                        getAttributeByName(
                                                                connectionAttributeList,
                                                                "ConnectionType",
                                                                connection.getName());
                                                GenericAttribute deviceAuthenticationAttribute =
                                                        getAttributeByName(
                                                                connectionAttributeList,
                                                                "DeviceAuthentication",
                                                                connection.getName());
                                                GenericAttribute sessionAuthenticityAttribute =
                                                        getAttributeByName(
                                                                connectionAttributeList,
                                                                "SessionAuthenticity",
                                                                connection.getName());

                                                String connectionType =
                                                        connectionTypeAttribute
                                                                .getValue()
                                                                .toString()
                                                                .toLowerCase();

                                                int deviceAuthentication =
                                                        Integer.parseInt(
                                                                deviceAuthenticationAttribute
                                                                        .getValue()
                                                                        .toString());

                                                int sessionAuthenticity =
                                                        Integer.parseInt(
                                                                sessionAuthenticityAttribute
                                                                        .getValue()
                                                                        .toString());

                                                if ((!scInsideTrustedBoundary
                                                                || connectionType.equalsIgnoreCase(
                                                                        "untrusted"))
                                                        && !scComponentKind.equalsIgnoreCase(
                                                                "hardware")
                                                        && ((scPedigree.equalsIgnoreCase("cots")
                                                                        || (scPedigree
                                                                                        .equalsIgnoreCase(
                                                                                                "sourced")
                                                                                && scSupplyChainSecurity
                                                                                        == 0
                                                                                && scTamperProtection
                                                                                        == 0))
                                                                || ((deviceAuthentication == 0
                                                                                && sessionAuthenticity
                                                                                        == 0)
                                                                        || scStrongCryptoAlgorithms
                                                                                == 0))) {

                                                    hasEligibleIncomingChannels = true;
                                                }
                                                break;
                                            }
                                        }
                                    }
                                }

                                if (hasEligibleIncomingChannels) {
                                    break;
                                }
                            }

                            if (hasEligibleIncomingChannels) {
                                vdm_components.add(componentType);
                                components.add(componentType.getId());
                            }
                        }
                    }
                }
            }

            this.attack_cmp_link_map.put("SV", components);

        } catch (CRVException e) {
            System.out.println("\tCRV Error " + e.getCode() + " " + e.getMessage());
        }
    }

    // Remote Code Injection:
    // - Select components c in the model M such that:
    // c.ComponentType = 'Software' v c.ComponentType = 'Hybrid'
    // & \exists ch\in M. p\in InputPort(c). ch = p.channel & ch.Connectin-Type =
    // Remote
    @Override
    public void remoteCodeInjection(HashSet<ComponentType> vdm_components) {
        try {
            HashSet<String> components = new HashSet<String>();

            HashSet<String> rciComponentTypeSet =
                    new HashSet<String>(
                            Arrays.asList("software", "swhwhybrid", "swhumanhybrid", "hybrid"));

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

                        List<GenericAttribute> attributeList = componentInstance.getAttribute();

                        GenericAttribute componentKindAttribute =
                                getAttributeByName(
                                        attributeList,
                                        "ComponentType",
                                        componentInstance.getName());
                        GenericAttribute staticCodeAnalysisAttribute =
                                getAttributeByName(
                                        attributeList,
                                        "StaticCodeAnalysis",
                                        componentInstance.getName());
                        GenericAttribute inputValidationAttribute =
                                getAttributeByName(
                                        attributeList,
                                        "InputValidation",
                                        componentInstance.getName());
                        GenericAttribute memoryProtectionAttribute =
                                getAttributeByName(
                                        attributeList,
                                        "MemoryProtection",
                                        componentInstance.getName());

                        String componentKind =
                                componentKindAttribute.getValue().toString().toLowerCase();

                        int staticCodeAnalysis =
                                Integer.parseInt(staticCodeAnalysisAttribute.getValue().toString());

                        int inputValidation =
                                Integer.parseInt(inputValidationAttribute.getValue().toString());

                        int memoryProtection =
                                Integer.parseInt(memoryProtectionAttribute.getValue().toString());

                        if (rciComponentTypeSet.contains(componentKind.toLowerCase())
                                && (staticCodeAnalysis == 0
                                        || inputValidation == 0
                                        || memoryProtection == 0)) {

                            Boolean hasEligibleIncomingChannels = false;

                            for (Port port : componentType.getPort()) {

                                PortMode mode = port.getMode();
                                if (mode == PortMode.IN) {

                                    for (Connection connection : blockImpl.getConnection()) {

                                        if (connection.getDestination().getSubcomponentPort()
                                                != null) {
                                            if (connection
                                                            .getDestination()
                                                            .getSubcomponentPort()
                                                            .getPort()
                                                    == port) {

                                                Boolean scInsideTrustedBoundary;
                                                String scComponentKind;
                                                String scPedigree;
                                                int scStrongCryptoAlgorithms;
                                                int scSupplyChainSecurity;
                                                int scTamperProtection;

                                                if (connection.getSource().getSubcomponentPort()
                                                        != null) {
                                                    ComponentInstance sourceComponent =
                                                            connection
                                                                    .getSource()
                                                                    .getSubcomponentPort()
                                                                    .getSubcomponent();

                                                    List<GenericAttribute>
                                                            sourceComponentAttributeList =
                                                                    sourceComponent.getAttribute();

                                                    GenericAttribute
                                                            sourceComponentInsideTrustedBoundaryAttribute =
                                                                    getAttributeByName(
                                                                            sourceComponentAttributeList,
                                                                            "InsideTrustedBoundary",
                                                                            sourceComponent
                                                                                    .getName());
                                                    GenericAttribute
                                                            sourceComponentComponentKindAttribute =
                                                                    getAttributeByName(
                                                                            sourceComponentAttributeList,
                                                                            "ComponentType",
                                                                            sourceComponent
                                                                                    .getName());
                                                    GenericAttribute
                                                            sourceComponentPedigreeAttribute =
                                                                    getAttributeByName(
                                                                            sourceComponentAttributeList,
                                                                            "Pedigree",
                                                                            sourceComponent
                                                                                    .getName());
                                                    GenericAttribute
                                                            sourceComponentStrongCryptoAlgorithmsAttribute =
                                                                    getAttributeByName(
                                                                            sourceComponentAttributeList,
                                                                            "StrongCryptoAlgorithms",
                                                                            sourceComponent
                                                                                    .getName());
                                                    GenericAttribute
                                                            sourceComponentSupplyChainSecurityAttribute =
                                                                    getAttributeByName(
                                                                            sourceComponentAttributeList,
                                                                            "SupplyChainSecurity",
                                                                            sourceComponent
                                                                                    .getName());
                                                    GenericAttribute
                                                            sourceComponentTamperProtectionAttribute =
                                                                    getAttributeByName(
                                                                            sourceComponentAttributeList,
                                                                            "TamperProtection",
                                                                            sourceComponent
                                                                                    .getName());

                                                    scInsideTrustedBoundary =
                                                            Boolean.parseBoolean(
                                                                    sourceComponentInsideTrustedBoundaryAttribute
                                                                            .getValue()
                                                                            .toString());

                                                    scComponentKind =
                                                            sourceComponentComponentKindAttribute
                                                                    .getValue()
                                                                    .toString()
                                                                    .toLowerCase();

                                                    scPedigree =
                                                            sourceComponentPedigreeAttribute
                                                                    .getValue()
                                                                    .toString()
                                                                    .toLowerCase();

                                                    scStrongCryptoAlgorithms =
                                                            Integer.parseInt(
                                                                    sourceComponentStrongCryptoAlgorithmsAttribute
                                                                            .getValue()
                                                                            .toString());

                                                    scSupplyChainSecurity =
                                                            Integer.parseInt(
                                                                    sourceComponentSupplyChainSecurityAttribute
                                                                            .getValue()
                                                                            .toString());

                                                    scTamperProtection =
                                                            Integer.parseInt(
                                                                    sourceComponentTamperProtectionAttribute
                                                                            .getValue()
                                                                            .toString());

                                                } else {
                                                    scInsideTrustedBoundary = true;
                                                    scComponentKind = "";
                                                    scPedigree = "";
                                                    scStrongCryptoAlgorithms = -1;
                                                    scSupplyChainSecurity = -1;
                                                    scTamperProtection = -1;
                                                }

                                                List<GenericAttribute> connectionAttributeList =
                                                        connection.getAttribute();

                                                GenericAttribute connectionTypeAttribute =
                                                        getAttributeByName(
                                                                connectionAttributeList,
                                                                "ConnectionType",
                                                                connection.getName());
                                                GenericAttribute deviceAuthenticationAttribute =
                                                        getAttributeByName(
                                                                connectionAttributeList,
                                                                "DeviceAuthentication",
                                                                connection.getName());
                                                GenericAttribute sessionAuthenticityAttribute =
                                                        getAttributeByName(
                                                                connectionAttributeList,
                                                                "SessionAuthenticity",
                                                                connection.getName());

                                                String connectionType =
                                                        connectionTypeAttribute
                                                                .getValue()
                                                                .toString()
                                                                .toLowerCase();

                                                int deviceAuthentication =
                                                        Integer.parseInt(
                                                                deviceAuthenticationAttribute
                                                                        .getValue()
                                                                        .toString());

                                                int sessionAuthenticity =
                                                        Integer.parseInt(
                                                                sessionAuthenticityAttribute
                                                                        .getValue()
                                                                        .toString());

                                                if ((!scInsideTrustedBoundary
                                                                || connectionType.equalsIgnoreCase(
                                                                        "untrusted"))
                                                        && !scComponentKind.equalsIgnoreCase(
                                                                "hardware")
                                                        && ((scPedigree.equalsIgnoreCase("cots")
                                                                        || (scPedigree
                                                                                        .equalsIgnoreCase(
                                                                                                "sourced")
                                                                                && scSupplyChainSecurity
                                                                                        == 0
                                                                                && scTamperProtection
                                                                                        == 0))
                                                                || ((deviceAuthentication == 0
                                                                                && sessionAuthenticity
                                                                                        == 0)
                                                                        || scStrongCryptoAlgorithms
                                                                                == 0))) {

                                                    hasEligibleIncomingChannels = true;
                                                }
                                                break;
                                            }
                                        }
                                    }
                                }

                                if (hasEligibleIncomingChannels) {
                                    break;
                                }
                            }

                            if (hasEligibleIncomingChannels) {
                                vdm_components.add(componentType);
                                components.add(componentType.getId());
                            }
                        }
                    }
                }
            }

            this.attack_cmp_link_map.put("RI", components);

        } catch (CRVException e) {
            System.out.println("CRV Error " + e.getCode() + e.getMessage());
        }
    }

    // HT
    // - Select all components c in C such that:
    // c.ComponentKind is in {Hardware, SwHwHybrid, HwHumanHybrid, Hybrid}
    // and c.adversariallyTestedForTrojanOrLogicBomb = 0
    // and (c.pedigree = COTS or (c.pedigree = Sourced and c.supplyChainSecurity = 0 and
    // c.tamperProtection = 0))
    @Override
    public void hardwareTrojan(HashSet<ComponentType> vdm_components) {

        try {
            HashSet<String> components = new HashSet<String>();

            HashSet<String> htComponentTypeSet =
                    new HashSet<String>(
                            Arrays.asList("hardware", "swhwhybrid", "hwhumanhybrid", "hybrid"));

            BlockImpl blockImpl = null;

            for (ComponentImpl componentImpl : vdm_model.getComponentImpl()) {

                blockImpl = componentImpl.getBlockImpl();

                // BlockImpl
                if (blockImpl != null) {

                    ComponentType componentType = componentImpl.getType();

                    for (ComponentInstance componentInstance : blockImpl.getSubcomponent()) {

                        componentType = getType(componentInstance);

                        List<GenericAttribute> attributeList = componentInstance.getAttribute();

                        GenericAttribute componentKindAttribute =
                                getAttributeByName(
                                        attributeList,
                                        "ComponentType",
                                        componentInstance.getName());
                        GenericAttribute adversariallyTestedForTrojanOrLogicBombAttribute =
                                getAttributeByName(
                                        attributeList,
                                        "AdversariallyTestedForTrojanOrLogicBomb",
                                        componentInstance.getName());
                        GenericAttribute pedigreeAttribute =
                                getAttributeByName(
                                        attributeList, "Pedigree", componentInstance.getName());
                        GenericAttribute supplyChainSecurityAttribute =
                                getAttributeByName(
                                        attributeList,
                                        "SupplyChainSecurity",
                                        componentInstance.getName());
                        GenericAttribute tamperProtectionAttribute =
                                getAttributeByName(
                                        attributeList,
                                        "TamperProtection",
                                        componentInstance.getName());

                        String componentKind =
                                componentKindAttribute.getValue().toString().toLowerCase();

                        int adversariallyTestedForTrojanOrLogicBomb =
                                Integer.parseInt(
                                        adversariallyTestedForTrojanOrLogicBombAttribute
                                                .getValue()
                                                .toString());

                        String pedigree = pedigreeAttribute.getValue().toString().toLowerCase();

                        int supplyChainSecurity =
                                Integer.parseInt(
                                        supplyChainSecurityAttribute.getValue().toString());

                        int tamperProtection =
                                Integer.parseInt(tamperProtectionAttribute.getValue().toString());

                        if (htComponentTypeSet.contains(componentKind)
                                && adversariallyTestedForTrojanOrLogicBomb == 0
                                && (pedigree.equalsIgnoreCase("cots")
                                        || (pedigree.equalsIgnoreCase("sourced")
                                                && supplyChainSecurity == 0
                                                && tamperProtection == 0))) {
                            // Store component
                            vdm_components.add(componentType);
                            components.add(componentType.getId());
                            // instrument_component(componentType, blockImpl);
                        }
                    }
                }
            }

            this.attack_cmp_link_map.put("HT", components);

        } catch (CRVException e) {
            System.out.println("\tCRV Error " + e.getCode() + " " + e.getMessage());
        }
    }

    // OT
    // - Select all components c in C such that:
    // c.componentType is in {Human, SwHumanHybrid, Hybrid, HwHumanHybrid}
    // and c.insideTrustBoundary = false and c.physicalAccessControl = 0
    // and (c.logging = 0 and (c.systemAccessControl = 0 and c.userAuthentication = 0))
    @Override
    public void outsiderThreat(HashSet<ComponentType> vdm_components) {
        try {
            HashSet<String> components = new HashSet<String>();

            HashSet<String> otComponentTypeSet =
                    new HashSet<String>(
                            Arrays.asList("human", "swhumanhybrid", "hwhumanhybrid", "hybrid"));

            BlockImpl blockImpl = null;

            for (ComponentImpl componentImpl : vdm_model.getComponentImpl()) {

                blockImpl = componentImpl.getBlockImpl();

                // BlockImpl
                if (blockImpl != null) {

                    ComponentType componentType = componentImpl.getType();

                    for (ComponentInstance componentInstance : blockImpl.getSubcomponent()) {

                        componentType = getType(componentInstance);

                        List<GenericAttribute> attributeList = componentInstance.getAttribute();

                        GenericAttribute componentKindAttribute =
                                getAttributeByName(
                                        attributeList,
                                        "ComponentType",
                                        componentInstance.getName());
                        GenericAttribute insideTrustedBoundaryAttribute =
                                getAttributeByName(
                                        attributeList,
                                        "InsideTrustedBoundary",
                                        componentInstance.getName());
                        GenericAttribute physicalAccessControlAttribute =
                                getAttributeByName(
                                        attributeList,
                                        "PhysicalAccessControl",
                                        componentInstance.getName());
                        GenericAttribute loggingAttribute =
                                getAttributeByName(
                                        attributeList, "Logging", componentInstance.getName());
                        GenericAttribute systemAccessControlAttribute =
                                getAttributeByName(
                                        attributeList,
                                        "SystemAccessControl",
                                        componentInstance.getName());
                        GenericAttribute userAuthenticationAttribute =
                                getAttributeByName(
                                        attributeList,
                                        "UserAuthentication",
                                        componentInstance.getName());

                        String componentKind =
                                componentKindAttribute.getValue().toString().toLowerCase();

                        Boolean insideTrustedBoundary =
                                Boolean.parseBoolean(
                                        insideTrustedBoundaryAttribute.getValue().toString());

                        int physicalAccessControl =
                                Integer.parseInt(
                                        physicalAccessControlAttribute.getValue().toString());

                        int logging = Integer.parseInt(loggingAttribute.getValue().toString());

                        int systemAccessControl =
                                Integer.parseInt(
                                        systemAccessControlAttribute.getValue().toString());

                        int userAuthentication =
                                Integer.parseInt(userAuthenticationAttribute.getValue().toString());

                        if (otComponentTypeSet.contains(componentKind)
                                && !insideTrustedBoundary
                                && physicalAccessControl == 0
                                && (logging == 0
                                        && (systemAccessControl == 0 || userAuthentication == 0))) {
                            // Store component
                            vdm_components.add(componentType);
                            components.add(componentType.getId());

                            // instrument_component(componentType, blockImpl);
                        }
                    }
                }
            }

            this.attack_cmp_link_map.put("OT", components);

        } catch (CRVException e) {
            System.out.println("\tCRV Error " + e.getCode() + " " + e.getMessage());
        }
    }

    // IT
    // - Select all components c in C such that:
    // c.componentType in {Human, SwHumanHybrid, HwHumanHybrid, Hybrid}
    // and c.insideTrustBoundary = true
    // and (c.logging = 0 and (c.systemAccessControl = 0 or c.userAuthentication = 0))
    @Override
    public void insiderThreat(HashSet<ComponentType> vdm_components) {

        try {
            HashSet<String> components = new HashSet<String>();

            HashSet<String> itComponentTypeSet =
                    new HashSet<String>(
                            Arrays.asList("human", "swhumanhybrid", "hwhumanhybrid", "hybrid"));

            BlockImpl blockImpl = null;

            for (ComponentImpl componentImpl : vdm_model.getComponentImpl()) {

                blockImpl = componentImpl.getBlockImpl();

                // BlockImpl
                if (blockImpl != null) {

                    ComponentType componentType = componentImpl.getType();

                    for (ComponentInstance componentInstance : blockImpl.getSubcomponent()) {

                        componentType = getType(componentInstance);

                        List<GenericAttribute> attributeList = componentInstance.getAttribute();

                        GenericAttribute componentKindAttribute =
                                getAttributeByName(
                                        attributeList,
                                        "ComponentType",
                                        componentInstance.getName());
                        GenericAttribute insideTrustedBoundaryAttribute =
                                getAttributeByName(
                                        attributeList,
                                        "InsideTrustedBoundary",
                                        componentInstance.getName());
                        GenericAttribute loggingAttribute =
                                getAttributeByName(
                                        attributeList, "Logging", componentInstance.getName());
                        GenericAttribute systemAccessControlAttribute =
                                getAttributeByName(
                                        attributeList,
                                        "SystemAccessControl",
                                        componentInstance.getName());
                        GenericAttribute userAuthenticationAttribute =
                                getAttributeByName(
                                        attributeList,
                                        "UserAuthentication",
                                        componentInstance.getName());

                        String componentKind =
                                componentKindAttribute.getValue().toString().toLowerCase();

                        Boolean insideTrustedBoundary =
                                Boolean.parseBoolean(
                                        insideTrustedBoundaryAttribute.getValue().toString());

                        int logging = Integer.parseInt(loggingAttribute.getValue().toString());

                        int systemAccessControl =
                                Integer.parseInt(
                                        systemAccessControlAttribute.getValue().toString());

                        int userAuthentication =
                                Integer.parseInt(userAuthenticationAttribute.getValue().toString());

                        if (itComponentTypeSet.contains(componentKind)
                                && insideTrustedBoundary
                                && (logging == 0
                                        && (systemAccessControl == 0 || userAuthentication == 0))) {
                            // Store component
                            vdm_components.add(componentType);
                            components.add(componentType.getId());
                            // instrument_component(componentType, blockImpl);
                        }
                    }
                }
            }

            this.attack_cmp_link_map.put("IT", components);

        } catch (CRVException e) {
            System.out.println("\tCRV Error " + e.getCode() + " " + e.getMessage());
        }
    }
}
