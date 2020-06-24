package com.ge.verdict.synthesis.dtree;

import com.ge.verdict.synthesis.util.Pair;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;

/**
 * Represents a component-defense pair. Each component-defense pair is represented by a unique
 * instance.
 */
public class DLeaf implements DTree {
    public final int id;
    public final String component;
    public final String defenseProperty;
    public final int cost;
    /** This is purely informative, but should not distinguish different leaves. */
    public final String attack;

    public static final class Factory {
        private final Map<Pair<String, String>, DLeaf> componentDefenseMap = new LinkedHashMap<>();

        private int idCounter = 0;
        private final Map<Integer, DLeaf> idMap = new HashMap<>();

        public DLeaf createIfNeeded(
                String component, String defenseProperty, String attack, int cost) {
            Pair<String, String> key = new Pair<>(component, defenseProperty);
            if (!componentDefenseMap.containsKey(key)) {
                DLeaf dleaf = new DLeaf(component, defenseProperty, attack, cost, idCounter++);
                idMap.put(dleaf.id, dleaf);
                componentDefenseMap.put(key, dleaf);
            }
            return componentDefenseMap.get(key);
        }

        public DLeaf fromId(int id) {
            DLeaf leaf = idMap.get(id);
            if (leaf != null) {
                return leaf;
            } else {
                throw new UndefinedIdException("Undefined ID: " + id);
            }
        }

        public Collection<DLeaf> allLeaves() {
            return componentDefenseMap.values();
        }
    }

    private DLeaf(String component, String defenseProperty, String attack, int cost, int id) {
        this.component = component;
        this.defenseProperty = defenseProperty;
        this.attack = attack;
        this.cost = cost;
        this.id = id;
    }

    public static class UndefinedIdException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public UndefinedIdException(String message) {
            super(message);
        }
    }

    private String smtName() {
        return "d" + id;
    }

    @Override
    public String prettyPrint() {
        return smtName() + "=(" + component + "," + defenseProperty + ")";
    }

    @Override
    public BoolExpr toZ3(Context context) {
        return context.mkBoolConst(smtName());
    }

    @Override
    public Variable toLogicNG(FormulaFactory factory) {
        return factory.variable(smtName());
    }

    @Override
    public DTree flattenNot() {
        return this;
    }

    @Override
    public String toString() {
        return prettyPrint();
    }
}
