package com.ge.verdict.synthesis;

import com.ge.verdict.synthesis.dtree.DLeaf;
import com.ge.verdict.synthesis.dtree.DTree;
import com.ge.verdict.synthesis.util.Pair;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Model;
import com.microsoft.z3.Optimize;
import com.microsoft.z3.Status;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
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

    public static Optional<Pair<Set<DLeaf>, Double>> performSynthesis(
            DTree tree, DLeaf.Factory factory, Approach approach) {
        switch (approach) {
            case MAXSMT:
                return performSynthesisMaxSmt(tree, factory);
            case MAXSAT:
                return performSynthesisMaxSat(tree, factory);
            default:
                throw new RuntimeException("excuse me");
        }
    }

    /**
     * Calculates (and returns) lowest common denominator of all leaf costs, and sets the
     * normalizedCost field in each leaf accordingly.
     *
     * @param leaves
     * @return
     */
    public static int normalizeCosts(Collection<DLeaf> leaves) {
        int costLcd =
                leaves.stream()
                        .map(leaf -> leaf.cost.getDenominator())
                        .reduce(1, ArithmeticUtils::lcm);

        for (DLeaf leaf : leaves) {
            Fraction normalizedCost = leaf.cost.multiply(costLcd);
            if (normalizedCost.getDenominator() != 1) {
                throw new RuntimeException();
            }
            leaf.normalizedCost = normalizedCost.getNumerator();
        }

        return costLcd;
    }

    public static Optional<Pair<Set<DLeaf>, Double>> performSynthesisMaxSmt(
            DTree tree, DLeaf.Factory factory) {
        Context context = new Context();
        Optimize optimizer = context.mkOptimize();

        Collection<DLeaf> leaves = factory.allLeaves();

        int costLcd = normalizeCosts(leaves);

        for (DLeaf leaf : leaves) {
            // System.out.println("LEAF: " + leaf + ", COST: " + leaf.cost + " [MaxSMT]");

            if (leaf.normalizedCost > 0) {
                // this id ("cover") doesn't matter but we have to specify something
                optimizer.AssertSoft(
                        context.mkNot(leaf.toZ3(context)), leaf.normalizedCost, "cover");
            }
        }

        optimizer.Assert(tree.toZ3(context));

        if (optimizer.Check().equals(Status.SATISFIABLE)) {
            Set<DLeaf> output = new LinkedHashSet<>();
            int totalNormalizedCost = 0;
            Model model = optimizer.getModel();
            for (DLeaf leaf : leaves) {
                Expr expr = model.eval(leaf.toZ3(context), true);
                switch (expr.getBoolValue()) {
                    case Z3_L_TRUE:
                        output.add(leaf);
                        totalNormalizedCost += leaf.normalizedCost;
                        break;
                    case Z3_L_FALSE:
                        break;
                    case Z3_L_UNDEF:
                    default:
                        throw new RuntimeException(
                                "Synthesis: Undefined variable in output model: "
                                        + leaf.toString());
                }
            }

            return Optional.of(new Pair<>(output, ((double) totalNormalizedCost) / costLcd));
        } else {
            System.err.println(
                    "Synthesis: SMT not satisfiable, perhaps there are unmitigatable attacks");
            return Optional.empty();
        }
    }

    public static Optional<Pair<Set<DLeaf>, Double>> performSynthesisMaxSat(
            DTree tree, DLeaf.Factory dleafFactory) {
        Collection<DLeaf> leaves = dleafFactory.allLeaves();

        FormulaFactory factory = new FormulaFactory();
        // if no soft clauses then the weighted version fails for some reason
        MaxSATSolver solver = leaves.isEmpty() ? MaxSATSolver.msu3() : MaxSATSolver.wmsu3();

        Formula cnf = tree.toLogicNG(factory).cnf();

        int costLcd = normalizeCosts(leaves);

        for (DLeaf leaf : leaves) {
            // System.out.println("LEAF: " + leaf + ", COST: " + leaf.cost + " [MaxSAT]");

            if (leaf.normalizedCost > 0) {
                solver.addSoftFormula(factory.not(leaf.toLogicNG(factory)), leaf.normalizedCost);
            }
        }

        // implicitly converts formula to CNF
        solver.addHardFormula(cnf);

        switch (solver.solve()) {
            case OPTIMUM:
                Set<DLeaf> output = new LinkedHashSet<>();
                int totalNormalizedCost = 0;
                Assignment model = solver.model();
                for (DLeaf leaf : leaves) {
                    if (model.evaluateLit(leaf.toLogicNG(factory))) {
                        output.add(leaf);
                        totalNormalizedCost += leaf.normalizedCost;
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
