package com.ge.research.osate.verdict.dsl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.IResourceDescriptions;
import org.eclipse.xtext.resource.impl.ResourceDescriptionsProvider;
import org.osate.aadl2.Aadl2Package;
import org.osate.aadl2.AadlPackage;
import org.osate.aadl2.AnnexLibrary;
import org.osate.aadl2.DefaultAnnexLibrary;
import org.osate.aadl2.Element;
import org.osate.aadl2.MetaclassReference;
import org.osate.aadl2.Property;
import org.osate.aadl2.PropertySet;

import com.ge.research.osate.verdict.dsl.type.AadlTypeWrapper;
import com.ge.research.osate.verdict.dsl.type.BuiltInType;
import com.ge.research.osate.verdict.dsl.type.VerdictField;
import com.ge.research.osate.verdict.dsl.type.VerdictType;
import com.ge.research.osate.verdict.dsl.type.VerdictVariable;
import com.ge.research.osate.verdict.dsl.type.VerdictVariableImpl;
import com.ge.research.osate.verdict.dsl.verdict.Exists;
import com.ge.research.osate.verdict.dsl.verdict.Forall;
import com.ge.research.osate.verdict.dsl.verdict.Intro;
import com.ge.research.osate.verdict.dsl.verdict.ThreatDatabase;
import com.ge.research.osate.verdict.dsl.verdict.ThreatEqualContains;
import com.ge.research.osate.verdict.dsl.verdict.ThreatModel;
import com.ge.research.osate.verdict.dsl.verdict.ThreatStatement;
import com.ge.research.osate.verdict.dsl.verdict.Var;
import com.ge.research.osate.verdict.dsl.verdict.VerdictContractLibrary;
import com.ge.research.osate.verdict.dsl.verdict.VerdictThreatModels;

/**
 * Utilities for validating threat models.
 * Contains most of the type checker code.
 */
public class ThreatModelUtil {
	/**
	 * Get the root AST "VerdictThreatModels" node from an annex.
	 *
	 * @param obj some object that might be a VerdictThreatModels
	 * @return the VerdictThreatModels
	 */
	public static VerdictThreatModels getVerdictThreatModels(Object obj) {
		if (obj == null) {
			return null;
		} else if (obj instanceof VerdictThreatModels) {
			return (VerdictThreatModels) obj;
		} else if (obj instanceof VerdictContractLibrary) {
			return ((VerdictContractLibrary) obj).getContract();
		} else if (obj instanceof DefaultAnnexLibrary) {
			return getVerdictThreatModels(((DefaultAnnexLibrary) obj).getParsedAnnexLibrary());
		} else {
			throw new IllegalArgumentException("bad verdict threat models: " + obj.getClass().getName());
		}
	}

	/**
	 * Get the set of all threat model IDs defined in the current AADL file.
	 *
	 * @param obj the object from which to search upward toward the AADL package
	 * @return the set of all threats defined in the AADL package
	 */
	public static Set<String> getDefinedThreatModels(EObject obj) {
		Set<String> definedThreats = new HashSet<>();
		for (AnnexLibrary library : getAadlPackage(obj).getOwnedPublicSection().getOwnedAnnexLibraries()) {
			if ("verdict".equals(library.getName())) {
				VerdictThreatModels threats = ThreatModelUtil.getVerdictThreatModels(library);
				for (ThreatStatement statement : threats.getStatements()) {
					if (statement instanceof ThreatModel) {
						definedThreats.add(((ThreatModel) statement).getId());
					}
				}
			}
		}
		return definedThreats;
	}

