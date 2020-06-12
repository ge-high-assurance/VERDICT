package com.ge.verdict.synthesis;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import com.ge.verdict.synthesis.dtree.DLeaf;
import com.ge.verdict.synthesis.dtree.DTree;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Model;
import com.microsoft.z3.Optimize;
import com.microsoft.z3.Status;

public class VerdictSynthesis {
	public static Optional<Set<DLeaf>> performSynthesis(DTree tree) {
        Context context = new Context();
        Optimize optimizer = context.mkOptimize();

        Collection<DLeaf> leaves = DLeaf.allLeaves();

        for (DLeaf leaf : leaves) {
			// this id ("cover") doesn't matter but we have to specify something
            optimizer.AssertSoft(context.mkNot(leaf.smt(context)), 1, "cover");
        }

        optimizer.Assert(tree.smt(context));

        if (optimizer.Check().equals(Status.SATISFIABLE)) {
			Set<DLeaf> output = new LinkedHashSet<>();
            Model model = optimizer.getModel();
            for (DLeaf leaf : leaves) {
				Expr expr = model.eval(leaf.smt(context), true);
				switch (expr.getBoolValue()) {
				case Z3_L_TRUE:
					output.add(leaf);
					break;
				case Z3_L_FALSE:
					break;
				case Z3_L_UNDEF:
				default:
					throw new RuntimeException("Synthesis: Undefined variable in output model: " + leaf.toString());
				}
            }
			return Optional.of(output);
        } else {
            System.err.println("Synthesis: SMT not satisfiable, is input tree valid?");
			return Optional.empty();
        }
    }
}
