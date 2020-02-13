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

import org.eclipse.emf.ecore.EObject;
import org.osate.aadl2.ComponentType;
import org.osate.aadl2.Connection;
import org.osate.aadl2.ConnectionEnd;
import org.osate.aadl2.DataPort;
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
import org.osate.aadl2.impl.AadlBooleanImpl;
import org.osate.aadl2.impl.AadlIntegerImpl;
import org.osate.aadl2.impl.AadlStringImpl;
import org.osate.aadl2.impl.BooleanLiteralImpl;
import org.osate.aadl2.impl.EnumerationLiteralImpl;
import org.osate.aadl2.impl.EnumerationTypeImpl;
import org.osate.aadl2.impl.IntegerLiteralImpl;
import org.osate.aadl2.impl.MetaclassReferenceImpl;
import org.osate.aadl2.impl.NamedValueImpl;
import org.osate.aadl2.impl.PropertySetImpl;

import edu.mit.csail.sdg.alloy4.Pair;
import edu.mit.csail.sdg.alloy4.Pos;
import edu.mit.csail.sdg.ast.Attr;
import edu.mit.csail.sdg.ast.Decl;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.ExprBinary;
import edu.mit.csail.sdg.ast.ExprList;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.ast.Sig.Field;
import edu.mit.csail.sdg.ast.Sig.PrimSig;
import edu.mit.csail.sdg.ast.Sig.SubsetSig;
import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.ast.Variable;

public class Aadl2KodkodTranslator {
	final String ENUM = "Enum";
	final String BOOL = "Bool";
	final String SYSTEM = "system";
	final String PORT = "port";
	final String CONNECTION = "connection";
	final String SUBCOMPONENTS = "subcomponents";
	final String CONNECTIONS = "connections";
	SysArchKodkodModel aadlKodkodModel;
		
	final Map<String, List<String>> allDeclProps = new HashMap<>();

	public Aadl2KodkodTranslator(SysArchKodkodModel kodkodModel) {
		aadlKodkodModel = kodkodModel;
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
		List<Relation> rels = new ArrayList<Relation>();
		for (SystemType system : systems) {
			printHeader("Translate a system type: " + system.getName());			
			translateSystem(system, rels);			
		}
		// All component type relations partition the system relation
		aadlKodkodModel.mkSubRelationship(aadlKodkodModel.systemUnaryRel, true, rels, false);
		
		// Translate system implementation, subcomponent, and connections		
		for (SystemImplementation systemImpl : systemImpls) {
			printHeader("Translate a system implementation: " + systemImpl.getName());
			translateSystemImpl(systemImpl);
			translateSystemImplSubcomponents(systemImpl);
			translateSystemImplConnections(systemImpl);
		}
		
		// Post-process additional facts
//		postProcessFacts();
		
		plain("Finished the Translation from AADL to Alloy");
	}
	
	/**
	 * Process all remaining facts
	 * 
	 * */
//	void postProcessFacts() {
//		if(!aadlKodkodModel.allInstInports.isEmpty()) {
//			aadlKodkodModel.facts.add(disj(aadlKodkodModel.allInstInports));
//		}
//		if(!aadlKodkodModel.allInstOutports.isEmpty()) {
//			aadlKodkodModel.facts.add(disj(aadlKodkodModel.allInstOutports));
//		}		
//	}

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
		Relation propBinaryRel = null;
		Relation propTypeUnaryRel = null;
		String propName = prop.getName();
		PropertyType propType = prop.getPropertyType();
		
		printHeader("Translating a property: " + propName);
		
