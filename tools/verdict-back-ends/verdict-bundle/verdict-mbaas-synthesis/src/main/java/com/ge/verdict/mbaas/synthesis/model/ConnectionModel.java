package com.ge.verdict.mbaas.synthesis.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConnectionModel extends Entity {
    /** The name of the connection. */
    private String name;
    /** The qualified name of the connection. */
    private String qualifiedName;
    /** The source system or system implementation. */
    private Entity srcSystem;
    /** The destination system or system implementation. */
    private Entity destSystem;
    /** The name of the port on the source system. */
    private String srcPortName;
    /** The name of the port on the destination system. */
    private String destPortName;
    /** All attributes set for this connection. */
    private Map<String, String> attributes;

    private List<Attack> applicableAttacks;
    private List<Defense> applicableDefenses;
    private List<Defense> implementedDefenses;

    /**
     * Create a new connection.
     *
     * @param name the name of the connection
     * @param source a resolver to the source system
     * @param dest a resolver to the destination system
     * @param sourcePort the name of the source port
     * @param destPort the name of the destination port
     */
    public ConnectionModel(
            String name, Entity source, Entity dest, String sourcePort, String destPort) {
        this.name = name;
        this.srcSystem = source;
        this.destSystem = dest;
        this.srcPortName = sourcePort;
        this.destPortName = destPort;
        attributes = new LinkedHashMap<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public void setQualifiedName(String qualifiedName) {
        this.qualifiedName = qualifiedName;
    }

    public Entity getSrcSystem() {
        return srcSystem;
    }

    public void setSrcSystem(SystemModel srcSystem) {
        this.srcSystem = srcSystem;
    }

    public Entity getDestSystem() {
        return destSystem;
    }

    public void setDestSystem(SystemModel destSystem) {
        this.destSystem = destSystem;
    }

    public String getSrcPortName() {
        return srcPortName;
    }

    public void setSrcPortName(String srcPortName) {
        this.srcPortName = srcPortName;
    }

    public String getDestPortName() {
        return destPortName;
    }

    public void setDestPortName(String destPortName) {
        this.destPortName = destPortName;
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

    public void setSrcSystem(Entity srcSystem) {
        this.srcSystem = srcSystem;
    }

    public void setDestSystem(Entity destSystem) {
        this.destSystem = destSystem;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
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

    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ConnectionModel && ((ConnectionModel) other).name.equals(name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
