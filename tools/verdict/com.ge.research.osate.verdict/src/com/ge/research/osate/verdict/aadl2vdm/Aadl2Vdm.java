package com.ge.research.osate.verdict.aadl2vdm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.osate.aadl2.AbstractImplementation;
import org.osate.aadl2.AbstractSubcomponent;
import org.osate.aadl2.AbstractType;
import org.osate.aadl2.AccessType;
import org.osate.aadl2.AnnexSubclause;
import org.osate.aadl2.BooleanLiteral;
import org.osate.aadl2.BusAccess;
import org.osate.aadl2.BusImplementation;
import org.osate.aadl2.BusSubcomponent;
import org.osate.aadl2.BusSubcomponentType;
import org.osate.aadl2.BusType;
import org.osate.aadl2.ComponentImplementation;
import org.osate.aadl2.ComponentType;
import org.osate.aadl2.Connection;
import org.osate.aadl2.ConnectionEnd;
import org.osate.aadl2.ContainedNamedElement;
import org.osate.aadl2.ContainmentPathElement;
import org.osate.aadl2.Context;
import org.osate.aadl2.DataAccess;
import org.osate.aadl2.DataImplementation;
import org.osate.aadl2.DataPort;
import org.osate.aadl2.DataSubcomponent;
import org.osate.aadl2.DataSubcomponentType;
import org.osate.aadl2.DataType;
import org.osate.aadl2.DeviceImplementation;
import org.osate.aadl2.DeviceSubcomponent;
import org.osate.aadl2.DeviceType;
import org.osate.aadl2.EventDataPort;
import org.osate.aadl2.EventPort;
import org.osate.aadl2.MemoryImplementation;
import org.osate.aadl2.MemorySubcomponent;
import org.osate.aadl2.MemoryType;
import org.osate.aadl2.ModalPropertyValue;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.ProcessImplementation;
import org.osate.aadl2.ProcessSubcomponent;
import org.osate.aadl2.ProcessType;
import org.osate.aadl2.ProcessorImplementation;
import org.osate.aadl2.ProcessorType;
import org.osate.aadl2.Property;
import org.osate.aadl2.PropertyAssociation;
import org.osate.aadl2.PropertyExpression;
import org.osate.aadl2.PropertyOwner;
import org.osate.aadl2.PropertyType;
import org.osate.aadl2.Subcomponent;
import org.osate.aadl2.SubprogramImplementation;
import org.osate.aadl2.SubprogramSubcomponent;
import org.osate.aadl2.SubprogramType;
import org.osate.aadl2.SystemImplementation;
import org.osate.aadl2.SystemSubcomponent;
import org.osate.aadl2.SystemType;
import org.osate.aadl2.ThreadGroupImplementation;
import org.osate.aadl2.ThreadGroupSubcomponent;
import org.osate.aadl2.ThreadGroupType;
import org.osate.aadl2.ThreadImplementation;
import org.osate.aadl2.ThreadSubcomponent;
import org.osate.aadl2.ThreadType;
import org.osate.aadl2.VirtualProcessorImplementation;
import org.osate.aadl2.VirtualProcessorSubcomponent;
import org.osate.aadl2.VirtualProcessorType;
import org.osate.aadl2.impl.AadlBooleanImpl;
import org.osate.aadl2.impl.AadlIntegerImpl;
import org.osate.aadl2.impl.AadlStringImpl;
import org.osate.aadl2.impl.AccessConnectionImpl;
import org.osate.aadl2.impl.BooleanLiteralImpl;
import org.osate.aadl2.impl.DataImplementationImpl;
import org.osate.aadl2.impl.DataTypeImpl;
import org.osate.aadl2.impl.EnumerationLiteralImpl;
import org.osate.aadl2.impl.EnumerationTypeImpl;
import org.osate.aadl2.impl.IntegerLiteralImpl;
import org.osate.aadl2.impl.ListValueImpl;
import org.osate.aadl2.impl.MetaclassReferenceImpl;
import org.osate.aadl2.impl.NamedValueImpl;
import org.osate.aadl2.impl.PropertySetImpl;
import org.osate.aadl2.impl.ReferenceValueImpl;
import org.osate.aadl2.impl.StringLiteralImpl;
import org.osate.aadl2.properties.PropertyAcc;
import org.osate.xtext.aadl2.Aadl2StandaloneSetup;

import com.ge.research.osate.verdict.dsl.VerdictUtil;
import com.ge.research.osate.verdict.dsl.verdict.CyberMission;
import com.ge.research.osate.verdict.dsl.verdict.CyberRel;
import com.ge.research.osate.verdict.dsl.verdict.CyberReq;
import com.ge.research.osate.verdict.dsl.verdict.Event;
import com.ge.research.osate.verdict.dsl.verdict.FExpr;
import com.ge.research.osate.verdict.dsl.verdict.LAnd;
import com.ge.research.osate.verdict.dsl.verdict.LExpr;
import com.ge.research.osate.verdict.dsl.verdict.LNot;
import com.ge.research.osate.verdict.dsl.verdict.LOr;
import com.ge.research.osate.verdict.dsl.verdict.LPort;
import com.ge.research.osate.verdict.dsl.verdict.SLAnd;
import com.ge.research.osate.verdict.dsl.verdict.SLExpr;
import com.ge.research.osate.verdict.dsl.verdict.SLNot;
import com.ge.research.osate.verdict.dsl.verdict.SLOr;
import com.ge.research.osate.verdict.dsl.verdict.SLPort;
import com.ge.research.osate.verdict.dsl.verdict.SafetyRel;
import com.ge.research.osate.verdict.dsl.verdict.SafetyReq;
import com.ge.research.osate.verdict.dsl.verdict.Statement;
import com.ge.research.osate.verdict.dsl.verdict.Verdict;
import com.google.inject.Injector;

import verdict.vdm.vdm_data.RecordField;
import verdict.vdm.vdm_data.RecordType;
import verdict.vdm.vdm_data.TypeDeclaration;
import verdict.vdm.vdm_model.Model;
import verdict.vdm.vdm_model.Port;



/**
*
* @author Saswata Paul
*
*/
public class Aadl2Vdm {

	/**
	 * The execute() method
	 * creates a new Model object and
	 * returns it
	 *
	 * @param inputDir a reference to a directory
	 *
	 * */
	public Model execute(File inputDir)
	   {
      		logHeader("AADL2VDM");
		    Model m = new Model();
		    Agree2Vdm agree2vdm= new Agree2Vdm();
		    //first argument in the function call returns list of objects from all AADL files in the project including 
		    //imported files - this is needed to resolve issue of missing cross references to types defined in imported files
		    //second argument returns the list of objects from only AADL files in the project - only these 
		    //objects are used while populating the VDM
			m = populateVDMFromAadlObjects(agree2vdm.preprocessAadlFiles(inputDir), preprocessAadlFiles(inputDir), m);
			System.out.println("Info: Created VDM object");
			return m;
	   }


	/**
	 * Assume the input is correct without any syntax errors
	 * Populate mission req, cyber and safety reqs and rels from AADL objects
	 *
	 *  @param objects a List of AADL objects,
	 * @param objectsFromFilesInProject 
	 * 	@param model an empty VDM model to populate
	 *  @return a populated VDM model
	 * Vidhya: modified function to add and process only objects in the aadl files in the project excluding those in imported aadl files
	 * */
	public Model populateVDMFromAadlObjects(List<EObject> objects, List<EObject> objectsFromFilesInProject, Model model) {
		HashSet<String> dataTypeDecl = new HashSet<String>();
		// variables for extracting data from the AADL object
		List<SystemType> systemTypes = new ArrayList<>();
		List<BusType> busTypes = new ArrayList<>();
		List<SubprogramType> subprogramTypes = new ArrayList<>();
		List<ThreadType> threadTypes = new ArrayList<>();
		List<MemoryType> memoryTypes = new ArrayList<>();
		List<DeviceType> deviceTypes = new ArrayList<>();
		List<AbstractType> abstractTypes = new ArrayList<>();
		List<ProcessType> processTypes = new ArrayList<>();
		List<ThreadGroupType> threadGroupTypes = new ArrayList<>();
		List<VirtualProcessorType> virtualProcessorTypes = new ArrayList<>();
		List<ProcessorType> processorTypes = new ArrayList<>();
		List<ComponentImplementation> compImpls = new ArrayList<>();
		Map<Property, String> connPropertyToName = new LinkedHashMap<>();
		Map<Property, String> componentPropertyToName = new LinkedHashMap<>();
		
		//process only those properties defined in files in the project and not in the imported files
		HashSet<String> objectNamesFromFilesInProject = getObjectNames(objectsFromFilesInProject);

		// extracting data from the AADLObject
		for(EObject obj : objects) {
			if (obj instanceof SystemType) {
				if(objectNamesFromFilesInProject.contains(((SystemType) obj).getName())) {
					systemTypes.add((SystemType) obj);
				}
			} else if (obj instanceof BusType) {
				if(objectNamesFromFilesInProject.contains(((BusType) obj).getName())) {
					busTypes.add((BusType)obj);
				}
			} else if (obj instanceof SubprogramType) {
				if(objectNamesFromFilesInProject.contains(((SubprogramType) obj).getName())) {
					subprogramTypes.add((SubprogramType)obj);
				}
			} else if (obj instanceof ThreadType) {
				if(objectNamesFromFilesInProject.contains(((ThreadType) obj).getName())) {
					threadTypes.add((ThreadType)obj);
				}
			} else if (obj instanceof MemoryType) {
				if(objectNamesFromFilesInProject.contains(((MemoryType) obj).getName())) {
					memoryTypes.add((MemoryType)obj);
				}
			} else if (obj instanceof DeviceType) {
				if(objectNamesFromFilesInProject.contains(((DeviceType) obj).getName())) {
					deviceTypes.add((DeviceType)obj);
				}
			} else if (obj instanceof AbstractType) {
				if(objectNamesFromFilesInProject.contains(((AbstractType) obj).getName())) {
					abstractTypes.add((AbstractType)obj);
				}
			} else if (obj instanceof ProcessType) {
				if(objectNamesFromFilesInProject.contains(((ProcessType) obj).getName())) {
					processTypes.add((ProcessType)obj);
				}
			} else if (obj instanceof ThreadGroupType) {
				if(objectNamesFromFilesInProject.contains(((ThreadGroupType) obj).getName())) {
					threadGroupTypes.add((ThreadGroupType)obj);
				}
			} else if (obj instanceof VirtualProcessorType) {
				if(objectNamesFromFilesInProject.contains(((VirtualProcessorType) obj).getName())) {
					virtualProcessorTypes.add((VirtualProcessorType)obj);
				}
			} else if (obj instanceof ProcessorType) {
				if(objectNamesFromFilesInProject.contains(((ProcessorType) obj).getName())) {
					processorTypes.add((ProcessorType)obj);
				}
			} else if (obj instanceof SystemImplementation) {
				if(objectNamesFromFilesInProject.contains(((SystemImplementation) obj).getName())) {
					compImpls.add((SystemImplementation) obj);
				}
			} else if (obj instanceof SubprogramImplementation) {
				if(objectNamesFromFilesInProject.contains(((SubprogramImplementation) obj).getName())) {
					compImpls.add((SubprogramImplementation) obj);
				}
			} else if (obj instanceof ThreadImplementation) {
				if(objectNamesFromFilesInProject.contains(((ThreadImplementation) obj).getName())) {
					compImpls.add((ThreadImplementation) obj);
				}
			} else if (obj instanceof MemoryImplementation) {
				if(objectNamesFromFilesInProject.contains(((MemoryImplementation) obj).getName())) {
					compImpls.add((MemoryImplementation) obj);
				}
			} else if (obj instanceof BusImplementation) {
				if(objectNamesFromFilesInProject.contains(((BusImplementation) obj).getName())) {
					compImpls.add((BusImplementation) obj);
				}
			} else if (obj instanceof AbstractImplementation) {
				if(objectNamesFromFilesInProject.contains(((AbstractImplementation) obj).getName())) {
					compImpls.add((AbstractImplementation) obj);
				}
			} else if (obj instanceof DeviceImplementation) {
				if(objectNamesFromFilesInProject.contains(((DeviceImplementation) obj).getName())) {
					compImpls.add((DeviceImplementation) obj);
				}
			} else if (obj instanceof ProcessImplementation) {
				if(objectNamesFromFilesInProject.contains(((ProcessImplementation) obj).getName())) {
					compImpls.add((ProcessImplementation) obj);
				}
			} else if (obj instanceof ThreadGroupImplementation) {
				if(objectNamesFromFilesInProject.contains(((ThreadGroupImplementation) obj).getName())) {
					compImpls.add((ThreadGroupImplementation) obj);
				}
			} else if (obj instanceof VirtualProcessorImplementation) {
				if(objectNamesFromFilesInProject.contains(((VirtualProcessorImplementation) obj).getName())) {
					compImpls.add((VirtualProcessorImplementation) obj);
				}
			} else if (obj instanceof ProcessorImplementation) {
				if(objectNamesFromFilesInProject.contains(((ProcessorImplementation) obj).getName())) {
					compImpls.add((ProcessorImplementation) obj);
				}
			} else if(obj instanceof PropertySetImpl) {
					Set<Property> compPropSet = new HashSet<Property>();
					Set<Property> connPropSet = new HashSet<Property>();
					for(Property prop : ((PropertySetImpl)obj).getOwnedProperties()) {
						// Save property owner to be used later
						for(PropertyOwner po : prop.getAppliesTos()) {
							String propCat = ((MetaclassReferenceImpl)po).getMetaclass().getName().toLowerCase();
							String propName = prop.getName();
							switch(propCat) {
								case "system": {
									if(objectNamesFromFilesInProject.contains(propName)) {
										componentPropertyToName.put(prop, propName);
									}
									compPropSet.add(prop);
									break;
								}
								case "thread": {
									if(objectNamesFromFilesInProject.contains(propName)) {
										componentPropertyToName.put(prop, propName);
									}
									compPropSet.add(prop);
									break;
								}
								case "processor": {
									if(objectNamesFromFilesInProject.contains(propName)) {
										componentPropertyToName.put(prop, propName);
									}
									compPropSet.add(prop);
									break;
								}
								case "memory": {
									if(objectNamesFromFilesInProject.contains(propName)) {
										componentPropertyToName.put(prop, propName);
									}
									compPropSet.add(prop);
									break;
								}
								case "connection": {
									if(objectNamesFromFilesInProject.contains(propName)) {
										connPropertyToName.put(prop, propName);
									}
									connPropSet.add(prop);
									break;
								}
								case "process": {
									if(objectNamesFromFilesInProject.contains(propName)) {
										componentPropertyToName.put(prop, propName);
									}
									compPropSet.add(prop);
									break;
								}
								case "abstract": {
									if(objectNamesFromFilesInProject.contains(propName)) {
										componentPropertyToName.put(prop, propName);
									}
									compPropSet.add(prop);
									break;
								}
								case "device": {
									if(objectNamesFromFilesInProject.contains(propName)) {
										componentPropertyToName.put(prop, propName);
									}
									compPropSet.add(prop);
									break;
								}
								case "threadgroup": {
									if(objectNamesFromFilesInProject.contains(propName)) {
										componentPropertyToName.put(prop, propName);
									}
									compPropSet.add(prop);
									break;
								}
								case "virtualprocessor": {
									if(objectNamesFromFilesInProject.contains(propName)) {
										componentPropertyToName.put(prop, propName);
									}
									compPropSet.add(prop);
									break;
								}
								case "bus": {
									if(objectNamesFromFilesInProject.contains(propName)) {
										componentPropertyToName.put(prop, propName);
									}
									compPropSet.add(prop);
									break;
								}
								case "port": {
									if(objectNamesFromFilesInProject.contains(propName)) {
										componentPropertyToName.put(prop, propName);
									}
									compPropSet.add(prop);
									break;
								}
								default: {
									if(objectNamesFromFilesInProject.contains(((PropertySetImpl) obj).getName())) {
										System.out.println(
											"Warning: unsupported property: " + propName + ", applies to: " + propCat);
									}
									break;
								}
							}
						}
					}
				}
		} // end of extracting data from the AADLObject



		/* Translating all Component Types */
		if(systemTypes.size()>0) {
			model = translateSystemTypeObjects(systemTypes, model, dataTypeDecl);
		}
		if(busTypes.size()>0) {
			model = translateBusTypeObjects(busTypes, model, dataTypeDecl);
		}
		if(subprogramTypes.size()>0) {
			model = translateSubprogramTypeObjects(subprogramTypes, model, dataTypeDecl);
		}
		if(threadTypes.size()>0) {
			model = translateThreadTypeObjects(threadTypes, model, dataTypeDecl);
		}
		if(memoryTypes.size()>0) {
			model = translateMemoryTypeObjects(memoryTypes, model, dataTypeDecl);
		}
		if(deviceTypes.size()>0) {
			model = translateDeviceTypeObjects(deviceTypes, model, dataTypeDecl);
		}
		if(abstractTypes.size()>0) {
			model = translateAbstractTypeObjects(abstractTypes, model, dataTypeDecl);
		}
		if(processTypes.size()>0) {
			model = translateProcessTypeObjects(processTypes, model, dataTypeDecl);
		}
		if(processTypes.size()>0) {
			model = translateProcessorTypeObjects(processorTypes, model, dataTypeDecl);
		}
		if(threadGroupTypes.size()>0) {
			model = translateThreadGroupTypeObjects(threadGroupTypes, model, dataTypeDecl);
		}
		if(virtualProcessorTypes.size()>0) {
			model = translateVirtualProcessorTypeObjects(virtualProcessorTypes, model, dataTypeDecl);
		}


		/* Translating all System Implementations */
//		model = translateSystemImplObjects(systemImpls, componentPropertyToName, connPropertyToName,model);
//		model = translateComponentImplObjects(compImpls, componentPropertyToName, connPropertyToName,model);


		/** Translating all component implementations */
		model = translateComponentImplObjects(compImpls, componentPropertyToName, connPropertyToName,model, dataTypeDecl);



		//return the final model
		return model;
	}//End of populateVDMFromAadlObjects



	/**
	 * Analyzing each systemType:
	 * 1. Determine if it is a lower-level system or higher-level system
	 * 2. If lower-level, add to componentType list attribute of Model
	 * 	2.1 Populate the port, contract, cyberRel, safetyRel, event, id, compCategory
	 *      fields of componentType of the Model object
	 * 3. If higher-level, assign to Model
	 * 	3.1 Populate the safetyReq
	 *      cyberReq, mission fields of Model object
	 * @param systemTypes
	 * @param m1
	 * @return
	 */
	public Model translateSystemTypeObjects(List<SystemType> systemTypes, Model m1, HashSet<String> dataTypeDecl) {
		for(SystemType sysType : systemTypes) {
			// variables for unpacking sysType
			List<Event> events = new ArrayList<>();
			List<CyberMission> missionReqs = new ArrayList<>();
			List<CyberRel> cyberRels = new ArrayList<>();
			List<SafetyRel> safetyRels = new ArrayList<>();
			List<CyberReq> cyberReqs = new ArrayList<>();
			List<SafetyReq> safetyReqs = new ArrayList<>();

			//a flag to check if a higher -level component has already been found
			boolean higher_flag = false;

			// unpacking sysType
			for(AnnexSubclause annex : sysType.getOwnedAnnexSubclauses()) {
				if(annex.getName().equalsIgnoreCase("verdict")) {
					Verdict verdictAnnex = VerdictUtil.getVerdict(annex);

					for (Statement statement : verdictAnnex.getElements()) {
						if (statement instanceof Event) {
							events.add((Event)statement);
						} else if(statement instanceof CyberMission) {
							missionReqs.add((CyberMission)statement);
						} else if(statement instanceof CyberReq) {
							cyberReqs.add((CyberReq)statement);
						} else if(statement instanceof CyberRel) {
							cyberRels.add((CyberRel)statement);
						} else if(statement instanceof SafetyReq) {
							safetyReqs.add((SafetyReq)statement);
						} else if(statement instanceof SafetyRel) {
							safetyRels.add((SafetyRel)statement);
						}
					}
				}
			} // End of unpacking sysType


			/**
			 *  For every SystemType,
			 *  populate the id, name, compCateg, port, event,
			 *  cyberRel, and safetyRel fields of componentType
			 *  and add it to the list of componentType
			 *  of the Model object
			 * */
			if(true) { //No Filter-- do for all System Types

				//to pack the sysType as a VDM component
				verdict.vdm.vdm_model.ComponentType packComponent = new verdict.vdm.vdm_model.ComponentType();

				//Note: Not populating "contract" for now

//ISSUE: There is no getId() function for systemType
				packComponent.setId(sysType.getQualifiedName());

				//populating "name"
				packComponent.setName(sysType.getName());


				//populating "compCateg"
				packComponent.setCompCateg(sysType.getCategory().getName());

				
				//get all bus accesses and store them as ports
				List<BusAccess> busAccesses = sysType.getOwnedBusAccesses();
				
				//checking each busAccess's details and adding it to the port list
				for(BusAccess busAccess : busAccesses) {

					String portName = busAccess.getName();
					String modeString = "in";
					if(busAccess.getKind() == AccessType.PROVIDES) {
						modeString = "providesBusAccess";
					}
					else if(busAccess.getKind() == AccessType.REQUIRES) {
						modeString = "requiresBusAccess";
					}

					//fetching data type information
			    	verdict.vdm.vdm_model.Port newPort = createVdmPort(portName, modeString, busAccess.getQualifiedName());

			    	//Note: Not populating "type" for now

//ISSUE: "probe", "event", and "id" not found in DataPort class or superclass

			    	//add to port list of component
			    	packComponent.getPort().add(newPort);
				}//End of checking each busAccess
				
				
				//get all data accesses and store them as ports
				List<DataAccess> dataAccesses = sysType.getOwnedDataAccesses();
				
				//checking each dataAccess's details and adding it to the port list
				for(DataAccess dataAccess : dataAccesses) {

					String portName = dataAccess.getName();
					String modeString = "in";
					if(dataAccess.getKind() == AccessType.PROVIDES) {
						modeString = "providesDataAccess";
					}
					else if(dataAccess.getKind() == AccessType.REQUIRES) {
						modeString = "requiresDataAccess";
					}


			    	verdict.vdm.vdm_model.Port newPort = createVdmPort(portName, modeString, dataAccess.getQualifiedName());

			    	//Note: Not populating "type" for now

//ISSUE: "probe", "event", and "id" not found in DataPort class or superclass

			    	//add to port list of component
			    	packComponent.getPort().add(newPort);
				}//End of checking each dataAccess
				
				//get all ports
				List<DataPort> dataPorts = sysType.getOwnedDataPorts();				

				//checking each port's mode and name and adding it to the port list
				for(DataPort dataPort : dataPorts) {

					verdict.vdm.vdm_model.Port newPort = createVdmPort(dataPort, m1, dataTypeDecl);

//ISSUE: "probe", "event", and "id" not found in DataPort class or superclass


			    	//add to port list of component
			    	packComponent.getPort().add(newPort);
				}//End of checking each port

				//get all event data ports
				List<EventDataPort> eventDataPorts = sysType.getOwnedEventDataPorts();
				for(EventDataPort eventDataPort:eventDataPorts) {
					verdict.vdm.vdm_model.Port newPort = createVdmPort(eventDataPort, m1, dataTypeDecl);
					//add to port list of component
			    	packComponent.getPort().add(newPort);
				}
				//get all event ports
				List<EventPort> eventPorts = sysType.getOwnedEventPorts();
				for(EventPort eventPort:eventPorts) {
					verdict.vdm.vdm_model.Port newPort = createVdmEventPort(eventPort);
					//add to port list of component
			    	packComponent.getPort().add(newPort);
				}

				//packing all events and adding to component
				for(Event anEvent : events) {
					//To pack the event as a VDM event
					verdict.vdm.vdm_model.Event packEvent = createVdmEvent(anEvent);

					//adding to the list of component's events
					packComponent.getEvent().add(packEvent);
				}//End of packing all events


				//packing all cyberRels and adding to component
				for(CyberRel aCyberRel : cyberRels) {
					//To pack the cyberRel as a VDM event
					verdict.vdm.vdm_model.CyberRel packCyberRel =createVdmCyberRel(aCyberRel);

					//adding to the list of component's Cyber relations
					packComponent.getCyberRel().add(packCyberRel);
				}//End of packing all cyberRels


				//packing all safetyRels and adding to component
				for(SafetyRel aSafetyRel : safetyRels) {
					//To pack the safetyRel as a VDM event
					verdict.vdm.vdm_model.SafetyRel packSafetyRel = createVdmSafetyRel(aSafetyRel);

					//adding to the list of component's Safety relations
					packComponent.getSafetyRel().add(packSafetyRel);
				}// End of packing all safetyRels


				//adding to the list of componentTypes of the Model object
				m1.getComponentType().add(packComponent);
			}//End of populate the id ...

			/** If a high-level system
			 *  populate the name, safetyReq, cyberReq, and mission
			 *  for the model object
			 * */
			if(!cyberReqs.isEmpty() || !safetyReqs.isEmpty() || !missionReqs.isEmpty()) {
				//checking if a high-level system has already been found
				if (higher_flag == false) {
					higher_flag = true;
				} else {
					System.out.println("Warning: Multiple high-level systems detected!");
				}

				//populating name
				m1.setName(sysType.getName());

				//packing all safetyReqs and adding to model
				for(SafetyReq aSafetyReq : safetyReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.SafetyReq packSafetyReq = createVdmSafetyReq(aSafetyReq,
							sysType.getFullName());

					//adding to the list of model's Safety requirements
					m1.getSafetyReq().add(packSafetyReq);

				}// End of packing all safetyReqs


				//packing all cyberReqs and adding to model
				for(CyberReq aCyberReq : cyberReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.CyberReq packCyberReq = createVdmCyberReq(aCyberReq, sysType.getFullName());

					//adding to the list of model's Cyber requirements
					m1.getCyberReq().add(packCyberReq);

				}// End of packing all cyberReqs


				//packing all missionReqs and adding to model
				for(CyberMission aMission : missionReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.Mission packMission = createVdmMission(aMission);

					//adding to the list of model's Mission
					m1.getMission().add(packMission);
				}// End of packing all missionReqs
			}//End of if a higher-level system
		}  //End of Analyzing each systemType

		//returning the populated Model
		return m1;
	}//End of translateSystemTypeObjects


