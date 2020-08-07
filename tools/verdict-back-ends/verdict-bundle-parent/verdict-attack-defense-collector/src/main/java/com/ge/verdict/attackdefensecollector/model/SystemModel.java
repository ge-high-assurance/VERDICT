package com.ge.verdict.attackdefensecollector.model;

import com.ge.verdict.attackdefensecollector.DependentRules;
import com.ge.verdict.attackdefensecollector.Logger;
import com.ge.verdict.attackdefensecollector.Pair;
import com.ge.verdict.attackdefensecollector.Util;
import com.ge.verdict.attackdefensecollector.adtree.ADAnd;
import com.ge.verdict.attackdefensecollector.adtree.ADNot;
import com.ge.verdict.attackdefensecollector.adtree.ADOr;
import com.ge.verdict.attackdefensecollector.adtree.ADTree;
import com.ge.verdict.attackdefensecollector.adtree.Attack;
import com.ge.verdict.attackdefensecollector.adtree.Defense;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Stores information about a system. Information is added as loading progresses. This class also
 * contains the logic for tracing cyber requirements down to attacks on specific components, through
 * the trace() method.
 */
public class SystemModel {
    /** System name. */
    private String name;
    /** Connections flowing to an input port of this system. */
    private List<ConnectionModel> connectionsIncoming;
    /** Connections flowing from an output port of this system. */
    private List<ConnectionModel> connectionsOutgoing;
    /** Cyber relations within this system. */
    private List<CyberRel> cyberRels;
    /** Cyber requirements within this system. */
    private List<CyberReq> cyberReqs;
    /** Subcomponent systems within this system. */
    private List<SystemModel> subcomponents;
    /**
     * Connections flowing from an input port of this system to the input port of a subcomponent.
     */
    private List<ConnectionModel> connectionsIncomingInternal;
    /**
     * Connections flowing from the output port of a subcomponent to an output port of this system.
     */
    private List<ConnectionModel> connectionsOutgoingInternal;

    private Attackable attackable;
    /** Map from cyber requirement names to cyber requirements for this system. */
    private Map<String, CyberReq> cyberReqMap;

    private Map<String, String> attributes;

    /**
     * Create a new system. Unlike the other modeling classes, the system is not fully realized
     * until everything has been added to it (connections, attacks, cyber relations, etc.).
     *
     * @param name the name of the system
     */
    public SystemModel(String name) {
        this.name = name;

        connectionsIncoming = new ArrayList<>();
        connectionsOutgoing = new ArrayList<>();
        cyberRels = new ArrayList<>();
        cyberReqs = new ArrayList<>();
        subcomponents = new ArrayList<>();
        connectionsIncomingInternal = new ArrayList<>();
        connectionsOutgoingInternal = new ArrayList<>();
        attackable = new Attackable(this);
        cyberReqMap = new LinkedHashMap<>();
        attributes = new LinkedHashMap<>();
    }

    /** @return the name of the system */
    public String getName() {
        return name;
    }

    /**
     * Adds an external connection that provides input to this system.
     *
     * @param connection
     */
    public void addIncomingConnection(ConnectionModel connection) {
        connectionsIncoming.add(connection);
    }

    /**
     * Adds an external connection that receives input from this system.
     *
     * @param connection
     */
    public void addOutgoingConnection(ConnectionModel connection) {
        connectionsOutgoing.add(connection);
    }

    /**
     * Adds a cyber relation to this system.
     *
     * @param cyberRel
     */
    public void addCyberRel(CyberRel cyberRel) {
        cyberRels.add(cyberRel);
    }

    /**
     * Adds a cyber requirement to this system.
     *
     * @param cyberReq
     */
    public void addCyberReq(CyberReq cyberReq) {
        cyberReqs.add(cyberReq);
        cyberReqMap.put(cyberReq.getName(), cyberReq);
    }

    /**
     * Adds a subcomponent to this system.
     *
     * @param subcomponent
     */
    public void addSubcomponent(SystemModel subcomponent) {
        subcomponents.add(subcomponent);
    }

    /**
     * Adds an internal connection that connects a system-wide input to the input of a subcomponent.
     *
     * @param connection
     */
    public void addIncomingInternalConnection(ConnectionModel connection) {
        connectionsIncomingInternal.add(connection);
    }

    /**
     * Adds an internal connection that connects the output of a subcomponent to a system-wide
     * output.
     *
     * @param connection
     */
    public void addOutgoingInternalConnection(ConnectionModel connection) {
        connectionsOutgoingInternal.add(connection);
    }

    public List<ConnectionModel> getIncomingConnections() {
        return connectionsIncoming;
    }

