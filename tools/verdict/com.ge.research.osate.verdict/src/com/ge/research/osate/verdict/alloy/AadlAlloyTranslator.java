package com.ge.research.osate.verdict.alloy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.osate.aadl2.ComponentImplementation;
import org.osate.aadl2.ComponentType;
import org.osate.aadl2.Connection;
import org.osate.aadl2.ConnectionEnd;
import org.osate.aadl2.DataPort;
import org.osate.aadl2.Element;
import org.osate.aadl2.Feature;
import org.osate.aadl2.ModalPropertyValue;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.NumericRange;
import org.osate.aadl2.Port;
import org.osate.aadl2.Property;
import org.osate.aadl2.PropertyAssociation;
import org.osate.aadl2.PropertyExpression;
import org.osate.aadl2.PropertyOwner;
import org.osate.aadl2.PropertyType;
import org.osate.aadl2.PublicPackageSection;
import org.osate.aadl2.Subcomponent;
import org.osate.aadl2.SubcomponentType;
import org.osate.aadl2.SystemImplementation;
import org.osate.aadl2.SystemType;
import org.osate.aadl2.TypedElement;
import org.osate.aadl2.impl.AadlBooleanImpl;
import org.osate.aadl2.impl.AadlIntegerImpl;
import org.osate.aadl2.impl.AadlStringImpl;
import org.osate.aadl2.impl.BooleanLiteralImpl;
import org.osate.aadl2.impl.EnumerationLiteralImpl;
import org.osate.aadl2.impl.EnumerationTypeImpl;
import org.osate.aadl2.impl.IntegerLiteralImpl;
import org.osate.aadl2.impl.MetaclassReferenceImpl;
import org.osate.aadl2.impl.NamedValueImpl;
import org.osate.aadl2.impl.PropertyAssociationImpl;
import org.osate.aadl2.impl.PropertyImpl;
import org.osate.aadl2.impl.PropertySetImpl;
import org.osate.aadl2.impl.RealLiteralImpl;
import org.osate.aadl2.impl.StringLiteralImpl;

import com.ge.research.osate.verdict.handlers.AADL2AlloyHandler;

import edu.mit.csail.sdg.alloy4.Pair;
import edu.mit.csail.sdg.alloy4.Pos;
import edu.mit.csail.sdg.ast.Attr;
import edu.mit.csail.sdg.ast.Decl;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.ExprBinary;
import edu.mit.csail.sdg.ast.ExprQt;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.ast.Sig.Field;
import edu.mit.csail.sdg.ast.Sig.PrimSig;
import edu.mit.csail.sdg.ast.Sig.SubsetSig;

public class AadlAlloyTranslator {
	final String ENUM = "Enum";
	final String SYSTEM = "system";
	final String PORT = "port";
	final String CONNECTION = "connection";
	SysArchAlloyModel aadlAlloyModel;
		
	final Map<String, List<String>> allDeclProps = new HashMap<>();

	public AadlAlloyTranslator(SysArchAlloyModel alloyModel) {
		aadlAlloyModel = alloyModel;
	}
	
	/**
	 * Translate all AADL objects
	 * */
	public void translateFromAADLObjects(Collection<EObject> objects) {
		// Load pre-declared sigs
		
		List<SystemType> systems = new ArrayList<>();
		List<SystemImplementation> systemImpls = new ArrayList<>();
		List<PropertySetImpl> allDeclProperties = new ArrayList<>();
		
		// Collect component type and implementation 		
		for(EObject obj : objects) {
			if (obj instanceof SystemType) {
				systems.add((SystemType) obj);
			} else if (obj instanceof SystemImplementation) {
				systemImpls.add((SystemImplementation) obj);
			} else if(obj instanceof PropertySetImpl) {
				allDeclProperties.add((PropertySetImpl)obj);
			}
		}
		
		// Translate properties
		for(PropertySetImpl propSet : allDeclProperties) {
			printHeader("Translate a property set: " + propSet.getName());
			for(Property prop : ((PropertySetImpl)propSet).getOwnedProperties()) {
				translateProperty(prop);
				
				// Save property owner to be used later					
				for(PropertyOwner po : prop.getAppliesTos()) {
					saveProperty(((MetaclassReferenceImpl)po).getMetaclass().getName().toLowerCase(), prop.getFullName());
				}
			}
		}
		
		// Translate system type 
		for (SystemType system : systems) {
			printHeader("Translate a system type: " + system.getName());
			translateSystem(system);
		}
		// Translate system implementation, subcomponent, and connections		
		for (SystemImplementation systemImpl : systemImpls) {
			printHeader("Translate a system implementation: " + systemImpl.getName());
			translateSystemImpl(systemImpl);
			translateSystemImplSubcomponents(systemImpl);
			translateSystemImplConnections(systemImpl);
		}
		plain("Finished the Translation from AADL to Alloy");
		// TODO state disjunction of all ports
	}
	