	/**
	 * Analyzing each busType:
	 * 1. Determine if it is a lower-level system or higher-level system
	 * 2. If lower-level, add to componentType list attribute of Model
	 * 	2.1 Populate the port, contract, cyberRel, safetyRel, event, id, compCategory
	 *      fields of componentType of the Model object
	 * 3. If higher-level, assign to Model
	 * 	3.1 Populate the safetyReq
	 *      cyberReq, mission fields of Model object
	 * @param busTypes
	 * @param m1
	 * @return
	 */
	public Model translateBusTypeObjects(List<BusType> busTypes, Model m1, HashSet<String> dataTypeDecl) {
		for(BusType bType : busTypes) {

			// variables for unpacking bType
			List<Event> events = new ArrayList<>();
			List<CyberMission> missionReqs = new ArrayList<>();
			List<CyberRel> cyberRels = new ArrayList<>();
			List<SafetyRel> safetyRels = new ArrayList<>();
			List<CyberReq> cyberReqs = new ArrayList<>();
			List<SafetyReq> safetyReqs = new ArrayList<>();

			//a flag to check if a higher -level component has already been found
			boolean higher_flag = false;

			// unpacking bType
			for(AnnexSubclause annex : bType.getOwnedAnnexSubclauses()) {
				if(annex.getName().equalsIgnoreCase("verdict")) {
					Verdict verdictAnnex = VerdictUtil.getVerdict(annex);

					for (Statement statement : verdictAnnex.getElements()) {
						if (statement instanceof Event) {
							events.add((Event)statement);
						} else if(statement instanceof CyberMission) {
							missionReqs.add((CyberMission)statement);
						} else if(statement instanceof CyberReq) {
							cyberReqs.add((CyberReq)statement);
						} else if(statement instanceof CyberRel) {
							cyberRels.add((CyberRel)statement);
						} else if(statement instanceof SafetyReq) {
							safetyReqs.add((SafetyReq)statement);
						} else if(statement instanceof SafetyRel) {
							safetyRels.add((SafetyRel)statement);
						}
					}
				}
			} // End of unpacking bType


			/**
			 *  For every BusType,
			 *  populate the id, name, compCateg, port, event,
			 *  cyberRel, and safetyRel fields of componentType
			 *  and add it to the list of componentType
			 *  of the Model object
			 * */
			if(true) { //No Filter-- do for all System Types

				//to pack the bType as a VDM component
				verdict.vdm.vdm_model.ComponentType packComponent = new verdict.vdm.vdm_model.ComponentType();

				//Note: Not populating "contract" for now

//ISSUE: There is no getId() function for busType
				packComponent.setId(bType.getQualifiedName());

				//populating "name"
				packComponent.setName(bType.getName());


				//populating "compCateg"
				packComponent.setCompCateg(bType.getCategory().getName());

				//get all bus accesses and store them as ports
				List<BusAccess> busAccesses = bType.getOwnedBusAccesses();
				
				//checking each busAccess's details and adding it to the port list
				for(BusAccess busAccess : busAccesses) {

					String portName = busAccess.getName();
					String modeString = "in";
					if(busAccess.getKind() == AccessType.PROVIDES) {
						modeString = "providesBusAccess";
					}
					else if(busAccess.getKind() == AccessType.REQUIRES) {
						modeString = "requiresBusAccess";
					}


			    	verdict.vdm.vdm_model.Port newPort = createVdmPort(portName, modeString, busAccess.getQualifiedName());

			    	//Note: Not populating "type" for now

//ISSUE: "probe", "event", and "id" not found in DataPort class or superclass

			    	//add to port list of component
			    	packComponent.getPort().add(newPort);
				}//End of checking each busAccess
				
				
//ISSUE: no getOwnedDataAccesses for busType
				
				//get all ports
				List<DataPort> dataPorts = bType.getOwnedDataPorts();

				//checking each port's mode and name and adding it to the port list
				for(DataPort dataPort : dataPorts) {

					verdict.vdm.vdm_model.Port newPort = createVdmPort(dataPort, m1, dataTypeDecl);

//ISSUE: "probe", "event", and "id" not found in DataPort class or superclass


			    	//add to port list of component
			    	packComponent.getPort().add(newPort);
				}//End of checking each port

				//get all event data ports
				List<EventDataPort> eventDataPorts = bType.getOwnedEventDataPorts();
				for(EventDataPort eventDataPort:eventDataPorts) {
					verdict.vdm.vdm_model.Port newPort = createVdmPort(eventDataPort, m1, dataTypeDecl);
					//add to port list of component
			    	packComponent.getPort().add(newPort);
				}
				//get all event ports
				List<EventPort> eventPorts = bType.getOwnedEventPorts();
				for(EventPort eventPort:eventPorts) {
					verdict.vdm.vdm_model.Port newPort = createVdmEventPort(eventPort);
					//add to port list of component
			    	packComponent.getPort().add(newPort);
				}
				//packing all events and adding to component
				for(Event anEvent : events) {
					//To pack the event as a VDM event
					verdict.vdm.vdm_model.Event packEvent = createVdmEvent(anEvent);

					//adding to the list of component's events
					packComponent.getEvent().add(packEvent);
				}//End of packing all events


				//packing all cyberRels and adding to component
				for(CyberRel aCyberRel : cyberRels) {
					//To pack the cyberRel as a VDM event
					verdict.vdm.vdm_model.CyberRel packCyberRel = createVdmCyberRel(aCyberRel);

					//adding to the list of component's Cyber relations
					packComponent.getCyberRel().add(packCyberRel);
				}//End of packing all cyberRels


				//packing all safetyRels and adding to component
				for(SafetyRel aSafetyRel : safetyRels) {
					//To pack the safetyRel as a VDM event
					verdict.vdm.vdm_model.SafetyRel packSafetyRel = createVdmSafetyRel(aSafetyRel);

					//adding to the list of component's Safety relations
					packComponent.getSafetyRel().add(packSafetyRel);
				}// End of packing all safetyRels


				//adding to the list of componentTypes of the Model object
				m1.getComponentType().add(packComponent);
			}//End of populate the id ...

			/** If a high-level system
			 *  populate the name, safetyReq, cyberReq, and mission
			 *  for the model object
			 * */
			if(!cyberReqs.isEmpty() || !safetyReqs.isEmpty() || !missionReqs.isEmpty()) {
				//checking if a high-level system has already been found
				if (higher_flag == false) {
					higher_flag = true;
				} else {
					System.out.println("Warning: Multiple high-level systems detected!");
				}

				//populating name
				m1.setName(bType.getName());

				//packing all safetyReqs and adding to model
				for(SafetyReq aSafetyReq : safetyReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.SafetyReq packSafetyReq = createVdmSafetyReq(aSafetyReq,
							bType.getFullName());

					//adding to the list of model's Safety requirements
					m1.getSafetyReq().add(packSafetyReq);

				}// End of packing all safetyReqs


				//packing all cyberReqs and adding to model
				for(CyberReq aCyberReq : cyberReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.CyberReq packCyberReq = createVdmCyberReq(aCyberReq, bType.getFullName());

					//adding to the list of model's Cyber requirements
					m1.getCyberReq().add(packCyberReq);

				}// End of packing all cyberReqs


				//packing all missionReqs and adding to model
				for(CyberMission aMission : missionReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.Mission packMission = createVdmMission(aMission);

					//adding to the list of model's Mission
					m1.getMission().add(packMission);
				}// End of packing all missionReqs
			}//End of if a higher-level system
		}  //End of Analyzing each busType

		//returning the populated Model
		return m1;
	}//End of translateBusTypeObjects


	/**
	 * Analyzing each subprogramType:
	 * 1. Determine if it is a lower-level system or higher-level system
	 * 2. If lower-level, add to componentType list attribute of Model
	 * 	2.1 Populate the port, contract, cyberRel, safetyRel, event, id, compCategory
	 *      fields of componentType of the Model object
	 * 3. If higher-level, assign to Model
	 * 	3.1 Populate the safetyReq
	 *      cyberReq, mission fields of Model object
	 * @param subprogramTypes
	 * @param m1
	 * @return
	 */
	public Model translateSubprogramTypeObjects(List<SubprogramType> subprogramTypes, Model m1, HashSet<String> dataTypeDecl) {
		for(SubprogramType subprogType : subprogramTypes) {

			// variables for unpacking subprogType
			List<Event> events = new ArrayList<>();
			List<CyberMission> missionReqs = new ArrayList<>();
			List<CyberRel> cyberRels = new ArrayList<>();
			List<SafetyRel> safetyRels = new ArrayList<>();
			List<CyberReq> cyberReqs = new ArrayList<>();
			List<SafetyReq> safetyReqs = new ArrayList<>();

			//a flag to check if a higher -level component has already been found
			boolean higher_flag = false;

			// unpacking subprogType
			for(AnnexSubclause annex : subprogType.getOwnedAnnexSubclauses()) {
				if(annex.getName().equalsIgnoreCase("verdict")) {
					Verdict verdictAnnex = VerdictUtil.getVerdict(annex);

					for (Statement statement : verdictAnnex.getElements()) {
						if (statement instanceof Event) {
							events.add((Event)statement);
						} else if(statement instanceof CyberMission) {
							missionReqs.add((CyberMission)statement);
						} else if(statement instanceof CyberReq) {
							cyberReqs.add((CyberReq)statement);
						} else if(statement instanceof CyberRel) {
							cyberRels.add((CyberRel)statement);
						} else if(statement instanceof SafetyReq) {
							safetyReqs.add((SafetyReq)statement);
						} else if(statement instanceof SafetyRel) {
							safetyRels.add((SafetyRel)statement);
						}
					}
				}
			} // End of unpacking subprogType


			/**
			 *  For every SubprogramType,
			 *  populate the id, name, compCateg, port, event,
			 *  cyberRel, and safetyRel fields of componentType
			 *  and add it to the list of componentType
			 *  of the Model object
			 * */
			if(true) { //No Filter-- do for all System Types

				//to pack the subprogType as a VDM component
				verdict.vdm.vdm_model.ComponentType packComponent = new verdict.vdm.vdm_model.ComponentType();

				//Note: Not populating "contract" for now

//ISSUE: There is no getId() function for subprogramType
				packComponent.setId(subprogType.getQualifiedName());

				//populating "name"
				packComponent.setName(subprogType.getName());


				//populating "compCateg"
				packComponent.setCompCateg(subprogType.getCategory().getName());
				
				
				//get all data accesses and store them as ports
				List<DataAccess> dataAccesses = subprogType.getOwnedDataAccesses();
				
//ISSUE: no getOwnedBusAccesses
				
				
				//checking each dataAccess's details and adding it to the port list
				for(DataAccess dataAccess : dataAccesses) {

					String portName = dataAccess.getName();
					String modeString = "in";
					if(dataAccess.getKind() == AccessType.PROVIDES) {
						modeString = "providesDataAccess";
					}
					else if(dataAccess.getKind() == AccessType.REQUIRES) {
						modeString = "requiresDataAccess";
					}


			    	verdict.vdm.vdm_model.Port newPort = createVdmPort(portName, modeString, dataAccess.getQualifiedName());

			    	//Note: Not populating "type" for now

//ISSUE: "probe", "event", and "id" not found in DataPort class or superclass

			    	//add to port list of component
			    	packComponent.getPort().add(newPort);
				}//End of checking each dataAccess

//ISSUE: No getOwneddataPOrts for SubProgramType
//				//get all ports
//				List<DataPort> dataPorts = subprogType.getOwnedDataPorts();


				//get all event data ports
				List<EventDataPort> eventDataPorts = subprogType.getOwnedEventDataPorts();
				for(EventDataPort eventDataPort:eventDataPorts) {
					verdict.vdm.vdm_model.Port newPort = createVdmPort(eventDataPort, m1, dataTypeDecl);
					//add to port list of component
			    	packComponent.getPort().add(newPort);
				}
				//get all event ports
				List<EventPort> eventPorts = subprogType.getOwnedEventPorts();
				for(EventPort eventPort:eventPorts) {
					verdict.vdm.vdm_model.Port newPort = createVdmEventPort(eventPort);
					//add to port list of component
			    	packComponent.getPort().add(newPort);
				}

				//packing all events and adding to component
				for(Event anEvent : events) {
					//To pack the event as a VDM event
					verdict.vdm.vdm_model.Event packEvent = createVdmEvent(anEvent);

					//adding to the list of component's events
					packComponent.getEvent().add(packEvent);
				}//End of packing all events


				//packing all cyberRels and adding to component
				for(CyberRel aCyberRel : cyberRels) {
					//To pack the cyberRel as a VDM event
					verdict.vdm.vdm_model.CyberRel packCyberRel = createVdmCyberRel(aCyberRel);
					//adding to the list of component's Cyber relations
					packComponent.getCyberRel().add(packCyberRel);
				}//End of packing all cyberRels


				//packing all safetyRels and adding to component
				for(SafetyRel aSafetyRel : safetyRels) {
					//To pack the safetyRel as a VDM event
					verdict.vdm.vdm_model.SafetyRel packSafetyRel = createVdmSafetyRel(aSafetyRel);

					//adding to the list of component's Safety relations
					packComponent.getSafetyRel().add(packSafetyRel);
				}// End of packing all safetyRels


				//adding to the list of componentTypes of the Model object
				m1.getComponentType().add(packComponent);
			}//End of populate the id ...

			/** If a high-level system
			 *  populate the name, safetyReq, cyberReq, and mission
			 *  for the model object
			 * */
			if(!cyberReqs.isEmpty() || !safetyReqs.isEmpty() || !missionReqs.isEmpty()) {
				//checking if a high-level system has already been found
				if (higher_flag == false) {
					higher_flag = true;
				} else {
					System.out.println("Warning: Multiple high-level systems detected!");
				}

				//populating name
				m1.setName(subprogType.getName());

				//packing all safetyReqs and adding to model
				for(SafetyReq aSafetyReq : safetyReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.SafetyReq packSafetyReq = createVdmSafetyReq(aSafetyReq,
							subprogType.getFullName());

					//adding to the list of model's Safety requirements
					m1.getSafetyReq().add(packSafetyReq);

				}// End of packing all safetyReqs


				//packing all cyberReqs and adding to model
				for(CyberReq aCyberReq : cyberReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.CyberReq packCyberReq = createVdmCyberReq(aCyberReq,
							subprogType.getFullName());

					//adding to the list of model's Cyber requirements
					m1.getCyberReq().add(packCyberReq);

				}// End of packing all cyberReqs


				//packing all missionReqs and adding to model
				for(CyberMission aMission : missionReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.Mission packMission = createVdmMission(aMission);

					//adding to the list of model's Mission
					m1.getMission().add(packMission);
				}// End of packing all missionReqs
			}//End of if a higher-level system
		}  //End of Analyzing each subprogramType

		//returning the populated Model
		return m1;
	}//End of translateSubprogramTypeObjects


	/**
	 * Analyzing each threadType:
	 * 1. Determine if it is a lower-level system or higher-level system
	 * 2. If lower-level, add to componentType list attribute of Model
	 * 	2.1 Populate the port, contract, cyberRel, safetyRel, event, id, compCategory
	 *      fields of componentType of the Model object
	 * 3. If higher-level, assign to Model
	 * 	3.1 Populate the safetyReq
	 *      cyberReq, mission fields of Model object
	 * @param threadTypes
	 * @param m1
	 * @return
	 */
	public Model translateThreadTypeObjects(List<ThreadType> threadTypes, Model m1, HashSet<String> dataTypeDecl) {
		for(ThreadType tType : threadTypes) {

			// variables for unpacking tType
			List<Event> events = new ArrayList<>();
			List<CyberMission> missionReqs = new ArrayList<>();
			List<CyberRel> cyberRels = new ArrayList<>();
			List<SafetyRel> safetyRels = new ArrayList<>();
			List<CyberReq> cyberReqs = new ArrayList<>();
			List<SafetyReq> safetyReqs = new ArrayList<>();

			//a flag to check if a higher -level component has already been found
			boolean higher_flag = false;

			// unpacking tType
			for(AnnexSubclause annex : tType.getOwnedAnnexSubclauses()) {
				if(annex.getName().equalsIgnoreCase("verdict")) {
					Verdict verdictAnnex = VerdictUtil.getVerdict(annex);

					for (Statement statement : verdictAnnex.getElements()) {
						if (statement instanceof Event) {
							events.add((Event)statement);
						} else if(statement instanceof CyberMission) {
							missionReqs.add((CyberMission)statement);
						} else if(statement instanceof CyberReq) {
							cyberReqs.add((CyberReq)statement);
						} else if(statement instanceof CyberRel) {
							cyberRels.add((CyberRel)statement);
						} else if(statement instanceof SafetyReq) {
							safetyReqs.add((SafetyReq)statement);
						} else if(statement instanceof SafetyRel) {
							safetyRels.add((SafetyRel)statement);
						}
					}
				}
			} // End of unpacking tType


			/**
			 *  For every ThreadType,
			 *  populate the id, name, compCateg, port, event,
			 *  cyberRel, and safetyRel fields of componentType
			 *  and add it to the list of componentType
			 *  of the Model object
			 * */
			if(true) { //No Filter-- do for all System Types

				//to pack the tType as a VDM component
				verdict.vdm.vdm_model.ComponentType packComponent = new verdict.vdm.vdm_model.ComponentType();

				//Note: Not populating "contract" for now

//ISSUE: There is no getId() function for threadType
				packComponent.setId(tType.getQualifiedName());

				//populating "name"
				packComponent.setName(tType.getName());


				//populating "compCateg"
				packComponent.setCompCateg(tType.getCategory().getName());

//ISSUE: no getOwnedBusAccesses
				
				//get all data accesses and store them as ports
				List<DataAccess> dataAccesses = tType.getOwnedDataAccesses();
				
				//checking each dataAccess's details and adding it to the port list
				for(DataAccess dataAccess : dataAccesses) {

					String portName = dataAccess.getName();
					String modeString = "in";
					if(dataAccess.getKind() == AccessType.PROVIDES) {
						modeString = "providesDataAccess";
					}
					else if(dataAccess.getKind() == AccessType.REQUIRES) {
						modeString = "requiresDataAccess";
					}


			    	verdict.vdm.vdm_model.Port newPort = createVdmPort(portName, modeString, dataAccess.getQualifiedName());

			    	//Note: Not populating "type" for now

//ISSUE: "probe", "event", and "id" not found in DataPort class or superclass

			    	//add to port list of component
			    	packComponent.getPort().add(newPort);
				}//End of checking each dataAccess
				
				//get all ports
				List<DataPort> dataPorts = tType.getOwnedDataPorts();

				//checking each port's mode and name and adding it to the port list
				for(DataPort dataPort : dataPorts) {

					verdict.vdm.vdm_model.Port newPort = createVdmPort(dataPort, m1, dataTypeDecl);

//ISSUE: "probe", "event", and "id" not found in DataPort class or superclass


			    	//add to port list of component
			    	packComponent.getPort().add(newPort);
				}//End of checking each port

				//get all event data ports
				List<EventDataPort> eventDataPorts = tType.getOwnedEventDataPorts();
				for(EventDataPort eventDataPort:eventDataPorts) {
					verdict.vdm.vdm_model.Port newPort = createVdmPort(eventDataPort, m1, dataTypeDecl);
					//add to port list of component
			    	packComponent.getPort().add(newPort);
				}
				//get all event ports
				List<EventPort> eventPorts = tType.getOwnedEventPorts();
				for(EventPort eventPort:eventPorts) {
					verdict.vdm.vdm_model.Port newPort = createVdmEventPort(eventPort);
					//add to port list of component
			    	packComponent.getPort().add(newPort);
				}
				
				//packing all events and adding to component
				for(Event anEvent : events) {
					//To pack the event as a VDM event
					verdict.vdm.vdm_model.Event packEvent = createVdmEvent(anEvent);

					//adding to the list of component's events
					packComponent.getEvent().add(packEvent);
				}//End of packing all events


				//packing all cyberRels and adding to component
				for(CyberRel aCyberRel : cyberRels) {
					//To pack the cyberRel as a VDM event
					verdict.vdm.vdm_model.CyberRel packCyberRel = createVdmCyberRel(aCyberRel);


					//adding to the list of component's Cyber relations
					packComponent.getCyberRel().add(packCyberRel);
				}//End of packing all cyberRels


				//packing all safetyRels and adding to component
				for(SafetyRel aSafetyRel : safetyRels) {
					//To pack the safetyRel as a VDM event
					verdict.vdm.vdm_model.SafetyRel packSafetyRel = createVdmSafetyRel(aSafetyRel);

					//adding to the list of component's Safety relations
					packComponent.getSafetyRel().add(packSafetyRel);
				}// End of packing all safetyRels


				//adding to the list of componentTypes of the Model object
				m1.getComponentType().add(packComponent);
			}//End of populate the id ...

			/** If a high-level system
			 *  populate the name, safetyReq, cyberReq, and mission
			 *  for the model object
			 * */
			if(!cyberReqs.isEmpty() || !safetyReqs.isEmpty() || !missionReqs.isEmpty()) {
				//checking if a high-level system has already been found
				if (higher_flag == false) {
					higher_flag = true;
				} else {
					System.out.println("Warning: Multiple high-level systems detected!");
				}

				//populating name
				m1.setName(tType.getName());

				//packing all safetyReqs and adding to model
				for(SafetyReq aSafetyReq : safetyReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.SafetyReq packSafetyReq = createVdmSafetyReq(aSafetyReq, tType.getFullName());

					//adding to the list of model's Safety requirements
					m1.getSafetyReq().add(packSafetyReq);

				}// End of packing all safetyReqs


				//packing all cyberReqs and adding to model
				for(CyberReq aCyberReq : cyberReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.CyberReq packCyberReq = createVdmCyberReq(aCyberReq, tType.getFullName());

					//adding to the list of model's Cyber requirements
					m1.getCyberReq().add(packCyberReq);

				}// End of packing all cyberReqs


				//packing all missionReqs and adding to model
				for(CyberMission aMission : missionReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.Mission packMission = createVdmMission(aMission);


					//adding to the list of model's Mission
					m1.getMission().add(packMission);
				}// End of packing all missionReqs
			}//End of if a higher-level system
		}  //End of Analyzing each threadType

		//returning the populated Model
		return m1;
	}//End of translateThreadTypeObjects


