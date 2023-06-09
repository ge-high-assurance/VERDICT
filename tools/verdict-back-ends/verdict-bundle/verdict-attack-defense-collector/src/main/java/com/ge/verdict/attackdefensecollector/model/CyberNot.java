package com.ge.verdict.attackdefensecollector.model;

import com.ge.verdict.attackdefensecollector.adtree.ADNot;
import com.ge.verdict.attackdefensecollector.adtree.ADTree;
import java.util.Optional;
import java.util.function.Function;

/** A cyber expression representing the negation of another cyber expression. */
public class CyberNot extends CyberExpr {
    /** The child cyber expression. */
    private CyberExpr cyberExpr;

    /**
     * Constructs a NOT expression.
     *
     * @param cyberExpr the child cyber expression
     */
    public CyberNot(CyberExpr cyberExpr) {
        this.cyberExpr = cyberExpr;
    }

    /**
     * Returns the mutable child cyber expression.
     *
     * @return the child
     */
    public CyberExpr getCyberExpr() {
        return cyberExpr;
    }

    @Override
    public Optional<ADTree> toADTree(Function<PortConcern, Optional<ADTree>> tracer) {
        // Simply convert child and wrap in NOT
        return cyberExpr.toADTree(tracer).map(ADNot::new);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CyberNot && cyberExpr.equals(((CyberNot) other).cyberExpr);
    }

    @Override
    public int hashCode() {
        return cyberExpr.hashCode();
    }
}