    public List<ConnectionModel> getOutgoingConnections() {
        return connectionsOutgoing;
    }

    public List<CyberRel> getCyberRels() {
        return cyberRels;
    }

    public List<CyberReq> getCyberReqs() {
        return cyberReqs;
    }

    /**
     * Gets the previously-added cyber requirement with the specified name, or the empty optional if
     * no cyber requirement with the specified name has been added.
     *
     * @param name the name to look up
     * @return the cyber requirement, or empty
     */
    public Optional<CyberReq> getCyberReq(String name) {
        if (cyberReqMap.containsKey(name)) {
            return Optional.of(cyberReqMap.get(name));
        } else {
            return Optional.empty();
        }
    }

    public List<SystemModel> getSubcomponents() {
        return subcomponents;
    }

    public List<ConnectionModel> getInternalIncomingConnections() {
        return connectionsIncomingInternal;
    }

    public List<ConnectionModel> getInternalOutgoingConnections() {
        return connectionsOutgoingInternal;
    }

    public Attackable getAttackable() {
        return attackable;
    }

    public void addAttribute(String name, String value) {
        attributes.put(name, value);
    }

    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    /** Map from output port concerns to cyber relations with those output port concerns. */
    private Map<PortConcern, List<CyberRel>> outputConcernToCyberRel;
    /** Map from destination ports to outgoing internal connections with those destination ports. */
    private Map<String, List<ConnectionModel>> destPortToOutgoingInternalConnection;
    /** Map from source ports to incoming internal connections with those source ports. */
    private Map<String, List<ConnectionModel>> sourcePortToIncomingInternalConnection;
    /** Map from input ports to incoming connections with those destination ports. */
    private Map<String, List<ConnectionModel>> inputPortToIncomingConnection;
    /** Map from attacks to defenses that defend against those attacks. */
    private Map<Attack, Defense> attackToDefense;

    /**
     * Build all of the maps used by trace(). This is performed once for significant time complexity
     * improvements.
     */
    public void concretize() {
        outputConcernToCyberRel = new LinkedHashMap<>();
        destPortToOutgoingInternalConnection = new LinkedHashMap<>();
        sourcePortToIncomingInternalConnection = new LinkedHashMap<>();
        inputPortToIncomingConnection = new LinkedHashMap<>();
        attackToDefense = new LinkedHashMap<>();

        for (CyberRel cyberRel : cyberRels) {
            Util.putListMap(outputConcernToCyberRel, cyberRel.getOutput(), cyberRel);
        }

        for (ConnectionModel connection : getInternalOutgoingConnections()) {
            Util.putListMap(
                    destPortToOutgoingInternalConnection,
                    connection.getDestinationPortName(),
                    connection);
        }

        for (ConnectionModel connection : getInternalIncomingConnections()) {
            Util.putListMap(
                    sourcePortToIncomingInternalConnection,
                    connection.getSourcePortName(),
                    connection);
        }

        for (ConnectionModel connection : getIncomingConnections()) {
            Util.putListMap(
                    inputPortToIncomingConnection, connection.getDestinationPortName(), connection);
        }

        Set<Attack> declaredAttacks = new HashSet<>();
        for (Attack attack : attackable.getAttacks()) {
            declaredAttacks.add(attack);
        }

        for (Defense defense : attackable.getDefenses()) {
            // Check that referenced attacks are added to this system
            if (!declaredAttacks.contains(defense.getAttack())) {
                throw new RuntimeException(
                        "Defense in system "
                                + getName()
                                + " refers to non-existant attack "
                                + defense.getAttack().getName());
            }

            attackToDefense.put(defense.getAttack(), defense);
        }
    }

    /** @return true iff concretize() has already been called */
    public boolean isConcretized() {
        return outputConcernToCyberRel != null;
    }

    /**
     * Trace an input port concern through incoming connections, constructing an attack-defense tree
     * for all possible attacks on the system.
     *
     * @param inputConcern the input port concern
     * @param cyclePrevention a set of previously-traced connections and CIAs, used to prevent
     *     cycles from causing infinite loops
     * @return the optional attack-defense tree constructed from tracing the input port concern
     */
    protected Optional<ADTree> traceInputConcern(
            PortConcern inputConcern, Set<Pair<ConnectionModel, CIA>> cyclePrevention) {
        List<ADTree> adtrees = new ArrayList<>();
        for (ConnectionModel incomingConnection :
                Util.guardedGet(inputPortToIncomingConnection, inputConcern.getPortName())) {
            incomingConnection.trace(inputConcern.getCia(), cyclePrevention).map(adtrees::add);
        }
        if (adtrees.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(new ADOr(adtrees));
        }
    }

