package com.ge.verdict.attackdefensecollector.adtree;

import com.ge.verdict.attackdefensecollector.IndentedStringBuilder;
import com.ge.verdict.attackdefensecollector.Prob;
import java.util.Objects;

/** A negation of an attack-defense tree. */
public class ADNot extends ADTree {
    /** The child attack-defense tree. */
    private ADTree adtree;

    /**
     * Constructs a NOT attack-defense tree.
     *
     * @param adtree the child attack-defense tree
     */
    public ADNot(ADTree adtree) {
        this.adtree = adtree;
    }

    @Override
    public ADTree crush() {
        // In either case, make sure to recursively crush the child

        // Double-not elimination
        if (adtree instanceof ADNot) {
            return ((ADNot) adtree).adtree.crush();
        }

        return new ADNot(adtree.crush());
    }

    @Override
    public Prob compute() {
        // Simply NOT the child's probability
        return Prob.not(adtree.compute());
    }

    @Override
    public void prettyPrint(IndentedStringBuilder builder) {
        // NOT child
        builder.append("NOT ");
        adtree.prettyPrint(builder);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ADNot && ((ADNot) other).adtree.equals(adtree);
    }

    @Override
    public int hashCode() {
        return Objects.hash(adtree);
    }
}
