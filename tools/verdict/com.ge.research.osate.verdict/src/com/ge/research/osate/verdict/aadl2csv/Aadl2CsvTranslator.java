package com.ge.research.osate.verdict.aadl2csv;

import org.osate.aadl2.SystemImplementation;
import org.osate.aadl2.SystemSubcomponent;
import org.osate.aadl2.SystemType;
import org.osate.aadl2.impl.AbstractSubcomponentImpl;
import org.osate.aadl2.impl.BooleanLiteralImpl;
import org.osate.aadl2.impl.DeviceSubcomponentImpl;
import org.osate.aadl2.impl.EnumerationLiteralImpl;
import org.osate.aadl2.impl.IntegerLiteralImpl;
import org.osate.aadl2.impl.ListValueImpl;
import org.osate.aadl2.impl.MetaclassReferenceImpl;
import org.osate.aadl2.impl.NamedValueImpl;
import org.osate.aadl2.impl.ProcessSubcomponentImpl;
import org.osate.aadl2.impl.PropertySetImpl;
import org.osate.aadl2.impl.ReferenceValueImpl;
import org.osate.aadl2.impl.SystemSubcomponentImpl;
import org.osate.aadl2.properties.PropertyAcc;
import org.osate.xtext.aadl2.Aadl2StandaloneSetup;
import org.osate.aadl2.AbstractImplementation;
import org.osate.aadl2.AbstractSubcomponent;
import org.osate.aadl2.AccessType;
import org.osate.aadl2.AnnexSubclause;
import org.osate.aadl2.BusImplementation;
import org.osate.aadl2.BusSubcomponent;
import org.osate.aadl2.ComponentImplementation;
import org.osate.aadl2.Connection;
import org.osate.aadl2.ConnectionEnd;
import org.osate.aadl2.ContainedNamedElement;
import org.osate.aadl2.ContainmentPathElement;
import org.osate.aadl2.Context;
import org.osate.aadl2.DataAccess;
import org.osate.aadl2.DataImplementation;
import org.osate.aadl2.DataPort;
import org.osate.aadl2.DataSubcomponent;
import org.osate.aadl2.DeviceImplementation;
import org.osate.aadl2.DeviceSubcomponent;
import org.osate.aadl2.EventDataPort;
import org.osate.aadl2.ModalPropertyValue;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.PortConnection;
import org.osate.aadl2.ProcessImplementation;
import org.osate.aadl2.ProcessSubcomponent;
import org.osate.aadl2.Property;
import org.osate.aadl2.PropertyAssociation;
import org.osate.aadl2.PropertyExpression;
import org.osate.aadl2.PropertyOwner;
import org.osate.aadl2.Subcomponent;

import com.ge.research.osate.verdict.dsl.VerdictUtil;
import com.ge.research.osate.verdict.dsl.verdict.CyberMission;
import com.ge.research.osate.verdict.dsl.verdict.CyberRel;
import com.ge.research.osate.verdict.dsl.verdict.CyberReq;
import com.ge.research.osate.verdict.dsl.verdict.Event;
import com.ge.research.osate.verdict.dsl.verdict.FExpr;
import com.ge.research.osate.verdict.dsl.verdict.LAnd;
import com.ge.research.osate.verdict.dsl.verdict.LExpr;
import com.ge.research.osate.verdict.dsl.verdict.LOr;
import com.ge.research.osate.verdict.dsl.verdict.LPort;
import com.ge.research.osate.verdict.dsl.verdict.SLAnd;
import com.ge.research.osate.verdict.dsl.verdict.SLExpr;
import com.ge.research.osate.verdict.dsl.verdict.SLOr;
import com.ge.research.osate.verdict.dsl.verdict.SLPort;
import com.ge.research.osate.verdict.dsl.verdict.SafetyRel;
import com.ge.research.osate.verdict.dsl.verdict.SafetyReq;
import com.ge.research.osate.verdict.dsl.verdict.Statement;
import com.ge.research.osate.verdict.dsl.verdict.Verdict;
import com.google.inject.Injector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.emf.ecore.resource.Resource;