	/**
	 * Translate a connection or system property
	 * For each enumerate property prop with values [v1, v2, ...], we translate it to:
	 * abstract sig prop_enum {}
	 * one sig v1, v2, ... extends prop_Enum {}
	 * For each prop sig, if it is applied to system or connection or port, we translated it 
	 * to a field of sig "system" or "connection".
	 * sig system/connection/port {
	 *   prop : prop_Enum (if prop is an enumerate type)
	 *   prop : Bool (if prop is an Boolean type)
	 * }
	 * */
	protected void translateProperty(Property prop) {
		Field propField = null;
		PrimSig propTypeSig = null;
		String propName = prop.getName();
		PropertyType propType = prop.getPropertyType();
		
		printHeader("Translating a property: " + propName);
		
		// translate property definition in property set
		if (propType instanceof EnumerationTypeImpl) {
			// Append "_Enum" to a enumerate property name
			String propEnumTypeName = propName + "_" + ENUM;
			propTypeSig = new PrimSig(propEnumTypeName, Attr.ABSTRACT);
			
			// For each enumerate value, we create a sig
			for(NamedElement ne : ((EnumerationTypeImpl)propType).getMembers()) {
				String enumValue = ne.getName();
				PrimSig enumValueSig = new PrimSig(enumValue, propTypeSig, Attr.ONE);
				// Save the pair of enum value and its corresponding sig 
				aadlAlloyModel.propNameToSigMap.put(enumValue, enumValueSig);
			}
			// Add an unknown sig for each prop for the case the property is unassigned
			String unknownEnumValue = aadlAlloyModel.UNKNOWN + propName; 
			PrimSig unknownEnumValueSig = new PrimSig(unknownEnumValue, propTypeSig, Attr.ONE);
			
			// Save the pair of prop enum type name and prop enum prop; and the pair of enum value and its corresponding sig
			aadlAlloyModel.propNameToSigMap.put(propEnumTypeName, propTypeSig);
			aadlAlloyModel.propNameToSigMap.put(unknownEnumValue, unknownEnumValueSig);
		} else if (propType instanceof AadlBooleanImpl) {
			// Don't need to translate it as we have declared a built-in sig Bool
			// But we need to save the unknown_propName and unknown_Bool for later use
			// This will cause that sysArchAlloyModel.propNameToSigMap.values() returns 
			// redundant unknown sigs
			String unknownPropName = aadlAlloyModel.UNKNOWN + propName;
			aadlAlloyModel.propNameToSigMap.put(unknownPropName, aadlAlloyModel.UNKNOWNBOOLSIG);			
			propTypeSig = aadlAlloyModel.BOOLSIG;
		} else if (propType instanceof AadlIntegerImpl) {
			NumericRange range = ((AadlIntegerImpl)propType).getRange();
			if(range != null) {
//				PropertyExpression lbExpr = range.getLowerBound();
//				PropertyExpression ubExpr = range.getLowerBound();
//				int lb = Integer.valueOf(lbExpr.toString());
//				int ub = Integer.valueOf(ubExpr.toString());
//				
//				for(int i = lb; i <= ub; ++i) {
//					
//				}
				// Let us assume that there are 0 - 9 DAL numbers
				// The number sigs have been preloaded
				// This property type is a DAL number
				String unknownPropName = aadlAlloyModel.UNKNOWN + propName;
				aadlAlloyModel.propNameToSigMap.put(unknownPropName, aadlAlloyModel.UNKNOWNDALSIG);
				propTypeSig = aadlAlloyModel.DALSIG;
			} else {
				throw new RuntimeException("The integer property is not a range!");
			}

		} else if (propType instanceof AadlStringImpl) {
			throw new RuntimeException("Unsupported type: String");
		} else {
			throw new RuntimeException("Unsupported type: " + propType);
		}
		
		// Get the "applies" of a property, and add the property 
		// fields to the owners (system, connection, or port)
		for(PropertyOwner po : prop.getAppliesTos()) {
			Sig propOwnerSig = null;
			String propOwner = ((MetaclassReferenceImpl)po).getMetaclass().getName();			
			
			if(propOwner.equalsIgnoreCase(CONNECTION)) {
				propField = aadlAlloyModel.CONNSIG.addField(propName, propTypeSig.oneOf());
				propOwnerSig = aadlAlloyModel.CONNSIG;
			} else if(propOwner.equalsIgnoreCase(SYSTEM)) {
				propField = aadlAlloyModel.SYSTEMSIG.addField(propName, propTypeSig.oneOf());
				propOwnerSig = aadlAlloyModel.SYSTEMSIG;
			} else if(propOwner.equalsIgnoreCase(PORT)) {
				propField = aadlAlloyModel.PORTSIG.addField(propName, propTypeSig.oneOf());
				propOwnerSig = aadlAlloyModel.PORTSIG;
			} else {
				throw new RuntimeException("Unsupported property applies to + " + propOwner);
			}
			aadlAlloyModel.compSigFdNameToFdMap.put(new Pair(propOwnerSig, propName), propField);
		}				
	}	
	
