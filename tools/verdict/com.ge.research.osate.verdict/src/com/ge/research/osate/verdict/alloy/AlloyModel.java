package com.ge.research.osate.verdict.alloy;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import edu.mit.csail.sdg.alloy4.Pair;
import edu.mit.csail.sdg.alloy4.Pos;
import edu.mit.csail.sdg.ast.Attr;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.ExprBinary;
import edu.mit.csail.sdg.ast.ExprUnary;
import edu.mit.csail.sdg.ast.ExprVar;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.ast.Sig.Field;
import edu.mit.csail.sdg.ast.Sig.PrimSig;
import edu.mit.csail.sdg.ast.Sig.SubsetSig;

/**
 * This file contains the Alloy AST elements that should exist in every model.
 * I did it in kind of a strange way that may not be the best.
 */
public class AlloyModel {
	public static final Map<String, Sig> typeMap = new LinkedHashMap<>();
	public static final Map<Pair<Sig, String>, Field> fieldMap = new LinkedHashMap<>();
	public static final Map<String, Sig> valueMap = new LinkedHashMap<>();
	
	private static final Sig addRelabelType(String relabel, Sig sig) {
		typeMap.put(relabel, sig);
		return sig;
	}
	
	private static final Sig addType(Sig sig) {
		return addRelabelType(sig.label, sig);
	}
	
	private static final Field addRelabelField(Sig sig, String label, Expr bound, String relabel) {
		Field field = sig.addField(label, bound);
		// We reference values of type "system" through the System sig, not e.g. the Component sig
		while (sig instanceof SubsetSig) {
			sig = ((SubsetSig) sig).parents.get(0);
		}
		fieldMap.put(new Pair<>(sig, relabel), field);
		return field;
	}
	
	private static final Field addField(Sig sig, String label, Expr bound) {
		return addRelabelField(sig, label, bound, label);
	}
	
	private static final Sig addRelabelValue(Sig sig, String relabel) {
		valueMap.put(relabel, sig);
		return sig;
	}
	
	private static final Sig addValue(Sig sig) {
		return addRelabelValue(sig, sig.label);
	}
	
	public static final Expr makeThis(Sig sig) {
		return ExprVar.make(Pos.UNKNOWN, "this/" + sig.label, sig.type());
	}
	
	public static final Expr joinThis(Sig sig, Expr toJoin) {
		return ExprBinary.Op.JOIN.make(Pos.UNKNOWN, Pos.UNKNOWN, makeThis(sig), toJoin);
	}
	
	public static final Sig Bool = new PrimSig("Bool");
	public static enum Bool_ {
		// We need to do the relabeling because enums can't be declared with lowercase letters
		True("true"), False("false"), UnknownBool("unkownBool");
		
		public final Sig sig;
		
		private Bool_(String relabel) {
			sig = addRelabelValue(new SubsetSig(name(), Arrays.asList(Bool), Attr.ONE), relabel);
		}
	}
	
	
	public static final Sig Category = new PrimSig("Category");
	public static enum Category_ {
		GPSCat, UMUCat, LIDARCat, UnknownCat;
		
		public final Sig sig;
		
		private Category_() {
			sig = addValue(new SubsetSig(name(), Arrays.asList(Category), Attr.ONE));
		}
	}
	
	public static final Sig ComponentKind = new PrimSig("ComponentKind");
	public static enum ComponentKind_ {
		Software, Hardware, Hybrid, Human, UnknownCompType;
		
		public final Sig sig;
		
		private ComponentKind_() {
			sig = addValue(new SubsetSig(name(), Arrays.asList(ComponentKind), Attr.ONE));
		}
	}
	
	public static final Sig Manufacturer = new PrimSig("Manufacturer");
	public static enum Manufacturer_ {
		InHouse, ThirdParty, UnknownMan;
		
		public final Sig sig;
		
		private Manufacturer_() {
			sig = addValue(new SubsetSig(name(), Arrays.asList(Manufacturer), Attr.ONE));
		}
	}
	
	public static final Sig ConnectionKind = new PrimSig("ConnectionKind");
	public static enum ConnectionKind_ {
		Remote, Internal, UnknownConnType;
		
		public final Sig sig;
		
		private ConnectionKind_() {
			sig = addValue(new SubsetSig(name(), Arrays.asList(ConnectionKind), Attr.ONE));
		}
	}
	
	public static final Sig PortDirection = new PrimSig("PortDirection");
	public static enum PortDirection_ {
		In, Out;
		
		public final Sig sig;
		
		private PortDirection_() {
			sig = addValue(new SubsetSig(name(), Arrays.asList(PortDirection), Attr.ONE));
		}
	}
	
	
	public static final Sig Connection = addRelabelType("connection", new PrimSig("Connection"));
	
	public static final Sig Port = addRelabelType("port", new PrimSig("Port"));
	public static final class Port_ {
		public static final Field direction = addField(Port, "direction", PortDirection.oneOf());
		public static final Field portConnections = addRelabelField(Port, "portConnections", Connection.setOf(), "connections");
	}
	
	public static final Sig InPort = new SubsetSig("InPort", Arrays.asList(Port), Attr.ONE);
	public static final Sig OutPort = new SubsetSig("OutPort", Arrays.asList(Port), Attr.ONE);
	
	public static final Sig System = addRelabelType("system", new PrimSig("System"));
	public static final class System_ {
		public static final Field category = addField(System, "category", Category.oneOf());
		public static final Field inports = addField(System, "inports", InPort.setOf());
		public static final Field outports = addField(System, "outports", OutPort.setOf());
		public static final Field ports = addField(System, "ports", Port);
		public static final Field systemConnections = addRelabelField(System, "systemConnections", Connection.setOf(), "connections");
	}
	