/**
*
* @author Paul Meng
*
*/
public class Aadl2CsvTranslator {
	String scenario = "";
	List<ComponentImplementation> allImpls = new ArrayList<>();
	Map<Property, String> connPropertyToName = new LinkedHashMap<>();
	Map<Property, String> systemPropertyToName = new LinkedHashMap<>();
	Map<String, List<Event>> compTypeNameToEvents = new LinkedHashMap<>();
	Map<String, List<CyberReq>> compTypeNameToCyberReqs = new LinkedHashMap<>();
	Map<String, List<CyberRel>> compTypeNameToCyberRels = new LinkedHashMap<>();
	Map<String, ComponentImplementation> compTypeNameToImpl = new LinkedHashMap<>();
	Map<String, List<SafetyReq>> compTypeNameToSafetyReqs = new LinkedHashMap<>();
	Map<String, List<SafetyRel>> compTypeNameToSafetyRels = new LinkedHashMap<>();
	Map<String, List<CyberMission>> compTypeNameToMissions = new LinkedHashMap<>();
	Map<ComponentImplementation, List<Connection>> sysImplToConns = new LinkedHashMap<>();
	Map<ComponentImplementation, List<PortConnection>> implToAppliesToConnsList = new HashMap<>();
	Map<PortConnection, List<String[]>> connToCompImplInstBusBusNamesList = new HashMap<>();
	
	
	/**
	 * Execute a sequence of commands
	 * 1. Populate the data structure
	 * 2. Build tables for STEM: 
	 *        ScnCompProps.csv, ScnConnections.csv, ScnBusBindings.csv
	 * 3. Build tables for Soteria++: 
	 *        CompDep.csv, Mission.csv, CompSaf.csv, 
	 *        Events.csv, ScnCompProps.csv, ScnConnections.csv
	 * 4. Output the csv files
	 * */
	public void execute(File inputDir, String stemDir, String soteriaDir) {
		logHeader("AADL2CSV");
		populateDataFromAadlObjects(preprocessAadlFiles(inputDir));
		Table scnCompPropsTable = buildScnCompPropsTable();
		Table eventsTable = buildEventsTable();
		Table compSafTable = buildCompSafTable();
		Table compDepTable = buildCompDepTable();
		Table missionTable = buildMissionTable();
		Table scnConnTable = buildScnConnectionsTable();
		Table scnBusBindingsTable = buildScnBusBindingsTable();
		
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
	
    private static void logLine() {
        System.out.println(
                "******************************************************************"
                        + "******************************************************");
    }
    

    private static void logHeader(String header) {
        System.out.println();
        logLine();
        System.out.println("      " + header);
        logLine();
        System.out.println();
    }    
	
	/**
	 * Assume the input model is correct without any syntax errors
	 * Populate mission req, cyber and safety reqs and rels from AADL objects
	 * */
	public void populateDataFromAadlObjects(List<EObject> objects) { 		
		List<SystemType> systemTypes = new ArrayList<>();

		
		for(EObject obj : objects) {
			if (obj instanceof SystemType) {
				systemTypes.add((SystemType) obj);
			} else if (obj instanceof SystemImplementation) {
				allImpls.add((SystemImplementation) obj);
			} else if (obj instanceof BusImplementation) {
				allImpls.add((BusImplementation) obj);
			} else if (obj instanceof AbstractImplementation) {
				allImpls.add((AbstractImplementation) obj);
			} else if (obj instanceof DeviceImplementation) {
				allImpls.add((DeviceImplementation) obj);
			} else if (obj instanceof ProcessImplementation) {
				allImpls.add((ProcessImplementation) obj);
			} else if(obj instanceof PropertySetImpl) {
				for(Property prop : ((PropertySetImpl)obj).getOwnedProperties()) {					
					// Save property owner to be used later					
					for(PropertyOwner po : prop.getAppliesTos()) {
						String propCat = ((MetaclassReferenceImpl)po).getMetaclass().getName().toLowerCase();
						String propName = prop.getName();
						
						switch(propCat) {
							case "system": {
								systemPropertyToName.put(prop, propName);
								break;
							}
							case "connection": {
								connPropertyToName.put(prop, propName);
								break;
							}
							default: {
								break;
							}
						}				
					}
				}
			}
		}
		
		for(SystemType sysType : systemTypes) {
			String compTypeName = sysType.getName();
			List<Event> events = new ArrayList<>();
			List<CyberMission> missionReqs = new ArrayList<>();
			List<CyberRel> cyberRels = new ArrayList<>();
			List<SafetyRel> safetyRels = new ArrayList<>();
			List<CyberReq> cyberReqs = new ArrayList<>();
			List<SafetyReq> safetyReqs = new ArrayList<>();			
			
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
			}
			if(!events.isEmpty()) {
				compTypeNameToEvents.put(compTypeName, events);
			}
			if(!missionReqs.isEmpty()) {
				compTypeNameToMissions.put(compTypeName, missionReqs);
			}
			if(!cyberRels.isEmpty()) {
				compTypeNameToCyberRels.put(compTypeName, cyberRels);
			}
			if(!safetyRels.isEmpty()) {
				compTypeNameToSafetyRels.put(compTypeName, safetyRels);
			}
			if(!cyberReqs.isEmpty()) {
				compTypeNameToCyberReqs.put(compTypeName, cyberReqs);
			}
			if(!safetyReqs.isEmpty()) {
				compTypeNameToSafetyReqs.put(compTypeName, safetyReqs);
			}			
		}
		
		for(ComponentImplementation sysImpl : allImpls) {
			compTypeNameToImpl.put(sysImpl.getType().getName(), sysImpl);
			
			if(!sysImpl.getAllConnections().isEmpty()) {
				sysImplToConns.put(sysImpl, sysImpl.getAllConnections());
			}		
		}		
	}
	
	void addImplToConnsToMap(ComponentImplementation impl, PortConnection conn) {
		if(implToAppliesToConnsList.containsKey(impl)) {
			implToAppliesToConnsList.get(impl).add(conn);
		} else {
			List<PortConnection> connections = new ArrayList<PortConnection>();
			connections.add(conn);
			implToAppliesToConnsList.put(impl, connections);	
		}
	}
	
