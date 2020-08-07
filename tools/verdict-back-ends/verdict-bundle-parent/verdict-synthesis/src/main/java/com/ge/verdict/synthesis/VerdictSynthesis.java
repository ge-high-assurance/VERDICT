package com.ge.verdict.synthesis;

import com.ge.verdict.synthesis.dtree.DLeaf;
import com.ge.verdict.synthesis.dtree.DLeaf.ComponentDefense;
import com.ge.verdict.synthesis.dtree.DTree;
import com.ge.verdict.synthesis.util.Pair;
import com.ge.verdict.vdm.synthesis.ResultsInstance;
import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Model;
import com.microsoft.z3.Optimize;
import com.microsoft.z3.RatNum;
import com.microsoft.z3.Status;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

    public static Optional<ResultsInstance> performSynthesisMultiple(
            DTree tree,
            DLeaf.Factory factory,
            CostModel costModel,
            boolean partialSolution,
            boolean inputSat,
            boolean meritAssignment,
            boolean dumpSmtLib) {
        Context context = new Context();
        Optimize optimizer = context.mkOptimize();

        System.out.println(
                "performSynthesisMultiple, configuration: partialSolution="
                        + partialSolution
                        + ", inputSat="
                        + inputSat
                        + ", meritAssignment="
                        + meritAssignment);

        Collection<ComponentDefense> pairs = factory.allComponentDefensePairs();

        optimizer.Assert(tree.toZ3Multi(context));

        if (meritAssignment) {
            optimizer.Assert(
                    context.mkAnd(
                            pairs.stream()
                                    .map(
                                            pair ->
                                                    context.mkLe(
                                                            pair.toZ3Multi(context),
                                                            DLeaf.fractionToZ3(
                                                                    pair.dalToRawCost(pair.implDal),
                                                                    context)))
                                    .collect(Collectors.toList())
                                    .toArray(new BoolExpr[] {})));
        }

        optimizer.Assert(
                // This assumes that DAL 0 is the smallest cost. Which will be true if
                // the cost function is monotone with respect to DAL, which it should be.
                // If for whatever reason we want to support non-monotone cost with respect
                // to DAL, then this can be changed to the minimum of all costs.
                context.mkAnd(
                        pairs.stream()
                                .map(
                                        pair ->
                                                context.mkGe(
                                                        pair.toZ3Multi(context),
                                                        DLeaf.fractionToZ3(
                                                                pair.dalToRawCost(0), context)))
                                .collect(Collectors.toList())
                                .toArray(new BoolExpr[] {})));

        optimizer.MkMinimize(
                context.mkAdd(
                        pairs.stream()
                                .map(pair -> pair.toZ3Multi(context))
                                .collect(Collectors.toList())
                                .toArray(new ArithExpr[] {})));

        if (dumpSmtLib) {
            try {
                PrintWriter writer = new PrintWriter("verdict-synthesis-dump.smtlib", "UTF-8");
                writer.println(optimizer.toString());
                writer.flush();
                writer.close();
            } catch (FileNotFoundException | UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        if (optimizer.Check().equals(Status.SATISFIABLE)) {
            List<ResultsInstance.Item> items = new ArrayList<>();
            Fraction totalInputCost = new Fraction(0), totalOutputCost = new Fraction(0);
            Model model = optimizer.getModel();
            for (ComponentDefense pair : pairs) {
                RatNum expr = (RatNum) model.eval(pair.toZ3Multi(context), true);
                Fraction rawCost =
                        new Fraction(expr.getNumerator().getInt(), expr.getDenominator().getInt());
                int dal = pair.rawCostToDal(rawCost);

                Fraction inputCost =
                        costModel.cost(pair.defenseProperty, pair.component, pair.implDal);
                Fraction outputCost = costModel.cost(pair.defenseProperty, pair.component, dal);

                totalInputCost = totalInputCost.add(inputCost);
                totalOutputCost = totalOutputCost.add(outputCost);

                items.add(
                        new ResultsInstance.Item(
                                pair.component,
                                pair.defenseProperty,
                                pair.implDal,
                                dal,
                                inputCost,
                                outputCost));
            }
            return Optional.of(
                    new ResultsInstance(
                            partialSolution,
                            meritAssignment,
                            inputSat,
                            totalInputCost,
                            totalOutputCost,
                            items));
        } else {
            System.err.println(
                    "Synthesis: SMT not satisfiable, perhaps there are unmitigatable attacks");
            return Optional.empty();
        }
    }

    public static Fraction totalImplCost(DLeaf.Factory factory) {
        return factory.allComponentDefensePairs().stream()
                .map(pair -> pair.dalToRawCost(pair.implDal))
                .reduce(Fraction.ZERO, Fraction::add);
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

    public static ResultsInstance addExtraImplDefenses(
            ResultsInstance results,
            Map<com.ge.verdict.attackdefensecollector.Pair<String, String>, Integer>
                    implCompDefPairs,
            CostModel costModel) {

        List<ResultsInstance.Item> items = new ArrayList<>(results.items);
        Fraction inputCost = results.inputCost;
        Fraction outputCost = results.outputCost;

        Set<com.ge.verdict.attackdefensecollector.Pair<String, String>> accountedCompDefPairs =
                results.items.stream()
                        .map(
                                item ->
                                        new com.ge.verdict.attackdefensecollector.Pair<>(
                                                item.component, item.defenseProperty))
                        .collect(Collectors.toSet());

        for (Map.Entry<com.ge.verdict.attackdefensecollector.Pair<String, String>, Integer> entry :
                implCompDefPairs.entrySet()) {
            if (!accountedCompDefPairs.contains(entry.getKey())) {
                String comp = entry.getKey().left;
                String defProp = entry.getKey().right;
                int implDal = entry.getValue();

                Fraction pairInputCost = costModel.cost(defProp, comp, implDal);
                Fraction pairOutputCost = costModel.cost(defProp, comp, 0);

                items.add(
                        new ResultsInstance.Item(
                                comp, defProp, implDal, 0, pairInputCost, pairOutputCost));
                inputCost = inputCost.add(pairInputCost);
                // this may seem silly, but in theory we can have a non-zero cost for DAL 0
                outputCost = outputCost.add(pairOutputCost);
            }
        }

        return new ResultsInstance(
                results.partialSolution,
                results.meritAssignment,
                results.inputSat,
                inputCost,
                outputCost,
                items);
    }
}
