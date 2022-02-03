package com.ge.research.osate.verdict.vdm2csv;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.ge.research.osate.verdict.aadl2csv.Table;
import com.ge.verdict.vdm.DefenseProperties;

import verdict.vdm.vdm_data.GenericAttribute;
import verdict.vdm.vdm_model.CIA;
import verdict.vdm.vdm_model.CIAPort;
import verdict.vdm.vdm_model.CompInstancePort;
import verdict.vdm.vdm_model.ComponentImpl;
import verdict.vdm.vdm_model.ComponentInstance;
import verdict.vdm.vdm_model.ComponentType;
import verdict.vdm.vdm_model.ConnectionEnd;
import verdict.vdm.vdm_model.CyberExpr;
import verdict.vdm.vdm_model.IAPort;
import verdict.vdm.vdm_model.Model;
import verdict.vdm.vdm_model.Port;
import verdict.vdm.vdm_model.SafetyRelExpr;
import verdict.vdm.vdm_model.SafetyReqExpr;
import verdict.vdm.vdm_model.Severity;

/**
*
* @author Vidhya Tekken Valapil
*
*/
public class Vdm2Csv {	
	private final boolean synthesis;
	public Vdm2Csv() {
		this(false);
	}
	/**
	 * If synthesis is enabled, then fools STEM by setting DAL 9 on all MBAA/MBAS defense properties.
	 * --code synonymous to code by William in the previous aadl2csvtranslator
	 * @param synthesis
	 */
	public Vdm2Csv(boolean synthesis) {
		this.synthesis = synthesis;
	}
	/**
	 * @param VDM
	 * @param stem directory 
	 * @param soteria directory
	 * @param model name i.e., project directory name 
	 * 1. Initialize tables with headers if they have static headers
	 * 2. Build tables for STEM:
	 *        ScnCompProps.csv, ScnConnections.csv, ScnBusBindings.csv
	 * 3. Build tables for Soteria++:
	 *        CompDep.csv, Mission.csv, CompSaf.csv,
	 *        Events.csv, ScnCompProps.csv, ScnConnections.csv
	 * 4. Output the csv files
	 * */
	public void execute(Model vdm, String stemDir, String soteriaDir, String modelName) {
		Table eventsTable = new Table("QualifiedName", "SanitizedQualifiedName", "PackageName", "Comp", "Event", "Probability");
		Table compSafTable = new Table("QualifiedName", "SanitizedQualifiedName", "PackageName","Comp", "InputPortOrEvent", "InputIAOrEvent", "OutputPort", "OutputIA");
		Table compDepTable = new Table("QualifiedName", "SanitizedQualifiedName", "PackageName","Comp", "InputPort", "InputCIA", "OutputPort", "OutputCIA");
		Table missionTable =  new Table("ModelVersion", "MissionReqId", "MissionReq", "ReqId","Req", "MissionImpactCIA",
                "Effect", "Severity", "CompInstanceDependency", "CompOutputDependency", "DependentCompOutputCIA", "ReqType");
		List<ComponentImpl> compImpls = vdm.getComponentImpl();
		Map<String, HashSet<String>> propToConnections = new HashMap<>();//map(property_name, hash_set_of_connections)
		//create map for connection names to attributes names and attribute names to attribute value
		Map<String, HashMap<String,String>> connectionAttributesMap = new HashMap<>();
		Map<String, HashSet<String>> propToCompInsts = new HashMap<>();//map(property_name, hash_set_of_components)
		//create map for component names to attributes names and attribute names to attribute value
		Map<String, HashMap<String,String>> compInstAttributesMap = new HashMap<>();
		Map<String, String> compToCompImpl = new HashMap<>();
		Map<String, List<ConnectionEnd>> connectionDestToSourceMap = new HashMap<>();
		//pre-process component implementations info in the VDM and update maps propToConnections, connectionAttributesMap, compToCompImpl, compInstAttributesMap
		//these maps will be used to build tables scnConnTable and scnCompPropsTable
		preprocessCompImpls(compImpls, modelName, propToConnections, connectionAttributesMap, compToCompImpl, propToCompInsts, compInstAttributesMap, connectionDestToSourceMap);
		Table scnConnTable = updateConnectionsTable(compImpls, modelName, propToConnections, connectionAttributesMap, compToCompImpl);
		Table scnBusBindingsTable = updateBusBindingsTable(compImpls, modelName, compToCompImpl);
		Table scnCompPropsTable = updateCompInstTable(compImpls, modelName, propToCompInsts, compInstAttributesMap, compToCompImpl);
		//build events, compDep, compSaf, mission tables
		updateTablesWithDataFromVDM(vdm, modelName, eventsTable, compDepTable,compSafTable, missionTable, connectionDestToSourceMap);
		//create CSVs for STEM and Soteria++
		// For STEM
		scnCompPropsTable.toCsvFile(new File(stemDir, "ScnCompProps.csv"));
		scnConnTable.toCsvFile(new File(stemDir, "ScnConnections.csv"));
		scnBusBindingsTable.toCsvFile(new File(stemDir, "ScnBusBindings.csv"));
		// For Soteria++
		eventsTable.toCsvFile(new File(soteriaDir, "Events.csv"));
		compSafTable.toCsvFile(new File(soteriaDir, "CompSaf.csv"));
		compDepTable.toCsvFile(new File(soteriaDir, "CompDep.csv"));
		missionTable.toCsvFile(new File(soteriaDir, "Mission.csv"));
		scnCompPropsTable.toCsvFile(new File(soteriaDir, "ScnCompProps.csv"));
		scnConnTable.toCsvFile(new File(soteriaDir, "ScnConnections.csv"));
	}
	/**
	 * Parse the VDM and populate corresponding tables
	 * @param inputVDM
	 * @param eventsTable 
	 * @param compDepTable 
	 * @param connectionDestToSourceMap 
	 */
	private void updateTablesWithDataFromVDM(Model inputVDM, String modelName, Table eventsTable, Table compDepTable, Table compSafTable, Table missionTable, Map<String, List<ConnectionEnd>> connectionDestToSourceMap) {	
		//process compTypes in VDM and update events, compsaf and compdep tables
		List<ComponentType> compTypes = inputVDM.getComponentType();
		processComponentTypes(compTypes, eventsTable, compDepTable, compSafTable);
		//create maps for cyber and safety reqs (req-id --> req-definition)
		Map<String, verdict.vdm.vdm_model.CyberReq> cyberReqsMap = new HashMap<>();
		Map<String, verdict.vdm.vdm_model.SafetyReq> safetyReqsMap = new HashMap<>();
		updateCyberSafetyReqsMaps(inputVDM.getCyberReq(), cyberReqsMap, inputVDM.getSafetyReq(), safetyReqsMap);
		//process mission in VDM and update missions table using safety/cyber req definitions in maps
		processMissionReqs(inputVDM.getMission(), cyberReqsMap, safetyReqsMap, missionTable, modelName, connectionDestToSourceMap);
	}
	private void preprocessCompImpls(List<ComponentImpl> compImpls, String scenario, Map<String, HashSet<String>> propToConnections, Map<String, HashMap<String, String>> connectionAttributesMap, Map<String, String> compToCompImpl, Map<String, HashSet<String>> propToCompInsts, Map<String, HashMap<String, String>> compInstAttributesMap, Map<String, List<ConnectionEnd>> connectionDestToSourceMap) {
		for (ComponentImpl compImpl: compImpls) {
			if(compImpl.getBlockImpl()!=null) {
				//process connections
				List<verdict.vdm.vdm_model.Connection> compConnections = compImpl.getBlockImpl().getConnection();
				for (verdict.vdm.vdm_model.Connection compConnection1 : compConnections) {
					// -- Get all property names associated with connections --> need it for headers in connections csv
					// -- Create a property name to set of connections with that property mapping
					List<GenericAttribute> connectionAttributes = compConnection1.getAttribute();
					for (GenericAttribute connectionAttribute : connectionAttributes) {
						if(propToConnections.containsKey(connectionAttribute.getName())) {
							String PropName = connectionAttribute.getName();
							HashSet<String> propToConnection = propToConnections.get(PropName);
							propToConnection.add(compConnection1.getName());
							propToConnections.replace(PropName, propToConnection);
						} else {
							HashSet<String> propToConnection = new HashSet<>();
							propToConnection.add(compConnection1.getName());
							propToConnections.put(connectionAttribute.getName(), propToConnection);
						}
					}
					// -- Create a map of connections that have a data-port as destination rather than subcomponent.data-port
					// this will be used for populating Missions Table
					verdict.vdm.vdm_model.ConnectionEnd dest = compConnection1.getDestination();
					if(dest.getComponentPort()!=null) {
						String destName = dest.getComponentPort().getName();
						if(connectionDestToSourceMap.containsKey(destName)) {
							List<ConnectionEnd> sources = connectionDestToSourceMap.get(destName);
							sources.add(compConnection1.getSource());
							connectionDestToSourceMap.replace(destName, sources);
						} else {
							List<ConnectionEnd> sources = new ArrayList<>();
							sources.add(compConnection1.getSource());
							connectionDestToSourceMap.put(destName, sources);
						}
					}
					//--- Update connection attributes/properties map
					HashMap<String,String> connAttributes = new HashMap<>();
					List<GenericAttribute> connAttributesList = compConnection1.getAttribute();
					for(GenericAttribute attr: connAttributesList) {
						connAttributes.put(attr.getName(), attr.getValue().toString());
					}
					connectionAttributesMap.put(compConnection1.getName(), connAttributes);
				}
				//--process component instances
				List<verdict.vdm.vdm_model.ComponentInstance> compInstances = compImpl.getBlockImpl().getSubcomponent();
				for (verdict.vdm.vdm_model.ComponentInstance compInst : compInstances) {
					List<GenericAttribute> compInstAttributesList = compInst.getAttribute();
					HashMap<String,String> compInstAttributes = new HashMap<>();
					for(GenericAttribute attr: compInstAttributesList) {
						//--update attribute to attribute value mapping for each component attribute
						compInstAttributes.put(attr.getName(), attr.getValue().toString());
						// -- Get all property names associated with components --> need it for headers in components csv
						// -- Create a property name to set of components with that property mapping
						if(propToCompInsts.containsKey(attr.getName())) {
							String PropName = attr.getName();
							HashSet<String> propToCompInst = propToCompInsts.get(PropName);
							propToCompInst.add(compInst.getId());
							propToCompInsts.replace(PropName, propToCompInst);
						} else {
							HashSet<String> propToCompInst = new HashSet<>();
							propToCompInst.add(compInst.getId());
							propToCompInsts.put(attr.getName(), propToCompInst);
						}
					}
					//Update component instances to attributes/properties map
					compInstAttributesMap.put(compInst.getId(), compInstAttributes);
				}
				//-- Create component to component-implementation mapping
				compToCompImpl.put(compImpl.getType().getName(), compImpl.getName());
			}
		}
	}
	private Table updateConnectionsTable(List<ComponentImpl> compImpls, String scenario, Map<String, HashSet<String>> propToConnections, Map<String, HashMap<String, String>> connectionAttributesMap, Map<String, String> compToCompImpl) {
		// --update headers of connections csv
		List<String> headers = new ArrayList<String>(
    			Arrays.asList("Scenario", "QualifiedName", "SanitizedQualifiedName", "PackageName","Comp", "Impl", "ConnQualifiedName", "SanitizedConnQualifiedName", "ConnectionName", "SrcCompInstQualifiedName", "SanitizedSrcCompInstQualifiedName", "SrcCompInstPackage", "SrcComp", "SrcImpl", "SrcCompInstance", "SrcCompCategory",
    					     "SrcPortName", "SrcPortType", "DestCompInstQualifiedName", "SanitizedDestCompInstQualifiedName", "DestCompInstPackage", "DestComp", "DestImpl", "DestCompInstance", "DestCompCategory",
    					     "DestPortName", "DestPortType"));
	    headers.addAll(propToConnections.keySet());
	    Table scnConnTable = new Table(headers);
    	for (ComponentImpl compImpl: compImpls) {
			if(compImpl.getBlockImpl()!=null) {
				List<verdict.vdm.vdm_model.Connection> compConnections = compImpl.getBlockImpl().getConnection();
				// --add rows to connections csv
				for (verdict.vdm.vdm_model.Connection compConnection2 : compConnections) {
					updateConnectionTable(compConnection2.getName(), compConnection2.getQualifiedName(), compConnection2.getSource(), compConnection2.getDestination(), scnConnTable, connectionAttributesMap, scenario, compImpl, compToCompImpl, propToConnections);
					HashMap<String, String> compImplToComp = new HashMap<String, String>();
					for (String key : compToCompImpl.keySet()){
						compImplToComp.put(compToCompImpl.get(key), key);
					}
				}
			}
		}
    	return scnConnTable;
	}
	private void updateConnectionTable(String connName, String connQualName, verdict.vdm.vdm_model.ConnectionEnd source, verdict.vdm.vdm_model.ConnectionEnd destination, 
			Table scnConnTable, Map<String, HashMap<String, String>> connectionAttributesMap, String scenario, ComponentImpl compImpl, Map<String, String> compToCompImpl, Map<String, HashSet<String>> propToConnections) {
		scnConnTable.addValue(scenario);
		String compQualName = compImpl.getType().getId();
		scnConnTable.addValue(compQualName);
		scnConnTable.addValue(replaceColonsWithUnderscore(compQualName));
		scnConnTable.addValue(compQualName.substring(0, compQualName.indexOf(':')));
		scnConnTable.addValue(compImpl.getType().getName());//component
		scnConnTable.addValue(compImpl.getName());//component implementation
		scnConnTable.addValue(connQualName);//connection qualified name
		scnConnTable.addValue(replaceColonsWithUnderscore(connQualName));//connection sanitized qualified name
		scnConnTable.addValue(connName);//connection name
		if(source.getComponentPort()!=null) {
			Port srcCompPort = source.getComponentPort();
			String fullQualNameSrcCompPort = srcCompPort.getId();
			//get the component name
			//remove port name from fullQualNameSrcCompPort and text before "::" and after "." to get the source component name
			String compName = fullQualNameSrcCompPort.substring(fullQualNameSrcCompPort.indexOf(':')+2,fullQualNameSrcCompPort.indexOf('.'));
			if(!compToCompImpl.containsKey(compName)) {
				throw new RuntimeException("Unable to find Component Implementation corresponding to "+compName);
			}
			scnConnTable.addValue("");//SrcCompInstQualifiedName
			scnConnTable.addValue("");//SanitizedSrcCompInstQualifiedName
			scnConnTable.addValue("");//SrcCompInstPackage
			scnConnTable.addValue(compName);//SrcComp
			scnConnTable.addValue(compToCompImpl.get(compName));//SrcImpl
			scnConnTable.addValue("");//SrcCompInstance
			scnConnTable.addValue(compImpl.getType().getCompCateg().toString());//SrcCompCategory
			scnConnTable.addValue(srcCompPort.getName());//SrcPortName
			scnConnTable.addValue(srcCompPort.getMode().value());//SrcPortType
		} else if (source.getSubcomponentPort()!=null) {
			String sourceInstQualName = source.getSubcomponentPort().getSubcomponent().getId();
			scnConnTable.addValue(sourceInstQualName);//SrcCompInstQualifiedName
			scnConnTable.addValue(replaceColonsWithUnderscore(sourceInstQualName));//SanitizedSrcCompInstQualifiedName
			scnConnTable.addValue(sourceInstQualName.substring(0, compQualName.indexOf(':')));//SrcCompInstPackage
			CompInstancePort srcCompPort = source.getSubcomponentPort();
			//get the component name
			scnConnTable.addValue(srcCompPort.getSubcomponent().getSpecification().getName());//SrcComp
			scnConnTable.addValue("");//SrcImpl
			scnConnTable.addValue(srcCompPort.getSubcomponent().getName());//SrcCompInstance
			scnConnTable.addValue(srcCompPort.getSubcomponent().getSpecification().getCompCateg());//SrcCompCategory
			scnConnTable.addValue(srcCompPort.getPort().getName());//SrcPortName
			scnConnTable.addValue(srcCompPort.getPort().getMode().value());//SrcPortType
		} else {
			throw new RuntimeException("Connection source has null values foe component port and subcomponent port");
		}
		if(destination.getComponentPort()!=null) {
			Port destCompPort = destination.getComponentPort();
			String fullQualNameDestCompPort = destCompPort.getId();
			//get the component name
			//remove port name from fullQualNameDestCompPort and text before "::" and after "." to get the destination component name
			String compName = fullQualNameDestCompPort.substring(fullQualNameDestCompPort.indexOf(':')+2,fullQualNameDestCompPort.indexOf('.'));
			if(!compToCompImpl.containsKey(compName)) {
				throw new RuntimeException("Unable to find Component Implementation corresponding to "+compName);
			}
			scnConnTable.addValue("");//DestCompInstQualifiedName
			scnConnTable.addValue("");//SanitizedDestCompInstQualifiedName
			scnConnTable.addValue("");//DestCompInstPackage
			scnConnTable.addValue(compName);//DestComp
			scnConnTable.addValue(compToCompImpl.get(compName));
			scnConnTable.addValue("");//DestCompInstance
			scnConnTable.addValue(compImpl.getType().getCompCateg().toString());//DestCompCategory
			scnConnTable.addValue(destCompPort.getName());//DestPortName
			scnConnTable.addValue(destCompPort.getMode().value());//DestPortType
		} else if (destination.getSubcomponentPort()!=null) {
			String destInstQualName = destination.getSubcomponentPort().getSubcomponent().getId();
			scnConnTable.addValue(destInstQualName);//DestCompInstQualifiedName
			scnConnTable.addValue(replaceColonsWithUnderscore(destInstQualName));//SanitizedDestCompInstQualifiedName
			scnConnTable.addValue(destInstQualName.substring(0, compQualName.indexOf(':')));//DestCompInstPackage
			CompInstancePort destCompPort = destination.getSubcomponentPort();
			//get the component name
			scnConnTable.addValue(destination.getSubcomponentPort().getSubcomponent().getSpecification().getName());//DestComp
			scnConnTable.addValue("");
			scnConnTable.addValue(destCompPort.getSubcomponent().getName());//DestCompInstance
			scnConnTable.addValue(destCompPort.getSubcomponent().getSpecification().getCompCateg());//DestCompCategory
			scnConnTable.addValue(destCompPort.getPort().getName());//DestPortName
			scnConnTable.addValue(destCompPort.getPort().getMode().value());//DestPortType
		} else {
			throw new RuntimeException("Connection destination has null values foe component port and subcomponent port");
		}
		//add connection attributes/properties
		HashMap<String,String> connAttrMap = connectionAttributesMap.get(connName);
		for(String propName: propToConnections.keySet()) {
			if (synthesis && DefenseProperties.MBAA_CONN_DEFENSE_PROPERTIES_SET.contains(propName)) {
				// this fools stem - --code synonymous to code by William in the previous aadl2csvtranslator
				scnConnTable.addValue("9");
			} else if (connAttrMap.containsKey(propName)) {//check if the connection has that property - add it to csv if it does
				scnConnTable.addValue(connAttrMap.get(propName));//connection property
			} else {
				scnConnTable.addValue("");
			}
		}
		scnConnTable.capRow();
	}
	//Assumption: component implementation containing the connection is same as the component implementation where the actual_connection_binding property was defined
	private Table updateBusBindingsTable(List<ComponentImpl> compImpls, String scenario, Map<String, String> compToCompImpl) {
		Table scnBusBindingsTable = new Table("Scenario", "QualifiedName", "SanitizedQualifiedName", "PackageName", "Comp", "Impl", "SrcCompInstQualifiedName", 
				"SanitizedSrcCompInstQualifiedName", "SrcCompInstPackage", "ActualConnectionBindingSrcComp","ActualConnectionBindingSrcImpl",
				"ActualConnectionBindingSrcCompInst", "SanitizedActualConnectionBindingSrcBusInstName","ActualConnectionBindingSrcBusInst",
				"DestCompInstQualifiedName", "SanitizedDestCompInstQualifiedName", "DestCompInstPackage", "ActualConnectionBindingDestConnComp", "ActualConnectionBindingDestConnImpl",
				"ActualConnectionBindingDestConnCompInst", "SanitizedActualConnectionBindingDestConnName", "ActualConnectionBindingDestConn");
		//creating hashmap (compImpl -> comp) from (comp -> compImpl)
		HashMap<String, String> compImplToComp = new HashMap<String, String>();
		for (String key : compToCompImpl.keySet()){
			compImplToComp.put(compToCompImpl.get(key), key);
		}
    	for (ComponentImpl compImpl: compImpls) {
    		if(compImpl.getBlockImpl()!=null) {
				List<verdict.vdm.vdm_model.Connection> compConnections = compImpl.getBlockImpl().getConnection();
				// --add rows to scnbusbindings csv
				for (verdict.vdm.vdm_model.Connection compConnection2 : compConnections) {
					if(compConnection2.getActualConnectionBinding()!=null) {
						String actualConnectionBinding = compConnection2.getActualConnectionBinding();
						scnBusBindingsTable.addValue(scenario);
						String compQualName = compImpl.getType().getId();
						scnBusBindingsTable.addValue(compQualName);//QualifiedName
						scnBusBindingsTable.addValue(replaceColonsWithUnderscore(compQualName));//SanitizedQualifiedName
						scnBusBindingsTable.addValue(compQualName.substring(0, compQualName.indexOf(':')));//PackageName
						scnBusBindingsTable.addValue(compImpl.getType().getName());//Comp
						scnBusBindingsTable.addValue(compImpl.getName());//Impl - component implementation		
						scnBusBindingsTable.addValue("");//SrcCompInstQualifiedName
						scnBusBindingsTable.addValue("");//SanitizedSrcCompInstQualifiedName
						scnBusBindingsTable.addValue("");//SrcCompInstPackage
						String bindingSubStr = actualConnectionBinding.substring(0,actualConnectionBinding.lastIndexOf("."));//everything except the instance name
						String implName = bindingSubStr.substring(bindingSubStr.lastIndexOf(":")+1,bindingSubStr.length());
						if(!compImplToComp.containsKey(implName)) {
							throw new RuntimeException("Unable to find Component corresponding to Implementation "+implName);
						}
						scnBusBindingsTable.addValue(compImplToComp.get(implName));//ActualConnectionBindingSrcComp
						scnBusBindingsTable.addValue(implName);//ActualConnectionBindingSrcImpl
						scnBusBindingsTable.addValue("");//ActualConnectionBindingSrcCompInst
						scnBusBindingsTable.addValue(replaceColonsWithUnderscore(actualConnectionBinding));//SanitizedActualConnectionBindingSrcBusInstName
						scnBusBindingsTable.addValue(actualConnectionBinding.substring(actualConnectionBinding.lastIndexOf('.')+1,actualConnectionBinding.length()));//ActualConnectionBindingSrcBusInst
						scnBusBindingsTable.addValue("");//DestCompInstQualifiedName
						scnBusBindingsTable.addValue("");//SanitizedDestCompInstQualifiedName
						scnBusBindingsTable.addValue("");//DestCompInstPackage
						scnBusBindingsTable.addValue(compImpl.getType().getName());//ActualConnectionBindingDestConnComp
						scnBusBindingsTable.addValue(compImpl.getName());//ActualConnectionBindingDestConnImpl
						scnBusBindingsTable.addValue("");//ActualConnectionBindingDestConnCompInst
						scnBusBindingsTable.addValue(replaceColonsWithUnderscore(compConnection2.getQualifiedName()));//SanitizedActualConnectionBindingDestConnName
						scnBusBindingsTable.addValue(compConnection2.getName());//ActualConnectionBindingDestConn
						scnBusBindingsTable.capRow();
					}
				}
    		}
		}
    	return scnBusBindingsTable;
	}	
	private Table updateCompInstTable(List<ComponentImpl> compImpls, String scenario, Map<String, HashSet<String>> propToCompInsts, Map<String, HashMap<String, String>> compInstAttributesMap, Map<String, String> compToCompImpl) {
		//update component instances prop table
		List<String> headers = new ArrayList<String>(Arrays.asList("Scenario", "QualifiedName", "SanitizedQualifiedName", "PackageName", "Comp", "Impl", "CompInstance"));
	    headers.addAll(propToCompInsts.keySet());
	    Table scnCompPropsTable = new Table(headers);
    	for (ComponentImpl compImpl: compImpls) {
    		if(compImpl.getBlockImpl()!=null) {
    			List<verdict.vdm.vdm_model.ComponentInstance> compInstances = compImpl.getBlockImpl().getSubcomponent();
    			// --add rows to comp properties csv
    			for (verdict.vdm.vdm_model.ComponentInstance compInst : compInstances) {
    				updateCompPropsTable(compInst.getName(), compInst, scnCompPropsTable, compInstAttributesMap, scenario, compImpl, compToCompImpl, propToCompInsts);
    			}
    		}
		}
    	return scnCompPropsTable;
	}
	private void updateCompPropsTable(String compInstName, ComponentInstance compInst, Table scnCompPropsTable,
			Map<String, HashMap<String, String>> compInstAttributesMap, String scenario, ComponentImpl compImpl,
			Map<String, String> compToCompImpl, Map<String, HashSet<String>> propToCompInsts) {
		scnCompPropsTable.addValue(scenario);
		String compQualName = compInst.getId();
		scnCompPropsTable.addValue(compQualName);
		scnCompPropsTable.addValue(replaceColonsWithUnderscore(compQualName));
		scnCompPropsTable.addValue(compQualName.substring(0, compQualName.indexOf(':')));
		scnCompPropsTable.addValue(compInst.getSpecification().getName());//comp
		if(compInst.getImplementation()!=null) {
			scnCompPropsTable.addValue(compInst.getImplementation().getName());//impl
		} else {
			scnCompPropsTable.addValue("");//impl
		}
		scnCompPropsTable.addValue(compInstName);//comp instance
		//add connection attributes/properties
		HashMap<String,String> connAttrMap = compInstAttributesMap.get(compInst.getId());
		for(String propName: propToCompInsts.keySet()) {
			//check if the component has that property - add it to csv if it does
			if (synthesis && DefenseProperties.MBAA_COMP_DEFENSE_PROPERTIES_SET.contains(propName)) {
				// this fools stem - --code synonymous to code by William in the previous aadl2csvtranslator
				scnCompPropsTable.addValue("9");
			} else if (connAttrMap.containsKey(propName)) {
				scnCompPropsTable.addValue(connAttrMap.get(propName));//connection property
			} else {
				scnCompPropsTable.addValue("");
			}
		}
		scnCompPropsTable.capRow();
	}
	private void updateCyberSafetyReqsMaps(List<verdict.vdm.vdm_model.CyberReq> cyberReqs, Map<String, verdict.vdm.vdm_model.CyberReq> cyberReqsMap, List<verdict.vdm.vdm_model.SafetyReq> safetyReqs, Map<String, verdict.vdm.vdm_model.SafetyReq> safetyReqsMap) {
		for (verdict.vdm.vdm_model.CyberReq cyberReq:cyberReqs) {
			cyberReqsMap.put(cyberReq.getId(), cyberReq);
		}	
		for (verdict.vdm.vdm_model.SafetyReq safetyReq:safetyReqs) {
			safetyReqsMap.put(safetyReq.getId(), safetyReq);
		}	
	}
	private void processMissionReqs(List<verdict.vdm.vdm_model.Mission> missionReqs, Map<String, verdict.vdm.vdm_model.CyberReq> cyberReqsMap, Map<String, verdict.vdm.vdm_model.SafetyReq> safetyReqsMap, Table missionTable, String scenario, Map<String, List<ConnectionEnd>> connectionDestToSourceMap) {
		for(verdict.vdm.vdm_model.Mission missionReq : missionReqs) {
			List<String> cyberReqIds= missionReq.getCyberReqs();//this may include safety requirements
			for (String cyberReqId : cyberReqIds) {
				//get cyberReq Definition
				if(cyberReqsMap.containsKey(cyberReqId)) {
					verdict.vdm.vdm_model.CyberReq cyberReqDef = cyberReqsMap.get(cyberReqId);
					//get the condition of the cyberReq
					CyberExpr cyberReqCondition = cyberReqDef.getCondition();
					//condition could be an OR expression containing multiple expressions
					//or just single port and CIA
					if (cyberReqCondition.getKind()!=null) {//if condition is an OR expression get individual sub-expressions
						if(cyberReqCondition.getKind().toString().equalsIgnoreCase("Or")) {
							List<CyberExpr> subCyberCondList =cyberReqCondition.getOr().getExpr();
							for (CyberExpr subCyberCond: subCyberCondList) {
								updateCyberMissionsTable(missionTable, scenario, missionReq.getId(), cyberReqId, cyberReqDef.getCia(), cyberReqDef.getSeverity(), subCyberCond, connectionDestToSourceMap);
							}
						} else if(cyberReqCondition.getKind().toString().equalsIgnoreCase("And")) {
								updateAndExprCyberMissionsTable(missionTable, scenario, missionReq.getId(), cyberReqId, cyberReqDef.getCia(), cyberReqDef.getSeverity(), cyberReqCondition, connectionDestToSourceMap);
						} else {
							throw new RuntimeException("Expression used as condition in Cyber Requirement is not supported.");
						}
					} else {//if condition has single port and CIa
						updateCyberMissionsTable(missionTable, scenario, missionReq.getId(), cyberReqId, cyberReqDef.getCia(), cyberReqDef.getSeverity(), cyberReqCondition, connectionDestToSourceMap);
					}
				} else if (safetyReqsMap.containsKey(cyberReqId)) {
					verdict.vdm.vdm_model.SafetyReq safetyReqDef = safetyReqsMap.get(cyberReqId);
					//get the condition of the cyberReq
					verdict.vdm.vdm_model.SafetyReqExpr safetyReqCondition = safetyReqDef.getCondition();
					//condition could be an OR expression containing multiple expressions
					//or just single port and IA
					if (safetyReqCondition.getKind()!=null) {//if condition is an OR expression get individual sub-expressions
						if(safetyReqCondition.getKind().toString().equalsIgnoreCase("Or")) {
							List<verdict.vdm.vdm_model.SafetyReqExpr> subSafetyExprCondList =safetyReqCondition.getOr().getExpr();
							for (verdict.vdm.vdm_model.SafetyReqExpr subCyberCond: subSafetyExprCondList) {
								updateSafetyMissionsTable(missionTable, scenario, missionReq.getId(), cyberReqId, safetyReqDef.getTargetProbability(), subCyberCond, connectionDestToSourceMap);
							}
						} else if(safetyReqCondition.getKind().toString().equalsIgnoreCase("And")) {
							updateAndExprSafetyMissionsTable(missionTable, scenario, missionReq.getId(), cyberReqId, safetyReqDef.getTargetProbability(), safetyReqCondition, connectionDestToSourceMap);
						} else {
							throw new RuntimeException("Expression used as condition in Safety Requirement is not supported.");
						}
					} else {//if condition has single port and CIa
						updateSafetyMissionsTable(missionTable, scenario, missionReq.getId(), cyberReqId, safetyReqDef.getTargetProbability(), safetyReqCondition, connectionDestToSourceMap);
					}
				} else {
					throw new RuntimeException("Undefined cyber or safety requirement used in mission requirement.");
				}
			}
		}
	}
	private void updateAndExprCyberMissionsTable(Table missionTable, String scenario, String missionReqId, String cyberReqId,
			CIA cyberCIA, Severity cyberSeverity, CyberExpr cyberExpr,
			Map<String, List<ConnectionEnd>> connectionDestToSourceMap) {
		missionTable.addValue(scenario);
		missionTable.addValue(sanitizeValue(missionReqId));
		missionTable.addValue(""); // MissionReq
		missionTable.addValue(cyberReqId);
		missionTable.addValue(""); // Req
		//get cia and add as MissionImpactCIA
		missionTable.addValue(formatToSmall(cyberCIA.toString()));
		missionTable.addValue(""); // Effect
		//get and add severity
		missionTable.addValue(formatToSmall(cyberSeverity.name()));
		//get concatenated port's linked source instances and concatenated CIAs
		String[] portsCIAs = new String[3];
		portsCIAs[0] = "";
		portsCIAs[1] = "";
		portsCIAs[2] = "";
		getPortsAndCIAsForMission(cyberExpr, portsCIAs, connectionDestToSourceMap);		
		//add it to table
		missionTable.addValue(portsCIAs[0]);
		missionTable.addValue(portsCIAs[1]);
		missionTable.addValue(portsCIAs[2]);
		missionTable.addValue("Cyber");
		missionTable.capRow();
	}
	private void updateCyberMissionsTable(Table missionTable, String scenario, String missionReqId, String cyberReqId,
			verdict.vdm.vdm_model.CIA cyberCIA, verdict.vdm.vdm_model.Severity cyberSeverity, CyberExpr cyberReqCondition, Map<String, List<ConnectionEnd>> connectionDestToSourceMap) {		
		//get port's linked source instance
		if(connectionDestToSourceMap.containsKey(cyberReqCondition.getPort().getName())) {
			List<ConnectionEnd> linkedSourcePorts = connectionDestToSourceMap.get(cyberReqCondition.getPort().getName());
			for (ConnectionEnd linkedSourcePort : linkedSourcePorts) {
				if(linkedSourcePort.getSubcomponentPort()!=null) {
					missionTable.addValue(scenario);
					missionTable.addValue(sanitizeValue(missionReqId));
					missionTable.addValue(""); // MissionReq
					missionTable.addValue(cyberReqId);
					missionTable.addValue(""); // Req
					//get cia and add as MissionImpactCIA
					missionTable.addValue(formatToSmall(cyberCIA.toString()));
					missionTable.addValue(""); // Effect
					//get and add severity
					missionTable.addValue(formatToSmall(cyberSeverity.name()));
					CompInstancePort destCompPort = linkedSourcePort.getSubcomponentPort();
					missionTable.addValue(destCompPort.getSubcomponent().getName());
					missionTable.addValue(destCompPort.getPort().getName());
					//get CIA and add it to table
					missionTable.addValue(formatToSmall(cyberReqCondition.getPort().getCia().name()));
					missionTable.addValue("Cyber");
					missionTable.capRow();
				} else {
					throw new RuntimeException("Linked Source Port has no instance information");
				}
			}
		} else {
			throw new RuntimeException("Missing component instance dependency. "+cyberReqCondition.getPort().getName()+" is not linked to a source port");
		}
	}
	private void updateAndExprSafetyMissionsTable(Table missionTable, String scenario, String missionReqId, String cyberReqId,
			String targetProbability, SafetyReqExpr subCyberCond, Map<String, List<ConnectionEnd>> connectionDestToSourceMap) {
		missionTable.addValue(scenario);
		missionTable.addValue(sanitizeValue(missionReqId));
		missionTable.addValue(""); // MissionReq
		missionTable.addValue(cyberReqId);
		missionTable.addValue(""); // Req
		//get cia and add as MissionImpactCIA
		missionTable.addValue("");
		missionTable.addValue(""); // Effect
		//get and add severity
		missionTable.addValue(targetProbability);
		//get concatenated port's linked source instances and concatenated CIAs
		String[] portsIAs = new String[3];
		portsIAs[0] = "";
		portsIAs[1] = "";
		portsIAs[2] = "";
		getPortsAndIAsForMission(subCyberCond, portsIAs, connectionDestToSourceMap);
		//add IA it to table
		missionTable.addValue(portsIAs[0]);
		missionTable.addValue(portsIAs[1]);
		missionTable.addValue(portsIAs[2]);
		missionTable.addValue("Safety");
		missionTable.capRow();
	}