	/**
	 * Build the scenario bus bindings table
	 * 
	 * 
	 * */
	Table buildScnBusBindingsTable() {
		Table scnBusBindingsTable = new Table("Scenario", "Comp", "Impl", "ActualConnectionBindingSrcComp", 
				"ActualConnectionBindingSrcImpl",
				"ActualConnectionBindingSrcCompInst", "ActualConnectionBindingSrcBusInst", 
				"ActualConnectionBindingDestConnComp", "ActualConnectionBindingDestConnImpl",
				"ActualConnectionBindingDestConnCompInst", "ActualConnectionBindingDestConn");
		
		for(ComponentImplementation sysImpl : allImpls) {
			for(PropertyAssociation propAssoc : sysImpl.getOwnedPropertyAssociations()) {
				 				
				if(propAssoc.getOwnedValues().size() != 1) {
					throw new RuntimeException("Unexpected number of property owned values: " + propAssoc.getOwnedValues().size());
				}
				
				PropertyExpression expr = propAssoc.getOwnedValues().get(0).getOwnedValue();
				
				// Obtain the bus reference values
				String[] compImplInstBusNames = getStrRepofExpr(expr);
				
				// We only consider the case where the length of compImplInstBusNames is 4
				if(compImplInstBusNames.length != 4){
					throw new RuntimeException("Unexpected number of values in property expression: " + compImplInstBusNames.length);
				}				
				// property: bus connection binding applies to connections				
				for(ContainedNamedElement appliesToImpl : propAssoc.getAppliesTos()) {
					PortConnection appliesToConn = null;
					SystemSubcomponent appliesToSubcomp = null; 
					
					scnBusBindingsTable.addValue(scenario);
					scnBusBindingsTable.addValue(sysImpl.getTypeName());
					scnBusBindingsTable.addValue(sysImpl.getName());
					scnBusBindingsTable.addValue(compImplInstBusNames[0]);
					scnBusBindingsTable.addValue(compImplInstBusNames[1]);
					scnBusBindingsTable.addValue(compImplInstBusNames[2]);
					scnBusBindingsTable.addValue(compImplInstBusNames[3]);
					
					if(appliesToImpl.getContainmentPathElements().size() > 2) {
						throw new RuntimeException("Unexpected number of values in ContainedNamedElement: " + appliesToImpl.getContainmentPathElements().size());
					}
					for(ContainmentPathElement element : appliesToImpl.getContainmentPathElements()) {
						NamedElement namedElement = element.getNamedElement();
						
						if(namedElement instanceof SystemSubcomponent) {
							appliesToSubcomp = (SystemSubcomponent)namedElement;
						} else if(namedElement instanceof PortConnection) {
							appliesToConn = (PortConnection)namedElement;
						} else {
							throw new RuntimeException("Unexpected value: " + namedElement);
						}
					}
					if(appliesToSubcomp != null) {
						ComponentImplementation compImpl = appliesToSubcomp.getComponentImplementation();
						
						scnBusBindingsTable.addValue(compImpl.getTypeName());
						scnBusBindingsTable.addValue(compImpl.getName());
						scnBusBindingsTable.addValue(appliesToSubcomp.getName());
					} else {
						scnBusBindingsTable.addValue(sysImpl.getTypeName());
						scnBusBindingsTable.addValue(sysImpl.getName());
						scnBusBindingsTable.addValue("");
					}
					scnBusBindingsTable.addValue(appliesToConn.getName());
					scnBusBindingsTable.capRow();
				}
			}			
		}	
		return scnBusBindingsTable;
	}
	