	/**
	 * Analyzing each memoryType:
	 * 1. Determine if it is a lower-level system or higher-level system
	 * 2. If lower-level, add to componenmemType list attribute of Model
	 * 	2.1 Populate the port, contract, cyberRel, safetyRel, event, id, compCategory
	 *      fields of componenmemType of the Model object
	 * 3. If higher-level, assign to Model
	 * 	3.1 Populate the safetyReq
	 *      cyberReq, mission fields of Model object
	 * @param memoryTypes
	 * @param m1
	 * @return
	 */
	public Model translateMemoryTypeObjects(List<MemoryType> memoryTypes, Model m1, HashSet<String> dataTypeDecl) {
		for(MemoryType memType : memoryTypes) {

			// variables for unpacking memType
			List<Event> events = new ArrayList<>();
			List<CyberMission> missionReqs = new ArrayList<>();
			List<CyberRel> cyberRels = new ArrayList<>();
			List<SafetyRel> safetyRels = new ArrayList<>();
			List<CyberReq> cyberReqs = new ArrayList<>();
			List<SafetyReq> safetyReqs = new ArrayList<>();

			//a flag to check if a higher -level component has already been found
			boolean higher_flag = false;

			// unpacking memType
			for(AnnexSubclause annex : memType.getOwnedAnnexSubclauses()) {
				if(annex.getName().equalsIgnoreCase("verdict")) {
					Verdict verdictAnnex = VerdictUtil.getVerdict(annex);

					for (Statement statement : verdictAnnex.getElements()) {
						if (statement instanceof Event) {
							events.add((Event)statement);
						} else if(statement instanceof CyberMission) {
							missionReqs.add((CyberMission)statement);
						} else if(statement instanceof CyberReq) {
							cyberReqs.add((CyberReq)statement);
						} else if(statement instanceof CyberRel) {
							cyberRels.add((CyberRel)statement);
						} else if(statement instanceof SafetyReq) {
							safetyReqs.add((SafetyReq)statement);
						} else if(statement instanceof SafetyRel) {
							safetyRels.add((SafetyRel)statement);
						}
					}
				}
			} // End of unpacking memType


			/**
			 *  For every MemoryType,
			 *  populate the id, name, compCateg, port, event,
			 *  cyberRel, and safetyRel fields of componenmemType
			 *  and add it to the list of componenmemType
			 *  of the Model object
			 * */
			if(true) { //No Filter-- do for all System Types

				//to pack the memType as a VDM component
				verdict.vdm.vdm_model.ComponentType packComponent = new verdict.vdm.vdm_model.ComponentType();

				//Note: Not populating "contract" for now

//ISSUE: There is no getId() function for memoryType
				packComponent.setId(memType.getQualifiedName());

				//populating "name"
				packComponent.setName(memType.getName());


				//populating "compCateg"
				packComponent.setCompCateg(memType.getCategory().getName());

				//get all bus accesses and store them as ports
				List<BusAccess> busAccesses = memType.getOwnedBusAccesses();
				
				//checking each busAccess's details and adding it to the port list
				for(BusAccess busAccess : busAccesses) {

					String portName = busAccess.getName();
					String modeString = "in";
					if(busAccess.getKind() == AccessType.PROVIDES) {
						modeString = "providesBusAccess";
					}
					else if(busAccess.getKind() == AccessType.REQUIRES) {
						modeString = "requiresBusAccess";
					}


			    	verdict.vdm.vdm_model.Port newPort = createVdmPort(portName, modeString, busAccess.getQualifiedName());

			    	//Note: Not populating "type" for now

//ISSUE: "probe", "event", and "id" not found in DataPort class or superclass

			    	//add to port list of component
			    	packComponent.getPort().add(newPort);
				}//End of checking each busAccess
				
				
//ISSUE: no getOwnedDataAccesses for memoryTypes
				
				//get all ports
				List<DataPort> dataPorts = memType.getOwnedDataPorts();

				//checking each port's mode and name and adding it to the port list
				for(DataPort dataPort : dataPorts) {

					verdict.vdm.vdm_model.Port newPort = createVdmPort(dataPort, m1, dataTypeDecl);

			    	//Note: Not populating "type" for now

//ISSUE: "probe", "event", and "id" not found in DataPort class or superclass


			    	//add to port list of component
			    	packComponent.getPort().add(newPort);
				}//End of checking each port

				//get all event data ports
				List<EventDataPort> eventDataPorts = memType.getOwnedEventDataPorts();
				for(EventDataPort eventDataPort:eventDataPorts) {
					verdict.vdm.vdm_model.Port newPort = createVdmPort(eventDataPort, m1, dataTypeDecl);
					//add to port list of component
			    	packComponent.getPort().add(newPort);
				}
				//get all event ports
				List<EventPort> eventPorts = memType.getOwnedEventPorts();
				for(EventPort eventPort:eventPorts) {
					verdict.vdm.vdm_model.Port newPort = createVdmEventPort(eventPort);
					//add to port list of component
			    	packComponent.getPort().add(newPort);
				}
				//packing all events and adding to component
				for(Event anEvent : events) {
					//To pack the event as a VDM event
					verdict.vdm.vdm_model.Event packEvent = createVdmEvent(anEvent);

					//adding to the list of component's events
					packComponent.getEvent().add(packEvent);
				}//End of packing all events


				//packing all cyberRels and adding to component
				for(CyberRel aCyberRel : cyberRels) {
					//To pack the cyberRel as a VDM event
					verdict.vdm.vdm_model.CyberRel packCyberRel = createVdmCyberRel(aCyberRel);


					//adding to the list of component's Cyber relations
					packComponent.getCyberRel().add(packCyberRel);
				}//End of packing all cyberRels


				//packing all safetyRels and adding to component
				for(SafetyRel aSafetyRel : safetyRels) {
					//To pack the safetyRel as a VDM event
					verdict.vdm.vdm_model.SafetyRel packSafetyRel = createVdmSafetyRel(aSafetyRel);

					//adding to the list of component's Safety relations
					packComponent.getSafetyRel().add(packSafetyRel);
				}// End of packing all safetyRels


				//adding to the list of componenmemTypes of the Model object
				m1.getComponentType().add(packComponent);
			}//End of populate the id ...

			/** If a high-level system
			 *  populate the name, safetyReq, cyberReq, and mission
			 *  for the model object
			 * */
			if(!cyberReqs.isEmpty() || !safetyReqs.isEmpty() || !missionReqs.isEmpty()) {
				//checking if a high-level system has already been found
				if (higher_flag == false) {
					higher_flag = true;
				} else {
					System.out.println("Warning: Multiple high-level systems detected!");
				}

				//populating name
				m1.setName(memType.getName());

				//packing all safetyReqs and adding to model
				for(SafetyReq aSafetyReq : safetyReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.SafetyReq packSafetyReq = createVdmSafetyReq(aSafetyReq,
							memType.getFullName());

					//adding to the list of model's Safety requirements
					m1.getSafetyReq().add(packSafetyReq);

				}// End of packing all safetyReqs


				//packing all cyberReqs and adding to model
				for(CyberReq aCyberReq : cyberReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.CyberReq packCyberReq = createVdmCyberReq(aCyberReq, memType.getFullName());

					//adding to the list of model's Cyber requirements
					m1.getCyberReq().add(packCyberReq);

				}// End of packing all cyberReqs


				//packing all missionReqs and adding to model
				for(CyberMission aMission : missionReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.Mission packMission = createVdmMission(aMission);

					//adding to the list of model's Mission
					m1.getMission().add(packMission);
				}// End of packing all missionReqs
			}//End of if a higher-level system
		}  //End of Analyzing each memoryType

		//returning the populated Model
		return m1;
	}//End of translateMemoryTypeObjects


	/**
	 * Analyzing each deviceType:
	 * 1. Determine if it is a lower-level system or higher-level system
	 * 2. If lower-level, add to componentType list attribute of Model
	 * 	2.1 Populate the port, contract, cyberRel, safetyRel, event, id, compCategory
	 *      fields of componentType of the Model object
	 * 3. If higher-level, assign to Model
	 * 	3.1 Populate the safetyReq
	 *      cyberReq, mission fields of Model object
	 * @param deviceTypes
	 * @param m1
	 * @return
	 */
	public Model translateDeviceTypeObjects(List<DeviceType> deviceTypes, Model m1, HashSet<String> dataTypeDecl) {
		for(DeviceType devType : deviceTypes) {

			// variables for unpacking devType
			List<Event> events = new ArrayList<>();
			List<CyberMission> missionReqs = new ArrayList<>();
			List<CyberRel> cyberRels = new ArrayList<>();
			List<SafetyRel> safetyRels = new ArrayList<>();
			List<CyberReq> cyberReqs = new ArrayList<>();
			List<SafetyReq> safetyReqs = new ArrayList<>();

			//a flag to check if a higher -level component has already been found
			boolean higher_flag = false;

			// unpacking devType
			for(AnnexSubclause annex : devType.getOwnedAnnexSubclauses()) {
				if(annex.getName().equalsIgnoreCase("verdict")) {
					Verdict verdictAnnex = VerdictUtil.getVerdict(annex);

					for (Statement statement : verdictAnnex.getElements()) {
						if (statement instanceof Event) {
							events.add((Event)statement);
						} else if(statement instanceof CyberMission) {
							missionReqs.add((CyberMission)statement);
						} else if(statement instanceof CyberReq) {
							cyberReqs.add((CyberReq)statement);
						} else if(statement instanceof CyberRel) {
							cyberRels.add((CyberRel)statement);
						} else if(statement instanceof SafetyReq) {
							safetyReqs.add((SafetyReq)statement);
						} else if(statement instanceof SafetyRel) {
							safetyRels.add((SafetyRel)statement);
						}
					}
				}
			} // End of unpacking devType


			/**
			 *  For every DeviceType,
			 *  populate the id, name, compCateg, port, event,
			 *  cyberRel, and safetyRel fields of componentType
			 *  and add it to the list of componentType
			 *  of the Model object
			 * */
			if(true) { //No Filter-- do for all System Types

				//to pack the memType as a VDM component
				verdict.vdm.vdm_model.ComponentType packComponent = new verdict.vdm.vdm_model.ComponentType();

				//Note: Not populating "contract" for now

//ISSUE: There is no getId() function for memoryType
				packComponent.setId(devType.getQualifiedName());

				//populating "name"
				packComponent.setName(devType.getName());


				//populating "compCateg"
				packComponent.setCompCateg(devType.getCategory().getName());

				//get all bus accesses and store them as ports
				List<BusAccess> busAccesses = devType.getOwnedBusAccesses();
				
				//checking each busAccess's details and adding it to the port list
				for(BusAccess busAccess : busAccesses) {

					String portName = busAccess.getName();
					String modeString = "in";
					if(busAccess.getKind() == AccessType.PROVIDES) {
						modeString = "providesBusAccess";
					}
					else if(busAccess.getKind() == AccessType.REQUIRES) {
						modeString = "requiresBusAccess";
					}


			    	verdict.vdm.vdm_model.Port newPort = createVdmPort(portName, modeString, busAccess.getQualifiedName());

			    	//Note: Not populating "type" for now

//ISSUE: "probe", "event", and "id" not found in DataPort class or superclass

			    	//add to port list of component
			    	packComponent.getPort().add(newPort);
				}//End of checking each busAccess
				
				
//ISSUE: no getOwnedDataAccesses for deviceType
				
				//get all ports
				List<DataPort> dataPorts = devType.getOwnedDataPorts();

				//checking each port's mode and name and adding it to the port list
				for(DataPort dataPort : dataPorts) {

					verdict.vdm.vdm_model.Port newPort = createVdmPort(dataPort, m1, dataTypeDecl);

//ISSUE: "probe", "event", and "id" not found in DataPort class or superclass


			    	//add to port list of component
			    	packComponent.getPort().add(newPort);
				}//End of checking each port

				//get all event data ports
				List<EventDataPort> eventDataPorts = devType.getOwnedEventDataPorts();
				for(EventDataPort eventDataPort:eventDataPorts) {
					verdict.vdm.vdm_model.Port newPort = createVdmPort(eventDataPort, m1, dataTypeDecl);
					//add to port list of component
			    	packComponent.getPort().add(newPort);
				}
				//get all event ports
				List<EventPort> eventPorts = devType.getOwnedEventPorts();
				for(EventPort eventPort:eventPorts) {
					verdict.vdm.vdm_model.Port newPort = createVdmEventPort(eventPort);
					//add to port list of component
			    	packComponent.getPort().add(newPort);
				}
				//packing all events and adding to component
				for(Event anEvent : events) {
					//To pack the event as a VDM event
					verdict.vdm.vdm_model.Event packEvent = createVdmEvent(anEvent);

					//adding to the list of component's events
					packComponent.getEvent().add(packEvent);
				}//End of packing all events


				//packing all cyberRels and adding to component
				for(CyberRel aCyberRel : cyberRels) {
					//To pack the cyberRel as a VDM event
					verdict.vdm.vdm_model.CyberRel packCyberRel = createVdmCyberRel(aCyberRel);


					//adding to the list of component's Cyber relations
					packComponent.getCyberRel().add(packCyberRel);
				}//End of packing all cyberRels


				//packing all safetyRels and adding to component
				for(SafetyRel aSafetyRel : safetyRels) {
					//To pack the safetyRel as a VDM event
					verdict.vdm.vdm_model.SafetyRel packSafetyRel = createVdmSafetyRel(aSafetyRel);

					//adding to the list of component's Safety relations
					packComponent.getSafetyRel().add(packSafetyRel);
				}// End of packing all safetyRels


				//adding to the list of componenmemTypes of the Model object
				m1.getComponentType().add(packComponent);
			}//End of populate the id ...

			/** If a high-level system
			 *  populate the name, safetyReq, cyberReq, and mission
			 *  for the model object
			 * */
			if(!cyberReqs.isEmpty() || !safetyReqs.isEmpty() || !missionReqs.isEmpty()) {
				//checking if a high-level system has already been found
				if (higher_flag == false) {
					higher_flag = true;
				} else {
					System.out.println("Warning: Multiple high-level systems detected!");
				}

				//populating name
				m1.setName(devType.getName());

				//packing all safetyReqs and adding to model
				for(SafetyReq aSafetyReq : safetyReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.SafetyReq packSafetyReq = createVdmSafetyReq(aSafetyReq,
							devType.getFullName());

					//adding to the list of model's Safety requirements
					m1.getSafetyReq().add(packSafetyReq);

				}// End of packing all safetyReqs


				//packing all cyberReqs and adding to model
				for(CyberReq aCyberReq : cyberReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.CyberReq packCyberReq = createVdmCyberReq(aCyberReq, devType.getFullName());

					//adding to the list of model's Cyber requirements
					m1.getCyberReq().add(packCyberReq);

				}// End of packing all cyberReqs


				//packing all missionReqs and adding to model
				for(CyberMission aMission : missionReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.Mission packMission = createVdmMission(aMission);

					//adding to the list of model's Mission
					m1.getMission().add(packMission);
				}// End of packing all missionReqs
			}//End of if a higher-level system
		}  //End of Analyzing each deviceType

		//returning the populated Model
		return m1;
	}//End of translateDeviceTypeObjects


	/**
	 * Analyzing each abstractType:
	 * 1. Determine if it is a lower-level system or higher-level system
	 * 2. If lower-level, add to componentType list attribute of Model
	 * 	2.1 Populate the port, contract, cyberRel, safetyRel, event, id, compCategory
	 *      fields of componentType of the Model object
	 * 3. If higher-level, assign to Model
	 * 	3.1 Populate the safetyReq
	 *      cyberReq, mission fields of Model object
	 * @param abstractTypes
	 * @param m1
	 * @return
	 */
	public Model translateAbstractTypeObjects(List<AbstractType> abstractTypes, Model m1, HashSet<String> dataTypeDecl) {
		for(AbstractType absType : abstractTypes) {

			// variables for unpacking absType
			List<Event> events = new ArrayList<>();
			List<CyberMission> missionReqs = new ArrayList<>();
			List<CyberRel> cyberRels = new ArrayList<>();
			List<SafetyRel> safetyRels = new ArrayList<>();
			List<CyberReq> cyberReqs = new ArrayList<>();
			List<SafetyReq> safetyReqs = new ArrayList<>();

			//a flag to check if a higher -level component has already been found
			boolean higher_flag = false;

			// unpacking absType
			for(AnnexSubclause annex : absType.getOwnedAnnexSubclauses()) {
				if(annex.getName().equalsIgnoreCase("verdict")) {
					Verdict verdictAnnex = VerdictUtil.getVerdict(annex);

					for (Statement statement : verdictAnnex.getElements()) {
						if (statement instanceof Event) {
							events.add((Event)statement);
						} else if(statement instanceof CyberMission) {
							missionReqs.add((CyberMission)statement);
						} else if(statement instanceof CyberReq) {
							cyberReqs.add((CyberReq)statement);
						} else if(statement instanceof CyberRel) {
							cyberRels.add((CyberRel)statement);
						} else if(statement instanceof SafetyReq) {
							safetyReqs.add((SafetyReq)statement);
						} else if(statement instanceof SafetyRel) {
							safetyRels.add((SafetyRel)statement);
						}
					}
				}
			} // End of unpacking absType


			/**
			 *  For every AbstractType,
			 *  populate the id, name, compCateg, port, event,
			 *  cyberRel, and safetyRel fields of componentType
			 *  and add it to the list of componentType
			 *  of the Model object
			 * */
			if(true) { //No Filter-- do for all System Types

				//to pack the memType as a VDM component
				verdict.vdm.vdm_model.ComponentType packComponent = new verdict.vdm.vdm_model.ComponentType();

				//Note: Not populating "contract" for now

//ISSUE: There is no getId() function for memoryType
				packComponent.setId(absType.getQualifiedName());

				//populating "name"
				packComponent.setName(absType.getName());


				//populating "compCateg"
				packComponent.setCompCateg(absType.getCategory().getName());

				//get all bus accesses and store them as ports
				List<BusAccess> busAccesses = absType.getOwnedBusAccesses();
				
				//checking each busAccess's details and adding it to the port list
				for(BusAccess busAccess : busAccesses) {

					String portName = busAccess.getName();
					String modeString = "in";
					if(busAccess.getKind() == AccessType.PROVIDES) {
						modeString = "providesBusAccess";
					}
					else if(busAccess.getKind() == AccessType.REQUIRES) {
						modeString = "requiresBusAccess";
					}


			    	verdict.vdm.vdm_model.Port newPort = createVdmPort(portName, modeString, busAccess.getQualifiedName());

			    	//Note: Not populating "type" for now

//ISSUE: "probe", "event", and "id" not found in DataPort class or superclass

			    	//add to port list of component
			    	packComponent.getPort().add(newPort);
				}//End of checking each busAccess
				
				
				//get all data accesses and store them as ports
				List<DataAccess> dataAccesses = absType.getOwnedDataAccesses();
				
				//checking each dataAccess's details and adding it to the port list
				for(DataAccess dataAccess : dataAccesses) {

					String portName = dataAccess.getName();
					String modeString = "in";
					if(dataAccess.getKind() == AccessType.PROVIDES) {
						modeString = "providesDataAccess";
					}
					else if(dataAccess.getKind() == AccessType.REQUIRES) {
						modeString = "requiresDataAccess";
					}


			    	verdict.vdm.vdm_model.Port newPort = createVdmPort(portName, modeString, dataAccess.getQualifiedName());

			    	//Note: Not populating "type" for now

//ISSUE: "probe", "event", and "id" not found in DataPort class or superclass

			    	//add to port list of component
			    	packComponent.getPort().add(newPort);
				}//End of checking each dataAccess				
				//get all ports
				List<DataPort> dataPorts = absType.getOwnedDataPorts();

				//checking each port's mode and name and adding it to the port list
				for(DataPort dataPort : dataPorts) {

					verdict.vdm.vdm_model.Port newPort = createVdmPort(dataPort, m1, dataTypeDecl);

//ISSUE: "probe", "event", and "id" not found in DataPort class or superclass


			    	//add to port list of component
			    	packComponent.getPort().add(newPort);
				}//End of checking each port

				//get all event data ports
				List<EventDataPort> eventDataPorts = absType.getOwnedEventDataPorts();
				for(EventDataPort eventDataPort:eventDataPorts) {
					verdict.vdm.vdm_model.Port newPort = createVdmPort(eventDataPort, m1, dataTypeDecl);
					//add to port list of component
			    	packComponent.getPort().add(newPort);
				}
				//get all event ports
				List<EventPort> eventPorts = absType.getOwnedEventPorts();
				for(EventPort eventPort:eventPorts) {
					verdict.vdm.vdm_model.Port newPort = createVdmEventPort(eventPort);
					//add to port list of component
			    	packComponent.getPort().add(newPort);
				}

				//packing all events and adding to component
				for(Event anEvent : events) {
					//To pack the event as a VDM event
					verdict.vdm.vdm_model.Event packEvent = createVdmEvent(anEvent);

					//adding to the list of component's events
					packComponent.getEvent().add(packEvent);
				}//End of packing all events


				//packing all cyberRels and adding to component
				for(CyberRel aCyberRel : cyberRels) {
					//To pack the cyberRel as a VDM event
					verdict.vdm.vdm_model.CyberRel packCyberRel = createVdmCyberRel(aCyberRel);


					//adding to the list of component's Cyber relations
					packComponent.getCyberRel().add(packCyberRel);
				}//End of packing all cyberRels


				//packing all safetyRels and adding to component
				for(SafetyRel aSafetyRel : safetyRels) {
					//To pack the safetyRel as a VDM event
					verdict.vdm.vdm_model.SafetyRel packSafetyRel = createVdmSafetyRel(aSafetyRel);

					//adding to the list of component's Safety relations
					packComponent.getSafetyRel().add(packSafetyRel);
				}// End of packing all safetyRels


				//adding to the list of componenmemTypes of the Model object
				m1.getComponentType().add(packComponent);
			}//End of populate the id ...

			/** If a high-level system
			 *  populate the name, safetyReq, cyberReq, and mission
			 *  for the model object
			 * */
			if(!cyberReqs.isEmpty() || !safetyReqs.isEmpty() || !missionReqs.isEmpty()) {
				//checking if a high-level system has already been found
				if (higher_flag == false) {
					higher_flag = true;
				} else {
					System.out.println("Warning: Multiple high-level systems detected!");
				}

				//populating name
				m1.setName(absType.getName());

				//packing all safetyReqs and adding to model
				for(SafetyReq aSafetyReq : safetyReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.SafetyReq packSafetyReq = createVdmSafetyReq(aSafetyReq,
							absType.getFullName());

					//adding to the list of model's Safety requirements
					m1.getSafetyReq().add(packSafetyReq);

				}// End of packing all safetyReqs


				//packing all cyberReqs and adding to model
				for(CyberReq aCyberReq : cyberReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.CyberReq packCyberReq = createVdmCyberReq(aCyberReq, absType.getFullName());

					//adding to the list of model's Cyber requirements
					m1.getCyberReq().add(packCyberReq);

				}// End of packing all cyberReqs


				//packing all missionReqs and adding to model
				for(CyberMission aMission : missionReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.Mission packMission = createVdmMission(aMission);

					//adding to the list of model's Mission
					m1.getMission().add(packMission);
				}// End of packing all missionReqs
			}//End of if a higher-level system
		}  //End of Analyzing each abstractType

		//returning the populated Model
		return m1;
	}//End of translateAbstractTypeObjects


