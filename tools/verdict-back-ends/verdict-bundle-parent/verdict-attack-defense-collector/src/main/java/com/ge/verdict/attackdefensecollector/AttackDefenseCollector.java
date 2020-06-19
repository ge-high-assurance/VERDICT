package com.ge.verdict.attackdefensecollector;

import com.ge.verdict.attackdefensecollector.adtree.ADOr;
import com.ge.verdict.attackdefensecollector.adtree.ADTree;
import com.ge.verdict.attackdefensecollector.adtree.Attack;
import com.ge.verdict.attackdefensecollector.adtree.Defense;
import com.ge.verdict.attackdefensecollector.model.CIA;
import com.ge.verdict.attackdefensecollector.model.ConnectionModel;
import com.ge.verdict.attackdefensecollector.model.CyberExpr;
import com.ge.verdict.attackdefensecollector.model.CyberOr;
import com.ge.verdict.attackdefensecollector.model.CyberRel;
import com.ge.verdict.attackdefensecollector.model.CyberReq;
import com.ge.verdict.attackdefensecollector.model.PortConcern;
import com.ge.verdict.attackdefensecollector.model.SystemModel;
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

/**
 * Main class of the attack-defense collector implementation.
 *
 * <p>Load a model with one of the constructors, then invoke perform() to perform computations and
 * write output files.
 */