    /**
     * Build the scenario architecture table.
     *
     * <p>Lists the properties associated with each connection.
     * @return
     */
    private Table buildScnConnectionsTable() {
    	List<String> headers = new ArrayList<String>(
    			Arrays.asList("Scenario", "Comp", "Impl", "ConnectionName", "SrcComp", "SrcImpl", "SrcCompInstance", "SrcCompCategory",
    					     "SrcPortName", "SrcPortType", "DestComp", "DestImpl", "DestCompInstance", "DestCompCategory",
    					     "DestPortName", "DestPortType"));
    	headers.addAll(connPropertyToName.values());
    	
        Table scnConnTable = new Table(headers);
        
		for(ComponentImplementation compImpl : allImpls) {
			if(compImpl.getOwnedConnections() != null && !compImpl.getOwnedConnections().isEmpty()) {
				for(Connection conn : compImpl.getOwnedConnections()) {
					String srcCompInstName = "";
					String destCompInstName = "";
					String srcCompName = compImpl.getTypeName();
					String destCompName = compImpl.getTypeName();
					String srcCompImplName = compImpl.getName();
					String destCompImplName = compImpl.getName();					
					String srcCompCatName = compImpl.getCategory().getName();
					String destCompCatName = compImpl.getCategory().getName();
					Context srcConnContext = conn.getAllSourceContext();
					Context destConnContext = conn.getAllDestinationContext();
					ConnectionEnd srcConnectionEnd = conn.getAllSource();
    				ConnectionEnd destConnectionEnd = conn.getAllDestination();
					
					if(srcConnContext != null) {
						srcCompInstName = srcConnContext.getName();
						
	    				if(srcConnContext instanceof ProcessSubcomponent) {
							srcCompCatName = ((ProcessSubcomponent)srcConnContext).getCategory().getName();						
							srcCompName = ((ProcessSubcomponent)srcConnContext).getComponentType().getName();
							srcCompImplName = ((ProcessSubcomponent)srcConnContext).getComponentImplementation() == null? 
													"":((ProcessSubcomponent)srcConnContext).getComponentImplementation().getName();	    					
	    				} else if(srcConnContext instanceof SystemSubcomponent) {
							srcCompCatName = ((SystemSubcomponent)srcConnContext).getCategory().getName();						
							srcCompName = ((SystemSubcomponent)srcConnContext).getComponentType().getName();
							srcCompImplName = ((SystemSubcomponent)srcConnContext).getComponentImplementation() == null? 
													"":((SystemSubcomponent)srcConnContext).getComponentImplementation().getName();
	    				} else if(srcConnContext instanceof DeviceSubcomponent) {
							srcCompCatName = ((DeviceSubcomponent)srcConnContext).getCategory().getName();						
							srcCompName = ((DeviceSubcomponent)srcConnContext).getComponentType().getName();
							srcCompImplName = ((DeviceSubcomponent)srcConnContext).getComponentImplementation() == null? 
													"":((DeviceSubcomponent)srcConnContext).getComponentImplementation().getName();	    					
	    				} else if(srcConnContext instanceof AbstractSubcomponent) {
							srcCompCatName = ((AbstractSubcomponent)srcConnContext).getCategory().getName();						
							srcCompName = ((AbstractSubcomponent)srcConnContext).getComponentType().getName();
							srcCompImplName = ((AbstractSubcomponent)srcConnContext).getComponentImplementation() == null? 
													"":((AbstractSubcomponent)srcConnContext).getComponentImplementation().getName();	   	    					
	    				} else {
	    					throw new RuntimeException("Unsupported AADL component element type: " + srcConnContext);
	    				}
					} 
					if(destConnContext != null) {
						destCompInstName = destConnContext.getName();
						
	    				if(destConnContext instanceof ProcessSubcomponent) {
							destCompCatName = ((ProcessSubcomponent)destConnContext).getCategory().getName();
							destCompName = ((ProcessSubcomponent)destConnContext).getComponentType().getName();
							destCompImplName = ((ProcessSubcomponent)destConnContext).getComponentImplementation() == null? 
													"":((ProcessSubcomponent)destConnContext).getComponentImplementation().getName();								    					
	    				} else if(destConnContext instanceof SystemSubcomponent) {
							destCompCatName = ((SystemSubcomponent)destConnContext).getCategory().getName();
							destCompName = ((SystemSubcomponent)destConnContext).getComponentType().getName();
							destCompImplName = ((SystemSubcomponent)destConnContext).getComponentImplementation() == null? 
													"":((SystemSubcomponent)destConnContext).getComponentImplementation().getName();
	    				} else if(destConnContext instanceof DeviceSubcomponent) {
							destCompCatName = ((DeviceSubcomponent)destConnContext).getCategory().getName();
							destCompName = ((DeviceSubcomponent)destConnContext).getComponentType().getName();
							destCompImplName = ((DeviceSubcomponent)destConnContext).getComponentImplementation() == null? 
													"":((DeviceSubcomponent)destConnContext).getComponentImplementation().getName();	    					
	    				} else if(destConnContext instanceof AbstractSubcomponent) {
							destCompCatName = ((AbstractSubcomponent)destConnContext).getCategory().getName();
							destCompName = ((AbstractSubcomponent)destConnContext).getComponentType().getName();
							destCompImplName = ((AbstractSubcomponent)destConnContext).getComponentImplementation() == null? 
													"":((AbstractSubcomponent)destConnContext).getComponentImplementation().getName();
	    				} else {
	    					throw new RuntimeException("Unsupported AADL component element type: " + destConnContext);
	    				}						
					} 
					
    				String srcPortTypeName = null;
    				String destPortTypeName = null;					
    				String srcPortName = srcConnectionEnd.getName();
    				String destPortName = destConnectionEnd.getName();
    				
    				if(srcConnectionEnd instanceof DataPort) {
    					srcPortTypeName = ((DataPort)srcConnectionEnd).isIn()?(((DataPort)srcConnectionEnd).isOut()? "in;out":"in"):"out";
    				} else if(srcConnectionEnd instanceof EventDataPort) {
    					srcPortTypeName = ((EventDataPort)srcConnectionEnd).isIn()?(((EventDataPort)srcConnectionEnd).isOut()? "in;out":"in"):"out";
    				} else if(srcConnectionEnd instanceof DataAccess) {
    					AccessType type = ((DataAccess) srcConnectionEnd).getKind();
    					if(type == AccessType.PROVIDES) {
    						srcPortTypeName = "provides data access";	
    					} else {
    						srcPortTypeName = "requires data access";
    					}
    				} else if(srcConnectionEnd instanceof DataSubcomponent){
    					srcPortTypeName = "data";
    				} else {
    					throw new RuntimeException("Unsupported AADL component element type: " + srcConnectionEnd);
    				}
    				
    				if(destConnectionEnd instanceof DataPort) {
    					destPortTypeName = ((DataPort)destConnectionEnd).isIn()?(((DataPort)destConnectionEnd).isOut()? "in;out":"in"):"out";
    				} else if(destConnectionEnd instanceof EventDataPort) {
    					destPortTypeName = ((EventDataPort)destConnectionEnd).isIn()?(((EventDataPort)destConnectionEnd).isOut()? "in;out":"in"):"out";    					
    				} else if(destConnectionEnd instanceof DataAccess) {
    					AccessType type = ((DataAccess) destConnectionEnd).getKind();
    					if(type == AccessType.PROVIDES) {
    						destPortTypeName = "provides data access";	
    					} else {
    						destPortTypeName = "requires data access";
    					}
    				}  else if(destConnectionEnd instanceof DataSubcomponent){
    					destPortTypeName = "data";
    				} else {
    					throw new RuntimeException("Unsupported AADL component element type: " + destConnectionEnd);
    				}    				
    				
    				scnConnTable.addValue(scenario);
    				scnConnTable.addValue(compImpl.getTypeName());
    				scnConnTable.addValue(compImpl.getName());
    				scnConnTable.addValue(conn.getName());
    				scnConnTable.addValue(srcCompName);
    				scnConnTable.addValue(srcCompImplName);
    				scnConnTable.addValue(srcCompInstName);
    				scnConnTable.addValue(srcCompCatName);
    				scnConnTable.addValue(srcPortName);
    				scnConnTable.addValue(srcPortTypeName);
    				
    				scnConnTable.addValue(destCompName);
    				scnConnTable.addValue(destCompImplName);
    				scnConnTable.addValue(destCompInstName);
    				scnConnTable.addValue(destCompCatName);
    				scnConnTable.addValue(destPortName);
    				scnConnTable.addValue(destPortTypeName);  
    				
    				for(Property prop : connPropertyToName.keySet()) {
    					String value = getStrRepofPropVal(conn.getPropertyValue(prop));
    					scnConnTable.addValue(value);
    				}
    				scnConnTable.capRow();
    				
    				// Fill in the reverse connection if the connection is bidirectional
    				if(conn.isBidirectional()) {
        				scnConnTable.addValue(scenario);
        				scnConnTable.addValue(compImpl.getTypeName());
        				scnConnTable.addValue(compImpl.getName());
        				scnConnTable.addValue(conn.getName() + "_reverse");
        				
        				scnConnTable.addValue(destCompName);
        				scnConnTable.addValue(destCompImplName);
        				scnConnTable.addValue(destCompInstName);
        				scnConnTable.addValue(destCompCatName);
        				scnConnTable.addValue(destPortName);
        				scnConnTable.addValue(destPortTypeName);   

        				scnConnTable.addValue(srcCompName);
        				scnConnTable.addValue(srcCompImplName);
        				scnConnTable.addValue(srcCompInstName);
        				scnConnTable.addValue(srcCompCatName);
        				scnConnTable.addValue(srcPortName);
        				scnConnTable.addValue(srcPortTypeName);
        				for(Property prop : connPropertyToName.keySet()) {
        					String value = getStrRepofPropVal(conn.getPropertyValue(prop));
        					scnConnTable.addValue(value);
        				}
        				scnConnTable.capRow();        				
    				}
				}
			}
		}
        
        return scnConnTable;        
    }
	