	private void updateSafetyMissionsTable(Table missionTable, String scenario, String missionReqId, String cyberReqId,
			 String probability, SafetyReqExpr safetyReqCondition, Map<String, List<ConnectionEnd>> connectionDestToSourceMap) {
		if(connectionDestToSourceMap.containsKey(safetyReqCondition.getPort().getName())) {
			List<ConnectionEnd> linkedSourcePorts = connectionDestToSourceMap.get(safetyReqCondition.getPort().getName());
			for (ConnectionEnd linkedSourcePort : linkedSourcePorts) {
				if(linkedSourcePort.getSubcomponentPort()!=null) {
					CompInstancePort destCompPort = linkedSourcePort.getSubcomponentPort();
					missionTable.addValue(scenario);
					missionTable.addValue(sanitizeValue(missionReqId));
					missionTable.addValue(""); // MissionReq
					missionTable.addValue(cyberReqId);
					missionTable.addValue(""); // Req
					//get cia and add as MissionImpactCIA
					missionTable.addValue("");
					missionTable.addValue(""); // Effect
					//get and add severity
					missionTable.addValue(probability);
					//get port's linked source instance
					missionTable.addValue(destCompPort.getSubcomponent().getName());
					missionTable.addValue(destCompPort.getPort().getName());
					//get CIA and add it to table
					missionTable.addValue(formatToSmall(safetyReqCondition.getPort().getIa().name()));
					missionTable.addValue("Safety");
					missionTable.capRow();
				} else {
					throw new RuntimeException("Linked Source Port has no instance information");
				}
			}
		} else {
			throw new RuntimeException("Missing component instance dependency. "+safetyReqCondition.getPort().getName()+" is not linked to a source port");
		}
	}
	private void processComponentTypes(List<ComponentType> compTypes, Table eventsTable, Table compDepTable, Table compSafTable) {
		//each ComponentType contains id, name, compCateg, List<Port>, ContractSpec, List<CyberRel>, List<SafetyRel>, List<Event>
		for (ComponentType compType : compTypes) {
			String compTypeName = compType.getName();
			String compQualName = compType.getId();
			String packageName = compQualName.substring(0, compQualName.indexOf(':'));
			//populate compTypeNameToEvents,  compTypeNameToCyberRels, compTypeNameToSafetyRels
			updateEventsTable(eventsTable, compQualName, packageName, compTypeName, compType.getEvent());
			updateCompDepTable(compDepTable, compQualName, packageName, compTypeName, compType.getCyberRel());
			updateCompSafTable(compSafTable, compQualName, packageName, compTypeName, compType.getSafetyRel());
		}
	}
	private void updateCompSafTable(Table compSafTable, String qualNameComp, String packageName, String compTypeName,
			List<verdict.vdm.vdm_model.SafetyRel> safetyRels) {
		for(verdict.vdm.vdm_model.SafetyRel safetyRel : safetyRels) {
			updateCompSafTable(compSafTable, qualNameComp, packageName, compTypeName, safetyRel.getFaultSrc(), safetyRel.getOutput());
		}
	}
	private void updateCompSafTable(Table compSafTable, String qualNameComp, String packageName, String compTypeName,
			verdict.vdm.vdm_model.SafetyRelExpr safetyRelExpr, IAPort outputIAPort) {
		if(safetyRelExpr.getKind()==null) {
			if(safetyRelExpr.getFault()!=null) {
				compSafTable.addValue(qualNameComp);
				compSafTable.addValue(replaceColonsWithUnderscore(qualNameComp));
				compSafTable.addValue(packageName);
				compSafTable.addValue(compTypeName);
				compSafTable.addValue(safetyRelExpr.getFault().getEventName());
				compSafTable.addValue(safetyRelExpr.getFault().getHappens().toString());
				compSafTable.addValue(outputIAPort.getName());
				compSafTable.addValue(formatToSmall(outputIAPort.getIa().name()));
				compSafTable.capRow();
			} else if(safetyRelExpr.getPort()!=null){
				compSafTable.addValue(qualNameComp);
				compSafTable.addValue(replaceColonsWithUnderscore(qualNameComp));
				compSafTable.addValue(packageName);
				compSafTable.addValue(compTypeName);
				compSafTable.addValue(safetyRelExpr.getPort().getName());
				compSafTable.addValue(formatToSmall(safetyRelExpr.getPort().getIa().name()));
				compSafTable.addValue(outputIAPort.getName());
				compSafTable.addValue(formatToSmall(outputIAPort.getIa().name()));
				compSafTable.capRow();
			} else {
				throw new RuntimeException("Safety Expression has null values for expr-kind and event-happens/fault");
			}
		} else if (safetyRelExpr.getKind().toString().equalsIgnoreCase("Or")){
			List<SafetyRelExpr> subInpSafList =safetyRelExpr.getOr().getExpr();
			for (SafetyRelExpr subInpSafExpr: subInpSafList) {
				updateCompSafTable(compSafTable, qualNameComp, packageName, compTypeName, subInpSafExpr, outputIAPort);
			}
		} else if (safetyRelExpr.getKind().toString().equalsIgnoreCase("And")){
			updateCompSafTableWithAndExpr(compSafTable, qualNameComp, packageName, compTypeName, safetyRelExpr, outputIAPort);
		} else {
			throw new RuntimeException("Expression used as Safety Relation input is not supported.");
		}
	}
	//Assuming that the sub-expression in the safety relation is AND over port-IA types
	private void updateCompSafTableWithAndExpr(Table compSafTable, String qualNameComp, String packageName,
			String compTypeName, SafetyRelExpr safetyRelExpr, IAPort outputIAPort) {		
		//get input ports and input cia concatenated
		String[] portsIAs = new String[2];
		portsIAs[0] = "";
		portsIAs[1] = "";
		getPortsAndIAsForCompSaf(safetyRelExpr, portsIAs);
		compSafTable.addValue(qualNameComp);
		compSafTable.addValue(replaceColonsWithUnderscore(qualNameComp));
		compSafTable.addValue(packageName);
		compSafTable.addValue(compTypeName);
		compSafTable.addValue(portsIAs[0]);
		compSafTable.addValue(portsIAs[1]);
		compSafTable.addValue(outputIAPort.getName());
		compSafTable.addValue(formatToSmall(outputIAPort.getIa().name()));
		compSafTable.capRow();
	}
	private void getPortsAndIAsForCompSaf(SafetyRelExpr safetyRelExpr, String[] portsIAs) {
		if(safetyRelExpr.getKind()==null) {
			if (portsIAs[0].equalsIgnoreCase("")) {
				portsIAs[0] = safetyRelExpr.getPort().getName();
				portsIAs[1] = formatToSmall(safetyRelExpr.getPort().getIa().name());
			} else {
				portsIAs[0] = portsIAs[0] + ";"+ safetyRelExpr.getPort().getName();
				portsIAs[1] = portsIAs[1] + ";" + formatToSmall(safetyRelExpr.getPort().getIa().name());
			}
		} else if (safetyRelExpr.getKind().toString().equalsIgnoreCase("And")){
			List<SafetyRelExpr> subInpCyberList =safetyRelExpr.getAnd().getExpr();
			for (SafetyRelExpr subInpSafetyExpr: subInpCyberList) {
				getPortsAndIAsForCompSaf(subInpSafetyExpr, portsIAs);
			}
		} else {
			throw new RuntimeException("VERDICT only supports DNF in safety relations.");
		}
		
	}
	private String formatToSmall(String name) {
		String updName = name;
		if(name.length()>1) {
			updName = name.substring(0, 1) + name.substring(1).toLowerCase();
		}
		return updName;
	}
	private void updateCompDepTable(Table compDepTable, String qualNameComp, String packageName, String compTypeName,
			List<verdict.vdm.vdm_model.CyberRel> cyberRels) {
    	for(verdict.vdm.vdm_model.CyberRel cyberRel : cyberRels) {
    		if(cyberRel.getInputs()!=null) {
    			updateCompDepTable(compDepTable, qualNameComp, packageName, compTypeName, cyberRel.getInputs(), cyberRel.getOutput());
    		} else {
    			compDepTable.addValue(qualNameComp);
    			compDepTable.addValue(replaceColonsWithUnderscore(qualNameComp));
    			compDepTable.addValue(packageName);
    			compDepTable.addValue(compTypeName);
    			compDepTable.addValue("");
				compDepTable.addValue("");
				compDepTable.addValue(cyberRel.getOutput().getName());
				compDepTable.addValue(formatToSmall(cyberRel.getOutput().getCia().name()));
				compDepTable.capRow();
    		}
    	}
	}
	private void updateCompDepTable(Table compDepTable, String qualNameComp, String packageName, String compTypeName, CyberExpr inputCyberExpr, CIAPort outputCIAPort) {
		if(inputCyberExpr.getKind()==null) {//expression is not an AND, OR, NOT expression
			compDepTable.addValue(qualNameComp);
			compDepTable.addValue(replaceColonsWithUnderscore(qualNameComp));
			compDepTable.addValue(packageName);
			compDepTable.addValue(compTypeName);
			CIAPort inpCIAPort = inputCyberExpr.getPort();
			compDepTable.addValue(inpCIAPort.getName());
			compDepTable.addValue(formatToSmall(inpCIAPort.getCia().name()));
			compDepTable.addValue(outputCIAPort.getName());
			compDepTable.addValue(formatToSmall(outputCIAPort.getCia().name()));
			compDepTable.capRow();
		} else if (inputCyberExpr.getKind().toString().equalsIgnoreCase("Or")) {
			List<CyberExpr> subInpCyberList =inputCyberExpr.getOr().getExpr();
			for (CyberExpr subInpCyberExpr: subInpCyberList) {
				updateCompDepTable(compDepTable, qualNameComp, packageName, compTypeName, subInpCyberExpr, outputCIAPort);
			}
		} else if (inputCyberExpr.getKind().toString().equalsIgnoreCase("And")) {
			updateCompDepTableWithAndCyberExpr(compDepTable, qualNameComp, packageName, compTypeName, inputCyberExpr, outputCIAPort);
		} else {
			throw new RuntimeException("Expression used as Cyber Relation input is not supported.");
		}
	}
	private void updateCompDepTableWithAndCyberExpr(Table compDepTable, String qualNameComp, String packageName,
			String compTypeName, CyberExpr inputCyberExpr, CIAPort outputCIAPort) {
		//get input ports and input cia concatenated
		String[] portsCIAs = new String[2];
		portsCIAs[0] = "";
		portsCIAs[1] = "";
		getPortsAndCIAsForCompDepTable(inputCyberExpr, portsCIAs);
		//add row to comp dep table
		compDepTable.addValue(qualNameComp);
		compDepTable.addValue(replaceColonsWithUnderscore(qualNameComp));
		compDepTable.addValue(packageName);
		compDepTable.addValue(compTypeName);
		compDepTable.addValue(portsCIAs[0]);
		compDepTable.addValue(portsCIAs[1]);
		compDepTable.addValue(outputCIAPort.getName());
		compDepTable.addValue(formatToSmall(outputCIAPort.getCia().name()));
		compDepTable.capRow();
	}
	private void getPortsAndCIAsForCompDepTable(CyberExpr inputCyberExpr, String[] portsCIAs) {
		if(inputCyberExpr.getKind()==null) {
			if (portsCIAs[0].equalsIgnoreCase("")) {
				portsCIAs[0] = inputCyberExpr.getPort().getName();
				portsCIAs[1] = formatToSmall(inputCyberExpr.getPort().getCia().name());
			} else {
				portsCIAs[0] = portsCIAs[0] + ";"+ inputCyberExpr.getPort().getName();
				portsCIAs[1] = portsCIAs[1] + ";" + formatToSmall(inputCyberExpr.getPort().getCia().name());
			}
		} else if (inputCyberExpr.getKind().toString().equalsIgnoreCase("And")){
			List<CyberExpr> subInpCyberList =inputCyberExpr.getAnd().getExpr();
			for (CyberExpr subInpCyberExpr: subInpCyberList) {
				getPortsAndCIAsForCompDepTable(subInpCyberExpr, portsCIAs);
			}
		} else {
			throw new RuntimeException("VERDICT only supports DNF in cyber relations.");
		}
	}
	private void getPortsAndCIAsForMission(CyberExpr inputCyberExpr, String[] portsCIAs, Map<String, List<ConnectionEnd>> connectionDestToSourceMap) {
		if(inputCyberExpr.getKind()==null) {
			//get port's linked source instance
			if(connectionDestToSourceMap.containsKey(inputCyberExpr.getPort().getName())) {
				List<ConnectionEnd> linkedSourcePorts = connectionDestToSourceMap.get(inputCyberExpr.getPort().getName());
				if(linkedSourcePorts.size()>1) {
					throw new RuntimeException("Multiple Linked Source Ports is unexpected for ports in cyber expression.");
				}
				if(linkedSourcePorts.get(0).getSubcomponentPort()!=null) {
					CompInstancePort depCompPort = linkedSourcePorts.get(0).getSubcomponentPort();
					if (portsCIAs[0].equalsIgnoreCase("")) {
						portsCIAs[0] = depCompPort.getSubcomponent().getName();
						portsCIAs[1] = depCompPort.getPort().getName();
						portsCIAs[2] = formatToSmall(inputCyberExpr.getPort().getCia().name());
					} else {
						portsCIAs[0] = portsCIAs[0] + ";"+ depCompPort.getSubcomponent().getName();
						portsCIAs[1] = portsCIAs[1] + ";" + depCompPort.getPort().getName();
						portsCIAs[2] = portsCIAs[2] + ";"+ formatToSmall(inputCyberExpr.getPort().getCia().name());
					}
				} else {
					throw new RuntimeException("Linked Source Port has no instance information");
				}
			} else {
				throw new RuntimeException("Missing component instance dependency. "+inputCyberExpr.getPort().getName()+" is not linked to a source port");
			}
		} else if (inputCyberExpr.getKind().toString().equalsIgnoreCase("And")){
			List<CyberExpr> subInpCyberList =inputCyberExpr.getAnd().getExpr();
			for (CyberExpr subInpCyberExpr: subInpCyberList) {
				getPortsAndCIAsForMission(subInpCyberExpr, portsCIAs, connectionDestToSourceMap);
			}
		} else {
			throw new RuntimeException("VERDICT only supports DNF in cyber requirements.");
		}
	}
	private void getPortsAndIAsForMission(SafetyReqExpr inputSafetyExpr, String[] portsIAs,
			Map<String, List<ConnectionEnd>> connectionDestToSourceMap) {		
		if(inputSafetyExpr.getKind()==null) {
			//get port's linked source instance
			if(connectionDestToSourceMap.containsKey(inputSafetyExpr.getPort().getName())) {
				List<ConnectionEnd> linkedSourcePorts = connectionDestToSourceMap.get(inputSafetyExpr.getPort().getName());
				if(linkedSourcePorts.size()>1) {
					throw new RuntimeException("Multiple Linked Source Ports is unexpected for ports in cyber expression.");
				}
				if(linkedSourcePorts.get(0).getSubcomponentPort()!=null) {
					CompInstancePort destCompPort = linkedSourcePorts.get(0).getSubcomponentPort();
					if(portsIAs[0].equalsIgnoreCase("")) {
						portsIAs[0] = destCompPort.getSubcomponent().getName();
						portsIAs[1] = destCompPort.getPort().getName();
						//get IA
						portsIAs[2] = formatToSmall(inputSafetyExpr.getPort().getIa().name());
					} else {
						portsIAs[0] = portsIAs[0]  + ";" + destCompPort.getSubcomponent().getName();
						portsIAs[1] = portsIAs[1]  + ";" + destCompPort.getPort().getName();
						//get IA
						portsIAs[2] = portsIAs[2]  + ";" + formatToSmall(inputSafetyExpr.getPort().getIa().name());						
					}
				} else {
					throw new RuntimeException("Linked Source Port has no instance information");
				}
			} else {
				throw new RuntimeException("Missing component instance dependency. "+inputSafetyExpr.getPort().getName()+" is not linked to a source port");
			}
		} else if (inputSafetyExpr.getKind().toString().equalsIgnoreCase("And")) {
			List<SafetyReqExpr> subInpSafetyList =inputSafetyExpr.getAnd().getExpr();
			for (SafetyReqExpr subInpSafetyExpr: subInpSafetyList) {
				getPortsAndIAsForMission(subInpSafetyExpr, portsIAs, connectionDestToSourceMap);
			}
		} else {
			throw new RuntimeException("VERDICT only supports DNF in safety requirements.");
		}
	}
	private void updateEventsTable(Table eventsTable, String qualNameComp, String packageName, String compTypeName, List<verdict.vdm.vdm_model.Event> events) {
		for(verdict.vdm.vdm_model.Event event : events) {
			eventsTable.addValue(qualNameComp);
			eventsTable.addValue(replaceColonsWithUnderscore(qualNameComp));
			eventsTable.addValue(packageName);
			eventsTable.addValue(sanitizeValue(compTypeName));
			eventsTable.addValue(sanitizeValue(event.getId()));
			eventsTable.addValue(sanitizeValue(event.getProbability()));
    		eventsTable.capRow();
    	}
		
	}
	String replaceColonsWithUnderscore(String inpStr) {
		if(inpStr == null) {return "";}
		inpStr = inpStr.replaceAll("(:)+", "_");
		return inpStr;
	}
	
    /**
     * @author Paul Meng
     * To make sure the input is not null
     * */
    String sanitizeValue(String val) {
    	return val == null ? "" : val;
    }
}
