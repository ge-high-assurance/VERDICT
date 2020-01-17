package com.ge.research.osate.verdict.alloy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.osate.aadl2.DataPort;
import org.osate.aadl2.ModalPropertyValue;
import org.osate.aadl2.Property;
import org.osate.aadl2.PropertyAssociation;
import org.osate.aadl2.PublicPackageSection;
import org.osate.aadl2.Subcomponent;
import org.osate.aadl2.SystemImplementation;
import org.osate.aadl2.SystemType;
import org.osate.aadl2.impl.PropertyImpl;

import edu.mit.csail.sdg.alloy4.Pair;
import edu.mit.csail.sdg.alloy4.Pos;
import edu.mit.csail.sdg.ast.Attr;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.ExprBinary;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.ast.Sig.Field;
import edu.mit.csail.sdg.ast.Sig.SubsetSig;

public class AadlAlloyTranslator {
	protected static final List<String> systemProperties =
			Arrays.asList("category", "componentType", "manufacturer", "insideTrustedBoundary");
	protected static final List<String> connectionProperties =
			Arrays.asList("connectionType", "dataEncrypted", "authenticated");
	
	public static class Result {
		public int system, connection, port;
		public List<Sig> sigs;
		public List<Expr> facts;
		
		protected Map<String, Sig> systemMap;
		protected Map<Pair<String, String>, Field> fieldMap;
		
		protected Result() {
			system = 0;
			connection = 0;
			port = 0;
			sigs = new ArrayList<>();
			facts = new ArrayList<>();
			
			systemMap = new LinkedHashMap<>();
			fieldMap = new LinkedHashMap<>();
		}
	}
	
	protected static String sanitizeName(String name) {
		return name != null ? name.replace("\\.", "_") : "";
	}
	
	public static Result fromObjects(Collection<EObject> objects) {
		List<PublicPackageSection> models = objects.stream()
				.map(AadlAlloyTranslator::getModel)
				.flatMap(Util::streamOfOptional)
				.collect(Collectors.toList());
		
		List<SystemType> systems = new ArrayList<>();
		List<SystemImplementation> systemImpls = new ArrayList<>();
		
		for (PublicPackageSection model : models) {
			TreeIterator<EObject> it = model.eAllContents();
			while (it.hasNext()) {
				EObject obj = it.next();
				if (obj instanceof SystemType) {
					systems.add((SystemType) obj);
					it.prune();
				} else if (obj instanceof SystemImplementation) {
					systemImpls.add((SystemImplementation) obj);
					it.prune();
				}
			}
		}
		
		Result result = new Result();
		
		for (SystemType system : systems) {
			translateSystem(system, result);
		}
		for (SystemImplementation systemImpl : systemImpls) {
			translateSystemImpl(systemImpl, result);
		}
		
		// TODO state disjunction of all ports
		
		return result;
	}
	
	protected static Optional<PublicPackageSection> getModel(EObject obj) {
		return Util.searchEObject(obj, PublicPackageSection.class);
	}
	
	protected static void translateSystem(SystemType system, Result result) {
		Sig systemSig = new SubsetSig(sanitizeName(system.getName()),
				Arrays.asList(AlloyModel.Component), new Attr[] {});
		
		Expr inportsExpr = null, outportsExpr = null;;
		
		for (DataPort port : system.getOwnedDataPorts()) {
			Field field;
			switch (port.getDirection()) {
			case IN:
				field = systemSig.addField(sanitizeName(port.getName()), AlloyModel.InPort.oneOf());
				if (inportsExpr == null) {
					inportsExpr = field;
				} else {
					inportsExpr = ExprBinary.Op.PLUS.make(Pos.UNKNOWN, Pos.UNKNOWN, inportsExpr, field);
				}
				break;
			case OUT:
				field = systemSig.addField(sanitizeName(port.getName()), AlloyModel.OutPort.oneOf());
				if (outportsExpr == null) {
					outportsExpr = field;
				} else {
					outportsExpr = ExprBinary.Op.PLUS.make(Pos.UNKNOWN, Pos.UNKNOWN, outportsExpr, field);
				}
				break;
			case IN_OUT:
			default:
				throw new RuntimeException("In/out port not supported");
			}
			
			result.fieldMap.put(new Pair<>(sanitizeName(system.getName()), sanitizeName(port.getName())), field);
			result.port++;
		}
		
		if (inportsExpr != null) {
			systemSig.addFact(ExprBinary.Op.EQUALS.make(Pos.UNKNOWN, Pos.UNKNOWN,
					AlloyModel.joinThis(systemSig, AlloyModel.System_.inports),
					AlloyModel.joinThis(systemSig, inportsExpr)));
		}
		if (outportsExpr != null) {
			systemSig.addFact(ExprBinary.Op.EQUALS.make(Pos.UNKNOWN, Pos.UNKNOWN,
					AlloyModel.joinThis(systemSig, AlloyModel.System_.outports),
					AlloyModel.joinThis(systemSig, outportsExpr)));
		}
		
		// Ignoring category because it is unclear how to implement it
		
		result.sigs.add(systemSig);
		result.systemMap.put(sanitizeName(system.getName()), systemSig);
	}
	