	/**
	 * Get the set of all threat database IDs defined in the current AADL file.
	 *
	 * @param obj the object from which to search upward toward the AADL package
	 * @return the set of all databases defined in the AADL package
	 */
	public static Set<String> getDefinedThreatDatabases(EObject obj) {
		Set<String> definedDatabases = new HashSet<>();
		for (AnnexLibrary library : getAadlPackage(obj).getOwnedPublicSection().getOwnedAnnexLibraries()) {
			if ("verdict".equals(library.getName())) {
				VerdictThreatModels threats = ThreatModelUtil.getVerdictThreatModels(library);
				for (ThreatStatement statement : threats.getStatements()) {
					if (statement instanceof ThreatDatabase) {
						definedDatabases.add(((ThreatDatabase) statement).getId());
					}
				}
			}
		}
		return definedDatabases;
	}

	private static void addBuiltin(Map<String, VerdictType> map, BuiltInType type) {
		map.put(type.getFullName(), type);
	}

	/**
	 * Build a map from type names to types. Traverses all files
	 * in the current project looking for property declarations, which
	 * are used to populate fields for the built-in types.
	 *
	 * Built-in types are system, connection, and port (also portDirection).
	 *
	 * This method is not very efficient, and it gets called several times
	 * on every keystroke. Fortunately there still seems to be reasonably fast.
	 * A crucial optimization will be caching the results because the set of
	 * properties does not change that frequently.
	 *
	 * @param obj an AST node context, used to get access to project files
	 * @param indexProvider an index provider, may be obtained through Guice
	 * @return the constructed type map
	 */
	public static LinkedHashMap<String, VerdictType> getTypes(EObject obj, ResourceDescriptionsProvider indexProvider) {
		LinkedHashMap<String, VerdictType> types = new LinkedHashMap<>();

		// Three main built-in types
		BuiltInType connection = new BuiltInType("connection");
		BuiltInType port = new BuiltInType("port");
		BuiltInType system = new BuiltInType("system");

		addBuiltin(types, connection);
		addBuiltin(types, port);
		addBuiltin(types, system);

		// Connection fields
		connection.addField("inPort", port);
		connection.addField("outPort", port);
		connection.addField("source", system);
		connection.addField("dest", system);

		// Port direction
		BuiltInType portDir = new BuiltInType("portDirection");
		portDir.addValue("in");
		portDir.addValue("out");

		// Port fields
		port.addField("direction", portDir);
		port.addField("connections", connection.getListType());

		// System fields
		system.addField("subcomponents", system.getListType());
		system.addField("connections", connection.getListType());
		system.addField("ports", port.getListType());

		// Get the path to the current resource, used to get the project path
		String[] resSegments = obj.eResource().getURI().segments();

		// Iterate through all resources
		IResourceDescriptions index = indexProvider.getResourceDescriptions(obj.eResource());
		descLoop:
		for (IEObjectDescription desc : index
				.getExportedObjectsByType(Aadl2Package.eINSTANCE.getPropertySet())) {

			// Get the path to the resource we are examining
			String[] propsResSegments = desc.getEObjectURI().segments();

			// Only accept this resource if it is from the same project
			// The project is determined by the first two URI segments
			for (int i = 0; i < Math.min(2, Math.min(resSegments.length, propsResSegments.length)); i++) {
				if (!resSegments[i].equals(propsResSegments[i])) {
					continue descLoop;
				}
			}

			// Load the resource into EMF-land; dynamically loads if necessary
			Resource res = obj.eResource().getResourceSet().getResource(desc.getEObjectURI(), true);
			if (res != null) {
				// Search the AST
				TreeIterator<EObject> it = res.getAllContents();
				while (it.hasNext()) {
					EObject next = it.next();
					if (next instanceof PropertySet) {
						PropertySet props = (PropertySet) next;
						// Iterate the declared properties
						for (Element elem : props.getOwnedElements()) {
							if (elem instanceof Property) {
								Property prop = (Property) elem;
								// Make sure type information is present
								if (prop.getPropertyType() != null) {
									// The metaclasses that a property applies to are
									// the types for which the property is a field
									for (MetaclassReference meta : prop.getAppliesToMetaclasses()) {
										// Get type name, lowercase it because it is a class name
										String appliesToMetaclass = meta.getMetaclass().getName().toLowerCase();
										// Hopefully this is a type that we have accounted for
										if (types.containsKey(appliesToMetaclass)) {
											((BuiltInType) types.get(appliesToMetaclass)).addField(prop.getName(),
													new AadlTypeWrapper(prop.getName(), prop.getPropertyType()));
										} else {
											// If we get this error message, then perhaps need to add
											// some built-in types
											System.err.println("could not find built in type: " + appliesToMetaclass);
										}
									}
								}
							}
						}
						// Discard all children of the property set
						it.prune();
					}
				}
			}
		}

		// Prevent synchronization issues
		portDir.lock();
		connection.lock();
		port.lock();
		system.lock();

		return types;
	}

