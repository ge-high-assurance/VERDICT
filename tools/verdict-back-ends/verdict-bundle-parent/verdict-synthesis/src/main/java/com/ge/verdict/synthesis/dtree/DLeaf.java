package com.ge.verdict.synthesis.dtree;

import com.ge.verdict.synthesis.CostModel;
import com.ge.verdict.synthesis.util.Pair;
import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.RatNum;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.math3.fraction.Fraction;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;

/**
 * Represents a leaf of the defense tree. Encapsulates a reference to a ComponentDefense that is
 * unique to each component-defense pair.
 *
 * <p>The only difference between different instances of a component-defense pair in the tree is
 * potentially the target DAL. As such that is the only field in the DLeaf class, everything else is
 * in ComponentDefense.
 */
public class DLeaf implements DTree {
    /** The unique component-defense pair. */
    public final ComponentDefense componentDefense;
    /** The target DAL of this leaf. */
    public final int targetDal;

    public static final class ComponentDefense {
        public final int id;
        public final String component;
        public final String defenseProperty;
        /** The DAL of the current implementation, 0 if no implemented property. */
        public final int implDal;
        /** This is purely informative, but should not distinguish different leaves. */
        public final String attack;

        private final Fraction[] costs;
        private Map<Fraction, Integer> rawCostToDal;

        private Map<Integer, Integer> dalToNormCost;
        private Map<Integer, Integer> normCostToDal;

        private ComponentDefense(
                String component,
                String defenseProperty,
                String attack,
                int implDal,
                int id,
                Fraction[] costs) {
            this.component = component;
            this.defenseProperty = defenseProperty;
            this.attack = attack;
            this.implDal = implDal;
            this.id = id;
            this.costs = new Fraction[10];
            if (costs.length != 10) {
                throw new RuntimeException(
                        "invalid costs array, contains " + costs.length + " items instead of 10");
            }
            rawCostToDal = new LinkedHashMap<>();
            for (int dal = 0; dal < this.costs.length; dal++) {
                this.costs[dal] = costs[dal];
                rawCostToDal.put(this.costs[dal], dal);
            }
        }

        public void normalizeCosts(int[] normalizedCosts) {
            dalToNormCost = new LinkedHashMap<>();
            normCostToDal = new LinkedHashMap<>();
            if (normalizedCosts.length != 10) {
                throw new RuntimeException(
                        "invalid normalized costs array, contains "
                                + normalizedCosts.length
                                + " items instead of 10");
            }
            for (int dal = 0; dal < normalizedCosts.length; dal++) {
                if (normalizedCosts[dal] < 0) {
                    throw new RuntimeException(
                            "trying to set negative normalized cost "
                                    + normalizedCosts[dal]
                                    + " for "
                                    + toString()
                                    + ", DAL "
                                    + dal);
                }
                dalToNormCost.put(dal, normalizedCosts[dal]);
                normCostToDal.put(normalizedCosts[dal], dal);
            }
        }

        public Fraction dalToRawCost(int dal) {
            return costs[dal];
        }

        public int rawCostToDal(Fraction rawCost) {
            Integer val = rawCostToDal.get(rawCost);
            if (val == null) {
                throw new RuntimeException("invalid raw cost: " + rawCost);
            }
            return val;
        }

        public int dalToNormCost(int dal) {
            Integer val = dalToNormCost.get(dal);
            if (val == null) {
                throw new RuntimeException("invalid dal: " + dal);
            }
            return val;
        }

        public int normCostToDal(int normCost) {
            Integer val = normCostToDal.get(normCost);
            if (val == null) {
                StringBuilder valid = new StringBuilder();
                for (Integer key : normCostToDal.keySet()) {
                    valid.append(key);
                    valid.append(",");
                }
                throw new RuntimeException(
                        "invalid normCost: " + normCost + ", valid: " + valid.toString());
            }
            return val;
        }

        private String smtName() {
            return "d" + id;
        }

        public BoolExpr toZ3(Context context) {
            return context.mkBoolConst(smtName());
        }

        public ArithExpr toZ3Multi(Context context) {
            return context.mkRealConst(smtName());
        }

