package com.ge.verdict.synthesis.dtree;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;

public interface DTree {
    public String prettyPrint();

    public BoolExpr smt(Context context);

	public DTree flattenNot();
}