	/**
	 * Get all variables in scope for an expression.
	 *
	 * Searches up the AST for variable introductions, starting from the
	 * immediate parent of obj.
	 *
	 * Note that if scoping a quantification, you cannot pass the object
	 * directly because this might include the newly-introduced variable
	 * in its own scope! See getContainerForClasses() for obtaining the
	 * correct parent to use for scoping.
	 *
	 * @param obj the context for which to find scope.
	 * @param indexProvider the index provider, may be obtained from Guice
	 * @return the list of variables that are in scope
	 */
	public static List<VerdictVariable> getScope(EObject obj, ResourceDescriptionsProvider indexProvider) {
		// Get type information
		LinkedHashMap<String, VerdictType> types = getTypes(obj, indexProvider);

		List<VerdictVariable> vars = new ArrayList<>();
		// Traverse upward until we find the enclosing threat model
		while (!(obj instanceof ThreatModel || obj == null)) {
			obj = obj.eContainer();
			if (obj instanceof ThreatModel) {
				// Threat model introduces a system
				ThreatModel threatModel = (ThreatModel) obj;
				vars.add(VerdictVariableImpl.fromIntro(threatModel.getIntro(), types));
			} else if (obj instanceof Forall) {
				// Forall introduces a variable
				Forall forall = (Forall) obj;
				vars.add(VerdictVariableImpl.fromIntro(forall.getIntro(), types));
			} else if (obj instanceof Exists) {
				// Exists introduces a variable
				Exists exists = (Exists) obj;
				vars.add(VerdictVariableImpl.fromIntro(exists.getIntro(), types));
			}
		}

		return vars;
	}

	/**
	 * The result of calling getVarType(). See field documentation.
	 */
	public static class FieldTypeResult {
		/**
		 * The name of the variable. Always present.
		 */
		public String varName;

		/**
		 * If the variable name is in scope, then the corresponding
		 * variable. Otherwise, empty.
		 */
		public Optional<VerdictVariable> var;

		/**
		 * If the variable name is in scope and all fields type-check,
		 * then the type of the final field. Otherwise, empty.
		 */
		public Optional<VerdictType> type;

		/**
		 * The index of the last field processed. Equal to the number of
		 * fields if all fields type-check, or the index of the first field
		 * that did not type-check.
		 */
		public int fieldIndex;

		/**
		 * The name of the last field if that field did not type-check.
		 * Null if entire var/field type checks. (Used for error reporting.)
		 */
		public String lastField;

		public FieldTypeResult() {
			varName = "";
			var = Optional.empty();
			type = Optional.empty();
			fieldIndex = 0;
			lastField = "";
		}
	}

