package com.ge.research.osate.verdict.alloy;

import java.util.ArrayList;
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

import kodkod.util.collections.Pair;
import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.ast.Variable;

public class Aadl2KodkodTranslator {
	final SysArchKodkodModel aadlKodkodModel;
	final Map<String, List<String>> allDeclProps = new HashMap<>();
	final Map<Relation, Expression> sysImplTypeRelToSubcompsExpr = new HashMap<Relation, Expression>();
	final Map<Relation, Expression> sysImplTypeRelToConnsExpr = new HashMap<Relation, Expression>();

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
		
		// Post-process additional facts
		postProcessConstraints();
		
		plain("Finished the Translation from AADL to Alloy");
	}
	
	/**
	 * Process all remaining facts
	 * 
	 * */
	void postProcessConstraints() {
		if(!aadlKodkodModel.allInstInports.isEmpty()) {
			aadlKodkodModel.mkMutualDisjoint(aadlKodkodModel.allInstInports);
		}
		if(!aadlKodkodModel.allInstOutports.isEmpty()) {
			aadlKodkodModel.mkMutualDisjoint(aadlKodkodModel.allInstOutports);
		}	
		if(!aadlKodkodModel.compTypeRelToInstRelMap.isEmpty()) {
			// All component type partitions "system"
			aadlKodkodModel.mkSubRelationship(aadlKodkodModel.systemUnaryRel, true, new ArrayList<Relation>(aadlKodkodModel.compTypeRelToInstRelMap.keySet()), false);
			
			// All sub-types partition the component type
			for(Map.Entry<Relation, Set<Relation>> entry : aadlKodkodModel.compTypeRelToInstRelMap.entrySet()) {
				Relation parentRel = entry.getKey();
				Set<Relation> subRels = entry.getValue();
				
				if(!hasImplRel(subRels) && !subRels.isEmpty()) {
					// All subcomponent instances partition the parent component type relation
					aadlKodkodModel.mkSubRelationship(parentRel, true, new ArrayList<Relation>(subRels), true);
				} else {
					//TODO: need to change here!
//					aadlKodkodModel.mkSubRelationship(parentRel, true, new ArrayList<Relation>(subRels), true);					
				}			
			}
		}
		
		Expression unionOfRels = Relation.NONE;
		if(!sysImplTypeRelToSubcompsExpr.isEmpty()) {
			// sys:entry.getKey() | sys.subcomponents = entry.value();
			
			for(Map.Entry<Relation, Expression> entry : sysImplTypeRelToSubcompsExpr.entrySet()) {
				Relation implRel = entry.getKey();
				Variable var = Variable.unary("x");					
				aadlKodkodModel.facts.add(var.join(aadlKodkodModel.subcomponentsBinaryRel).eq(entry.getValue()).forAll(var.oneOf(implRel)));
				unionOfRels = unionOfRels.union(implRel);
			}
		}
		// sys:systems - unionOfRels | sys.subcomponents = NONE 
		Variable sys1 = Variable.unary("s");
		aadlKodkodModel.facts.add(sys1.join(aadlKodkodModel.subcomponentsBinaryRel).eq(Relation.NONE).forAll(sys1.oneOf(aadlKodkodModel.systemUnaryRel.difference(unionOfRels))));
		
		Expression unionOfRels2 = Relation.NONE;
		if(!sysImplTypeRelToConnsExpr.isEmpty()) {
			// sys: SystemImpl | sys.connections = entry.value();			
			for(Map.Entry<Relation, Expression> entry : sysImplTypeRelToConnsExpr.entrySet()) {
				Relation implRel = entry.getKey();
				Variable var = Variable.unary("x");
				aadlKodkodModel.facts.add(var.join(aadlKodkodModel.connectionsBinaryRel).eq(entry.getValue()).forAll(var.oneOf(implRel)));
				unionOfRels2 = unionOfRels2.union(implRel);
			}
		}	
		// sys:system - unionOfRels | sys.connections = NONE
		Variable sys2 = Variable.unary("s");
		aadlKodkodModel.facts.add(sys2.join(aadlKodkodModel.connectionsBinaryRel).eq(Relation.NONE).forAll(sys2.oneOf(aadlKodkodModel.systemUnaryRel.difference(unionOfRels2))));
		
		// Make all connections extends the connection relation
		if(!aadlKodkodModel.allConnectionRels.isEmpty()) {
			aadlKodkodModel.mkSubRelationship(aadlKodkodModel.connectionUnaryRel, true, new ArrayList<Relation>(aadlKodkodModel.allConnectionRels), true);
		}
		
		// system.(in1 ... ink) allInports are distinct 
	}
	
	boolean hasImplRel(Set<Relation> subRels) {
		boolean hasImpl = false;
		
		for(Relation subRel : subRels) {
			if(aadlKodkodModel.compImplRelToInstRelMap.containsKey(subRel)) {
				hasImpl = true;
				break;
			}
		}
		return hasImpl;
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
		Relation propTypeUnaryRel = null;
		String propName = prop.getName();
		PropertyType propType = prop.getPropertyType();
		
		printHeader("Translating a property: " + propName);
		
		// translate property definition in property set
		if (propType instanceof EnumerationTypeImpl) {
			// Append "_Enum" to a enumerate property name
			String propEnumTypeName = propName + aadlKodkodModel.ENUM;
			propTypeUnaryRel = aadlKodkodModel.mkUnaryRel(propEnumTypeName);
			Set<Relation> propValueRels = new HashSet<Relation>();
			
			// For each enumerate value, we create a sig
			for(NamedElement ne : ((EnumerationTypeImpl)propType).getMembers()) {
				propValueRels.add(aadlKodkodModel.mkUnaryRel(ne.getName()));
			}
			// Add an unknown_propName relation for each prop for the case the property is unassigned
			propValueRels.add(aadlKodkodModel.mkUnaryRel(aadlKodkodModel.UNKNOWN + propName));
			aadlKodkodModel.mkSubRelationship(propTypeUnaryRel, true, propValueRels, true);
			
			// Save the property relation and its value relations
			aadlKodkodModel.propTypeRelToValRelMap.put(propTypeUnaryRel, propValueRels);
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
			Relation propRel = null;
			
			if(propOwner.equalsIgnoreCase(aadlKodkodModel.CONNECTION)) {
				propRel = aadlKodkodModel.mkBinaryRel(aadlKodkodModel.connectionUnaryRel, propTypeUnaryRel, true, propName);
				aadlKodkodModel.binaryRelToDomainRangeRelMap.put(propRel, new Pair<>(aadlKodkodModel.connectionUnaryRel, propTypeUnaryRel));
			} else if(propOwner.equalsIgnoreCase(aadlKodkodModel.SYSTEM)) {
				propRel = aadlKodkodModel.mkBinaryRel(aadlKodkodModel.systemUnaryRel, propTypeUnaryRel, true, propName);
				aadlKodkodModel.binaryRelToDomainRangeRelMap.put(propRel, new Pair<>(aadlKodkodModel.systemUnaryRel, propTypeUnaryRel));
			} else if(propOwner.equalsIgnoreCase(aadlKodkodModel.PORT)) {
				propRel = aadlKodkodModel.mkBinaryRel(aadlKodkodModel.portUnaryRel, propTypeUnaryRel, true, propName);
				aadlKodkodModel.binaryRelToDomainRangeRelMap.put(propRel, new Pair<>(aadlKodkodModel.portUnaryRel, propTypeUnaryRel));
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
	protected void translateSystem(SystemType system) {
		String sanitizedSysName = sanitizeName(system.getName());
		Relation compTypeRel = aadlKodkodModel.mkUnaryRel(sanitizedSysName);
		Expression allInportsUnionExpr = null, allOutportsUnionExpr = null;

		// Save the component type relation 
		// This is necessary because some component types are not used in the model.
		saveTypeRelAndInstRel(aadlKodkodModel.compTypeRelToInstRelMap, compTypeRel, null);
		
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
					allInportsUnionExpr = allInportsUnionExpr.union(portBinaryRel);
				}
				if(!aadlKodkodModel.allBinaryInportRels.contains(portBinaryRel)) {
					aadlKodkodModel.allBinaryInportRels.add(portBinaryRel);
				} else {
					throw new RuntimeException("Mutltiple system " + sanitizedSysName + " has the same inport name: " + sanitizedPortName);
				}
				// Save port binary relation and its domain and range relation
				aadlKodkodModel.binaryRelToDomainRangeRelMap.put(portBinaryRel, new Pair<>(compTypeRel, aadlKodkodModel.inPortUnaryRel));
				break;
			case OUT:
				portBinaryRel = aadlKodkodModel.mkBinaryRel(compTypeRel, aadlKodkodModel.outPortUnaryRel, true, sanitizedPortName);
				if (allOutportsUnionExpr == null) {
				} else {
					allOutportsUnionExpr = allOutportsUnionExpr.union(portBinaryRel);
				}
				allOutportsUnionExpr = portBinaryRel;
				if(!aadlKodkodModel.allBinaryOutportRels.contains(portBinaryRel)) {
					aadlKodkodModel.allBinaryOutportRels.add(portBinaryRel);
				} else {
					throw new RuntimeException("Mutltiple system " + sanitizedSysName + " has the same outport name: " + sanitizedPortName);
				}
				// Save port binary relation and its domain and range relation
				aadlKodkodModel.binaryRelToDomainRangeRelMap.put(portBinaryRel, new Pair<>(compTypeRel, aadlKodkodModel.outPortUnaryRel));
				break;
			case IN_OUT:
			default:
				throw new RuntimeException("In/out port not supported");
			}
			
			// Handle used port properties
//			Set<String> usedPropNames = new HashSet<>();			
//			handleUsedProperties(usedPropNames, null, null, port, subcompSig);
			
			// Handle non-used port properties
//			handleNonUsedProperties(allDeclProps.get(PORT), usedPropNames, subcompSig, aadlAlloyModel.PORTSIG);			
		}
		
		// The union of all inports and outports should be equal to 
		// the "system" field's inports and outports respectively 
		// all x : compTypeRel | x.inPorts = x.allInportsUnionExpr
		// all x : compTypeRel | x.outPorts = x.allOutportsUnionExpr		
		if (allInportsUnionExpr != null) {
	        Variable var = Variable.unary("x");
	        Formula fact = var.join(aadlKodkodModel.inPortsBinaryRel).eq(var.join(allInportsUnionExpr)).forAll(var.oneOf(compTypeRel));			
			aadlKodkodModel.facts.add(fact);
		}
		if (allOutportsUnionExpr != null) {
	        Variable var = Variable.unary("x");
	        Formula fact = var.join(aadlKodkodModel.outPortsBinaryRel).eq(var.join(allOutportsUnionExpr)).forAll(var.oneOf(compTypeRel));			
			aadlKodkodModel.facts.add(fact);
		}		
	}
	
	/**
	 * 
	 * Translate s system implementation
	 * For each system implementation SysImpl, we translate it into the following:
	 * 
	 * */
	protected void translateSystemImpl(SystemImplementation systemImpl) {
		SystemType systemImplType = systemImpl.getType();
		String sanitizedSystemImplName = sanitizeName(systemImpl.getName());
		String sanitizedSystemImplTypeName = sanitizeName(systemImplType.getFullName());
		Relation systemImplTypeRel = aadlKodkodModel.nameToUnaryRelMap.get(sanitizedSystemImplTypeName);
		Relation systemImplRel = aadlKodkodModel.mkUnaryRel(sanitizedSystemImplName);
		
		// Save the implementation type and implementation relations pair
		// Consider the implementation as an instance of the implementation type
		saveTypeRelAndInstRel(aadlKodkodModel.compTypeRelToInstRelMap, systemImplTypeRel, systemImplRel);
		
		// Obtain all the systemImpl join inports and outports expressions
		for(Feature feature : systemImplType.getOwnedFeatures()) {				
			if(feature instanceof DataPort) {
				DataPort dp = (DataPort) feature;
				String portName = sanitizeName(dp.getFullName());
				Relation portBinaryRel = aadlKodkodModel.domainRelNameToRelMap.get(new Pair<Relation, String>(systemImplTypeRel, portName));
				
				switch (dp.getDirection()) {
				case IN:
					aadlKodkodModel.allInstInports.add(systemImplRel.join(portBinaryRel));
					break;
				case OUT:
					aadlKodkodModel.allInstOutports.add(systemImplRel.join(portBinaryRel));
					break;
				case IN_OUT:
				default:
					throw new RuntimeException("In/out port not supported");
				}
			}
		}		
	}
	
	/**
	 * Translate subcomponents of a system implementation 
	 * TODO subcomponent sigs' names need to be unique
	 * 1. Make a sig for each subcomponent
	 * 2. Make a fact "systemImpl.subcomponents = sub1 + sub2 + sub3..."
	 * */
	protected void translateSystemImplSubcomponents(SystemImplementation systemImpl) {
		printHeader("Translate Subcomponents of " + systemImpl.getFullName());
		Expression allSubcompsUnionExpr = null;
						
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
			
			Relation subcompCompTypeRel = aadlKodkodModel.nameToUnaryRelMap.get(sanitizedSubcompCompTypeName);
			Relation subcompTypeRel = aadlKodkodModel.nameToUnaryRelMap.get(sanitizedSubcompTypeName);
			
			// Make an instance (one sig subcomp in subcompTypeSig) for the subcomponent
			String subcompName = sanitizeName(subcomp.getName());
			Relation subcompRel = aadlKodkodModel.mkUnaryRel(subcompName);
			
			// Save the component type relation to instance relations pair
			// if the subcomponent is an instance of a component type.
			// Otherwise, save the implementation and the instance pair; 
			// and also save the pair between the type and the implementation.
			if(subcompTypeRel==subcompCompTypeRel) {
				saveTypeRelAndInstRel(aadlKodkodModel.compTypeRelToInstRelMap, subcompCompTypeRel, subcompRel);
			} else {
				saveTypeRelAndInstRel(aadlKodkodModel.compTypeRelToInstRelMap, subcompCompTypeRel, subcompTypeRel);
				saveTypeRelAndInstRel(aadlKodkodModel.compImplRelToInstRelMap, subcompTypeRel, subcompRel);
			}
			
			// Make the union of all subcomponents relations
			if (allSubcompsUnionExpr == null) {
				allSubcompsUnionExpr = subcompRel;
			} else {
				allSubcompsUnionExpr = allSubcompsUnionExpr.union(subcompRel);						
			}
			
			// Handle used subcomponent properties
			Set<String> usedPropNames = new HashSet<>();			
			handleUsedProperties(usedPropNames, subcomp, null, null, subcompRel);
			
			// Handle non-used properties
			handleNonUsedProperties(allDeclProps.get(aadlKodkodModel.SYSTEM), usedPropNames, subcompRel, aadlKodkodModel.systemUnaryRel);
				
			// Obtain all the subcomp rel joins its fields: inports and outports expressions
			for(Feature feature: subcompCompType.getOwnedFeatures()) {				
				if(feature instanceof DataPort) {
					DataPort dp = (DataPort) feature;
					String portName = sanitizeName(dp.getFullName());
					Relation portRel = aadlKodkodModel.domainRelNameToRelMap.get(new Pair<Relation, String>(subcompCompTypeRel, portName));
					
					switch (dp.getDirection()) {
					case IN:
						aadlKodkodModel.allInstInports.add(subcompRel.join(portRel));
						break;
					case OUT:
						aadlKodkodModel.allInstOutports.add(subcompRel.join(portRel));
						break;
					case IN_OUT:
					default:
						throw new RuntimeException("In/out port not supported");
					}
				}
			}
		}
		
		// Save: systemImplRel.subcomponents = allSubcompsUnionExpr
		if (allSubcompsUnionExpr != null) {			
			Relation systemImplRel = aadlKodkodModel.nameToUnaryRelMap.get(sanitizeName(systemImpl.getName()));
			sysImplTypeRelToSubcompsExpr.put(systemImplRel, allSubcompsUnionExpr);
		}		
	}
	
	/**
	 * 	Translate connections of a system implementation
 	 *  TODO: Need to make sure the connection name is unique
 	 *  1. Make a relation for each connection
 	 *  2. The union of all connections = system impl sig join with its "connections" field
	 * */
	protected void translateSystemImplConnections(SystemImplementation systemImpl) {
		Expression allConnsUnionExpr = null;
		Relation systemImplRel = aadlKodkodModel.nameToUnaryRelMap.get(sanitizeName(systemImpl.getFullName()));
		
		for(Connection conn : systemImpl.getOwnedConnections()) {			
			Relation srcSubcompRel = null;
			Relation srcSubcompTypeRel = null;
			Relation destSubcompRel = null;
			Relation destSubcompTypeRel = null;			
			String connName = sanitizeName(conn.getName());
			Relation connRel = aadlKodkodModel.mkUnaryRel(connName);
			
			if(!aadlKodkodModel.allConnectionRels.contains(connRel)) {
				aadlKodkodModel.allConnectionRels.add(connRel);
			} else {
				throw new RuntimeException("Multiple connections with the same name in the AADL model: " + connName);
			}
						
			System.out.println("*** Translate a connection = " + connName);
			// The ports in the connection
			ConnectionEnd srcConnectionEnd = conn.getAllSource();
			ConnectionEnd destConnectionEnd = conn.getAllDestination();
			
			// Obtain the src and dest subcomponent sig and their type sigs 
			// If conn.getAllSource()/conn.getAllDestinationContext() is null, it means the connection
			// conn.getAllSourceContext() is the subcomponent 
			if(conn.getAllSourceContext()!= null) {
				String srcConnectionSubcompName = sanitizeName(conn.getAllSourceContext().getFullName());
				String srcSubcompTypeName = sanitizeName(srcConnectionEnd.getContainingClassifier().getFullName());
				
				srcSubcompRel = aadlKodkodModel.nameToUnaryRelMap.get(srcConnectionSubcompName);
				srcSubcompTypeRel = aadlKodkodModel.nameToUnaryRelMap.get(srcSubcompTypeName);
			} else {
				srcSubcompTypeRel = aadlKodkodModel.nameToUnaryRelMap.get(sanitizeName(srcConnectionEnd.getContainingClassifier().getFullName()));
			}
			if(conn.getAllDestinationContext() != null) {
				String destConnectionSubcompName = conn.getAllDestinationContext().getFullName();
				String destSubcompTypeName = sanitizeName(destConnectionEnd.getContainingClassifier().getFullName());
				
				destSubcompRel = aadlKodkodModel.nameToUnaryRelMap.get(destConnectionSubcompName);
				destSubcompTypeRel = aadlKodkodModel.nameToUnaryRelMap.get(destSubcompTypeName);
			} else {
				destSubcompTypeRel = aadlKodkodModel.nameToUnaryRelMap.get(sanitizeName(destConnectionEnd.getContainingClassifier().getFullName()));;
			}

			
			String srcPortName = sanitizeName(srcConnectionEnd.getFullName());			
			String destPortName = sanitizeName(destConnectionEnd.getFullName());
			Relation srcPortRel = aadlKodkodModel.domainRelNameToRelMap.get(new Pair<>(srcSubcompTypeRel, srcPortName));
			Relation destPortRel = aadlKodkodModel.domainRelNameToRelMap.get(new Pair<>(destSubcompTypeRel, destPortName));			
			Expression connSrcPortExpr = connRel.join(aadlKodkodModel.srcPortBinaryRel);
			Expression actualConnSrcPortExpr = (srcSubcompRel != null ? srcSubcompRel : systemImplRel).join(srcPortRel);			
			Expression connDestPortExpr = connRel.join(aadlKodkodModel.destPortBinaryRel);
			Expression actualConnDestPortExpr = (destSubcompRel != null ? destSubcompRel : systemImplRel).join(destPortRel);
			
			// Kodkod Encoding:
			// connectionSig.srcPort = subcomponentSig/systemImplSig.srcPort
		    // connectionSig.destPort = subcomponentSig/systemImplSig.destPort			
			aadlKodkodModel.mkEq(connSrcPortExpr, actualConnSrcPortExpr);
			aadlKodkodModel.mkEq(connDestPortExpr, actualConnDestPortExpr);
			
			// Handle declared CONNECTION properties
			Set<String> declPropNames = new HashSet<>();			
			handleUsedProperties(declPropNames, null, conn, null, connRel);			
			// Handle non-declared properties
			handleNonUsedProperties(allDeclProps.get(aadlKodkodModel.CONNECTION), declPropNames, connRel, aadlKodkodModel.connectionUnaryRel);
			
			// Union of all connections of the system implementation 
			if(allConnsUnionExpr == null) {
				allConnsUnionExpr = connRel;
			} else {
				allConnsUnionExpr = allConnsUnionExpr.union(connRel);
			}
		}
		
		// The union of all "subcomponent" expressions is equal to the system impl sig
		// join with its "subcomponents" field 
		if (allConnsUnionExpr != null) {	
			sysImplTypeRelToConnsExpr.put(systemImplRel, allConnsUnionExpr);
		}				
	}
	
	/**
	 * Handle declared properties
	 * */
	 void handleUsedProperties(Set<String> declPropNames, Subcomponent subcomp, Connection conn, Port port, Relation relevantRel) {
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
				Relation propRel = null;
				printHeader("Translate a used property: " + propName);
				
				if(subcomp != null) {
					propRel = aadlKodkodModel.domainRelNameToRelMap.get(new Pair<>(aadlKodkodModel.systemUnaryRel, propName));
				} else if(conn != null) {
					propRel = aadlKodkodModel.domainRelNameToRelMap.get(new Pair<>(aadlKodkodModel.connectionUnaryRel, propName));
				} else if(port != null){
					propRel = aadlKodkodModel.domainRelNameToRelMap.get(new Pair<>(aadlKodkodModel.portUnaryRel, propName));
				}
				
				// Add to declared props set
				declPropNames.add(propName);
				
				// We assume that each property only has only 1 non-list value for now
				if(prop.getOwnedValues().size() == 1) {
					ModalPropertyValue val = prop.getOwnedValues().get(0);					
					Relation propValRel = getPropertyValueRel(val.getOwnedValue());
					
					if(propValRel != null) {
						// subcompSig.propSig = propValSig 
						aadlKodkodModel.mkEq(relevantRel.join(propRel), propValRel);
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
	void handleNonUsedProperties(List<String> allDeclPropNames, Set<String> declPropNames, Relation relevantSig, Relation relevantArchSig) {
		
		if(allDeclPropNames != null) {
			for(String propName : allDeclPropNames) {
				if(!declPropNames.contains(propName)) {
					printHeader("Translate a not used property: " + propName);
					String unknownPropName = aadlKodkodModel.UNKNOWN + propName;
					Relation unknownPropValueRel = aadlKodkodModel.nameToUnaryRelMap.get(unknownPropName);
					
					aadlKodkodModel.mkEq(relevantSig.join(aadlKodkodModel.domainRelNameToRelMap.get(new Pair<>(relevantArchSig, propName))), unknownPropValueRel);
				}
			}
		}
	}	
	
	
 	/**
	 * Aux functions to create expressions
	 * */
	
	protected static String sanitizeName(String name) {
		return name != null ? name.replace(".", "_") : "";
	}

	
	protected Optional<PublicPackageSection> getPublicModel(EObject obj) {
		return Util.searchEObject(obj, PublicPackageSection.class);
	}
	
	/**
	 * Get the relation for the input property value
	 * */
	public Relation getPropertyValueRel(PropertyExpression exp)
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
			IntegerLiteralImpl intVal = ((IntegerLiteralImpl) exp);
			value = aadlKodkodModel.DALNames[(int)intVal.getValue()];
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
		return aadlKodkodModel.nameToUnaryRelMap.get(value);
	}	
	
	/**
	 * Increase relation's count by 1
	 * */
	void increaseRelCountByOne(Map<Relation, Integer> map, Relation rel) {
		if(map.containsKey(rel)) {
			int currCount = map.get(rel);
			map.put(rel, currCount+1);
		} else {
			map.put(rel, 1);
		}
	}	
	
	void saveTypeRelAndInstRel(Map<Relation, Set<Relation>> map, Relation compTypeRel, Relation instRel) {
		
		if(map.containsKey(compTypeRel)) {
			System.out.println("1 &*********** Adding type relation: " + compTypeRel + " system implementation relation: " + instRel);
			Set<Relation> instRels = map.get(compTypeRel); 
			instRels.add(instRel);
			map.put(compTypeRel, instRels);
		} else {
			System.out.println("2 &*********** Adding type relation: " + compTypeRel + " system implementation relation: " + instRel);
			Set<Relation> instRels = new HashSet<Relation>();
			if(instRel != null) {
				instRels.add(instRel);
			}
			map.put(compTypeRel, instRels);
		}
	}
	
	
	/**
	 * Add a system, connection or port property 
	 * */
	void saveProperty(String propCat, String propName) {
		if(allDeclProps.containsKey(propCat)) {
			allDeclProps.get(propCat).add(propName);
		} else {			
			if(propCat.equalsIgnoreCase(aadlKodkodModel.PORT) || 
					propCat.equalsIgnoreCase(aadlKodkodModel.SYSTEM) || 
					propCat.equalsIgnoreCase(aadlKodkodModel.CONNECTION)) {
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