    /**
     * Trace a port concern through a system from an output port, or from the input port of a
     * subsystem with an internal incoming connection, constructing an attack-defense tree for all
     * possible attacks on the system.
     *
     * <p>Uses attacks directly affecting the system, cyber relations, internal connections to
     * subcomponents, and incoming connections to construct the entire attack-defense tree.
     *
     * <p>Avoids entering infinite loops by keeping track of pairs of connections and CIAs that have
     * already been traced.
     *
     * <p>Builds maps (with concretize()) on first invocation.
     *
     * @param concern the port and CIA to trace
     * @param cyclePrevention a set of previously-traced connections and CIAs, used to prevent
     *     cycles from causing infinite loops
     * @return the optional attack-defense tree constructed from tracing the port concern
     */
    protected Optional<ADTree> trace(
            PortConcern concern, Set<Pair<ConnectionModel, CIA>> cyclePrevention) {
        if (!isConcretized()) {
            concretize();
        }

        //		Logger.println("Tracing " + getName() + " " + concern.getPortName() + ":" +
        // concern.getCia());

        // All attack-defense trees that will be OR-ed together at the end
        Set<ADTree> children = new HashSet<>();
        // If we find a cyber rel with this output port, then even if we have successfully
        // traced the port concern even if we don't turn up an attack-defense tree at the end
        boolean hasCyberRel = false;

        // Attacks which apply directly to this system
        for (Attack attack : attackable.getAttacks()) {
            // Only allow matching CIA attacks
            if (attack.getCia().equals(concern.getCia())) {
                if (attackToDefense.containsKey(attack)) {
                    // There is a defense associated
                    Optional<ADTree> dependentRules =
                            DependentRules.getComponentDependence(this, attack.getName());
                    if (dependentRules.isPresent()) {
                        children.add(
                                new ADAnd(
                                        new ADNot(attackToDefense.get(attack)),
                                        attack,
                                        dependentRules.get()));
                    } else {
                        children.add(new ADAnd(new ADNot(attackToDefense.get(attack)), attack));
                    }
                } else {
                    // There is no defense, just a raw attack
                    children.add(attack);
                }
            }
        }

        // Search in cyber relations
        for (CyberRel cyberRel : Util.guardedGet(outputConcernToCyberRel, concern)) {
            hasCyberRel = true;
            if (cyberRel.getInput().isPresent()) {
                // Trace cyber relation
                cyberRel.getInput()
                        .get()
                        .toADTree(inputConcern -> traceInputConcern(inputConcern, cyclePrevention))
                        .map(children::add);
            }
        }

        // Search in subcomponents (using internal connections)
        for (ConnectionModel internalConnection :
                Util.guardedGet(destPortToOutgoingInternalConnection, concern.getPortName())) {
            internalConnection.trace(concern.getCia(), cyclePrevention).map(children::add);
        }

        // If concern refers to an incoming port of this system traced from a subcomponent
        // (This happens when tracing from a subcomponent back to an input of the overall system)
        for (ConnectionModel internalConnection :
                Util.guardedGet(sourcePortToIncomingInternalConnection, concern.getPortName())) {
            traceInputConcern(
                            new PortConcern(
                                    internalConnection.getSourcePortName(), concern.getCia()),
                            cyclePrevention)
                    .map(children::add);
        }

        if (children.isEmpty()) {
            if (!hasCyberRel) {
                Logger.showWarning(
                        "Found no trace for "
                                + getName()
                                + " "
                                + concern.getPortName()
                                + ":"
                                + concern.getCia());
            }
            return Optional.empty();
        } else {
            // Disjunction of all of the inputs
            return Optional.of(new ADOr(children));
        }
    }

    /**
     * Trace a port concern through a system from an output port, constructing an attack-defense
     * tree for all possible attacks on the system.
     *
     * @param concern the port and CIA to trace
     * @return the optional attack-defense tree constructed from tracing the port concern
     */
    public Optional<ADTree> trace(PortConcern concern) {
        return trace(concern, new HashSet<>());
    }

    /**
     * Trace a cyber expression through a system from an output port, constructing an attack-defense
     * tree for all possible attacks on the system.
     *
     * @param expr the cyber expression to trace
     * @return the optional attack-defense tree constructed from tracing the cyber expression
     */
    public Optional<ADTree> trace(CyberExpr expr) {
        // Simply trace down the expression
        return expr.toADTree(this::trace);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SystemModel && ((SystemModel) other).getName().equals(name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
