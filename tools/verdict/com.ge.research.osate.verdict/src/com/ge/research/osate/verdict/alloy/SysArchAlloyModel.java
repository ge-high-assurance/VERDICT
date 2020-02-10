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
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.ast.Sig.Field;
import edu.mit.csail.sdg.ast.Sig.PrimSig;
import edu.mit.csail.sdg.ast.Sig.SubsetSig;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;

import static edu.mit.csail.sdg.alloy4.A4Reporter.NOP;

public class SysArchAlloyModel {
	public int system = 0, connection = 0, port = 0;
	public String UNKNOWN = "unknown_";
	public final List<Expr> allInstInports = new ArrayList<>();
	public final List<Expr> allInstOutports = new ArrayList<>();
	
	public final List<Expr> facts = new ArrayList<>();
	public final Map<String, Sig> compNameToSigMap = new LinkedHashMap<>();
	public final Map<String, Sig> propNameToSigMap = new LinkedHashMap<>();
	public final Map<Pair<Sig, String>, Field> compSigFdNameToFdMap = new LinkedHashMap<>();
	
	/**
	 * For printing
	 * */
	public final List<Sig> topLevelSigs = new ArrayList<>();
	public final Map<Pair<String, Sig>, Sig> subsetSigAndParent = new LinkedHashMap<>();
	public final Map<Pair<String, Sig>, Sig> subSigAndParent = new LinkedHashMap<>();
	
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
	
	public void addAlloyModelForPrinting() {
		// For printing
		// SYSTEM
		topLevelSigs.add(SYSTEMSIG);				
		// PORT
		topLevelSigs.add(PORTSIG);		
		subSigAndParent.put(new Pair<>("one", INPORTSIG), PORTSIG);
		subSigAndParent.put(new Pair<>("one", OUTPORTSIG), PORTSIG);
		// CONNECTION
		topLevelSigs.add(CONNSIG);		
		// BOOL
		topLevelSigs.add(BOOLSIG);
		subSigAndParent.put(new Pair<>("one", TRUESIG), BOOLSIG);
		subSigAndParent.put(new Pair<>("one", FALSESIG), BOOLSIG);
		subSigAndParent.put(new Pair<>("one", UNKNOWNBOOLSIG), BOOLSIG);
		//DAL
		topLevelSigs.add(DALSIG);
		subSigAndParent.put(new Pair<>("one", UNKNOWNDALSIG), DALSIG);
	}
	
	
	/**
	 * Load built-in Alloy constructs
	 * */
	public void loadBuiltinConstructs() {
		compNameToSigMap.put("port", PORTSIG);
		compNameToSigMap.put("InPort", INPORTSIG);
		compNameToSigMap.put("OutPort", OUTPORTSIG);
		compNameToSigMap.put("system", SYSTEMSIG);
		compNameToSigMap.put("connection", CONNSIG);
//		compNameToSigMap.put("Component", COMP);
//		compNameToSigMap.put("ComponentImpl", COMPIMPL);
		
//		propNameToSigMap.put("Bool", BOOL);
//		propNameToSigMap.put("true", TRUE);
//		propNameToSigMap.put("false", FALSE);
//		propNameToSigMap.put("unknown_Bool", UNKNOWNBOOL);		
//		propNameToSigMap.put("unknown_DAL", UNKNOWNDAL);
//		propNameToSigMap.put("DAL", DAL);
		
//		for(int i = 0; i < DALNames.length; ++i) {
//			PrimSig dalSig = new PrimSig(DALNames[i], DAL, Attr.ONE);
//			propNameToSigMap.put(DALNames[i], dalSig);
//			
//			// Add alloy model for printing
////			subSigAndParent.put(new Pair<>("one", dalSig), DAL);
//		}
		// Add alloy model for printing
//		addAlloyModelForPrinting();
	}
	
	Expr mkFacts(List<Expr> facts, StringBuilder sb) {		
		Expr expr = ExprBinary.Op.IN.make(Pos.UNKNOWN, Pos.UNKNOWN, SYSTEMSIG, Sig.UNIV);;
		sb.append(expr.toString()).append("\n");
		if(facts.size() > 0) {			
			for(int i = 0; i < facts.size(); ++i) {
				expr = expr.and(facts.get(i));
				sb.append(facts.get(i).toString()).append("\n");				
			}
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
					sb.append("extends ").append(primSig.parent.label);
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
        opt.solver = A4Options.SatSolver.SAT4J;
        Set<Sig> filterSigSet = new LinkedHashSet<>();
        filterSigSet.addAll(compNameToSigMap.values());
        filterSigSet.addAll(propNameToSigMap.values());

        
        // Print sigs
        printSigs(filterSigSet, sb);
        Expr fact = mkFacts(facts, sb);
//        mkFacts(facts, sb);
//        Command cmd0 = new Command(false, 15, 3, 3, facts.get(0));
        System.out.println("Total Ports: " + this.port);
        Command cmd1 = new Command(false, 20, 4, 4, fact);
        System.out.println("********************* Alloy Model ********************* ");
        sb.append("\n").append(cmd1.toString()).append("\n");
        System.out.println(sb.toString());
        
        A4Solution sol1 = TranslateAlloyToKodkod.execute_command(NOP, filterSigSet, cmd1, opt);
//        System.out.println("KodKod Input: ");
//        System.out.println(sol1.debugExtractKInput());
        System.out.println("[Solution1]:");        
        System.out.println(sol1.toString());
	}	
	
}
