package com.ge.verdict.mbaas.synthesis.model;

import com.ge.verdict.mbaas.synthesis.adtree.ADTree;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * A cyber-expression representing the disjunction of other cyber expressions. A CyberOr always
 * contains at least one child expression.
 */
public class CyberOr extends CyberExpr {
    /** The child cyber expressions. */
    private List<CyberExpr> cyberExprs;

    /**
     * Constructs an OR expression.
     *
     * <p>Requires that cyberExprs is not empty.
     *
     * @param cyberExprs
     */
    public CyberOr(List<CyberExpr> cyberExprs) {
        if (cyberExprs.isEmpty()) {
            throw new RuntimeException("Created empty CyberOr");
        }
        this.cyberExprs = cyberExprs;
    }

    /**
     * Returns a mutable list of children. It is the callee's responsibility to not remove the last
     * element from this list, which is a violation of the non-empty property.
     *
     * @return the list of children
     */
    public List<CyberExpr> getCyberExprs() {
        return cyberExprs;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CyberOr && cyberExprs.equals(((CyberOr) other).cyberExprs);
    }

    @Override
    public int hashCode() {
        return cyberExprs.hashCode();
    }

    @Override
    public Optional<ADTree> toADTree(Function<PortConcern, Optional<ADTree>> tracer) {
        // TODO Auto-generated method stub
        return null;
    }
}
