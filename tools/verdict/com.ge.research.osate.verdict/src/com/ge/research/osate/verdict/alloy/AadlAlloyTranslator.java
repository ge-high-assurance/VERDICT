package com.ge.research.osate.verdict.alloy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.osate.aadl2.Connection;
import org.osate.aadl2.ConnectionEnd;
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
import edu.mit.csail.sdg.ast.Sig.PrimSig;

public class AadlAlloyTranslator {

	public static void translateFromAADLObjects(Collection<EObject> objects) {
		List<PublicPackageSection> models = objects.stream()
				.map(AadlAlloyTranslator::getModel)
				.flatMap(Util::streamOfOptional)
				.collect(Collectors.toList());
		
		List<SystemType> systems = new ArrayList<>();
		List<SystemImplementation> systemImpls = new ArrayList<>();
		
		// Collect component type and implementation 
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
		
		for (SystemType system : systems) {
			translateSystem(system);
		}
		for (SystemImplementation systemImpl : systemImpls) {
			translateSystemImpl(systemImpl);
			translateSystemImplSubcomponents(systemImpl);
			translateSystemImplConnections(systemImpl);
		}
		
		// TODO state disjunction of all ports
	}
	
	protected static Optional<PublicPackageSection> getModel(EObject obj) {
		return Util.searchEObject(obj, PublicPackageSection.class);
	}
	
	/**
	 * 
	 * Translate a System Type 
	 * */
	protected static void translateSystem(SystemType system) {
		String sanitizedSysName = sanitizeName(system.getName());
		Sig compTypeSig = new PrimSig(sanitizedSysName, SysArchAlloyModel.COMPTYPE, new Attr[] {});
		
		Expr allInportsUnionExpr = null, allOutportsUnionExpr = null;;
		
		for (DataPort port : system.getOwnedDataPorts()) {			
			Field field;
			String sanitizedPortName = sanitizeName(port.getName());
			
			switch (port.getDirection()) {
			case IN:
				field = compTypeSig.addField(sanitizedPortName, SysArchAlloyModel.INPORT.oneOf());
				if (allInportsUnionExpr == null) {
					allInportsUnionExpr = field;
				} else {
					allInportsUnionExpr = union(allInportsUnionExpr, field);
				}
				break;
			case OUT:
				field = compTypeSig.addField(sanitizedPortName, SysArchAlloyModel.OUTPORT.oneOf());
				if (allOutportsUnionExpr == null) {
					allOutportsUnionExpr = field;
				} else {
					allOutportsUnionExpr = union(allOutportsUnionExpr, field);
				}
				break;
			case IN_OUT:
			default:
				throw new RuntimeException("In/out port not supported");
			}
			
			SysArchAlloyModel.compSigFdNameToFdMap.put(new Pair<>(compTypeSig, sanitizedPortName), field);
			SysArchAlloyModel.port++;
		}
		
		// The union of all inports and outports should be equal to 
		// the component type field's inports and outports respectively 
		if (allInportsUnionExpr != null) {
			compTypeSig.addFact(equal(join(compTypeSig, SysArchAlloyModel.SYSINPORTS),
					join(compTypeSig, allInportsUnionExpr)));
		}
		if (allOutportsUnionExpr != null) {
			compTypeSig.addFact(equal(join(compTypeSig, SysArchAlloyModel.SYSOUTPORTS), join(compTypeSig, allOutportsUnionExpr)));
		}
		
		// Ignoring category because it is unclear how to implement it
		
		SysArchAlloyModel.compNameToSigMap.put(sanitizeName(system.getName()), compTypeSig);
	}
	
	/**
	 * Translate system implementations
	 * 
	 * */
	protected static void translateSystemImpl(SystemImplementation systemImpl) {
		String sanitizedSystemImplName = sanitizeName(systemImpl.getName());
		Sig systemImplSig = new PrimSig(sanitizedSystemImplName, SysArchAlloyModel.COMPIMPL);
		
		SysArchAlloyModel.compNameToSigMap.put(sanitizedSystemImplName, systemImplSig);
	}
	
