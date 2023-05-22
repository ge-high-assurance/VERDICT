package com.ge.verdict.attackdefensecollector;

import com.ge.verdict.attackdefensecollector.adtree.ADAnd;
import com.ge.verdict.attackdefensecollector.adtree.ADNot;
import com.ge.verdict.attackdefensecollector.adtree.ADOr;
import com.ge.verdict.attackdefensecollector.adtree.ADTree;
import com.ge.verdict.attackdefensecollector.adtree.Attack;
import com.ge.verdict.attackdefensecollector.adtree.Defense;

import org.logicng.formulas.And;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Not;
import org.logicng.formulas.Or;
import org.logicng.formulas.Variable;
import org.logicng.transformations.dnf.DNFFactorization;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Generate cut sets. Not currently used. */
public class CutSetGenerator {
    /**
     * Cache used internally by the cut set generator. Prevents creation of duplicate LogicNG
     * variables and allows for reconstructing the attack-defense tree at the end.
     */
    public static class Cache {
        public Map<Attack, Variable> attackToVar = new LinkedHashMap<>();
        public Map<String, Attack> varToAttack = new LinkedHashMap<>();
        public Map<Defense, Variable> defenseToVar = new LinkedHashMap<>();
        public Map<String, Defense> varToDefense = new LinkedHashMap<>();
    }

    /**
     * Perform cut set generation for the given attack-defense tree.
     *
     * @param adtree
     * @return
     */
    public static ADTree generate(ADTree adtree) {
        FormulaFactory factory = new FormulaFactory();
        Cache cache = new Cache();

        Formula formula = adtree.toLogicNg(factory, cache);

        long startTime = System.currentTimeMillis();

        // this is terribly inefficient for any non-trivial system
        // and it has not yet been observed to terminate
        // Formula minimal = QuineMcCluskeyAlgorithm.compute(formula);

        // this should be inefficient too, but it finishes trivially for trees already in DNF form
        // not yet tested on non-DNF trees because we don't have a model that produces one
        Formula minimal = (new DNFFactorization()).apply(formula, false);

        // for comparing approaches
        System.out.println(
                "converted to DNF in " + (System.currentTimeMillis() - startTime) + " ms");

        return extract(minimal, cache);
    }

    /**
     * Converts a LogicNG formula back into an attack-defense tree.
     *
     * @param formula
     * @param cache
     * @return
     */
    private static ADTree extract(Formula formula, Cache cache) {
        if (formula instanceof And) {
            List<ADTree> children = new ArrayList<>();
            for (Formula child : formula) {
                children.add(extract(child, cache));
            }
            return new ADAnd(children);
        } else if (formula instanceof Or) {
            List<ADTree> children = new ArrayList<>();
            for (Formula child : formula) {
                children.add(extract(child, cache));
            }
            return new ADOr(children);
        } else if (formula instanceof Not) {
            return new ADNot(extract(((Not) formula).operand(), cache));
        } else if (formula instanceof Literal) {
            String name = ((Literal) formula).name();
            if (cache.varToAttack.containsKey(name)) {
                return cache.varToAttack.get(name);
            } else if (cache.varToDefense.containsKey(name)) {
                return cache.varToDefense.get(name);
            } else {
                throw new RuntimeException("got an unknown literal: " + name);
            }
        } else {
            throw new RuntimeException(
                    "got unexpected dnf node: " + formula.getClass().getCanonicalName());
        }
    }
}
