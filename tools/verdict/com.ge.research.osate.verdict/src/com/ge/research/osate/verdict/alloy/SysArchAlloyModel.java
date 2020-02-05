package com.ge.research.osate.verdict.alloy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.mit.csail.sdg.alloy4.Pair;
import edu.mit.csail.sdg.ast.Attr;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.ast.Sig.Field;
import edu.mit.csail.sdg.ast.Sig.PrimSig;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;

import static edu.mit.csail.sdg.alloy4.A4Reporter.NOP;

public class SysArchAlloyModel {
	public int system = 0, connection = 0, port = 0;
	public final List<Expr> allInports = new ArrayList<>();
	public final List<Expr> allOutports = new ArrayList<>();
	
	public final List<Expr> facts = new ArrayList<>();
	public final Map<String, Sig> compNameToSigMap = new HashMap<>();
	public final Map<String, Sig> propNameToSigMap = new HashMap<>();
	public final Map<Pair<Sig, String>, Field> compSigFdNameToFdMap = new HashMap<>();
	
	public String UNKNOWN = "unknown_";
	/** 
	 * abstract sig Port {}
	 one sig InPort extends Port {}
	 one sig OutPort extends Port {}
	 */
	public final PrimSig PORT = new PrimSig("port", Attr.ABSTRACT);
	public final PrimSig INPORT = new PrimSig("InPort", PORT);
	public final PrimSig OUTPORT = new PrimSig("OutPort", PORT);
	

	/**
	abstract sig AadlModel {}
	one sig HybridComponent extends AadlModel {}
	one sig PlatformComponent extends AadlModel {}
	one sig SoftwareComponent extends AadlModel {}
	sig System extends AadlModel {
		inPorts : set InPort,
	 	outPorts : set OutPort,	
	}	
	
	sig ComponentType extends System {}	
	sig ComponentImpl extends ComponentType {
		subcomponents : set system,	
	    connections : set connection,
	*/	
	public final PrimSig AADLMODEL = new PrimSig("AADLModel", Attr.ABSTRACT);
	public final PrimSig SYSTEM = new PrimSig("system", AADLMODEL);
	public final PrimSig CONNECTION = new PrimSig("connection");
	
	public final Field CONNECTIONSRCPORT = CONNECTION.addField("srcPort", PORT.oneOf());
	public final Field CONNECTIONDESTPORT = CONNECTION.addField("destPort", PORT.oneOf());
	
	public final Field SYSINPORTS = SYSTEM.addField("inPorts", INPORT.oneOf());
	public final Field SYSOUTPORTS = SYSTEM.addField("outPorts", OUTPORT.oneOf());
	public final PrimSig COMPTYPE = new PrimSig("ComponentType", SYSTEM);
	public final PrimSig COMPIMPL = new PrimSig("ComponentImpl", COMPTYPE);
	public final Field COMPIMPLSUBCOMPS = COMPIMPL.addField("subcomponents", SYSTEM.setOf());
	public final Field COMPIMPLCONNECTIONS = COMPIMPL.addField("connections", CONNECTION.setOf());
	
//	public static final PrimSig HYBRIDCOMP = new PrimSig("HybridComponent", AADLMODEL, Attr.ONE);
//	public static final PrimSig PLATFORMCOMP = new PrimSig("PlatformComponent", AADLMODEL, Attr.ONE);
//	public static final PrimSig SOFTWARECOMP = new PrimSig("SoftwareComponent", AADLMODEL, Attr.ONE);
	
	/**
	 * abstract sig Bool {}
	 * one sig true, false, unknown extends Bool {} 
	 * */
	public final PrimSig BOOL = new PrimSig("Bool", Attr.ABSTRACT);
	public final PrimSig TRUE = new PrimSig("true", BOOL, Attr.ONE);
	public final PrimSig FALSE = new PrimSig("false", BOOL, Attr.ONE);
	public final PrimSig UNKNOWNBOOL = new PrimSig("unknown_Bool", BOOL, Attr.ONE);
	
	/**
	 * abstract sig DAL {}
	 * one sig zero, one, two, ..., nine, unknownDAL extends DAL {} 
	 * */
	public final PrimSig DAL = new PrimSig("DAL", Attr.ABSTRACT); 
	public String[] DALNames = {"zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine"};
	public final PrimSig UNKNOWNDAL = new PrimSig("unknown_DAL", DAL, Attr.ONE);
	
	/**
	 * Load built-in Alloy constructs
	 * */
	public void loadBuiltinConstructs() {
		compNameToSigMap.put("port", PORT);
		compNameToSigMap.put("InPort", INPORT);
		compNameToSigMap.put("OutPort", OUTPORT);
		
		compNameToSigMap.put("AadlModel", AADLMODEL);
		compNameToSigMap.put("system", SYSTEM);
		compNameToSigMap.put("connection", CONNECTION);
		compNameToSigMap.put("ComponentType", COMPTYPE);
		compNameToSigMap.put("ComponentImpl", COMPIMPL);
		
		propNameToSigMap.put("Bool", BOOL);
		propNameToSigMap.put("true", TRUE);
		propNameToSigMap.put("false", FALSE);
		propNameToSigMap.put("unknown_Bool", UNKNOWNBOOL);		
		propNameToSigMap.put("unknown_DAL", DAL);
		
		for(int i = 0; i < DALNames.length; ++i) {
			PrimSig dalSig = new PrimSig(DALNames[i], DAL, Attr.ONE);
			propNameToSigMap.put(DALNames[i], dalSig);
		}
	}
	
	Expr mkFacts(List<Expr> facts, StringBuilder sb) {		
		Expr expr = null;
		if(facts.size() > 0) {
			expr = facts.get(0);
			sb.append(facts.get(0).toString()).append("\n");
			for(int i = 1; i < facts.size(); ++i) {
				expr = expr.and(facts.get(i));
				sb.append(facts.get(i).toString()).append("\n");				
			}
		}
		return expr;
	}
	
	void printSigs(List<Sig> sigs, StringBuilder sb) {
		for(Sig sig : sigs) {			
			sb.append(sig.explain()).append("\n");
		}
	}
	
	public void execute () {
		StringBuilder sb = new StringBuilder("");
        // Chooses the Alloy4 options
        A4Options opt = new A4Options();
        opt.solver = A4Options.SatSolver.SAT4J;
        List<Sig> sigs = new ArrayList<>();
        sigs.addAll(compNameToSigMap.values());
        sigs.addAll(propNameToSigMap.values());
        
        printSigs(sigs, sb);
//        Command cmd0 = new Command(false, 15, 3, 3, facts.get(0));
        Command cmd1 = new Command(false, 15, 3, 3, mkFacts(facts, sb));
        System.out.println("********************* Alloy Model ********************* ");
        System.out.println(sb.toString());
        A4Solution sol1 = TranslateAlloyToKodkod.execute_command(NOP, sigs, cmd1, opt);
        System.out.println("[Solution1]:");
        System.out.println(sol1.toString());    
	}	
	
}
