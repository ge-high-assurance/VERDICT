package com.ge.verdict.synthesis.dtree;

import com.ge.verdict.attackdefensecollector.adtree.Attack;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import java.util.Objects;
import java.util.Optional;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;

public class ALeaf implements DTree {
    private Attack attack;
    private boolean mitigated;

    public ALeaf(Attack attack) {
        this.attack = attack;
    }

    public void setMitigated() {
        mitigated = true;
    }

    public Attack getAttack() {
        return attack;
    }

    public boolean isMitigated() {
        return mitigated;
    }

    @Override
    public String prettyPrint() {
        return "attack(" + attack.toString() + ")";
    }

    @Override
    public BoolExpr toZ3(Context context) {
        if (isMitigated()) {
            throw new RuntimeException(
                    "mitigated ALeaf should not be present, did you call prepare?");
        }

        // an unmitigated attack is a thorn in our side
        return context.mkBool(false);
    }

    @Override
    public BoolExpr toZ3Multi(Context context) {
        return toZ3(context);
    }

    @Override
    public Formula toLogicNG(FormulaFactory factory) {
        if (isMitigated()) {
            throw new RuntimeException(
                    "mitigated ALeaf should not be present, did you call prepare?");
        }

        // an unmitigated attack is a thorn in our side
        return factory.constant(false);
    }

    @Override
    public Optional<DTree> prepare() {
        return isMitigated() ? Optional.empty() : Optional.of(this);
    }

    @Override
    public String toString() {
        return prettyPrint();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ALeaf && ((ALeaf) other).attack.equals(attack);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attack);
    }
}