	/**
	 * 
	 * @return a scenario component properties table
	 * */
	public Table buildScnCompPropsTable() {
		// "Scenario", "Comp", "Impl", "CompInstance"
		List<String> headers = new ArrayList<String>(Arrays.asList("Scenario", "Comp", "Impl", "CompInstance"));
		headers.addAll(systemPropertyToName.values());
		
		Table scnCompPropsTable = new Table(headers);	
		
		for(ComponentImplementation sysImpl : allImpls) {
			if(sysImpl.getOwnedSubcomponents() != null && !sysImpl.getOwnedSubcomponents().isEmpty()) {
				for (Subcomponent subcomp : sysImpl.getOwnedSubcomponents()) {
					String subcompCompTypeName = subcomp.getComponentType().getName();
					String subcompTypeName = subcomp.getSubcomponentType().getName();
					
					scnCompPropsTable.addValue(scenario);
					scnCompPropsTable.addValue(subcomp.getComponentType().getName());
					scnCompPropsTable.addValue(subcompCompTypeName.equalsIgnoreCase(subcompTypeName)?"":subcompTypeName);
					scnCompPropsTable.addValue(subcomp.getName());
					
					if(subcomp.getCategory().getName().equals("system")) {
						for(Property prop : systemPropertyToName.keySet()) {
							String value = getStrRepofPropVal(subcomp.getPropertyValue(prop));
							scnCompPropsTable.addValue(value);
						}
					} else {
						for(int i = 0; i < systemPropertyToName.keySet().size(); ++i) {
							scnCompPropsTable.addValue("");
						}
					}

					scnCompPropsTable.capRow();
				}
			}
		}
		
		return scnCompPropsTable;
	}
	
