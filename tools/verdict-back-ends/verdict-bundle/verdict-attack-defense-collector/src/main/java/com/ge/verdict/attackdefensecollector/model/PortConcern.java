package com.ge.verdict.attackdefensecollector.model;

import com.ge.verdict.attackdefensecollector.adtree.ADTree;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * A cyber expression for a single port concern, the fundamental unit for all cyber expressions.
 * Consists of a port name and a CIA.
 */
public class PortConcern extends CyberExpr {
    /** The name of the port. */
    private String portName;
    /** The CIA concern for the port. */
    private CIA cia;

    /**
     * Construct a port concern.
     *
     * @param portName the name of the port
     * @param cia the CIA concern for the port
     */
    public PortConcern(String portName, CIA cia) {
        this.portName = portName;
        this.cia = cia;
    }

    /**
     * @return the name of the port
     */
    public String getPortName() {
        return portName;
    }

    /**
     * @return the CIA concern for the port
     */
    public CIA getCia() {
        return cia;
    }

    @Override
    public Optional<ADTree> toADTree(Function<PortConcern, Optional<ADTree>> tracer) {
        // Directly apply provided tracer
        // This is the fundamental cyber expression unit, so no more work to do
        // The toADTree() method exists to support AND, OR, and NOT
        return tracer.apply(this);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof PortConcern) {
            PortConcern otherPortConcern = (PortConcern) other;
            return this.portName.equals(otherPortConcern.portName)
                    && this.cia.equals(otherPortConcern.cia);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(portName, cia);
    }

    @Override
    public String toString() {
        return portName + ":" + cia.toString();
    }
}
