package com.ge.research.osate.verdict.alloy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.mit.csail.sdg.alloy4.Pair;
import edu.mit.csail.sdg.alloy4.Pos;
import edu.mit.csail.sdg.ast.Attr;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Decl;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.ExprBinary;
import edu.mit.csail.sdg.ast.Func;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.ast.Sig.Field;
import edu.mit.csail.sdg.ast.Sig.PrimSig;
import edu.mit.csail.sdg.ast.Sig.SubsetSig;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;

import static edu.mit.csail.sdg.alloy4.A4Reporter.NOP;

public class SysArchAlloyModel {
	public int systemNum = 0, connectionNum = 0, portNum = 0;
	public String UNKNOWN = "unknown_";
	public final List<Expr> allInstInports = new ArrayList<>();
	public final List<Expr> allInstOutports = new ArrayList<>();
	
	public final List<Expr> facts = new ArrayList<>();
	public final Map<String, Sig> compNameToSigMap = new LinkedHashMap<>();
	public final Map<String, Sig> propNameToSigMap = new LinkedHashMap<>();
	public final Map<Pair<Sig, String>, Field> compSigFdNameToFdMap = new LinkedHashMap<>();
	public final Map<String, List<String>> allDeclProps = new HashMap<>();
	public final List<Sig> subcompSigs = new ArrayList<>();
	
	public Field testField = null;
	public Sig testSig = null;
	
	/**
	-- abstract sig AadlModel {}
	-- one sig HybridComponent extends AadlModel {}
	-- one sig PlatformComponent extends AadlModel {}
	-- one sig SoftwareComponent extends AadlModel {}
	*/	
//	public final PrimSig AADLMODEL = new PrimSig("AADLModel", Attr.ABSTRACT);
//	public static final PrimSig HYBRIDCOMP = new PrimSig("HybridComponent", AADLMODEL, Attr.ONE);
//	public static final PrimSig PLATFORMCOMP = new PrimSig("PlatformComponent", AADLMODEL, Attr.ONE);
//	public static final PrimSig SOFTWARECOMP = new PrimSig("SoftwareComponent", AADLMODEL, Attr.ONE);
	
	/**
	 * 
	 * -- Don't need to use comp and compImpl sigs
	 	abstract sig Port {}
	    sig InPort extends Port {}
	    sig OutPort extends Port {}
	    	
		abstract sig system {
		    inPorts : set InPort,
	 	    outPorts : set OutPort,			
		}	
		-- For each actual implementation declared in AADL,
		-- we declare the "subcomponents" and "connections" fields.
		sig actual_Impl extends some_comp_decl {
		    subcomponents : set system,	
	        connections : set connection
		}
		abstract sig connection {
			srcPort: one Port,
			destPort: one Port,
		}
	 */
	public final PrimSig PORTSIG = new PrimSig("port", Attr.ABSTRACT);
	public final PrimSig INPORTSIG = new PrimSig("InPort", PORTSIG);
	public final PrimSig OUTPORTSIG = new PrimSig("OutPort", PORTSIG);
		
	public final PrimSig SYSTEMSIG = new PrimSig("system", Attr.ABSTRACT);		
	public final Field SYSINPORTSSIG = SYSTEMSIG.addField("inPorts", INPORTSIG.setOf());
	public final Field SYSOUTPORTSSIG = SYSTEMSIG.addField("outPorts", OUTPORTSIG.setOf());
	
	public final PrimSig CONNSIG = new PrimSig("connection", Attr.ABSTRACT);			
	public final Field CONNSRCPORTSIG = CONNSIG.addField("srcPort", PORTSIG.oneOf());
	public final Field CONNDESTPORTSIG = CONNSIG.addField("destPort", PORTSIG.oneOf());

	
	/**
	 * abstract sig Bool {}
	 * one sig true, false, unknown extends Bool {} 
	 * */
	public final PrimSig BOOLSIG = new PrimSig("Bool", Attr.ABSTRACT);
	public final PrimSig TRUESIG = new PrimSig("true", BOOLSIG, Attr.ONE);
	public final PrimSig FALSESIG = new PrimSig("false", BOOLSIG, Attr.ONE);
	public final PrimSig UNKNOWNBOOLSIG = new PrimSig("unknown_Bool", BOOLSIG, Attr.ONE);
	
