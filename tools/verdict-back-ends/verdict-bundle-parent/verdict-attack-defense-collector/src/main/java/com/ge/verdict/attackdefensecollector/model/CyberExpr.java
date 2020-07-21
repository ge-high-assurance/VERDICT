package com.ge.verdict.attackdefensecollector.model;

import com.ge.verdict.attackdefensecollector.adtree.ADTree;
import java.util.Optional;
import java.util.function.Function;

/**
 * An arbitary logical cyber expression. May be comprised of port concerns, AND, OR, and NOT. The
 * fundamental unit of a cyber expression is the port concern.
 *
 * <p>The primary difference between a cyber expression and an attack-defense tree is that a cyber
 * expression has not fully resolved its port concerns into attack-defense trees. The attack-defense
 * tree construction constructs cyber expressions from attack-defense trees by resolving the port
 * concerns into attack-defense trees (with the toADTree() method). The fundamental units of an
 * attack-defense tree are the attack and the defense.
 */
public abstract class CyberExpr {
    /**
     * Build an attack-defense tree from this cyber expression.
     *
     * @param tracer function for converting port concerns into attack-defense trees
     * @return the optional constructed attack-defense tree
     */
    public abstract Optional<ADTree> toADTree(Function<PortConcern, Optional<ADTree>> tracer);

    @Override
    public abstract boolean equals(Object other);

    @Override
    public abstract int hashCode();
}
