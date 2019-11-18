/* See LICENSE in project directory */
package com.ge.verdict.mbas;

import com.ge.verdict.vdm.VdmTranslator;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import verdict.vdm.vdm_model.CIAPort;
import verdict.vdm.vdm_model.ComponentImpl;
import verdict.vdm.vdm_model.ComponentInstance;
import verdict.vdm.vdm_model.ComponentType;
import verdict.vdm.vdm_model.Connection;
import verdict.vdm.vdm_model.CyberExpr;
import verdict.vdm.vdm_model.CyberRel;
import verdict.vdm.vdm_model.CyberReq;
import verdict.vdm.vdm_model.Mission;
import verdict.vdm.vdm_model.Model;

/** Convert parsed VDM XML to CSV files for input to MBAS (STEM and Soteria++). */
public class VDM2CSV extends VdmTranslator {

    /**
     * Marshal a Verdict data model to Mbas files.
     *
     * <p>Produces the following files:
     *
     * <ul>
     *   <li>ScnArch.csv (STEM and Soteria++)
     *   <li>ScnCompProps.csv (STEM)
     *   <li>ScnConnectionProps.csv (STEM)
     *   <li>CompSaf.csv (Soteria++)
     *   <li>Events.csv (Soteria++)
     *   <li>ScnComp.csv (Soteria++)
     *   <li>CompDep.csv (Soteria++)
     *   <li>Mission.csv (Soteria++)
     * </ul>
     *
     * @param model Verdict data model to marshal
     * @param inputPath input path of the Verdict data model
     * @param stemOutputPath output path where the STEM related CSV files be written to
     * @param soteriaOutputPath output path where the Soteria++ related CSV files be written to
     */
    public void marshalToMbasInputs(
            Model model, String inputPath, String stemOutputPath, String soteriaOutputPath) {

        String scenario = (new File(inputPath)).getName().replace(".xml", "");

        if (scenario.length() == 0) {
            System.err.println(
                    "Error: Input path is not in the correct format. Scenario name is empty.");
        }

        Table scnArchTable = buildScnArchTable(model, scenario);
        Table scnCompPropsTable = buildScnCompPropsTable(model, scenario);
        Table compDepTable = buildCompDepTable(model);
        Table missionTable = buildMissionTable(model, scenario);
        // New CSV files
        Table scnConnectionPropsTable = buildScnConnectionPropsTable(model, scenario);
        Table compSafTable = buildCompSafTable(model, scenario);
        Table eventsTable = buildEventsTable(model, scenario);

        // Generate STEM input
        scnArchTable.toCsvFile(new File(stemOutputPath, "ScnArch.csv"));
        scnCompPropsTable.toCsvFile(new File(stemOutputPath, "ScnCompProps.csv"));
        scnConnectionPropsTable.toCsvFile(new File(stemOutputPath, "ScnConnectionProps.csv"));

        // Generate Soteria_pp input
        scnArchTable.toCsvFile(new File(soteriaOutputPath, "ScnArch.csv"));
        compDepTable.toCsvFile(new File(soteriaOutputPath, "CompDep.csv"));
        missionTable.toCsvFile(new File(soteriaOutputPath, "Mission.csv"));
        compSafTable.toCsvFile(new File(soteriaOutputPath, "CompSaf.csv"));
        eventsTable.toCsvFile(new File(soteriaOutputPath, "Events.csv"));
    }

    /**
     * Build the component safety table.
     *
     * @param model
     * @param scenario
     * @return
     */
    private Table buildCompSafTable(Model model, String scenario) {
        Table table = new Table("Comp", "InputPortOrEvent", "InputIA", "OutputPort", "OutputIA");
        //        for (ComponentType comp : model.getComponentType()) {
        //            table.addValue(comp.getName()); // comp
        //        	for(SafetyRel rel : comp.getSafetyRel()) {
        //
        //        	}
        //            table.capRow();
        //        }

        return table;
    }

    /**
     * Build the events table.
     *
     * @param model
     * @param scenario
     * @return
     */
    private Table buildEventsTable(Model model, String scenario) {
        Table table = new Table("Comp", "Event", "Probability");
        //        for (ComponentType comp : model.getComponentType()) {
        //            table.addValue(comp.getName()); // comp
        //        	for(SafetyRel rel : comp.getSafetyRel()) {
        //
        //        	}
        //            table.capRow();
        //        }

        return table;
    }

