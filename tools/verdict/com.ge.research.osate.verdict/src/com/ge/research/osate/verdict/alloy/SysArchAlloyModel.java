package com.ge.research.osate.verdict.alloy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.mit.csail.sdg.alloy4.Pair;
import edu.mit.csail.sdg.alloy4.Pos;
import edu.mit.csail.sdg.ast.Attr;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.ExprBinary;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.ast.Sig.Field;
import edu.mit.csail.sdg.ast.Sig.PrimSig;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;
import kodkod.ast.BinaryExpression;

import static edu.mit.csail.sdg.alloy4.A4Reporter.NOP;

public class SysArchAlloyModel {
	public static int system = 0, connection = 0, port = 0;
	public static String UNKNOWN = "unknown_";
	public static List<Expr> facts = new ArrayList<>();
	public static final Map<String, Sig> compNameToSigMap = new HashMap<>();
	public static final Map<String, Sig> propNameToSigMap = new HashMap<>();
	public static final Map<Pair<Sig, String>, Field> compSigFdNameToFdMap = new HashMap<>();
	
	/** 
	 * abstract sig Port {}
	 one sig InPort extends Port {}
	 one sig OutPort extends Port {}
	 */
	public static final PrimSig PORT = new PrimSig("port", Attr.ABSTRACT);
	public static final PrimSig INPORT = new PrimSig("InPort", PORT, Attr.ONE);
	public static final PrimSig OUTPORT = new PrimSig("OutPort", PORT, Attr.ONE);
	

	/**
	abstract sig AadlModel {}
	one sig HybridComponent extends AadlModel {}
	one sig PlatformComponent extends AadlModel {}
	one sig SoftwareComponent extends AadlModel {}
	one sig System extends AadlModel {
		inPorts : set InPort,
	 	   outPorts : set OutPort,	
	}	
	
	one sig ComponentType extends System {}	
	one sig ComponentImpl extends ComponentType {
		subcomponents : set system,	
	    connections : set connection,
	*/	
	public static final PrimSig AADLMODEL = new PrimSig("AadlModel", Attr.ABSTRACT);
//	public static final PrimSig HYBRIDCOMP = new PrimSig("HybridComponent", AADLMODEL, Attr.ONE);
//	public static final PrimSig PLATFORMCOMP = new PrimSig("PlatformComponent", AADLMODEL, Attr.ONE);
//	public static final PrimSig SOFTWARECOMP = new PrimSig("SoftwareComponent", AADLMODEL, Attr.ONE);
	public static final PrimSig SYSTEM = new PrimSig("system", AADLMODEL);
	public static final PrimSig CONNECTION = new PrimSig("connection");
	
	public static final Field CONNECTIONSRCPORT = SYSTEM.addField("srcPort", PORT.oneOf());
	public static final Field CONNECTIONDESTPORT = SYSTEM.addField("destPort", PORT.oneOf());
	
	public static final Field SYSINPORTS = SYSTEM.addField("inPorts", INPORT.oneOf());
	public static final Field SYSOUTPORTS = SYSTEM.addField("outPorts", OUTPORT.oneOf());
	public static final PrimSig COMPTYPE = new PrimSig("ComponentType", SYSTEM);
	public static final PrimSig COMPIMPL = new PrimSig("ComponentImpl", SYSTEM);
	public static final Field COMPIMPLSUBCOMPS = SYSTEM.addField("subcomponents", SYSTEM.setOf());
	public static final Field COMPIMPLCONNECTIONS = SYSTEM.addField("connections", CONNECTION.setOf());
	
	/**
	 * abstract sig Bool {}
	 * one sig true, false, unknown extends Bool {} 
	 * */
	public static final PrimSig BOOL = new PrimSig("Bool", Attr.ABSTRACT);
	public static final PrimSig TRUE = new PrimSig("true", BOOL, Attr.ONE);
	public static final PrimSig FALSE = new PrimSig("false", BOOL, Attr.ONE);
	public static final PrimSig UNKNOWNBOOL = new PrimSig("unknown_Bool", BOOL, Attr.ONE);
	
	/**
	 * abstract sig DAL {}
	 * one sig zero, one, two, ..., nine, unknownDAL extends DAL {} 
	 * */
	public static final PrimSig DAL = new PrimSig("DAL", Attr.ABSTRACT); 
	public static String[] DALNames = {"zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine"};
	public static final PrimSig UNKNOWNDAL = new PrimSig("unknown_DAL", DAL, Attr.ONE);
	
	/**
	 * Load built-in Alloy constructs
	 * */
	public static void loadBuiltinConstructs() {
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
	
	static Expr mkAnd(List<Expr> facts) {
		Expr expr = null;
		if(facts.size() > 0) {
			expr = facts.get(0);
			for(int i = 1; i < facts.size(); ++i) {
				expr = expr.and(facts.get(i));
			}
		}
		return expr;
	}
	
	public static void execute () {
        // Chooses the Alloy4 options
        A4Options opt = new A4Options();
        opt.solver = A4Options.SatSolver.SAT4J;
        List<Sig> sigs = new ArrayList<>();
        sigs.addAll(compNameToSigMap.values());
        sigs.addAll(propNameToSigMap.values());
        
        System.out.println("All sigs: " + sigs);
        System.out.println("All facts: " + mkAnd(facts));
        
        Command cmd1 = new Command(false, 10, 10, 10, mkAnd(facts));
//        CompUtil.flushModelToFile(model, tmpAls);
        A4Solution sol1 = TranslateAlloyToKodkod.execute_command(NOP, sigs, cmd1, opt);
        System.out.println("[Solution1]:");
        System.out.println(sol1.toString());        
	}	
}
