package com.ge.verdict.attackdefensecollector;

import com.ge.verdict.attackdefensecollector.adtree.ADOr;
import com.ge.verdict.attackdefensecollector.adtree.ADTree;
import com.ge.verdict.attackdefensecollector.adtree.Attack;
import com.ge.verdict.attackdefensecollector.adtree.Defense;
import com.ge.verdict.attackdefensecollector.model.CIA;
import com.ge.verdict.attackdefensecollector.model.ConnectionModel;
import com.ge.verdict.attackdefensecollector.model.CyberAnd;
import com.ge.verdict.attackdefensecollector.model.CyberExpr;
import com.ge.verdict.attackdefensecollector.model.CyberNot;
import com.ge.verdict.attackdefensecollector.model.CyberOr;
import com.ge.verdict.attackdefensecollector.model.CyberRel;
import com.ge.verdict.attackdefensecollector.model.CyberReq;
import com.ge.verdict.attackdefensecollector.model.PortConcern;
import com.ge.verdict.attackdefensecollector.model.SystemModel;
import com.ge.verdict.vdm.DefenseProperties;
import com.ge.verdict.vdm.VdmTranslator;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

/**
 * Main class of the attack-defense collector implementation.
 *
 * <p>Load a model with one of the constructors, then invoke perform() to perform computations and
 * write output files.
 */
public class AttackDefenseCollector {
    /** System models. */
    private Map<String, SystemModel> sysNameToSystemModelMap;

    /** Connection models. */
    private Map<String, Set<ConnectionModel>> connNameToConnectionModelMap;

    /**
     * Keep track of all defense property implemented DALs. We use this because we fool STEM, so it
     * doesn't give us the correct implemented DAL information, and because we want to report the
     * extraneous implented defense properties, which are the implemented DALS that appear hear but
     * not in the attack-defense tree. This is used when loading from VDM, but it is kept null when
     * loading from CSV.
     */
    private Map<Pair<String, String>, Integer> compDefenseToImplDal;

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
    private SystemModel getSystem(String name) {
        if (!sysNameToSystemModelMap.containsKey(name)) {
            sysNameToSystemModelMap.put(name, new SystemModel(name));
        }
        return sysNameToSystemModelMap.get(name);
    }