	/**
	 * Analyzing each processType:
	 * 1. Determine if it is a lower-level system or higher-level system
	 * 2. If lower-level, add to componentType list attribute of Model
	 * 	2.1 Populate the port, contract, cyberRel, safetyRel, event, id, compCategory
	 *      fields of componentType of the Model object
	 * 3. If higher-level, assign to Model
	 * 	3.1 Populate the safetyReq
	 *      cyberReq, mission fields of Model object
	 * @param processTypes
	 * @param m1
	 * @return
	 */
	public Model translateProcessTypeObjects(List<ProcessType> processTypes, Model m1, HashSet<String> dataTypeDecl) {
		for(ProcessType prcsType : processTypes) {
			// variables for unpacking prcsType
			List<Event> events = new ArrayList<>();
			List<CyberMission> missionReqs = new ArrayList<>();
			List<CyberRel> cyberRels = new ArrayList<>();
			List<SafetyRel> safetyRels = new ArrayList<>();
			List<CyberReq> cyberReqs = new ArrayList<>();
			List<SafetyReq> safetyReqs = new ArrayList<>();

			//a flag to check if a higher -level component has already been found
			boolean higher_flag = false;

			// unpacking prcsType
			for(AnnexSubclause annex : prcsType.getOwnedAnnexSubclauses()) {
				if(annex.getName().equalsIgnoreCase("verdict")) {
					Verdict verdictAnnex = VerdictUtil.getVerdict(annex);

					for (Statement statement : verdictAnnex.getElements()) {
						if (statement instanceof Event) {
							events.add((Event)statement);
						} else if(statement instanceof CyberMission) {
							missionReqs.add((CyberMission)statement);
						} else if(statement instanceof CyberReq) {
							cyberReqs.add((CyberReq)statement);
						} else if(statement instanceof CyberRel) {
							cyberRels.add((CyberRel)statement);
						} else if(statement instanceof SafetyReq) {
							safetyReqs.add((SafetyReq)statement);
						} else if(statement instanceof SafetyRel) {
							safetyRels.add((SafetyRel)statement);
						}
					}
				}
			} // End of unpacking prcsType


			/**
			 *  For every ProcessType,
			 *  populate the id, name, compCateg, port, event,
			 *  cyberRel, and safetyRel fields of componentType
			 *  and add it to the list of componentType
			 *  of the Model object
			 * */
			if(true) { //No Filter-- do for all System Types

				//to pack the memType as a VDM component
				verdict.vdm.vdm_model.ComponentType packComponent = new verdict.vdm.vdm_model.ComponentType();

				//Note: Not populating "contract" for now

//ISSUE: There is no getId() function for memoryType
				packComponent.setId(prcsType.getQualifiedName());

				//populating "name"
				packComponent.setName(prcsType.getName());


				//populating "compCateg"
				packComponent.setCompCateg(prcsType.getCategory().getName());

//ISSUE: no getOwnedBusAccesses				
				
				//get all data accesses and store them as ports
				List<DataAccess> dataAccesses = prcsType.getOwnedDataAccesses();
				
				//checking each dataAccess's details and adding it to the port list
				for(DataAccess dataAccess : dataAccesses) {

					String portName = dataAccess.getName();
					String modeString = "in";
					if(dataAccess.getKind() == AccessType.PROVIDES) {
						modeString = "providesDataAccess";
					}
					else if(dataAccess.getKind() == AccessType.REQUIRES) {
						modeString = "requiresDataAccess";
					}


			    	verdict.vdm.vdm_model.Port newPort = createVdmPort(portName, modeString, dataAccess.getQualifiedName());

			    	//Note: Not populating "type" for now

//ISSUE: "probe", "event", and "id" not found in DataPort class or superclass

			    	//add to port list of component
			    	packComponent.getPort().add(newPort);
				}//End of checking each dataAccess
				
				//get all ports
				List<DataPort> dataPorts = prcsType.getOwnedDataPorts();

				//checking each port's mode and name and adding it to the port list
				for(DataPort dataPort : dataPorts) {

					verdict.vdm.vdm_model.Port newPort = createVdmPort(dataPort, m1, dataTypeDecl);

			    	//Note: Not populating "type" for now

//ISSUE: "probe", "event", and "id" not found in DataPort class or superclass


			    	//add to port list of component
			    	packComponent.getPort().add(newPort);
				}//End of checking each port
				
				//get all event data ports
				List<EventDataPort> eventDataPorts = prcsType.getOwnedEventDataPorts();
				for(EventDataPort eventDataPort:eventDataPorts) {
					verdict.vdm.vdm_model.Port newPort = createVdmPort(eventDataPort, m1, dataTypeDecl);
					//add to port list of component
			    	packComponent.getPort().add(newPort);
				}
				
				//get all event ports
				List<EventPort> eventPorts = prcsType.getOwnedEventPorts();
				for(EventPort eventPort:eventPorts) {
					verdict.vdm.vdm_model.Port newPort = createVdmEventPort(eventPort);
					//add to port list of component
			    	packComponent.getPort().add(newPort);
				}
				
				//packing all events and adding to component
				for(Event anEvent : events) {
					//To pack the event as a VDM event
					verdict.vdm.vdm_model.Event packEvent = createVdmEvent(anEvent);

					//adding to the list of component's events
					packComponent.getEvent().add(packEvent);
				}//End of packing all events


				//packing all cyberRels and adding to component
				for(CyberRel aCyberRel : cyberRels) {
					//To pack the cyberRel as a VDM event
					verdict.vdm.vdm_model.CyberRel packCyberRel = createVdmCyberRel(aCyberRel);


					//adding to the list of component's Cyber relations
					packComponent.getCyberRel().add(packCyberRel);
				}//End of packing all cyberRels


				//packing all safetyRels and adding to component
				for(SafetyRel aSafetyRel : safetyRels) {
					//To pack the safetyRel as a VDM event
					verdict.vdm.vdm_model.SafetyRel packSafetyRel = createVdmSafetyRel(aSafetyRel);

					//adding to the list of component's Safety relations
					packComponent.getSafetyRel().add(packSafetyRel);
				}// End of packing all safetyRels


				//adding to the list of componenmemTypes of the Model object
				m1.getComponentType().add(packComponent);
			}//End of populate the id ...

			/** If a high-level system
			 *  populate the name, safetyReq, cyberReq, and mission
			 *  for the model object
			 * */
			if(!cyberReqs.isEmpty() || !safetyReqs.isEmpty() || !missionReqs.isEmpty()) {
				//checking if a high-level system has already been found
				if (higher_flag == false) {
					higher_flag = true;
				} else {
					System.out.println("Warning: Multiple high-level systems detected!");
				}

				//populating name
				m1.setName(prcsType.getName());

				//packing all safetyReqs and adding to model
				for(SafetyReq aSafetyReq : safetyReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.SafetyReq packSafetyReq = createVdmSafetyReq(aSafetyReq,
							prcsType.getFullName());

					//adding to the list of model's Safety requirements
					m1.getSafetyReq().add(packSafetyReq);

				}// End of packing all safetyReqs


				//packing all cyberReqs and adding to model
				for(CyberReq aCyberReq : cyberReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.CyberReq packCyberReq = createVdmCyberReq(aCyberReq, prcsType.getFullName());

					//adding to the list of model's Cyber requirements
					m1.getCyberReq().add(packCyberReq);

				}// End of packing all cyberReqs


				//packing all missionReqs and adding to model
				for(CyberMission aMission : missionReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.Mission packMission = createVdmMission(aMission);

					//adding to the list of model's Mission
					m1.getMission().add(packMission);
				}// End of packing all missionReqs
			}//End of if a higher-level system
		}  //End of Analyzing each processType

		//returning the populated Model
		return m1;
	}//End of translateProcessTypeObjects


	/**
	 * Analyzing each threadGroupType:
	 * 1. Determine if it is a lower-level system or higher-level system
	 * 2. If lower-level, add to componentType list attribute of Model
	 * 	2.1 Populate the port, contract, cyberRel, safetyRel, event, id, compCategory
	 *      fields of componentType of the Model object
	 * 3. If higher-level, assign to Model
	 * 	3.1 Populate the safetyReq
	 *      cyberReq, mission fields of Model object
	 * @param threadGroupTypes
	 * @param m1
	 * @return
	 */
	public Model translateThreadGroupTypeObjects(List<ThreadGroupType> threadGroupTypes, Model m1, HashSet<String> dataTypeDecl) {
		for(ThreadGroupType tgType : threadGroupTypes) {

			// variables for unpacking tgType
			List<Event> events = new ArrayList<>();
			List<CyberMission> missionReqs = new ArrayList<>();
			List<CyberRel> cyberRels = new ArrayList<>();
			List<SafetyRel> safetyRels = new ArrayList<>();
			List<CyberReq> cyberReqs = new ArrayList<>();
			List<SafetyReq> safetyReqs = new ArrayList<>();

			//a flag to check if a higher -level component has already been found
			boolean higher_flag = false;

			// unpacking tgType
			for(AnnexSubclause annex : tgType.getOwnedAnnexSubclauses()) {
				if(annex.getName().equalsIgnoreCase("verdict")) {
					Verdict verdictAnnex = VerdictUtil.getVerdict(annex);

					for (Statement statement : verdictAnnex.getElements()) {
						if (statement instanceof Event) {
							events.add((Event)statement);
						} else if(statement instanceof CyberMission) {
							missionReqs.add((CyberMission)statement);
						} else if(statement instanceof CyberReq) {
							cyberReqs.add((CyberReq)statement);
						} else if(statement instanceof CyberRel) {
							cyberRels.add((CyberRel)statement);
						} else if(statement instanceof SafetyReq) {
							safetyReqs.add((SafetyReq)statement);
						} else if(statement instanceof SafetyRel) {
							safetyRels.add((SafetyRel)statement);
						}
					}
				}
			} // End of unpacking tgType


			/**
			 *  For every ThreadGroupType,
			 *  populate the id, name, compCateg, port, event,
			 *  cyberRel, and safetyRel fields of componentType
			 *  and add it to the list of componentType
			 *  of the Model object
			 * */
			if(true) { //No Filter-- do for all System Types

				//to pack the memType as a VDM component
				verdict.vdm.vdm_model.ComponentType packComponent = new verdict.vdm.vdm_model.ComponentType();

				//Note: Not populating "contract" for now

//ISSUE: There is no getId() function for memoryType
				packComponent.setId(tgType.getQualifiedName());

				//populating "name"
				packComponent.setName(tgType.getName());


				//populating "compCateg"
				packComponent.setCompCateg(tgType.getCategory().getName());

//ISSUE: no getOwnedBusAccesses				
				
				//get all data accesses and store them as ports
				List<DataAccess> dataAccesses = tgType.getOwnedDataAccesses();
				
				//checking each dataAccess's details and adding it to the port list
				for(DataAccess dataAccess : dataAccesses) {

					String portName = dataAccess.getName();
					String modeString = "in";
					if(dataAccess.getKind() == AccessType.PROVIDES) {
						modeString = "providesDataAccess";
					}
					else if(dataAccess.getKind() == AccessType.REQUIRES) {
						modeString = "requiresDataAccess";
					}


			    	verdict.vdm.vdm_model.Port newPort = createVdmPort(portName, modeString, dataAccess.getQualifiedName());

			    	//Note: Not populating "type" for now

//ISSUE: "probe", "event", and "id" not found in DataPort class or superclass

			    	//add to port list of component
			    	packComponent.getPort().add(newPort);
				}//End of checking each dataAccess
				
				//get all ports
				List<DataPort> dataPorts = tgType.getOwnedDataPorts();

				//checking each port's mode and name and adding it to the port list
				for(DataPort dataPort : dataPorts) {

					verdict.vdm.vdm_model.Port newPort = createVdmPort(dataPort, m1, dataTypeDecl);

//ISSUE: "probe", "event", and "id" not found in DataPort class or superclass


			    	//add to port list of component
			    	packComponent.getPort().add(newPort);
				}//End of checking each port

				//get all event data ports
				List<EventDataPort> eventDataPorts = tgType.getOwnedEventDataPorts();
				for(EventDataPort eventDataPort:eventDataPorts) {
					verdict.vdm.vdm_model.Port newPort = createVdmPort(eventDataPort, m1, dataTypeDecl);
					//add to port list of component
			    	packComponent.getPort().add(newPort);
				}				
				//get all event ports
				List<EventPort> eventPorts = tgType.getOwnedEventPorts();
				for(EventPort eventPort:eventPorts) {
					verdict.vdm.vdm_model.Port newPort = createVdmEventPort(eventPort);
					//add to port list of component
			    	packComponent.getPort().add(newPort);
				}

				//packing all events and adding to component
				for(Event anEvent : events) {
					//To pack the event as a VDM event
					verdict.vdm.vdm_model.Event packEvent = createVdmEvent(anEvent);

					//adding to the list of component's events
					packComponent.getEvent().add(packEvent);
				}//End of packing all events


				//packing all cyberRels and adding to component
				for(CyberRel aCyberRel : cyberRels) {
					//To pack the cyberRel as a VDM event
					verdict.vdm.vdm_model.CyberRel packCyberRel = createVdmCyberRel(aCyberRel);


					//adding to the list of component's Cyber relations
					packComponent.getCyberRel().add(packCyberRel);
				}//End of packing all cyberRels


				//packing all safetyRels and adding to component
				for(SafetyRel aSafetyRel : safetyRels) {
					//To pack the safetyRel as a VDM event
					verdict.vdm.vdm_model.SafetyRel packSafetyRel = createVdmSafetyRel(aSafetyRel);

					//adding to the list of component's Safety relations
					packComponent.getSafetyRel().add(packSafetyRel);
				}// End of packing all safetyRels


				//adding to the list of componenmemTypes of the Model object
				m1.getComponentType().add(packComponent);
			}//End of populate the id ...

			/** If a high-level system
			 *  populate the name, safetyReq, cyberReq, and mission
			 *  for the model object
			 * */
			if(!cyberReqs.isEmpty() || !safetyReqs.isEmpty() || !missionReqs.isEmpty()) {
				//checking if a high-level system has already been found
				if (higher_flag == false) {
					higher_flag = true;
				} else {
					System.out.println("Warning: Multiple high-level systems detected!");
				}

				//populating name
				m1.setName(tgType.getName());

				//packing all safetyReqs and adding to model
				for(SafetyReq aSafetyReq : safetyReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.SafetyReq packSafetyReq = createVdmSafetyReq(aSafetyReq,
							tgType.getFullName());

					//adding to the list of model's Safety requirements
					m1.getSafetyReq().add(packSafetyReq);

				}// End of packing all safetyReqs


				//packing all cyberReqs and adding to model
				for(CyberReq aCyberReq : cyberReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.CyberReq packCyberReq = createVdmCyberReq(aCyberReq, tgType.getFullName());

					//adding to the list of model's Cyber requirements
					m1.getCyberReq().add(packCyberReq);

				}// End of packing all cyberReqs


				//packing all missionReqs and adding to model
				for(CyberMission aMission : missionReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.Mission packMission = createVdmMission(aMission);

					//adding to the list of model's Mission
					m1.getMission().add(packMission);
				}// End of packing all missionReqs
			}//End of if a higher-level system
		}  //End of Analyzing each threadGroupType

		//returning the populated Model
		return m1;
	}//End of translateThreadGroupTypeObjects


	/**
	 * Analyzing each virtualProcessorType:
	 * 1. Determine if it is a lower-level system or higher-level system
	 * 2. If lower-level, add to componentType list attribute of Model
	 * 	2.1 Populate the port, contract, cyberRel, safetyRel, event, id, compCategory
	 *      fields of componentType of the Model object
	 * 3. If higher-level, assign to Model
	 * 	3.1 Populate the safetyReq
	 *      cyberReq, mission fields of Model object
	 * @param virtualProcessorTypes
	 * @param m1
	 * @return
	 */
	public Model translateVirtualProcessorTypeObjects(List<VirtualProcessorType> virtualProcessorTypes, Model m1, HashSet<String> dataTypeDecl) {
		for(VirtualProcessorType vprocType : virtualProcessorTypes) {

			// variables for unpacking vprocType
			List<Event> events = new ArrayList<>();
			List<CyberMission> missionReqs = new ArrayList<>();
			List<CyberRel> cyberRels = new ArrayList<>();
			List<SafetyRel> safetyRels = new ArrayList<>();
			List<CyberReq> cyberReqs = new ArrayList<>();
			List<SafetyReq> safetyReqs = new ArrayList<>();

			//a flag to check if a higher -level component has already been found
			boolean higher_flag = false;

			// unpacking vprocType
			for(AnnexSubclause annex : vprocType.getOwnedAnnexSubclauses()) {
				if(annex.getName().equalsIgnoreCase("verdict")) {
					Verdict verdictAnnex = VerdictUtil.getVerdict(annex);

					for (Statement statement : verdictAnnex.getElements()) {
						if (statement instanceof Event) {
							events.add((Event)statement);
						} else if(statement instanceof CyberMission) {
							missionReqs.add((CyberMission)statement);
						} else if(statement instanceof CyberReq) {
							cyberReqs.add((CyberReq)statement);
						} else if(statement instanceof CyberRel) {
							cyberRels.add((CyberRel)statement);
						} else if(statement instanceof SafetyReq) {
							safetyReqs.add((SafetyReq)statement);
						} else if(statement instanceof SafetyRel) {
							safetyRels.add((SafetyRel)statement);
						}
					}
				}
			} // End of unpacking vprocType


			/**
			 *  For every VirtualProcessorType,
			 *  populate the id, name, compCateg, port, event,
			 *  cyberRel, and safetyRel fields of componentType
			 *  and add it to the list of componentType
			 *  of the Model object
			 * */
			if(true) { //No Filter-- do for all System Types

				//to pack the memType as a VDM component
				verdict.vdm.vdm_model.ComponentType packComponent = new verdict.vdm.vdm_model.ComponentType();

				//Note: Not populating "contract" for now

//ISSUE: There is no getId() function for memoryType
				packComponent.setId(vprocType.getQualifiedName());

				//populating "name"
				packComponent.setName(vprocType.getName());


				//populating "compCateg"
				packComponent.setCompCateg(vprocType.getCategory().getName());

				//get all bus accesses and store them as ports
				List<BusAccess> busAccesses = vprocType.getOwnedBusAccesses();
				
				//checking each busAccess's details and adding it to the port list
				for(BusAccess busAccess : busAccesses) {

					String portName = busAccess.getName();
					String modeString = "in";
					if(busAccess.getKind() == AccessType.PROVIDES) {
						modeString = "providesBusAccess";
					}
					else if(busAccess.getKind() == AccessType.REQUIRES) {
						modeString = "requiresBusAccess";
					}


			    	verdict.vdm.vdm_model.Port newPort = createVdmPort(portName, modeString, busAccess.getQualifiedName());

			    	//Note: Not populating "type" for now

//ISSUE: "probe", "event", and "id" not found in DataPort class or superclass

			    	//add to port list of component
			    	packComponent.getPort().add(newPort);
				}//End of checking each busAccess
				
				
//ISSUE: no getOwnedDataAccesses for virtualProcessorType
				
				//get all ports
				List<DataPort> dataPorts = vprocType.getOwnedDataPorts();

				//checking each port's mode and name and adding it to the port list
				for(DataPort dataPort : dataPorts) {
					verdict.vdm.vdm_model.Port newPort = createVdmPort(dataPort, m1, dataTypeDecl);

			    	//Note: Not populating "type" for now

//ISSUE: "probe", "event", and "id" not found in DataPort class or superclass


			    	//add to port list of component
			    	packComponent.getPort().add(newPort);
				}//End of checking each port

				//get all event data ports
				List<EventDataPort> eventDataPorts = vprocType.getOwnedEventDataPorts();
				for(EventDataPort eventDataPort:eventDataPorts) {
					verdict.vdm.vdm_model.Port newPort = createVdmPort(eventDataPort, m1, dataTypeDecl);
					//add to port list of component
			    	packComponent.getPort().add(newPort);
				}				
				//get all event ports
				List<EventPort> eventPorts = vprocType.getOwnedEventPorts();
				for(EventPort eventPort:eventPorts) {
					verdict.vdm.vdm_model.Port newPort = createVdmEventPort(eventPort);
					//add to port list of component
			    	packComponent.getPort().add(newPort);
				}
				//packing all events and adding to component
				for(Event anEvent : events) {
					//To pack the event as a VDM event
					verdict.vdm.vdm_model.Event packEvent = createVdmEvent(anEvent);

					//adding to the list of component's events
					packComponent.getEvent().add(packEvent);
				}//End of packing all events


				//packing all cyberRels and adding to component
				for(CyberRel aCyberRel : cyberRels) {
					//To pack the cyberRel as a VDM event
					verdict.vdm.vdm_model.CyberRel packCyberRel = createVdmCyberRel(aCyberRel);


					//adding to the list of component's Cyber relations
					packComponent.getCyberRel().add(packCyberRel);
				}//End of packing all cyberRels


				//packing all safetyRels and adding to component
				for(SafetyRel aSafetyRel : safetyRels) {
					//To pack the safetyRel as a VDM event
					verdict.vdm.vdm_model.SafetyRel packSafetyRel = createVdmSafetyRel(aSafetyRel);

					//adding to the list of component's Safety relations
					packComponent.getSafetyRel().add(packSafetyRel);
				}// End of packing all safetyRels


				//adding to the list of componenmemTypes of the Model object
				m1.getComponentType().add(packComponent);
			}//End of populate the id ...

			/** If a high-level system
			 *  populate the name, safetyReq, cyberReq, and mission
			 *  for the model object
			 * */
			if(!cyberReqs.isEmpty() || !safetyReqs.isEmpty() || !missionReqs.isEmpty()) {
				//checking if a high-level system has already been found
				if (higher_flag == false) {
					higher_flag = true;
				} else {
					System.out.println("Warning: Multiple high-level systems detected!");
				}

				//populating name
				m1.setName(vprocType.getName());

				//packing all safetyReqs and adding to model
				for(SafetyReq aSafetyReq : safetyReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.SafetyReq packSafetyReq = createVdmSafetyReq(aSafetyReq,
							vprocType.getFullName());

					//adding to the list of model's Safety requirements
					m1.getSafetyReq().add(packSafetyReq);

				}// End of packing all safetyReqs


				//packing all cyberReqs and adding to model
				for(CyberReq aCyberReq : cyberReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.CyberReq packCyberReq = createVdmCyberReq(aCyberReq, vprocType.getFullName());

					//adding to the list of model's Cyber requirements
					m1.getCyberReq().add(packCyberReq);

				}// End of packing all cyberReqs


				//packing all missionReqs and adding to model
				for(CyberMission aMission : missionReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.Mission packMission = createVdmMission(aMission);


					//adding to the list of model's Mission
					m1.getMission().add(packMission);
				}// End of packing all missionReqs
			}//End of if a higher-level system
		}  //End of Analyzing each virtualProcessorType

		//returning the populated Model
		return m1;
	}//End of translateVirtualProcessorTypeObjects


	/**
	 * Analyzing each processorType:
	 * 1. Determine if it is a lower-level system or higher-level system
	 * 2. If lower-level, add to componentType list attribute of Model
	 * 	2.1 Populate the port, contract, cyberRel, safetyRel, event, id, compCategory
	 *      fields of componentType of the Model object
	 * 3. If higher-level, assign to Model
	 * 	3.1 Populate the safetyReq
	 *      cyberReq, mission fields of Model object
	 * @param processorTypes
	 * @param m1
	 * @return
	 */
	public Model translateProcessorTypeObjects(List<ProcessorType> processorTypes, Model m1, HashSet<String> dataTypeDecl) {
		for(ProcessorType proType : processorTypes) {

			// variables for unpacking proType
			List<Event> events = new ArrayList<>();
			List<CyberMission> missionReqs = new ArrayList<>();
			List<CyberRel> cyberRels = new ArrayList<>();
			List<SafetyRel> safetyRels = new ArrayList<>();
			List<CyberReq> cyberReqs = new ArrayList<>();
			List<SafetyReq> safetyReqs = new ArrayList<>();

			//a flag to check if a higher -level component has already been found
			boolean higher_flag = false;

			// unpacking proType
			for(AnnexSubclause annex : proType.getOwnedAnnexSubclauses()) {
				if(annex.getName().equalsIgnoreCase("verdict")) {
					Verdict verdictAnnex = VerdictUtil.getVerdict(annex);

					for (Statement statement : verdictAnnex.getElements()) {
						if (statement instanceof Event) {
							events.add((Event)statement);
						} else if(statement instanceof CyberMission) {
							missionReqs.add((CyberMission)statement);
						} else if(statement instanceof CyberReq) {
							cyberReqs.add((CyberReq)statement);
						} else if(statement instanceof CyberRel) {
							cyberRels.add((CyberRel)statement);
						} else if(statement instanceof SafetyReq) {
							safetyReqs.add((SafetyReq)statement);
						} else if(statement instanceof SafetyRel) {
							safetyRels.add((SafetyRel)statement);
						}
					}
				}
			} // End of unpacking proType


			/**
			 *  For every ProcessorType,
			 *  populate the id, name, compCateg, port, event,
			 *  cyberRel, and safetyRel fields of componentType
			 *  and add it to the list of componentType
			 *  of the Model object
			 * */
			if(true) { //No Filter-- do for all System Types

				//to pack the memType as a VDM component
				verdict.vdm.vdm_model.ComponentType packComponent = new verdict.vdm.vdm_model.ComponentType();

				//Note: Not populating "contract" for now

//ISSUE: There is no getId() function for memoryType
				packComponent.setId(proType.getQualifiedName());

				//populating "name"
				packComponent.setName(proType.getName());


				//populating "compCateg"
				packComponent.setCompCateg(proType.getCategory().getName());

				//get all bus accesses and store them as ports
				List<BusAccess> busAccesses = proType.getOwnedBusAccesses();
				
				//checking each busAccess's details and adding it to the port list
				for(BusAccess busAccess : busAccesses) {

					String portName = busAccess.getName();
					String modeString = "in";
					if(busAccess.getKind() == AccessType.PROVIDES) {
						modeString = "providesBusAccess";
					}
					else if(busAccess.getKind() == AccessType.REQUIRES) {
						modeString = "requiresBusAccess";
					}


			    	verdict.vdm.vdm_model.Port newPort = createVdmPort(portName, modeString, busAccess.getQualifiedName());

			    	//Note: Not populating "type" for now

//ISSUE: "probe", "event", and "id" not found in DataPort class or superclass

			    	//add to port list of component
			    	packComponent.getPort().add(newPort);
				}//End of checking each busAccess
				
				
//ISSUE: no getOwnedDataAccess for processorType
				
				//get all ports
				List<DataPort> dataPorts = proType.getOwnedDataPorts();

				//checking each port's mode and name and adding it to the port list
				for(DataPort dataPort : dataPorts) {
					verdict.vdm.vdm_model.Port newPort = createVdmPort(dataPort, m1, dataTypeDecl);

//ISSUE: "probe", "event", and "id" not found in DataPort class or superclass


			    	//add to port list of component
			    	packComponent.getPort().add(newPort);
				}//End of checking each port

				//get all event data ports
				List<EventDataPort> eventDataPorts = proType.getOwnedEventDataPorts();
				for(EventDataPort eventDataPort:eventDataPorts) {
					verdict.vdm.vdm_model.Port newPort = createVdmPort(eventDataPort, m1, dataTypeDecl);
					//add to port list of component
			    	packComponent.getPort().add(newPort);
				}	
				//get all event ports
				List<EventPort> eventPorts = proType.getOwnedEventPorts();
				for(EventPort eventPort:eventPorts) {
					verdict.vdm.vdm_model.Port newPort = createVdmEventPort(eventPort);
					//add to port list of component
			    	packComponent.getPort().add(newPort);
				}
				
				//packing all events and adding to component
				for(Event anEvent : events) {
					//To pack the event as a VDM event
					verdict.vdm.vdm_model.Event packEvent = createVdmEvent(anEvent);

					//adding to the list of component's events
					packComponent.getEvent().add(packEvent);
				}//End of packing all events


				//packing all cyberRels and adding to component
				for(CyberRel aCyberRel : cyberRels) {
					//To pack the cyberRel as a VDM event
					verdict.vdm.vdm_model.CyberRel packCyberRel = createVdmCyberRel(aCyberRel);


					//adding to the list of component's Cyber relations
					packComponent.getCyberRel().add(packCyberRel);
				}//End of packing all cyberRels


				//packing all safetyRels and adding to component
				for(SafetyRel aSafetyRel : safetyRels) {
					//To pack the safetyRel as a VDM event
					verdict.vdm.vdm_model.SafetyRel packSafetyRel = createVdmSafetyRel(aSafetyRel);

					//adding to the list of component's Safety relations
					packComponent.getSafetyRel().add(packSafetyRel);
				}// End of packing all safetyRels


				//adding to the list of componenmemTypes of the Model object
				m1.getComponentType().add(packComponent);
			}//End of populate the id ...

			/** If a high-level system
			 *  populate the name, safetyReq, cyberReq, and mission
			 *  for the model object
			 * */
			if(!cyberReqs.isEmpty() || !safetyReqs.isEmpty() || !missionReqs.isEmpty()) {
				//checking if a high-level system has already been found
				if (higher_flag == false) {
					higher_flag = true;
				} else {
					System.out.println("Warning: Multiple high-level systems detected!");
				}

				//populating name
				m1.setName(proType.getName());

				//packing all safetyReqs and adding to model
				for(SafetyReq aSafetyReq : safetyReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.SafetyReq packSafetyReq = createVdmSafetyReq(aSafetyReq,
							proType.getFullName());

					//adding to the list of model's Safety requirements
					m1.getSafetyReq().add(packSafetyReq);

				}// End of packing all safetyReqs


				//packing all cyberReqs and adding to model
				for(CyberReq aCyberReq : cyberReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.CyberReq packCyberReq = createVdmCyberReq(aCyberReq, proType.getFullName());

					//adding to the list of model's Cyber requirements
					m1.getCyberReq().add(packCyberReq);

				}// End of packing all cyberReqs


				//packing all missionReqs and adding to model
				for(CyberMission aMission : missionReqs) {
					//To pack the safettReq as a VDM event
					verdict.vdm.vdm_model.Mission packMission = createVdmMission(aMission);


					//adding to the list of model's Mission
					m1.getMission().add(packMission);
				}// End of packing all missionReqs
			}//End of if a higher-level system
		}  //End of Analyzing each processorType

		//returning the populated Model
		return m1;
	}//End of translateProcessorTypeObjects


