package com.ge.verdict.mbaas.synthesis;

import com.ge.verdict.mbaas.synthesis.model.CIA;
import com.ge.verdict.mbaas.synthesis.model.ConnectionModel;
import com.ge.verdict.mbaas.synthesis.model.CyberAnd;
import com.ge.verdict.mbaas.synthesis.model.CyberExpr;
import com.ge.verdict.mbaas.synthesis.model.CyberOr;
import com.ge.verdict.mbaas.synthesis.model.CyberRel;
import com.ge.verdict.mbaas.synthesis.model.CyberReq;
import com.ge.verdict.mbaas.synthesis.model.PortConcern;
import com.ge.verdict.mbaas.synthesis.model.SystemModel;
import com.ge.verdict.vdm.DefenseProperties;
import com.ge.verdict.vdm.VdmTranslator;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import verdict.vdm.vdm_data.GenericAttribute;
import verdict.vdm.vdm_model.CIAPort;
import verdict.vdm.vdm_model.ComponentImpl;
import verdict.vdm.vdm_model.ComponentInstance;
import verdict.vdm.vdm_model.ComponentType;
import verdict.vdm.vdm_model.Connection;
import verdict.vdm.vdm_model.Mission;
import verdict.vdm.vdm_model.Model;
import verdict.vdm.vdm_model.Severity;

public class ADTreeConstructor {

    /** System models. */
    private Map<String, SystemModel> sysNameToSystemModelMap;
    /** Connection models. */
    private Map<String, Set<ConnectionModel>> connNameToConnModelMap;

    /**
     * Keep track of all defense property implemented DALs. We use this because we fool STEM, so it
     * doesn't give us the correct implemented DAL information, and because we want to report the
     * extraneous implented defense properties, which are the implemented DALS that appear hear but
     * not in the attack-defense tree. This is used when loading from VDM, but it is kept null when
     * loading from CSV.
     */
    private Map<Pair<String, String>, Integer> entityDefenseNamePairToImplDal;

    /**
     * Get the system model with the specified name, creating it and adding it to the resolution
     * table if necessary.
     *
     * <p>Using only this function to access system models guarantees that there is only ever one
     * system model with a given name.
     *
     * @param name the name of the system model
     * @return the system model with the specified name
     */
    private SystemModel getSystemModelByName(String name) {
        if (!sysNameToSystemModelMap.containsKey(name)) {
            sysNameToSystemModelMap.put(name, new SystemModel(name));
        }
        return sysNameToSystemModelMap.get(name);
    }

