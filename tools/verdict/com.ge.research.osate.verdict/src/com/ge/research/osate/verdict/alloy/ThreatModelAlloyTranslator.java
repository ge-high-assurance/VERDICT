package com.ge.research.osate.verdict.alloy;

import edu.mit.csail.sdg.alloy4.Pair;
import edu.mit.csail.sdg.alloy4.Pos;
import edu.mit.csail.sdg.ast.Decl;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.ExprBinary;
import edu.mit.csail.sdg.ast.ExprHasName;
import edu.mit.csail.sdg.ast.ExprList;
import edu.mit.csail.sdg.ast.ExprQt;
import edu.mit.csail.sdg.ast.ExprUnary;
import edu.mit.csail.sdg.ast.ExprVar;
import edu.mit.csail.sdg.ast.Func;
import edu.mit.csail.sdg.ast.Sig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ge.research.osate.verdict.dsl.verdict.Exists;
import com.ge.research.osate.verdict.dsl.verdict.Forall;
import com.ge.research.osate.verdict.dsl.verdict.Implies;
import com.ge.research.osate.verdict.dsl.verdict.Intro;
import com.ge.research.osate.verdict.dsl.verdict.ThreatAnd;
import com.ge.research.osate.verdict.dsl.verdict.ThreatEqualContains;
import com.ge.research.osate.verdict.dsl.verdict.ThreatExpr;
import com.ge.research.osate.verdict.dsl.verdict.ThreatModel;
import com.ge.research.osate.verdict.dsl.verdict.ThreatNot;
import com.ge.research.osate.verdict.dsl.verdict.ThreatOr;
import com.ge.research.osate.verdict.dsl.verdict.Var;

public class ThreatModelAlloyTranslator {	
	public static Map<String, List<Func>> predicates = new HashMap<>();
	
	public static List<Func> translate(Collection<ThreatModel> threats) {
		return threats.stream()
				.map(ThreatModelAlloyTranslator::translateThreat)
				.collect(Collectors.toList());
	}
	
	/**
	 * Translate each threat's entity part into a predicate
	 * */
	protected static Func translateThreat(ThreatModel threat) {
		String id = threat.getId();		
		Map<String, Pair<ExprHasName, Sig>> env = new HashMap<>();		
		List<Decl> intro = translateIntro(threat.getIntro(), env);		
		Expr expr = translateExpr(threat.getExpr(), env);
		Func pred = new Func(Pos.UNKNOWN, id, intro, null, expr);
		
		addPred(threat.getIntro().getType(), pred);
		return pred;
	}
	
	/**
	 * Store a applied comp name - predicate pair
	 * */
	protected static void addPred(String name, Func pred) {
		if(predicates.containsKey(name)) {
			predicates.get(name).add(pred);
		} else {
			List<Func> preds = new ArrayList<Func>();
			
			preds.add(pred);
			predicates.put(name, preds);
		}
	}
	
	/**
	 * Translate a threat expression
	 * */
	protected static Expr translateExpr(ThreatExpr expr, Map<String, Pair<ExprHasName, Sig>> env) {
		if (expr instanceof Exists) {
			return translateExists((Exists) expr, env);
		} else if (expr instanceof Forall) {
			return translateForall((Forall) expr, env);
		} else if (expr instanceof Implies) {
			return translateImplies((Implies) expr, env);
		} else if (expr instanceof ThreatEqualContains) {
			return translateEqualContains((ThreatEqualContains) expr, env);
		} else if (expr instanceof ThreatNot) {
			return translateNot((ThreatNot) expr, env);
		} else {
			throw new RuntimeException("Got a bad expression??");
		}
	}
	
	/**
	 * Translate an intro and add the introduced variable to the environment (mutating).
	 * Assumption: The intro part can only talk about one var at a time
	 * @param intro
	 * @param env
	 * @return
	 */
	protected static List<Decl> translateIntro(Intro intro, Map<String, Pair<ExprHasName, Sig>> env) {
		Sig typeSig;
		String introType = intro.getType();
		
		if (SysArchAlloyModel.compNameToSigMap.containsKey(introType)) {
			typeSig = SysArchAlloyModel.compNameToSigMap.get(introType);
		} else {
			throw new RuntimeException("Missing type: " + intro.getType());
		}
		
		List<ExprVar> introVars = new ArrayList<>();
		
		introVars.add(ExprVar.make(Pos.UNKNOWN, intro.getId(), typeSig.type()));
		
		Decl introDecl = new Decl(Pos.UNKNOWN, Pos.UNKNOWN, Pos.UNKNOWN, introVars, typeSig);
		
		List<Decl> introParams = new ArrayList<>();
		introParams.add(introDecl);
		env.put(intro.getId(), new Pair<>(introDecl.get(), typeSig));
		return introParams;
	}
	