	/**
	 * Analyzing each component implementation
	 * @param comImpls
	 * @param m2
	 * @return
	 */
	public Model translateComponentImplObjects(List<ComponentImplementation> comImpls, Map<Property, String> componentPropertyToName, Map<Property, String> connPropertyToName, Model m2, HashSet<String> dataTypeDecl) {

		Map<String, String> connectionToBusMap = new HashMap<>();
		//creating an object for each implementation first as we will need it later
		for(ComponentImplementation aSystemImpl : comImpls) {

			//to pack the sysImpl as a VDM componentImpl
			verdict.vdm.vdm_model.ComponentImpl packCompImpl = new verdict.vdm.vdm_model.ComponentImpl();

			//setting "name" field of packCompImpl, will need later
			packCompImpl.setName(aSystemImpl.getName());


			//Note: Will skip "Nodebody" field for now

			//ISSUE: No "id" field in Component implementations
			packCompImpl.setId(aSystemImpl.getQualifiedName());

			//adding object to "componentImpl" field of m2
			m2.getComponentImpl().add(packCompImpl);
			
			
			/* Getting all the bus binding information from all component implementations*/
			//get and process properties associated with the implementation - especially aadl property - actual_connection_binding
			//update map (connection-name -> bus-Instance-Name)
			for(PropertyAssociation propAssoc : aSystemImpl.getOwnedPropertyAssociations()) {
				if(!(propAssoc.getProperty().getName().equalsIgnoreCase("Actual_Connection_Binding"))) {
					throw new RuntimeException("System Implementation contains property "+propAssoc.getProperty().getName()+" which is not currently handled.");
				}
				if(propAssoc.getOwnedValues().size() != 1) {
					throw new RuntimeException("Unexpected number of property owned values: " + propAssoc.getOwnedValues().size());
				}
				if(!(propAssoc.getOwnedValues().get(0).getOwnedValue() instanceof ListValueImpl)) {
					throw new RuntimeException("Unexpected type of property owned value");
				} else {
					ListValueImpl listVal = (ListValueImpl)propAssoc.getOwnedValues().get(0).getOwnedValue();
					if(listVal.getOwnedListElements().size() != 1) {
						throw new RuntimeException("Unexpected number of list elements are associated with the property owned value");
					} else if(!(listVal.getOwnedListElements().get(0) instanceof ReferenceValueImpl)) {
						throw new RuntimeException("Unexpected number of list elements are associated with the property owned value");
					} else {
						ReferenceValueImpl refVal = (ReferenceValueImpl)listVal.getOwnedListElements().get(0);
						ContainmentPathElement pathEle = refVal.getPath();
						while(!(pathEle.getNamedElement() instanceof BusSubcomponent)) {
							pathEle = pathEle.getPath();
						}
						String busInstanceName = pathEle.getNamedElement().getQualifiedName();
						for(ContainedNamedElement connection: propAssoc.getAppliesTos()) {
							//updating map (connection name -> bus name)
							connectionToBusMap.put(connection.getPath().getNamedElement().getQualifiedName(),busInstanceName);
						}
					}
				}
			}
			
		}//End of creating an object

		//Getting the reference of the object previously created and populating
		for(ComponentImplementation aCompImpl : comImpls) {

			//variable to refer to previously created object
			verdict.vdm.vdm_model.ComponentImpl packCompImpl = new verdict.vdm.vdm_model.ComponentImpl();

			//finding previously created object
			for (verdict.vdm.vdm_model.ComponentImpl anImplObj : m2.getComponentImpl()) {
				if(anImplObj.getName().equals(aCompImpl.getName())) {
					packCompImpl = anImplObj;

				}
			}

			//setting "type" field of packCompImpl
			for(verdict.vdm.vdm_model.ComponentType cType : m2.getComponentType()) {
				if(aCompImpl.getTypeName().equals(cType.getName())){
					packCompImpl.setType(cType);
				}
			}//End of setting "type"

			//a BlockImpl object to pack all info for packCompImpl.blockImpl
			verdict.vdm.vdm_model.BlockImpl packBlockImpl = new verdict.vdm.vdm_model.BlockImpl();

			//adding all subcomponents to "subcomponent" field of packBlockImpl
			for (Subcomponent aSubComp : aCompImpl.getOwnedSubcomponents()) {

				//to pack all information of a subcomponent
				verdict.vdm.vdm_model.ComponentInstance packSubComp = new verdict.vdm.vdm_model.ComponentInstance();

				//ISSUE: No "id" field in subcomponents
				packSubComp.setId(aSubComp.getQualifiedName());

				//setting "name" field of packSubComp
				packSubComp.setName(aSubComp.getFullName());

				//setting "specification" field of packSubComp
				for(verdict.vdm.vdm_model.ComponentType cType : m2.getComponentType()) {
					if(aSubComp.getComponentType().getName().equals(cType.getName())){
						packSubComp.setSpecification(cType);

					}
				}

				//setting the "implementation" field of packSubComp
				for(verdict.vdm.vdm_model.ComponentImpl cImpl : m2.getComponentImpl()) {
					//if(aSubComp.getSubcomponentType().getName().equals(cImpl.getName())){
					if(aSubComp.getSubcomponentType().getQualifiedName().equals(cImpl.getId())){	
						packSubComp.setImplementation(cImpl);

					}
				}

				//setting "attribute" field of packSubComp
				String aSubCompCatName = aSubComp.getCategory().getName().toLowerCase(); //category of subComponent
                //checking all collected properties in componentPropertyToName
				for(Property prop : componentPropertyToName.keySet()) {
					if(isApplicableToCat(prop, aSubCompCatName)) {

						//create a GenericAttribute object to pack the property
						verdict.vdm.vdm_data.GenericAttribute anAttribute = new verdict.vdm.vdm_data.GenericAttribute();

						String value = "";
						PropertyAcc propAcc = aSubComp.getPropertyValue(prop);
						PropertyExpression defPropExpr = prop.getDefaultValue();

						if(propAcc != null && !propAcc.getAssociations().isEmpty()) {
							value = getStrRepofPropVal(aSubComp.getPropertyValue(prop));
						} else if(defPropExpr != null) {
							value = getStrRepofExpr(defPropExpr)[0];
						}

						if (!value.equals("")){
							//setting the "name" and "value" field of anAttribute
							anAttribute.setName(componentPropertyToName.get(prop));
							anAttribute.setValue(value);

							//get the property type
							PropertyType propType = prop.getPropertyType();
							QName type = new QName("String");
							if(propType instanceof AadlBooleanImpl) {
								type = new QName("Bool");
							} else if(propType instanceof AadlIntegerImpl) {
								type = new QName("Int");
							} else if(propType instanceof EnumerationTypeImpl) {
								type = new QName("String");
							} else {
								if(!(propType instanceof AadlStringImpl)) {
									type = new QName(propType.toString());
								}
							}
							//parse propertyType fetched using prop.getOwnedPropertyType() and map it to "Bool", "Int", or "String"
							anAttribute.setType(type);



							//adding asAttribute to packSubComp
							packSubComp.getAttribute().add(anAttribute);
						}
					} else { //for outer if
						continue;
					}
				}

				//adding packSubComp to packBlockImpl
                packBlockImpl.getSubcomponent().add(packSubComp);
			}//End of adding all subcomponents
		
			//adding all connections to "connections" field of packBlockImpl
			if(aCompImpl.getOwnedConnections() != null && !aCompImpl.getOwnedConnections().isEmpty()) {
				for (Connection aConn : aCompImpl.getOwnedConnections()) {
					//to pack all information of a connection
					verdict.vdm.vdm_model.Connection packConn = new verdict.vdm.vdm_model.Connection();

									
					//populate connectionKind
					packConn.setConnectionKind(getConnectionKind(aConn));
					
					
					//variables to unpack information from AADL object
					String srcCompInstName = "";
					String destCompInstName = "";
					Context srcConnContext = aConn.getAllSourceContext();
					Context destConnContext = aConn.getAllDestinationContext();
					ConnectionEnd srcConnectionEnd = aConn.getAllSource();
    				ConnectionEnd destConnectionEnd = aConn.getAllDestination();

					if(srcConnContext != null) {
						srcCompInstName = srcConnContext.getName();
					}
					if(destConnContext != null) {
						destCompInstName = destConnContext.getName();
					}

    				String srcPortTypeName = "";
    				String destPortTypeName = "";
    				String srcPortName = srcConnectionEnd.getName();
    				String destPortName = destConnectionEnd.getName();
    				
    				
    				//variables to capture data type information
					DataSubcomponentType srcDataSubCompType = null;
					DataSubcomponentType destDataSubCompType = null;
//					BusSubcomponentType srcBusSubCompType = null;
//					BusSubcomponentType destBusSubCompType = null;
//					BusImplementation srcBusImpl = null;
//					BusImplementation destBusImpl = null;

    				if(srcConnectionEnd instanceof DataPort) {
    					srcPortTypeName = ((DataPort)srcConnectionEnd).isIn()?(((DataPort)srcConnectionEnd).isOut()? "inOut":"in"):"out";
    					srcDataSubCompType = ((DataPort)srcConnectionEnd).getDataFeatureClassifier();
    				} else if(srcConnectionEnd instanceof EventDataPort) {
    					srcPortTypeName = ((EventDataPort)srcConnectionEnd).isIn()?(((EventDataPort)srcConnectionEnd).isOut()? "inOut":"in"):"out";
    					srcDataSubCompType = ((EventDataPort)srcConnectionEnd).getDataFeatureClassifier();
    				} else if(srcConnectionEnd instanceof DataAccess) {
    					AccessType type = ((DataAccess) srcConnectionEnd).getKind();
    					if(type == AccessType.PROVIDES) {
    						srcPortTypeName = "providesDataAccess";
    					} else if(type == AccessType.REQUIRES) {
    						srcPortTypeName = "requiresDataAccess";
    					} else {
    						throw new RuntimeException("Unexpected access type: " + type);
    					}
    					srcDataSubCompType = ((DataAccess) srcConnectionEnd).getDataFeatureClassifier();
    				} else if(srcConnectionEnd instanceof DataSubcomponent){
    					srcDataSubCompType = ((DataSubcomponent)srcConnectionEnd).getDataSubcomponentType();
    					srcPortTypeName = "data";
    				} else if(srcConnectionEnd instanceof BusAccess) {
//    					AccessType type = ((BusAccess) srcConnectionEnd).getKind();
//    					if(type == AccessType.PROVIDES) {
//    						srcPortTypeName = "providesBusAccess";
//    					} else if(type == AccessType.REQUIRES) {
//    						srcPortTypeName = "requiresBusAccess";
//    					} else {
//    						throw new RuntimeException("Unexpected access type: " + type);
//    					}
//    					BusFeatureClassifier busfeatureClassifier = ((BusAccess) srcConnectionEnd).getBusFeatureClassifier();
//    					if(busfeatureClassifier instanceof BusImplementation) {
//    						srcBusImpl = (BusImplementation)busfeatureClassifier;
//    					}
    					System.out.println("Warning: Unsupported AADL component element type: " + srcConnectionEnd);
    					continue;
    				} else if(srcConnectionEnd instanceof BusSubcomponent){
//    					srcBusSubCompType = ((BusSubcomponent)srcConnectionEnd).getBusSubcomponentType();
//    					srcPortTypeName = "bus";
    					System.out.println("Warning: Unsupported AADL component element type: " + srcConnectionEnd);
    					continue;
    				} else if(srcConnectionEnd instanceof EventPort){
    					srcPortTypeName = ((EventPort)srcConnectionEnd).isIn()?(((EventPort)srcConnectionEnd).isOut()? "inOut":"in"):"out";
    				} else {
    					throw new RuntimeException("Unsupported AADL component element type: " + srcConnectionEnd+ "encountered while processing connections");
    				}

    				if(destConnectionEnd instanceof DataPort) {
    					destPortTypeName = ((DataPort)destConnectionEnd).isIn()?(((DataPort)destConnectionEnd).isOut()? "inOut":"in"):"out";
    					destDataSubCompType = ((DataPort)destConnectionEnd).getDataFeatureClassifier();
    				} else if(destConnectionEnd instanceof EventDataPort) {
    					destPortTypeName = ((EventDataPort)destConnectionEnd).isIn()?(((EventDataPort)destConnectionEnd).isOut()? "inOut":"in"):"out";
    					destDataSubCompType = ((EventDataPort)destConnectionEnd).getDataFeatureClassifier();
    				} else if(destConnectionEnd instanceof DataAccess) {
    					AccessType type = ((DataAccess) destConnectionEnd).getKind();
    					if(type == AccessType.PROVIDES) {
    						destPortTypeName = "providesDataAccess";
    					} else  if(type == AccessType.REQUIRES) {
    						destPortTypeName = "requiresDataAccess";
    					}
    					destDataSubCompType = ((DataAccess) destConnectionEnd).getDataFeatureClassifier();
    				}  else if(destConnectionEnd instanceof DataSubcomponent){
    					destDataSubCompType = ((DataSubcomponent)destConnectionEnd).getDataSubcomponentType();
    					destPortTypeName = "data";
    				} else if(destConnectionEnd instanceof BusAccess) {
//    					AccessType type = ((BusAccess) destConnectionEnd).getKind();
//    					if(type == AccessType.PROVIDES) {
//    						destPortTypeName = "providesBusAccess";
//    					} else if(type == AccessType.REQUIRES) {
//    						destPortTypeName = "requiresBusAccess";
//    					} else {
//    						throw new RuntimeException("Unexpected access type: " + type);
//    					}
//    					BusFeatureClassifier busfeatureClassifier = ((BusAccess) destConnectionEnd).getBusFeatureClassifier();
//    					if(busfeatureClassifier instanceof BusImplementation) {
//    						destBusImpl = (BusImplementation)busfeatureClassifier;
//    					}
    					System.out.println("Warning: Unsupported AADL component element type: " + destConnectionEnd);
    					continue;
    				}  else if(destConnectionEnd instanceof BusSubcomponent){
//    					destBusSubCompType = ((BusSubcomponent)destConnectionEnd).getBusSubcomponentType();
//    					destPortTypeName = "bus";
    					System.out.println("Warning: Unsupported AADL component element type: " + destConnectionEnd);
    					continue;
    				} else if(destConnectionEnd instanceof EventPort){
    					destPortTypeName = ((EventPort)destConnectionEnd).isIn()?(((EventPort)destConnectionEnd).isOut()? "inOut":"in"):"out";
    				} else {
    					throw new RuntimeException("Unsupported AADL component element type: " + destConnectionEnd+ "encountered while processing connections");
    				}

    				//setting name
    				packConn.setName(aConn.getFullName());
    				packConn.setQualifiedName(aConn.getQualifiedName());
    				
    				if(connectionToBusMap.containsKey(aConn.getQualifiedName())) {
    					packConn.setActualConnectionBinding(connectionToBusMap.get(aConn.getQualifiedName()));
    				}

    				//--- Populate packConn below ---

    				//to pack source
    				verdict.vdm.vdm_model.ConnectionEnd packSrcEnd = new verdict.vdm.vdm_model.ConnectionEnd();

					//to pack "componentPort"  of packSrcEnd
    				verdict.vdm.vdm_model.Port packSrcEndPort = new verdict.vdm.vdm_model.Port();
    				
//    				if(srcBusImpl != null) {
//    					packSrcEndPort = createVdmConnectionPort(srcPortName,srcPortTypeName, srcConnectionEnd.getQualifiedName(), srcBusImpl);
//    				} else if(srcBusSubCompType != null) {
//    					packSrcEndPort = createVdmConnectionPort(srcPortName,srcPortTypeName, srcConnectionEnd.getQualifiedName(), srcBusSubCompType);
//   				} else
    				if(srcConnectionEnd instanceof EventPort) {
    					packSrcEndPort = createVdmConnectionEventPort(srcPortName, srcPortTypeName, srcConnectionEnd.getQualifiedName());
    				} else {//if not a bus access port or bus implementation port or event port
    					packSrcEndPort = createVdmConnectionPort(srcPortName,srcPortTypeName, srcConnectionEnd.getQualifiedName(), srcDataSubCompType, m2, dataTypeDecl);
        			}	
    				

    				//If source port is independent of a component instance
    				if(srcCompInstName.equals("")) {
        				packSrcEnd.setComponentPort(packSrcEndPort);
    				} else {
    					//to pack "subcomponentPort" of packSrcEnd
        				verdict.vdm.vdm_model.CompInstancePort packSrcEndCompInstPort = new verdict.vdm.vdm_model.CompInstancePort();

        				//putting a reference to appropriate "subcomponent" from packBlockImpl in "subcomponent" of packSrcEndCompInstPort
        				for (verdict.vdm.vdm_model.ComponentInstance checkCompInst : packBlockImpl.getSubcomponent()) {
        					if(checkCompInst.getName().equals(srcCompInstName)) {

        						packSrcEndCompInstPort.setSubcomponent(checkCompInst);
        						break;
        					} else {
								continue;
							}
        				}
        				packSrcEndCompInstPort.setPort(packSrcEndPort);

        				//setting "subcomponentPort" of packSrcEnd
        				packSrcEnd.setSubcomponentPort(packSrcEndCompInstPort);
    				}

    				//adding to "source" of packConn
    				packConn.setSource(packSrcEnd);

    				//to pack destination
    				verdict.vdm.vdm_model.ConnectionEnd packDestEnd = new verdict.vdm.vdm_model.ConnectionEnd();

					//to pack "componentPort"  of packDestEnd
    				verdict.vdm.vdm_model.Port packDestEndPort = new verdict.vdm.vdm_model.Port();
//    				if(destBusImpl != null){
//    					packDestEndPort = createVdmConnectionPort(destPortName,destPortTypeName, destConnectionEnd.getQualifiedName(), destBusImpl);
//    				} else if(destBusSubCompType != null){
//    					packDestEndPort = createVdmConnectionPort(destPortName,destPortTypeName, destConnectionEnd.getQualifiedName(), destBusSubCompType);
//    				} else 
    				if(destConnectionEnd instanceof EventPort) {
    					packDestEndPort = createVdmConnectionEventPort(destPortName,destPortTypeName, destConnectionEnd.getQualifiedName());
    				} else {//if not a bus access port or bus implementation port or eventport
    					packDestEndPort = createVdmConnectionPort(destPortName,destPortTypeName, destConnectionEnd.getQualifiedName(), destDataSubCompType, m2, dataTypeDecl);
    				}
    				
    				//If source port is independent of a component instance
    				if(destCompInstName.equals("")) {
        				packDestEnd.setComponentPort(packDestEndPort);
    				} else {
    					//to pack "subcomponentPort" of packSrcEnd
        				verdict.vdm.vdm_model.CompInstancePort packDestEndCompInstPort = new verdict.vdm.vdm_model.CompInstancePort();

        				//putting a reference to appropriate "subcomponent" from packBlockImpl in "subcomponent" of packSrcEndCompInstPort
        				for (verdict.vdm.vdm_model.ComponentInstance checkCompInst : packBlockImpl.getSubcomponent()) {
        					if(checkCompInst.getName().equals(destCompInstName)) {

        						packDestEndCompInstPort.setSubcomponent(checkCompInst);
        						break;
        					} else {
								continue;
							}
        				}
        				packDestEndCompInstPort.setPort(packDestEndPort);

        				//setting "subcomponentPort" of packDestEnd
        				packDestEnd.setSubcomponentPort(packDestEndCompInstPort);
    				}

    				//adding to "source" of packConn
    				packConn.setDestination(packDestEnd);


                    //adding connection properties from connProperty.ToName
    				for(Property prop : connPropertyToName.keySet()) {

						//create a GenericAttribute object to pack the property
						verdict.vdm.vdm_data.GenericAttribute aConnAttribute = new verdict.vdm.vdm_data.GenericAttribute();

    					String value = "";
						PropertyAcc propAcc = aConn.getPropertyValue(prop);
						PropertyExpression defPropExpr = prop.getDefaultValue();

						if(propAcc != null && !propAcc.getAssociations().isEmpty()) {
							value = getStrRepofPropVal(propAcc);
						} else if(defPropExpr != null) {
							value = getStrRepofExpr(defPropExpr)[0];
						}

						if (!value.equals("")){
							//setting the "name" and "value" field of anAttribute
							aConnAttribute.setName(connPropertyToName.get(prop));
							aConnAttribute.setValue(value);

							PropertyType propType = prop.getPropertyType();
							QName type = new QName("String");
							if(propType instanceof AadlBooleanImpl) {
								type = new QName("Bool");
							} else if(propType instanceof AadlIntegerImpl) {
								type = new QName("Int");
							} else if(propType instanceof EnumerationTypeImpl) {
								type = new QName("String");
							} else { 
								if(!(propType instanceof AadlStringImpl)) {
									type = new QName(propType.toString());
								}
							}
							//parse propertyType fetched using prop.getOwnedPropertyType() and map it to "Bool", "Int", or "String"
							aConnAttribute.setType(type);




							//adding asAttribute to packSubComp
							packConn.getAttribute().add(aConnAttribute);
						}
    				}


    				if(aConn.isBidirectional()) {
    					packConn.setDirection(verdict.vdm.vdm_model.Direction.fromValue("bidirectional"));
    					
    					//to pack reverse connection
    					verdict.vdm.vdm_model.Connection packReverseConn = new verdict.vdm.vdm_model.Connection();
    					packReverseConn.setName(packConn.getName() + "_reverse");
    					packReverseConn.setSource(packConn.getDestination());
    					packReverseConn.setDestination(packConn.getSource());
    					for (verdict.vdm.vdm_data.GenericAttribute anAttribute : packConn.getAttribute()) {
    						packReverseConn.getAttribute().add(anAttribute);
    					}
    					packReverseConn.setDirection(verdict.vdm.vdm_model.Direction.fromValue("bidirectional"));
    					
    					//add packReverseConn to packBlockImpl
    					packBlockImpl.getConnection().add(packReverseConn);
    				} else {
    					packConn.setDirection(verdict.vdm.vdm_model.Direction.fromValue("unidirectional"));
    				}

					//add packConn to packBlockImpl
					packBlockImpl.getConnection().add(packConn);
				}
			}//End of adding all connections

			//setting "blackImpl" field of packCompImpl
			packCompImpl.setBlockImpl(packBlockImpl);
		}//End of Getting the reference

		//return populated Model
		return m2;
	}//End of translateSystemImplObjects



/** AUXILIARY FUNCTIONS */

	/**
	 * packs an aadl event as a Vdm event
	 * @param anEvent
	 * @return
	 */
	verdict.vdm.vdm_model.Event createVdmEvent(Event anEvent){
		verdict.vdm.vdm_model.Event packEvent= new verdict.vdm.vdm_model.Event();

		String id = sanitizeValue(anEvent.getId());
		String probability = sanitizeValue(anEvent.getProbability().getProp());
		String comment = sanitizeValue(anEvent.getComment());
		String description = sanitizeValue(anEvent.getDescription());

//ISSUE: "name" field missing in com.ge.research.osate.verdict.dsl.verdict.Event class and superclass
//		packEvent.setName("Not found");


		packEvent.setId(id);
		packEvent.setProbability(probability);
		packEvent.setComment(comment);
		packEvent.setDescription(description);

		return packEvent;
	}