        public Variable toLogicNG(FormulaFactory factory) {
            return factory.variable(smtName());
        }

        @Override
        public String toString() {
            return smtName() + "=(" + component + "," + defenseProperty + "," + implDal + ")";
        }
    }

    public static final class Factory {
        private final Map<Pair<String, String>, ComponentDefense> componentDefenseMap =
                new LinkedHashMap<>();

        private int idCounter = 0;
        private final Map<Integer, ComponentDefense> idMap = new HashMap<>();

        private ComponentDefense createIfNeeded(
                String component,
                String defenseProperty,
                String attack,
                int implDal,
                Fraction[] costs) {
            Pair<String, String> key = new Pair<>(component, defenseProperty);
            if (!componentDefenseMap.containsKey(key)) {
                ComponentDefense pair =
                        new ComponentDefense(
                                component, defenseProperty, attack, implDal, idCounter++, costs);
                idMap.put(pair.id, pair);
                componentDefenseMap.put(key, pair);
            }
            return componentDefenseMap.get(key);
        }

        public Optional<ComponentDefense> lookup(String component, String defense) {
            return Optional.ofNullable(componentDefenseMap.get(new Pair<>(component, defense)));
        }

        public ComponentDefense fromId(int id) {
            ComponentDefense leaf = idMap.get(id);
            if (leaf != null) {
                return leaf;
            } else {
                throw new UndefinedIdException("Undefined ID: " + id);
            }
        }

        public Collection<ComponentDefense> allComponentDefensePairs() {
            return componentDefenseMap.values();
        }
    }

    public DLeaf(
            String component,
            String defenseProperty,
            String attack,
            int implDal,
            int targetDal,
            Fraction[] costs,
            Factory factory) {

        componentDefense =
                factory.createIfNeeded(component, defenseProperty, attack, implDal, costs);
        this.targetDal = targetDal;
    }

    public DLeaf(
            String component,
            String defenseProperty,
            String attack,
            int implDal,
            int targetDal,
            CostModel costModel,
            Factory factory,
            boolean usePartialSolution,
            boolean meritAssignment) {

        Fraction implCost = costModel.cost(defenseProperty, component, implDal);

        Fraction[] costs = new Fraction[10];
        for (int dal = 0; dal < costs.length; dal++) {
            Fraction targetCost = costModel.cost(defenseProperty, component, dal);
            // this handles if implDal > targetDal
            if (usePartialSolution && !meritAssignment) {
                Fraction difference = targetCost.subtract(implCost);
                if (difference.compareTo(new Fraction(0)) > 0) {
                    costs[dal] = difference;
                } else {
                    costs[dal] = new Fraction(0);
                }
            } else {
                costs[dal] = targetCost;
            }
        }

        componentDefense =
                factory.createIfNeeded(component, defenseProperty, attack, implDal, costs);
        this.targetDal = targetDal;
    }

    public static class UndefinedIdException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public UndefinedIdException(String message) {
            super(message);
        }
    }

    private String smtName() {
        return "d" + componentDefense.id;
    }

    @Override
    public String prettyPrint() {
        return smtName()
                + "=("
                + componentDefense.component
                + ","
                + componentDefense.defenseProperty
                + ","
                + componentDefense.implDal
                + "/"
                + targetDal
                + ")";
    }

    @Override
    public BoolExpr toZ3(Context context) {
        return componentDefense.toZ3(context);
    }

    @Override
    public BoolExpr toZ3Multi(Context context) {
        ArithExpr var = componentDefense.toZ3Multi(context);
        return context.mkGe(var, fractionToZ3(componentDefense.dalToRawCost(targetDal), context));
    }

    @Override
    public Variable toLogicNG(FormulaFactory factory) {
        return componentDefense.toLogicNG(factory);
    }

    @Override
    public Optional<DTree> prepare() {
        return Optional.of(this);
    }

    @Override
    public String toString() {
        return prettyPrint();
    }

    public static RatNum fractionToZ3(Fraction fraction, Context context) {
        return context.mkReal(fraction.getNumerator(), fraction.getDenominator());
    }
}