    /**
     * Build the component dependency table.
     * @return a component dependency table
     */
    private Table buildCompDepTable() {
        Table table = new Table("Comp", "InputPort", "InputCIA", "OutputPort", "OutputCIA");
        for(Map.Entry<String, List<CyberRel>> entry : compTypeNameToCyberRels.entrySet()) {
        	for(CyberRel cyberRel : entry.getValue()) {
        		List<String> allPortNames = new ArrayList<>();
        		List<String> allPortCIAs = new ArrayList<>();   
        		
        		if(cyberRel.getOutput() == null || cyberRel.getOutput().getValue() == null ) {
        			throw new RuntimeException("Unexpected: the output of the cyber relation does not have a value!");
        		}
        		
                String outport = cyberRel.getOutput().getValue().getPort();
                String cia = convertAbbreviation(cyberRel.getOutput().getValue().getCia().getLiteral());
                
                if(cyberRel.getInputs() == null) {
                	table.addValue(sanitizeValue(entry.getKey()));
                	table.addValue("");
                	table.addValue("");
                	table.addValue(outport);
                	table.addValue(cia);
                	table.capRow();                	
                } else {
                    extractPortCIAFromCyberRel(cyberRel.getInputs().getValue(), allPortNames, allPortCIAs);
                    
                    if(allPortNames.size() == allPortCIAs.size()) {
                        for(int i = 0; i < allPortCIAs.size(); ++i) {
                        	table.addValue(sanitizeValue(entry.getKey()));	
                        	table.addValue(allPortNames.get(i));
                        	table.addValue(allPortCIAs.get(i));
                        	table.addValue(outport);
                        	table.addValue(cia);
                        	table.capRow();
                        }
                    } else {
                    	throw new RuntimeException("Unexpected: allPortsEventsNames's size is not the same as allPortsIAEventsHappens's size!");
                    }
                }
        	}
        }        
        return table;
    }
    /**
     * Build the mission table.
     *
     * @return the mission table
     */
    private Table buildMissionTable() {
        // 12 Headers
        Table table =
                new Table(
                        "ModelVersion",
                        "MissionReqId",
                        "MissionReq",
                        "ReqId",
                        "Req",
                        "MissionImpactCIA",
                        "Effect",
                        "Severity",
                        "CompInstanceDependency",
                        "CompOutputDependency",
                        "DependentCompOutputCIA",
                        "ReqType");
        
        for(Map.Entry<String, List<CyberMission>> entry : compTypeNameToMissions.entrySet()) {
        	for(CyberMission mission : entry.getValue()) {
        		if(mission.getCyberReqs() != null) {
        			for(String reqId : mission.getCyberReqs()) {
                		Object req = findReqWithId(entry.getKey(), reqId);
                		
                		if(req instanceof CyberReq) {
                			//Assume that the condition expression is only a disjunction of literals
                			CyberReq cyberReq = (CyberReq)req; 
                    		List<String> allPortNames = new ArrayList<>();
                    		List<String> allPortCIAs = new ArrayList<>();
                    		
                    		if(cyberReq.getCondition() == null) {
                    			throw new RuntimeException("Unexpected: the condition is cyber req is null!");
                    		}
                    		
                			extractPortCIAFromCyberRel(cyberReq.getCondition().getValue(), allPortNames, allPortCIAs);
                			
                			for(int i = 0; i < allPortNames.size(); ++i) {
                				String[] depCompPortName = findDepCompNameAndPortName(entry.getKey(), allPortNames.get(i));
                				
                        		table.addValue(scenario);
                        		table.addValue(sanitizeValue(mission.getId()));
                        		table.addValue(""); // MissionReq
                        		table.addValue(reqId);
                        		table.addValue(""); // Req
                        		
                    			table.addValue(convertAbbreviation(cyberReq.getCia().getLiteral())); // MissionImpactCIA
                    			table.addValue(""); // Effect
                    			table.addValue(cyberReq.getSeverity().getLiteral()); // Severity   
                    			if(depCompPortName[0] != null) {
                    				table.addValue(depCompPortName[0]);	
                    			} else {
                    				throw new RuntimeException("Expression in condition field of cyber requirement is unexpected!");
                    			}
                    			if(depCompPortName[1] != null) {
                    				table.addValue(depCompPortName[1]);	
                    			} else {
                    				throw new RuntimeException("Expression in condition field of cyber requirement is unexpected!");                    				
                    			}  
                    			table.addValue(allPortCIAs.get(i));
                    			table.addValue("Cyber");
                    			table.capRow();
                			}
                		} else if(req instanceof SafetyReq) {
                			SafetyReq safetyReq = (SafetyReq)req;
                    		List<String> allPortNames = new ArrayList<>();
                    		List<String> allPortIAs = new ArrayList<>();
                    		
                    		if(safetyReq.getCondition() == null) {
                    			throw new RuntimeException("Unexpected: the condition is safety req is null!");
                    		} 
                    		
                    		extractPortsAndEventsFromSafetyRel(safetyReq.getCondition().getValue(), allPortNames, allPortIAs);
                    		
                    		for(int i = 0; i < allPortNames.size(); ++i) {
                    			String[] depCompPortName = findDepCompNameAndPortName(entry.getKey(), allPortNames.get(i));
                    			
                        		table.addValue(scenario);
                        		table.addValue(sanitizeValue(mission.getId()));
                        		table.addValue(""); // MissionReq
                        		table.addValue(reqId);
                        		table.addValue(""); // Req
                    			table.addValue(""); // MissionImpactCIA
                    			table.addValue(""); // Effect
                    			
                    			table.addValue(safetyReq.getSeverity().getTargetLikelihood().toString()); // Severity
                    			if(depCompPortName[0] != null) {
                    				table.addValue(depCompPortName[0]);	
                    			} else {
                    				throw new RuntimeException("Expression in condition field of safety requirement is unexpected!");
                    			}
                    			if(depCompPortName[1] != null) {
                    				table.addValue(depCompPortName[1]);	
                    			} else {
                    				throw new RuntimeException("Expression in condition field of safety requirement is unexpected!");                    				
                    			}                   			
                    			table.addValue(allPortIAs.get(i));
                    			table.addValue("Safety");   
                    			table.capRow();
                    		}
                		} else {
                			throw new RuntimeException("Unexpected: there should be some requirements associated with the mission!");
                		}
        			}
        		}
        	}        	
        }
        return table;
    }
    
    /**
     * Find dependent component name and port name
     * @return dependent component name and port name
     * */
    public String[] findDepCompNameAndPortName(String compTypeName, String portName) {
    	String[] compAndPortNames = new String[2];
    	
    	if(compTypeNameToImpl.containsKey(compTypeName)) {
    		if(sysImplToConns.containsKey(compTypeNameToImpl.get(compTypeName))) {
    			List<Connection> conns = sysImplToConns.get(compTypeNameToImpl.get(compTypeName));
    			
    			if(portName.contains(";")) {
    				String[] portNames = portName.split(";");
    				StringBuilder sb1 = new StringBuilder();
    				StringBuilder sb2 = new StringBuilder();
    				
    				for(int i = 0; i < portNames.length; ++i) {
            			for(Connection conn : conns) {
            				if(conn.getAllDestinationContext() == null) {
                				ConnectionEnd destConnectionEnd = conn.getAllDestination(); 
                				String destPortName = destConnectionEnd.getName();
                				
                				if(destPortName.equals(portNames[i])) {
                					sb1.append(conn.getAllSourceContext().getName());
                					sb2.append(conn.getAllSource().getName());
                					if(i < portNames.length-1) {
                						sb1.append(";");
                						sb2.append(";");
                					}
                					break;
                				}
            				}
            			}      					
    				}   		
					compAndPortNames[0] = sb1.toString();
					compAndPortNames[1] = sb2.toString();
    			} else {
        			for(Connection conn : conns) {
        				if(conn.getAllDestinationContext() == null) {
            				ConnectionEnd destConnectionEnd = conn.getAllDestination(); 
            				String destPortName = destConnectionEnd.getName();
            				
            				if(destPortName.equals(portName)) {
            					compAndPortNames[0] = conn.getAllSourceContext().getName();
            					compAndPortNames[1] = conn.getAllSource().getName();
            					break;
            				}
        				}
        			}    				
    			}
    		}
    	} else {
    		throw new RuntimeException("Unexpected: no implementation (connections) is defined for component type " + compTypeName);
    	}
    	return compAndPortNames;
    }
    
