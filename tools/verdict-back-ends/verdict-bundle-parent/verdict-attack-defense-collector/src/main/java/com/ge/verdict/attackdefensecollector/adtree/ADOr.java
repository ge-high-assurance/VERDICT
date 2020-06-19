package com.ge.verdict.attackdefensecollector.adtree;

import com.ge.verdict.attackdefensecollector.IndentedStringBuilder;
import com.ge.verdict.attackdefensecollector.Prob;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** A disjunction of several attack-defense trees. An ADOr always contains at least one child. */
public class ADOr extends ADTree {
    /** The child attack-defense trees. */
    private List<ADTree> adtrees;

    /**
     * Constructs an OR attack-defense tree.
     *
     * @param adtrees the list of children
     * @param allowEmpty whether or not to allow an empty list
     */
    public ADOr(List<ADTree> adtrees, boolean allowEmpty) {
        if (adtrees.isEmpty() && !allowEmpty) {
            throw new RuntimeException("Created empty ADOr");
        }
        this.adtrees = adtrees;
    }

    /**
     * Constructs an OR attack-defense tree.
     *
     * <p>Requires that adtrees is not empty.
     *
     * @param adtrees the list of children
     */
    public ADOr(List<ADTree> adtrees) {
        this(adtrees, false);
    }

    /**
     * Constructs an OR attack-defense tree.
     *
     * <p>Requires that adtrees is not empty.
     *
     * @param adtrees the set of children
     */
    public ADOr(Set<ADTree> adtrees) {
        this(adtrees.stream().collect(Collectors.toList()));
    }

    /**
     * Constructs an OR attack-defense tree.
     *
     * <p>Requires that adtrees is not empty.
     *
     * @param adtrees the varargs array of children
     */
    public ADOr(ADTree... adtrees) {
        this(Arrays.asList(adtrees));
    }

    public List<ADTree> children() {
        return adtrees;
    }

    @Override
    public ADTree crush() {
        // The basic idea here is that if we have an OR with OR children, all of the
        // children of the OR children can be made children directly. We also make sure
        // to crush the children recursively and remove duplicate children. A similar
        // implementation is used in ADAnd.

        List<ADTree> crushed = adtrees.stream().map(ADTree::crush).collect(Collectors.toList());

        // Disjunction is commutative
        Stream<ADTree> combinedOrs =
                crushed.stream()
                        .filter(adtree -> adtree instanceof ADOr)
                        .map(or -> (ADOr) or)
                        .flatMap(or -> or.adtrees.stream());
        Stream<ADTree> others = crushed.stream().filter(adtree -> !(adtree instanceof ADOr));

        return new ADOr(Stream.concat(combinedOrs, others).distinct().collect(Collectors.toList()));
    }

    @Override
    public Prob compute() {
        // Fold across children with OR operator as follows:
        // 0 OR child_1 OR child_2 OR ... OR child_n
        return adtrees.stream()
                .map(ADTree::compute)
                .reduce(Prob.impossible(), (a, b) -> Prob.or(a, b));
    }

    @Override
    public void prettyPrint(IndentedStringBuilder builder) {
        // child_1 \/
        // child_2 \/
        // ... \/
        // child_n
        builder.newLine();
        for (int i = 0; i < adtrees.size(); i++) {
            builder.append('(');

            // Indent children
            builder.indent();
            adtrees.get(i).prettyPrint(builder);
            builder.unindent();

            builder.append(')');
            if (i < adtrees.size() - 1) {
                builder.append(" \\/ ");
                builder.newLine();
            }
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ADOr && adtrees.equals(((ADOr) other).adtrees);
    }

    @Override
    public int hashCode() {
        return Objects.hash(adtrees);
    }
}