		// translate property definition in property set
		if (propType instanceof EnumerationTypeImpl) {
			// Append "_Enum" to a enumerate property name
			String propEnumTypeName = propName + "_" + ENUM;
			propTypeUnaryRel = aadlKodkodModel.mkUnaryRel(propEnumTypeName);
			List<String> propValueNames = new ArrayList<String>();
			
			// For each enumerate value, we create a sig
			for(NamedElement ne : ((EnumerationTypeImpl)propType).getMembers()) {
				String enumValue = ne.getName();
				propValueNames.add(enumValue);
			}
			// Add an unknown sig for each prop for the case the property is unassigned
			propValueNames.add(aadlKodkodModel.UNKNOWN + propName);
			aadlKodkodModel.mkSubRelationships(propTypeUnaryRel, true, true, propValueNames);
		} else if (propType instanceof AadlBooleanImpl) {
			// Don't need to translate it as we have declared a built-in unary relation Bool
			// But we need to save the unknown_propName and unknown_Bool for later use in 
			// handling non-used properties
			String unknownPropValName = aadlKodkodModel.UNKNOWN + propName;
			aadlKodkodModel.nameToUnaryRelMap.put(unknownPropValName, aadlKodkodModel.nameToUnaryRelMap.get(aadlKodkodModel.UNKNOWNBOOL));			
			propTypeUnaryRel = aadlKodkodModel.boolUnaryRel;
		} else if (propType instanceof AadlIntegerImpl) {
			NumericRange range = ((AadlIntegerImpl)propType).getRange();
			if(range != null) {
				// Let us assume that there are 0 - 9 DAL numbers
				// The number sigs have been preloaded
				// This property type is a DAL number
				String unknownPropValName = aadlKodkodModel.UNKNOWN + propName;
				aadlKodkodModel.nameToUnaryRelMap.put(unknownPropValName, aadlKodkodModel.nameToUnaryRelMap.get(aadlKodkodModel.UNKNOWNDAL));
				propTypeUnaryRel = aadlKodkodModel.dalUnaryRel;
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
			String propOwner = ((MetaclassReferenceImpl)po).getMetaclass().getName();			
			if(propOwner.equalsIgnoreCase(CONNECTION)) {
				propBinaryRel = aadlKodkodModel.mkBinaryRel(aadlKodkodModel.connectionUnaryRel, propTypeUnaryRel, true, propName);;
			} else if(propOwner.equalsIgnoreCase(SYSTEM)) {
				propBinaryRel = aadlKodkodModel.mkBinaryRel(aadlKodkodModel.systemUnaryRel, propTypeUnaryRel, true, propName);;
			} else if(propOwner.equalsIgnoreCase(PORT)) {
				propBinaryRel = aadlKodkodModel.mkBinaryRel(aadlKodkodModel.portUnaryRel, propTypeUnaryRel, true, propName);;
			} else {
				throw new RuntimeException("Unsupported property applies to + " + propOwner);
			}
		}				
	}	
	
