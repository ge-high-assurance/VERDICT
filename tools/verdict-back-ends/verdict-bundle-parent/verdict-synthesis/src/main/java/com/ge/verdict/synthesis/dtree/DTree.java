package com.ge.verdict.synthesis.dtree;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import java.util.Optional;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;

public interface DTree {
    public String prettyPrint();

    public BoolExpr toZ3(Context context);

    public Formula toLogicNG(FormulaFactory factory);

    public Optional<DTree> prepare();
}