    /**
     * Build the scenario connection properties table.
     *
     * <p>Lists the properties associated with each connection.
     *
     * @param model
     * @param scenario
     * @return
     */
    private Table buildScnConnectionPropsTable(Model model, String scenario) {
        Table table =
                new Table(
                        "Scenario",
                        "ConnectionName",
                        "SrcComp",
                        "SrcImpl",
                        "SrcCompInstance",
                        "DestComp",
                        "DestImpl",
                        "DestCompInstance",
                        "Flow1",
                        "Flow2",
                        "Flow3",
                        "trustedConnection",
                        "encryptedTransmission");
        for (ComponentImpl comp : model.getComponentImpl()) {
            // if a component implementation does have connections, we continue
            if (comp.getBlockImpl() == null || comp.getBlockImpl().getConnection() == null) {
                continue;
            }

            for (Connection connection : comp.getBlockImpl().getConnection()) {
                // if a connection does not
                if ((connection.getSource().getSubcomponentPort() == null
                                && connection.getSource().getComponentPort() == null)
                        || (connection.getDestination().getSubcomponentPort() == null
                                && connection.getDestination().getComponentPort() == null)) {
                    continue;
                }

                table.addValue(scenario); // scenario
                table.addValue(connection.getName()); // connection name

                // Source port can be either a subcomponent port or a component port
                if (connection.getSource().getSubcomponentPort() != null) {
                    table.addValue(
                            getStrNullChk(
                                    () ->
                                            connection
                                                    .getSource()
                                                    .getSubcomponentPort()
                                                    .getSubcomponent()
                                                    .getSpecification()
                                                    .getName())); // src comp
                    table.addValue(
                            getStrNullChk(
                                    () ->
                                            connection
                                                    .getSource()
                                                    .getSubcomponentPort()
                                                    .getSubcomponent()
                                                    .getImplementation()
                                                    .getName())); // src comp impl

                    table.addValue(
                            getStrNullChk(
                                    () ->
                                            connection
                                                    .getSource()
                                                    .getSubcomponentPort()
                                                    .getSubcomponent()
                                                    .getName())); // src comp instance

                } else if (connection.getSource().getComponentPort() != null) {
                    table.addValue(comp.getType().getName()); // src comp
                    table.addValue(comp.getName()); // src comp impl
                    table.addValue(""); // src comp instance
                }

                // Destination port can be either a subcomponent port or a component port
                if (connection.getDestination().getSubcomponentPort() != null) {
                    table.addValue(
                            getStrNullChk(
                                    () ->
                                            connection
                                                    .getDestination()
                                                    .getSubcomponentPort()
                                                    .getSubcomponent()
                                                    .getSpecification()
                                                    .getName())); // dest comp

                    table.addValue(
                            getStrNullChk(
                                    () ->
                                            connection
                                                    .getDestination()
                                                    .getSubcomponentPort()
                                                    .getSubcomponent()
                                                    .getImplementation()
                                                    .getName())); // dest comp impl
                    table.addValue(
                            getStrNullChk(
                                    () ->
                                            connection
                                                    .getDestination()
                                                    .getSubcomponentPort()
                                                    .getSubcomponent()
                                                    .getName())); // dest comp instance
                } else if (connection.getDestination().getComponentPort() != null) {
                    table.addValue(comp.getType().getName()); // dest comp type

                    table.addValue(comp.getName()); // dest comp impl
                    table.addValue(""); // dest comp instance
                }

                String flow = connection.getFlowType().value();

                table.addValue("xdata".equals(flow.toLowerCase()) ? "Xdata" : ""); // flow1
                table.addValue("xcontrol".equals(flow.toLowerCase()) ? "Xcontrol" : ""); // flow2
                table.addValue("xrequest".equals(flow.toLowerCase()) ? "Xrequest" : ""); // flow3
                // add the value for trustedConnection
                if (connection.isTrustedConnection() != null) {
                    if (connection.isTrustedConnection()) {
                        table.addValue("1");
                    } else {
                        table.addValue("0");
                    }
                } else {
                    table.addValue("");
                }

                // add the value for encryptedTransmission
                if (connection.isEncryptedTransmission() != null) {
                    if (connection.isEncryptedTransmission()) {
                        if (connection.getEncryptedTransmissionDAL() != null) {
                            table.addValue(
                                    "1#"
                                            + String.valueOf(
                                                    connection.getEncryptedTransmissionDAL()));
                        }
                    } else {
                        table.addValue("0");
                    }
                } else {
                    table.addValue("");
                }
                table.capRow();
            }
        }

        return table;
    }