	/**
	 * 
	 * Translate a System Type 
	 * For example, we translate a system type: comp 
	 * to "abstract sig comp in system", only subcomponents (instantiations) have 
	 * elements of the component type sig
	 * */
	protected void translateSystem(SystemType system, List<Relation> compRels) {
		String sanitizedSysName = sanitizeName(system.getName());
		Relation compTypeRel = aadlKodkodModel.mkUnaryRel(sanitizedSysName);
		Expression allInportsUnionExpr = null, allOutportsUnionExpr = null;
		
		compRels.add(compTypeRel);
		
		// Create a binary relation for each data port
		for (DataPort port : system.getOwnedDataPorts()) {			
			Relation portBinaryRel;
			String sanitizedPortName = sanitizeName(port.getName());
			
			switch (port.getDirection()) {
			case IN:
				portBinaryRel = aadlKodkodModel.mkBinaryRel(compTypeRel, aadlKodkodModel.inPortUnaryRel, true, sanitizedPortName);
				if (allInportsUnionExpr == null) {
					allInportsUnionExpr = portBinaryRel;
				} else {
					allInportsUnionExpr = aadlKodkodModel.mkUnionExprs(allInportsUnionExpr, portBinaryRel);
				}
				break;
			case OUT:
				portBinaryRel = aadlKodkodModel.mkBinaryRel(compTypeRel, aadlKodkodModel.outPortUnaryRel, true, sanitizedPortName);
				if (allOutportsUnionExpr == null) {
					allOutportsUnionExpr = portBinaryRel;
				} else {
					allOutportsUnionExpr = aadlKodkodModel.mkUnionExprs(allOutportsUnionExpr, portBinaryRel);
				}
				break;
			case IN_OUT:
			default:
				throw new RuntimeException("In/out port not supported");
			}
			
			// Handle used port properties
			Set<String> usedPropNames = new HashSet<>();			
//			handleUsedProperties(usedPropNames, null, null, port, subcompSig);
			
			// Handle non-used port properties
//			handleNonUsedProperties(allDeclProps.get(PORT), usedPropNames, subcompSig, aadlAlloyModel.PORTSIG);			

		}
		
		// The union of all inports and outports should be equal to 
		// the "system" field's inports and outports respectively 
		// all x : compTypeRel | x.inPorts = allInportsUnionExpr
		// all x : compTypeRel | x.outPorts = allOutportsUnionExpr		
		if (allInportsUnionExpr != null) {
	        Variable var = Variable.unary("x");
	        Formula fact = var.join(aadlKodkodModel.inPortsBinaryRel).eq(var.join(allInportsUnionExpr)).forAll(var.oneOf(compTypeRel));			
			aadlKodkodModel.facts.add(fact);
		}
		if (allOutportsUnionExpr != null) {
	        Variable var = Variable.unary("x");
	        Formula fact = var.join(aadlKodkodModel.inPortsBinaryRel).eq(var.join(allOutportsUnionExpr)).forAll(var.oneOf(compTypeRel));			
			aadlKodkodModel.facts.add(fact);
		}		
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
		SystemType systemImplType = systemImpl.getType();
		String sanitizedSystemImplName = sanitizeName(systemImpl.getName());
		String sanitizedSystemImplTypeName = sanitizeName(systemImplType.getFullName());
		Relation systemImplTypeRel = aadlKodkodModel.nameToUnaryRelMap.get(sanitizedSystemImplTypeName);
		Relation systemImplRel = aadlKodkodModel.mkUnaryRel(sanitizedSystemImplName);
		
		aadlKodkodModel.mkSubRelationship(systemImplTypeRel, false, Arrays.asList(systemImplRel), true);
		
		// Obtain all the systemImpl join inports and outports expressions
		for(Feature feature: systemImplType.getOwnedFeatures()) {				
			if(feature instanceof DataPort) {
				DataPort dp = (DataPort) feature;
				String portName = sanitizeName(dp.getFullName());
				Relation portBinaryRel = aadlKodkodModel.compSigFdNameToFdMap.get(new Pair<Sig, String>(systemImplTypeSig, portName));
				
				switch (dp.getDirection()) {
				case IN:
					aadlKodkodModel.allInstInports.add(join(systemImplSig, portField));
					break;
				case OUT:
					aadlKodkodModel.allInstOutports.add(join(systemImplSig, portField));
					break;
				case IN_OUT:
				default:
					throw new RuntimeException("In/out port not supported");
				}
				
				// TODO: Need to handle the case where there are multiple instances of system implementation
				aadlKodkodModel.portNum++;
			}
		}		
		// Save the systemImpl sig
		aadlKodkodModel.systemNum++;
	}
	