	/**
	 * Translate subcomponents of a system implementation 
	 * TODO subcomponent sigs' names need to be unique
	 * */
	protected static void translateSystemImplSubcomponents(SystemImplementation systemImpl) {
		// An implementation name usually has two parts divided by "." 
		String sanitizedSystemName = sanitizeName(systemImpl.getName().split("\\.")[0]);
		String sanitizedSysImplName = sanitizeName(systemImpl.getName());
		
		Sig compTypeSig = SysArchAlloyModel.compNameToSigMap.get(sanitizedSystemName);
		Sig compImplSig = SysArchAlloyModel.compNameToSigMap.get(sanitizedSysImplName);
		
		Expr allSubcompsUnionExpr = null;
		
		// subcomponents field of a system impl = the sum of all subcomponents 
		for (Subcomponent subcomp : systemImpl.getOwnedSubcomponents()) {
			Sig subcompTypeSig = null;
			
			if (subcomp.getComponentType() != null) {
				subcompTypeSig = SysArchAlloyModel.compNameToSigMap.get(sanitizeName(subcomp.getComponentType().getName()));
			} else if (subcomp.getSubcomponentType() != null) {
				subcompTypeSig = SysArchAlloyModel.compNameToSigMap.get(sanitizeName(subcomp.getSubcomponentType().getName()));
			}
			if (subcompTypeSig == null) {
				System.err.println("Could not find system type for subcomponent: " + subcomp.getName());
				continue;
			}
			// Make an instance (one sig subcomp) for the subcomponent
			String subcompName = sanitizeName(subcomp.getName());
			Sig subcompSig = new Sig.SubsetSig(subcompName, Arrays.asList(subcompTypeSig), Attr.ONE);
			
			// Make the union of all subcomponents sigs
			if (allSubcompsUnionExpr == null) {
				allSubcompsUnionExpr = subcompSig;
			} else {
				allSubcompsUnionExpr = ExprBinary.Op.PLUS.make(Pos.UNKNOWN, Pos.UNKNOWN, allSubcompsUnionExpr, subcompSig);
			}
			
			System.out.println("translating subcomponent: " + subcomp.getName());
			

			for (PropertyAssociation prop : subcomp.getOwnedPropertyAssociations()) {
				System.out.println("propery name: " + prop.getProperty().getName());
				for (ModalPropertyValue val : prop.getOwnedValues()) {
					System.out.println("value name: " + val.getName());
					System.out.println("value: " + val.getOwnedValue());
				}
			}
			
			SysArchAlloyModel.compNameToSigMap.put(subcompName, subcompSig);
			SysArchAlloyModel.system++;
		}
		
		// The union of all subcomponent expression is equal to the system 
		// join with its "subcomponents" field 
		if (allSubcompsUnionExpr != null) {
			compImplSig.addFact(equal(join(compTypeSig, SysArchAlloyModel.COMPIMPLSUBCOMPS),
					allSubcompsUnionExpr));
		}		
	}
	
