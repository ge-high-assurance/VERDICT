package com.ge.verdict.mbaas.synthesis.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SystemModel extends Entity {
    /** Whether the system model is an implementation or not */
    private boolean isImpl = false;

    /** System name. */
    private String name;

    /** System qualified name. */
    private String qualifiedName;

    private List<SystemModel> subcomponents;

    /** Connections flowing to an input port of this system. */
    private List<ConnectionModel> incomingConnections;

    /** Connections flowing from an output port of this system. */
    private List<ConnectionModel> outgoingConnections;

    /**
     * Connections flowing from an input port of this system to the input port of a subcomponent.
     */
    private List<ConnectionModel> internalIncomingConnections;
    /**
     * Connections flowing from the output port of a subcomponent to an output port of this system.
     */
    private List<ConnectionModel> internalOutgoingConnections;

    /** Cyber relations within this system. */
    private List<CyberRel> cyberRels;

    /** Cyber requirements within this system. */
    private List<CyberReq> cyberReqs;

    /** Map from cyber requirement names to cyber requirements for this system. */
    private Map<String, CyberReq> nametoCyberReqMap;
    /** All attributes set for this system. */
    private Map<String, String> attributes;

    private List<Attack> applicableAttacks;
    private List<Defense> applicableDefenses;
    private List<Defense> implementedDefenses;

    public SystemModel(String name) {
        this.name = name;
        this.cyberRels = new ArrayList<>();
        this.cyberReqs = new ArrayList<>();
        this.nametoCyberReqMap = new LinkedHashMap<>();
        this.attributes = new LinkedHashMap<>();
        this.applicableAttacks = new ArrayList<>();
        this.applicableDefenses = new ArrayList<>();
        this.implementedDefenses = new ArrayList<>();
        this.incomingConnections = new ArrayList<>();
        this.outgoingConnections = new ArrayList<>();
        this.internalIncomingConnections = new ArrayList<>();
        this.internalOutgoingConnections = new ArrayList<>();
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public void setQualifiedName(String qualifiedName) {
        this.qualifiedName = qualifiedName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ConnectionModel> getIncomingConnections() {
        return incomingConnections;
    }

    public void setIncomingConnections(List<ConnectionModel> incomingConnections) {
        this.incomingConnections = incomingConnections;
    }

    public List<ConnectionModel> getOutgoingConnections() {
        return outgoingConnections;
    }

    public void setOutgoingConnections(List<ConnectionModel> outgoingConnections) {
        this.outgoingConnections = outgoingConnections;
    }

    public List<CyberRel> getCyberRels() {
        return cyberRels;
    }

    public void setCyberRels(List<CyberRel> cyberRels) {
        this.cyberRels = cyberRels;
    }

    public List<CyberReq> getCyberReqs() {
        return cyberReqs;
    }

    public void setCyberReqs(List<CyberReq> cyberReqs) {
        this.cyberReqs = cyberReqs;
    }

    public Map<String, CyberReq> getCyberReqMap() {
        return nametoCyberReqMap;
    }

    public void setCyberReqMap(Map<String, CyberReq> cyberReqMap) {
        this.nametoCyberReqMap = cyberReqMap;
    }

    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public List<Attack> getApplicableAttacks() {
        return applicableAttacks;
    }

    public void setApplicableAttacks(List<Attack> applicableAttacks) {
        this.applicableAttacks = applicableAttacks;
    }

    public List<Defense> getApplicableDefenses() {
        return applicableDefenses;
    }

    public void setApplicableDefenses(List<Defense> applicableDefenses) {
        this.applicableDefenses = applicableDefenses;
    }

    public List<Defense> getImplementedDefenses() {
        return implementedDefenses;
    }

    public void setImplementedDefenses(List<Defense> implementedDefenses) {
        this.implementedDefenses = implementedDefenses;
    }

    public List<SystemModel> getSubcomponents() {
        return subcomponents;
    }

    public void setSubcomponents(List<SystemModel> subcomponents) {
        this.subcomponents = subcomponents;
    }

    public List<ConnectionModel> getInternalIncomingConnections() {
        return internalIncomingConnections;
    }

    public void setInternalIncomingConnections(List<ConnectionModel> internalIncomingConnections) {
        this.internalIncomingConnections = internalIncomingConnections;
    }

    public List<ConnectionModel> getInternalOutgoingConnections() {
        return internalOutgoingConnections;
    }

    public void setInternalOutgoingConnections(List<ConnectionModel> internalOutgoingConnections) {
        this.internalOutgoingConnections = internalOutgoingConnections;
    }

    public boolean isImpl() {
        return isImpl;
    }

    public void setImpl(boolean isImpl) {
        this.isImpl = isImpl;
    }

    public void addIncomingConnection(ConnectionModel connection) {
        this.incomingConnections.add(connection);
    }

    public void addOutgoingConnection(ConnectionModel connection) {
        this.outgoingConnections.add(connection);
    }

    public void addInternalIncomingConnection(ConnectionModel connection) {
        this.internalIncomingConnections.add(connection);
    }

    public void addInternalOutgoingConnection(ConnectionModel connection) {
        this.internalOutgoingConnections.add(connection);
    }

    public void addCyberRel(CyberRel cyberRel) {
        cyberRels.add(cyberRel);
    }

    /**
     * Adds an attribute.
     *
     * @param name
     * @param value
     */
    public void addAttribute(String name, String value) {
        attributes.put(name, value);
    }

    /**
     * Adds a cyber requirement to this system.
     *
     * @param cyberReq
     */
    public void addCyberReq(CyberReq cyberReq) {
        cyberReqs.add(cyberReq);
        nametoCyberReqMap.put(cyberReq.getName(), cyberReq);
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
