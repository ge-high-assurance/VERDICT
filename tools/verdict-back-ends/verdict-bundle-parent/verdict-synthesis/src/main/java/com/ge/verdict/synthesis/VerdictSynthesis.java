package com.ge.verdict.synthesis;

import java.util.Collection;

import com.ge.verdict.synthesis.dtree.DLeaf;
import com.ge.verdict.synthesis.dtree.DTree;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Model;
import com.microsoft.z3.Optimize;
import com.microsoft.z3.Status;

public class VerdictSynthesis {
	public static void performSynthesis(DTree tree) {
		Context context = new Context();
		Optimize optimizer = context.mkOptimize();

		Collection<DLeaf> leaves = DLeaf.allLeaves();

		for (DLeaf leaf : leaves) {
			optimizer.AssertSoft(context.mkNot(leaf.smt(context)), 1, "cover");
		}

		optimizer.Assert(tree.smt(context));

		if (optimizer.Check().equals(Status.SATISFIABLE)) {
			Model model = optimizer.getModel();
			for (DLeaf leaf : leaves) {
				Expr expr = model.eval(leaf.smt(context), true);
				System.out.println("Result for " + leaf.prettyPrint() + ": " + expr.toString());
			}
		} else {
			System.err.println("Synthesis: SMT not satisfiable, is input tree valid?");
		}
	}
}
