package com.ge.verdict.synthesis.dtree;

import com.ge.verdict.synthesis.ICostModel;
import com.ge.verdict.synthesis.util.Pair;
import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.RatNum;

import org.apache.commons.math3.fraction.Fraction;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

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

    /**
     * A component-defense pair, divorced from a concrete place in the tree.
     *
     * <p>You cannot construct this directly. Instead, construct a DLeaf and a new instance of this
     * will be encapsulated by the constructed DLeaf.
     */
    public static final class ComponentDefense {
        /** The unique ID used for encoding to SMT. */
        public final int id;
        /** The component. */
        public final String component;
        /** The defense property. */
        public final String defenseProperty;
        /** The DAL of the current implementation, 0 if no implemented property. */
        public final int implDal;
        /** This is purely informative, but should not distinguish different leaves. */
        public final String attack;

        /** The raw fractional costs (indexed by DAL). */
        private final Fraction[] costs;
        /** Reverse-lookup of the raw fractional costs. */
        private Map<Fraction, Integer> rawCostToDal;

        /**
         * Normalized costs lookup table, used only in the single-requirement version of synthesis.
         */
        private Map<Integer, Integer> dalToNormCost;
        /**
         * Reverse of normalized costs lookup table, used only in the single-requirement version of
         * synthesis.
         */
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
            /*
             * constructing in this way guarantees that if two DALs have the same cost,
             * the higher of the two will be produced.
             */
            for (int dal = 0; dal < this.costs.length; dal++) {
                this.costs[dal] = costs[dal];
                // we override a previous DAL with this cost if such a thing happens
                rawCostToDal.put(this.costs[dal], dal);
            }
        }

        /**
         * Assign normalized costs.
         *
         * @param normalizedCosts
         * @deprecated use the multi-requirement version of synthesis instead
         */
        @Deprecated
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

        /**
         * Get the raw fractional cost associated with a DAL.
         *
         * @param dal
         * @return
         */
        public Fraction dalToRawCost(int dal) {
            return costs[dal];
        }

        /**
         * Get the DAL associated with a raw fractional cost.
         *
         * @param rawCost
         * @return
         */
        public int rawCostToDal(Fraction rawCost) {
            Integer val = rawCostToDal.get(rawCost);
            if (val == null) {
                throw new RuntimeException("invalid raw cost: " + rawCost);
            }
            return val;
        }

        /**
         * @param dal
         * @return
         * @deprecated use the multi-requirement version of synthesis instead
         */
        @Deprecated
        public int dalToNormCost(int dal) {
            Integer val = dalToNormCost.get(dal);
            if (val == null) {
                throw new RuntimeException("invalid dal: " + dal);
            }
            return val;
        }

        /**
         * @param normCost
         * @return
         * @deprecated use the multi-requirement version of synthesis instead
         */
        @Deprecated
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

        /**
         * @return the name used for encoding this component-defense pair into SMT
         */
        private String smtName() {
            return "d" + id;
        }

        /**
         * Construct the Z3 object used for the MaxSMT single requirement version of synthesis.
         *
         * @param context
         * @return
         * @deprecated use the multi-requirement version of synthesis instead
         */
        @Deprecated
        public BoolExpr toZ3(Context context) {
            return context.mkBoolConst(smtName());
        }

        /**
         * Construct the Z3 object used for the multi-requirement version of synthesis.
         *
         * @param context
         * @return
         */
        public ArithExpr toZ3Multi(Context context) {
            return context.mkRealConst(smtName());
        }

        /**
         * Construct the LogicNG object used for the MaxSAT single requirement version of synthesis.
         *
         * @param factory
         * @return
         * @deprecated use the multi-requirement version of synthesis instead
         */
        @Deprecated
        public Variable toLogicNG(FormulaFactory factory) {
            return factory.variable(smtName());
        }

        @Override
        public String toString() {
            return smtName() + "=(" + component + "," + defenseProperty + "," + implDal + ")";
        }
    }

    /**
     * Produces component-defense pairs and keeps track of all such constructed component-defense
     * pairs.
     */
    public static final class Factory {
        /** the map from component/defense to constructed component-defense pairs */
        private final Map<Pair<String, String>, ComponentDefense> componentDefenseMap =
                new LinkedHashMap<>();
        /** use incremental IDs to produce unique SMT names */
        private int idCounter = 0;
        /** the map from IDs to constructed component-defense pairs */
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

    /**
     * This constructor is used for debugging.
     *
     * <p>Construct a new DLeaf. Will reuse an existing component-defense pair if it already exists
     * in the factory.
     *
     * @param component the name of the component
     * @param defenseProperty the defense property
     * @param attack the name of the attack defended by the defense
     * @param implDal the current implemented DAL
     * @param targetDal the target DAL (based on cyber requirement severity)
     * @param costs the raw fractional costs, indexed by DAL
     * @param factory the factory to use
     */
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

    /**
     * Construct a new DLeaf. Will reuse an existing component-defense pair if it already exists in
     * the factory.
     *
     * @param component the name of the component
     * @param defenseProperty the defense property
     * @param attack the name of the attack defended by the defense
     * @param implDal the current implemented DAL
     * @param targetDal the target DAL (based on cyber requirement severity)
     * @param costModel the cost model to use for obtaining costs
     * @param factory the factory to use
     * @param usePartialSolution whether we are using partial solutions
     * @param meritAssignment whether we are performing merit assignment
     */
    public DLeaf(
            String component,
            String defenseProperty,
            String attack,
            int implDal,
            int targetDal,
            ICostModel costModel,
            Factory factory,
            boolean usePartialSolution,
            boolean meritAssignment) {

        Fraction implCost = costModel.getCost(defenseProperty, component, implDal);

        // construct each cost
        Fraction[] costs = new Fraction[10];
        for (int dal = 0; dal < costs.length; dal++) {
            Fraction currentDALCost = costModel.getCost(defenseProperty, component, dal);
            if (usePartialSolution && !meritAssignment) {
                // in the partial solutions (but no merit assignment) case, we treat
                // implemented defenses as a sunk cost
                Fraction difference = currentDALCost.subtract(implCost);
                if (difference.compareTo(new Fraction(0)) > 0) {
                    costs[dal] = difference;
                } else {
                    // don't have negative cost
                    costs[dal] = new Fraction(0);
                }
            } else {
                costs[dal] = currentDALCost;
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
