package com.ge.verdict.synthesis;

import com.ge.verdict.synthesis.dtree.DLeaf;
import com.ge.verdict.synthesis.dtree.DLeaf.ComponentDefense;
import com.ge.verdict.synthesis.dtree.DTree;
import com.ge.verdict.synthesis.util.Pair;
import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntNum;
import com.microsoft.z3.Model;
import com.microsoft.z3.Optimize;
import com.microsoft.z3.Status;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.math3.fraction.Fraction;
import org.apache.commons.math3.util.ArithmeticUtils;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.solvers.MaxSATSolver;

public class VerdictSynthesis {
    public static enum Approach {
        MAXSMT,
        MAXSAT
    }

    public static Optional<Pair<List<Pair<ComponentDefense, Integer>>, Double>>
            performSynthesisMultiple(DTree tree, DLeaf.Factory factory) {
        Context context = new Context();
        Optimize optimizer = context.mkOptimize();

        Collection<ComponentDefense> pairs = factory.allComponentDefensePairs();

        int costLcd = normalizeCosts(pairs);

        optimizer.Assert(tree.toZ3Multi(context));

        optimizer.Assert(
                context.mkAnd(
                        pairs.stream()
                                .map(
                                        pair ->
                                                context.mkGe(
                                                        pair.toZ3Multi(context), context.mkInt(0)))
                                .collect(Collectors.toList())
                                .toArray(new BoolExpr[] {})));

        optimizer.MkMinimize(
                context.mkAdd(
                        pairs.stream()
                                .map(pair -> pair.toZ3Multi(context))
                                .collect(Collectors.toList())
                                .toArray(new ArithExpr[] {})));

        if (optimizer.Check().equals(Status.SATISFIABLE)) {
            List<Pair<ComponentDefense, Integer>> output = new ArrayList<>();
            int totalNormalizedCost = 0;
            Model model = optimizer.getModel();
            for (ComponentDefense pair : pairs) {
                IntNum expr = (IntNum) model.eval(pair.toZ3Multi(context), true);
                int normCost = expr.getInt();
                int dal = pair.normCostToDal(normCost);
                totalNormalizedCost += normCost;
                output.add(new Pair<>(pair, dal));
            }

            return Optional.of(new Pair<>(output, ((double) totalNormalizedCost) / costLcd));
        } else {
            System.err.println(
                    "Synthesis: SMT not satisfiable, perhaps there are unmitigatable attacks");
            return Optional.empty();
        }
    }

    public static Optional<Pair<Set<ComponentDefense>, Double>> performSynthesisSingle(
            DTree tree, int targetDal, DLeaf.Factory factory, Approach approach) {
        switch (approach) {
            case MAXSMT:
                return performSynthesisMaxSmt(tree, targetDal, factory);
            case MAXSAT:
                return performSynthesisMaxSat(tree, targetDal, factory);
            default:
                throw new RuntimeException("excuse me");
        }
    }

    /**
     * Calculates (and returns) lowest common denominator of all leaf costs, and sets the
     * normalizedCost field in each leaf accordingly.
     *
     * @param pairs
     * @return
     */
    public static int normalizeCosts(Collection<ComponentDefense> pairs) {
        int costLcd =
                pairs.stream()
                        .flatMap(
                                (ComponentDefense pair) ->
                                        IntStream.range(0, 10)
                                                .map(dal -> pair.dalToRawCost(dal).getDenominator())
                                                .mapToObj(x -> x)) // kind of dumb but need to go
                        // from IntStream ->
                        // Stream<Integer>
                        .reduce(1, ArithmeticUtils::lcm);

        for (ComponentDefense pair : pairs) {
            int[] normCosts = new int[10];
            for (int dal = 0; dal < 10; dal++) {
                Fraction normalizedCost = pair.dalToRawCost(dal).multiply(costLcd);
                if (normalizedCost.getDenominator() != 1) {
                    throw new RuntimeException();
                }
                normCosts[dal] = normalizedCost.getNumerator();
            }
            pair.normalizeCosts(normCosts);
        }

        return costLcd;
    }