	/**
	 * abstract sig DAL {}
	 * one sig zero, one, two, ..., nine, unknownDAL extends DAL {} 
	 * */
	public final PrimSig DALSIG = new PrimSig("DAL", Attr.ABSTRACT); 
	public String[] DALNames = {"Zero", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine"};
	public final PrimSig UNKNOWNDALSIG = new PrimSig("unknown_DAL", DALSIG, Attr.ONE);
	
	
	/**
	 * Load built-in Alloy constructs
	 * */
	public void loadBuiltinConstructs() {
		compNameToSigMap.put("port", PORTSIG);
		compNameToSigMap.put("InPort", INPORTSIG);
		compNameToSigMap.put("OutPort", OUTPORTSIG);
		compNameToSigMap.put("system", SYSTEMSIG);
		compNameToSigMap.put("connection", CONNSIG);
		
		propNameToSigMap.put("Bool", BOOLSIG);
		propNameToSigMap.put("true", TRUESIG);
		propNameToSigMap.put("false", FALSESIG);
		propNameToSigMap.put("unknown_Bool", UNKNOWNBOOLSIG);		
		propNameToSigMap.put("unknown_DAL", UNKNOWNDALSIG);
		propNameToSigMap.put("DAL", DALSIG);
		
		for(int i = 0; i < DALNames.length; ++i) {
			PrimSig dalSig = new PrimSig(DALNames[i], DALSIG, Attr.ONE);
			propNameToSigMap.put(DALNames[i], dalSig);
		}
	}
	
	Expr mkFacts(List<Expr> facts, StringBuilder sb) {		
		Expr expr = null;
		
		if(!facts.isEmpty()) {	
			expr = facts.get(0);
			sb.append(expr.toString()).append("\n");
			for(int i = 1; i < facts.size(); ++i) {
				expr = expr.and(facts.get(i));
				sb.append(facts.get(i).toString()).append("\n");				
			}
		}
		if(expr == null) {
			expr = ExprBinary.Op.IN.make(Pos.UNKNOWN, Pos.UNKNOWN, Sig.NONE, Sig.UNIV);
			sb.append(expr.toString()).append("\n");
		}
		return expr;
	}
	
	void printSigs(Set<Sig> sigs, StringBuilder sb) {
		for(Sig sig : sigs) {
			if(sig.isAbstract != null) {
				sb.append("abstract ");
			}
			if(sig.isOne != null)  {
				sb.append("one ");
			}
			if(sig instanceof PrimSig) {
				PrimSig primSig = (PrimSig)sig;
				sb.append("sig ").append(primSig.label+" ");
				if(primSig.parent != null) {
					if(!primSig.parent.label.equalsIgnoreCase("univ")) {
						sb.append("extends ").append(primSig.parent.label);
					}
				}
				sb.append("{").append("\n");
				for(int i = 0; i < primSig.getFields().size(); ++i) {
					Field field = primSig.getFields().get(i);
					sb.append("    ").append(field.label).append(" : ");
					sb.append(field.decl().expr);
					if(i < primSig.getFieldDecls().size()-1) {
						sb.append(",");
					}
					sb.append("\n");
				}
				sb.append("}").append("\n");
			} else if(sig instanceof SubsetSig) {
				SubsetSig subsetSig = (SubsetSig)sig;
				sb.append("sig ").append(subsetSig.label+" ");
				if(!subsetSig.parents.isEmpty()) {
					sb.append("in ").append(subsetSig.parents.get(0).label);
				}
				sb.append("{");
				for(int i = 0; i < subsetSig.getFields().size(); ++i) {
					Field field = subsetSig.getFields().get(i);
					sb.append("    ").append(field.label).append(" : ");
					sb.append(field.decl().expr);
					if(i < subsetSig.getFieldDecls().size()-1) {
						sb.append(",");
					}
					sb.append("\n");
				}
				sb.append("}").append("\n");				
			}
			for(Expr fact : sig.getFacts()) {
				sb.append(fact.toString()).append("\n");
			}
			sb.append("\n");
		}
	}
	
	public void execute () {
		StringBuilder sb = new StringBuilder("");
        // Chooses the Alloy4 options
        A4Options opt = new A4Options();
        opt.solver = A4Options.SatSolver.MiniSatJNI;
        opt.symmetry = 0;
        Set<Sig> filterSigSet = new LinkedHashSet<>();
        
        filterSigSet.addAll(compNameToSigMap.values());
        filterSigSet.addAll(propNameToSigMap.values());

        // Print sigs
        printSigs(filterSigSet, sb);
        Expr fact = mkFacts(facts, sb);
        
        Expr body = null;
        
        Func isSoftware = null;
        
//        if(testField != null) {
//        	Decl sys = SYSTEMSIG.oneOf("s");
//        	Sig positionEstimatorSig = null;
//        	
//        	for(Sig sig : subcompSigs) {
//        		if(sig.label.equalsIgnoreCase("positionEstimator")) {
//        			positionEstimatorSig = sig;
//        			break;
//        		}
//        	}
//        	body = sys.get().join(testField).equal(testSig);
//        	isSoftware = new Func(null, "isSoftware", edu.mit.csail.sdg.alloy4.Util.asList(sys), null, body);
//        	Expr expr = isSoftware.call(positionEstimatorSig);
//        	sb.append(expr.toString());
//        	fact = fact.and(expr);
//        }
        
        // Prepare the command to execute the Alloy model 
        Command cmd = new Command(false, 100, 4, 4, fact);
        cmd = cmd.change(SYSTEMSIG, true, systemNum)
        		.change(CONNSIG, true, connectionNum)
                		.change(PORTSIG, true, portNum);
        long startTime = System.currentTimeMillis();


        System.out.println("********************* Alloy Model *********************");        
        sb.append("\n").append(cmd.toString()).append("\n");
        System.out.println(sb.toString());
        System.out.println("*******************************************************");        
        
        A4Solution sol1 = TranslateAlloyToKodkod.execute_command(NOP, filterSigSet, cmd, opt);
        System.out.println("[Solution1]:");        
        System.out.println(sol1.toString());
        
        long endTime = System.currentTimeMillis();

        System.out.println("Executing Alloy takes: " + (endTime - startTime)/1000 + " seconds");
	}	
	
}
