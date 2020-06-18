package com.ge.verdict.attackdefensecollector.model;

import com.ge.verdict.attackdefensecollector.NameResolver;

/** Stores information about a connection. */
public class ConnectionModel {
    /** The name of the connection. */
    private String name;
    /** The source system. */
    private NameResolver<SystemModel> source;
    /** The destination system. */
    private NameResolver<SystemModel> dest;
    /** The name of the port on the source system. */
    private String sourcePort;
    /** The name of the port on the destination system. */
    private String destPort;

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
            String name,
            NameResolver<SystemModel> source,
            NameResolver<SystemModel> dest,
            String sourcePort,
            String destPort) {
        this.name = name;
        this.source = source;
        this.dest = dest;
        this.sourcePort = sourcePort;
        this.destPort = destPort;
    }

    /** @return the name of the connection */
    public String getName() {
        return name;
    }

    /** @return the source system model */
    public SystemModel getSource() {
        return source.get();
    }

    /** @return the destination system model */
    public SystemModel getDestination() {
        return dest.get();
    }

    /** @return the name of the source port */
    public String getSourcePortName() {
        return sourcePort;
    }

    /** @return the name of the destination port */
    public String getDestinationPortName() {
        return destPort;
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
