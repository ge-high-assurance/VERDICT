package com.ge.verdict.synthesis.dtree;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;

public final class DAnd implements DTree {
    public final List<DTree> children;

    public DAnd(List<DTree> children) {
        this.children = Collections.unmodifiableList(children);
    }

    @Override
    public String prettyPrint() {
        return "("
                + children.stream().map(DTree::prettyPrint).collect(Collectors.joining(" ^ "))
                + ")";
    }

    @Override
    public BoolExpr toZ3(Context context) {
        return context.mkAnd(
                children.stream()
                        .map((child) -> child.toZ3(context))
                        .collect(Collectors.toList())
                        .toArray(new BoolExpr[0]));
    }

    @Override
    public Formula toLogicNG(FormulaFactory factory) {
        return factory.and(
                children.stream()
                        .map(child -> child.toLogicNG(factory))
                        .collect(Collectors.toList()));
    }

    @Override
    public DTree flattenNot() {
        return new DAnd(children.stream().map(DTree::flattenNot).collect(Collectors.toList()));
    }
}