    public static Optional<Pair<Set<ComponentDefense>, Double>> performSynthesisMaxSmt(
            DTree tree, int targetDal, DLeaf.Factory factory) {
        Context context = new Context();
        Optimize optimizer = context.mkOptimize();

        Collection<ComponentDefense> pairs = factory.allComponentDefensePairs();

        int costLcd = normalizeCosts(pairs);

        for (ComponentDefense pair : pairs) {
            // System.out.println("LEAF: " + leaf + ", COST: " + leaf.cost + " [MaxSMT]");

            if (pair.dalToNormCost(targetDal) > 0) {
                // this id ("cover") doesn't matter but we have to specify something
                optimizer.AssertSoft(
                        context.mkNot(pair.toZ3(context)), pair.dalToNormCost(targetDal), "cover");
            }
        }

        optimizer.Assert(tree.toZ3(context));

        if (optimizer.Check().equals(Status.SATISFIABLE)) {
            Set<ComponentDefense> output = new LinkedHashSet<>();
            int totalNormalizedCost = 0;
            Model model = optimizer.getModel();
            for (ComponentDefense pair : pairs) {
                Expr expr = model.eval(pair.toZ3(context), true);
                switch (expr.getBoolValue()) {
                    case Z3_L_TRUE:
                        output.add(pair);
                        totalNormalizedCost += pair.dalToNormCost(targetDal);
                        break;
                    case Z3_L_FALSE:
                        break;
                    case Z3_L_UNDEF:
                    default:
                        throw new RuntimeException(
                                "Synthesis: Undefined variable in output model: "
                                        + pair.toString());
                }
            }

            return Optional.of(new Pair<>(output, ((double) totalNormalizedCost) / costLcd));
        } else {
            System.err.println(
                    "Synthesis: SMT not satisfiable, perhaps there are unmitigatable attacks");
            return Optional.empty();
        }
    }

    public static Optional<Pair<Set<ComponentDefense>, Double>> performSynthesisMaxSat(
            DTree tree, int targetDal, DLeaf.Factory dleafFactory) {
        Collection<ComponentDefense> pairs = dleafFactory.allComponentDefensePairs();

        FormulaFactory factory = new FormulaFactory();
        MaxSATSolver solver = MaxSATSolver.wbo();

        Formula cnf = tree.toLogicNG(factory).cnf();

        int costLcd = normalizeCosts(pairs);

        for (ComponentDefense pair : pairs) {
            // System.out.println("LEAF: " + leaf + ", COST: " + leaf.cost + " [MaxSAT]");

            if (pair.dalToNormCost(targetDal) > 0) {
                solver.addSoftFormula(
                        factory.not(pair.toLogicNG(factory)), pair.dalToNormCost(targetDal));
            }
        }

        // implicitly converts formula to CNF
        solver.addHardFormula(cnf);

        switch (solver.solve()) {
            case OPTIMUM:
                Set<ComponentDefense> output = new LinkedHashSet<>();
                int totalNormalizedCost = 0;
                Assignment model = solver.model();
                for (ComponentDefense pair : pairs) {
                    if (model.evaluateLit(pair.toLogicNG(factory))) {
                        output.add(pair);
                        totalNormalizedCost += pair.dalToNormCost(targetDal);
                    }
                }
                return Optional.of(new Pair<>(output, ((double) totalNormalizedCost) / costLcd));
            case UNDEF:
                System.err.println("Synthesis: SAT undefined, is input tree valid?");
                return Optional.empty();
            case UNSATISFIABLE:
                System.err.println(
                        "Synthesis: SAT not satisfiable, perhaps there are unmitigatable attacks");
                return Optional.empty();
            default:
                throw new RuntimeException("impossible");
        }
    }
}
