package com.ge.research.osate.verdict.aadl2csv;

import org.osate.aadl2.SystemImplementation;
import org.osate.aadl2.SystemType;
import org.osate.aadl2.impl.BooleanLiteralImpl;
import org.osate.aadl2.impl.EnumerationLiteralImpl;
import org.osate.aadl2.impl.IntegerLiteralImpl;
import org.osate.aadl2.impl.MetaclassReferenceImpl;
import org.osate.aadl2.impl.NamedValueImpl;
import org.osate.aadl2.impl.PropertySetImpl;
import org.osate.aadl2.properties.PropertyAcc;
import org.osate.xtext.aadl2.Aadl2StandaloneSetup;
import org.osate.aadl2.impl.PropertySetImpl;
import org.osate.aadl2.AnnexSubclause;
import org.osate.aadl2.ModalPropertyValue;
import org.osate.aadl2.Property;
import org.osate.aadl2.PropertyAssociation;
import org.osate.aadl2.PropertyExpression;
import org.osate.aadl2.PropertyOwner;
import org.osate.aadl2.Subcomponent;

import com.ge.research.osate.verdict.dsl.VerdictUtil;
import com.ge.research.osate.verdict.dsl.verdict.CyberRel;
import com.ge.research.osate.verdict.dsl.verdict.CyberReq;
import com.ge.research.osate.verdict.dsl.verdict.Event;
import com.ge.research.osate.verdict.dsl.verdict.SafetyRel;
import com.ge.research.osate.verdict.dsl.verdict.SafetyReq;
import com.ge.research.osate.verdict.dsl.verdict.Statement;
import com.ge.research.osate.verdict.dsl.verdict.Verdict;
import com.ge.research.osate.verdict.handlers.VerdictHandlersUtils;
import com.google.inject.Injector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.emf.ecore.resource.Resource;

public class Aadl2CsvTranslator {
	String scenario = null;
	
	public void execute(ExecutionEvent event) {
		translateFromAADLObjects(preprocessAadlFiles(event));
		
	}
	
	/**
	 * Assume the input model is correct without any syntax errors
	 * */
	public void translateFromAADLObjects(List<EObject> objects) { 		
		List<SystemType> systemTypes = new ArrayList<>();
		List<SystemImplementation> systemImpls = new ArrayList<>();
		Map<Property, String> compPropertyToName = new LinkedHashMap<>();
		Map<Property, String> connPropertyToName = new LinkedHashMap<>();
		
		for(EObject obj : objects) {
			if (obj instanceof SystemType) {
				systemTypes.add((SystemType) obj);
			} else if (obj instanceof SystemImplementation) {
				systemImpls.add((SystemImplementation) obj);
			} else if(obj instanceof PropertySetImpl) {
				for(Property prop : ((PropertySetImpl)obj).getOwnedProperties()) {					
					// Save property owner to be used later					
					for(PropertyOwner po : prop.getAppliesTos()) {
						String propCat = ((MetaclassReferenceImpl)po).getMetaclass().getName().toLowerCase();
						String propName = prop.getName();
						
						switch(propCat) {
							case "system": {
								compPropertyToName.put(prop, propName);
								break;
							}
							case "connection": {
								connPropertyToName.put(prop, propName);
								break;
							}
							default: {
								break;
//								throw new RuntimeException("Unsupported new type of property: " + propCat);
							}
						}				
					}
				}
			}
		}
		
		for(SystemType sysType : systemTypes) {
			for(AnnexSubclause annex : sysType.getOwnedAnnexSubclauses()) {
				if(annex.getName().equalsIgnoreCase("verdict")) {
					Verdict verdictAnnex = VerdictUtil.getVerdict(annex);
					
					// Get all cyber req IDs
					for (Statement statement : verdictAnnex.getElements()) {
						if (statement instanceof Event) {
							System.out.println("Event id = "+((Event)statement).getId());
						} else if(statement instanceof CyberReq) {
							System.out.println("CyberReq id = "+((CyberReq)statement).getId());
						} else if(statement instanceof CyberRel) {
							System.out.println("CyberRel id = "+((CyberRel)statement).getId());
						} else if(statement instanceof SafetyReq) {
							System.out.println("SafetyReq id = "+((SafetyReq)statement).getId());
						} else if(statement instanceof SafetyRel) {
							System.out.println("SafetyRel id = "+((SafetyRel)statement).getId());
						}
					}
				}
			}
		}
		
		// Build ScnCompProps.csv, ScnConnections.csv tables 
//		buildScnCompPropsTable(systemImpls, compPropertyToName);
	}
	
