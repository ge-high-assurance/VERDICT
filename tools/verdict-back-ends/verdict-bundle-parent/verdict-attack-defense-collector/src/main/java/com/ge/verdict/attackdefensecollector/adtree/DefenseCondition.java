package com.ge.verdict.attackdefensecollector.adtree;

import com.ge.verdict.attackdefensecollector.CutSetGenerator.Cache;
import com.ge.verdict.attackdefensecollector.IndentedStringBuilder;
import com.ge.verdict.attackdefensecollector.Prob;
import com.ge.verdict.attackdefensecollector.model.Attackable;
import java.util.Objects;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;

public class DefenseCondition extends ADTree {
    private Attackable attackable;
    private String defenseProperty;
    private int minImplDal;

    public DefenseCondition(Attackable attackable, String defenseProperty, int minImplDal) {
        this.attackable = attackable;
        this.defenseProperty = defenseProperty;
        this.minImplDal = minImplDal;
    }

    public Attackable getAttackable() {
        return attackable;
    }

    public String getDefenseProperty() {
        return defenseProperty;
    }

    public int getMinImplDal() {
        return minImplDal;
    }

    @Override
    public Prob compute() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ADTree crush() {
        return this;
    }

    @Override
    public void prettyPrint(IndentedStringBuilder builder) {
        builder.append("{");
        builder.append(attackable.getParentName());
        builder.append(":");
        builder.append(defenseProperty);
        builder.append(" >= ");
        builder.append(minImplDal);
        builder.append("}");
    }

    @Override
    public Formula toLogicNg(FormulaFactory factory, Cache cache) {
        throw new RuntimeException(
                "minimization not implemented for trees with defense conditions");
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof DefenseCondition) {
            DefenseCondition otherDefCond = (DefenseCondition) other;
            return attackable.getParentName().equals(otherDefCond.getAttackable().getParentName())
                    && defenseProperty.equals(otherDefCond.getDefenseProperty())
                    && minImplDal == otherDefCond.getMinImplDal();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(attackable.getParentName(), defenseProperty, minImplDal);
    }
}