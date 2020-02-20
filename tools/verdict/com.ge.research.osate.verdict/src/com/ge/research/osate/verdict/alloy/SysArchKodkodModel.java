package com.ge.research.osate.verdict.alloy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.engine.Solution;
import kodkod.engine.Solver;
import kodkod.engine.satlab.SATFactory;
import kodkod.instance.Bounds;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;
import kodkod.instance.Universe;
import kodkod.util.collections.Pair;

public class SysArchKodkodModel {
	final String ENUM = "_Enum";
	final String UNKNOWN = "unknown_";
	final String UNKNOWNDAL = UNKNOWN+"DAL";
	final String UNKNOWNBOOL = UNKNOWN+"Bool";
	
	final String SYSTEM = "system";
	final String PORT = "port";
	final String CONNECTION = "connection";
	
	final String TRUE = "true";
	final String FALSE = "false";
	
	final String CONNECTIONS = "connections";
	final String SUBCOMPONENTS = "subcomponents";
	
	final Map<Relation, Pair<Relation, Relation>> binaryRelToDomainRangeRelMap = new HashMap<>();
	final Map<Relation, Set<Relation>> compTypeRelToInstRelMap = new HashMap<>();
	final Map<Relation, Set<Relation>> compImplRelToInstRelMap = new HashMap<>();
	final Map<Relation, Set<Relation>> propTypeRelToValRelMap = new HashMap<>();
	final Set<Relation> allConnectionRels = new HashSet<Relation>();
	final Set<Relation> allBinaryInportRels = new HashSet<Relation>();
	final Set<Relation> allBinaryOutportRels = new HashSet<Relation>();
	
	final Map<Relation, Pair<String, Formula>> instRelToPredicateMap = new HashMap<>();
	
	/**
	 * Unary Relations
	 * */
	public final Relation portUnaryRel, inPortUnaryRel, outPortUnaryRel, systemUnaryRel, connectionUnaryRel,
						  boolUnaryRel, dalUnaryRel;

