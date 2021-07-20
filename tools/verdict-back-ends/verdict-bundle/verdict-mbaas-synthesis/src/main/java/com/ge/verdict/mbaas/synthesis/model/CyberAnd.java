package com.ge.verdict.mbaas.synthesis.model;

import com.ge.verdict.mbaas.synthesis.adtree.ADTree;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * A cyber-expression representing the conjunction of other cyber expressions. A CyberAnd always
 * contains at least one child expression.
 */
public class CyberAnd extends CyberExpr {
    /** The child cyber expressions. */
    private List<CyberExpr> cyberExprs;

    /**
     * Constructs an AND expression.
     *
     * <p>Requires that cyberExprs is not empty.
     *
     * @param cyberExprs the list of children
     */
    public CyberAnd(List<CyberExpr> cyberExprs) {
        if (cyberExprs.isEmpty()) {
            throw new RuntimeException("Created empty CyberAnd");
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
        return other instanceof CyberAnd && cyberExprs.equals(((CyberAnd) other).cyberExprs);
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