	/**
	 * packs an aadl cyber relation as a vdm cyber relation
	 * @param aCyberRel
	 * @return
	 */
	verdict.vdm.vdm_model.CyberRel createVdmCyberRel(CyberRel aCyberRel){
		//To pack the cyberRel as a VDM event
		verdict.vdm.vdm_model.CyberRel packCyberRel = new verdict.vdm.vdm_model.CyberRel();

        String id = sanitizeValue(aCyberRel.getId());
        String comment = sanitizeValue(aCyberRel.getComment());
        String description = sanitizeValue(aCyberRel.getDescription());
        String outPort = aCyberRel.getOutput().getValue().getPort();
        String outCia = aCyberRel.getOutput().getValue().getCia().getLiteral();
        verdict.vdm.vdm_model.CIAPort output = createVdmCIAPort(outPort, outCia);

        if(aCyberRel.getInputs() != null) {
            verdict.vdm.vdm_model.CyberExpr input = createVdmCyberExpr(aCyberRel.getInputs().getValue());
            packCyberRel.setInputs(input);
        }


        packCyberRel.setId(id);
        packCyberRel.setComment(comment);
        packCyberRel.setDescription(description);
        packCyberRel.setOutput(output);
        //ISSUE: "name", "phases" and "extern" fields missing in com.ge.research.osate.verdict.dsl.verdict.CyberRel class and superclass

		return packCyberRel;
	}


    /**
     * packs an aadl safety relation as a Vdm safety relation
     * @param aSafetyRel
     * @return
     */
	verdict.vdm.vdm_model.SafetyRel createVdmSafetyRel(SafetyRel aSafetyRel){
		//To pack the safetyRel as a VDM event
		verdict.vdm.vdm_model.SafetyRel packSafetyRel = new verdict.vdm.vdm_model.SafetyRel();

        String id = sanitizeValue(aSafetyRel.getId());
        String comment = sanitizeValue(aSafetyRel.getComment());
        String description = sanitizeValue(aSafetyRel.getDescription());
        String outPort = aSafetyRel.getOutput().getValue().getPort();
        String outIa = aSafetyRel.getOutput().getValue().getIa().getLiteral();
        verdict.vdm.vdm_model.IAPort output = createVdmIAPort(outPort, outIa);

        if(aSafetyRel.getFaultSrc() != null) {

            verdict.vdm.vdm_model.SafetyRelExpr faultSrc = createVdmSafetyRelExpr(aSafetyRel.getFaultSrc().getValue());
            packSafetyRel.setFaultSrc(faultSrc);
        }



        packSafetyRel.setId(id);
        packSafetyRel.setComment(comment);
        packSafetyRel.setDescription(description);
        packSafetyRel.setOutput(output);
        //ISSUE: "name", "phases" and "extern" fields missing in com.ge.research.osate.verdict.dsl.verdict.CyberRel class and superclass

		return packSafetyRel;
	}


    /**
     * packs an aadl safety requirement as a Vdm safety requirement
     * @param aSafetyReq
     * @return
     */
	verdict.vdm.vdm_model.SafetyReq createVdmSafetyReq(SafetyReq aSafetyReq, String compType) {
		//To pack the safettReq as a VDM event
		verdict.vdm.vdm_model.SafetyReq packSafetyReq = new verdict.vdm.vdm_model.SafetyReq();

        String id = sanitizeValue(aSafetyReq.getId());
        String comment = sanitizeValue(aSafetyReq.getComment());
        String description = sanitizeValue(aSafetyReq.getDescription());
        String targetProbability = aSafetyReq.getSeverity().getTargetLikelihood().toString();


        if(aSafetyReq.getCondition() != null) {

            verdict.vdm.vdm_model.SafetyReqExpr condition = createVdmSafetyReqExpr(aSafetyReq.getCondition().getValue());
            packSafetyReq.setCondition(condition);
        }


        packSafetyReq.setId(id);
        packSafetyReq.setComment(comment);
        packSafetyReq.setDescription(description);
        packSafetyReq.setTargetProbability(targetProbability);
        //ISSUE: "name", "phases" and "extern" fields missing in com.ge.research.osate.verdict.dsl.verdict.CyberRel class and superclass

        if(aSafetyReq.getJustification() != null) {
            packSafetyReq.setJustification(aSafetyReq.getJustification());
        }

        if(aSafetyReq.getAssumption() != null) {
            packSafetyReq.setAssumption(aSafetyReq.getAssumption());
        }

        if(aSafetyReq.getStrategy() != null) {
            packSafetyReq.setStrategy(aSafetyReq.getStrategy());
        }

		packSafetyReq.setCompType(compType);

        return packSafetyReq;
	}


	/**
	 * packs an aadl cyber requirement as a Vdm cyber requirement
	 * @param aCyberReq
	 * @return
	 */
	verdict.vdm.vdm_model.CyberReq createVdmCyberReq(CyberReq aCyberReq, String compType) {
		//To pack the safettReq as a VDM event
		verdict.vdm.vdm_model.CyberReq packCyberReq = new verdict.vdm.vdm_model.CyberReq();

        String id = sanitizeValue(aCyberReq.getId());
        String comment = sanitizeValue(aCyberReq.getComment());
        String description = sanitizeValue(aCyberReq.getDescription());
        verdict.vdm.vdm_model.Severity severity =  convertToVdmSeverity(aCyberReq.getSeverity().toString());

        if(aCyberReq.getCondition() != null) {

            verdict.vdm.vdm_model.CyberExpr condition = createVdmCyberExpr(aCyberReq.getCondition().getValue());
            packCyberReq.setCondition(condition);
        }

        if (aCyberReq.getCia().getLiteral() != null) {
        	verdict.vdm.vdm_model.CIA cia = convertToVdmCia(aCyberReq.getCia().getLiteral());
        	packCyberReq.setCia(cia);
        }

        packCyberReq.setId(id);
        packCyberReq.setComment(comment);
        packCyberReq.setDescription(description);
        packCyberReq.setSeverity(severity);
        //ISSUE: "name", "phases" and "extern" fields missing in com.ge.research.osate.verdict.dsl.verdict.CyberRel class and superclass

        if(aCyberReq.getJustification() != null) {
            packCyberReq.setJustification(aCyberReq.getJustification());
        }

        if(aCyberReq.getAssumption() != null) {
            packCyberReq.setAssumption(aCyberReq.getAssumption());
        }

        if(aCyberReq.getStrategy() != null) {
            packCyberReq.setStrategy(aCyberReq.getStrategy());
        }

		packCyberReq.setCompType(compType);


        return packCyberReq;
	}


	/**
	 * packs an aadl mission requirement as a Vdm mission requirement
	 * @param aMission
	 * @return
	 */
	verdict.vdm.vdm_model.Mission createVdmMission(CyberMission aMission){
		//To pack the safettReq as a VDM event
		verdict.vdm.vdm_model.Mission packMission = new verdict.vdm.vdm_model.Mission();

        String id = sanitizeValue(aMission.getId());
        String description = sanitizeValue(aMission.getDescription());
        EList<String> missionCyberReqs= aMission.getCyberReqs();
        packMission.setId(id);
        packMission.setDescription(description);

        if(aMission.getJustification() != null) {
            packMission.setJustification(aMission.getJustification());
        }

        if(aMission.getAssumption() != null) {
            packMission.setAssumption(aMission.getAssumption());
        }

        if(aMission.getStrategy() != null) {
            packMission.setStrategy(aMission.getStrategy());
        }


        for (String CyberReq : missionCyberReqs) {
        	packMission.getCyberReqs().add(CyberReq);
        }

        //ISSUE: "comment" and "name" fields missing in com.ge.research.osate.verdict.dsl.verdict.CyberMission class and superclass

		return packMission;
	}


	/**
	 * creates a Vdm CyberExpr object and returns
	 * @param expr
	 * @return
	 */
	verdict.vdm.vdm_model.CyberExpr createVdmCyberExpr(LExpr expr){
		//to pack the CyberExpr and return
		verdict.vdm.vdm_model.CyberExpr packCyberExpr= new verdict.vdm.vdm_model.CyberExpr();

		//--------- variables for debugging
		List<String> allPortNames = new ArrayList<>();
		List<String> allPortCIAs = new ArrayList<>();

    	if(expr instanceof LOr) { //HAS to be an LOr since LExpr can only be an LOr
    		if(((LOr)expr).getExprs().size() == 1){      //Is a solo disjunct


    			/**
    			 * If it is a single disjunct, just send it to handleAndCyberExpr
    			 * and return the same package returned by handleAndCyberExpr
    			 */
    			LAnd soloAndExpr = ((LOr)expr).getExprs().get(0);
    			packCyberExpr = handleAndCyberExpr(soloAndExpr, allPortNames, allPortCIAs);

    		} else if (((LOr)expr).getExprs().size() > 1) {
        		verdict.vdm.vdm_model.CyberExprKind kind = verdict.vdm.vdm_model.CyberExprKind.fromValue("Or");
                packCyberExpr.setKind(kind); //setting "kind" of expression

                //to pack all disjunct subexpressions in a single list
                verdict.vdm.vdm_model.CyberExprList packAndList= new verdict.vdm.vdm_model.CyberExprList();

        		for(LAnd andExpr : ((LOr)expr).getExprs()) { //for each disjunct (each disjunct is a LAnd)
        			//to pack the CyberExpr for this disjunct
        			verdict.vdm.vdm_model.CyberExpr packDisjunctCyberExpr= handleAndCyberExpr(andExpr, allPortNames, allPortCIAs);

        			//adding to the list of disjuncts
        			packAndList.getExpr().add(packDisjunctCyberExpr);
    			}

        		//setting the "or" field of packCyberExpr
        		packCyberExpr.setOr(packAndList);
    		}
    	} else {
    		throw new RuntimeException("Warning -- LExpr should be an LOr"); //Should never occur, but keeping for sanity
    	}


		//return the CyberExpr
		return packCyberExpr;
	}


	/**
	 * to handle LAnd expressions while creating a CyberExpr object
	 * @param andExpr
	 * @param allPortNames
	 * @param allPortCIAs
	 * @return
	 */
	verdict.vdm.vdm_model.CyberExpr handleAndCyberExpr(LAnd andExpr, List<String> allPortNames, List<String> allPortCIAs){
		//to pack this CyberExpr
		verdict.vdm.vdm_model.CyberExpr packCyberExpr = new verdict.vdm.vdm_model.CyberExpr();


		if(andExpr.getExprs().size() == 1){      //Is a Port or a Not


			LExpr subAndExpr = andExpr.getExprs().get(0);

    		if(subAndExpr instanceof LPort) {


    			verdict.vdm.vdm_model.CIAPort port = handleCIAPort(subAndExpr, allPortNames, allPortCIAs);

    			//setting "port" field of packCyberExp
    			packCyberExpr.setPort(port);

    		} else if(subAndExpr instanceof LNot) {


	            verdict.vdm.vdm_model.CyberExprKind kind = verdict.vdm.vdm_model.CyberExprKind.fromValue("Not");
	            packCyberExpr.setKind(kind); //setting "kind" of expression

    			//send to handler for not expressions
    			verdict.vdm.vdm_model.CyberExpr packNotCyberExpr= handleNotCyberExpr((LNot)subAndExpr, allPortNames, allPortCIAs);

    			//set the "not" of packCyberExpr
    			packCyberExpr.setNot(packNotCyberExpr);
    		} else if(subAndExpr instanceof LOr) {


    			/**
    			 * If it is a single conjunct, just send it to handleOrCyberExpr
    			 * and return the same package returned by handleAndCyberExpr
    			 */
    			packCyberExpr = handleOrCyberExpr(subAndExpr, allPortNames, allPortCIAs);
    		}
		} else if(andExpr.getExprs().size() > 1){ //collection of conjuncts


            verdict.vdm.vdm_model.CyberExprKind kind = verdict.vdm.vdm_model.CyberExprKind.fromValue("And");
            packCyberExpr.setKind(kind); //setting "kind" of expression

            //to pack all conjunct subexpressions in a single list
            verdict.vdm.vdm_model.CyberExprList packOrList= new verdict.vdm.vdm_model.CyberExprList();

    		for(LExpr expr : andExpr.getExprs()) { //for each conjunct (each disjunct is a LExpr)
    			//to pack the CyberExpr for this conjunct
    			verdict.vdm.vdm_model.CyberExpr packConjunctCyberExpr= handleOrCyberExpr(expr, allPortNames, allPortCIAs);

    			//adding to the list of conjuncts
    			packOrList.getExpr().add(packConjunctCyberExpr);
			}

    		//setting the "or" field of packCyberExpr
    		packCyberExpr.setAnd(packOrList);
		}

		return packCyberExpr;
	}


	/**
	 * to handle LOr expressions while creating a CyberExpr object
	 * @param orExpr
	 * @param allPortNames
	 * @param allPortCIAs
	 * @return
	 */
	verdict.vdm.vdm_model.CyberExpr handleOrCyberExpr(LExpr orExpr, List<String> allPortNames, List<String> allPortCIAs){
		//to pack this CyberExpr
		verdict.vdm.vdm_model.CyberExpr packCyberExpr = new verdict.vdm.vdm_model.CyberExpr();



		if(orExpr instanceof LPort) {


			verdict.vdm.vdm_model.CIAPort port = handleCIAPort(orExpr, allPortNames, allPortCIAs);

			//setting "port" field of packCyberExp
			packCyberExpr.setPort(port);
		} else if(orExpr instanceof LNot) {


            verdict.vdm.vdm_model.CyberExprKind kind = verdict.vdm.vdm_model.CyberExprKind.fromValue("Not");
            packCyberExpr.setKind(kind); //setting "kind" of expression

			//send to handler for Not expression
			verdict.vdm.vdm_model.CyberExpr packNotCyberExpr= handleNotCyberExpr((LNot)orExpr, allPortNames, allPortCIAs);

			//set the "not" of packCyberExpr
			packCyberExpr.setNot(packNotCyberExpr);
		} else if(((LOr)orExpr).getExprs().size() == 1){      //Is a solo disjunct


			/**
			 * If it is a single disjunct, just send it to handleAndCyberExpr
			 * and return the same package returned by handleAndCyberExpr
			 */
			LAnd soloAndExpr = ((LOr)orExpr).getExprs().get(0);
			packCyberExpr = handleAndCyberExpr(soloAndExpr, allPortNames, allPortCIAs);
		} else if (((LOr)orExpr).getExprs().size() > 1) {


    		verdict.vdm.vdm_model.CyberExprKind kind = verdict.vdm.vdm_model.CyberExprKind.fromValue("Or");
            packCyberExpr.setKind(kind); //setting "kind" of expression

            //to pack all disjunct subexpressions in a single list
            verdict.vdm.vdm_model.CyberExprList packAndList= new verdict.vdm.vdm_model.CyberExprList();

    		for(LAnd andExpr : ((LOr)orExpr).getExprs()) { //for each disjunct (each disjunct is a LAnd)
    			//to pack the CyberExpr for this disjunct
    			verdict.vdm.vdm_model.CyberExpr packDisjunctCyberExpr= handleAndCyberExpr(andExpr, allPortNames, allPortCIAs);

    			//adding to the list of disjuncts
    			packAndList.getExpr().add(packDisjunctCyberExpr);
			}

    		//setting the "and" field of packCyberExpr
    		packCyberExpr.setOr(packAndList);
		}

		return packCyberExpr;
	}

	/**
	 * to handle LNot expressions while creating a CyberExpr object
	 * @param notExpr
	 * @param allPortNames
	 * @param allPortCIAs
	 * @return
	 */
	verdict.vdm.vdm_model.CyberExpr handleNotCyberExpr(LNot notExpr, List<String> allPortNames, List<String> allPortCIAs){
		//to pack this CyberExpr
		verdict.vdm.vdm_model.CyberExpr packCyberExpr = new verdict.vdm.vdm_model.CyberExpr();



		if(notExpr.getExpr() instanceof LPort){


			verdict.vdm.vdm_model.CIAPort port = handleCIAPort(notExpr.getExpr(), allPortNames, allPortCIAs);

			//setting "port" field of packCyberExp
			packCyberExpr.setPort(port);

		} else if(notExpr.getExpr() instanceof LOr){


			packCyberExpr = handleOrCyberExpr(notExpr.getExpr(), allPortNames, allPortCIAs);

		} else if(notExpr.getExpr() instanceof LAnd){


			packCyberExpr = handleAndCyberExpr((LAnd)notExpr.getExpr(), allPortNames, allPortCIAs);
		}

		return packCyberExpr;
	}


	/**
	 * to create a CIAPort object for a CyberExpr object
	 * @param portExpr
	 * @param allPortNames
	 * @param allPortCIAs
	 * @return
	 */
	verdict.vdm.vdm_model.CIAPort handleCIAPort(LExpr portExpr, List<String> allPortNames, List<String> allPortCIAs){

		String portName = ((LPort)portExpr).getPort();
		String portCia =  ((LPort)portExpr).getCia().getLiteral();

		//to pack this Port
		verdict.vdm.vdm_model.CIAPort port = createVdmCIAPort(portName, portCia);

		//---------for debugging
		allPortNames.add(((LPort)portExpr).getPort());
		allPortCIAs.add(convertAbbreviation(((LPort)portExpr).getCia().getLiteral()));

		return port;
	}


	/**
	 * creates a Vdm SafetyReqExpr object and returns
	 * @param expr
	 * @return
	 */
	verdict.vdm.vdm_model.SafetyReqExpr createVdmSafetyReqExpr(SLExpr expr){
		//to pack the SafetyReqExpr and return
		verdict.vdm.vdm_model.SafetyReqExpr packSafetyReqExpr= new verdict.vdm.vdm_model.SafetyReqExpr();

		//---------variables for debugging
		List<String> allPortNames = new ArrayList<>();
		List<String> allPortCIAs = new ArrayList<>();

    	if(expr instanceof SLOr) { //HAS to be an LOr since SLExpr can only be an SLOr
    		if(((SLOr)expr).getExprs().size() == 1){      //Is a solo disjunct


    			/**
    			 * If it is a single disjunct, just send it to handleAndCyberExpr
    			 * and return the same package returned by handleAndCyberExpr
    			 */
    			SLAnd soloAndExpr = ((SLOr)expr).getExprs().get(0);
    			packSafetyReqExpr = handleAndSafetyReqExpr(soloAndExpr, allPortNames, allPortCIAs);

    		} else if (((SLOr)expr).getExprs().size() > 1) {
        		verdict.vdm.vdm_model.SafetyReqExprKind kind = verdict.vdm.vdm_model.SafetyReqExprKind.fromValue("Or");
                packSafetyReqExpr.setKind(kind); //setting "kind" of expression

                //to pack all disjunct subexpressions in a single list
                verdict.vdm.vdm_model.SafetyReqExprList packAndList= new verdict.vdm.vdm_model.SafetyReqExprList();

        		for(SLAnd andExpr : ((SLOr)expr).getExprs()) { //for each disjunct (each disjunct is a LAnd)
        			//to pack the SafetyReqExpr for this disjunct
        			verdict.vdm.vdm_model.SafetyReqExpr packDisjunctSafetyReqExpr= handleAndSafetyReqExpr(andExpr, allPortNames, allPortCIAs);

        			//adding to the list of disjuncts
        			packAndList.getExpr().add(packDisjunctSafetyReqExpr);
    			}

        		//setting the "and" field of packSafetyReq
        		packSafetyReqExpr.setOr(packAndList);
    		}
    	} else {
    		throw new RuntimeException("Warning -- LExpr should be an LOr"); //Should never occur, but keeping for sanity
    	}


		return packSafetyReqExpr;
	}

	/**
	 * to handle LAnd expressions while creating a SafetyReqExpr object
	 * @param andExpr
	 * @param allPortNames
	 * @param allPortCIAs
	 * @return
	 */
	verdict.vdm.vdm_model.SafetyReqExpr handleAndSafetyReqExpr(SLAnd andExpr, List<String> allPortNames, List<String> allPortCIAs){
		//to pack this SafetyReqExpr
		verdict.vdm.vdm_model.SafetyReqExpr packSafetyReqExpr = new verdict.vdm.vdm_model.SafetyReqExpr();



		if(andExpr.getExprs().size() == 1){      //Is a Port or a Not


			SLExpr subAndExpr = andExpr.getExprs().get(0);

    		if(subAndExpr instanceof SLPort) {


    			verdict.vdm.vdm_model.IAPort port = handleIAPort(subAndExpr, allPortNames, allPortCIAs);

    			//setting "port" field of packSafetyReq
    			packSafetyReqExpr.setPort(port);
    		} else if(subAndExpr instanceof SLNot) {


	            verdict.vdm.vdm_model.SafetyReqExprKind kind = verdict.vdm.vdm_model.SafetyReqExprKind.fromValue("Not");
	            packSafetyReqExpr.setKind(kind); //setting "kind" of expression

    			//to pack the SafetyReqExpr for the Not
    			verdict.vdm.vdm_model.SafetyReqExpr packNotSafetyReqExpr= handleNotSafetyReqExpr((SLNot)subAndExpr, allPortNames, allPortCIAs);

    			//set the "not" of packSafetyReqExpr
    			packSafetyReqExpr.setNot(packNotSafetyReqExpr);
    		} else if(subAndExpr instanceof SLOr) {


    			/**
    			 * If it is a single conjunct, just send it to handleOrCyberExpr
    			 * and return the same package returned by handleAndCyberExpr
    			 */
    			packSafetyReqExpr = handleOrSafetyReqExpr(subAndExpr, allPortNames, allPortCIAs);
    		}
		} else if(andExpr.getExprs().size() > 1){ //collection of conjuncts


            verdict.vdm.vdm_model.SafetyReqExprKind kind = verdict.vdm.vdm_model.SafetyReqExprKind.fromValue("And");
            packSafetyReqExpr.setKind(kind); //setting "kind" of expression

            //to pack all conjunct subexpressions in a single list
            verdict.vdm.vdm_model.SafetyReqExprList packOrList= new verdict.vdm.vdm_model.SafetyReqExprList();

    		for(SLExpr expr : andExpr.getExprs()) { //for each conjunct (each disjunct is a LExpr)
    			//to pack the SafetyReqExpr for this conjunct
    			verdict.vdm.vdm_model.SafetyReqExpr packConjunctSafetyReqExpr= handleOrSafetyReqExpr(expr, allPortNames, allPortCIAs);

    			//adding to the list of conjuncts
    			packOrList.getExpr().add(packConjunctSafetyReqExpr);
			}

    		//setting the "or" field of packSafetyReqExpr
    		packSafetyReqExpr.setAnd(packOrList);
		}

		return packSafetyReqExpr;
	}


	/**
	 * to handle LOr expressions while creating a SafetyReqExpr object
	 * @param orExpr
	 * @param allPortNames
	 * @param allPortCIAs
	 * @return
	 */
	verdict.vdm.vdm_model.SafetyReqExpr handleOrSafetyReqExpr(SLExpr orExpr, List<String> allPortNames, List<String> allPortCIAs){
		//to pack this SafetyReqExpr
		verdict.vdm.vdm_model.SafetyReqExpr packSafetyReqExpr = new verdict.vdm.vdm_model.SafetyReqExpr();



		if(orExpr instanceof SLPort) {


			verdict.vdm.vdm_model.IAPort port = handleIAPort(orExpr, allPortNames, allPortCIAs);

			//setting "port" field of packSafetyReqExp
			packSafetyReqExpr.setPort(port);

		} else if(orExpr instanceof SLNot) {


            verdict.vdm.vdm_model.SafetyReqExprKind kind = verdict.vdm.vdm_model.SafetyReqExprKind.fromValue("Not");
            packSafetyReqExpr.setKind(kind); //setting "kind" of expression

			//to pack the SafetyReqExpr for the Not
			verdict.vdm.vdm_model.SafetyReqExpr packNotCyberExpr= handleNotSafetyReqExpr((SLNot)orExpr, allPortNames, allPortCIAs);

			//set the "not" of packSafetyReqExpr
			packSafetyReqExpr.setNot(packNotCyberExpr);
		} else if(((SLOr)orExpr).getExprs().size() == 1){      //Is a solo disjunct


			/**
			 * If it is a single disjunct, just send it to handleAndCyberExpr
			 * and return the same package returned by handleAndCyberExpr
			 */
			SLAnd soloAndExpr = ((SLOr)orExpr).getExprs().get(0);
			packSafetyReqExpr = handleAndSafetyReqExpr(soloAndExpr, allPortNames, allPortCIAs);

		} else if (((SLOr)orExpr).getExprs().size() > 1) {


    		verdict.vdm.vdm_model.SafetyReqExprKind kind = verdict.vdm.vdm_model.SafetyReqExprKind.fromValue("Or");
            packSafetyReqExpr.setKind(kind); //setting "kind" of expression

            //to pack all disjunct subexpressions in a single list
            verdict.vdm.vdm_model.SafetyReqExprList packAndList= new verdict.vdm.vdm_model.SafetyReqExprList();

    		for(SLAnd andExpr : ((SLOr)orExpr).getExprs()) { //for each disjunct (each disjunct is a LAnd)
    			//to pack the SafetyReqExpr for this disjunct
    			verdict.vdm.vdm_model.SafetyReqExpr packDisjunctSafetyReqExpr= handleAndSafetyReqExpr(andExpr, allPortNames, allPortCIAs);

    			//adding to the list of disjuncts
    			packAndList.getExpr().add(packDisjunctSafetyReqExpr);
			}

    		//setting the "and" field of packSafetyReqExpr
    		packSafetyReqExpr.setOr(packAndList);
		}

		return packSafetyReqExpr;
	}

