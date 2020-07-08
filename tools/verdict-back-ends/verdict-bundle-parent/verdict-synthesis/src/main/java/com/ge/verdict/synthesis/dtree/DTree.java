package com.ge.verdict.synthesis.dtree;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import java.util.Optional;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;

public interface DTree {
    public String prettyPrint();

    /** Convert to Z3 expression for single cyber requirement. */
    public BoolExpr toZ3(Context context);

    /** Convert to Z3 expression for multiple cyber requirements. */
    public BoolExpr toZ3Multi(Context context);

    /** Convert to LogicNG formula for single cyber requirement. */
    public Formula toLogicNG(FormulaFactory factory);

    public Optional<DTree> prepare();
}
