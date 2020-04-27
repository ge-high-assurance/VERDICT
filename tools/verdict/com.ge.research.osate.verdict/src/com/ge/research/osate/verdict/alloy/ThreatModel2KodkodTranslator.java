package com.ge.research.osate.verdict.alloy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.ge.research.osate.verdict.dsl.verdict.Exists;
import com.ge.research.osate.verdict.dsl.verdict.Forall;
import com.ge.research.osate.verdict.dsl.verdict.Implies;
import com.ge.research.osate.verdict.dsl.verdict.Intro;
import com.ge.research.osate.verdict.dsl.verdict.ThreatAnd;
import com.ge.research.osate.verdict.dsl.verdict.ThreatDefense;
import com.ge.research.osate.verdict.dsl.verdict.ThreatEqualContains;
import com.ge.research.osate.verdict.dsl.verdict.ThreatExpr;
import com.ge.research.osate.verdict.dsl.verdict.ThreatModel;
import com.ge.research.osate.verdict.dsl.verdict.ThreatNot;
import com.ge.research.osate.verdict.dsl.verdict.ThreatOr;
import com.ge.research.osate.verdict.dsl.verdict.ThreatStatement;
import com.ge.research.osate.verdict.dsl.verdict.Var;
import com.ge.research.osate.verdict.dsl.verdict.VerdictThreatModels;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.ast.Variable;
import kodkod.ast.operator.FormulaOperator;
import kodkod.util.collections.Pair;

public class ThreatModel2KodkodTranslator {
	SysArchKodkodModel kodkodModel = null;
	Map<String, Relation> varNameToRelMap = new HashMap<String, Relation>();
	
	public ThreatModel2KodkodTranslator(SysArchKodkodModel sysKodkodModel) {
		kodkodModel = sysKodkodModel;
		varNameToRelMap.put(kodkodModel.TRUE, kodkodModel.nameToUnaryRelMap.get(kodkodModel.TRUE));
		varNameToRelMap.put(kodkodModel.FALSE, kodkodModel.nameToUnaryRelMap.get(kodkodModel.FALSE));
		varNameToRelMap.put(kodkodModel.CONNECTION, kodkodModel.connectionUnaryRel);
		varNameToRelMap.put(kodkodModel.CONNECTIONS, kodkodModel.connectionsBinaryRel);
		varNameToRelMap.put(kodkodModel.SUBCOMPONENTS, kodkodModel.subcomponentsBinaryRel);
		varNameToRelMap.put(kodkodModel.PORT, kodkodModel.portUnaryRel);
		varNameToRelMap.put(kodkodModel.SRCPORT, kodkodModel.srcPortBinaryRel);
		varNameToRelMap.put(kodkodModel.DESTPORT, kodkodModel.destPortBinaryRel);
		varNameToRelMap.put(kodkodModel.SYSTEM, kodkodModel.systemUnaryRel);
		varNameToRelMap.put(kodkodModel.PORTS, kodkodModel.portsBinaryRel);
		// Add all property names and its value names
		for(Relation propRel : kodkodModel.binaryRelToDomainRangeRelMap.keySet()) {
			varNameToRelMap.put(propRel.name(), propRel);
		}
		for(Set<Relation> propValRels : kodkodModel.propTypeRelToValRelMap.values()) {
			for(Relation rel : propValRels) {
				varNameToRelMap.put(rel.name(), rel);
			}
		}
	}
	
	/**
	 * Translate each of the verdict file in paths
	 * */
	public void translate(List<String> paths) {
		List<VerdictThreatModels> threatModels = ThreatModelParser.parseModels(paths);
		
		for(VerdictThreatModels model : threatModels) {
			List<ThreatStatement> statements = model.getStatements();
			for(ThreatStatement threatStatement : statements) {
				if(threatStatement instanceof ThreatModel) {
					translateThreatEffects((ThreatModel) threatStatement);
				} else if(threatStatement instanceof ThreatDefense) {
					translateThreatDefense((ThreatDefense) threatStatement);
				}
			}
		}
	}
	
	/**
	 * Translate the threat effect statement for each instance
	 * */
	public void translateThreatEffects(ThreatModel threatStatement) {
		String id = threatStatement.getIntro().getId();
		String entityType = threatStatement.getIntro().getType();
		
		if(entityType.equalsIgnoreCase(kodkodModel.SYSTEM)) {			
			for(Set<Relation> instRels : kodkodModel.compTypeRelToInstRelMap.values()) {
				for(Relation instRel : instRels) {
					Map<String, Expression> context = new HashMap<>();
					
					if(kodkodModel.compImplRelToInstRelMap.containsKey(instRel)) {
						for(Relation implInstRel : kodkodModel.compImplRelToInstRelMap.get(instRel)) {
							context.put(id, implInstRel);
							addPredicate(kodkodModel.instRelToPredMap, implInstRel, new Pair<String, Formula>(threatStatement.getId(), translateThreatExpr(threatStatement.getExpr(), context)));							
						}
					} else {						
						context.put(id, instRel);
						addPredicate(kodkodModel.instRelToPredMap, instRel, new Pair<String, Formula>(threatStatement.getId(), translateThreatExpr(threatStatement.getExpr(), context)));
					}
				}
			}
		} else {
			throw new RuntimeException("Unsupported entity type: " + entityType);
		}		
	}
	
