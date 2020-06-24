package com.ge.verdict.synthesis.dtree;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;

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
    public Formula toLogicNG(FormulaFactory factory) {
        throw new RuntimeException("cannot encode NOT nodes");
    }

    @Override
    public DTree flattenNot() {
        if (child instanceof DNot) {
            return ((DNot) child).child;
        } else {
            throw new RuntimeException("unable to flatten NOT node: " + prettyPrint());
        }
    }
}
