package com.ge.verdict.synthesis.dtree;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;

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
    public BoolExpr smt(Context context) {
        return context.mkAnd(
                children.stream()
                        .map((child) -> child.smt(context))
                        .collect(Collectors.toList())
                        .toArray(new BoolExpr[0]));
    }

	@Override
	public DTree flattenNot() {
		return new DAnd(children.stream().map(DTree::flattenNot).collect(Collectors.toList()));
	}
}