	/**
	 * to handle LNot expressions while creating a SafetyReqExpr object
	 * @param notExpr
	 * @param allPortNames
	 * @param allPortCIAs
	 * @return
	 */
	verdict.vdm.vdm_model.SafetyReqExpr handleNotSafetyReqExpr(SLNot notExpr, List<String> allPortNames, List<String> allPortCIAs){
		//to pack this SafetyReqExpr
		verdict.vdm.vdm_model.SafetyReqExpr packSafetyReqExpr = new verdict.vdm.vdm_model.SafetyReqExpr();



		if(notExpr.getExpr() instanceof SLPort){


			verdict.vdm.vdm_model.IAPort port = handleIAPort(notExpr.getExpr(), allPortNames, allPortCIAs);

			//setting "port" field of packSafetyReqExp
			packSafetyReqExpr.setPort(port);

		} else if(notExpr.getExpr() instanceof SLOr){


			packSafetyReqExpr = handleOrSafetyReqExpr(notExpr.getExpr(), allPortNames, allPortCIAs);

		} else if(notExpr.getExpr() instanceof SLAnd){


			packSafetyReqExpr = handleAndSafetyReqExpr((SLAnd)notExpr.getExpr(), allPortNames, allPortCIAs);
		}

		return packSafetyReqExpr;
	}


	/**
	 * to create a IAPort object for a SafetyReqExpr or
	 * a SafetyRelExpr object
	 * @param portExpr
	 * @param allPortNames
	 * @param allPortCIAs
	 * @return
	 */
	verdict.vdm.vdm_model.IAPort handleIAPort(SLExpr portExpr, List<String> allPortNames, List<String> allPortCIAs){

		String portName = ((SLPort)portExpr).getPort();
		String portCia =  ((SLPort)portExpr).getIa().getLiteral();

		//to pack this Port
		verdict.vdm.vdm_model.IAPort port = createVdmIAPort(portName, portCia);

		//for testing --- delete later
		allPortNames.add(((SLPort)portExpr).getPort());
		allPortCIAs.add(convertAbbreviation(((SLPort)portExpr).getIa().getLiteral()));

		return port;
	}


	/**
	 * creates a Vdm SafetyRelExpr object and returns
	 * @param expr
	 * @return
	 */
	verdict.vdm.vdm_model.SafetyRelExpr createVdmSafetyRelExpr(SLExpr expr){
		//to pack the SafetyRelExpr and return
		verdict.vdm.vdm_model.SafetyRelExpr packSafetyRelExpr= new verdict.vdm.vdm_model.SafetyRelExpr();

		//---------variables for testing---- will remove when testing is over
		List<String> allPortNames = new ArrayList<>();
		List<String> allPortCIAs = new ArrayList<>();

    	if(expr instanceof SLOr) { //HAS to be an LOr since LExpr can only be an LOr
    		if(((SLOr)expr).getExprs().size() == 1){      //Is a solo disjunct

    			/**
    			 * If it is a single disjunct, just send it to handleAndCyberExpr
    			 * and return the same package returned by handleAndCyberExpr
    			 */
    			SLAnd soloAndExpr = ((SLOr)expr).getExprs().get(0);
    			packSafetyRelExpr = handleAndSafetyRelExpr(soloAndExpr, allPortNames, allPortCIAs);

    		} else if (((SLOr)expr).getExprs().size() > 1) {
        		verdict.vdm.vdm_model.SafetyRelExprKind kind = verdict.vdm.vdm_model.SafetyRelExprKind.fromValue("Or");
                packSafetyRelExpr.setKind(kind); //setting "kind" of expression

                //to pack all disjunct subexpressions in a single list
                verdict.vdm.vdm_model.SafetyRelExprList packAndList= new verdict.vdm.vdm_model.SafetyRelExprList();

        		for(SLAnd andExpr : ((SLOr)expr).getExprs()) { //for each disjunct (each disjunct is a LAnd)
        			//to pack the SafetyRelExpr for this disjunct
        			verdict.vdm.vdm_model.SafetyRelExpr packDisjunctSafetyRelExpr= handleAndSafetyRelExpr(andExpr, allPortNames, allPortCIAs);

        			//adding to the list of disjuncts
        			packAndList.getExpr().add(packDisjunctSafetyRelExpr);
    			}

        		//setting the "and" field of packSafetyRelExpr
        		packSafetyRelExpr.setOr(packAndList);
    		}
    	} else {
    		throw new RuntimeException("Warning -- LExpr should be an LOr"); //Should never occur, but keeping for sanity
    	}




		return packSafetyRelExpr;
	}

	/**
	 * to handle LAnd expressions while creating a SafetyRelExpr object
	 * @param andExpr
	 * @param allPortNames
	 * @param allPortCIAs
	 * @return
	 */
	verdict.vdm.vdm_model.SafetyRelExpr handleAndSafetyRelExpr(SLAnd andExpr, List<String> allPortNames, List<String> allPortCIAs){
		//to pack this SafetyRelExpr
		verdict.vdm.vdm_model.SafetyRelExpr packSafetyRelExpr = new verdict.vdm.vdm_model.SafetyRelExpr();



		if(andExpr.getExprs().size() == 1){      //Is a Port or a Not or a Happens or a Not


			SLExpr subAndExpr = andExpr.getExprs().get(0);

    		if(subAndExpr instanceof SLPort) {


    			verdict.vdm.vdm_model.IAPort port = handleIAPort(subAndExpr, allPortNames, allPortCIAs);

    			//setting "port" field of packSafetyRelExpr
    			packSafetyRelExpr.setPort(port);
    		} else if(subAndExpr instanceof SLNot) {


	            verdict.vdm.vdm_model.SafetyRelExprKind kind = verdict.vdm.vdm_model.SafetyRelExprKind.fromValue("Not");
	            packSafetyRelExpr.setKind(kind); //setting "kind" of expression

    			//to pack the SafetyRelExpr for the Not
    			verdict.vdm.vdm_model.SafetyRelExpr packNotSafetyRelExpr= handleNotSafetyRelExpr((SLNot)subAndExpr, allPortNames, allPortCIAs);

    			//set the "not" of packSafetyRelExpr
    			packSafetyRelExpr.setNot(packNotSafetyRelExpr);
    		} else if(subAndExpr instanceof FExpr ) {


    			verdict.vdm.vdm_model.EventHappens fault = handleEventHappens(subAndExpr, allPortNames, allPortCIAs);

    			//setting "port" field of packSafetyRelExpr
    			packSafetyRelExpr.setFault(fault);

    		} else if(subAndExpr instanceof SLOr) {



    			/**
    			 * If it is a single conjunct, just send it to handleOrCyberExpr
    			 * and return the same package returned by handleAndCyberExpr
    			 */
    			packSafetyRelExpr = handleOrSafetyRelExpr(subAndExpr, allPortNames, allPortCIAs);
    		}
		} else if(andExpr.getExprs().size() > 1){ //collection of conjuncts


            verdict.vdm.vdm_model.SafetyRelExprKind kind = verdict.vdm.vdm_model.SafetyRelExprKind.fromValue("And");
            packSafetyRelExpr.setKind(kind); //setting "kind" of expression

            //to pack all conjunct subexpressions in a single list
            verdict.vdm.vdm_model.SafetyRelExprList packOrList= new verdict.vdm.vdm_model.SafetyRelExprList();

    		for(SLExpr expr : andExpr.getExprs()) { //for each conjunct (each disjunct is a LExpr)
    			//to pack the SafetyRelExpr for this conjunct
    			verdict.vdm.vdm_model.SafetyRelExpr packConjunctSafetyRelExpr= handleOrSafetyRelExpr(expr, allPortNames, allPortCIAs);

    			//adding to the list of conjuncts
    			packOrList.getExpr().add(packConjunctSafetyRelExpr);
			}

    		//setting the "or" field of packSafetyRelExpr
    		packSafetyRelExpr.setAnd(packOrList);
		}

		return packSafetyRelExpr;
	}


	/**
	 * to handle LOr expressions while creating a SafetyRelExpr object
	 * @param orExpr
	 * @param allPortNames
	 * @param allPortCIAs
	 * @return
	 */
	verdict.vdm.vdm_model.SafetyRelExpr handleOrSafetyRelExpr(SLExpr orExpr, List<String> allPortNames, List<String> allPortCIAs){
		//to pack this SafetyRelExpr
		verdict.vdm.vdm_model.SafetyRelExpr packSafetyRelExpr = new verdict.vdm.vdm_model.SafetyRelExpr();



		if(orExpr instanceof SLPort) {


			verdict.vdm.vdm_model.IAPort port = handleIAPort(orExpr, allPortNames, allPortCIAs);

			//setting "port" field of packSafetyRelExpr
			packSafetyRelExpr.setPort(port);

		} else if(orExpr instanceof SLNot) {


            verdict.vdm.vdm_model.SafetyRelExprKind kind = verdict.vdm.vdm_model.SafetyRelExprKind.fromValue("Not");
            packSafetyRelExpr.setKind(kind); //setting "kind" of expression

			//to pack the SafetyRelExpr for the Not
			verdict.vdm.vdm_model.SafetyRelExpr packNotCyberExpr= handleNotSafetyRelExpr((SLNot)orExpr, allPortNames, allPortCIAs);

			//set the "not" of packSafetyRelExpr
			packSafetyRelExpr.setNot(packNotCyberExpr);

		} else if(orExpr instanceof FExpr ) {


			verdict.vdm.vdm_model.EventHappens fault = handleEventHappens(orExpr, allPortNames, allPortCIAs);

			//setting "port" field of packSafetyRelExpr
			packSafetyRelExpr.setFault(fault);

		} else if(((SLOr)orExpr).getExprs().size() == 1){      //Is a solo disjunct


			/**
			 * If it is a single disjunct, just send it to handleAndCyberExpr
			 * and return the same package returned by handleAndCyberExpr
			 */
			SLAnd soloAndExpr = ((SLOr)orExpr).getExprs().get(0);
			packSafetyRelExpr = handleAndSafetyRelExpr(soloAndExpr, allPortNames, allPortCIAs);

		} else if (((SLOr)orExpr).getExprs().size() > 1) {


    		verdict.vdm.vdm_model.SafetyRelExprKind kind = verdict.vdm.vdm_model.SafetyRelExprKind.fromValue("Or");
            packSafetyRelExpr.setKind(kind); //setting "kind" of expression

            //to pack all disjunct subexpressions in a single list
            verdict.vdm.vdm_model.SafetyRelExprList packAndList= new verdict.vdm.vdm_model.SafetyRelExprList();

    		for(SLAnd andExpr : ((SLOr)orExpr).getExprs()) { //for each disjunct (each disjunct is a LAnd)
    			//to pack the SafetyRelExprfor this disjunct
    			verdict.vdm.vdm_model.SafetyRelExpr packDisjunctSafetyRelExpr= handleAndSafetyRelExpr(andExpr, allPortNames, allPortCIAs);

    			//adding to the list of disjuncts
    			packAndList.getExpr().add(packDisjunctSafetyRelExpr);
			}

    		//setting the "and" field of packSafetyRelExpr
    		packSafetyRelExpr.setOr(packAndList);
		}

		return packSafetyRelExpr;
	}

	/**
	 * to handle LNot expressions while creating a SafetyRelExpr object
	 * @param notExpr
	 * @param allPortNames
	 * @param allPortCIAs
	 * @return
	 */
	verdict.vdm.vdm_model.SafetyRelExpr handleNotSafetyRelExpr(SLNot notExpr, List<String> allPortNames, List<String> allPortCIAs){
		//to pack this SafetyRelExpr
		verdict.vdm.vdm_model.SafetyRelExpr packSafetyRelExpr = new verdict.vdm.vdm_model.SafetyRelExpr();



		if(notExpr.getExpr() instanceof SLPort){


			verdict.vdm.vdm_model.IAPort port = handleIAPort(notExpr.getExpr(), allPortNames, allPortCIAs);

			//setting "port" field of packSafetyRelExpr
			packSafetyRelExpr.setPort(port);

		} else if(notExpr.getExpr() instanceof FExpr){


			verdict.vdm.vdm_model.EventHappens fault = handleEventHappens(notExpr.getExpr(), allPortNames, allPortCIAs);

			//setting "port" field of packSafetyRelExpr
			packSafetyRelExpr.setFault(fault);

		} else if(notExpr.getExpr() instanceof SLOr){


			packSafetyRelExpr = handleOrSafetyRelExpr(notExpr.getExpr(), allPortNames, allPortCIAs);

		} else if(notExpr.getExpr() instanceof SLAnd){


			packSafetyRelExpr = handleAndSafetyRelExpr((SLAnd)notExpr.getExpr(), allPortNames, allPortCIAs);
		}

		return packSafetyRelExpr;
	}


	/**
	 * to create a EventHappens object for a SafetyRelExpr object
	 * @param portExpr
	 * @param allPortNames
	 * @param allPortCIAs
	 * @return
	 */
	verdict.vdm.vdm_model.EventHappens handleEventHappens(SLExpr eventExpr, List<String> allPortNames, List<String> allPortCIAs){

		String eventName = ((FExpr)eventExpr).getEventName();
		Object happens = ((FExpr)eventExpr).getFault();

		//to pack this Port
		verdict.vdm.vdm_model.EventHappens event = createVdmEventHappens(eventName, happens);

		//for testing --- delete later
		allPortNames.add(eventName);
		allPortCIAs.add(happens.toString());

		return event;
	}
    /**
     * Creates a new Vdm Port object and returns
     * Populates only "name" and "mode" for now
     * @param portName
     * @param modeString
     * @return
     */
	verdict.vdm.vdm_model.Port createVdmPort(String portName, String modeString, String qualifiedname){
		verdict.vdm.vdm_model.Port newPort = new verdict.vdm.vdm_model.Port();
		newPort.setProbe(false);
		newPort.setId(qualifiedname);
		newPort.setName(portName);
		newPort.setMode(convertToVdmPortMode(modeString));
		return newPort;
	}
	

	private Port createVdmEventPort(EventPort eventPort) {
		String modeString = "in";
		if(eventPort.isIn()) {
			modeString = "in";
		}
		else if(eventPort.isOut()) {
			modeString = "out";
		}
		verdict.vdm.vdm_model.Port newPort = new verdict.vdm.vdm_model.Port();
		newPort.setProbe(false);
		newPort.setId(eventPort.getQualifiedName());
		newPort.setName(eventPort.getName());
		newPort.setMode(convertToVdmPortMode(modeString));
		newPort.setEvent(true);
		return newPort;
	}

    /**
     * @author Vidhya Tekken Valapil
     * Creates a new Vdm Port object and returns
     * Populates "name", "mode" and "type"
     * @param dataport
     * @return vdm port
     */
	private Port createVdmPort(DataPort dataPort,Model model, HashSet<String> dataTypeDecl) {
		String modeString = "in";
		if(dataPort.isIn()) {
			modeString = "in";
		}
		else if(dataPort.isOut()) {
			modeString = "out";
		}
		//fetching data type information
		DataSubcomponentType dSubCompType = dataPort.getDataFeatureClassifier();
		verdict.vdm.vdm_data.DataType dtype = new verdict.vdm.vdm_data.DataType();
		if(dSubCompType instanceof DataTypeImpl) {
			org.osate.aadl2.DataType aadlDType = (org.osate.aadl2.DataType)dSubCompType;
			dtype = resolveAADLDataType(aadlDType, model, dataTypeDecl);
		} else if(dSubCompType instanceof DataImplementationImpl) {
			org.osate.aadl2.DataImplementation aadlDImpl = (org.osate.aadl2.DataImplementation)dSubCompType;
			dtype = resolveAADLDataImplementationType(aadlDImpl, model, dataTypeDecl);
		} else {
			System.out.println("Unresolved/unexpected Named Element.");
		}
		verdict.vdm.vdm_model.Port newPort = new verdict.vdm.vdm_model.Port();
		newPort.setProbe(false);
		if(dataPort.getOwnedPropertyAssociations().size()!=0) {
			EList<PropertyAssociation> propertyAssocs = dataPort.getOwnedPropertyAssociations();
			for(PropertyAssociation propertyAssoc : propertyAssocs) {
				if(propertyAssoc.getProperty().getName().equalsIgnoreCase("probe")) {
					EList<ModalPropertyValue> propVals = propertyAssoc.getOwnedValues();
					if(propVals.size()==0 || propVals.size()>1) {
						throw new RuntimeException("Unexpected number for values for probe property of port.");
					}
					if(propVals.get(0).getOwnedValue() instanceof BooleanLiteral) {
						BooleanLiteral probeVal = (BooleanLiteral)propVals.get(0).getOwnedValue();
						newPort.setProbe(probeVal.getValue());
					} else {
						throw new RuntimeException("Unexpected type of value for probe property of port.");
					}
				}
			}
		}
		newPort.setId(dataPort.getQualifiedName());
		newPort.setName(dataPort.getName());
		newPort.setMode(convertToVdmPortMode(modeString));
		newPort.setType(dtype);
		return newPort;
	}
    /**
     * @author Vidhya Tekken Valapil
     * Creates a new Vdm Port object and returns
     * Populates "name", "mode" and "type"
     * @param eventdataport
     * @return vdm port
     */
	private Port createVdmPort(EventDataPort dataPort, Model model, HashSet<String> dataTypeDecl) {
		String modeString = "in";
		if(dataPort.isIn()) {
			modeString = "in";
		}
		else if(dataPort.isOut()) {
			modeString = "out";
		}
		//fetching data type information
		DataSubcomponentType dSubCompType = dataPort.getDataFeatureClassifier();
		verdict.vdm.vdm_model.Port newPort = new verdict.vdm.vdm_model.Port();
		if(dSubCompType!=null) {
			verdict.vdm.vdm_data.DataType dtype = new verdict.vdm.vdm_data.DataType();
			if(dSubCompType instanceof DataTypeImpl) {
				org.osate.aadl2.DataType aadlDType = (org.osate.aadl2.DataType)dSubCompType;
				dtype = resolveAADLDataType(aadlDType, model, dataTypeDecl);
			} else if(dSubCompType instanceof DataImplementationImpl) {
				org.osate.aadl2.DataImplementation aadlDImpl = (org.osate.aadl2.DataImplementation)dSubCompType;
				dtype = resolveAADLDataImplementationType(aadlDImpl, model, dataTypeDecl);
			} else {
				System.out.println("Unresolved/unexpected Named Element.");
			}
			newPort.setType(dtype);
		}
		newPort.setProbe(false);
		newPort.setId(dataPort.getQualifiedName());
		newPort.setName(dataPort.getName());
		newPort.setMode(convertToVdmPortMode(modeString));
		newPort.setEvent(true);
		return newPort;
	}
	
    /**
     * Creates a new Vdm Port object and returns
     * Populates "name", "mode" and "type"
     * @param portName
     * @param modeString
     * @param dSubCompType 
     * @return vdm port
     */
	verdict.vdm.vdm_model.Port createVdmConnectionPort(String portName, String modeString, String qualifiedname, DataSubcomponentType dSubCompType, Model model, HashSet<String> dataTypeDecl){		
		//fetching data type information
		verdict.vdm.vdm_data.DataType dtype = new verdict.vdm.vdm_data.DataType();
		if(dSubCompType instanceof DataTypeImpl) {
			org.osate.aadl2.DataType aadlDType = (org.osate.aadl2.DataType)dSubCompType;
			dtype = resolveAADLDataType(aadlDType, model, dataTypeDecl);
		} else if(dSubCompType instanceof DataImplementationImpl) {
			org.osate.aadl2.DataImplementation aadlDImpl = (org.osate.aadl2.DataImplementation)dSubCompType;
			dtype = resolveAADLDataImplementationType(aadlDImpl, model, dataTypeDecl);
		} else {
			System.out.println("Unresolved/unexpected Named Element.");
		}
		verdict.vdm.vdm_model.Port newPort = new verdict.vdm.vdm_model.Port();
		newPort.setProbe(false);
		newPort.setId(qualifiedname);
		newPort.setName(portName);
		newPort.setMode(convertToVdmPortMode(modeString));
		newPort.setType(dtype);
		return newPort;
	}
    /**
     * @author Vidhya Tekken Valapil
     * Creates a new Vdm Port object and returns
     * Populates "name", "mode" and "type"
     * @param portName
     * @param modeString
     * @param BusSubcomponentType  
     * @return vdm port
     */
	verdict.vdm.vdm_model.Port createVdmConnectionPort(String portName, String modeString, String qualifiedName,
			BusSubcomponentType busSubCompType) {
		//fetching data type information
		verdict.vdm.vdm_data.DataType dtype = new verdict.vdm.vdm_data.DataType();
		dtype.setUserDefinedType(busSubCompType.getName());
		verdict.vdm.vdm_model.Port newPort = new verdict.vdm.vdm_model.Port();
		newPort.setProbe(false);
		newPort.setId(qualifiedName);
		newPort.setName(portName);
		newPort.setMode(convertToVdmPortMode(modeString));
		newPort.setType(dtype);
		return newPort;	
	}
    /**
     * @author Vidhya Tekken Valapil
     * Creates a new Vdm Port object and returns
     * Populates "name", "mode" and "type"
     * @param portName
     * @param modeString
     * @param BusSubcomponentType  
     * @return vdm port
     */
	verdict.vdm.vdm_model.Port createVdmConnectionEventPort(String portName, String modeString, String qualifiedName) {
		verdict.vdm.vdm_model.Port newPort = new verdict.vdm.vdm_model.Port();
		newPort.setProbe(false);
		newPort.setId(qualifiedName);
		newPort.setName(portName);
		newPort.setMode(convertToVdmPortMode(modeString));
		newPort.setEvent(true);
		return newPort;	
	}
    /**
     * @author Vidhya Tekken Valapil
     * Creates a new Vdm Port object and returns
     * Populates "name", "mode" and "type"
     * @param portName
     * @param modeString
     * @param busImplementation  
     * @return vdm port
     */
	verdict.vdm.vdm_model.Port createVdmConnectionPort(String portName, String modeString, String qualifiedName,
			BusImplementation srcBusImpl) {
		//fetching data type information
		verdict.vdm.vdm_data.DataType dtype = new verdict.vdm.vdm_data.DataType();
		dtype.setUserDefinedType(srcBusImpl.getName());
		verdict.vdm.vdm_model.Port newPort = new verdict.vdm.vdm_model.Port();
		newPort.setProbe(false);
		newPort.setId(qualifiedName);
		newPort.setName(portName);
		newPort.setMode(convertToVdmPortMode(modeString));
		newPort.setType(dtype);
		return newPort;
	}

    /**
     * Creates a new Vdm CIAPort object and returns
     * @param portName
     * @param modeString
     * @return
     */
	verdict.vdm.vdm_model.CIAPort createVdmCIAPort(String portName, String modeString){
		verdict.vdm.vdm_model.CIAPort newPort = new verdict.vdm.vdm_model.CIAPort();
		newPort.setName(portName);
		newPort.setCia(convertToVdmCia(modeString));
		return newPort;
	}


    /**
     * Creates a new Vdm IAPort object and returns
     * @param portName
     * @param modeString
     * @return
     */
	verdict.vdm.vdm_model.IAPort createVdmIAPort(String portName, String modeString){
		verdict.vdm.vdm_model.IAPort newPort = new verdict.vdm.vdm_model.IAPort();
		newPort.setName(portName);
		newPort.setIa(convertToVdmIa(modeString));
		return newPort;
	}


    /**
     * Creates a new Vdm EventHappens object and returns
     * @param eventName
     * @param eventHappens
     * @return
     */
	verdict.vdm.vdm_model.EventHappens createVdmEventHappens(String eventName, Object happens){
		verdict.vdm.vdm_model.EventHappens newEvent = new verdict.vdm.vdm_model.EventHappens();
		newEvent.setEventName(eventName);
		newEvent.setHappens(happens);
		return newEvent;
	}


    /**
     * To convert a String cia to VDM CIA
     * */
    verdict.vdm.vdm_model.CIA convertToVdmCia(String cia) {
    	String full = cia;
    	if(cia != null && cia.length() == 1) {
			switch (cia) {
			case "C":
				full = "Confidentiality";
				break;
			case "I":
				full = "Integrity";
				break;
			case "A":
				full = "Availability";
				break;
			default:
				break;
			}
    	} else {
    		throw new RuntimeException("Unexpected!");
    	}
    	verdict.vdm.vdm_model.CIA CiaObj = verdict.vdm.vdm_model.CIA.fromValue(full);
    	return CiaObj;
    }