	/**
	 * Translate subcomponents of a system implementation 
	 * TODO subcomponent sigs' names need to be unique
	 * 1. Make a sig for each subcomponent
	 * 2. Make a fact "systemImpl.subcomponents = sub1 + sub2 + sub3..."
	 * */
	protected void translateSystemImplSubcomponents(SystemImplementation systemImpl) {
		printHeader("Translate Subcomponents of " + systemImpl.getFullName());
		Expr allSubcompsUnionExpr = null;
		String sanitizedSysImplName = sanitizeName(systemImpl.getName());		
		Sig systemImplSig = aadlKodkodModel.compNameToSigMap.get(sanitizedSysImplName);				
		
		// subcomponents field of a system impl = the sum of all subcomponents 
		for (Subcomponent subcomp : systemImpl.getOwnedSubcomponents()) {
			printHeader("Translating a subcomponent: " + subcomp.getFullName());
			
			// subcompType could be a system implementation
			// However, if the subcomp is an instance of some component type
			// "subcompType" will be the same as "subcompCompType",
			ComponentType subcompCompType = subcomp.getComponentType();			
			SubcomponentType subcompType = subcomp.getSubcomponentType();
			
			String sanitizedSubcompCompTypeName = sanitizeName(subcompCompType.getName());
			String sanitizedSubcompTypeName = sanitizeName(subcompType.getName());
			
			Sig subcompCompTypeSig = aadlKodkodModel.compNameToSigMap.get(sanitizedSubcompCompTypeName);
			Sig subcompTypeSig = aadlKodkodModel.compNameToSigMap.get(sanitizedSubcompTypeName);
			
			// Make an instance (one sig subcomp in subcompTypeSig) for the subcomponent
			String subcompName = sanitizeName(subcomp.getName());
			// We expect all instances of an implementation to possess the same set of properties
			// That is why we use the multiplicity keyword "ONE"
			
			SubsetSig subcompSig = new SubsetSig(subcompName, Arrays.asList((PrimSig)subcompTypeSig), Attr.ONE);
			
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
			handleNonUsedProperties(allDeclProps.get(SYSTEM), usedPropNames, subcompSig, aadlKodkodModel.SYSTEMSIG);
				
			// Obtain all the subcomp sig joins its fields: inports and outports expressions
			for(Feature feature: subcompCompType.getOwnedFeatures()) {				
				if(feature instanceof DataPort) {
					DataPort dp = (DataPort) feature;
					String portName = sanitizeName(dp.getFullName());
					Field portField = aadlKodkodModel.compSigFdNameToFdMap.get(new Pair<Sig, String>(subcompCompTypeSig, portName));
					
					switch (dp.getDirection()) {
					case IN:
						aadlKodkodModel.allInstInports.add(join(subcompSig, portField));
						break;
					case OUT:
						aadlKodkodModel.allInstOutports.add(join(subcompSig, portField));
						break;
					case IN_OUT:
					default:
						throw new RuntimeException("In/out port not supported");
					}
					aadlKodkodModel.portNum++;
				}
			}
			
			// Save subcomSig and increase system number
			aadlKodkodModel.subcompSigs.add(subcompSig);
			aadlKodkodModel.compNameToSigMap.put(subcompName, subcompSig);
			aadlKodkodModel.systemNum++;
		}
		
		// The union of all "subcomponent" expressions is equal to the system impl sig
		// join with its "subcomponents" field 
		if (allSubcompsUnionExpr != null) {
			for(Field field : systemImplSig.getFields()) {
				if(field.label.equalsIgnoreCase(SUBCOMPONENTS)) {
					aadlKodkodModel.facts.add(equal(join(systemImplSig, field),
							allSubcompsUnionExpr));
					break;
				}
			}
		}		
	}
	
