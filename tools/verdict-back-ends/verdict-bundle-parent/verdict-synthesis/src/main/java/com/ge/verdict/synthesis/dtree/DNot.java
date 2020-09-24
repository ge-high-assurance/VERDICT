package com.ge.verdict.synthesis.dtree;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import java.util.Optional;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;

/**
 * Represents a NOT in a defense tree. NOT nodes are not valid in the prepared defense tree, but
 * they may appear in the intermediary defense tree. Preparation cancels out two consecutive nots,
 * resulting in both being removed from the tree.
 */
public final class DNot implements DTree {
    public final DTree child;

    public DNot(DTree child) {
        this.child = child;
    }

    @Override
    public String prettyPrint() {
        return "(not " + child.prettyPrint() + ")";
    }

    @Override
    public BoolExpr toZ3(Context context) {
        // We could encode NOT, but this would invalidate the analysis
        throw new RuntimeException("cannot encode NOT nodes");
    }

    @Override
    public BoolExpr toZ3Multi(Context context) {
        // We could encode NOT, but this would invalidate the analysis
        throw new RuntimeException("cannot encode NOT nodes");
    }

    @Override
    public Formula toLogicNG(FormulaFactory factory) {
        throw new RuntimeException("cannot encode NOT nodes");
    }

    @Override
    public Optional<DTree> prepare() {
        if (child instanceof DNot) {
            // we can flatten by applying the rule (not (not x)) -> x
            return Optional.of(((DNot) child).child);
        } else {
            throw new RuntimeException("unable to flatten NOT node: " + prettyPrint());
        }
    }
}
