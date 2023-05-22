package com.ge.verdict.synthesis.dtree;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;

import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;

import java.util.Optional;

/** A node in a defense tree. */
public interface DTree {
    /**
     * Pretty-print this tree to a string.
     *
     * @return the pretty-printed string
     */
    public String prettyPrint();

    /**
     * Convert to Z3 expression for single cyber requirement.
     *
     * @deprecated use the multi-requirement approach instead
     */
    @Deprecated
    public BoolExpr toZ3(Context context);

    /** Convert to Z3 expression for multiple cyber requirements. */
    public BoolExpr toZ3Multi(Context context);

    /**
     * Convert to LogicNG formula for single cyber requirement.
     *
     * @deprecated use the multi-requirement approach instead
     */
    @Deprecated
    public Formula toLogicNG(FormulaFactory factory);

    /**
     * Perform any necessary tree transformations and return as a new tree. Returning
     * Optional.empty() indicates that the node should be removed.
     */
    public Optional<DTree> prepare();
}