    /**
     * Find the requirement with component name compName and id
     * @return requirement
     * */
    public Object findReqWithId(String compName, String id) {
    	Object req = null;
    	
		for(CyberReq cyberReq : compTypeNameToCyberReqs.get(compName)) {
			if(cyberReq.getId().equals(id)) {
				return cyberReq;
			}
		}
    	
		for(SafetyReq safetyReq : compTypeNameToSafetyReqs.get(compName)) {
			if(safetyReq.getId().equals(id)) {
				return safetyReq;
			}
		}   	
    	
    	return req;
    }
	
    /**
     * Build the events table.
     *
     * @return an events table
     */
    private Table buildEventsTable() {
        Table table = new Table("Comp", "Event", "Probability");
        for(Map.Entry<String, List<Event>> entry : compTypeNameToEvents.entrySet()) {
        	for(Event event : entry.getValue()) {
        		table.addValue(sanitizeValue(entry.getKey()));
        		table.addValue(sanitizeValue(event.getId()));
        		table.addValue(sanitizeValue(event.getProbability().getProp()));
        		table.capRow();
        	}        	
        }
        return table;
    }	
    
    /**
     * Build the component safety relations table.
     * @return a csv table
     */
    private Table buildCompSafTable() {
        Table table = new Table("Comp", "InputPortOrEvent", "InputIAOrEvent", "OutputPort", "OutputIA");
        for(Map.Entry<String, List<SafetyRel>> entry : compTypeNameToSafetyRels.entrySet()) {
        	for(SafetyRel safetyRel : entry.getValue()) {
        		List<String> allPortsEventsNames = new ArrayList<>();
        		List<String> allPortsIAEventsHappens = new ArrayList<>();                
                String outport = safetyRel.getOutput().getValue().getPort();
                String ia = convertAbbreviation(safetyRel.getOutput().getValue().getIa().getLiteral());
                
                if(safetyRel.getFaultSrc() == null) {
                	table.addValue(sanitizeValue(entry.getKey()));	
                	table.addValue("");
                	table.addValue("");
                	table.addValue(outport);
                	table.addValue(ia);
                	table.capRow();
                } else {
                    extractPortsAndEventsFromSafetyRel(safetyRel.getFaultSrc().getValue(), allPortsEventsNames, allPortsIAEventsHappens);
                    
                    if(allPortsEventsNames.size() == allPortsIAEventsHappens.size()) {
                        for(int i = 0; i < allPortsIAEventsHappens.size(); ++i) {
                        	table.addValue(sanitizeValue(entry.getKey()));	
                        	table.addValue(allPortsEventsNames.get(i));
                        	table.addValue(allPortsIAEventsHappens.get(i));
                        	table.addValue(outport);
                        	table.addValue(ia);
                        	table.capRow();
                        }
                    } else {
                    	throw new RuntimeException("Unexpected: allPortsEventsNames's size is not the same as allPortsIAEventsHappens's size!");
                    }
                }
        	}
        }
        return table;
    }	
	
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
	 * The calling function should know the size of the return array
	 * */
	String[] getStrRepofExpr(PropertyExpression expr) {
		String[] values = new String[4];
		if (expr instanceof BooleanLiteralImpl) {
			BooleanLiteralImpl bool = ((BooleanLiteralImpl) expr);			
			values[0] = bool.getValue()?"1":"0";
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
				throw new RuntimeException("Unsupported property value: " + expr);
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
				NamedElement namedElement = (NamedElement) element.getNamedElement();
				
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
		} else {
			throw new RuntimeException("Unsupported property value: " + expr);
		}
		return values;
	}
	
	public void translateSystemType(SystemType sysType) {
		
	}
	