    /**
     * Build the scenario architecture table.
     *
     * <p>Lists the properties associated with each connection.
     *
     * @param model
     * @param scenario
     * @return
     */
    private Table buildScnArchTable(Model model, String scenario) {
        Table table =
                new Table(
                        "Scenario",
                        "ConnectionName",
                        "SrcComp",
                        "SrcImpl", // added
                        "SrcCompInstance", // added
                        "SrcCompCategory",
                        "SrcPortName",
                        "SrcPortType", // added
                        "DestComp",
                        "DestImpl", // added
                        "DestCompInstance", // added
                        "DestCompCategory",
                        "DestPortName",
                        "DestPortType" // added
                        //                        "Flow1",
                        //                        "Flow2",
                        //                        "Flow3"

                        /* ,
                         * "FlowType"
                         */ );
        // 14 columns

        for (ComponentImpl comp : model.getComponentImpl()) {
            // if a component implementation does have connections, we continue
            if (comp.getBlockImpl() == null || comp.getBlockImpl().getConnection() == null) {
                continue;
            }

            for (Connection connection : comp.getBlockImpl().getConnection()) {
                // if a connection does not
                if ((connection.getSource().getSubcomponentPort() == null
                                && connection.getSource().getComponentPort() == null)
                        || (connection.getDestination().getSubcomponentPort() == null
                                && connection.getDestination().getComponentPort() == null)) {
                    continue;
                }

                table.addValue(scenario); // scenario
                table.addValue(connection.getName()); // connection name

                // Source port can be either a subcomponent port or a component port
                if (connection.getSource().getSubcomponentPort() != null) {
                    table.addValue(
                            getStrNullChk(
                                    () ->
                                            connection
                                                    .getSource()
                                                    .getSubcomponentPort()
                                                    .getSubcomponent()
                                                    .getSpecification()
                                                    .getName())); // src comp
                    table.addValue(
                            getStrNullChk(
                                    () ->
                                            connection
                                                    .getSource()
                                                    .getSubcomponentPort()
                                                    .getSubcomponent()
                                                    .getImplementation()
                                                    .getName())); // src comp impl

                    table.addValue(
                            getStrNullChk(
                                    () ->
                                            connection
                                                    .getSource()
                                                    .getSubcomponentPort()
                                                    .getSubcomponent()
                                                    .getName())); // src comp instance
                    table.addValue(""); // src comp category
                    table.addValue(
                            getStrNullChk(
                                    () ->
                                            connection
                                                    .getSource()
                                                    .getSubcomponentPort()
                                                    .getPort()
                                                    .getName())); // src port name
                    table.addValue(
                            connection
                                    .getSource()
                                    .getSubcomponentPort()
                                    .getPort()
                                    .getMode()
                                    .value()); // src port mode: in or out
                } else if (connection.getSource().getComponentPort() != null) {
                    table.addValue(comp.getType().getName()); // src comp type
                    table.addValue(comp.getName()); // src comp impl
                    table.addValue(""); // src comp instance
                    table.addValue(""); // src comp category
                    table.addValue(
                            connection.getSource().getComponentPort().getName()); // src port name
                    table.addValue(
                            connection
                                    .getSource()
                                    .getComponentPort()
                                    .getMode()
                                    .value()); // src port mode: in or out
                }

                // Destination port can be either a subcomponent port or a component port
                if (connection.getDestination().getSubcomponentPort() != null) {
                    table.addValue(
                            getStrNullChk(
                                    () ->
                                            connection
                                                    .getDestination()
                                                    .getSubcomponentPort()
                                                    .getSubcomponent()
                                                    .getSpecification()
                                                    .getName())); // dest comp

                    table.addValue(
                            getStrNullChk(
                                    () ->
                                            connection
                                                    .getDestination()
                                                    .getSubcomponentPort()
                                                    .getSubcomponent()
                                                    .getImplementation()
                                                    .getName())); // dest comp impl
                    table.addValue(
                            getStrNullChk(
                                    () ->
                                            connection
                                                    .getDestination()
                                                    .getSubcomponentPort()
                                                    .getSubcomponent()
                                                    .getName())); // dest comp instance
                    table.addValue(""); // dest comp category
                    table.addValue(
                            getStrNullChk(
                                    () ->
                                            connection
                                                    .getDestination()
                                                    .getSubcomponentPort()
                                                    .getPort()
                                                    .getName())); // dest port name
                    table.addValue(
                            connection
                                    .getDestination()
                                    .getSubcomponentPort()
                                    .getPort()
                                    .getMode()
                                    .value()); // dest port mode: in or out
                } else if (connection.getDestination().getComponentPort() != null) {
                    table.addValue(comp.getType().getName()); // dest comp type

                    table.addValue(comp.getName()); // dest comp impl
                    table.addValue(""); // dest comp instance
                    table.addValue(""); // dest comp category
                    table.addValue(
                            connection
                                    .getDestination()
                                    .getComponentPort()
                                    .getName()); // dest port name
                    table.addValue(
                            connection
                                    .getDestination()
                                    .getComponentPort()
                                    .getMode()
                                    .value()); // dest port mode: in or out
                }
                table.capRow();
            }
        }

        return table;
    }

