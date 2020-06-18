package com.ge.verdict.attackdefensecollector.model;

import com.ge.verdict.attackdefensecollector.adtree.ADAnd;
import com.ge.verdict.attackdefensecollector.adtree.ADTree;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public Optional<ADTree> toADTree(Function<PortConcern, Optional<ADTree>> tracer) {
        // Convert all children into attack-defense trees and AND them together
        List<ADTree> adtrees =
                cyberExprs.stream()
                        .map(expr -> expr.toADTree(tracer))
                        .flatMap(opt -> opt.isPresent() ? Stream.of(opt.get()) : Stream.empty())
                        .collect(Collectors.toList());
        // Only return an attack-defense tree if it has children
        if (adtrees.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(new ADAnd(adtrees));
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CyberAnd && cyberExprs.equals(((CyberAnd) other).cyberExprs);
    }

    @Override
    public int hashCode() {
        return cyberExprs.hashCode();
    }
}
