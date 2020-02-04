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
import org.osate.aadl2.Connection;
import org.osate.aadl2.ConnectionEnd;
import org.osate.aadl2.DataPort;
import org.osate.aadl2.Element;
import org.osate.aadl2.ModalPropertyValue;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.NumericRange;
import org.osate.aadl2.Property;
import org.osate.aadl2.PropertyAssociation;
import org.osate.aadl2.PropertyExpression;
import org.osate.aadl2.PropertyOwner;
import org.osate.aadl2.PropertyType;
import org.osate.aadl2.PublicPackageSection;
import org.osate.aadl2.Subcomponent;
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

import com.ge.research.osate.verdict.handlers.AlloyTranslator;

import edu.mit.csail.sdg.alloy4.Pair;
import edu.mit.csail.sdg.alloy4.Pos;
import edu.mit.csail.sdg.ast.Attr;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.ExprBinary;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.ast.Sig.Field;
import edu.mit.csail.sdg.ast.Sig.PrimSig;

public class AadlAlloyTranslator extends AlloyTranslator {
	static final String ENUM = "Enum";
	static final String SYSTEM = "system";
	static final String PORT = "port";
	static final String CONNECTION = "connection";
	
	static final Map<String, List<String>> allDeclProps = new HashMap<>();

	
	/**
	 * Translate all AADL objects
	 * */
	public static void translateFromAADLObjects(Collection<EObject> objects) {
		// Load pre-declared sigs
		SysArchAlloyModel.loadBuiltinConstructs();
		
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
					saveAProp(((MetaclassReferenceImpl)po).getMetaclass().getName().toLowerCase(), prop.getFullName());
				}
			}
		}
		
		// Translate system type 
		for (SystemType system : systems) {
			printHeader("Translate a system type: " + system.getName());
			translateSystem(system);
		}
		// Translate system implementation		
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
	protected static void translateProperty(Property prop) {
		printHeader("Translating Properties");
		
		Field propField = null;
		PrimSig propTypeSig = null;
		String propName = prop.getName();
		PropertyType propType = prop.getPropertyType();		
		
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
				SysArchAlloyModel.propNameToSigMap.put(enumValue, enumValueSig);
			}
			// Add an unknown sig for each prop for the case the property is unassigned
			String unknownEnumValue = SysArchAlloyModel.UNKNOWN + propName; 
			PrimSig unknownEnumValueSig = new PrimSig(unknownEnumValue, propTypeSig, Attr.ONE);
			
			// Save the pair of prop enum type name and prop enum prop; and the pair of enum value and its corresponding sig
			SysArchAlloyModel.propNameToSigMap.put(propEnumTypeName, propTypeSig);
			SysArchAlloyModel.propNameToSigMap.put(unknownEnumValue, unknownEnumValueSig);
		} else if (propType instanceof AadlBooleanImpl) {
			// Don't need to translate it as we have declared a built-in sig Bool
			// But we need to save the unknown_propName and unknown_Bool for later use
			String unknownPropName = SysArchAlloyModel.UNKNOWN + propName;
			propTypeSig = SysArchAlloyModel.BOOL;
			SysArchAlloyModel.propNameToSigMap.put(unknownPropName, SysArchAlloyModel.UNKNOWNBOOL);
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
				String unknownPropName = SysArchAlloyModel.UNKNOWN + propName;
				SysArchAlloyModel.propNameToSigMap.put(unknownPropName, SysArchAlloyModel.UNKNOWNDAL);
				propTypeSig = SysArchAlloyModel.DAL;
			} else {
				throw new RuntimeException("The integer property is not a range!");
			}

		} else if (propType instanceof AadlStringImpl) {
			throw new RuntimeException("Unsupported type: String");
		} else {
			throw new RuntimeException("Unsupported type: " + propType);
		}
		
		// Get the "applies" of a property, and add the property fields
		for(PropertyOwner po : prop.getAppliesTos()) {
			Sig propOwnerSig = null;
			String propOwner = ((MetaclassReferenceImpl)po).getMetaclass().getName();			
			
			if(propOwner.equalsIgnoreCase(CONNECTION)) {
				propField = SysArchAlloyModel.CONNECTION.addField(propName, propTypeSig.oneOf());
				propOwnerSig = SysArchAlloyModel.CONNECTION;
			} else if(propOwner.equalsIgnoreCase(SYSTEM)) {
				propField = SysArchAlloyModel.SYSTEM.addField(propName, propTypeSig.oneOf());
				propOwnerSig = SysArchAlloyModel.SYSTEM;
			} else if(propOwner.equalsIgnoreCase(PORT)) {
				propField = SysArchAlloyModel.PORT.addField(propName, propTypeSig.oneOf());
				propOwnerSig = SysArchAlloyModel.PORT;
			} else {
				throw new RuntimeException("Unsupported property applies to + " + propOwner);
			}
			SysArchAlloyModel.compSigFdNameToFdMap.put(new Pair(propOwnerSig, propName), propField);
		}				
	}	
	
	/**
	 * 
	 * Translate a System Type 
	 * */
	protected static void translateSystem(SystemType system) {
		String sanitizedSysName = sanitizeName(system.getName());
		Sig compTypeSig = new PrimSig(sanitizedSysName, SysArchAlloyModel.COMPTYPE, new Attr[] {});
		
		Expr allInportsUnionExpr = null, allOutportsUnionExpr = null;
		
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
		printHeader("Translate Subcomponents of " + systemImpl.getFullName());
		
		// An implementation name usually has two parts divided by "." 
		// The first part is the type of the implementation
		// The second part is "Impl"
		String sanitizedSystemName = sanitizeName(systemImpl.getName().split("\\.")[0]);
		String sanitizedSysImplName = sanitizeName(systemImpl.getName());
		
		Sig compTypeSig = SysArchAlloyModel.compNameToSigMap.get(sanitizedSystemName);
		Sig compImplSig = SysArchAlloyModel.compNameToSigMap.get(sanitizedSysImplName);
		
		Expr allSubcompsUnionExpr = null;
		
		// subcomponents field of a system impl = the sum of all subcomponents 
		for (Subcomponent subcomp : systemImpl.getOwnedSubcomponents()) {
			printHeader("Translating a subcomponent: " + subcomp.getFullName());
			
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
				allSubcompsUnionExpr = union(allSubcompsUnionExpr, subcompSig);						
			}
			
			// Handle declared subcomponent properties
			Set<String> declPropNames = new HashSet<>();			
			handleUsedProperties(declPropNames, subcomp, null, subcompSig);
			
			// Handle non-declared properties
			handleNonUsedProperties(allDeclProps.get(SYSTEM), declPropNames, subcompSig, SysArchAlloyModel.SYSTEM);
			
			// Save subcomSig and increase system number
			SysArchAlloyModel.compNameToSigMap.put(subcompName, subcompSig);
			SysArchAlloyModel.system++;
		}
		
		// The union of all "subcomponent" expressions is equal to the system 
		// join with its "subcomponents" field 
		if (allSubcompsUnionExpr != null) {
			compImplSig.addFact(equal(join(compTypeSig, SysArchAlloyModel.COMPIMPLSUBCOMPS),
					allSubcompsUnionExpr));
		}		
	}
	
	/**
	 * Handle declared properties
	 * */
	static void handleUsedProperties(Set<String> declPropNames, Subcomponent subcomp, Connection conn, Sig relevantSig) {
		List<PropertyAssociation> propAccs = subcomp == null ? conn.getOwnedPropertyAssociations() : subcomp.getOwnedPropertyAssociations();
		
		if(propAccs != null) {
			for (PropertyAssociation prop : propAccs) { 								
				String propName = prop.getProperty().getFullName();
				Field propField = null;
				printHeader("Translate a used property: " + propName);
				
				if(subcomp != null) {
					propField = SysArchAlloyModel.compSigFdNameToFdMap.get(new Pair<>(SysArchAlloyModel.SYSTEM, propName));
				} else if(conn != null) {
					propField = SysArchAlloyModel.compSigFdNameToFdMap.get(new Pair<>(SysArchAlloyModel.CONNECTION, propName));
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
						SysArchAlloyModel.facts.add(equal(relevantSig.join(propField), propValSig));
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
	static void handleNonUsedProperties(List<String> allDeclPropNames, Set<String> declPropNames, Sig relevantSig, Sig relevantArchSig) {
		
		if(allDeclPropNames != null) {
			for(String propName : allDeclPropNames) {
				if(!declPropNames.contains(propName)) {
					printHeader("Translate a not used property: " + propName);
					String unknownPropName = SysArchAlloyModel.UNKNOWN + propName;
					Sig unknownPropValueSig = SysArchAlloyModel.propNameToSigMap.get(unknownPropName);
					
					SysArchAlloyModel.facts.add(equal(relevantSig.join(SysArchAlloyModel.compSigFdNameToFdMap.get(new Pair<>(relevantArchSig, propName))), unknownPropValueSig));
				}
			}
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
			if(conn.getAllSourceContext()!= null) {
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
			
			
			// Handle declared CONNECTION properties
			Set<String> declPropNames = new HashSet<>();			
			handleUsedProperties(declPropNames, null, conn, connSig);			
			// Handle non-declared properties
			handleNonUsedProperties(allDeclProps.get(CONNECTION), declPropNames, connSig, SysArchAlloyModel.CONNECTION);
			
			// save connection name and sig
			SysArchAlloyModel.compNameToSigMap.put(connName, connSig);
			SysArchAlloyModel.connection++;
		}		
	}
	
	
	protected static String sanitizeName(String name) {
		return name != null ? name.replace(".", "_") : "";
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
	
	
	protected static Optional<PublicPackageSection> getPublicModel(EObject obj) {
		return Util.searchEObject(obj, PublicPackageSection.class);
	}
	
	/**
	 * Get the sig for the input property value
	 * */
	public static Sig getPropertyValueSig(PropertyExpression exp)
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
			value = SysArchAlloyModel.DALNames[(int)inte.getValue()];
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
		return SysArchAlloyModel.propNameToSigMap.get(value);
	}	
	
	
	/**
	 * Add a system, connection or port property 
	 * */
	static void saveAProp(String propCat, String propName) {
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
		System.out.println("---------------- " + msg + " ----------------");
	}
}