    /**
     * Loads the model from the specified VDM file, and the attacks/defenses from the specified STEM
     * output directory. Requires the following STEM files:
     *
     * <ul>
     *   <li>CAPEC.csv
     *   <li>Defenses.csv
     * </ul>
     *
     * @param vdm the input VDM file
     * @param stemOutputDir the STEM output directory
     * @param inference whether or not to infer cyber relations in systems with no cyber relations
     * @throws CSVFile.MalformedInputException
     * @throws IOException
     */
    public void AttackDefenseCollector(File vdm, File stemOutputDir, boolean inference)
            throws IOException {
        Model vdmModel = VdmTranslator.unmarshalFromXml(vdm);

        sysNameToSystemModelMap = new LinkedHashMap<>();
        connNameToConnModelMap = new LinkedHashMap<>();
        entityDefenseNamePairToImplDal = new LinkedHashMap<>();

        // Keep track of component instances associated with each component type and impl
        Map<String, Set<SystemModel>> compTypeNameToSubInstSysModelSet = new HashMap<>();
        Map<String, Set<SystemModel>> compImplNameToSubInstSysModelSet = new HashMap<>();

        // For some reason the connection names in CAPEC.csv and Defenses.csv are confusing, so make
        // a map from the confusing names to the correct names
        Map<String, String> connectionAttackNames = new HashMap<>();

        // Load all subcomponent instances as system models
        for (ComponentImpl impl : vdmModel.getComponentImpl()) {
            if (impl.getBlockImpl() != null) {
                for (ComponentInstance subcompInst : impl.getBlockImpl().getSubcomponent()) {
                    // TODO: Change the getName() to getQualifiedName() later after changing VDM
                    SystemModel instModel = getSystemModelByName(subcompInst.getName());

                    for (GenericAttribute attrib : subcompInst.getAttribute()) {
                        // TODO: Be careful here about types of AADL properties
                        if (attrib.getValue() instanceof String) {
                            instModel.addAttribute(attrib.getName(), (String) attrib.getValue());
                        }
                    }
                    // Map AADL component type or implementation name to the internal system object
                    if (subcompInst.getSpecification() != null) {
                        Util.putSetMap(
                                compTypeNameToSubInstSysModelSet,
                                subcompInst.getSpecification().getName(),
                                instModel);
                    }
                    if (subcompInst.getImplementation() != null) {
                        Util.putSetMap(
                                compImplNameToSubInstSysModelSet,
                                subcompInst.getImplementation().getName(),
                                instModel);
                    }
                }
            }
        }

        // Load top-level system implementations that don't exist as instances as system models
        for (ComponentImpl impl : vdmModel.getComponentImpl()) {
            if (!compImplNameToSubInstSysModelSet.containsKey(impl.getName())) {
                SystemModel sysImplModel = getSystemModelByName(impl.getName());

                if (impl.getType() != null) {
                    Util.putSetMap(
                            compTypeNameToSubInstSysModelSet,
                            impl.getType().getName(),
                            sysImplModel);
                }
                Util.putSetMap(compImplNameToSubInstSysModelSet, impl.getName(), sysImplModel);
                sysImplModel.setImpl(true);
            }
        }

        // Load connections
        for (ComponentImpl impl : vdmModel.getComponentImpl()) {
            if (impl.getBlockImpl() != null) {
                for (Connection conn : impl.getBlockImpl().getConnection()) {
                    boolean isInternalIncoming = conn.getSource().getSubcomponentPort() == null;
                    boolean isInternalOutgoing =
                            conn.getDestination().getSubcomponentPort() == null;

                    String srcPortName, destPortName;
                    if (isInternalIncoming) {
                        srcPortName = conn.getSource().getComponentPort().getName();
                    } else {
                        if (conn.getSource().getSubcomponentPort().getPort() != null) {
                            srcPortName =
                                    conn.getSource().getSubcomponentPort().getPort().getName();
                        } else {
                            System.out.println("Null in port: " + conn.getName());
                            srcPortName = "null";
                        }
                    }
                    if (isInternalOutgoing) {
                        destPortName = conn.getDestination().getComponentPort().getName();
                    } else {
                        if (conn.getDestination().getSubcomponentPort().getPort() != null) {
                            destPortName =
                                    conn.getDestination().getSubcomponentPort().getPort().getName();
                        } else {
                            System.out.println("Null out port: " + conn.getName());
                            destPortName = "null";
                        }
                    }

                    Collection<SystemModel> connSrcSysModel =
                            isInternalIncoming
                                    ? compImplNameToSubInstSysModelSet.get(impl.getName())
                                    : Collections.singleton(
                                            getSystemModelByName(
                                                    conn.getSource()
                                                            .getSubcomponentPort()
                                                            .getSubcomponent()
                                                            .getName()));
                    Collection<SystemModel> connDestSysModel =
                            isInternalOutgoing
                                    ? compImplNameToSubInstSysModelSet.get(impl.getName())
                                    : Collections.singleton(
                                            getSystemModelByName(
                                                    conn.getDestination()
                                                            .getSubcomponentPort()
                                                            .getSubcomponent()
                                                            .getName()));

                    for (SystemModel srcSysModel : connSrcSysModel) {
                        for (SystemModel destSysModel : connDestSysModel) {
                            ConnectionModel connModel =
                                    new ConnectionModel(
                                            conn.getName(),
                                            srcSysModel,
                                            destSysModel,
                                            srcPortName,
                                            destPortName);

                            // Add attributes to the connection model
                            for (GenericAttribute attrib : conn.getAttribute()) {
                                if (attrib.getValue() instanceof String) {
                                    connModel.addAttribute(
                                            attrib.getName(), (String) attrib.getValue());
                                }
                            }

                            Util.putSetMap(connNameToConnModelMap, conn.getName(), connModel);

                            // Store connection in a different place depending on internal/external
                            // and outgoing/incoming
                            if (isInternalIncoming) {
                                srcSysModel.addInternalIncomingConnection(connModel);
                                destSysModel.addIncomingConnection(connModel);
                            } else if (isInternalOutgoing) {
                                srcSysModel.addOutgoingConnection(connModel);
                                destSysModel.addInternalOutgoingConnection(connModel);
                            } else {
                                srcSysModel.addOutgoingConnection(connModel);
                                destSysModel.addIncomingConnection(connModel);
                            }
                        }
                    }

                    // This is the way that connection names are stored in CAPEC.csv and
                    // Defenses.csv
                    connectionAttackNames.put(
                            conn.getName() + impl.getName() + impl.getType().getName(),
                            conn.getName());
                }
            }
        }

        // Load cyber relations
        for (ComponentType compType : vdmModel.getComponentType()) {
            for (verdict.vdm.vdm_model.CyberRel rel : compType.getCyberRel()) {
                for (SystemModel sysModel :
                        compTypeNameToSubInstSysModelSet.get(compType.getName())) {
                    if (rel.getInputs() != null) {
                        sysModel.addCyberRel(
                                new CyberRel(
                                        rel.getId(),
                                        convertCyberExpr(rel.getInputs()),
                                        convertCIAPort(rel.getOutput())));
                    } else {
                        // no input
                        sysModel.addCyberRel(
                                new CyberRel(rel.getId(), convertCIAPort(rel.getOutput())));
                    }
                }
            }
        }

        // Load cyber requirements

        // these maps let us look up requirements by name
        Map<String, verdict.vdm.vdm_model.CyberReq> cyberReqMap = new HashMap<>();
        Map<String, verdict.vdm.vdm_model.SafetyReq> safetyReqMap = new HashMap<>();

        for (verdict.vdm.vdm_model.CyberReq req : vdmModel.getCyberReq()) {
            cyberReqMap.put(req.getId(), req);
        }

        for (verdict.vdm.vdm_model.SafetyReq req : vdmModel.getSafetyReq()) {
            safetyReqMap.put(req.getId(), req);
        }

        // load from missions and cyber/safety reqs
        for (Mission mission : vdmModel.getMission()) {
            for (String reqName : mission.getCyberReqs()) {
                if (cyberReqMap.containsKey(reqName)) {
                    verdict.vdm.vdm_model.CyberReq req = cyberReqMap.get(reqName);
                    for (SystemModel system :
                            compTypeNameToSubInstSysModelSet.get(req.getCompType())) {
                        system.addCyberReq(
                                new CyberReq(
                                        req.getId(),
                                        mission.getId(),
                                        convertSeverity(req.getSeverity()),
                                        convertCyberExpr(req.getCondition())));
                    }
                } else if (safetyReqMap.containsKey(reqName)) {
                    //                    verdict.vdm.vdm_model.SafetyReq req =
                    // safetyReqMap.get(reqName);
                    // TODO support safety reqs
                } else {
                    throw new RuntimeException(
                            "Undefined cyber/safety requirement \""
                                    + reqName
                                    + "\" in mission \""
                                    + mission.getName()
                                    + "\"");
                }
            }
        }

        // load all implemented defense DALs from VDM
        // determine extraneous defenses later
        for (ComponentImpl impl : vdmModel.getComponentImpl()) {
            if (impl.getBlockImpl() != null) {
                // load defense properties on components
                for (ComponentInstance inst : impl.getBlockImpl().getSubcomponent()) {
                    for (GenericAttribute attrib : inst.getAttribute()) {
                        if (attrib.getValue() instanceof String) {
                            // chop qualifier (normally "CASE_Consolidated_Properties::")
                            int lastColon = attrib.getName().lastIndexOf(':');
                            String name =
                                    lastColon != -1
                                            ? attrib.getName().substring(lastColon + 1)
                                            : attrib.getName();

                            // only add property if it's a defense property, as opposed to, say,
                            // componentType
                            if (DefenseProperties.MBAA_COMP_DEFENSE_PROPERTIES_SET.contains(name)) {
                                try {
                                    Integer dal = Integer.parseInt((String) attrib.getValue());
                                    // only implemented if greater than zero
                                    if (dal > 0) {
                                        entityDefenseNamePairToImplDal.put(
                                                new Pair<>(inst.getName(), name), dal);
                                    }
                                } catch (NumberFormatException e) {
                                    throw new RuntimeException(
                                            "Invalid DAL for "
                                                    + impl.getName()
                                                    + " - "
                                                    + inst.getName()
                                                    + ":"
                                                    + name
                                                    + ", "
                                                    + attrib.getValue());
                                }
                            }
                        }
                    }
                }

                // load defense properties on connections
                for (Connection conn : impl.getBlockImpl().getConnection()) {
                    for (GenericAttribute attrib : conn.getAttribute()) {
                        if (attrib.getValue() instanceof String) {
                            // chop qualifier (normally "CASE_Consolidated_Properties::")
                            int lastColon = attrib.getName().lastIndexOf(':');
                            String name =
                                    lastColon != -1
                                            ? attrib.getName().substring(lastColon + 1)
                                            : attrib.getName();

                            // only add property if it's a defense property, as opposed to, say,
                            // connectionType
                            if (DefenseProperties.MBAA_CONN_DEFENSE_PROPERTIES_SET.contains(name)) {
                                try {
                                    Integer dal = Integer.parseInt((String) attrib.getValue());
                                    // only implemented if greater than zero
                                    if (dal > 0) {
                                        entityDefenseNamePairToImplDal.put(
                                                new Pair<>(conn.getName(), name), dal);
                                    }
                                } catch (NumberFormatException e) {
                                    throw new RuntimeException(
                                            "Invalid DAL for "
                                                    + impl.getName()
                                                    + " - "
                                                    + conn.getName()
                                                    + ":"
                                                    + name
                                                    + ", "
                                                    + attrib.getValue());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Make an attack-defense collector CyberExpr from a VDM CyberExpr.
     *
     * @param expr
     * @return
     */
    public static CyberExpr convertCyberExpr(verdict.vdm.vdm_model.CyberExpr expr) {
        if (expr.getAnd() != null) {
            return new CyberAnd(
                    expr.getAnd().getExpr().stream()
                            .map(ADTreeConstructor::convertCyberExpr)
                            .collect(Collectors.toList()));
        } else if (expr.getOr() != null) {
            return new CyberOr(
                    expr.getOr().getExpr().stream()
                            .map(ADTreeConstructor::convertCyberExpr)
                            .collect(Collectors.toList()));
        } else if (expr.getNot() != null) {
            throw new RuntimeException(
                    "A \"not cyber expression\" is not expected to be present in CyberExpression");
        } else if (expr.getPort() != null) {
            return convertCIAPort(expr.getPort());
        } else {
            throw new RuntimeException("impossible");
        }
    }

    /**
     * Make an attack-defense collector PortConcern from a VDM CIAPort.
     *
     * @param port
     * @return
     */
    public static PortConcern convertCIAPort(CIAPort port) {
        return new PortConcern(port.getName(), convertCIA(port.getCia()));
    }

    /**
     * Make an attack-defense collector CIA from a VDM CIA.
     *
     * @param cia
     * @return
     */
    public static CIA convertCIA(verdict.vdm.vdm_model.CIA cia) {
        switch (cia) {
            case CONFIDENTIALITY:
                return CIA.C;
            case INTEGRITY:
                return CIA.I;
            case AVAILABILITY:
                return CIA.A;
            default:
                throw new RuntimeException("impossible");
        }
    }

    /**
     * Get the equivalent DAL for a VDM severity.
     *
     * @param severity
     * @return
     */
    public static int convertSeverity(Severity severity) {
        switch (severity) {
            case NONE:
                return 0;
            case MINOR:
                return 3;
            case MAJOR:
                return 5;
            case HAZARDOUS:
                return 7;
            case CATASTROPHIC:
                return 9;
            default:
                throw new RuntimeException("impossible");
        }
    }
}