	/**
	 * Get the type of a variable/field.
	 *
	 * First finds the type of the variable by looking it up in the
	 * scope. Then finds the type of each variable by looking it up
	 * in the fields of the previous type.
	 *
	 * If at any point something is out of scope or does not type-check,
	 * then the returned type will be empty.
	 *
	 * @param varField the Var AST object to type
	 * @param indexProvider the index provider, may be obtained from Guice
	 * @return see FieldTypeResult
	 */
	public static FieldTypeResult getVarType(Var varField, ResourceDescriptionsProvider indexProvider) {
		FieldTypeResult result = new FieldTypeResult();

		// Get correct parent for scoping
		EObject scopeParent = getContainerForClasses(varField, VAR_FIELD_SCOPE_PARENT_CLASSES);

		// ID is always present
		result.varName = varField.getId();

		// Get all variables in scope
		List<VerdictVariable> vars = ThreatModelUtil.getScope(scopeParent, indexProvider);

		// Get the variable corresponding to the ID
		// Empty if that ID is not in scope
		result.var = vars.stream().filter(var -> var.getId().equals(result.varName)).findFirst();

		if (!result.var.isPresent()) {
			return result;
		} else {
			if (!result.var.get().getType().isPresent()) {
				// It is possible that the variable is in scope, but it does not
				// yet have a type because the user is still editing the var/field
				return result;
			} else {
				// Check type iteratively for all fields and their children
				// Invariant: "type" holds the type of the rightmost var/field that has been processed
				VerdictType type = result.var.get().getType().get();
				if (varField.getIds() != null) {
					for (String fieldName : varField.getIds()) {
						// Find the field of the current type for the next field name
						Optional<VerdictField> field = type.getFields().stream()
								.filter(f -> f.getName().equals(fieldName)).findFirst();
						if (field.isPresent()) {
							// Well-typed, advance to the next field
							type = field.get().getType();
							result.fieldIndex++;
						} else {
							// Not well-typed, crash and burn
							result.lastField = fieldName;
							return result;
						}
					}
				}
				// All fields type-check and "type" holds the rightmost type
				result.type = Optional.of(type);
				return result;
			}
		}
	}

	/**
	 * Get the type of a variable introduced by an Intro. Empty if not well-typed.
	 *
	 * @param intro the Intro
	 * @param indexProvider an index provider, may be obtained from Guice
	 * @return the type of the Intro
	 */
	public static Optional<VerdictType> getIntroType(Intro intro, ResourceDescriptionsProvider indexProvider) {
		return Optional.ofNullable(getTypes(intro, indexProvider).get(intro.getType()));
	}

	/**
	 * Get the AADL package surrounding the given object.
	 *
	 * @param obj the object
	 * @return the enclosing AADL package
	 */
	public static AadlPackage getAadlPackage(EObject obj) {
		return (AadlPackage) getContainerForClasses(obj, AADL_PACKAGE_CLASSES);
	}

	// Preset values for passing to getContainerForClasses()
	public static List<Class<? extends EObject>> VAR_FIELD_SCOPE_PARENT_CLASSES, INTRO_SCOPE_PARENT_CLASSES,
			AADL_PACKAGE_CLASSES;

	static {
		VAR_FIELD_SCOPE_PARENT_CLASSES = new ArrayList<>();
		VAR_FIELD_SCOPE_PARENT_CLASSES.add(Forall.class);
		VAR_FIELD_SCOPE_PARENT_CLASSES.add(Exists.class);
		VAR_FIELD_SCOPE_PARENT_CLASSES.add(ThreatEqualContains.class);

		INTRO_SCOPE_PARENT_CLASSES = new ArrayList<>();
		INTRO_SCOPE_PARENT_CLASSES.add(Forall.class);
		INTRO_SCOPE_PARENT_CLASSES.add(Exists.class);

		AADL_PACKAGE_CLASSES = new ArrayList<>();
		AADL_PACKAGE_CLASSES.add(AadlPackage.class);
	}

	/**
	 * Get the most direct parent of an AST object that is a subclass of
	 * on of the provided classes.
	 *
	 * @param obj the object to search upward from
	 * @param classes the list of classes to match against
	 * @return the most direct parent of obj matching the classes, or null if
	 *         such a parent could not be found
	 */
	public static EObject getContainerForClasses(EObject obj, List<Class<? extends EObject>> classes) {
		while (obj != null) {
			for (Class<? extends EObject> cls : classes) {
				if (cls.isAssignableFrom(obj.getClass())) {
					return obj;
				}
			}
			obj = obj.eContainer();
		}
		return null;
	}
}
