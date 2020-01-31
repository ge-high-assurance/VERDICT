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
	
	static final String BOOL = "Bool";
	static final String ENUM = "Enum";
	static final String INT = "Int";
	static final String SYSTEM = "system";
	static final String PORT = "port";
	static final String CONNECTION = "connection";
	
	static final Map<String, List<String>> aadlProps = new HashMap<>();
	
	/**
	 * 
	 * */
	public static void translateFromAADLObjects(Collection<EObject> objects) {
		List<PublicPackageSection> models = objects.stream()
				.map(AadlAlloyTranslator::getPublicModel)
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
		// Translate properties
		for(EObject obj : objects) {
			if(obj instanceof PropertySetImpl) {
				for(Property prop : ((PropertySetImpl)obj).getOwnedProperties()) {
					translateProperty(prop);
					
					// Store property to be used later					
					for(PropertyOwner po : prop.getAppliesTos()) {
						saveAProp(((MetaclassReferenceImpl)po).getMetaclass().getName().toLowerCase(), prop.getFullName());
					}
				}
			} 			
		}
		
		// Translate system type 
		for (SystemType system : systems) {
			translateSystem(system);
		}
		// Translate system implementation		
		for (SystemImplementation systemImpl : systemImpls) {
			translateSystemImpl(systemImpl);
			translateSystemImplSubcomponents(systemImpl);
			translateSystemImplConnections(systemImpl);
		}
		
		// TODO state disjunction of all ports
	}
	
	/**
	 * Translate a connection or system property
	 * */
	protected static void translateProperty(Property prop) {
		PrimSig propSig = null;
		String propName = prop.getName();
		PropertyType propType = prop.getPropertyType();
		
		if (propType instanceof EnumerationTypeImpl) {
			String propEnumTypeName = propName + "_" + ENUM;
			propSig = new PrimSig(propEnumTypeName, Attr.ABSTRACT);
			
			// For each enumerate value, we create a sig
			for(NamedElement ne : ((EnumerationTypeImpl)propType).getMembers()) {
				String enumValue = ne.getName();
				PrimSig enumValueSig = new PrimSig(enumValue, propSig, Attr.ONE);
				// Save the pair of enum value and its corresponding sig 
				SysArchAlloyModel.propEnumValToSigMap.put(enumValue, enumValueSig);
			}
			// Add an unknown sig for each prop for the case the property is unassigned
			String unknownEnumValue = SysArchAlloyModel.UNKNOWN + propName; 
			PrimSig unknownEnumValueSig = new PrimSig(unknownEnumValue, propSig, Attr.ONE);
			
			// Save the pair of prop enum type name and prop enum prop; and the pair of enum value and its corresponding sig
			SysArchAlloyModel.propNameToSigMap.put(propName, propSig);
			SysArchAlloyModel.propEnumValToSigMap.put(unknownEnumValue, unknownEnumValueSig);
		} else if (propType instanceof AadlBooleanImpl) {
			// Don't need to translate it as we have declared a built-in sig Bool
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
			} else {
				throw new RuntimeException("The integer property is not a range!");
			}
			// 
		} else if (propType instanceof AadlStringImpl) {
			throw new RuntimeException("Unsupported type: String");
		} else {
			throw new RuntimeException("Unsupported type: " + propType);
		}
		
		if (propSig != null) {
			SysArchAlloyModel.propNameToSigMap.put(propName, propSig);
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
			
			System.out.println("translating subcomponent: " + subcomp.getName());
			
			// Handle declared subcomponent properties
			Set<String> declPropNames = new HashSet<>();			
			handleUsedProperties(declPropNames, subcomp, null, subcompSig);
			
			// Handle non-declared properties
			handleNonUsedProperties(aadlProps.get(SYSTEM), declPropNames, subcompSig);
			
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
			for (PropertyAssociation prop : subcomp.getOwnedPropertyAssociations()) {
				PropertyAssociationImpl propImpl = (PropertyAssociationImpl)prop; 
				System.out.println("propery name: " + propImpl.getProperty().getName());
				String propName = prop.getProperty().getFullName();
				Sig propSig = SysArchAlloyModel.propNameToSigMap.get(propName);
				
				// Add to declared props set
				declPropNames.add(propName);
				
				// We assume that each property only has only 1 value for now
				if(prop.getOwnedValues().size() == 1) {
					ModalPropertyValue val = prop.getOwnedValues().get(0);
					Sig propValSig = getPropertyValueSig(val.getOwnedValue());
					
					if(propValSig != null) {
						// sugcompSig.propSig = propValSig
						relevantSig.addFact(equal(relevantSig.join(propSig), propValSig));
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
	static void handleNonUsedProperties(List<String> allDeclPropNames, Set<String> declPropNames, Sig relevantSig) {
		if(allDeclPropNames != null) {
			for(String prop : allDeclPropNames) {
				if(!declPropNames.contains(prop)) {
					String unknownPropName = SysArchAlloyModel.UNKNOWN + prop;
					Sig unknownPropNameSig = SysArchAlloyModel.propNameToSigMap.get(unknownPropName);
					SysArchAlloyModel.facts.add(equal(relevantSig.join(SysArchAlloyModel.propNameToSigMap.get(prop)), unknownPropNameSig));
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
			
			
			// Handle declared CONNECTION properties
			Set<String> declPropNames = new HashSet<>();			
			handleUsedProperties(declPropNames, null, conn, connSig);			
			// Handle non-declared properties
			handleNonUsedProperties(aadlProps.get(CONNECTION), declPropNames, connSig);
			
			// save connection name and sig
			SysArchAlloyModel.compNameToSigMap.put(connName, connSig);
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
	
	
	protected static Optional<PublicPackageSection> getPublicModel(EObject obj) {
		return Util.searchEObject(obj, PublicPackageSection.class);
	}
	
	/**
	 * Get the type name from a property type
	 * */
	static String getPropTypeName(PropertyType propType) {
		String typeName = "";
		
		if(propType instanceof EnumerationTypeImpl) {
			typeName = ENUM;
		} else if(propType instanceof AadlBooleanImpl) {
			typeName = BOOL;
		} else if (propType instanceof AadlIntegerImpl) {
			typeName = INT;
		} else {
			throw new RuntimeException("Unsupported type: " + propType.getFullName());
		}
		
		return typeName;
	}	
	
	/**
	 * Get the sig for the input property value
	 * */
	public static Sig getPropertyValueSig(PropertyExpression exp)
	{
		if(exp == null) {
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
		if(aadlProps.containsKey(propCat)) {
			aadlProps.get(propCat).add(propName);
		} else {			
			if(propCat.equalsIgnoreCase(PORT) || propCat.equalsIgnoreCase(SYSTEM) || propCat.equalsIgnoreCase(CONNECTION)) {
				List<String> props = new ArrayList<>();
				props.add(propName);
				aadlProps.put(propCat.toLowerCase(), props);	
			} else {
				throw new RuntimeException("New type of property: " + propCat);
			}												
		}
	}
}
