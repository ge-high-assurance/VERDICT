package com.ge.verdict.attackdefensecollector.model;

import java.util.Optional;

/**
 * A cyber relation. Comprised of a name, an input expression, and an output port concern. The input
 * is optional; if it is not present, it signifies a cyber relation whose output is always an active
 * concern.
 *
 * <p>This class has different constructors for VDM and CSV input. The VDM constructors are more
 * powerful and are intended for use when the VDM input method is implemented. CSV input does not
 * support arbitrary logical expressions, for example.
 */
public class CyberRel {
    /** The name of the cyber relation. */
    private String name;
    /** The optional input expression. */
    private Optional<CyberExpr> input;
    /** The output port concern. */
    private PortConcern output;

    /**
     * Construct a cyber relation with an input expression. (Use for VDM input.)
     *
     * @param name the name of the cyber relation
     * @param input the input expression
     * @param output the output port concern
     */
    public CyberRel(String name, CyberExpr input, PortConcern output) {
        this.name = name;
        this.input = Optional.of(input);
        this.output = output;
    }

    /**
     * Construct a cyber relation with no input. (Use for VDM input.)
     *
     * @param name the name of the cyber relation
     * @param output the output port concern
     */
    public CyberRel(String name, PortConcern output) {
        this.name = name;
        input = Optional.empty();
        this.output = output;
    }

    /**
     * Construct a cyber relation with an input port concern. (Use for CSV input.)
     *
     * @param inputPort the input port name
     * @param inputCia the input port CIA concern
     * @param outputPort the output port name
     * @param outputCia the output port CIA concern
     */
    public CyberRel(String inputPort, CIA inputCia, String outputPort, CIA outputCia) {
        name = "";
        input = Optional.of(new PortConcern(inputPort, inputCia));
        output = new PortConcern(outputPort, outputCia);
    }

    /**
     * Construct a cyber relation with no input. (Use for CSV input.)
     *
     * @param outputPort the output port name
     * @param outputCia the output port CIA concern
     */
    public CyberRel(String outputPort, CIA outputCia) {
        name = "";
        input = Optional.empty();
        output = new PortConcern(outputPort, outputCia);
    }

    /**
     * Get the name of this cyber relation.
     *
     * <p>Note that for cyber relations constructed with the CSV input constructors, the name is the
     * empty string (the CSV format does not provide names).
     *
     * @return the name of the cyber relation
     */
    public String getName() {
        return name;
    }

    /**
     * @return the optional input expression
     */
    public Optional<CyberExpr> getInput() {
        return input;
    }

    /**
     * @return the output port concern
     */
    public PortConcern getOutput() {
        return output;
    }
}
