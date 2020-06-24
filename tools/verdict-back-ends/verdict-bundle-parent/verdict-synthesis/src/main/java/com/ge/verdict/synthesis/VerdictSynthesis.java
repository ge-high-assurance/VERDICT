package com.ge.verdict.synthesis;

import com.ge.verdict.synthesis.dtree.DLeaf;
import com.ge.verdict.synthesis.dtree.DTree;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Model;
import com.microsoft.z3.Optimize;
import com.microsoft.z3.Status;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.solvers.MaxSATSolver;

public class VerdictSynthesis {
    public static enum Approach {
        MAXSMT,
        MAXSAT
    }

    public static Optional<Set<DLeaf>> performSynthesis(
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

    public static Optional<Set<DLeaf>> performSynthesisMaxSmt(DTree tree, DLeaf.Factory factory) {
        Context context = new Context();
        Optimize optimizer = context.mkOptimize();

        Collection<DLeaf> leaves = factory.allLeaves();

        for (DLeaf leaf : leaves) {
            // System.out.println("LEAF: " + leaf + ", COST: " + leaf.cost + " [MaxSMT]");

            // this id ("cover") doesn't matter but we have to specify something
            if (leaf.cost > 0) {
                optimizer.AssertSoft(context.mkNot(leaf.toZ3(context)), leaf.cost, "cover");
            }
        }

        optimizer.Assert(tree.toZ3(context));

        if (optimizer.Check().equals(Status.SATISFIABLE)) {
            Set<DLeaf> output = new LinkedHashSet<>();
            Model model = optimizer.getModel();
            for (DLeaf leaf : leaves) {
                Expr expr = model.eval(leaf.toZ3(context), true);
                switch (expr.getBoolValue()) {
                    case Z3_L_TRUE:
                        output.add(leaf);
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
            return Optional.of(output);
        } else {
            System.err.println("Synthesis: SMT not satisfiable, is input tree valid?");
            return Optional.empty();
        }
    }

    public static Optional<Set<DLeaf>> performSynthesisMaxSat(
            DTree tree, DLeaf.Factory dleafFactory) {
        FormulaFactory factory = new FormulaFactory();
        MaxSATSolver solver = MaxSATSolver.wmsu3();

        Formula cnf = tree.toLogicNG(factory).cnf();

        Collection<DLeaf> leaves = dleafFactory.allLeaves();

        for (DLeaf leaf : leaves) {
            // System.out.println("LEAF: " + leaf + ", COST: " + leaf.cost + " [MaxSAT]");

            if (leaf.cost > 0) {
                solver.addSoftFormula(factory.not(leaf.toLogicNG(factory)), leaf.cost);
            }
        }

        // implicitly converts formula to CNF
        solver.addHardFormula(cnf);

        switch (solver.solve()) {
            case OPTIMUM:
                Set<DLeaf> output = new LinkedHashSet<>();
                Assignment model = solver.model();
                for (DLeaf leaf : leaves) {
                    if (model.evaluateLit(leaf.toLogicNG(factory))) {
                        output.add(leaf);
                    }
                }
                return Optional.of(output);
            case UNDEF:
                System.err.println("Synthesis: SAT undefined, is input tree valid?");
                return Optional.empty();
            case UNSATISFIABLE:
                System.err.println("Synthesis: SAT not satisfiable, is input tree valid?");
                return Optional.empty();
            default:
                throw new RuntimeException("impossible");
        }
    }
}