	/**
	 * Process an event corresponding to a selection of AADL project
	 * Translate an AADL project into objects 
	 * 
	 * */
	public List<EObject> preprocessAadlFiles(File dir) {
		final Injector injector = new Aadl2StandaloneSetup().createInjectorAndDoEMFRegistration();
		final XtextResourceSet rs = injector.getInstance(XtextResourceSet.class);										
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
	
	/**
	 * Extract ports cia from cyber relations
	 * */
	void extractPortCIAFromCyberRel(LExpr expr, List<String> allPortNames, List<String> allPortCIAs) {
    	if(expr instanceof LOr) {
			for(LAnd andExpr : ((LOr)expr).getExprs()) {
		    	if(andExpr.getExprs().size() == 1){
		    		extractPortsFromAtomicExpr(andExpr.getExprs().get(0), allPortNames, allPortCIAs);
				} else if(andExpr.getExprs().size() > 1){
					extractPortsFromAndExpr(andExpr, allPortNames, allPortCIAs);
				} else {
					throw new RuntimeException("MBAA only support DNF in cyber relations.");
				}
			}  			
    	} else {
    		throw new RuntimeException("MBAA expect expressions in cyber relations to be DNF.");
    	}		
	}
    /**
     * Auxiliary functions
     * */
    private void extractPortsFromCyberRel(LOr expr, List<String> allPortNames, List<String> allPortCIAs) {
		for(LAnd andExpr : expr.getExprs()) {
	    	if(andExpr.getExprs().size() == 1){
	    		extractPortsFromAtomicExpr(andExpr.getExprs().get(0), allPortNames, allPortCIAs);
			} else if(andExpr.getExprs().size() > 1){
				extractPortsFromAndExpr(andExpr, allPortNames, allPortCIAs);
			} else {
				throw new RuntimeException("MBAA only support DNF in safety relations.");
			}
		}
	}
    
    void extractPortsFromAtomicExpr(Object lExpr, List<String> allPortNames, List<String> allPortCIAs) {
		if(lExpr instanceof LPort) {
			allPortNames.add(((LPort)lExpr).getPort());
			allPortCIAs.add(convertAbbreviation(((LPort)lExpr).getCia().getLiteral()));
		} else if(lExpr instanceof LOr) {
			extractPortsFromCyberRel((LOr)lExpr, allPortNames, allPortCIAs);
		} else {
			throw new RuntimeException("MBAA does not support parsing nested safety relation expression: " + lExpr);
		}    	
    }
    void extractPortsFromAndExpr(LAnd andExpr, List<String> allPortNames, List<String> allPortCIAs) {
		StringBuilder portEventNames = new StringBuilder();
		StringBuilder cias = new StringBuilder();
		
		for(int i = 0; i < andExpr.getExprs().size(); ++i) {
			LExpr subAndExpr = andExpr.getExprs().get(i);
			
			if(subAndExpr instanceof LPort) {
				portEventNames.append(((LPort)subAndExpr).getPort());
				cias.append(convertAbbreviation(((LPort)subAndExpr).getCia().getLiteral()));
			// We probably need to change this to support nested conjunction expressions
			} else {
				throw new RuntimeException("MBAA does not support parsing nested safety relation expression: " + subAndExpr);
			}
			if(i < andExpr.getExprs().size()-1) {
				portEventNames.append(";");
				cias.append(";");
			}
		}
		allPortNames.add(portEventNames.toString());
		allPortCIAs.add(cias.toString());
    }    
	
	
    /**
     *
     * MBAS does not support arbitrary relation expressions, it only supports DNF. 
     * This function extract conjunction of events and ports from DNF, elements 
     * in the conjunction will be populated in a list. 
     * The two functions below are set up in this way because the typing systems in 
     * VERDICT annex is not flexible. 
     * 
     * @param expr
     * @param allPortsEvents
     */
    void extractPortsAndEventsFromSafetyRel(SLExpr expr, List<String> allPortsEventsNames, List<String> allPortsIAEventsHappens) {
    	if(expr instanceof SLOr) {
			for(SLAnd andExpr : ((SLOr)expr).getExprs()) {
		    	if(andExpr.getExprs().size() == 1){
		    		extractPortsAndEventsFromAtomicExpr(andExpr.getExprs().get(0), allPortsEventsNames, allPortsIAEventsHappens);
				} else if(andExpr.getExprs().size() > 1){
					extractPortsAndEventsFromAndExpr(andExpr, allPortsEventsNames, allPortsIAEventsHappens);
				} else {
					throw new RuntimeException("MBAA only support DNF in safety relations.");
				}
			}  			
    	} else {
    		throw new RuntimeException("MBAA expect expressions in safety relations to be DNF.");
    	}
    }	
    /**
     * Auxiliary functions
     * */
    private void extractPortsAndEventsFromSafetyRel(SLOr expr, List<String> allPortsEventsNames, List<String> allPortsIAEventsHappens) {
		for(SLAnd andExpr : expr.getExprs()) {
	    	if(andExpr.getExprs().size() == 1){
	    		extractPortsAndEventsFromAtomicExpr(andExpr.getExprs().get(0), allPortsEventsNames, allPortsIAEventsHappens);
			} else if(andExpr.getExprs().size() > 1){
				extractPortsAndEventsFromAndExpr(andExpr, allPortsEventsNames, allPortsIAEventsHappens);
			} else {
				throw new RuntimeException("MBAA only support DNF in safety relations.");
			}
		}
	}
    
    void extractPortsAndEventsFromAtomicExpr(Object slExpr, List<String> allPortsEventsNames, List<String> allPortsIAEventsHappens) {
		if(slExpr instanceof FExpr) {
    		allPortsEventsNames.add(((FExpr)slExpr).getEventName());
    		allPortsIAEventsHappens.add("happens");
		} else if(slExpr instanceof SLPort) {
    		allPortsEventsNames.add(((SLPort)slExpr).getPort());
    		allPortsIAEventsHappens.add(convertAbbreviation(((SLPort)slExpr).getIa().getLiteral()));
		} else if(slExpr instanceof SLOr) {
			extractPortsAndEventsFromSafetyRel((SLOr)slExpr, allPortsEventsNames, allPortsIAEventsHappens);
		} else {
			throw new RuntimeException("MBAA does not support parsing nested safety relation expression: " + slExpr);
		}    	
    }
    void extractPortsAndEventsFromAndExpr(SLAnd andExpr, List<String> allPortsEventsNames, List<String> allPortsIAEventsHappens) {
		StringBuilder portEventNames = new StringBuilder();
		StringBuilder iasHappens = new StringBuilder();
		
		for(int i = 0; i < andExpr.getExprs().size(); ++i) {
			SLExpr subAndExpr = andExpr.getExprs().get(i);
			
			if(subAndExpr instanceof FExpr) {
				portEventNames.append(((FExpr)subAndExpr).getEventName());
				iasHappens.append("happens");		    				
			} else if(subAndExpr instanceof SLPort) {
				portEventNames.append(((SLPort)subAndExpr).getPort());
				iasHappens.append(convertAbbreviation(((SLPort)subAndExpr).getIa().getLiteral()));
			// We probably need to change this to support nested conjunction expressions
			} else {
				throw new RuntimeException("MBAA does not support parsing nested safety relation expression: " + subAndExpr);
			}
			if(i < andExpr.getExprs().size()-1) {
				portEventNames.append(";");
				iasHappens.append(";");
			}
		}
		allPortsEventsNames.add(portEventNames.toString());
		allPortsIAEventsHappens.add(iasHappens.toString());
    }	
    
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
     * To make sure the input is not null
     * */
    String sanitizeValue(String val) {
    	return val == null ? "" : val;
    }
}
