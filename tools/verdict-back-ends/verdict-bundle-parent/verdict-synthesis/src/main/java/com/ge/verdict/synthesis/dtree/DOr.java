package com.ge.verdict.synthesis.dtree;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class DOr implements DTree {
    public final List<DTree> children;

    public DOr(List<DTree> children) {
        this.children = Collections.unmodifiableList(children);
    }

    @Override
    public String prettyPrint() {
        return "("
                + children.stream().map(DTree::prettyPrint).collect(Collectors.joining(" v "))
                + ")";
    }

    @Override
    public BoolExpr smt(Context context) {
        return context.mkOr(
                children.stream()
                        .map((child) -> child.smt(context))
                        .collect(Collectors.toList())
                        .toArray(new BoolExpr[0]));
    }
}