    /**
     * To convert a String ia to VDM IA
     * */
    verdict.vdm.vdm_model.IA convertToVdmIa(String ia){
    	String full = ia;
    	if(ia != null && ia.length() == 1) {
			switch (ia) {
			case "I":
				full = "Integrity";
				break;
			case "A":
				full = "Availability";
				break;
			default:
				break;
			}
    	} else {
    		throw new RuntimeException("Unexpected!");
    	}
    	verdict.vdm.vdm_model.IA IaObj = verdict.vdm.vdm_model.IA.fromValue(full);
    	return IaObj;
    }


    /**
     * To convert a String to VDM PortMode
    * */
    verdict.vdm.vdm_model.PortMode convertToVdmPortMode(String s){
    	verdict.vdm.vdm_model.PortMode portModeObj = verdict.vdm.vdm_model.PortMode.fromValue(s);
    	return portModeObj;
    }


    /**
     * To convert a string to VDM Severity
     * */
    verdict.vdm.vdm_model.Severity convertToVdmSeverity(String severity){
    	verdict.vdm.vdm_model.Severity severityObj = verdict.vdm.vdm_model.Severity.fromValue(severity);
    	return severityObj;
    }

    
    /**
     * Returns the VDM ConnectionKind
     * @param kind
     * @return
     */
    verdict.vdm.vdm_model.ConnectionKind getConnectionKind(Connection aConn) {
    	String kindString = "port";
    	
    	if(aConn.getClass().getSimpleName().equalsIgnoreCase("AccessConnectionImpl")) {
    		AccessConnectionImpl accessConnImpl = (AccessConnectionImpl) aConn;    		
    		if(accessConnImpl.getAccessCategory().getName().equalsIgnoreCase("data")) {
    			kindString = "dataAccess";	
    		} else if(accessConnImpl.getAccessCategory().getName().equalsIgnoreCase("bus")) {
    			kindString = "busAccess";
    		}
    	} else if(aConn.getClass().getSimpleName().equalsIgnoreCase("PortConnectionImpl")) {
    		kindString = "port";
    	} else if(aConn.getClass().getSimpleName().equalsIgnoreCase("ParameterConnectionImpl")) {
    		kindString = "parameter";
    	} 
    	
    	verdict.vdm.vdm_model.ConnectionKind returnKind =  verdict.vdm.vdm_model.ConnectionKind.fromValue(kindString);
    	return returnKind;
    }

       
/** The auxiliary functions below were borrowed verbatim from com.ge.research.osate.verdict.aadl2csv.Asdl2CsvTranslator.java */

	/**
	 * @author Paul Meng
	 * Checks if a property is applicable for a given component category
	 * @param prop
	 * @param cat
	 * @return
	 */
	private boolean isApplicableToCat(Property prop, String cat) {
		for(PropertyOwner po : prop.getAppliesTos()) {
			String propCat = ((MetaclassReferenceImpl)po).getMetaclass().getName().toLowerCase();

			if(cat.equals("abstract") && propCat.equals("system")) {
				return true;
			}
			if(propCat.equalsIgnoreCase(cat)) {
				return true;
			}
		}
		return false;
	}


	/**
	 * @author Paul Meng
	 * The calling function should know the size of the return array
	 * */
	String[] getStrRepofExpr(PropertyExpression expr) {
		String[] values = new String[4];
		if (expr instanceof BooleanLiteralImpl) {
			BooleanLiteralImpl bool = ((BooleanLiteralImpl) expr);
			//values[0] = bool.getValue()?"1":"0";
			values[0] = bool.getValue()?"true":"false";
		} else if (expr instanceof IntegerLiteralImpl) {
			IntegerLiteralImpl intVal = ((IntegerLiteralImpl) expr);
			values[0] = String.valueOf((int)intVal.getValue());
		} else if (expr instanceof NamedValueImpl) {
			NamedValueImpl namedValue =((NamedValueImpl) expr);

			if (namedValue.getNamedValue() instanceof EnumerationLiteralImpl)
			{
				EnumerationLiteralImpl enu = ((EnumerationLiteralImpl) namedValue.getNamedValue());
				values[0] = enu.getName();
			} else {
				throw new RuntimeException("Unsupported property value: " + namedValue.getNamedValue());
			}
		} else if (expr instanceof ListValueImpl) {
			ListValueImpl listValue = (ListValueImpl)expr;
			if(listValue.getOwnedListElements().size() == 1) {
				values = getStrRepofExpr(listValue.getOwnedListElements().get(0));
			} else {
				throw new RuntimeException("Unexpected!");
			}
		} else if (expr instanceof ReferenceValueImpl) {
			// We only consider the value of expr is a bus expression here.
			ReferenceValueImpl refValue = (ReferenceValueImpl) expr;

			if(refValue.getContainmentPathElements().size() == 1) {
				ContainmentPathElement element = refValue.getContainmentPathElements().get(0);
				NamedElement namedElement = element.getNamedElement();

				if(namedElement instanceof BusSubcomponent) {
					ComponentImplementation impl = ((BusSubcomponent)namedElement).getContainingComponentImpl();
					String compTypeName = impl.getTypeName();

					values[0] = compTypeName;
					values[1] = impl.getName();
					values[2] = "";
					values[3] = namedElement.getName();
				} else {
					throw new RuntimeException("Unexpected!");
				}
			} else if(refValue.getContainmentPathElements().size() == 2) {
				// This is to deal with the expression "subcomponent . bus"
				ContainmentPathElement elementZero = refValue.getContainmentPathElements().get(0);
				ContainmentPathElement elementOne = refValue.getContainmentPathElements().get(1);
				NamedElement namedElementZero = elementZero.getNamedElement();
				NamedElement namedElementOne = elementOne.getNamedElement();

				if(namedElementZero instanceof SystemSubcomponent) {
					ComponentImplementation impl = ((SystemSubcomponent)namedElementZero).getComponentImplementation();
					values[0] = impl.getTypeName();
					values[1] = impl.getName();
					values[2] = namedElementZero.getName();
					values[3] = namedElementOne.getName();
				} else {
					throw new RuntimeException("Unexpected!");
				}
			} else {
				throw new RuntimeException("Unexpected number of property values: " + refValue.getContainmentPathElements().size());
			}
		} else if(expr instanceof StringLiteralImpl) {
			StringLiteralImpl strVal = ((StringLiteralImpl) expr);
			values[0] = strVal.getValue();
		} else {
			throw new RuntimeException("Unsupported property value: " + expr);
		}
		return values;
	}


	/**
	 * @author Paul Meng
	 * Process an event corresponding to a selection of AADL project
	 * Translate an AADL project into objects
	 *
	 * */
	public List<EObject> preprocessAadlFiles(File dir) {
		final Injector injector = new Aadl2StandaloneSetup().createInjectorAndDoEMFRegistration();
		final XtextResourceSet rs = injector.getInstance(XtextResourceSet.class);
		List<String> aadlFileNames = new ArrayList<>();

		// Set scenario name
		//scenario = dir.getName();

		// Obtain all AADL files contents in the project
		List<EObject> objects = new ArrayList<>();

		List<File> dirs = collectAllDirs(dir);

		for(File subdir: dirs) {
			for (File file : subdir.listFiles()) {
				if (file.getAbsolutePath().endsWith(".aadl")) {
					aadlFileNames.add(file.getAbsolutePath());
				}
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


	/**
	 * @author Paul Meng
	 * The following two functions are used to get lists of
	 * all aadl files from directories
	 * @param dir
	 * @return
	 */
	List<File> collectAllDirs(File dir) {
		List<File> allDirs = new ArrayList<File>();
		allDirs.add(dir);
		for(File file : dir.listFiles()) {
			if(file.isDirectory()) {
				allDirs.add(file);
				collectDir(file, allDirs);
			}
		}
		return allDirs;
	}
	void collectDir(File dir, List<File> allDirs) {
		for(File file : dir.listFiles()) {
			if(file.isDirectory()) {
				allDirs.add(file);
				collectDir(file, allDirs);
			}
		}
	}


	/**
	 * @author Paul Meng
	 * @param propAcc
	 * @return
	 */
	public String getStrRepofPropVal(PropertyAcc propAcc) {
		String value = "";

		if(propAcc != null) {
			List<PropertyAssociation> propAssocs = propAcc.getAssociations();

			if(!propAssocs.isEmpty() && propAssocs.size() == 1) {
				PropertyAssociation propAssoc = propAssocs.get(0);

				// We assume that each property only has only 1 non-list value for now
				if(propAssoc.getOwnedValues().size() == 1) {
					ModalPropertyValue propVal = propAssoc.getOwnedValues().get(0);
					PropertyExpression exp = propVal.getOwnedValue();
					value = getStrRepofExpr(exp)[0];
				} else {
					throw new RuntimeException("Unexpected: property is a list of values with size = : " + propAssoc.getOwnedValues().size());
				}
			} else {
//				throw new RuntimeException("Unexpected property association size: " + propAssocs.size());
			}
		}
		return value;
	}


    /**
     *
     * @author Paul Meng
     * To make sure the input is not null
     * */
    String sanitizeValue(String val) {
    	return val == null ? "" : val;
    }


    /**
     * @author Paul Meng
     * Obtain the connection component information from the input context
     *
     * */
    public String[] obtainConnCompInfo(Context connContext) {
    	String[] info = new String[3];
    	String compCat = "";
    	String compName = "";
    	String compImplName = "";
		if(connContext instanceof ProcessSubcomponent) {
			compCat = ((ProcessSubcomponent)connContext).getCategory().getName();
			compName = ((ProcessSubcomponent)connContext).getComponentType().getName();
			compImplName = ((ProcessSubcomponent)connContext).getComponentImplementation() == null?
									"":((ProcessSubcomponent)connContext).getComponentImplementation().getName();
		} else if(connContext instanceof SystemSubcomponent) {
			compCat = ((SystemSubcomponent)connContext).getCategory().getName();
			compName = ((SystemSubcomponent)connContext).getComponentType().getName();
			compImplName = ((SystemSubcomponent)connContext).getComponentImplementation() == null?
									"":((SystemSubcomponent)connContext).getComponentImplementation().getName();
		} else if(connContext instanceof DeviceSubcomponent) {
			compCat = ((DeviceSubcomponent)connContext).getCategory().getName();
			compName = ((DeviceSubcomponent)connContext).getComponentType().getName();
			compImplName = ((DeviceSubcomponent)connContext).getComponentImplementation() == null?
									"":((DeviceSubcomponent)connContext).getComponentImplementation().getName();
		} else if(connContext instanceof AbstractSubcomponent) {
			compCat = ((AbstractSubcomponent)connContext).getCategory().getName();
			compName = ((AbstractSubcomponent)connContext).getComponentType().getName();
			compImplName = ((AbstractSubcomponent)connContext).getComponentImplementation() == null?
									"":((AbstractSubcomponent)connContext).getComponentImplementation().getName();
		}  else if(connContext instanceof DataSubcomponent) {
			compCat = ((DataSubcomponent)connContext).getCategory().getName();
			compName = ((DataSubcomponent)connContext).getComponentType().getName();
			compImplName = ((DataSubcomponent)connContext).getComponentImplementation() == null?
									"":((DataSubcomponent)connContext).getComponentImplementation().getName();
		} else if(connContext instanceof ThreadSubcomponent) {
			compCat = ((ThreadSubcomponent)connContext).getCategory().getName();
			compName = ((ThreadSubcomponent)connContext).getComponentType().getName();
			compImplName = ((ThreadSubcomponent)connContext).getComponentImplementation() == null?
									"":((ThreadSubcomponent)connContext).getComponentImplementation().getName();
		} else if(connContext instanceof MemorySubcomponent) {
			compCat = ((MemorySubcomponent)connContext).getCategory().getName();
			compName = ((MemorySubcomponent)connContext).getComponentType().getName();
			compImplName = ((MemorySubcomponent)connContext).getComponentImplementation() == null?
									"":((MemorySubcomponent)connContext).getComponentImplementation().getName();
		} else if(connContext instanceof SubprogramSubcomponent) {
			compCat = ((SubprogramSubcomponent)connContext).getCategory().getName();
			compName = ((SubprogramSubcomponent)connContext).getComponentType().getName();
			compImplName = ((SubprogramSubcomponent)connContext).getComponentImplementation() == null?
									"":((SubprogramSubcomponent)connContext).getComponentImplementation().getName();
		} else if(connContext instanceof ThreadGroupSubcomponent) {
			compCat = ((ThreadGroupSubcomponent)connContext).getCategory().getName();
			compName = ((ThreadGroupSubcomponent)connContext).getComponentType().getName();
			compImplName = ((ThreadGroupSubcomponent)connContext).getComponentImplementation() == null?
									"":((ThreadGroupSubcomponent)connContext).getComponentImplementation().getName();
		} else if(connContext instanceof VirtualProcessorSubcomponent) {
			compCat = ((VirtualProcessorSubcomponent)connContext).getCategory().getName();
			compName = ((VirtualProcessorSubcomponent)connContext).getComponentType().getName();
			compImplName = ((VirtualProcessorSubcomponent)connContext).getComponentImplementation() == null?
									"":((VirtualProcessorSubcomponent)connContext).getComponentImplementation().getName();
		} else {
			throw new RuntimeException("Unsupported AADL component element type: " + connContext);
		}
		info[0] = compCat;
		info[1] = compName;
		info[2] = compImplName;
    	return info;
    }


      	/**
  	 * for pretty printing header
      	 */
      private static void logLine() {
          System.out.println(
                  "******************************************************************"
                          + "******************************************************");
      }


  	/**
  	 * for pretty printing header
  	 */
      private static void logHeader(String header) {
          System.out.println();
          logLine();
          System.out.println("      " + header);
          logLine();
          System.out.println();
      }


  	/**
  	 * NOTE:- Only used for debugging purposes
  	 * @author Paul Meng
  	 * @param cia
  	 * @return
  	 */
        String convertAbbreviation(String cia) {
        	String full = cia;

        	if(cia != null && cia.length() == 1) {
    			switch (cia) {
    			case "C":
    				full = "Confidentiality";
    				break;
    			case "I":
    				full = "Integrity";
    				break;
    			case "A":
    				full = "Availability";
    				break;
    			default:
    				break;
    			}
        	} else {
        		throw new RuntimeException("Unexpected!");
        	}
        	return full;
        }
    /**
     * @author Vidhya Tekken Valapil
     * Obtain data type information and return vdm-data-type
     * */   
    private verdict.vdm.vdm_data.DataType resolveAADLDataType(org.osate.aadl2.DataType aadlDataType, Model model, HashSet<String> dataTypeDecl) {
		verdict.vdm.vdm_data.DataType dtype = new verdict.vdm.vdm_data.DataType();
		if(aadlDataType.getName().contentEquals("Float")){
			dtype.setPlainType(verdict.vdm.vdm_data.PlainType.fromValue("real"));
		} else if(aadlDataType.getName().contentEquals("Integer")){
			dtype.setPlainType(verdict.vdm.vdm_data.PlainType.fromValue("int"));
		} else if(aadlDataType.getName().contentEquals("Boolean")){
			dtype.setPlainType(verdict.vdm.vdm_data.PlainType.fromValue("bool"));
		} else {//not float or int or bool or enum
			dtype.setUserDefinedType(aadlDataType.getName());
		}
		defineDataType(aadlDataType, model, dataTypeDecl);
		return dtype;
	}
    /**
     * @author Vidhya Tekken Valapil
     * Fetch data implementation type information and return vdm-data-type
     * */
    private verdict.vdm.vdm_data.DataType resolveAADLDataImplementationType(DataImplementation dataImplementation, Model model, HashSet<String> dataTypeDecl) {
		verdict.vdm.vdm_data.DataType dtype = new verdict.vdm.vdm_data.DataType();
		dtype.setUserDefinedType(dataImplementation.getName());
		defineDataImplementationType(dataImplementation, model, dataTypeDecl);
		return dtype;
	}
    /**
     * @author Vidhya Tekken Valapil
     * Fetch names of objects and return the list of names
     * */
	private HashSet<String> getObjectNames(List<EObject> objects) {
		HashSet<String> objNames = new HashSet<String>();
		for(EObject obj : objects) {
			//process only those objects in files in the project and not in the imported files
			if (obj instanceof SystemType) {
				objNames.add(((SystemType) obj).getName());
			} else if (obj instanceof BusType) {
				objNames.add(((BusType)obj).getName());
			} else if (obj instanceof SubprogramType) {
				objNames.add(((SubprogramType)obj).getName());
			} else if (obj instanceof ThreadType) {
				objNames.add(((ThreadType)obj).getName());
			} else if (obj instanceof MemoryType) {
				objNames.add(((MemoryType)obj).getName());
			} else if (obj instanceof DeviceType) {
				objNames.add(((DeviceType)obj).getName());
			} else if (obj instanceof AbstractType) {
				objNames.add(((AbstractType)obj).getName());
			} else if (obj instanceof ProcessType) {
				objNames.add(((ProcessType)obj).getName());
			} else if (obj instanceof ThreadGroupType) {
				objNames.add(((ThreadGroupType)obj).getName());
			} else if (obj instanceof VirtualProcessorType) {
				objNames.add(((VirtualProcessorType)obj).getName());
			} else if (obj instanceof ProcessorType) {
				objNames.add(((ProcessorType)obj).getName());
			} else if (obj instanceof SystemImplementation) {
				objNames.add(((SystemImplementation) obj).getName());
			} else if (obj instanceof SubprogramImplementation) {
				objNames.add(((SubprogramImplementation) obj).getName());
			} else if (obj instanceof ThreadImplementation) {
				objNames.add(((ThreadImplementation) obj).getName());
			} else if (obj instanceof MemoryImplementation) {
				objNames.add(((MemoryImplementation) obj).getName());
			} else if (obj instanceof BusImplementation) {
				objNames.add(((BusImplementation) obj).getName());
			} else if (obj instanceof AbstractImplementation) {
				objNames.add(((AbstractImplementation) obj).getName());
			} else if (obj instanceof DeviceImplementation) {
				objNames.add(((DeviceImplementation) obj).getName());
			} else if (obj instanceof ProcessImplementation) {
				objNames.add(((ProcessImplementation) obj).getName());
			} else if (obj instanceof ThreadGroupImplementation) {
				objNames.add(((ThreadGroupImplementation) obj).getName());
			} else if (obj instanceof VirtualProcessorImplementation) {
				objNames.add(((VirtualProcessorImplementation) obj).getName());
			} else if (obj instanceof ProcessorImplementation) {
				objNames.add(((ProcessorImplementation) obj).getName());
			}  else if(obj instanceof PropertySetImpl) {
				for(Property prop : ((PropertySetImpl)obj).getOwnedProperties()) {
					// Save property owner to be used later
					for(PropertyOwner po : prop.getAppliesTos()) {
						String propCat = ((MetaclassReferenceImpl)po).getMetaclass().getName().toLowerCase();
						String propName = prop.getName();
						switch(propCat) {
							case "system": {
								objNames.add(propName);
								break;
							}
							case "thread": {
								objNames.add(propName);
								break;
							}
							case "processor": {
								objNames.add(propName);
								break;
							}
							case "memory": {
								objNames.add(propName);
								break;
							}
							case "connection": {
								objNames.add(propName);
								break;
							}
							case "process": {
								objNames.add(propName);
								break;
							}
							case "abstract": {
								objNames.add(propName);
								break;
							}
							case "device": {
								objNames.add(propName);
								break;
							}
							case "threadgroup": {
								objNames.add(propName);
								break;
							}
							case "virtualprocessor": {
								objNames.add(propName);
								break;
							}
							case "bus": {
								objNames.add(propName);
								break;
							}
							case "port": {
								objNames.add(propName);
								break;
							}
							default: {
								System.out.println(
										"Warning: unsupported property: " + propName + ", applies to: " + propCat);
								break;
							}
						}
					}
				}
			}
		}
		return objNames;
	}

    /**
     * @author Vidhya Tekken Valapil
     * populate information related to data implementation types in the vdm
     * */
	private void defineDataImplementationType(DataImplementation dataImplementation, Model model, HashSet<String> dataTypeDecl) {
		//DEFINE DATA TYPE IN DECLARATIONS IF NOT ALREADY DEFINED
		String dataImplementationName = dataImplementation.getName();
		if(!dataTypeDecl.contains(dataImplementationName)) {
			dataTypeDecl.add(dataImplementationName);
			verdict.vdm.vdm_data.DataType dtype = new verdict.vdm.vdm_data.DataType();
			//GET DETAILS OF THE DATA IMPLEMENTATION AND CREATE CORRESPONDING VDM DATATYPE
			EList<DataSubcomponent> subcomponents= dataImplementation.getOwnedDataSubcomponents();
			if(!subcomponents.isEmpty()) {//if the dataType definition has subcomponents
				RecordType recType = new RecordType();
				for (DataSubcomponent dataSubComp: subcomponents) {
					RecordField recField = new RecordField();
					recField.setName(dataSubComp.getName());
					DataSubcomponentType dataSubCompType = dataSubComp.getDataSubcomponentType();
					if (dataSubCompType instanceof org.osate.aadl2.DataType){
						org.osate.aadl2.DataType aadlDataType = (org.osate.aadl2.DataType)dataSubCompType;
						Agree2Vdm agree2vdm= new Agree2Vdm();
						verdict.vdm.vdm_data.DataType recFieldDtype = agree2vdm.getVdmTypeFromAADLType(aadlDataType);
						recField.setType(recFieldDtype);	
					} else if (dataSubCompType instanceof DataImplementation){
						DataImplementation dataSubCompDataImplementation = (DataImplementation)dataSubCompType;
						verdict.vdm.vdm_data.DataType recFieldDtype = new verdict.vdm.vdm_data.DataType();
						recFieldDtype.setUserDefinedType(dataSubCompDataImplementation.getName());
						recField.setType(recFieldDtype);	
					} else {
						System.out.println("Unexpected Data Subcomponent that is not a DataTypeImpl or DataImplementatioImpl.");
					}
					recType.getRecordField().add(recField);
				}
				dtype.setRecordType(recType);
			} else { //if the dataType is base type boolean or integer or char or string
				System.out.println("Data implementation type has no subcomponents");
			}
			//vdm data type declaration
			TypeDeclaration dataTypeVdm = new TypeDeclaration();
			dataTypeVdm.setName(dataImplementation.getName());
			dataTypeVdm.setDefinition(dtype);
			//add the typeDeclaration to the model
			model.getTypeDeclaration().add(dataTypeVdm);
		}
	}

    /**
     * @author Vidhya Tekken Valapil
     * populate information related to data types in the vdm
     * */
	private void defineDataType(DataType aadlDataType, Model model, HashSet<String> dataTypeDecl) {
		//DEFINE DATA TYPE IN DECLARATIONS IF NOT ALREADY DEFINED
		String aadlDataTypeName = aadlDataType.getName();
		if(!dataTypeDecl.contains(aadlDataTypeName) && (!aadlDataTypeName.equalsIgnoreCase("Float")) && (!aadlDataTypeName.equalsIgnoreCase("Integer")) && (!aadlDataTypeName.equalsIgnoreCase("Boolean"))) {
			dataTypeDecl.add(aadlDataType.getName());
			verdict.vdm.vdm_data.DataType dtype = new verdict.vdm.vdm_data.DataType();
			if(aadlDataType.getName().contentEquals("Float")){
				dtype.setPlainType(verdict.vdm.vdm_data.PlainType.fromValue("real"));
			} else if(aadlDataType.getName().contentEquals("Integer")){
				dtype.setPlainType(verdict.vdm.vdm_data.PlainType.fromValue("int"));
			} else if(aadlDataType.getName().contentEquals("Boolean")){
				dtype.setPlainType(verdict.vdm.vdm_data.PlainType.fromValue("bool"));
			} else if (!(aadlDataType.getAllPropertyAssociations().isEmpty())){//if the dataType definition has properties
				EList<PropertyAssociation> properties= aadlDataType.getAllPropertyAssociations();
				Agree2Vdm agree2vdm= new Agree2Vdm();
				agree2vdm.updateVDMDatatypeUsingProperties(dtype,properties);
			} else {//not float or int or bool or enum
				dtype.setUserDefinedType(aadlDataType.getName());
				//System.out.println("Unresolved AADL Data type value is "+aadlDataType.getName());
			}
			//vdm data type declaration
			TypeDeclaration dataTypeVdm = new TypeDeclaration();
			dataTypeVdm.setName(aadlDataType.getName());
			if((aadlDataType.getName().contentEquals("Float")) || (aadlDataType.getName().contentEquals("Integer")) || (aadlDataType.getName().contentEquals("Boolean")) || !(aadlDataType.getAllPropertyAssociations().isEmpty())) {
				dataTypeVdm.setDefinition(dtype);
			}
			//add the typeDeclaration to the model
			model.getTypeDeclaration().add(dataTypeVdm);
		}
	}
}