	protected static Expr translateOr(ThreatOr expr, Map<String, Pair<ExprHasName, Sig>> env) {
		return ExprList.make(Pos.UNKNOWN, Pos.UNKNOWN, ExprList.Op.OR,
				expr.getExprs().stream()
				.map(e -> translateAnd(e, env))
				.collect(Collectors.toList()));
	}
	
	protected static Expr translateAnd(ThreatAnd expr, Map<String, Pair<ExprHasName, Sig>> env) {
		return ExprList.make(Pos.UNKNOWN, Pos.UNKNOWN, ExprList.Op.AND,
				expr.getExprs().stream()
				.map(e -> translateExpr(e, env))
				.collect(Collectors.toList()));
	}
	
	protected static Expr translateQuant(Intro introExpr, ThreatExpr pred, Map<String, Pair<ExprHasName, Sig>> env, ExprQt.Op op) {
		// remember shadowed binding (if any)
		String id = introExpr.getId();
		Pair<ExprHasName, Sig> shadowed = env.get(id);

		List<Decl> intro = translateIntro(introExpr, env); // this adds binding to env
		Expr subExpr = translateExpr(pred, env);

		// undo the binding
		env.remove(id);
		if (shadowed != null) {
			env.put(id, shadowed);
		}

		return op.make(Pos.UNKNOWN, Pos.UNKNOWN, intro, subExpr);
	}
	
	protected static Expr translateExists(Exists expr, Map<String, Pair<ExprHasName, Sig>> env) {
		return translateQuant(expr.getIntro(), expr.getExpr(), env, ExprQt.Op.SOME);
	}
	
	protected static Expr translateForall(Forall expr, Map<String, Pair<ExprHasName, Sig>> env) {
		return translateQuant(expr.getIntro(), expr.getExpr(), env, ExprQt.Op.ALL);
	}
	
	protected static Expr translateImplies(Implies expr, Map<String, Pair<ExprHasName, Sig>> env) {
		if (expr.getConsequent() != null) {
			return ExprBinary.Op.IMPLIES.make(Pos.UNKNOWN, Pos.UNKNOWN,
					translateOr(expr.getAntecedent(), env), translateOr(expr.getConsequent(), env));
		} else {
			// Due to a quirk in the grammar, we can get a shell implies that isn't actually an implies
			return translateOr(expr.getAntecedent(), env);
		}
	}
	
	protected static Expr translateEqualContains(ThreatEqualContains expr, Map<String, Pair<ExprHasName, Sig>> env) {
		ExprBinary.Op op;
		
		if (expr.isEqual()) {
			op = ExprBinary.Op.EQUALS;
		} else if (expr.isContains()) {
			op = ExprBinary.Op.IN;
		} else {
			throw new RuntimeException("God a bad equalcontains??");
		}
		
		Expr left = translateVar(expr.getLeft(), env);
		Expr right = translateVar(expr.getRight(), env);
		
		// Flip order because in/contains is backward
		return op.make(Pos.UNKNOWN, Pos.UNKNOWN, right, left);
	}
	
	protected static Expr translateNot(ThreatNot expr, Map<String, Pair<ExprHasName, Sig>> env) {
		return ExprUnary.Op.NOT.make(Pos.UNKNOWN, translateExpr(expr, env));
	}
	
	protected static Expr translateVar(Var var, Map<String, Pair<ExprHasName, Sig>> env) {
		Expr expr =null;
		Sig sig;
		
		if (env.containsKey(var.getId())) {
			Pair<ExprHasName, Sig> pair = env.get(var.getId());
			expr = pair.a;
			sig = pair.b;
		} else {
			throw new RuntimeException("Unbound variable or constant: " + var.getId());
		}
		
//		else if (AlloyModel.valueMap.containsKey(var.getId())) {
//			expr = AlloyModel.valueMap.get(var.getId());
//			sig = null;
//		} 
		
		for (String id : var.getIds()) {
			Pair<Sig, String> lookup = new Pair<>(sig, id);
			
			if (SysArchAlloyModel.compSigFdNameToFdMap.containsKey(lookup)) {
				expr = ExprBinary.Op.JOIN.make(Pos.UNKNOWN, Pos.UNKNOWN, expr, SysArchAlloyModel.compSigFdNameToFdMap.get(lookup));
			} else {
				throw new RuntimeException("Unbound field: " + id);
			}
		}
		
		return expr;
	}
}
