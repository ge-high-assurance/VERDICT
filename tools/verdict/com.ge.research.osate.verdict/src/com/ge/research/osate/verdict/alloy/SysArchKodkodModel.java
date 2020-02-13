package com.ge.research.osate.verdict.alloy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Relation;

public class SysArchKodkodModel {
	public final String UNKNOWN = "unknown_";
	public final String UNKNOWNDAL = UNKNOWN+"DAL";
	public final String UNKNOWNBOOL = UNKNOWN+"Bool";
	
	/**
	 * 
	 	abstract sig port {}
	    sig InPort extends port {}
	    sig OutPort extends port {}
	    	
		abstract sig system {
		    inPorts : set InPort,
	 	    outPorts : set OutPort,			
		}	

		abstract sig connection {
			srcPort: one Port,
			destPort: one Port,
		}
		
		-- For each actual implementation declared in AADL,
		-- we declare the "subcomponents" and "connections" fields.
		sig actual_Impl extends some_comp_decl {
		    subcomponents : set system,	
	        connections : set connection
		}
	 */	
	
	/**
	 * Unary Relations
	 * */
	public final Relation portUnaryRel, inPortUnaryRel, outPortUnaryRel, systemUnaryRel, connectionUnaryRel,
						  boolUnaryRel, dalUnaryRel;

	public String[] DALNames = {"Zero", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", UNKNOWN+"DAL"};
	/**
	 * Binary Relations
	 * */	
	public final Relation inPortsBinaryRel, outPortsBinaryRel, srcPortBinaryRel, destPortBinaryRel;
	
	
	
	public final List<Formula> facts = new ArrayList<Formula>();
	
	public final Map<String, Relation> nameToUnaryRelMap = new LinkedHashMap<>();
	public final Map<String, Relation> nameToBinaryRelMap = new LinkedHashMap<>();
	public final Map<String, Relation> propNameToBinaryRelMap = new LinkedHashMap<>();
	public final Map<Relation, Integer> unaryRelToNumMap = new LinkedHashMap<>();
	
	public SysArchKodkodModel() {
		portUnaryRel = mkUnaryRel("port");
		inPortUnaryRel = mkUnaryRel("InPort");
		outPortUnaryRel = mkUnaryRel("OutPort");
		systemUnaryRel = mkUnaryRel("system");
		connectionUnaryRel = mkUnaryRel("connection");
		
		boolUnaryRel = mkUnaryRel("Bool");
		mkSubRelationship(boolUnaryRel, true, true, "true", "false", UNKNOWN+"Bool");
		
		dalUnaryRel = mkUnaryRel("DAL");
		mkSubRelationship(dalUnaryRel, true, true, DALNames);
		
		inPortsBinaryRel = mkBinaryRel("inPorts");
		outPortsBinaryRel = mkBinaryRel("outPorts");
		srcPortBinaryRel = mkBinaryRel("srcPort");
		destPortBinaryRel = mkBinaryRel("destPort");
		
		// Load decl facts
		facts.add(decls());
	}
	
    public final Formula decls() {
    	// inPort and outPort partition port
    	final Formula f0 = portUnaryRel.eq(inPortUnaryRel.union(outPortUnaryRel)).and(inPortUnaryRel.intersection(outPortUnaryRel).no());
    	// inPorts <= system x InPort
    	// all x : system | one (x.inPorts)
    	final Formula f1 = inPortsBinaryRel.function(systemUnaryRel, inPortUnaryRel);
    	final Formula f2 = outPortsBinaryRel.function(systemUnaryRel, outPortUnaryRel);
    	
    	final Formula f3 = srcPortBinaryRel.function(connectionUnaryRel, portUnaryRel);
    	final Formula f4 = destPortBinaryRel.function(systemUnaryRel, portUnaryRel);    	
    	
    	return f0.and(f1).and(f2).and(f3).and(f4);
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
     * Make a binary relation
     * */
    public Relation mkBinaryRel(String name) {
    	if(name != null) {
    		Relation rel = Relation.binary(name);
    		nameToBinaryRelMap.put(name, rel);
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
    	return mkUnionExprs(new ArrayList<Expression>(rels));
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
    				for(int j = 1; j < exprs.size(); ++j) {
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
		mkSubRelationships(parentRel, isExtends, isOne, Arrays.asList(extendsNames));
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
		nameToBinaryRelMap.put(binaryRelName, binaryRel);	
		return binaryRel;
	}		
}