    /**
     * Loads the model from the specified input directory in the CSV format. Specifically, requires
     * the following files:
     *
     * <ul>
     *   <li>CAPEC.csv
     *   <li>CompDep.csv
     *   <li>Defenses.csv
     *   <li>Mission.csv
     *   <li>ScnComp.csv
     * </ul>
     *
     * TODO take advantage of additional files added recently
     *
     * @param inputDir the directory from which to load the files
     * @param inference whether or not to infer cyber relations in systems with no cyber relations
     * @throws IOException if there was an IO exception while trying to read the files (or one or
     *     more file does not exist)
     * @throws CSVFile.MalformedInputException if a CSV file is malformed
     * @deprecated use the VDM constructor instead
     */
    @Deprecated
    public AttackDefenseCollector(String inputDir, boolean inference)
            throws IOException, CSVFile.MalformedInputException {
        /*
         * Important notes:
         *
         * We store systems by their "name". For component instances, this is the instance
         * name. But some component implementations don't have an instance (particularly
         * top-level systems), so we use the implementation name instead.
         *
         * We also keep track of which systems are associated with which component
         * types/implementations for things like cyber relations, which are defined
         * over component types, not instances or implementations. There is no explicit
         * mapping from component instances to component types (or vice versa), so we
         * instead use ScnComp.csv (the list of connections) to obtain this information
         * because it lists both the component types and instances (in the case that it
         * is an instance, rather than an implementation) for the connection end points.
         * This means that we don't know about any components that don't have connections.
         * In practice all components should have connections, but this is still something
         * to keep in mind.
         */

        sysNameToSystemModelMap = new LinkedHashMap<>();
        connNameToConnectionModelMap = new LinkedHashMap<>();

        compDefenseToImplDal = null;

        // Load all the files as CSV
        CSVFile compDepCsv =
                new CSVFile(
                        new File(inputDir, "CompDep.csv"),
                        "Comp",
                        "InputPort",
                        "InputCIA",
                        "OutputPort",
                        "OutputCIA");
        CSVFile missionCsv =
                new CSVFile(
                        new File(inputDir, "Mission.csv"),
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
        CSVFile scnConnectionsCsv =
                new CSVFile(
                        new File(inputDir, "ScnConnections.csv"),
                        "Scenario",
                        "Comp",
                        "Impl",
                        "ConnectionName",
                        "SrcComp",
                        "SrcImpl",
                        "SrcCompInstance",
                        "SrcCompCategory",
                        "SrcPortName",
                        "SrcPortType",
                        "DestComp",
                        "DestImpl",
                        "DestCompInstance",
                        "DestCompCategory",
                        "DestPortName",
                        "Trust_Level",
                        "Control_Level",
                        "Data_Classification");

        // Keep track of component instances associated with each component type
        Map<String, Set<SystemModel>> compTypeToSystem = new HashMap<>();
        // Do the same for component implementations
        Map<String, Set<SystemModel>> compImplToSystem = new HashMap<>();
        // Keep track of outgoing internal connections for use in correctly mapping cyber
        // requirements
        Map<Pair<SystemModel, String>, Pair<SystemModel, String>> outgoingConnectionMap =
                new HashMap<>();
        // For some reason the connection names in CAPEC.csv and Defenses.csv are confusing, so make
        // a map
        // from the confusing names to the correct names
        Map<String, String> connectionAttackNames = new HashMap<>();

        // Build component type and implementation maps
        for (CSVFile.RowData row : scnConnectionsCsv.getRowDatas()) {
            String sourceTypeName = row.getCell("SrcComp");
            String destTypeName = row.getCell("DestComp");
            String sourceImplName = row.getCell("SrcImpl");
            String destImplName = row.getCell("DestImpl");
            String sourceInstName = row.getCell("SrcCompInstance");
            String destInstName = row.getCell("DestCompInstance");

            boolean internalIncoming = sourceInstName.length() == 0;
            boolean internalOutgoing = destInstName.length() == 0;

            if (!internalIncoming) {
                SystemModel source = getSystem(sourceInstName);
                Util.putSetMap(compTypeToSystem, sourceTypeName, source);
                if (sourceImplName.length() != 0) {
                    Util.putSetMap(compImplToSystem, sourceImplName, source);
                }
            }
            if (!internalOutgoing) {
                SystemModel dest = getSystem(destInstName);
                Util.putSetMap(compTypeToSystem, destTypeName, dest);
                if (destImplName.length() != 0) {
                    Util.putSetMap(compImplToSystem, destImplName, dest);
                }
            }
        }

        // Top-level systems don't have instances... so we need to store them by
        // implementation names. But do this in a second pass so that we don't
        // only pick up component implementations that definitely don't have
        // instances.
        for (CSVFile.RowData row : scnConnectionsCsv.getRowDatas()) {
            String sourceImplName = row.getCell("SrcImpl");
            String destImplName = row.getCell("DestImpl");

            if (sourceImplName.length() != 0 && !compImplToSystem.containsKey(sourceImplName)) {
                Util.putSetMap(compImplToSystem, sourceImplName, getSystem(sourceImplName));
            }
            if (destImplName.length() != 0 && !compImplToSystem.containsKey(destImplName)) {
                Util.putSetMap(compImplToSystem, destImplName, getSystem(destImplName));
            }
        }

        // Load connections
        for (CSVFile.RowData row : scnConnectionsCsv.getRowDatas()) {
            String sourceTypeName = row.getCell("SrcComp");
            String destTypeName = row.getCell("DestComp");
            String sourceImplName = row.getCell("SrcImpl");
            String destImplName = row.getCell("DestImpl");
            String sourceInstName = row.getCell("SrcCompInstance");
            String destInstName = row.getCell("DestCompInstance");

            String name = row.getCell("ConnectionName");
            String sourcePort = row.getCell("SrcPortName");
            String destPort = row.getCell("DestPortName");

            connectionAttackNames.put(name + row.getCell("Impl") + row.getCell("Comp"), name);

            // If instance is empty, then that means we have a component implementation
            // (one example is the top-level system)
            boolean internalIncoming = sourceInstName.length() == 0;
            boolean internalOutgoing = destInstName.length() == 0;

            Collection<SystemModel> sources =
                    internalIncoming
                            ? compImplToSystem.get(sourceImplName)
                            : Collections.singleton(getSystem(sourceInstName));
            Collection<SystemModel> dests =
                    internalOutgoing
                            ? compImplToSystem.get(destImplName)
                            : Collections.singleton(getSystem(destInstName));

            if (sources == null) {
                throw new RuntimeException(
                        "Could not find component implementation: " + sourceImplName);
            } else if (dests == null) {
                throw new RuntimeException(
                        "Could not find component implementation: " + destImplName);
            }

            // this isn't actually quadratic because only one of the lists has more than one element
            for (SystemModel source : sources) {
                for (SystemModel dest : dests) {
                    ConnectionModel connection =
                            new ConnectionModel(name, source, dest, sourcePort, destPort);

                    /*
                     * Logger.println("loaded connection: " + name + " from (" + sourceTypeName + ", " + sourceImplName
                     * + ", " + sourceInstName + ") to (" + destTypeName + ", " + destImplName + ", "
                     * + destInstName + ")");
                     */

                    Util.putSetMap(connNameToConnectionModelMap, name, connection);

                    // Store connection in a different place depending on internal/external and
                    // outgoing/incoming
                    if (internalIncoming) {
                        source.addIncomingInternalConnection(connection);
                        dest.addIncomingConnection(connection);
                    } else if (internalOutgoing) {
                        source.addOutgoingConnection(connection);
                        dest.addOutgoingInternalConnection(connection);
                        outgoingConnectionMap.put(
                                new Pair<>(source, sourcePort), new Pair<>(dest, destPort));
                    } else {
                        source.addOutgoingConnection(connection);
                        dest.addIncomingConnection(connection);
                    }

                    // Associate these system models with the component types
                    Util.putSetMap(compTypeToSystem, sourceTypeName, source);
                    Util.putSetMap(compTypeToSystem, destTypeName, dest);
                    // and component implementations
                    Util.putSetMap(compImplToSystem, sourceImplName, source);
                    Util.putSetMap(compImplToSystem, destImplName, dest);
                    // Note that this might be redundant now but not 100% sure
                }
            }
        }

        // Load cyber relations
        for (CSVFile.RowData row : compDepCsv.getRowDatas()) {
            String systemTypeName = row.getCell("Comp");
            // Apply to all systems with this type
            for (SystemModel system : compTypeToSystem.get(systemTypeName)) {
                String outputPort = row.getCell("OutputPort");
                CIA outputCia = CIA.fromString(row.getCell("OutputCIA"));

                String inputPort = row.getCell("InputPort");

                // No-input (always-on) cyber relations are specified by empty input cells
                if (inputPort.length() > 0) {
                    CIA inputCia = CIA.fromString(row.getCell("InputCIA"));
                    system.addCyberRel(new CyberRel(inputPort, inputCia, outputPort, outputCia));
                } else {
                    system.addCyberRel(new CyberRel(outputPort, outputCia));
                }
            }
        }

        // Load cyber requirements
        for (CSVFile.RowData row : missionCsv.getRowDatas()) {
            // TODO support safety requirements
            if ("Cyber".equals(row.getCell("ReqType"))) {
                String missionId = row.getCell("MissionReqId");
                String cyberReqId = row.getCell("ReqId");
                int severityDal = Prob.dalOfSeverity(row.getCell("Severity"));
                SystemModel systemInternal = getSystem(row.getCell("CompInstanceDependency"));
                String portNameInternal = row.getCell("CompOutputDependency");
                // Look at all three columns to figure out which one is being used
                CIA portCia = CIA.fromString(row.getCell("DependentCompOutputCIA"));

                Pair<SystemModel, String> internal = new Pair<>(systemInternal, portNameInternal);
                Pair<SystemModel, String> systemPort;

                // The system/port are internal to the overall system
                // We follow the connection to convert back to the top-level system port
                if (outgoingConnectionMap.containsKey(internal)) {
                    systemPort = outgoingConnectionMap.get(internal);
                } else {
                    Logger.showWarning(
                            "Could not find outgoing internal connection from system: "
                                    + systemInternal.getName()
                                    + ", port: "
                                    + portNameInternal);
                    // We could proceed by analyzing the internal port, which is technically not
                    // wrong.
                    // But this connection should be present, and its ommission is an error.
                    // Furthermore, we need this connection information in order to to determine
                    // which
                    // system with which to associate the cyber requirement.
                    continue;
                }

                // Add to existing cyber requirement if it's already there
                Optional<CyberReq> opt = systemPort.left.getCyberReq(cyberReqId);
                if (opt.isPresent()) {
                    opt.get().addDisjunct(new PortConcern(systemPort.right, portCia));
                } else {
                    systemPort.left.addCyberReq(
                            new CyberReq(
                                    cyberReqId, missionId, severityDal, systemPort.right, portCia));
                }
            }
        }

        loadAttacksDefensesFromCsv(inputDir, connectionAttackNames);

        if (inference) {
            performInference();
        }
    }

    /**
     * Load the attacks and defenses from CAPEC.csv and Defenses.csv, respectively.
     *
     * <p>This is factored out because it is used by both the CSV and VDM approaches.
     *
     * @param inputDir the STEM output directory
     * @param connectionNameMap the map from connection names in the CSV files to the actual
     *     connection names
     * @throws CSVFile.MalformedInputException
     * @throws IOException
     */
    private void loadAttacksDefensesFromCsv(String inputDir, Map<String, String> connectionNameMap)
            throws CSVFile.MalformedInputException, IOException {
        // Load all the files as CSV
        CSVFile capecCsv =
                new CSVFile(
                        new File(inputDir, "CAPEC.csv"),
                        true,
                        "CompType",
                        "CompInst",
                        "CAPEC",
                        "CAPECDescription",
                        "Confidentiality",
                        "Integrity",
                        "Availability",
                        "LikelihoodOfSuccess");
        CSVFile defensesCsv =
                new CSVFile(
                        new File(inputDir, "Defenses.csv"),
                        true,
                        "CompType",
                        "CompInst",
                        "CAPEC",
                        "Confidentiality",
                        "Integrity",
                        "Availability",
                        "ApplicableDefenseProperties",
                        "ImplProperties",
                        "DAL");

        // Load attacks
        for (CSVFile.RowData row : capecCsv.getRowDatas()) {
            String systemTypeName = row.getCell("CompType");
            String systemInstName = row.getCell("CompInst");

            String attackName = row.getCell("CAPEC");
            String attackDesc = row.getCell("CAPECDescription");
            Prob likelihood = Prob.certain();
            // Look at all three columns to figure out which one is being used
            CIA cia =
                    CIA.fromStrings(
                            row.getCell("Confidentiality"),
                            row.getCell("Integrity"),
                            row.getCell("Availability"));

            if ("Connection".equals(systemTypeName)) {
                String connectionName = connectionNameMap.get(systemInstName);
                for (ConnectionModel connection :
                        connNameToConnectionModelMap.get(connectionName)) {
                    connection
                            .getAttackable()
                            .addAttack(
                                    new Attack(
                                            connection.getAttackable(),
                                            attackName,
                                            attackDesc,
                                            likelihood,
                                            cia));
                }
            } else {
                SystemModel system = getSystem(systemInstName);
                system.getAttackable()
                        .addAttack(
                                new Attack(
                                        system.getAttackable(),
                                        attackName,
                                        attackDesc,
                                        likelihood,
                                        cia));
            }
        }

        // Load defenses
        // Note we don't use implemented property column in csv files if we vdm as input
        for (CSVFile.RowData row : defensesCsv.getRowDatas()) {
            String systemTypeName = row.getCell("CompType");
            String systemInstName = row.getCell("CompInst");

            String attackName = row.getCell("CAPEC");
            CIA cia =
                    CIA.fromStrings(
                            row.getCell("Confidentiality"),
                            row.getCell("Integrity"),
                            row.getCell("Availability"));
            List<String> defenseNames =
                    Arrays.asList(row.getCell("ApplicableDefenseProperties").split(";")).stream()
                            .map(
                                    name ->
                                            name.length() > 0
                                                    ? Character.toString(name.charAt(0))
                                                                    .toLowerCase()
                                                            + name.substring(1)
                                                    : "")
                            .collect(Collectors.toList());
            List<String> implProps = Arrays.asList(row.getCell("ImplProperties").split(";"));
            List<String> likelihoodStrings = Arrays.asList(row.getCell("DAL").split(";"));

            if (defenseNames.size() != implProps.size()
                    || defenseNames.size() != likelihoodStrings.size()) {
                throw new RuntimeException(
                        "ApplicableDefenseProperties, ImplProperties, and DAL must have same cardinality");
            }

            List<Defense> defenses = new ArrayList<>();
            List<Defense.DefenseLeaf> clause = new ArrayList<>();

            if ("Connection".equals(systemTypeName)) {
                String connectionName = connectionNameMap.get(systemInstName);
                for (ConnectionModel connection :
                        connNameToConnectionModelMap.get(connectionName)) {
                    Defense defense =
                            connection.getAttackable().getDefenseByAttackAndCia(attackName, cia);
                    if (defense == null) {
                        Attack attack =
                                connection.getAttackable().getAttackByNameAndCia(attackName, cia);
                        if (attack == null) {
                            throw new RuntimeException(
                                    "could not find attack: " + attackName + ", " + cia);
                        }
                        defense = new Defense(attack);
                        connection.getAttackable().addDefense(defense);
                    }
                    defenses.add(defense);
                }
            } else {
                SystemModel system = getSystem(systemInstName);

                Defense defense = system.getAttackable().getDefenseByAttackAndCia(attackName, cia);
                if (defense == null) {
                    Attack attack = system.getAttackable().getAttackByNameAndCia(attackName, cia);
                    if (attack == null) {
                        throw new RuntimeException(
                                "could not find attack: " + attackName + ", " + cia);
                    }
                    defense = new Defense(attack);
                    system.getAttackable().addDefense(defense);
                }
                defenses.add(defense);
            }

            // TODO get defense descriptions from Defenses2NIST?

            // Need to get correct name if connection
            String entityName =
                    "Connection".equals(systemTypeName)
                            ? connectionNameMap.get(systemInstName)
                            : systemInstName;

            for (int i = 0; i < defenseNames.size(); i++) {
                if (!"null".equals(defenseNames.get(i))) {
                    int dal = -1;
                    // it will be null if we are not loading from VDM
                    if (compDefenseToImplDal != null) {
                        // load DAL from VDM if available
                        Pair<String, String> pair = new Pair<>(entityName, defenseNames.get(i));
                        if (compDefenseToImplDal.containsKey(pair)) {
                            dal = compDefenseToImplDal.get(pair);
                        } else {
                            // if there is no binding present, then it is not implemented
                            dal = 0;
                        }
                    }

                    // this code treats applicable defense and impl defense as separate things
                    // but we have changed the capitalization so that they should be the same
                    Optional<Pair<String, Integer>> impl;
                    if (dal == -1) {
                        impl =
                                "null".equals(implProps.get(i))
                                        ? Optional.empty()
                                        : Optional.of(
                                                new Pair<>(
                                                        implProps.get(i),
                                                        Integer.parseInt(
                                                                likelihoodStrings.get(i))));
                    } else {
                        impl =
                                dal == 0
                                        ? Optional.empty()
                                        : Optional.of(new Pair<>(defenseNames.get(i), dal));
                    }
                    clause.add(new Defense.DefenseLeaf(defenseNames.get(i), impl));
                }
            }

            // Each row is a conjunction
            // And there are potentially multiple such rows, forming a DNF
            for (Defense defense : defenses) {
                defense.addDefenseClause(clause);
            }
        }
    }

    /**
     * Perform inference on the loaded model. Factored out because it is used in both the CSV and
     * VDM approaches. Must be called after systems and cyber relations are loaded.
     */
    private void performInference() {
        int inferenceCounter = 0;
        for (SystemModel system : sysNameToSystemModelMap.values()) {
            // the internal connections bit is used in lieu of checking for the presence of
            // subcomponents
            // because we don't want to infer cyber relations for a system with subcomponents
            if (system.getCyberRels().isEmpty()
                    && system.getInternalIncomingConnections().isEmpty()
                    && system.getInternalOutgoingConnections().isEmpty()) {
                Logger.println("Inferring cyber relations for system " + system.getName());
                // We don't have explicit lists of ports, but we have the connections.
                // If a port is not mentioned in a connection, then it doesn't matter
                // anyway because it can't be traced.
                for (ConnectionModel outgoing : system.getOutgoingConnections()) {
                    if (!system.getIncomingConnections().isEmpty()) {
                        // For each of C, I, A, we have X -> X
                        for (CIA cia : CIA.values()) {
                            CyberExpr condition =
                                    new CyberOr(
                                            system.getIncomingConnections().stream()
                                                    .map(
                                                            incoming ->
                                                                    new PortConcern(
                                                                            incoming
                                                                                    .getDestinationPortName(),
                                                                            cia))
                                                    .collect(Collectors.toList()));
                            system.addCyberRel(
                                    new CyberRel(
                                            "_inference" + (inferenceCounter++),
                                            condition,
                                            new PortConcern(outgoing.getSourcePortName(), cia)));
                        }

                        // We also have I -> A
                        system.addCyberRel(
                                new CyberRel(
                                        "_inference" + (inferenceCounter++),
                                        new CyberOr(
                                                system.getIncomingConnections().stream()
                                                        .map(
                                                                incoming ->
                                                                        new PortConcern(
                                                                                incoming
                                                                                        .getDestinationPortName(),
                                                                                CIA.I))
                                                        .collect(Collectors.toList())),
                                        new PortConcern(outgoing.getSourcePortName(), CIA.A)));
                    }
                }
            }
        }
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
    public AttackDefenseCollector(File vdm, File stemOutputDir, boolean inference)
            throws CSVFile.MalformedInputException, IOException {
        Model vdmModel = VdmTranslator.unmarshalFromXml(vdm);

        sysNameToSystemModelMap = new LinkedHashMap<>();
        connNameToConnectionModelMap = new LinkedHashMap<>();

        // Keep track of component instances associated with each component type and impl
        Map<String, Set<SystemModel>> compTypeToSystem = new HashMap<>();
        Map<String, Set<SystemModel>> compImplToSystem = new HashMap<>();

        // For some reason the connection names in CAPEC.csv and Defenses.csv are confusing, so make
        // a map
        // from the confusing names to the correct names
        Map<String, String> connectionAttackNames = new HashMap<>();

        compDefenseToImplDal = new LinkedHashMap<>();

        // Load all instances as systems
        for (ComponentImpl impl : vdmModel.getComponentImpl()) {
            if (impl.getBlockImpl() != null) {
                for (ComponentInstance inst : impl.getBlockImpl().getSubcomponent()) {
                    // TODO: Change the getName() to getQualifiedName() later after changing VDM
                    SystemModel system = getSystem(inst.getName());
                    for (GenericAttribute attrib : inst.getAttribute()) {
                        // TODO: Be careful here about types of AADL properties
                        if (attrib.getValue() instanceof String) {
                            system.addAttribute(attrib.getName(), (String) attrib.getValue());
                        }
                    }
                    // Map AADL component type or implementation name to the internal system object
                    if (inst.getSpecification() != null) {
                        Util.putSetMap(compTypeToSystem, inst.getSpecification().getName(), system);
                    }
                    if (inst.getImplementation() != null) {
                        Util.putSetMap(
                                compImplToSystem, inst.getImplementation().getName(), system);
                    }
                }
            }
        }

        // Load top-level systems that don't exist as instances
        for (ComponentImpl impl : vdmModel.getComponentImpl()) {
            if (!compImplToSystem.containsKey(impl.getName())) {
                SystemModel system = getSystem(impl.getName());
                // TODO:
                if (impl.getType() != null) {
                    Util.putSetMap(compTypeToSystem, impl.getType().getName(), system);
                }
                Util.putSetMap(compImplToSystem, impl.getName(), system);
            }
        }

        // Load connections
        for (ComponentImpl impl : vdmModel.getComponentImpl()) {
            if (impl.getBlockImpl() != null) {
                for (Connection conn : impl.getBlockImpl().getConnection()) {
                    boolean internalIncoming = conn.getSource().getSubcomponentPort() == null;
                    boolean internalOutgoing = conn.getDestination().getSubcomponentPort() == null;

                    String sourcePort, destPort;
                    if (internalIncoming) {
                        sourcePort = conn.getSource().getComponentPort().getName();
                    } else {
                        if (conn.getSource().getSubcomponentPort().getPort() != null) {
                            sourcePort = conn.getSource().getSubcomponentPort().getPort().getName();
                        } else {
                            System.out.println("Null in port: " + conn.getName());
                            sourcePort = "null";
                        }
                    }
                    if (internalOutgoing) {
                        destPort = conn.getDestination().getComponentPort().getName();
                    } else {
                        if (conn.getDestination().getSubcomponentPort().getPort() != null) {
                            destPort =
                                    conn.getDestination().getSubcomponentPort().getPort().getName();
                        } else {
                            System.out.println("Null out port: " + conn.getName());
                            destPort = "null";
                        }
                    }

                    Collection<SystemModel> sources =
                            internalIncoming
                                    ? compImplToSystem.get(impl.getName())
                                    : Collections.singleton(
                                            getSystem(
                                                    conn.getSource()
                                                            .getSubcomponentPort()
                                                            .getSubcomponent()
                                                            .getName()));
                    Collection<SystemModel> dests =
                            internalOutgoing
                                    ? compImplToSystem.get(impl.getName())
                                    : Collections.singleton(
                                            getSystem(
                                                    conn.getDestination()
                                                            .getSubcomponentPort()
                                                            .getSubcomponent()
                                                            .getName()));

                    for (SystemModel srcSysModel : sources) {
                        for (SystemModel destSysModel : dests) {
                            ConnectionModel connection =
                                    new ConnectionModel(
                                            conn.getName(),
                                            srcSysModel,
                                            destSysModel,
                                            sourcePort,
                                            destPort);

                            for (GenericAttribute attrib : conn.getAttribute()) {
                                if (attrib.getValue() instanceof String) {
                                    connection.addAttribute(
                                            attrib.getName(), (String) attrib.getValue());
                                }
                            }

                            // System.out.println("ADDING CONNECTION " + conn.getName() + " from "
                            // + source.getName() + ":"
                            // + sourcePort + " to " + dest.getName() + ":" + destPort);

                            Util.putSetMap(
                                    connNameToConnectionModelMap, conn.getName(), connection);

                            // Store connection in a different place depending on internal/external
                            // and
                            // outgoing/incoming
                            if (internalIncoming) {
                                srcSysModel.addIncomingInternalConnection(connection);
                                destSysModel.addIncomingConnection(connection);
                            } else if (internalOutgoing) {
                                srcSysModel.addOutgoingConnection(connection);
                                destSysModel.addOutgoingInternalConnection(connection);
                            } else {
                                srcSysModel.addOutgoingConnection(connection);
                                destSysModel.addIncomingConnection(connection);
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
                for (SystemModel system : compTypeToSystem.get(compType.getName())) {
                    if (rel.getInputs() != null) {
                        system.addCyberRel(
                                new CyberRel(
                                        rel.getId(),
                                        convertCyberExpr(rel.getInputs()),
                                        convertCIAPort(rel.getOutput())));
                    } else {
                        // no input
                        system.addCyberRel(
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
                    for (SystemModel system : compTypeToSystem.get(req.getCompType())) {
                        system.addCyberReq(
                                new CyberReq(
                                        req.getId(),
                                        mission.getId(),
                                        convertSeverity(req.getSeverity()),
                                        convertCyberExpr(req.getCondition())));
                    }
                } else if (safetyReqMap.containsKey(reqName)) {
                    verdict.vdm.vdm_model.SafetyReq req = safetyReqMap.get(reqName);
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
                                        compDefenseToImplDal.put(
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
                                        compDefenseToImplDal.put(
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

        loadAttacksDefensesFromCsv(stemOutputDir.getAbsolutePath(), connectionAttackNames);

        if (inference) {
            performInference();
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
                            .map(AttackDefenseCollector::convertCyberExpr)
                            .collect(Collectors.toList()));
        } else if (expr.getOr() != null) {
            return new CyberOr(
                    expr.getOr().getExpr().stream()
                            .map(AttackDefenseCollector::convertCyberExpr)
                            .collect(Collectors.toList()));
        } else if (expr.getNot() != null) {
            return new CyberNot(convertCyberExpr(expr.getNot()));
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

    /**
     * Trace all cyber requirements, build attack-defense tree, and calculate probabilities for the
     * loaded model.
     *
     * <p>The bulk of the work is actually done in SystemModel::trace and ConnectionModel::trace.
     */
    public List<Result> perform() {
        List<Result> output = new ArrayList<>();

        // Find all cyber requirements, not just those declared in top-level systems
        // We are also ignoring whether or not the cyber requirement is in a mission
        for (SystemModel system : sysNameToSystemModelMap.values()) {
            for (CyberReq cyberReq : system.getCyberReqs()) {
                Optional<ADTree> treeOpt = system.trace(cyberReq.getCondition());
                // Crush the tree to remove redundant nodes
                ADTree crushed =
                        treeOpt.isPresent()
                                ? treeOpt.get().crush()
                                : new ADOr(Collections.emptyList(), true);

                // not enabling this for now because it is potentially inefficient
                // ADTree adtree = CutSetGenerator.generate(crushed);
                ADTree adtree = crushed;

                // Compute probability of attack
                Prob computed = adtree.compute();

                output.add(new Result(system, cyberReq, adtree, computed));
            }
        }

        return output;
    }

    /**
     * Get the full set of implemented component-defense pairs, not just those that appear in the
     * attack-defense tree.
     *
     * <p>Note: this only works if loading from VDM (the second constructor).
     *
     * @return a map from pairs (component, defense) to implemented DAL.
     */
    public Map<Pair<String, String>, Integer> getImplDal() {
        return Collections.unmodifiableMap(
                compDefenseToImplDal != null ? compDefenseToImplDal : new HashMap<>());
    }

    public static final class Result {
        public final SystemModel system;
        public final CyberReq cyberReq;
        public final ADTree adtree;
        public final Prob prob;

        public Result(SystemModel system, CyberReq cyberReq, ADTree adtree, Prob prob) {
            super();
            this.system = system;
            this.cyberReq = cyberReq;
            this.adtree = adtree;
            this.prob = prob;
        }
    }
}