    /**
     * Capitalize the first letter of a String.
     *
     * @param str the input string
     * @return the input string with the first letter capitalized
     */
    private String capitalizeFirstLetter(String str) {
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * Invokes a method (using reflection) on an object. Handles failure cases.
     *
     * <p>Contains an unchecked cast (due to type erasure), so the caller is responsible for
     * guaranteeing the return type of the method.
     *
     * @param <T> return type
     * @param obj object on which to invoke the method
     * @param method method to invoke
     * @param defaultVal default value in case invocation fails
     * @return the output value of the invocation, or the default value in case of failure
     */
    @SuppressWarnings("unchecked")
    private <T> T invokeMethod(Object obj, Method method, T defaultVal) {
        try {
            if (method != null) {
                Object val = method.invoke(obj);
                if (val != null) {
                    return (T) val;
                }
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
        }

        return defaultVal;
    }

    /**
     * Build the scenario component properties table.
     *
     * <p>Lists the properties associated with each component.
     *
     * @param model
     * @param scenario
     * @return
     */
    private Table buildScnCompPropsTable(Model model, String scenario) {
        String[] mainHeaders = {"Scenario", "Comp", "Impl", "CompInstance"}; // compImpl added

        /*
         * The set of properties can be modified simply by changing the array "props".
         * All values and DALs are obtained through reflection from these property names.
         * 9 already supported properties
         *
         */
        String[] props = {
            // The columns below are to be filled with 1 for true, nothing for false
            // 10 props
            "canReceiveConfigUpdate",
            "canReceiveSWUpdate",
            "hasSensitiveInfo",
            "insideTrustedBoundary",
            "componentKind", // enum type
            "pedigree", // enum type
            "controlReceivedFromUntrusted",
            "dataReceivedFromUntrusted",
            "controlSentToUntrusted",
            "dataSentToUntrusted",

            // 42 props with DAL The columns below are to be filled with 1#N,
            // where 1 is for true, # is the separator, and N is the DAL number
            "antiJamming",
            //            "antiJammingDAL",
            "auditMessageResponses",
            //            "auditMessageResponsesDAL",
            "deviceAuthentication",
            //            "deviceAuthenticationDAL",
            "dosProtection",
            //            "dosProtectionDAL",
            "encryptedStorage",
            //            "encryptedStorageDAL",
            "heterogeneity",
            //            "heterogeneityDAL",
            "inputValidation",
            //            "inputValidationDAL",
            "logging",
            //            "loggingDAL",
            "memoryProtection",
            //            "memoryProtectionDAL",
            "physicalAccessControl",
            //            "physicalAccessControlDAL",
            "removeIdentifyingInformation",
            //            "removeIdentifyingInformationDAL",
            "resourceAvailability",
            //            "resourceAvailabilityDAL",
            "resourceIsolation",
            //            "resourceIsolationDAL",
            "secureBoot",
            //            "secureBootDAL",
            "sessionAuthenticity",
            //            "sessionAuthenticityDAL",
            "staticCodeAnalysis",
            //            "staticCodeAnalysisDAL",
            "strongCryptoAlgorithms",
            //            "strongCryptoAlgorithmsDAL",
            "supplyChainSecurity",
            //            "supplyChainSecurityDAL",
            "systemAccessControl",
            //            "systemAccessControlDAL",
            "tamperProtection",
            //            "tamperProtectionDAL",
            "userAuthentication",
            //            "userAuthenticationDAL",

            // 12 Cyber Attack Properties from TA1
            "Configuration_Attack",
            "Physical_Theft_Attack",
            "Interception_Attack",
            "Hardware_Integrity_Attack",
            "Supply_Chain_Attack",
            "Brute_Force_Attack",
            "Fault_Injection_Attack",
            "Identity_Spoofing_Attack",
            "Excessive_Allocation_Attack",
            "Sniffing_Attack",
            "Buffer_Attack",
            "Flooding_Attack"

            //            "broadcastFromOutsideTB",
            //            "wifiFromOutsideTB",
            //            "encryption",
            //            "antiFlooding",
            //            "antiFuzzing"
        };

        // Methods for determining property values (true or false)
        Method[] isPropMethods = new Method[props.length];
        // Methods for determining property DALs (0-9)
        Method[] getDalMethods = new Method[props.length];
        // Methods for getting the property enum value
        //        Method[] getPropMethods = new Method[props.length];

        // Find methods for all properties
        Class<ComponentInstance> cls = ComponentInstance.class;
        for (int i = 0; i < props.length; i++) {
            String propName = props[i];
            // Find isXxx method
            try {
                Method method = cls.getDeclaredMethod("is" + capitalizeFirstLetter(propName));
                // Check return type
                if (Boolean.class.equals(method.getReturnType())) {
                    isPropMethods[i] = method;
                }
            } catch (Exception e) {
            }
            // Find getXxxDal method
            try {
                Method method =
                        cls.getDeclaredMethod("get" + capitalizeFirstLetter(propName) + "DAL");
                // Check return type
                if (Integer.class.equals(method.getReturnType())) {
                    getDalMethods[i] = method;
                }
            } catch (Exception e) {
            }
        }

        // The two arrays get merged together into one set of columns
        Table table = new Table(mainHeaders, props);

        Integer dal;
        String isProp;

        for (ComponentImpl comp : model.getComponentImpl()) {
            if (comp.getBlockImpl() == null || comp.getBlockImpl().getSubcomponent() == null) {
                continue;
            }

            for (ComponentInstance inst : comp.getBlockImpl().getSubcomponent()) {
                if (inst.getSpecification() == null) {
                    continue;
                }

                table.addValue(scenario); // scenario

                table.addValue(getStrNullChk(() -> inst.getSpecification().getName())); // comp type
                table.addValue(
                        getStrNullChk(() -> inst.getImplementation().getName())); // // comp impl

                table.addValue(inst.getName()); // comp instance

                for (int i = 0; i < props.length; i++) {
                    // Perform reflection. invokeMethod() handles most of the nasty bits.
                    // We checked the return types of the methods above, so this is type-safe.
                    isProp = null;
                    dal = invokeMethod(inst, getDalMethods[i], -1);
                    if (invokeMethod(inst, isPropMethods[i], null) != null) {
                        isProp = invokeMethod(inst, isPropMethods[i], false) ? "1" : "0";
                    }

                    if (!Integer.valueOf(-1).equals(dal)) {
                        // DAL is provided for this property and it is set in this particular case
                        // (not null)
                        // In practice this should only be true when the property is present (output
                        // "1")
                        table.addValue(isProp + "#" + dal.toString());
                    } else if (isProp != null) {
                        // No DAL information available
                        table.addValue(isProp);
                    } else if (props[i].equals("pedigree")) {
                        if (inst.getPedigree() != null) {
                            table.addValue(inst.getPedigree().value());
                        } else {
                            table.addValue("");
                        }
                    } else if (props[i].equals("componentKind")) {
                        if (inst.getComponentKind() != null) {
                            table.addValue(inst.getComponentKind().value());
                        } else {
                            table.addValue("");
                        }
                    } else {
                        table.addValue("");
                    }
                }

                table.capRow();
            }
        }

        return table;
    }

    /**
     * Build the scenario component table.
     *
     * <p>Lists component types with parent information and the number of instances.
     *
     * @param model
     * @param scenario
     * @return
     */
    private Table buildScnCompTable(Model model, String scenario) {
        Table table = new Table("Scenario", "Parent", "CompType", "Cardinality");

        // Count component instances
        Map<String, Integer> compCounts = new HashMap<>();
        if (model.getComponentImpl() != null) {
            for (ComponentImpl comp : model.getComponentImpl()) {
                if (comp.getBlockImpl() == null || comp.getBlockImpl().getSubcomponent() == null) {
                    continue;
                }
                for (ComponentInstance inst : comp.getBlockImpl().getSubcomponent()) {
                    if (inst.getSpecification() == null
                            || inst.getSpecification().getName() == null) {
                        continue;
                    }
                    // Found an instance
                    String key = inst.getSpecification().getName();
                    compCounts.put(key, compCounts.getOrDefault(key, 0) + 1);
                }
            }
        }

        for (ComponentImpl comp : model.getComponentImpl()) {
            if (comp.getBlockImpl() == null) {
                continue;
            }

            for (ComponentInstance inst : comp.getBlockImpl().getSubcomponent()) {
                if (inst.getSpecification() == null || inst.getSpecification().getName() == null) {
                    continue;
                }

                table.addValue(scenario); // scenario
                table.addValue(comp.getType().getName()); // parent
                table.addValue(inst.getSpecification().getName()); // comp type
                // Get number of component instances counted above
                table.addValue(
                        Integer.toString(
                                compCounts.get(inst.getSpecification().getName()))); // cardinality

                table.capRow();
            }
        }

        return table;
    }

    /**
     * Get a list of all CIAPorts in a given expression MBAA only supports a disjunctions of
     * conjunctions.
     *
     * <p>MBAS does not currently support arbitrary logical expressions, it only supports OR. For
     * the time being, we simply find all ports in the input expression and "or" them together.
     *
     * <p>TODO update MBAS to support arbitrary expressions
     *
     * @param expr
     * @param ports
     */
    private void extractCIAPorts(CyberExpr expr, List<List<CIAPort>> ports) {
        if (expr == null) {
            return;
        }

        // Note: the kind field is not currently being set properly
        // The only case we will see an expr without any operator is the expr itself
        if (expr.getPort() != null) {
            List<CIAPort> andPorts = new ArrayList<>();
            andPorts.add(expr.getPort());
            ports.add(andPorts);
        } else if (expr.getOr() != null) {
            for (CyberExpr or : expr.getOr().getExpr()) {
                extractCIAPorts(or, ports);
            }
        } else if (expr.getAnd() != null) {
            // Terminate when we get to an AND expr, because of limitations of Soteria_pp
            List<CIAPort> andPorts = new ArrayList<>();
            for (CyberExpr and : expr.getAnd().getExpr()) {
                if (and.getPort() != null) {
                    andPorts.add(and.getPort());
                } else {
                    errAndExit(
                            "MBAA only supports a dijunction of conjunctions of ports' CIA in cyber relatioins! Something unexpected!");
                }
            }
            ports.add(andPorts);
            //            if (expr.getAnd().getExpr().size() > 1) {
            //                System.err.println("MBAS does not currently support AND expressions");
            //                throw new RuntimeException("AND not supported");
            //            } else {
            //                // An AND with a single child is used implicitly due to the structure
            // of the parser
            //                for (CyberExpr and : expr.getAnd().getExpr()) {
            //                    extractCIAPorts(and, ports);
            //                }
            //            }
        } else if (expr.getNot() != null) {
            System.err.println("Error: MBAS does not currently support NOT expressions");
            throw new RuntimeException("NOT not supported");
        } else {
            throw new RuntimeException("We don't supported other operator yet: " + expr.getKind());
        }
    }

    /**
     * Build the component dependency table.
     *
     * <p>Lists cyber relations.
     *
     * @param model
     * @return
     */
    private Table buildCompDepTable(Model model) {
        Table table = new Table("Comp", "InputPort", "InputCIA", "OutputPort", "OutputCIA");

        for (ComponentType type : model.getComponentType()) {
            if (type.getCyberRel() == null) {
                continue;
            }

            for (CyberRel rel : type.getCyberRel()) {
                List<List<CIAPort>> inputPorts = new ArrayList<>();
                extractCIAPorts(rel.getInputs(), inputPorts);

                if (inputPorts.isEmpty()) {
                    // No input, i.e. always active
                    table.addValue(type.getName()); // comp type
                    table.addValue(""); // input port (empty)
                    table.addValue(""); // input CIA (empty)
                    table.addValue(rel.getOutput().getName()); // output port
                    table.addValue(rel.getOutput().getCia().value()); // output CIA

                    table.capRow();
                } else {
                    // Produce one rule for each port we find
                    // MBAS interprets each rule as OR-ed together
                    for (List<CIAPort> andPortList : inputPorts) {
                        table.addValue(type.getName()); // comp type
                        table.addValue(convertListOfPortNameToStr(andPortList)); // input ports
                        table.addValue(convertListOfPortCIAToStr(andPortList)); // input ports CIA
                        table.addValue(rel.getOutput().getName()); // output port
                        table.addValue(rel.getOutput().getCia().value()); // output CIA

                        table.capRow();
                    }
                }
            }
        }

        return table;
    }

    /**
     * Build the mission table.
     *
     * <p>Lists missions/cyber requirements.
     *
     * @param model
     * @param scenario
     * @return
     */
    private Table buildMissionTable(Model model, String scenario) {
        // 12 Headers
        Table table =
                new Table(
                        "ModelVersion",
                        "MissionReqId",
                        "MissionReq",
                        "CyberReqId",
                        "CyberReq",
                        "MissionImpactCIA",
                        "Effect",
                        "Severity",
                        "CompInstanceDependency",
                        "CompOutputDependency",
                        "DependentCompOutputCIA",
                        "ReqType");

        // Map output ports (the name in the system implementation
        // not necessarily the name in the component type) to component instance names
        Map<String, String> outportToDepComps = new HashMap<>();
        Map<String, String> outportToSrcCompPort = new HashMap<>();
        for (ComponentImpl compImpl : model.getComponentImpl()) {
            if (compImpl.getBlockImpl() == null
                    || compImpl.getBlockImpl().getConnection() == null) {
                continue;
            }
            for (Connection connection : compImpl.getBlockImpl().getConnection()) {
                if (connection.getSource().getSubcomponentPort() == null
                        || connection.getSource().getSubcomponentPort().getSubcomponent() == null) {
                    continue;
                }
                // Key is the destination port of a connection of some inner component or an outport
                // of the out-most component implementation
                String key;

                if (connection.getDestination().getSubcomponentPort() != null) {
                    key = connection.getDestination().getSubcomponentPort().getPort().getName();
                } else if (connection.getDestination().getComponentPort() != null) {
                    key = connection.getDestination().getComponentPort().getName();
                } else {
                    continue;
                }

                // outputPortComps: The value is source subcomponent name connecting to key
                //                  or the component implementation name
                // outputPortCompPorts: The value is source subcomponent port name connecting to key
                //                      or the outmost component implementation port name connecting
                // to key
                if (connection.getSource().getSubcomponentPort() != null) {
                    outportToDepComps.put(
                            key,
                            connection
                                    .getSource()
                                    .getSubcomponentPort()
                                    .getSubcomponent()
                                    .getName());
                    outportToSrcCompPort.put(
                            key, connection.getSource().getSubcomponentPort().getPort().getName());
                } else if (connection.getSource().getComponentPort() != null) {
                    outportToDepComps.put(key, compImpl.getName());
                    outportToSrcCompPort.put(
                            key, connection.getSource().getComponentPort().getName());
                } else {
                    continue;
                }
            }
        }

        // Collect the mappings between mission and reqs
        Map<String, Mission> missionMap = new HashMap<>();
        if (model.getMission() != null) {
            for (Mission mission : model.getMission()) {
                for (String reqId : mission.getCyberReqs()) {
                    missionMap.put(reqId, mission);
                }
            }
        }

        // Iterate over all the cyber reqs
        if (model.getCyberReq() != null) {
            for (CyberReq req : model.getCyberReq()) {
                List<List<CIAPort>> condPorts = new ArrayList<>();
                extractCIAPorts(req.getCondition(), condPorts);

                Mission mission = missionMap.get(req.getId());

                if (mission == null) {
                    System.out.println(
                            "Warning: CyberReq " + req.getId() + " does not belong to any mission");
                }

                for (List<CIAPort> andPortList : condPorts) {
                    table.addValue(scenario); // Scenario
                    table.addValue(getStrNullChk(() -> mission.getId())); // mission req ID
                    table.addValue(getStrNullChk(() -> mission.getName())); // mission req
                    table.addValue(req.getId()); // cyber req ID
                    table.addValue(getStrNullChk(() -> req.getName())); // cyber req
                    table.addValue(getStrNullChk(() -> req.getCia().value())); // mission impact CIA
                    table.addValue(""); // effect
                    table.addValue(req.getSeverity().value()); // Severity
                    // Get the name of the component with this output port, determined above
                    table.addValue(
                            convertCompOrSrcPortDepToStr(
                                    outportToDepComps,
                                    andPortList)); // comp instance dependency (one layer inwards)
                    table.addValue(
                            convertCompOrSrcPortDepToStr(
                                    outportToSrcCompPort,
                                    andPortList)); // comp output dependency (one layer inwards)

                    table.addValue(
                            convertListOfPortCIAToStr(
                                    andPortList)); // Dependent Component Output CIA
                    table.addValue("Cyber"); // cyber for req type
                    table.capRow();
                }
            }
        }

        // also need to iterate over SafetyReq

        return table;
    }

    /* Auxiliary functions*/

    @SafeVarargs
    private final String getStrNullChk(Supplier<String>... suppliers) {
        for (Supplier<String> supplier : suppliers) {
            try {
                String ret = supplier.get();
                if (ret != null) {
                    return ret;
                }
            } catch (NullPointerException e) {
            }
        }

        return "";
    }

    /**
     * Find outports' dependencies in conditions of a CyberReq or SafetyReq and convert the
     * dependencies to string with ";" to indicate AND
     */
    public String convertCompOrSrcPortDepToStr(
            Map<String, String> portToDepCompOrPortmap, List<CIAPort> andPortList) {
        StringBuilder sb = new StringBuilder("");
        List<String> compNames = new ArrayList<>();

        for (CIAPort port : andPortList) {
            String portName = port.getName();
            if (portToDepCompOrPortmap.containsKey(portName)) {
                compNames.add(portToDepCompOrPortmap.get(portName));
            }
        }
        for (int i = 0; i < compNames.size(); ++i) {
            sb.append(compNames.get(i));
            if (i < compNames.size() - 1) {
                sb.append(";");
            }
        }
        return sb.toString();
    }

    /** convert a list ports' names to a string with ";" to indicate AND */
    private String convertListOfPortNameToStr(List<CIAPort> andPortList) {
        StringBuilder sb = new StringBuilder("");
        for (int i = 0; i < andPortList.size(); i++) {
            sb.append(andPortList.get(i).getName());
            if (i < andPortList.size() - 1) {
                sb.append(";");
            }
        }
        return sb.toString();
    }

    /** convert a list ports' CIAs to a string with ";" to indicate AND */
    private String convertListOfPortCIAToStr(List<CIAPort> andPortList) {
        StringBuilder sb = new StringBuilder("");
        for (int i = 0; i < andPortList.size(); i++) {
            sb.append(andPortList.get(i).getCia().value());
            if (i < andPortList.size() - 1) {
                sb.append(";");
            }
        }
        return sb.toString();
    }

    private void errAndExit(String msg) {
        System.err.println("Error: " + msg);
        System.exit(-1);
    }
}