	/**
	 * 	Translate connections of a system implementation
 	 *  TODO: Need to make sure the connection name is unique
 	 *  1. Make a sig for each connection
 	 *  2. The union of all connections = system impl sig join with its "connections" field
	 * */
	protected void translateSystemImplConnections(SystemImplementation systemImpl) {
		Expr allConnsUnionExpr = null;
		String sysImplName = sanitizeName(systemImpl.getFullName());
		Sig systemImplSig = aadlKodkodModel.compNameToSigMap.get(sysImplName);
		
		for(Connection conn : systemImpl.getOwnedConnections()) {			
			Sig srcSubcompSig = null;
			Sig srcSubcompTypeSig = null;
			Sig destSubcompSig = null;
			Sig destSubcompTypeSig = null;
			
			String connName = sanitizeName(conn.getFullName());
			Sig connSig = new PrimSig(connName, aadlKodkodModel.CONNSIG, Attr.ONE);			
			System.out.println("*** translate connection = " + connName);
			
			ConnectionEnd srcConnectionEnd = conn.getAllSource();
			ConnectionEnd destConnectionEnd = conn.getAllDestination();
			
			// Obtain the src and dest subcomponent sig and their type sigs 
			if(conn.getAllSourceContext()!= null) {
				String srcConnectionSubcompName = sanitizeName(conn.getAllSourceContext().getFullName());
				String srcSubcompTypeName = sanitizeName(srcConnectionEnd.getContainingClassifier().getFullName());
				
				srcSubcompSig = aadlKodkodModel.compNameToSigMap.get(srcConnectionSubcompName);
				srcSubcompTypeSig = aadlKodkodModel.compNameToSigMap.get(srcSubcompTypeName);
			} else {
				srcSubcompTypeSig = aadlKodkodModel.compNameToSigMap.get(sanitizeName(srcConnectionEnd.getContainingClassifier().getFullName()));
			}
			if(conn.getAllDestinationContext() != null) {
				String destConnectionSubcompName = conn.getAllDestinationContext().getFullName();
				String destSubcompTypeName = sanitizeName(destConnectionEnd.getContainingClassifier().getFullName());
				
				destSubcompSig = aadlKodkodModel.compNameToSigMap.get(destConnectionSubcompName);
				destSubcompTypeSig = aadlKodkodModel.compNameToSigMap.get(destSubcompTypeName);
			} else {
				destSubcompTypeSig = aadlKodkodModel.compNameToSigMap.get(sanitizeName(destConnectionEnd.getContainingClassifier().getFullName()));;
			}

			
			String srcPortName = sanitizeName(srcConnectionEnd.getFullName());			
			String destPortName = sanitizeName(destConnectionEnd.getFullName());
			Field srcPortField = aadlKodkodModel.compSigFdNameToFdMap.get(new Pair<>(srcSubcompTypeSig, srcPortName));
			Field destPortField = aadlKodkodModel.compSigFdNameToFdMap.get(new Pair<>(destSubcompTypeSig, destPortName));
			
			// Alloy Encoding:
			// connectionSig.srcPort = subcomponentSig/systemImplSig.srcPort
			Expr connSrcPortExpr = join(connSig, aadlKodkodModel.CONNSRCPORTSIG);
			Expr actualConnSrcPortExpr = join(srcSubcompSig != null ? srcSubcompSig : systemImplSig, srcPortField);			
		    // connectionSig.destPort = subcomponentSig/systemImplSig.destPort
			Expr connDestPortExpr = join(connSig, aadlKodkodModel.CONNDESTPORTSIG);
			Expr actualConnDestPortExpr = join(destSubcompSig != null ? destSubcompSig : systemImplSig, destPortField);
			
			aadlKodkodModel.facts.add(equal(connSrcPortExpr, actualConnSrcPortExpr));
			aadlKodkodModel.facts.add(equal(connDestPortExpr, actualConnDestPortExpr));
			
			
			// Handle declared CONNECTION properties
			Set<String> declPropNames = new HashSet<>();			
			handleUsedProperties(declPropNames, null, conn, null, connSig);			
			// Handle non-declared properties
			handleNonUsedProperties(allDeclProps.get(CONNECTION), declPropNames, connSig, aadlKodkodModel.CONNSIG);
			
			// Union of all connections of the system implementation 
			if(allConnsUnionExpr == null) {
				allConnsUnionExpr = connSig;
			} else {
				allConnsUnionExpr = union(allConnsUnionExpr, connSig);
			}
			
			// save connection name and sig
			aadlKodkodModel.compNameToSigMap.put(connName, connSig);
			aadlKodkodModel.connectionNum++;
		}
		
		// The union of all "subcomponent" expressions is equal to the system impl sig
		// join with its "subcomponents" field 
		if (allConnsUnionExpr != null) {
			for(Field field : systemImplSig.getFields()) {
				if(field.label.equalsIgnoreCase(CONNECTIONS)) {
					aadlKodkodModel.facts.add(equal(join(systemImplSig, field),
							allConnsUnionExpr));
					break;
				}
			}
		}			
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
					propField = aadlKodkodModel.compSigFdNameToFdMap.get(new Pair<>(aadlKodkodModel.SYSTEMSIG, propName));
				} else if(conn != null) {
					propField = aadlKodkodModel.compSigFdNameToFdMap.get(new Pair<>(aadlKodkodModel.CONNSIG, propName));
				} else {
					
				}
				
				// Add to declared props set
				declPropNames.add(propName);
				
				// We assume that each property only has only 1 value for now
				if(prop.getOwnedValues().size() == 1) {
					ModalPropertyValue val = prop.getOwnedValues().get(0);					
					Sig propValSig = getPropertyValueSig(val.getOwnedValue());
					
					if(propValSig != null) {
						// subcompSig.propSig = propValSig 
						aadlKodkodModel.facts.add(equal(relevantSig.join(propField), propValSig));
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
					String unknownPropName = aadlKodkodModel.UNKNOWN + propName;
					Sig unknownPropValueSig = aadlKodkodModel.propNameToSigMap.get(unknownPropName);
					
					aadlKodkodModel.facts.add(equal(relevantSig.join(aadlKodkodModel.compSigFdNameToFdMap.get(new Pair<>(relevantArchSig, propName))), unknownPropValueSig));
				}
			}
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
	
	public final Expr disj(List<Expr> exprs) {
		return ExprList.makeDISJOINT(Pos.UNKNOWN, Pos.UNKNOWN, exprs);
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
			value = aadlKodkodModel.DALNames[(int)inte.getValue()];
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
		return aadlKodkodModel.propNameToSigMap.get(value);
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
				throw new RuntimeException("Unsupported new type of property: " + propCat);
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
