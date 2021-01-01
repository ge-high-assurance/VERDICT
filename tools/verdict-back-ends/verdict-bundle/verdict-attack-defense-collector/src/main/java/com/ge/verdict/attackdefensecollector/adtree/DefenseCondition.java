package com.ge.verdict.attackdefensecollector.adtree;

import com.ge.verdict.attackdefensecollector.CutSetGenerator.Cache;
import com.ge.verdict.attackdefensecollector.IndentedStringBuilder;
import com.ge.verdict.attackdefensecollector.Prob;
import com.ge.verdict.attackdefensecollector.model.Attackable;
import java.util.Objects;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;

/**
 * Represents the situation where an attack is dependent on the implementation of a defense. This
 * structure does not store the attack itself; instead, it is embedded in the attack-defense tree in
 * a conjunction next to the dependent attack.
 */
public class DefenseCondition extends ADTree {
    /** The attackable (component/defense) to which this defense condition applies. */
    private Attackable attackable;
    /** The defense property which, when implemented, triggers this defense condition. */
    private String defenseProperty;
    /** The minimum implemented DAL to trigger this defense condition. */
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

    /**
     * Retrieves the actual implemented DAL in the model.
     *
     * @return
     */
    public int getImplDal() {
        String dalStr = attackable.getParentAttributes().get(defenseProperty);
        if (dalStr != null) {
            try {
                return Integer.parseInt(dalStr);
            } catch (NumberFormatException e) {
                throw new RuntimeException(
                        "invalid DAL: "
                                + dalStr
                                + " for defense property "
                                + defenseProperty
                                + " on component/connection "
                                + attackable.getParentName());
            }
        } else {
            // if it's not in the attributes list, then it must be unimplemented
            return 0;
        }
    }

    @Override
    public Prob compute() {
        return getImplDal() >= minImplDal ? Prob.certain() : Prob.impossible();
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
        return getImplDal() >= minImplDal ? factory.verum() : factory.falsum();
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