	/**
	 * Translate the threat defense statement for each instance
	 * */
	public void translateThreatDefense(ThreatDefense threatStatement) {
		String defenseId = threatStatement.getName();
		List<String> threats = threatStatement.getThreats();
		if(defenseId != null && threats != null && !threats.isEmpty()) {
			for(String threat : threats) {
				kodkodModel.threatToDefense.put(threat, defenseId);
			}
		}
	}
	
	public void addPredicate(Map<Relation, List<Pair<String, Formula>>> instRelToPredicateMap, Relation rel, Pair<String, Formula> predPair) {
		if(instRelToPredicateMap.containsKey(rel)) {
			instRelToPredicateMap.get(rel).add(predPair);
		} else {
			List<Pair<String, Formula>> preds = new ArrayList<Pair<String,Formula>>();
			preds.add(predPair);
			instRelToPredicateMap.put(rel, preds);
		}
	}
	
	public void translateThreatDefenses() {
		
	}	
	
	/**
	 * 
	 * Translate an threat expression 
	 * */
	public Formula translateThreatExpr(ThreatExpr expr, Map<String, Expression> context) {
		Formula kodkodFormula = null;
		
		if(expr instanceof Implies) {
			kodkodFormula = translateImpliesExpr((Implies) expr, context);
		} else if(expr instanceof Forall) {
			kodkodFormula = translateForallExpr((Forall) expr, context);
		} else if(expr instanceof Exists) {
			kodkodFormula = translateExistsExpr((Exists) expr, context);
		} else if (expr instanceof ThreatEqualContains) {
			kodkodFormula = translateThreatEqualContainsExpr((ThreatEqualContains) expr, context);
		} else if (expr instanceof ThreatNot) {
			kodkodFormula = translateThreatNotExpr((ThreatNot) expr, context);
		} else {
			throw new RuntimeException("Got a bad expression!");
		}
		return kodkodFormula;
	}
	
	public Formula translateForallExpr(Forall expr, Map<String, Expression> context) {
		Intro intro = expr.getIntro();
		String id = intro.getId();
		String idType = intro.getType();
		Variable var = Variable.unary(id);
		Expression idExpr = findExpression(idType, context);
		
		context.put(id, var);
		Formula bodyFormula = translateThreatExpr(expr.getExpr(), context);
		context.remove(id);
		return bodyFormula.forAll(var.oneOf(idExpr));
	}	
	
	public Formula translateExistsExpr(Exists expr, Map<String, Expression> context) {
		Intro intro = expr.getIntro();
		String id = intro.getId();
		String idType = intro.getType();
		Variable var = Variable.unary(id);
		Expression idExpr = findExpression(idType, context);
		
		context.put(id, var);
		Formula bodyFormula = translateThreatExpr(expr.getExpr(), context);
		context.remove(id);
		return bodyFormula.forSome(var.oneOf(idExpr));
	}	
	
	public Formula translateThreatNotExpr(ThreatNot expr, Map<String, Expression> context) {
		return translateThreatExpr(expr.getExpr(), context).not();
	}	
	
	/**
	 * 
	 * A var expression is built by joining multiple constants with variables. 
	 * The constants could be the following: 
	 * connections, subcomponents, true, false, in , out, property values defined in 
	 * AADL property set, and more.
	 * 
	 * */
	public Expression translateVarExpr(Var var, Map<String, Expression> context) {
		List<String> ids = var.getIds();
		Expression varExpr = findExpression(var.getId(), context);
		
		if(ids != null) {
			for(String id : ids) {
				varExpr = varExpr.join(findExpression(id, context));
			}
		}
		return varExpr;
	}
	
	public Expression findExpression(String name, Map<String, Expression> context) {
		Expression varExpr = null;
		if(context.containsKey(name)) {
			varExpr = context.get(name);
		} else if(varNameToRelMap.containsKey(name)) {
			varExpr = varNameToRelMap.get(name);
		} else {
			throw new RuntimeException("Unsupported expression: " + name);
		}
		
		return varExpr;
	}
	
	public Formula translateThreatEqualContainsExpr(ThreatEqualContains expr, Map<String, Expression> context) {
		Formula formula = null;
		Expression lhsVarExpr = translateVarExpr(expr.getLeft(), context);
		Expression rhsVarExpr = translateVarExpr(expr.getRight(), context);
		
		if(expr.isContains()) {
			formula = rhsVarExpr.in(lhsVarExpr);
		} else if(expr.isEqual()) {
			formula = rhsVarExpr.eq(lhsVarExpr);
		} else {
			throw new RuntimeException("Unreachable in translateThreatEqualContainsExpr!");
		}
		return formula;
	}
	
	public Formula translateImpliesExpr(Implies impliesExpr, Map<String, Expression> context) {
		Formula formula = null;
		ThreatOr rhsExpr = impliesExpr.getConsequent();		
		
		formula = translateThreatOr(impliesExpr.getAntecedent(), context);
		
		if(rhsExpr != null) {
			formula = formula.implies(translateThreatOr(impliesExpr.getConsequent(), context));
		}
		
		return formula;
	}
	
	public Formula translateThreatOr(ThreatOr expr, Map<String, Expression> context) {
		List<Formula> formulas = expr.getExprs().stream().map(e -> translateThreatAnd(e, context)).collect(Collectors.toList());
		return Formula.compose(FormulaOperator.OR, formulas);		
	}
	
	public Formula translateThreatAnd(ThreatAnd expr, Map<String, Expression> context) {
		List<Formula> formulas = expr.getExprs().stream().map(e -> translateThreatExpr(e, context)).collect(Collectors.toList());
		return Formula.compose(FormulaOperator.AND, formulas);
	}
}
