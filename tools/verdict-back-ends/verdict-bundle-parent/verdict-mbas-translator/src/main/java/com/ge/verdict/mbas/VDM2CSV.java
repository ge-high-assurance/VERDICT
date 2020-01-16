/* See LICENSE in project directory */
package com.ge.verdict.mbas;

import com.ge.verdict.vdm.VdmTranslator;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import verdict.vdm.vdm_model.CIAPort;
import verdict.vdm.vdm_model.CompInstancePort;
import verdict.vdm.vdm_model.ComponentImpl;
import verdict.vdm.vdm_model.ComponentInstance;
import verdict.vdm.vdm_model.ComponentType;
import verdict.vdm.vdm_model.Connection;
import verdict.vdm.vdm_model.CyberExpr;
import verdict.vdm.vdm_model.CyberRel;
import verdict.vdm.vdm_model.CyberReq;
import verdict.vdm.vdm_model.Event;
import verdict.vdm.vdm_model.EventHappens;
import verdict.vdm.vdm_model.IAPort;
import verdict.vdm.vdm_model.Mission;
import verdict.vdm.vdm_model.Model;
import verdict.vdm.vdm_model.SafetyRel;
import verdict.vdm.vdm_model.SafetyRelExpr;
import verdict.vdm.vdm_model.SafetyReq;
import verdict.vdm.vdm_model.SafetyReqExpr;

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

        Table scnConnectionsTable = buildScnConnectionsTable(model, scenario);
        Table scnCompPropsTable = buildScnCompPropsTable(model, scenario);
        Table compDepTable = buildCompDepTable(model);
        Table missionTable = buildMissionTable(model, scenario);
        // New CSV files
        Table scnConnectionPropsTable = buildScnConnectionPropsTable(model, scenario);
        Table compSafTable = buildCompSafTable(model, scenario);
        Table eventsTable = buildEventsTable(model, scenario);

        // Generate STEM input
        scnConnectionsTable.toCsvFile(new File(stemOutputPath, "ScnArch.csv"));
        scnCompPropsTable.toCsvFile(new File(stemOutputPath, "ScnCompProps.csv"));
        scnConnectionPropsTable.toCsvFile(new File(stemOutputPath, "ScnConnectionProps.csv"));

        // Generate Soteria_pp input
        scnConnectionsTable.toCsvFile(new File(soteriaOutputPath, "ScnArch.csv"));
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
        Table table =
                new Table("Comp", "InputPortOrEvent", "InputIAOrEvent", "OutputPort", "OutputIA");
        for (ComponentType comp : model.getComponentType()) {
            for (SafetyRel safeRel : comp.getSafetyRel()) {
                List<List<Object>> allPortsEvents = new ArrayList<>();
                extractIAPortsAndEvents(safeRel.getFaultSrc(), allPortsEvents);

                for (int i = 0; i < allPortsEvents.size(); ++i) {
                    table.addValue(comp.getName()); // comp
                    table.addValue(
                            convertPortsAndEventsToStr(allPortsEvents.get(i))); // InputPortOrEvent
                    table.addValue(
                            convertPortsIAAndEventHappensToStr(
                                    allPortsEvents.get(i))); // InputIAOrEvent
                    table.addValue(safeRel.getOutput().getName()); // OutputPort
                    table.addValue(safeRel.getOutput().getIa().value()); // OutputIA
                    table.capRow();
                }
            }
        }

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
        for (ComponentType comp : model.getComponentType()) {
            for (Event e : comp.getEvent()) {
                table.addValue(getStrNullChk(() -> comp.getName())); // comp
                table.addValue(getStrNullChk(() -> e.getId()));
                table.addValue(getStrNullChk(() -> e.getProbability()));
                table.capRow();
            }
        }

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
                            getCompTypeName(
                                    connection
                                            .getSource()
                                            .getSubcomponentPort()
                                            .getSubcomponent())); // src comp type
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
                            connection
                                    .getSource()
                                    .getSubcomponentPort()
                                    .getSubcomponent()
                                    .getName()); // src comp instance

                } else if (connection.getSource().getComponentPort() != null) {
                    table.addValue(comp.getType().getName()); // src comp
                    table.addValue(comp.getName()); // src comp impl
                    table.addValue(""); // src comp instance
                }

                // Destination port can be either a subcomponent port or a component port
                if (connection.getDestination().getSubcomponentPort() != null) {
                    table.addValue(
                            getCompTypeName(
                                    connection
                                            .getDestination()
                                            .getSubcomponentPort()
                                            .getSubcomponent())); // dest comp type

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
                            connection
                                    .getDestination()
                                    .getSubcomponentPort()
                                    .getSubcomponent()
                                    .getName()); // dest comp instance
                } else if (connection.getDestination().getComponentPort() != null) {
                    table.addValue(comp.getType().getName()); // dest comp type

                    table.addValue(comp.getName()); // dest comp impl
                    table.addValue(""); // dest comp instance
                }

                // Fill in the flow type information
                String flowType =
                        connection.getFlowType() != null ? connection.getFlowType().value() : "";

                table.addValue("xdata".equals(flowType.toLowerCase()) ? "Xdata" : ""); // flow1
                table.addValue(
                        "xcontrol".equals(flowType.toLowerCase()) ? "Xcontrol" : ""); // flow2
                table.addValue(
                        "xrequest".equals(flowType.toLowerCase()) ? "Xrequest" : ""); // flow3

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
                    boolean isEncrypted = connection.isEncryptedTransmission();
                    if (connection.getEncryptedTransmissionDAL() != null) {
                        String dal = String.valueOf(connection.getEncryptedTransmissionDAL());
                        table.addValue(isEncrypted ? ("1#" + dal) : ("0#" + dal));
                    } else {
                        table.addValue(isEncrypted ? "1#0" : "0#0");
                    }
                } else if (connection.getEncryptedTransmissionDAL() != null) {
                    table.addValue(
                            "null#" + String.valueOf(connection.getEncryptedTransmissionDAL()));
                } else {
                    table.addValue("");
                }

                table.capRow();
            }
        }

        return table;
    }

    private String getCompTypeName(ComponentInstance ci) {
        String compTypeName = "";
        if (ci.getSpecification() != null) {
            compTypeName = ci.getSpecification().getName();
        } else if (ci.getImplementation() != null) {
            compTypeName = ci.getImplementation().getType().getName();
        } else {
            errAndExit(
                    "Cannot reach here, something is wrong with component instance declaratioins!");
        }
        return compTypeName;
    }

    private ComponentType getCompType(ComponentInstance ci) {
        ComponentType compType = null;
        if (ci.getSpecification() != null) {
            compType = ci.getSpecification();
        } else if (ci.getImplementation() != null) {
            compType = ci.getImplementation().getType();
        } else {
            errAndExit(
                    "Cannot reach here, something is wrong with component instance declaratioins!");
        }
        return compType;
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
    private Table buildScnConnectionsTable(Model model, String scenario) {
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
                        "DestPortType",
                        "Flow1",
                        "Flow2",
                        "Flow3",
                        "trustedConnection",
                        "encryptedTransmission");
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
                    CompInstancePort srcCip = connection.getSource().getSubcomponentPort();

                    table.addValue(getCompTypeName(srcCip.getSubcomponent())); // src comp type
                    table.addValue(
                            getStrNullChk(
                                    () ->
                                            srcCip.getSubcomponent()
                                                    .getImplementation()
                                                    .getName())); // src comp impl
                    table.addValue(srcCip.getSubcomponent().getName()); // src comp instance
                    table.addValue(
                            getStrNullChk(
                                    () ->
                                            getCompType(srcCip.getSubcomponent())
                                                    .getCompCateg())); // src comp category
                    table.addValue(
                            getStrNullChk(() -> srcCip.getPort().getName())); // src port name
                    table.addValue(srcCip.getPort().getMode().value()); // src port mode: in or out
                } else if (connection.getSource().getComponentPort() != null) {
                    table.addValue(comp.getType().getName()); // src comp type
                    table.addValue(comp.getName()); // src comp impl
                    table.addValue(""); // src comp instance
                    table.addValue(
                            getStrNullChk(
                                    () -> comp.getType().getCompCateg())); // src comp category
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
                    CompInstancePort destCip = connection.getDestination().getSubcomponentPort();

                    table.addValue(getCompTypeName(destCip.getSubcomponent())); // dest comp type
                    table.addValue(
                            getStrNullChk(
                                    () ->
                                            destCip.getSubcomponent()
                                                    .getImplementation()
                                                    .getName())); // dest comp impl
                    table.addValue(destCip.getSubcomponent().getName()); // dest comp instance
                    table.addValue(
                            getStrNullChk(
                                    () ->
                                            getCompType(destCip.getSubcomponent())
                                                    .getCompCateg())); // dest comp category
                    table.addValue(
                            getStrNullChk(() -> destCip.getPort().getName())); // dest port name
                    table.addValue(
                            destCip.getPort().getMode().value()); // dest port mode: in or out
                } else if (connection.getDestination().getComponentPort() != null) {
                    table.addValue(comp.getType().getName()); // dest comp type

                    table.addValue(comp.getName()); // dest comp impl
                    table.addValue(""); // dest comp instance
                    table.addValue(
                            getStrNullChk(
                                    () -> comp.getType().getCompCateg())); // dest comp category
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

                // Fill in the flow type information
                String flowType =
                        connection.getFlowType() != null ? connection.getFlowType().value() : "";

                table.addValue("xdata".equals(flowType.toLowerCase()) ? "Xdata" : ""); // flow1
                table.addValue(
                        "xcontrol".equals(flowType.toLowerCase()) ? "Xcontrol" : ""); // flow2
                table.addValue(
                        "xrequest".equals(flowType.toLowerCase()) ? "Xrequest" : ""); // flow3

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
                    boolean isEncrypted = connection.isEncryptedTransmission();
                    if (connection.getEncryptedTransmissionDAL() != null) {
                        String dal = String.valueOf(connection.getEncryptedTransmissionDAL());
                        table.addValue(isEncrypted ? ("1#" + dal) : ("0#" + dal));
                    } else {
                        table.addValue(isEncrypted ? "1#0" : "0#0");
                    }
                } else if (connection.getEncryptedTransmissionDAL() != null) {
                    table.addValue(
                            "null#" + String.valueOf(connection.getEncryptedTransmissionDAL()));
                } else {
                    table.addValue("");
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

    private String replaceUnderscoreWithSpace(String str) {
        return str.replaceAll("_", "");
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
            "componentType", // enum type
            "pedigree", // enum type
            "controlReceivedFromUntrusted",
            "dataReceivedFromUntrusted",
            "controlSentToUntrusted",
            "dataSentToUntrusted",

            // 21 props with DAL The columns below are to be filled with 1#N,
            // where 1 is for true, # is the separator, and N is the DAL number
            "antiJamming",
            "auditMessageResponses",
            "deviceAuthentication",
            "dosProtection",
            "encryptedStorage",
            "heterogeneity",
            "inputValidation",
            "logging",
            "memoryProtection",
            "physicalAccessControl",
            "removeIdentifyingInformation",
            "resourceAvailability",
            "resourceIsolation",
            "secureBoot",
            "sessionAuthenticity",
            "staticCodeAnalysis",
            "strongCryptoAlgorithms",
            "supplyChainSecurity",
            "systemAccessControl",
            "tamperProtection",
            "userAuthentication",

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
                Method method =
                        cls.getDeclaredMethod(
                                "is" + replaceUnderscoreWithSpace(capitalizeFirstLetter(propName)));
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
                //                if (inst.getSpecification() == null) {
                //                    continue;
                //                }

                table.addValue(scenario); // scenario

                table.addValue(getCompTypeName(inst)); // comp
                table.addValue(
                        getStrNullChk(() -> inst.getImplementation().getName())); // // comp impl

                table.addValue(inst.getName()); // comp instance

                for (int i = 0; i < props.length; i++) {
                    // Perform reflection. invokeMethod() handles most of the nasty bits.
                    // We checked the return types of the methods above, so this is type-safe.
                    isProp = null; // this null value is used later
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
                        // No DAL information available but need to check if a property
                        // is a DAL property or not to accomadate Abha's needs
                        if (isDALProp(props[i])) {
                            table.addValue(isProp + "#0");
                        } else {
                            table.addValue(isProp);
                        }
                    } else if (props[i].equals("pedigree")) {
                        if (inst.getPedigree() != null) {
                            table.addValue(inst.getPedigree().value());
                        } else {
                            table.addValue("");
                        }
                    } else if (props[i].equals("componentType")) {
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

    private boolean isDALProp(String prop) {
        List<String> dalProps =
                Arrays.asList(
                        "antiJamming",
                        "auditMessageResponses",
                        "deviceAuthentication",
                        "dosProtection",
                        "encryptedStorage",
                        "heterogeneity",
                        "inputValidation",
                        "logging",
                        "memoryProtection",
                        "physicalAccessControl",
                        "removeIdentifyingInformation",
                        "resourceAvailability",
                        "resourceIsolation",
                        "secureBoot",
                        "sessionAuthenticity",
                        "staticCodeAnalysis",
                        "strongCryptoAlgorithms",
                        "supplyChainSecurity",
                        "systemAccessControl",
                        "tamperProtection",
                        "userAuthentication");
        return dalProps.contains(prop);
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
        } else if (expr.getNot() != null) {
            System.err.println("Error: MBAS does not currently support NOT expressions");
            throw new RuntimeException("NOT not supported");
        } else {
            throw new RuntimeException("We don't supported other operator yet: " + expr.getKind());
        }
    }

    private void extractReqExprIAPorts(SafetyReqExpr expr, List<List<IAPort>> ports) {
        if (expr == null) {
            return;
        }

        // Note: the kind field is not currently being set properly
        // The only case we will see an expr without any operator is the expr itself
        if (expr.getPort() != null) {
            List<IAPort> andPorts = new ArrayList<>();
            andPorts.add(expr.getPort());
            ports.add(andPorts);
        } else if (expr.getOr() != null) {
            for (SafetyReqExpr or : expr.getOr().getExpr()) {
                extractReqExprIAPorts(or, ports);
            }
        } else if (expr.getAnd() != null) {
            // Terminate when we get to an AND expr, because of limitations of Soteria_pp
            List<IAPort> andPorts = new ArrayList<>();
            for (SafetyReqExpr and : expr.getAnd().getExpr()) {
                if (and.getPort() != null) {
                    andPorts.add(and.getPort());
                } else {
                    errAndExit(
                            "MBAA only supports a dijunction of conjunctions of ports' CIA in cyber relatioins! Something unexpected!");
                }
            }
            ports.add(andPorts);
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
                        "ReqId",
                        "Req",
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

        // Allow us to look up requirements by name
        Map<String, CyberReq> cyberReqMap = new HashMap<>();
        Map<String, SafetyReq> safetyReqMap = new HashMap<>();
        if (model.getCyberReq() != null) {
            for (CyberReq req : model.getCyberReq()) {
                cyberReqMap.put(req.getId(), req);
            }
        }
        if (model.getSafetyReq() != null) {
            for (SafetyReq req : model.getSafetyReq()) {
                safetyReqMap.put(req.getId(), req);
            }
        }

        // We only care about requirements that are part of a mission
        if (model.getMission() != null) {
            for (Mission mission : model.getMission()) {
                for (String reqId : mission.getCyberReqs()) {
                    if (cyberReqMap.containsKey(reqId)) {
                        CyberReq req = cyberReqMap.get(reqId);

                        List<List<CIAPort>> condPorts = new ArrayList<>();
                        extractCIAPorts(req.getCondition(), condPorts);

                        for (List<CIAPort> andPortList : condPorts) {
                            table.addValue(scenario); // Scenario
                            table.addValue(getStrNullChk(() -> mission.getId())); // mission req ID
                            table.addValue(getStrNullChk(() -> mission.getName())); // mission req
                            table.addValue(req.getId()); // cyber req ID
                            table.addValue(getStrNullChk(() -> req.getName())); // cyber req
                            table.addValue(
                                    getStrNullChk(
                                            () -> req.getCia().value())); // mission impact CIA
                            table.addValue(""); // effect
                            table.addValue(req.getSeverity().value()); // Severity
                            // Get the name of the component with this output port, determined above
                            table.addValue(
                                    convertCompOrSrcPortDepToStr(
                                            outportToDepComps,
                                            andPortList)); // comp instance dependency (one layer
                            // inwards)
                            table.addValue(
                                    convertCompOrSrcPortDepToStr(
                                            outportToSrcCompPort,
                                            andPortList)); // comp output dependency (one layer
                            // inwards)

                            table.addValue(
                                    convertListOfPortCIAToStr(
                                            andPortList)); // Dependent Component Output CIA
                            table.addValue("Cyber"); // cyber for req type
                            table.capRow();
                        }
                    } else if (safetyReqMap.containsKey(reqId)) {
                        SafetyReq req = safetyReqMap.get(reqId);

                        List<List<IAPort>> condPorts = new ArrayList<>();
                        extractReqExprIAPorts(req.getCondition(), condPorts);

                        for (List<IAPort> andPortList : condPorts) {
                            table.addValue(scenario); // Scenario
                            table.addValue(getStrNullChk(() -> mission.getId())); // mission req ID
                            table.addValue(getStrNullChk(() -> mission.getName())); // mission req
                            table.addValue(req.getId()); // cyber req ID
                            table.addValue(getStrNullChk(() -> req.getName())); // cyber req
                            table.addValue(""); // mission impact CIA
                            table.addValue(""); // effect
                            table.addValue(req.getTargetProbability()); // Severity
                            // Get the name of the component with this output port, determined above
                            table.addValue(
                                    convertSafetyCompOrSrcPortDepToStr(
                                            outportToDepComps,
                                            andPortList)); // comp instance dependency (one layer
                            // inwards)
                            table.addValue(
                                    convertSafetyCompOrSrcPortDepToStr(
                                            outportToSrcCompPort,
                                            andPortList)); // comp output dependency (one layer
                            // inwards)

                            table.addValue(
                                    convertListOfPortIAToStr(
                                            andPortList)); // Dependent Component Output CIA
                            table.addValue("Safety"); // cyber for req type
                            table.capRow();
                        }
                    } else {
                        errAndExit(
                                "Missing requirement \""
                                        + reqId
                                        + "\" defined in mission "
                                        + mission.getId());
                    }
                }
            }
        }

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

    public String convertSafetyCompOrSrcPortDepToStr(
            Map<String, String> portToDepCompOrPortmap, List<IAPort> andPortList) {
        StringBuilder sb = new StringBuilder("");
        List<String> compNames = new ArrayList<>();

        for (IAPort port : andPortList) {
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

    private String convertListOfPortIAToStr(List<IAPort> andPortList) {
        StringBuilder sb = new StringBuilder("");
        for (int i = 0; i < andPortList.size(); i++) {
            sb.append(andPortList.get(i).getIa().value());
            if (i < andPortList.size() - 1) {
                sb.append(";");
            }
        }
        return sb.toString();
    }

    /** Convert a list of ports and events names to a string with ";" to indicate "AND" */
    private String convertPortsAndEventsToStr(List<Object> portsEvents) {
        StringBuilder sb = new StringBuilder("");
        for (int i = 0; i < portsEvents.size(); i++) {
            Object obj = portsEvents.get(i);

            if (obj instanceof IAPort) {
                sb.append(((IAPort) obj).getName());
            } else if (obj instanceof EventHappens) {
                sb.append(((EventHappens) obj).getEventName());
            } else {
                errAndExit("Unexpected!");
            }
            if (i < portsEvents.size() - 1) {
                sb.append(";");
            }
        }
        return sb.toString();
    }

    /** Convert a list of ports' IA and events to a string with ";" to indicate "AND" */
    private String convertPortsIAAndEventHappensToStr(List<Object> portsEvents) {
        StringBuilder sb = new StringBuilder("");
        for (int i = 0; i < portsEvents.size(); i++) {
            Object obj = portsEvents.get(i);

            if (obj instanceof IAPort) {
                sb.append(((IAPort) obj).getIa().value());
            } else if (obj instanceof EventHappens) {
                sb.append("happens");
            } else {
                errAndExit("Unexpected!");
            }
            if (i < portsEvents.size() - 1) {
                sb.append(";");
            }
        }
        return sb.toString();
    }

    /**
     * Get a list of all IAPorts and evetns in a given expression. MBAA only supports a disjunctions
     * of conjunctions.
     *
     * <p>MBAS does not currently support arbitrary logical expressions, it only supports OR. For
     * the time being, we simply find all ports in the input expression and "or" them together.
     *
     * <p>TODO update MBAS to support arbitrary expressions
     *
     * @param expr
     * @param allPorts
     * @param allEvents
     */
    private void extractIAPortsAndEvents(SafetyRelExpr expr, List<List<Object>> allPortsEvents) {
        if (expr == null) {
            return;
        }

        // Note: the kind field is not currently being set properly
        // The only case we will see an expr without any operator is the expr itself
        if (expr.getPort() != null) {
            List<Object> ports = new ArrayList<>();
            ports.add(expr.getPort());
            allPortsEvents.add(ports);
        } else if (expr.getFault() != null) {
            List<Object> events = new ArrayList<>();
            events.add(expr.getFault());
            allPortsEvents.add(events);
        } else if (expr.getOr() != null) {
            for (SafetyRelExpr or : expr.getOr().getExpr()) {
                extractIAPortsAndEvents(or, allPortsEvents);
            }
        } else if (expr.getAnd() != null) {
            // Terminate when we get to an AND expr, because of limitations of Soteria_pp
            List<Object> portsEvents = new ArrayList<>();

            for (SafetyRelExpr andExpr : expr.getAnd().getExpr()) {
                if (andExpr.getPort() != null) {
                    portsEvents.add(andExpr.getPort());
                } else if (andExpr.getFault() != null) {
                    portsEvents.add(andExpr.getFault());
                } else {
                    errAndExit(
                            "MBAA only supports a dijunction of conjunctions of ports' CIA in cyber relatioins! Something unexpected!");
                }
            }
            allPortsEvents.add(portsEvents);
        } else if (expr.getNot() != null) {
            System.err.println("Error: MBAS does not currently support NOT expressions");
            throw new RuntimeException("NOT not supported");
        } else {
            throw new RuntimeException("We don't supported other operator yet: " + expr.getKind());
        }
    }

    private void errAndExit(String msg) {
        System.err.println("Error: " + msg);
        System.exit(-1);
    }
}