	public String[] DALNames = {"Zero", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", UNKNOWNDAL};
	
	/**
	 * Binary Relations
	 * */	
	public final Relation inPortsBinaryRel, outPortsBinaryRel, srcPortBinaryRel, destPortBinaryRel,
						  connectionsBinaryRel, subcomponentsBinaryRel;
	
	
	public final List<Formula> facts = new ArrayList<Formula>();
	
	public final Map<String, Relation> nameToUnaryRelMap = new LinkedHashMap<>();
	public final Map<Pair<Relation, String>, Relation> domainRelNameToRelMap = new LinkedHashMap<>();
	public final Map<String, Relation> propNameToBinaryRelMap = new LinkedHashMap<>();

	public final List<Expression> allInstInports = new ArrayList<Expression>();
	public final List<Expression> allInstOutports = new ArrayList<Expression>();
	
	public SysArchKodkodModel() {
		portUnaryRel = mkUnaryRel(PORT);
		inPortUnaryRel = mkUnaryRel("InPort");
		outPortUnaryRel = mkUnaryRel("OutPort");
		mkSubRelationship(portUnaryRel, true, Arrays.asList(inPortUnaryRel, outPortUnaryRel), false);
		
		boolUnaryRel = mkUnaryRel("Bool");
		mkSubRelationship(boolUnaryRel, true, true, TRUE, FALSE, UNKNOWNBOOL);
		Set<Relation> boolSubrels = new HashSet<Relation>();
		boolSubrels.add(nameToUnaryRelMap.get(TRUE));
		boolSubrels.add(nameToUnaryRelMap.get(FALSE));
		boolSubrels.add(nameToUnaryRelMap.get(UNKNOWNBOOL));
		propTypeRelToValRelMap.put(boolUnaryRel, boolSubrels);
		
		dalUnaryRel = mkUnaryRel("DAL");
		mkSubRelationship(dalUnaryRel, true, true, DALNames);
		
		Set<Relation> dalRels = new HashSet<Relation>();
		for(String dalName : DALNames) {
			dalRels.add(nameToUnaryRelMap.get(dalName));
		}
		propTypeRelToValRelMap.put(dalUnaryRel, dalRels);
		
		systemUnaryRel = mkUnaryRel(SYSTEM);
		connectionUnaryRel = mkUnaryRel(CONNECTION);
		
		inPortsBinaryRel = mkBinaryRel(systemUnaryRel, inPortUnaryRel, false, "inPorts");
		outPortsBinaryRel = mkBinaryRel(systemUnaryRel, outPortUnaryRel, false, "outPorts");
		
		srcPortBinaryRel = mkBinaryRel(connectionUnaryRel, portUnaryRel, true, "srcPort");
		destPortBinaryRel = mkBinaryRel(connectionUnaryRel, portUnaryRel, true, "destPort");
		
		connectionsBinaryRel = mkBinaryRel(systemUnaryRel, connectionUnaryRel, false, CONNECTIONS);
		subcomponentsBinaryRel = mkBinaryRel(systemUnaryRel, systemUnaryRel, false, SUBCOMPONENTS);
	}
	
	/**
	 * Make bounds for relations
	 * */
	public final Bounds mkBounds() {
		List<String> atoms = new ArrayList<String>();
		Map<Relation, List<String>> relToBoundMap = new HashMap<Relation, List<String>>();
		
		// Property bounds
		for(Map.Entry<Relation, Set<Relation>> propRelToValRel : propTypeRelToValRelMap.entrySet()) {
			Relation propRel = propRelToValRel.getKey();
			Set<Relation> valRels = propRelToValRel.getValue();
			List<Relation> valRelsList = new ArrayList<Relation>(valRels);
			List<String> propTuples = new ArrayList<String>();
			
			for(int i = 0; i < valRelsList.size(); i++) {
				List<String> propValTuple = new ArrayList<String>();
				String tupName = propRel.name()+"$"+i;
				atoms.add(tupName);
				propTuples.add(tupName);
				propValTuple.add(tupName);
				relToBoundMap.put(valRelsList.get(i), propValTuple);
			}			
			relToBoundMap.put(propRel, propTuples);
		}
		
		// Connection bounds
		List<Relation> connectionRels = new ArrayList<Relation>(allConnectionRels);
		List<String> connTuples = new ArrayList<String>();
		
		for(int i = 0; i < connectionRels.size(); ++i) {
			String tupName = connectionRels.get(i).name() + "$" + i;
			List<String> connMember = new ArrayList<String>();
			connTuples.add(tupName);
			connMember.add(tupName);
			atoms.add(tupName);	
			relToBoundMap.put(connectionRels.get(i), connMember);
		}
		relToBoundMap.put(connectionUnaryRel, connTuples);
		
		// Port bounds
		List<String> portTuples = new ArrayList<String>();
		List<String> inportTuples = new ArrayList<String>();
		List<String> outportTuples = new ArrayList<String>();
		
		for(int i = 0; i < allBinaryInportRels.size(); ++i) {
			String inportTupName = "inport$" + i;
			portTuples.add(inportTupName);
			inportTuples.add(inportTupName);
			atoms.add(inportTupName);
		}
		for(int i = 0; i < allBinaryOutportRels.size(); ++i) {
			String inportTupName = "outport$" + i;
			portTuples.add(inportTupName);
			outportTuples.add(inportTupName);
			atoms.add(inportTupName);
		}
		relToBoundMap.put(portUnaryRel, portTuples);
		relToBoundMap.put(inPortUnaryRel, inportTuples);
		relToBoundMap.put(outPortUnaryRel, outportTuples);
		
		// System bounds
		List<String> systemTuples = new ArrayList<String>();
		for(Map.Entry<Relation, Set<Relation>> relInsts : compTypeRelToInstRelMap.entrySet()) {
			List<Relation> sysInsts = new ArrayList<Relation>(relInsts.getValue());
			List<String> allSysInstsTuples = new ArrayList<String>();
			
			for(int i = 0; i < sysInsts.size(); ++i) {
				Relation sysInstRel = sysInsts.get(i);
				String tupName = relInsts.getKey().name()+"$"+i;
				List<String> sysInstTuples = new ArrayList<String>();
				
				atoms.add(tupName);
				allSysInstsTuples.add(tupName);
				systemTuples.add(tupName);
				sysInstTuples.add(tupName);
				relToBoundMap.put(sysInstRel, sysInstTuples);
			}
			relToBoundMap.put(relInsts.getKey(), allSysInstsTuples);
		}
		relToBoundMap.put(systemUnaryRel, systemTuples);
				
		
//		System.out.println(Relation.UNIV + " : " + atoms);
		
		final Universe u = new Universe(atoms);
        final Bounds b = new Bounds(u);
        final TupleFactory f = u.factory();
        Map<Relation, TupleSet> unaryRelToBoundMap = new HashMap<>();
        
        for(Map.Entry<Relation, List<String>> relToBound : relToBoundMap.entrySet()) {
        	Relation rel = relToBound.getKey();
        	List<String> tuples = relToBound.getValue();
        	
        	if(!tuples.isEmpty()) {
        		final TupleSet exactBound = f.range(f.tuple(tuples.get(0)), f.tuple(tuples.get(tuples.size()-1)));
        		b.boundExactly(rel, exactBound);
        		unaryRelToBoundMap.put(rel, exactBound);
//        		System.out.println(rel.name() + " : " + exactBound.toString());
        	} else {
        		b.bound(rel, f.noneOf(1));
        		unaryRelToBoundMap.put(rel, f.noneOf(1));
//        		System.out.println(rel.name() + " : " + f.noneOf(1).toString());
        	}
        }
        
        // Assign upper bounds for each binary relation   
        b.bound(inPortsBinaryRel, unaryRelToBoundMap.get(systemUnaryRel).product(unaryRelToBoundMap.get(inPortUnaryRel)));
        b.bound(outPortsBinaryRel, unaryRelToBoundMap.get(systemUnaryRel).product(unaryRelToBoundMap.get(outPortUnaryRel)));
        b.bound(srcPortBinaryRel, unaryRelToBoundMap.get(connectionUnaryRel).product(unaryRelToBoundMap.get(portUnaryRel)));
        b.bound(destPortBinaryRel, unaryRelToBoundMap.get(connectionUnaryRel).product(unaryRelToBoundMap.get(portUnaryRel)));
        b.bound(connectionsBinaryRel, unaryRelToBoundMap.get(systemUnaryRel).product(unaryRelToBoundMap.get(connectionUnaryRel)));
        b.bound(subcomponentsBinaryRel, unaryRelToBoundMap.get(systemUnaryRel).product(unaryRelToBoundMap.get(systemUnaryRel)));
        
        for(Map.Entry<Relation, Pair<Relation, Relation>> propRelToDomainRangeRel : binaryRelToDomainRangeRelMap.entrySet()) {
        	Relation propRel = propRelToDomainRangeRel.getKey();
        	Relation domainRel = propRelToDomainRangeRel.getValue().a;
        	Relation rangeRel = propRelToDomainRangeRel.getValue().b;
        	
        	if(!unaryRelToBoundMap.containsKey(domainRel) || !unaryRelToBoundMap.containsKey(rangeRel)) {
        		throw new RuntimeException("No bound found for domian or range relation: " + domainRel + ", " + rangeRel);
        	}
        	b.bound(propRel, unaryRelToBoundMap.get(domainRel).product(unaryRelToBoundMap.get(rangeRel)));
        }
        return b;
	}
	
	public Formula mkFacts() {
		Formula fact = Formula.TRUE;
		for(Formula f : facts) {
			fact = fact.and(f);
			System.out.println(f);
		}
//		Relation sysImplRel = nameToUnaryRelMap.get("comp1_inst");
//		Relation compTypePropRel = domainRelNameToRelMap.get(new Pair<>(systemUnaryRel, "userAuthentication"));
//		
//		if(sysImplRel != null && compTypePropRel != null) {
//			Formula f  = sysImplRel.join(compTypePropRel).eq(nameToUnaryRelMap.get("true"));
//			System.out.println(f);
//			fact = fact.and(f);
//		}
		return fact;
	}
	
	public void execute() {
		final Solver solver = new Solver();
        solver.options().setSolver(SATFactory.MiniSat);
        Formula facts = mkFacts();
        
        for(Map.Entry<Relation, Pair<String, Formula>> instRelToPred : instRelToPredicateMap.entrySet()) {
        	Relation instRel = instRelToPred.getKey();
        	Pair<String, Formula> pred = instRelToPred.getValue();
        	System.out.println("********************************************");
            Solution sol = solver.solve(facts.and(pred.b), mkBounds());
            
            if(sol.sat()) {
            	System.out.println("System Instance " + instRel.name() + " is subject to " + pred.a);
            } else if(sol.unsat()){
            	System.out.println("System Instance " + instRel.name() + " is NOT subject to " + pred.a);
            } else {
            	throw new RuntimeException("Unreachable!");
            }
            System.out.println("********************************************");
        }
	}

    /**
     * Make a unary relation
     * */
    public Relation mkUnaryRel(String name) {
    	if(name != null) {
    		Relation rel = Relation.unary(name);
    		nameToUnaryRelMap.put(name, rel);
    		return rel;
    	} else {
    		throw new RuntimeException("Relation name is null!");
    	}
    }  
    
    /**
     * Make the union of expressions
     * */
    public Expression mkUnionExprs(Expression ... exprs) {
    	if(exprs == null) {
    		throw new RuntimeException("The input to the function is NULL!");
    	}
    	return mkUnionExprs(Arrays.asList(exprs));
    }
    public Expression mkUnionRels(List<Relation> rels) {
    	if(rels == null) {
    		throw new RuntimeException("The input to the function is NULL!");
    	}
    	Expression unionExpr = rels.get(0);
    	for(int i = 1; i < rels.size(); ++i) {
    		unionExpr = unionExpr.union(rels.get(i));
    	}
    	return unionExpr;
    }
    public Expression mkUnionExprs(List<Expression> exprs) {
    	if(exprs != null && exprs.size() > 0) {
    		Expression unionExpr = exprs.get(0);
    		for(int i = 1; i < exprs.size(); ++i) {
    			unionExpr.union(exprs.get(i));
    		}
    		return unionExpr;
    	} else {
    		throw new RuntimeException("No valid input is given to function \"union\": " + exprs);
    	}    	
    }
    
    /**
     * Make the equation of expr1 and expr2
     * */
    public void mkEq(Expression expr1, Expression expr2) {
    	if(expr1 == null || expr2 == null) {
    		throw new RuntimeException("The input to the function is NULL!");
    	}
    	facts.add(expr1.eq(expr2));
    }
    
    /**
     * Make the mutual disjoint constraints
     * */
    public void mkMutualDisjoint(Expression ... exprs) {
    	if(exprs == null) {
    		throw new RuntimeException("The input to the function is NULL!");
    	}
    	mkMutualDisjoint(Arrays.asList(exprs));
    }
    public void mkMutualDisjoint(List<Expression> exprs) {
    	if(exprs != null && exprs.size() > 0) {
    		if(exprs.size() > 1) {
    			for(int i = 0;i < exprs.size()-1; ++i) {
    				Expression fstExpr = exprs.get(i);    				
    				for(int j = i+1; j < exprs.size(); ++j) {
    					Expression sndExpr = exprs.get(j);
    					Formula constraint = fstExpr.intersection(sndExpr).no();
    					facts.add(constraint);
    				}
    			}
    		} else {
    		}
    	} else {
    		throw new RuntimeException("No valid input is given to function \"mkMutualDisjoint\": " + exprs);
    	}    	
    } 
    
    public void mkMutualDisjointRels(List<Relation> rels) {
    	if(rels == null) {
    		throw new RuntimeException("The input to the function is NULL!");
    	}
    	mkMutualDisjoint(new ArrayList<Expression>(rels));
    }   
    
    public void mkSubsetRels(Relation rel1, Relation rel2) {
    	if(rel1 == null || rel2 == null) {
    		throw new RuntimeException("The input to the function is NULL!");
    	}
    	facts.add(rel1.in(rel2));
    }    
    
    /**
     * Make "one expr"
     * */
    public void mkOne(Expression expr) {
    	if(expr == null) {
    		throw new RuntimeException("The input to the function is NULL!");
    	}
    	facts.add(expr.one());
    }
    
	/**
	 * 
	 * 1. Make unary relations with extendsNames
	 * 2. Make additional constraints for "extends" or "in" relationship, and isOne
	 * 
	 * */
	void mkSubRelationship(Relation parentRel, boolean isExtends, boolean isOne, String ... extendsNames) {
		List<String> names = new ArrayList<String>();
		
		for(String name : extendsNames) {
			names.add(name);
		}
		mkSubRelationships(parentRel, isExtends, isOne, names);
	}	
	void mkSubRelationships(Relation parentRel, boolean isExtends, boolean isOne, List<String> extendsNames) {
		if(extendsNames != null && extendsNames.size() > 0) {
			List<Relation> unaryRels = new ArrayList<Relation>();
			
			for(String name : extendsNames) {
				Relation unaryRel = mkUnaryRel(name);
				unaryRels.add(unaryRel);
				
				// Make "one unaryRel"
				if(isOne) {
					mkOne(unaryRel);
				}
			}
			
			if(parentRel != null) {
				if(isExtends) {
					// Create the union of all unary relations
					Expression unionExpr = mkUnionRels(unaryRels);					
					// The union of all sub relations is equivalent to the parent relation
					mkEq(unionExpr, parentRel);
					
					// Sub-signatures are mutually disjoint
					if(unaryRels.size() >= 2) {
						mkMutualDisjointRels(unaryRels);
					}
				} else {
					// Each unaryRel is in parentRel
					for(Relation unaryRel : unaryRels) {
						mkSubsetRels(unaryRel, parentRel);
					}
				}
			} 
		} else {
			throw new RuntimeException("Sub-relation names are not given: " + extendsNames);
		}
	}    
	
	void mkSubRelationship(Relation parentRel, boolean isExtends, List<Relation> subRelations, boolean isOne) {
		if(subRelations != null && subRelations.size() > 0 && parentRel != null) {
			if(isOne) {
				for(Relation rel : subRelations) {
					mkOne(rel);
				}
			}
			if(isExtends) {
				// Create the union of all unary relations
				Expression unionExpr = mkUnionRels(subRelations);					
				// The union of all sub relations is equivalent to the parent relation
				mkEq(unionExpr, parentRel);
				
				// Sub-signatures are mutually disjoint
				if(subRelations.size() >= 2) {
					mkMutualDisjointRels(subRelations);
				}
			} else {
				// Each unaryRel is in parentRel
				for(Relation unaryRel : subRelations) {
					mkSubsetRels(unaryRel, parentRel);
				}
			}
		} else {
			throw new RuntimeException("Sub-relations or parent relation are not given!");
		}
	} 	
	void mkSubRelationship(Relation parentRel, boolean isExtends, Set<Relation> subRelations, boolean isOne) {
		mkSubRelationship(parentRel, isExtends, new ArrayList<Relation>(subRelations), isOne);
	}
	
	/**
	 * Make a binary relation
	 * */
	Relation mkBinaryRel(Relation domain, Relation range, boolean isFunc, String binaryRelName) {
		if(domain == null || range == null || binaryRelName == null){
			throw new RuntimeException("Some inputs are not given: " + " domain: " + domain + " range: " +range + " binaryRelName: " + binaryRelName);
		}
		Relation binaryRel = Relation.binary(binaryRelName);
		if(isFunc) {				
			facts.add(binaryRel.function(domain, range));				
		} else {
			facts.add(binaryRel.in(domain.product(range)));
		}
		domainRelNameToRelMap.put(new Pair<>(domain, binaryRelName), binaryRel);	
		return binaryRel;
	}
}