	protected static void translateSystemImpl(SystemImplementation systemImpl, Result result) {
		String systemName = sanitizeName(systemImpl.getName().split("\\.")[0]);
		String systemImplName = sanitizeName(systemImpl.getName());
		if (!result.systemMap.containsKey(systemName)) {
			throw new RuntimeException("Missing system " + systemName + " for system impl " + systemImpl.getName());
		}
		Sig systemSig = result.systemMap.get(systemName);
		Sig systemImplSig = new Sig.SubsetSig(systemImplName, Arrays.asList(AlloyModel.ComponentImpl, systemSig), Attr.ONE);
		
		Expr subcomponentsExpr = null;
		
		for (Subcomponent comp : systemImpl.getOwnedSubcomponents()) {
			Sig typeSig = null;
			if (comp.getComponentType() != null) {
				typeSig = result.systemMap.get(sanitizeName(comp.getComponentType().getName()));
			} else if (comp.getSubcomponentType() != null) {
				typeSig = result.systemMap.get(sanitizeName(comp.getSubcomponentType().getName()));
			}
			if (typeSig == null) {
				System.err.println("Could not find system type for subcomponent: " + comp.getName());
				continue;
			}
			Sig compSig = new Sig.SubsetSig(sanitizeName(comp.getName()), Arrays.asList(typeSig), Attr.ONE);
			
			if (subcomponentsExpr == null) {
				subcomponentsExpr = compSig;
			} else {
				subcomponentsExpr = ExprBinary.Op.PLUS.make(Pos.UNKNOWN, Pos.UNKNOWN, subcomponentsExpr, compSig);
			}
			
			// TODO properties
			
			// Will 2020-01-17:
			// I haven't figured out how to get property key-value pairs.
			// We know the set of valid properties - systemProperties.
			// We can also get the list of properties - see below.
			// But upon inspection of the property objects, I haven't figured out how to recover
			// the names of the properties, so I can't match up keys to values.
			// I only spent 20 minutes or so trying to figure this out,
			// so it might be pretty simple.
			
			System.out.println("translating subcomponent: " + comp.getName());
			
			for (String propName : systemProperties) {
				
			}
			for (PropertyAssociation prop : comp.getOwnedPropertyAssociations()) {
				System.out.println("propery name: " + prop.getProperty().getName());
				for (ModalPropertyValue val : prop.getOwnedValues()) {
					System.out.println("value name: " + val.getName());
					System.out.println("value: " + val.getOwnedValue());
				}
			}
			
			result.sigs.add(compSig);
			result.system++;
		}
		
		// TODO connections
		
		// Will 2020-01-17:
		// Need to instantiate "one sig" for each connection.
		// Also need to state facts about each field for each connection.
		
		if (subcomponentsExpr != null) {
			systemImplSig.addFact(ExprBinary.Op.EQUALS.make(Pos.UNKNOWN, Pos.UNKNOWN,
					AlloyModel.joinThis(systemSig, AlloyModel.ComponentImpl_.subcomponents),
					subcomponentsExpr));
		}
		
		result.sigs.add(systemImplSig);
		result.systemMap.put(systemImplName, systemImplSig);
		result.system++;
	}
}