	/**
	 * 	Translate connections of a system implementation
 	 *  TODO: Need to make sure the connection name is unique
	 * */
	protected static void translateSystemImplConnections(SystemImplementation systemImpl) {
		String sysImplName = sanitizeName(systemImpl.getFullName());
		Sig sysImplSig = SysArchAlloyModel.compNameToSigMap.get(sysImplName);
		
		for(Connection conn : systemImpl.getOwnedConnections()) {			
			Sig srcSubcompSig = null;
			Sig srcSubcompTypeSig = null;
			Sig destSubcompSig = null;
			Sig destSubcompTypeSig = null;
			
			String connName = sanitizeName(conn.getFullName());
			Sig connSig = new PrimSig(connName, SysArchAlloyModel.CONNECTION, Attr.ONE);			
			
			ConnectionEnd srcConnectionEnd = conn.getAllSource();
			ConnectionEnd destConnectionEnd = conn.getAllDestination();
			
			// Obtain the src and dest subcomponent sig and their type sigs 
			if(conn.getAllDestinationContext() != null) {
				String srcConnectionSubcompName = sanitizeName(conn.getAllSourceContext().getFullName());
				String srcSubcompTypeName = sanitizeName(srcConnectionEnd.getContainingClassifier().getFullName());
				
				srcSubcompSig = SysArchAlloyModel.compNameToSigMap.get(srcConnectionSubcompName);
				srcSubcompTypeSig = SysArchAlloyModel.compNameToSigMap.get(srcSubcompTypeName);
			} else {
				srcSubcompTypeSig = SysArchAlloyModel.compNameToSigMap.get(sanitizeName(srcConnectionEnd.getContainingClassifier().getFullName()));
			}
			if(conn.getAllDestinationContext() != null) {
				String destConnectionSubcompName = conn.getAllDestinationContext().getFullName();
				String destSubcompTypeName = sanitizeName(destConnectionEnd.getContainingClassifier().getFullName());
				
				destSubcompSig = SysArchAlloyModel.compNameToSigMap.get(destConnectionSubcompName);
				destSubcompTypeSig = SysArchAlloyModel.compNameToSigMap.get(destSubcompTypeName);
			} else {
				destSubcompTypeSig = SysArchAlloyModel.compNameToSigMap.get(sanitizeName(destConnectionEnd.getContainingClassifier().getFullName()));;
			}

			
			String srcPortName = sanitizeName(srcConnectionEnd.getFullName());			
			String destPortName = sanitizeName(destConnectionEnd.getFullName());
			Field srcPortField = SysArchAlloyModel.compSigFdNameToFdMap.get(new Pair<>(srcSubcompTypeSig, srcPortName));
			Field destPortField = SysArchAlloyModel.compSigFdNameToFdMap.get(new Pair<>(destSubcompTypeSig, destPortName));
			
			// Alloy Encoding:
			// connectionSig.srcPort = subcomponentSig/systemImplSig.srcPort
			Expr connSrcPortExpr = join(connSig, SysArchAlloyModel.CONNECTIONSRCPORT);
			Expr actualConnSrcPortExpr = join(srcSubcompSig != null ? srcSubcompSig : sysImplSig, srcPortField);			
		    // connectionSig.destPort = subcomponentSig/systemImplSig.destPort
			Expr connDestPortExpr = join(connSig, SysArchAlloyModel.CONNECTIONDESTPORT);
			Expr actualConnDestPortExpr = join(destSubcompSig != null ? destSubcompSig : sysImplSig, destPortField);
			
			SysArchAlloyModel.facts.add(equal(connSrcPortExpr, actualConnSrcPortExpr));
			SysArchAlloyModel.facts.add(equal(connDestPortExpr, actualConnDestPortExpr));
			SysArchAlloyModel.connection++;
		}		
	}
	
	
	protected static String sanitizeName(String name) {
		return name != null ? name.replace("\\.", "_") : "";
	}
	
 	/**
	 * Aux functions to create expressions
	 * */
	public static final Expr join(Expr expr1, Expr expr2) {
		return ExprBinary.Op.JOIN.make(Pos.UNKNOWN, Pos.UNKNOWN, expr1, expr2);
	}
	
	public static final Expr union(Expr expr1, Expr expr2) {
		return ExprBinary.Op.PLUS.make(Pos.UNKNOWN, Pos.UNKNOWN, expr1, expr2);
	}	
	
	public static final Expr equal(Expr expr1, Expr expr2) {
		return ExprBinary.Op.EQUALS.make(Pos.UNKNOWN, Pos.UNKNOWN, expr1, expr2);
	}	
	
}
