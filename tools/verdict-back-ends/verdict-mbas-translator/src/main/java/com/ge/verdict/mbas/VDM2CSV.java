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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import verdict.vdm.vdm_model.CIA;
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
import verdict.vdm.vdm_model.Severity;

/** Convert parsed VDM XML to CSV files for input to MBAS (STEM and Soteria++). */
public class VDM2CSV extends VdmTranslator {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppClass.class);

    /**
     * Marshal a Verdict data model to Mbas files.
     *
     * <p>Produces the following files:
     *
     * <ul>
     *   <li>ScnArch.csv (STEM and Soteria++)
     *   <li>ScnCompProps.csv (STEM)
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
            LOGGER.error("Input path is not in the correct format. Scenario name is empty.");
        }

        Table scnArchTable = buildScnArchTable(model, scenario);
        Table scnCompPropsTable = buildScnCompPropsTable(model, scenario);
        Table scnCompTable = buildScnCompTable(model, scenario);
        Table compDepTable = buildCompDepTable(model);
        Table missionTable = buildMissionTable(model, scenario);

        // Replace all dots with underscores
        scnArchTable.setReplaceDots(true);
        scnCompPropsTable.setReplaceDots(true);
        scnCompTable.setReplaceDots(true);
        compDepTable.setReplaceDots(true);
        missionTable.setReplaceDots(true);

        scnArchTable.toCsvFile(new File(stemOutputPath, "ScnArch.csv"));
        scnArchTable.toCsvFile(new File(soteriaOutputPath, "ScnArch.csv"));
        scnCompPropsTable.toCsvFile(new File(stemOutputPath, "ScnCompProps.csv"));
        scnCompTable.toCsvFile(new File(soteriaOutputPath, "ScnComp.csv"));
        compDepTable.toCsvFile(new File(soteriaOutputPath, "CompDep.csv"));
        missionTable.toCsvFile(new File(soteriaOutputPath, "Mission.csv"));
    }

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
                        "SrcCompType",
                        //                        "SrcCompImpl", // added
                        "SrcCompInstance",
                        "SrcPortName",
                        "DestCompType",
                        //                        "DestCompImpl", // added
                        "DestCompInstance",
                        "DestPortName",
                        "Flow1",
                        "Flow2",
                        "Flow3" /*
                                 * ,
                                 * "FlowType"
                                 */);

        for (ComponentImpl comp : model.getComponentImpl()) {
            if (comp.getBlockImpl() == null || comp.getBlockImpl().getConnection() == null) {
                continue;
            }

            for (Connection connection : comp.getBlockImpl().getConnection()) {
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
                                                    .getName())); // src comp type
                    //                    table.addValue(
                    //                            getStrNullChk(
                    //                                    () ->
                    //                                            connection
                    //                                                    .getSource()
                    //                                                    .getSubcomponentPort()
                    //                                                    .getSubcomponent()
                    //                                                    .getImplementation()
                    //                                                    .getName())); // src comp
                    // impl
                    table.addValue(
                            getStrNullChk(
                                    () ->
                                            connection
                                                    .getSource()
                                                    .getSubcomponentPort()
                                                    .getSubcomponent()
                                                    .getName())); // src comp instance
                    table.addValue(
                            getStrNullChk(
                                    () ->
                                            connection
                                                    .getSource()
                                                    .getSubcomponentPort()
                                                    .getPort()
                                                    .getName())); // src port name
                } else if (connection.getSource().getComponentPort() != null) {
                    table.addValue(comp.getType().getName()); // src comp type
                    //                    table.addValue(comp.getName()); // src comp impl
                    table.addValue(""); // src comp instance
                    table.addValue(
                            connection.getSource().getComponentPort().getName()); // src port name
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
                                                    .getName())); // dest comp type
                    //                    table.addValue(
                    //                            getStrNullChk(
                    //                                    () ->
                    //                                            connection
                    //                                                    .getDestination()
                    //                                                    .getSubcomponentPort()
                    //                                                    .getSubcomponent()
                    //                                                    .getImplementation()
                    //                                                    .getName())); // dest comp
                    // impl
                    table.addValue(
                            getStrNullChk(
                                    () ->
                                            connection
                                                    .getDestination()
                                                    .getSubcomponentPort()
                                                    .getSubcomponent()
                                                    .getName())); // dest comp instance
                    table.addValue(
                            getStrNullChk(
                                    () ->
                                            connection
                                                    .getDestination()
                                                    .getSubcomponentPort()
                                                    .getPort()
                                                    .getName())); // dest port name
                } else if (connection.getDestination().getComponentPort() != null) {
                    table.addValue(comp.getType().getName()); // dest comp type
                    //                    table.addValue(comp.getName()); // dest comp impl
                    table.addValue(""); // dest comp instance
                    table.addValue(
                            connection
                                    .getDestination()
                                    .getComponentPort()
                                    .getName()); // dest port name
                }

                // TODO update MBAS to consolidate these into one column
                String flow = connection.getFlow().name();

                table.addValue("XDATA".equals(flow) ? "Xdata" : ""); // flow1
                table.addValue("CONTROL".equals(flow) ? "Control" : ""); // flow2
                table.addValue("DATA".equals(flow) ? "Data" : ""); // flow3

                //                table.addValue(capitalizeFirstLetter(flow.toLowerCase()));

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
        String[] mainHeaders = {
            "Scenario", "CompType" /* , "CompImpl" */, "CompInstance"
        }; // compImpl added

        /*
         * The set of properties can be modified simply by changing the array "props".
         * All values and DALs are obtained through reflection from these property names.
         */
        String[] props = {
            "hasSensitiveInfo",
            "insideTrustedBoundary",
            "broadcastFromOutsideTB",
            "wifiFromOutsideTB",
            "heterogeneity",
            "encryption",
            "antiJamming",
            "antiFlooding",
            "antiFuzzing"
        };

        // Methods for determining property values (true or false)
        Method[] isPropMethods = new Method[props.length];
        // Methods for determining property DALs (0-9)
        Method[] getDalMethods = new Method[props.length];

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
                        cls.getDeclaredMethod("get" + capitalizeFirstLetter(propName) + "Dal");
                // Check return type
                if (Integer.class.equals(method.getReturnType())) {
                    getDalMethods[i] = method;
                }
            } catch (Exception e) {
            }
        }

        // The two arrays get merged together into one set of columns
        Table table = new Table(mainHeaders, props);

        String isProp;
        Integer dal;

        for (ComponentImpl comp : model.getComponentImpl()) {
            if (comp.getBlockImpl() == null || comp.getBlockImpl().getSubcomponent() == null) {
                continue;
            }

            for (ComponentInstance inst : comp.getBlockImpl().getSubcomponent()) {
                if (inst.getSpecification() == null) {
                    continue;
                }

                table.addValue(scenario); // scenario
                //                table.addValue(
                //                        getStrNullChk(() -> inst.getImplementation().getName()));
                // // comp impl
                table.addValue(getStrNullChk(() -> inst.getSpecification().getName())); // comp type
                table.addValue(inst.getName()); // comp instance

                for (int i = 0; i < props.length; i++) {
                    // Perform reflection. invokeMethod() handles most of the nasty bits.
                    // We checked the return types of the methods above, so this is type-safe.

                    isProp = invokeMethod(inst, isPropMethods[i], false) ? "1" : "";
                    dal = invokeMethod(inst, getDalMethods[i], -1);

                    if (!Integer.valueOf(-1).equals(dal)) {
                        // DAL is provided for this property and it is set in this particular case
                        // (not null)
                        // In practice this should only be true when the property is present (output
                        // "1")
                        table.addValue(isProp + "#" + dal.toString());
                    } else {
                        // No DAL information available
                        table.addValue(isProp);
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
     * Get a list of all CIAPorts in a given expression, excluding those nested within AND or NOT.
     *
     * <p>MBAS does not currently support arbitrary logical expressions, it only supports OR. For
     * the time being, we simply find all ports in the input expression and "or" them together.
     *
     * <p>TODO update MBAS to support arbitrary expressions
     *
     * @param expr
     * @param ports
     */
    private void extractCIAPorts(CyberExpr expr, List<CIAPort> ports) {
        if (expr == null) {
            return;
        }

        // Note: the kind field is not currently being set properly

        if (expr.getPort() != null) {
            ports.add(expr.getPort());
        } else if (expr.getOr() != null) {
            for (CyberExpr or : expr.getOr().getExpr()) {
                extractCIAPorts(or, ports);
            }
        } else if (expr.getAnd() != null) {
            if (expr.getAnd().getExpr().size() > 1) {
                System.err.println("MBAS does not currently support AND expressions");
                throw new RuntimeException("AND not supported");
            } else {
                // An AND with a single child is used implicitly due to the structure of the parser
                for (CyberExpr and : expr.getAnd().getExpr()) {
                    extractCIAPorts(and, ports);
                }
            }
        } else if (expr.getNot() != null) {
            System.err.println("MBAS does not currently support NOT expressions");
            throw new RuntimeException("NOT not supported");
        }
    }

    /**
     * Convert CIA to string.
     *
     * @param cia
     * @return
     */
    private String ciaToString(CIA cia) {
        switch (cia) {
            case CONFIDENTIALITY:
                return "Confidentiality";
            case INTEGRITY:
                return "Integrity";
            case AVAILABILITY:
                return "Availability";
            default:
                // This shouldn't happen?
                throw new RuntimeException("Unknown CIA: " + cia.toString());
        }
    }

    /**
     * Convert Severity to String.
     *
     * @param severity
     * @return
     */
    private String severityToString(Severity severity) {
        switch (severity) {
            case CATASTROPHIC:
                return "Catastrophic";
            case HAZARDOUS:
                return "Hazardous";
            case MAJOR:
                return "Major";
            case MINOR:
                return "Minor";
            case NONE:
                return "None";
            default:
                // This shouldn't happen?
                throw new RuntimeException("Unknown severity: " + severity.toString());
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
        Table table = new Table("CompType", "InputPort", "InputCIA", "OutputPort", "OutputCIA");

        for (ComponentType type : model.getComponentType()) {
            if (type.getCyberRel() == null) {
                continue;
            }

            for (CyberRel rel : type.getCyberRel()) {
                List<CIAPort> inputPorts = new ArrayList<>();
                extractCIAPorts(rel.getInputs(), inputPorts);

                if (inputPorts.isEmpty()) {
                    // No input, i.e. always active
                    table.addValue(type.getName()); // comp type
                    table.addValue(""); // input port (empty)
                    table.addValue(""); // input CIA (empty)
                    table.addValue(rel.getOutput().getName()); // output port
                    table.addValue(ciaToString(rel.getOutput().getCia())); // output CIA

                    table.capRow();
                } else {
                    // Produce one rule for each port we find
                    // MBAS interprets each rule as OR-ed together
                    for (CIAPort port : inputPorts) {
                        table.addValue(type.getName()); // comp type
                        table.addValue(port.getName()); // input port
                        table.addValue(ciaToString(port.getCia())); // input CIA
                        table.addValue(rel.getOutput().getName()); // output port
                        table.addValue(ciaToString(rel.getOutput().getCia())); // output CIA

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
                        "Confidentiality",
                        "Integrity",
                        "Availability" /*
                                        * ,
                                        * "CIA"
                                        */);

        // Map output ports (the name in the system implementation
        // not necessarily the name in the component type) to component instance names
        Map<String, String> outputPortComps = new HashMap<>();
        Map<String, String> outputPortCompPorts = new HashMap<>();
        for (ComponentImpl comp : model.getComponentImpl()) {
            if (comp.getBlockImpl() == null || comp.getBlockImpl().getConnection() == null) {
                continue;
            }
            for (Connection connection : comp.getBlockImpl().getConnection()) {
                if (connection.getSource().getSubcomponentPort() == null
                        || connection.getSource().getSubcomponentPort().getSubcomponent() == null) {
                    continue;
                }

                String key;

                if (connection.getDestination().getSubcomponentPort() != null) {
                    key = connection.getDestination().getSubcomponentPort().getPort().getName();
                } else if (connection.getDestination().getComponentPort() != null) {
                    key = connection.getDestination().getComponentPort().getName();
                } else {
                    continue;
                }

                if (connection.getSource().getSubcomponentPort() != null) {
                    outputPortComps.put(
                            key,
                            connection
                                    .getSource()
                                    .getSubcomponentPort()
                                    .getSubcomponent()
                                    .getName());
                    outputPortCompPorts.put(
                            key, connection.getSource().getSubcomponentPort().getPort().getName());
                } else if (connection.getSource().getComponentPort() != null) {
                    outputPortComps.put(key, comp.getName());
                    outputPortCompPorts.put(
                            key, connection.getSource().getComponentPort().getName());
                } else {
                    continue;
                }
            }
        }

        Map<String, Mission> missionMap = new HashMap<>();
        if (model.getMission() != null) {
            for (Mission mission : model.getMission()) {
                for (String reqId : mission.getCyberReqs()) {
                    missionMap.put(reqId, mission);
                }
            }
        }

        if (model.getCyberReq() != null) {
            for (CyberReq req : model.getCyberReq()) {
                List<CIAPort> condPorts = new ArrayList<>();
                extractCIAPorts(req.getCondition(), condPorts);

                Mission mission = missionMap.get(req.getId());

                if (mission == null) {
                    System.out.println(
                            "Warning: CyberReq " + req.getId() + " does not belong to any mission");
                }

                for (CIAPort port : condPorts) {
                    table.addValue(scenario);
                    table.addValue(getStrNullChk(() -> mission.getId())); // mission req ID
                    table.addValue(getStrNullChk(() -> mission.getName())); // mission req
                    table.addValue(req.getId()); // cyber req ID
                    table.addValue(getStrNullChk(() -> req.getName())); // cyber req
                    table.addValue(
                            getStrNullChk(() -> ciaToString(req.getCia()))); // mission impact CIA
                    table.addValue(""); // effect
                    table.addValue(severityToString(req.getSeverity()));
                    // Get the name of the component with this output port, determined above
                    table.addValue(
                            outputPortComps.getOrDefault(
                                    port.getName(), "")); // comp instance dependency
                    table.addValue(
                            outputPortCompPorts.getOrDefault(
                                    port.getName(), "")); // comp output dependency

                    // TODO change MBAS to consolidate into one column
                    table.addValue(
                            CIA.CONFIDENTIALITY.equals(port.getCia()) ? "Confidentiality" : "");
                    table.addValue(CIA.INTEGRITY.equals(port.getCia()) ? "Integrity" : "");
                    table.addValue(CIA.AVAILABILITY.equals(port.getCia()) ? "Availability" : "");

                    //                    table.addValue(ciaToString(port.getCia()));

                    table.capRow();
                }
            }
        }

        return table;
    }
}