	/**
	 * 
	 * Translate a System Type 
	 * For example, we translate a system type: comp 
	 * to "abstract sig comp in system", only subcomponents (instantiations) have 
	 * elements of the component type sig
	 * */
	protected void translateSystem(SystemType system) {
		String sanitizedSysName = sanitizeName(system.getName());
		PrimSig compTypeSig = new PrimSig(sanitizedSysName, aadlAlloyModel.SYSTEMSIG, Attr.ABSTRACT);
		
		Expr allInportsUnionExpr = null, allOutportsUnionExpr = null;
		
		for (DataPort port : system.getOwnedDataPorts()) {			
			Field field;
			String sanitizedPortName = sanitizeName(port.getName());
			
			switch (port.getDirection()) {
			case IN:
				field = compTypeSig.addField(sanitizedPortName, aadlAlloyModel.INPORTSIG.oneOf());
				if (allInportsUnionExpr == null) {
					allInportsUnionExpr = field;
				} else {
					allInportsUnionExpr = union(allInportsUnionExpr, field);
				}
				break;
			case OUT:
				field = compTypeSig.addField(sanitizedPortName, aadlAlloyModel.OUTPORTSIG.oneOf());
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
			
			aadlAlloyModel.compSigFdNameToFdMap.put(new Pair<>(compTypeSig, sanitizedPortName), field);
			aadlAlloyModel.port++;
			
			// Add for printing
//			sysArchAlloyModel.subsetSigAndParent.put(new Pair<>("", compTypeSig), sysArchAlloyModel.SYSTEMSIG);
		}
		
		// The union of all inports and outports should be equal to 
		// the "system" field's inports and outports respectively 
		if (allInportsUnionExpr != null) {
	        // fact { all x:Obj-Root | one x.parent }
	        Decl x = compTypeSig.oneOf("x");
	        Expr fact = x.get().join(aadlAlloyModel.SYSINPORTSSIG).equal(x.get().join(allInportsUnionExpr)).forAll(x);			
			aadlAlloyModel.facts.add(fact);
//			compTypeSig.addFact(equal(aadlAlloyModel.SYSINPORTSSIG, allInportsUnionExpr));
		}
		if (allOutportsUnionExpr != null) {
			Decl x = compTypeSig.oneOf("x");
	        Expr fact = x.get().join(aadlAlloyModel.SYSOUTPORTSSIG).equal(x.get().join(allOutportsUnionExpr)).forAll(x);			
			aadlAlloyModel.facts.add(fact);
//			compTypeSig.addFact(equal(aadlAlloyModel.SYSOUTPORTSSIG, allOutportsUnionExpr));
		}		
		aadlAlloyModel.compNameToSigMap.put(sanitizedSysName, compTypeSig);
	}
	
	/**
	 * 
	 * Translate system implementations
	 * For each system implementation SysImpl, we translate it into the following:
	 * "one sig SysImpl extends SysImplTypeSig {
	 * 		subcomponents : set system,
	 * 		connections : set connection
	 * }"
	 * 
	 * */
	protected void translateSystemImpl(SystemImplementation systemImpl) {
		String sanitizedSystemImplName = sanitizeName(systemImpl.getName());
		String sanitizedSystemImplTypeName = sanitizeName(systemImpl.getType().getFullName());
		Sig systemImplTypeSig = aadlAlloyModel.compNameToSigMap.get(sanitizedSystemImplTypeName);
		
		System.out.println("******** Type for " + sanitizedSystemImplName + " is " + sanitizedSystemImplTypeName);
		
		PrimSig systemImplSig = new PrimSig(sanitizedSystemImplName, (PrimSig)systemImplTypeSig, Attr.ONE);
		
		// Make sure the order of adding the fields does not change, 
		// because later code depends on the order
		if(!systemImpl.getOwnedSubcomponents().isEmpty()) {
			systemImplSig.addField("subcomponents", aadlAlloyModel.SYSTEMSIG.setOf());
		}
		if(!systemImpl.getOwnedConnections().isEmpty()) {
			systemImplSig.addField("connections", aadlAlloyModel.CONNSIG.setOf());
		}
		
		aadlAlloyModel.compNameToSigMap.put(sanitizedSystemImplName, systemImplSig);
	}
	
	/**
	 * Translate subcomponents of a system implementation 
	 * TODO subcomponent sigs' names need to be unique
	 * */
	protected void translateSystemImplSubcomponents(SystemImplementation systemImpl) {
		printHeader("Translate Subcomponents of " + systemImpl.getFullName());
		Expr allSubcompsUnionExpr = null;
		String sanitizedSysImplName = sanitizeName(systemImpl.getName());		
		Sig systemImplSig = aadlAlloyModel.compNameToSigMap.get(sanitizedSysImplName);				
		
		// subcomponents field of a system impl = the sum of all subcomponents 
		for (Subcomponent subcomp : systemImpl.getOwnedSubcomponents()) {
			printHeader("Translating a subcomponent: " + subcomp.getFullName());
			
			// subcompType could be an implementation
			// However, "subcompType" will be the same as "subcompCompType", 
			// if the subcomp is an instance of some component type 
			ComponentType subcompCompType = subcomp.getComponentType();			
			SubcomponentType subcompType = subcomp.getSubcomponentType();
			
			String sanitizedSubcompCompTypeName = sanitizeName(subcompCompType.getName());
			String sanitizedSubcompTypeName = sanitizeName(subcompType.getName());
			
			Sig subcompCompTypeSig = aadlAlloyModel.compNameToSigMap.get(sanitizedSubcompCompTypeName);
			Sig subcompTypeSig = aadlAlloyModel.compNameToSigMap.get(sanitizedSubcompTypeName);
			
			// Make an instance (one sig subcomp in subcompTypeSig) for the subcomponent
			String subcompName = sanitizeName(subcomp.getName());
			// We expect all instances of an implementation to possess the same set of properties
			// That is why we use the multiplicity keyword "ONE"
			
			SubsetSig subcompSig = new SubsetSig(subcompName, Arrays.asList((PrimSig)subcompTypeSig), Attr.ONE);
			System.out.println("*** Subcomponent sig = " + subcompSig);
			System.out.println("^^^^ Subcomponent's parent sig = " + subcompTypeSig);
			
			// Make the union of all subcomponents sigs
			if (allSubcompsUnionExpr == null) {
				allSubcompsUnionExpr = subcompSig;
			} else {
				allSubcompsUnionExpr = union(allSubcompsUnionExpr, subcompSig);						
			}
			
			// Handle used subcomponent properties
			Set<String> usedPropNames = new HashSet<>();			
			handleUsedProperties(usedPropNames, subcomp, null, null, subcompSig);
			
			// Handle non-used properties
			handleNonUsedProperties(allDeclProps.get(SYSTEM), usedPropNames, subcompSig, aadlAlloyModel.SYSTEMSIG);
			
			System.out.println("******** Subcomponent:  " + subcomp.getFullName());			
			// Obtain all the inports and outports expressions
			for(Feature feature: subcompCompType.getOwnedFeatures()) {				
				if(feature instanceof DataPort) {
					DataPort dp = (DataPort) feature;
					String portName = sanitizeName(dp.getFullName());
					Field portField = aadlAlloyModel.compSigFdNameToFdMap.get(new Pair<Sig, String>(subcompCompTypeSig, portName));
					
					switch (dp.getDirection()) {
					case IN:
						aadlAlloyModel.allInstInports.add(join(subcompCompTypeSig, portField));
						break;
					case OUT:
						aadlAlloyModel.allInstOutports.add(join(subcompCompTypeSig, portField));
						break;
					case IN_OUT:
					default:
						throw new RuntimeException("In/out port not supported");
					}
				}
			}
			
			// Save subcomSig and increase system number
			aadlAlloyModel.compNameToSigMap.put(subcompName, subcompSig);
			aadlAlloyModel.system++;
		}
		
		// The union of all "subcomponent" expressions is equal to the system impl sig
		// join with its "subcomponents" field 
		// systemImplSig.getFields().get(0) is the "subcomponents" field
//		if (allSubcompsUnionExpr != null) {
//			sysArchAlloyModel.facts.add(equal(join(systemImplSig, systemImplSig.getFields().get(0)),
//					allSubcompsUnionExpr));
//		}		
	}
	
	/**
	 * Handle declared properties
	 * */
	 void handleUsedProperties(Set<String> declPropNames, Subcomponent subcomp, Connection conn, Port port, Sig relevantSig) {
		List<PropertyAssociation> propAccs = null;
		
		if(subcomp != null) {
			propAccs = subcomp.getOwnedPropertyAssociations();
		} else if(conn != null) {
			propAccs = conn.getOwnedPropertyAssociations();
		} else if(port != null) {
			propAccs = port.getOwnedPropertyAssociations();
		} else {
			throw new RuntimeException("Do not know which properties association!");
		}
		if(propAccs != null && !propAccs.isEmpty()) {
			for (PropertyAssociation prop : propAccs) { 								
				String propName = prop.getProperty().getFullName();
				Field propField = null;
				printHeader("Translate a used property: " + propName);
				
				if(subcomp != null) {
					propField = aadlAlloyModel.compSigFdNameToFdMap.get(new Pair<>(aadlAlloyModel.SYSTEMSIG, propName));
				} else if(conn != null) {
					propField = aadlAlloyModel.compSigFdNameToFdMap.get(new Pair<>(aadlAlloyModel.CONNSIG, propName));
				} else {
					
				}
				
				// Add to declared props set
				declPropNames.add(propName);
				
				// We assume that each property only has only 1 value for now
				if(prop.getOwnedValues().size() == 1) {
					ModalPropertyValue val = prop.getOwnedValues().get(0);					
					Sig propValSig = getPropertyValueSig(val.getOwnedValue());
					
					if(propValSig != null) {
						// sugcompSig.propSig = propValSig 
						aadlAlloyModel.facts.add(equal(relevantSig.join(propField), propValSig));
					} else {
						throw new RuntimeException("Unexpected: property value is null!");
					}
				} else {
					throw new RuntimeException("Unexpected: property " + propName +" value has " + prop.getOwnedValues().size() + " values!");
				}
			}
		}
	}	
	
	/**
	 * Deal with non-declared properties
	 * For all non-declared system, connection, and port properties, we add the following facts:
	 * relevantSig.propSig = unkownPropSig
	 * */
	void handleNonUsedProperties(List<String> allDeclPropNames, Set<String> declPropNames, Sig relevantSig, Sig relevantArchSig) {
		
		if(allDeclPropNames != null) {
			for(String propName : allDeclPropNames) {
				if(!declPropNames.contains(propName)) {
					printHeader("Translate a not used property: " + propName);
					String unknownPropName = aadlAlloyModel.UNKNOWN + propName;
					Sig unknownPropValueSig = aadlAlloyModel.propNameToSigMap.get(unknownPropName);
					
					aadlAlloyModel.facts.add(equal(relevantSig.join(aadlAlloyModel.compSigFdNameToFdMap.get(new Pair<>(relevantArchSig, propName))), unknownPropValueSig));
				}
			}
		}
	}
	
	/**
	 * 	Translate connections of a system implementation
 	 *  TODO: Need to make sure the connection name is unique
	 * */
	protected void translateSystemImplConnections(SystemImplementation systemImpl) {
		String sysImplName = sanitizeName(systemImpl.getFullName());
		Sig sysImplSig = aadlAlloyModel.compNameToSigMap.get(sysImplName);
		
		for(Connection conn : systemImpl.getOwnedConnections()) {			
			Sig srcSubcompSig = null;
			Sig srcSubcompTypeSig = null;
			Sig destSubcompSig = null;
			Sig destSubcompTypeSig = null;
			
			String connName = sanitizeName(conn.getFullName());
			Sig connSig = new PrimSig(connName, aadlAlloyModel.CONNSIG, Attr.ONE);			
			System.out.println("*** translate connection = " + connName);
			ConnectionEnd srcConnectionEnd = conn.getAllSource();
			ConnectionEnd destConnectionEnd = conn.getAllDestination();
			
			// Obtain the src and dest subcomponent sig and their type sigs 
			if(conn.getAllSourceContext()!= null) {
				String srcConnectionSubcompName = sanitizeName(conn.getAllSourceContext().getFullName());
				String srcSubcompTypeName = sanitizeName(srcConnectionEnd.getContainingClassifier().getFullName());
				
				srcSubcompSig = aadlAlloyModel.compNameToSigMap.get(srcConnectionSubcompName);
				srcSubcompTypeSig = aadlAlloyModel.compNameToSigMap.get(srcSubcompTypeName);
			} else {
				srcSubcompTypeSig = aadlAlloyModel.compNameToSigMap.get(sanitizeName(srcConnectionEnd.getContainingClassifier().getFullName()));
			}
			if(conn.getAllDestinationContext() != null) {
				String destConnectionSubcompName = conn.getAllDestinationContext().getFullName();
				String destSubcompTypeName = sanitizeName(destConnectionEnd.getContainingClassifier().getFullName());
				
				destSubcompSig = aadlAlloyModel.compNameToSigMap.get(destConnectionSubcompName);
				destSubcompTypeSig = aadlAlloyModel.compNameToSigMap.get(destSubcompTypeName);
			} else {
				destSubcompTypeSig = aadlAlloyModel.compNameToSigMap.get(sanitizeName(destConnectionEnd.getContainingClassifier().getFullName()));;
			}

			
			String srcPortName = sanitizeName(srcConnectionEnd.getFullName());			
			String destPortName = sanitizeName(destConnectionEnd.getFullName());
			Field srcPortField = aadlAlloyModel.compSigFdNameToFdMap.get(new Pair<>(srcSubcompTypeSig, srcPortName));
			Field destPortField = aadlAlloyModel.compSigFdNameToFdMap.get(new Pair<>(destSubcompTypeSig, destPortName));
			
			// Alloy Encoding:
			// connectionSig.srcPort = subcomponentSig/systemImplSig.srcPort
			System.out.println("&&&&&&& Source &&&&&&&");
			System.out.println(" srcSubcompSig = " + srcSubcompSig);
			System.out.println(" sysImplSig = " + sysImplSig);
			System.out.println(" srcPortField = " + srcPortField);
			Expr connSrcPortExpr = join(connSig, aadlAlloyModel.CONNSRCPORTSIG);
			Expr actualConnSrcPortExpr = join(srcSubcompSig != null ? srcSubcompSig : sysImplSig, srcPortField);			
		    // connectionSig.destPort = subcomponentSig/systemImplSig.destPort
			System.out.println("&&&&&&& Destination &&&&&&&");
			System.out.println(" destSubcompSig = " + destSubcompSig);
			System.out.println(" sysImplSig = " + sysImplSig);
			System.out.println(" destPortField = " + destPortField);
			Expr connDestPortExpr = join(connSig, aadlAlloyModel.CONNDESTPORTSIG);
			Expr actualConnDestPortExpr = join(destSubcompSig != null ? destSubcompSig : sysImplSig, destPortField);
			
			aadlAlloyModel.facts.add(equal(connSrcPortExpr, actualConnSrcPortExpr));
			aadlAlloyModel.facts.add(equal(connDestPortExpr, actualConnDestPortExpr));
			
			
			// Handle declared CONNECTION properties
			Set<String> declPropNames = new HashSet<>();			
			handleUsedProperties(declPropNames, null, conn, null, connSig);			
			// Handle non-declared properties
			handleNonUsedProperties(allDeclProps.get(CONNECTION), declPropNames, connSig, aadlAlloyModel.CONNSIG);
			
			// save connection name and sig
			aadlAlloyModel.compNameToSigMap.put(connName, connSig);
			aadlAlloyModel.connection++;
		}		
	}
	
	
	protected static String sanitizeName(String name) {
		return name != null ? name.replace(".", "_") : "";
	}
	
 	/**
	 * Aux functions to create expressions
	 * */
	public final Expr join(Expr expr1, Expr expr2) {
		return ExprBinary.Op.JOIN.make(Pos.UNKNOWN, Pos.UNKNOWN, expr1, expr2);
	}
	
	public final Expr union(Expr expr1, Expr expr2) {
		return ExprBinary.Op.PLUS.make(Pos.UNKNOWN, Pos.UNKNOWN, expr1, expr2);
	}	
	
	public final Expr equal(Expr expr1, Expr expr2) {
		return ExprBinary.Op.EQUALS.make(Pos.UNKNOWN, Pos.UNKNOWN, expr1, expr2);
	}
	
	
	protected Optional<PublicPackageSection> getPublicModel(EObject obj) {
		return Util.searchEObject(obj, PublicPackageSection.class);
	}
	
	/**
	 * Get the sig for the input property value
	 * */
	public Sig getPropertyValueSig(PropertyExpression exp)
	{
		if(exp == null) {
			warning("The input to getPropertyValueSig is null!");
			return null;
		}
		
		String value = null;
		
		if (exp instanceof BooleanLiteralImpl) {
			BooleanLiteralImpl bool = ((BooleanLiteralImpl) exp);
			value = Boolean.toString(bool.getValue());
		} else if (exp instanceof IntegerLiteralImpl) {
			IntegerLiteralImpl inte = ((IntegerLiteralImpl) exp);
			value = aadlAlloyModel.DALNames[(int)inte.getValue()];
		} else if (exp instanceof NamedValueImpl) {
			NamedValueImpl namedValue =((NamedValueImpl) exp);
			
			if (namedValue.getNamedValue() instanceof EnumerationLiteralImpl)
			{
				EnumerationLiteralImpl enu = ((EnumerationLiteralImpl) namedValue.getNamedValue());
				value = enu.getName();
			} else {
				throw new RuntimeException("Unsupported property value: " + exp);
			} 
		} else {
			throw new RuntimeException("Unsupported property value: " + exp);
		}
		return aadlAlloyModel.propNameToSigMap.get(value);
	}	
	
	
	/**
	 * Add a system, connection or port property 
	 * */
	void saveProperty(String propCat, String propName) {
		if(allDeclProps.containsKey(propCat)) {
			allDeclProps.get(propCat).add(propName);
		} else {			
			if(propCat.equalsIgnoreCase(PORT) || propCat.equalsIgnoreCase(SYSTEM) || propCat.equalsIgnoreCase(CONNECTION)) {
				List<String> props = new ArrayList<>();
				props.add(propName);
				allDeclProps.put(propCat.toLowerCase(), props);	
			} else {
				throw new RuntimeException("New type of property: " + propCat);
			}												
		}
	}

	static void plain(String msg) {
		System.out.println("*********** " + msg + " ***********");
	}
	static void warning(String msg) {
		System.out.println("Warning: " + msg);
	}
	
	static void error(String msg) {
		System.out.println("Error: " + msg);
	}	
	
	static void info(String msg) {
		System.out.println("Info: " + msg);
	}		
	
	static void printHeader(String msg) {
		System.out.println("-- " + msg);
	}
}