	public void buildScnCompPropsTable(List<SystemImplementation> systemImpls, Map<Property, String> compPropertyToName) {
		// "Scenario", "Comp", "Impl", "CompInstance"
		List<String> headers = new ArrayList<String>();
		headers.add("Scenario");
		headers.add("Comp");
		headers.add("Impl");
		headers.add("CompInstance");
		headers.addAll(compPropertyToName.values());
		Table scnCompPropsTable = new Table(headers);	
		
		for(SystemImplementation sysImpl : systemImpls) {
			constructScnCompPropsTableContent(scnCompPropsTable, sysImpl, new ArrayList<>(compPropertyToName.keySet()));
		}		
	}
	
	/**
	 * 
	 * */
	public void constructScnCompPropsTableContent(Table scnCompPropsTable, SystemImplementation systemImpl, List<Property> compProperties) {
		if(systemImpl.getOwnedSubcomponents() != null && !systemImpl.getOwnedSubcomponents().isEmpty()) {
			for (Subcomponent subcomp : systemImpl.getOwnedSubcomponents()) {
				String subcompCompTypeName = subcomp.getComponentType().getName();
				String subcompTypeName = subcomp.getSubcomponentType().getName();
				
				scnCompPropsTable.addValue(scenario);
				scnCompPropsTable.addValue(subcomp.getComponentType().getName());
				scnCompPropsTable.addValue(subcompCompTypeName.equalsIgnoreCase(subcompTypeName)?"":subcompCompTypeName);
				scnCompPropsTable.addValue(subcomp.getName());
				
				for(Property prop : compProperties) {
					String value = getStrRepofPropVal(subcomp.getPropertyValue(prop));
				}
			}
		}
	}	
	
	public String getStrRepofPropVal(PropertyAcc propAcc) {
		String value = null;
		
		if(propAcc != null) {
			List<PropertyAssociation> propAssocs = propAcc.getAssociations();
			
			if(!propAssocs.isEmpty() && propAssocs.size() == 1) {
				PropertyAssociation propAssoc = propAssocs.get(0);
				
				// We assume that each property only has only 1 non-list value for now
				if(propAssoc.getOwnedValues().size() == 1) {
					ModalPropertyValue propVal = propAssoc.getOwnedValues().get(0);					
					
					PropertyExpression exp = propVal.getOwnedValue();
					
					if (exp instanceof BooleanLiteralImpl) {
						BooleanLiteralImpl bool = ((BooleanLiteralImpl) exp);
						value = Boolean.toString(bool.getValue());
					} else if (exp instanceof IntegerLiteralImpl) {
						IntegerLiteralImpl intVal = ((IntegerLiteralImpl) exp);
						value = String.valueOf((int)intVal.getValue());
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
				} else {
					throw new RuntimeException("Unexpected: property value has " + propAssoc.getOwnedValues().size() + " values!");
				}
			} else {
				throw new RuntimeException("Unexpected property association size: " + propAssocs.size());
			}
		}
		return value;
	}
	
	public void translateSystemType(SystemType sysType) {
		
	}
	
	/**
	 * Process an event corresponding to a selection of AADL project
	 * Translate an AADL project into objects 
	 * 
	 * */
	public List<EObject> preprocessAadlFiles(ExecutionEvent event) {
		final Injector injector = new Aadl2StandaloneSetup().createInjectorAndDoEMFRegistration();
		final XtextResourceSet rs = injector.getInstance(XtextResourceSet.class);					
		File dir = new File(VerdictHandlersUtils.getCurrentSelection(event).get(0));									
		List<String> aadlFileNames = new ArrayList<>();
		
		// Set scenario name
		scenario = dir.getName();
		
		// Obtain all AADL files contents in the project
		List<EObject> objects = new ArrayList<>();
		
		for (File file : dir.listFiles()) {
			if (file.getAbsolutePath().endsWith(".aadl")) {
				aadlFileNames.add(file.getAbsolutePath());
			}
		}
		
		final Resource[] resources = new Resource[aadlFileNames.size()];
		for (int i = 0; i < aadlFileNames.size(); i++) {
			resources[i] = rs.getResource(URI.createFileURI(aadlFileNames.get(i)), true);
		}
		
		// Load the resources
		for (final Resource resource : resources) {
			try {
				resource.load(null);
			} catch (final IOException e) {
				System.err.println("ERROR LOADING RESOURCE: " + e.getMessage());
			}
		}
		
		// Load all objects from resources
		for (final Resource resource : resources) {
			resource.getAllContents().forEachRemaining(objects::add);
		}	
		return objects;
	}
	
}