	public static final Sig Component = new SubsetSig("Component", Arrays.asList(System), new Attr[] {});
	public static final class Component_ {
		public static final Field componentType = addField(Component, "componentType", ComponentKind.oneOf());
		public static final Field manufacturer = addField(Component, "manufacturer", Manufacturer.oneOf());
		public static final Field insideTrustedBoundary = addField(Component, "insideTrustedBoundary", Bool.oneOf());
	}
	
	public static final Sig ComponentImpl = new SubsetSig("ComponentIml", Arrays.asList(System), new Attr[] {});
	public static final class ComponentImpl_ {
		public static final Field subcomponents = addField(ComponentImpl, "subcomponents", System.oneOf());
	}
	
	// For Connection - split due to cyclic dependency
	public static final class Connection_ {
		public static final Field srcPort = addField(Connection, "srcPort", Port.oneOf());
		public static final Field destPort = addField(Connection, "destPort", Port.oneOf());
		public static final Field src = addField(Connection, "src", System.oneOf());
		public static final Field dest = addField(Connection, "dest", System.oneOf());
		public static final Field connectionType = addField(Connection, "connectionType", ConnectionKind.oneOf());
		public static final Field dataEncrypted = addField(Connection, "dataEncrypted", Bool.oneOf());
		public static final Field authenticated = addField(Connection, "authenticated", Bool.oneOf());
	}
	
	// For InPort, OutPort - split due to cyclic dependency
	static {
		InPort.addFact(ExprBinary.Op.EQUALS.make(Pos.UNKNOWN, Pos.UNKNOWN,
				ExprBinary.Op.JOIN.make(Pos.UNKNOWN, Pos.UNKNOWN, makeThis(InPort), Port_.direction),
				PortDirection_.In.sig));
		InPort.addFact(ExprBinary.Op.EQUALS.make(Pos.UNKNOWN, Pos.UNKNOWN,
				ExprBinary.Op.JOIN.make(Pos.UNKNOWN, Pos.UNKNOWN, makeThis(InPort),
						ExprUnary.Op.TRANSPOSE.make(Pos.UNKNOWN, Port_.portConnections)),
				ExprBinary.Op.JOIN.make(Pos.UNKNOWN, Pos.UNKNOWN, makeThis(InPort),
						ExprUnary.Op.TRANSPOSE.make(Pos.UNKNOWN, Connection_.destPort))));
	}
	static {
		OutPort.addFact(ExprBinary.Op.EQUALS.make(Pos.UNKNOWN, Pos.UNKNOWN,
				ExprBinary.Op.JOIN.make(Pos.UNKNOWN, Pos.UNKNOWN, makeThis(OutPort), Port_.direction),
				PortDirection_.Out.sig));
		OutPort.addFact(ExprBinary.Op.EQUALS.make(Pos.UNKNOWN, Pos.UNKNOWN,
				ExprBinary.Op.JOIN.make(Pos.UNKNOWN, Pos.UNKNOWN, makeThis(OutPort),
						ExprUnary.Op.TRANSPOSE.make(Pos.UNKNOWN, Port_.portConnections)),
				ExprBinary.Op.JOIN.make(Pos.UNKNOWN, Pos.UNKNOWN, makeThis(OutPort),
						ExprUnary.Op.TRANSPOSE.make(Pos.UNKNOWN, Connection_.srcPort))));
	}
	
	// For System - split due to cyclic dependency
	static {
		System.addFact(ExprBinary.Op.EQUALS.make(Pos.UNKNOWN, Pos.UNKNOWN, System_.ports,
				ExprBinary.Op.PLUS.make(Pos.UNKNOWN, Pos.UNKNOWN, System_.inports, System_.outports)));
		System.addFact(ExprBinary.Op.EQUALS.make(Pos.UNKNOWN, Pos.UNKNOWN,
				ExprBinary.Op.JOIN.make(Pos.UNKNOWN, Pos.UNKNOWN, makeThis(System),
						System_.systemConnections),
				ExprBinary.Op.JOIN.make(Pos.UNKNOWN, Pos.UNKNOWN, makeThis(System),
						ExprBinary.Op.PLUS.make(Pos.UNKNOWN, Pos.UNKNOWN,
								ExprUnary.Op.TRANSPOSE.make(Pos.UNKNOWN, Connection_.src),
								ExprUnary.Op.TRANSPOSE.make(Pos.UNKNOWN, Connection_.dest)))));
	}
	
	static {
		// Force them to load
		// This is kind of disgusting and unforseen
		@SuppressWarnings("unused")
		Bool_ bool = Bool_.UnknownBool;
		@SuppressWarnings("unused")
		Category_ category = Category_.UnknownCat;
		@SuppressWarnings("unused")
		ComponentKind_ componentKind = ComponentKind_.UnknownCompType;
		@SuppressWarnings("unused")
		Manufacturer_ manufacturer = Manufacturer_.UnknownMan;
		@SuppressWarnings("unused")
		ConnectionKind_ connectionKind = ConnectionKind_.UnknownConnType;
		@SuppressWarnings("unused")
		PortDirection_ portDirection = PortDirection_.In;
		@SuppressWarnings("unused")
		Port_ port = new Port_();
		@SuppressWarnings("unused")
		System_ system = new System_();
		@SuppressWarnings("unused")
		Component_ component = new Component_();
		@SuppressWarnings("unused")
		ComponentImpl_ componentImpl = new ComponentImpl_();
		@SuppressWarnings("unused")
		Connection_ connection = new Connection_();
	}
}
