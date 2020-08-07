package com.ge.verdict.synthesis.dtree;

import com.ge.verdict.attackdefensecollector.adtree.DefenseCondition;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import java.util.Objects;
import java.util.Optional;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;

public class DCondition implements DTree {
    public final DefenseCondition defenseCond;
    private DLeaf.ComponentDefense compDef;

    public DCondition(DefenseCondition defenseCond) {
        this.defenseCond = defenseCond;
    }

    public void setCompDef(DLeaf.ComponentDefense compDef) {
        if (this.compDef != null) {
            throw new RuntimeException("compDef has already been set");
        }
        if (!(compDef.component.equals(defenseCond.getAttackable().getParentName())
                && compDef.defenseProperty.equals(defenseCond.getDefenseProperty()))) {
            throw new RuntimeException("compDef and defenseCond do not match");
        }
        this.compDef = compDef;
    }

    @Override
    public String prettyPrint() {
        return "{"
                + defenseCond.getAttackable().getParentName()
                + ":"
                + defenseCond.getDefenseProperty()
                + " >= "
                + defenseCond.getMinImplDal()
                + "}";
    }

    @Override
    public BoolExpr toZ3(Context context) {
        throw new RuntimeException("DCondition only supported by Z3-multi");
    }

    @Override
    public BoolExpr toZ3Multi(Context context) {
        if (compDef != null) {
            // Note that we use < instead of >= because the defense tree is inverted compared to the
            // attack-defense tree
            return context.mkLt(
                    compDef.toZ3Multi(context),
                    DLeaf.fractionToZ3(compDef.dalToRawCost(defenseCond.getMinImplDal()), context));
        } else {
            throw new RuntimeException("DCondition missing comp def: " + prettyPrint());
        }
    }

    @Override
    public Formula toLogicNG(FormulaFactory factory) {
        throw new RuntimeException("DCondition only supported by Z3-multi");
    }

    @Override
    public Optional<DTree> prepare() {
        return Optional.of(this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(defenseCond);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DCondition && defenseCond.equals(((DCondition) other).defenseCond);
    }
}