public class AttackDefenseCollector {
    /** Resolution table for system models. */
    private Map<String, SystemModel> systems;
    /** Resolution table for connection models. */
    private Map<String, ConnectionModel> connections;

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
        if (!systems.containsKey(name)) {
            systems.put(name, new SystemModel(name));
        }
        return systems.get(name);
    }

    /**
     * Builds a NameResolver for the specified system model.
     *
     * <p>This method should only be called if the system model has already been added to the system
     * model resolution table. Calling this method without first adding the system model to the
     * table (ideally with getSystem()) will throw an exception.
     *
     * @param system the system to build the resolver from
     * @return a resolver to the specified system
     */
    private NameResolver<SystemModel> resolver(SystemModel system) {
        if (!systems.containsKey(system.getName())) {
            throw new RuntimeException(
                    "Building resolver for system: " + system.getName() + " without adding to map");
        }
        return new NameResolver<>(system.getName(), systems);
    }

    /**
     * Builds a NameResolver for a specified connection model.
     *
     * <p>This method should only be called if the connection model has already been added to the
     * connection model resolution table. Calling this method without first adding the connection
     * model to the table will throw an exception.
     *
     * @param connection the connection to build the resolver from
     * @return a resolver to the specified connection
     */
    private NameResolver<ConnectionModel> resolver(ConnectionModel connection) {
        if (!connections.containsKey(connection.getName())) {
            throw new RuntimeException(
                    "Building resolver for connection: "
                            + connection.getName()
                            + " without adding to map");
        }
        return new NameResolver<>(connection.getName(), connections);
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
     *   <li>ScnArch.csv
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
     */
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
         *
         * TODO update to use ScnCompProps.csv
         */

        systems = new LinkedHashMap<>();
        connections = new LinkedHashMap<>();

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
        CSVFile compDepCsv =
                new CSVFile(
                        new File(inputDir, "CompDep.csv"),
                        "Comp",
                        "InputPort",
                        "InputCIA",
                        "OutputPort",
                        "OutputCIA");
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
        CSVFile scnCompPropsCsv =
                new CSVFile(
                        new File(inputDir, "ScnCompProps.csv"),
                        "Scenario",
                        "Comp",
                        "Impl",
                        "CompInstance");
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
                            new ConnectionModel(
                                    name, resolver(source), resolver(dest), sourcePort, destPort);

                    //					Logger.println("loaded connection: " + name + " from (" + sourceTypeName
                    // + ", "
                    //							+ sourceImplName + ", " + sourceInstName + ") to (" + destTypeName + ",
                    // " + destImplName
                    //							+ ", " + destInstName + ")");

                    connections.put(name, connection);

                    // Store connection in a different place depending on internal/external and
                    // outgoing/incoming
                    if (internalIncoming) {
                        source.addIncomingInternalConnection(resolver(connection));
                        dest.addIncomingConnection(resolver(connection));
                    } else if (internalOutgoing) {
                        source.addOutgoingConnection(resolver(connection));
                        dest.addOutgoingInternalConnection(resolver(connection));
                        outgoingConnectionMap.put(
                                new Pair<>(source, sourcePort), new Pair<>(dest, destPort));
                    } else {
                        source.addOutgoingConnection(resolver(connection));
                        dest.addIncomingConnection(resolver(connection));
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

        // Load attacks
        for (CSVFile.RowData row : capecCsv.getRowDatas()) {
            String systemTypeName = row.getCell("CompType");
            String systemInstName = row.getCell("CompInst");

            if (!"Connection".equals(systemTypeName)) {
                Set<SystemModel> systems = Collections.singleton(getSystem(systemInstName));

                String attackName = row.getCell("CAPEC");
                String attackDesc = row.getCell("CAPECDescription");
                Prob likelihood = Prob.certain();
                // Look at all three columns to figure out which one is being used
                CIA cia =
                        CIA.fromStrings(
                                row.getCell("Confidentiality"),
                                row.getCell("Integrity"),
                                row.getCell("Availability"));

                // Apply to all systems of this component type (unless particular instance
                // specified)
                for (SystemModel system : systems) {
                    system.addAttack(
                            new Attack(resolver(system), attackName, attackDesc, likelihood, cia));
                }
            }
        }

        // Load defenses
        for (CSVFile.RowData row : defensesCsv.getRowDatas()) {
            String systemTypeName = row.getCell("CompType");
            String systemInstName = row.getCell("CompInst");

            Set<SystemModel> systems = Collections.singleton(getSystem(systemInstName));

            String attackName = row.getCell("CAPEC");
            CIA cia =
                    CIA.fromStrings(
                            row.getCell("Confidentiality"),
                            row.getCell("Integrity"),
                            row.getCell("Availability"));
            List<String> defenseNames =
                    Arrays.asList(row.getCell("ApplicableDefenseProperties").split(";"));
            List<String> implProps = Arrays.asList(row.getCell("ImplProperties").split(";"));
            List<String> likelihoodStrings = Arrays.asList(row.getCell("DAL").split(";"));
            // Prob likelihood = Prob.not(Prob.fromDal(row.getCell("DAL"), Prob.certain()));

            if (defenseNames.size() != implProps.size()
                    || defenseNames.size() != likelihoodStrings.size()) {
                throw new RuntimeException(
                        "ApplicableDefenseProperties, ImplProperties, and DAL must have same cardinality");
            }

            // Apply to all systems of the this component type (unless particular instance
            // specified)
            for (SystemModel system : systems) {
                // Each row is a conjunction
                // And there are potentially multiple such rows, forming a DNF
                Defense defense = system.getDefenseByAttackAndCia(attackName, cia);
                if (defense == null) {
                    defense = new Defense(system.getAttackByNameAndCia(attackName, cia));
                    system.addDefense(defense);
                }

                // TODO get defense descriptions from Defenses2NIST?

                List<Defense.DefenseLeaf> clause = new ArrayList<>();
                for (int i = 0; i < defenseNames.size(); i++) {
                    if (!"null".equals(defenseNames.get(i))) {
                        Optional<Pair<String, Prob>> impl =
                                "null".equals(implProps.get(i))
                                        ? Optional.empty()
                                        : Optional.of(
                                                new Pair<>(
                                                        implProps.get(i),
                                                        Prob.fromDal(likelihoodStrings.get(i))));
                        clause.add(new Defense.DefenseLeaf(defenseNames.get(i), impl));
                    }
                }
                defense.addDefenseClause(clause);
            }
        }

        if (inference) {
            // Inference
            int inferenceCounter = 0;
            for (SystemModel system : systems.values()) {
                // We can't check subcomponents because it isn't actually populated...
                if (system.getCyberRels().isEmpty()
                        && system.getInternalIncomingConnections().isEmpty()
                        && system.getInternalOutgoingConnections().isEmpty()) {
                    Logger.println("Inferring cyber relations for system " + system.getName());

                    // We don't have explicit lists of ports, but we have the connections.
                    // If a port is not mentioned in a connection, then it doesn't matter
                    // anyway because it can't be traced.
                    for (ConnectionModel outgoing : system.getOutgoingConnections()) {
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
                    }
                }
            }
        }
    }

    /**
     * Trace all cyber requirements, build attack-defense tree, and calculate probabilities for the
     * loaded model. Output this information to standard out.
     *
     * <p>The bulk of the work is actually done in SystemModel::trace.
     */
    public List<Result> perform() {
        List<Result> output = new ArrayList<>();

        // Find all cyber requirements, not just those declared in top-level systems
        // We are also ignoring whether or not the cyber requirement is in a mission
        for (SystemModel system : systems.values()) {
            for (CyberReq cyberReq : system.getCyberReqs()) {
                Optional<ADTree> treeOpt = system.trace(cyberReq.getCondition());
                // Crush the tree to remove redundant nodes
                ADTree adtree =
                        treeOpt.isPresent()
                                ? treeOpt.get().crush()
                                : new ADOr(Collections.emptyList(), true);
                // Compute probability of attack
                Prob computed = adtree.compute();

                output.add(new Result(system, cyberReq, adtree, computed));
            }
        }

        return output;
    }

    public static final class Result {
        public final SystemModel system;
        public final CyberReq cyberReq;
        public final ADTree adtree;
        public final Prob prob;

        private Result(SystemModel system, CyberReq cyberReq, ADTree adtree, Prob prob) {
            super();
            this.system = system;
            this.cyberReq = cyberReq;
            this.adtree = adtree;
            this.prob = prob;
        }
    }
}
