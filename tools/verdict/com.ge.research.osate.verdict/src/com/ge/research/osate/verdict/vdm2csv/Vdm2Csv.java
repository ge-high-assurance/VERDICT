package com.ge.research.osate.verdict.vdm2csv;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.osate.aadl2.ComponentImplementation;
import org.osate.aadl2.Connection;
import org.osate.aadl2.PortConnection;
import org.osate.aadl2.Property;

import com.ge.research.osate.verdict.aadl2csv.Table;
import com.ge.research.osate.verdict.dsl.verdict.CyberMission;
import com.ge.research.osate.verdict.dsl.verdict.CyberRel;
import com.ge.research.osate.verdict.dsl.verdict.CyberReq;
import com.ge.research.osate.verdict.dsl.verdict.Event;
import com.ge.research.osate.verdict.dsl.verdict.SafetyRel;
import com.ge.research.osate.verdict.dsl.verdict.SafetyReq;
import com.ge.verdict.vdm.VdmTranslator;

import verdict.vdm.vdm_model.CIA;
import verdict.vdm.vdm_model.CIAPort;
import verdict.vdm.vdm_model.ComponentImpl;
import verdict.vdm.vdm_model.ComponentType;
import verdict.vdm.vdm_model.CyberExpr;
import verdict.vdm.vdm_model.CyberExprList;
import verdict.vdm.vdm_model.EventHappens;
import verdict.vdm.vdm_model.IAPort;
import verdict.vdm.vdm_model.Mission;
import verdict.vdm.vdm_model.Model;
import verdict.vdm.vdm_model.Port;
import verdict.vdm.vdm_model.SafetyRelExpr;
/**
*
* @author Vidhya Tekken Valapil
*
*/
public class Vdm2Csv {
	List<ComponentImplementation> compImpls = new ArrayList<>();
	Map<Property, String> connPropertyToName = new LinkedHashMap<>();
	Map<Property, String> componentPropertyToName = new LinkedHashMap<>();
	Map<String, List<Event>> compTypeNameToEvents = new LinkedHashMap<>();
	Map<String, List<CyberReq>> compTypeNameToCyberReqs = new LinkedHashMap<>();
	Map<String, List<CyberRel>> compTypeNameToCyberRels = new LinkedHashMap<>();
	Map<String, ComponentImplementation> compTypeNameToImpl = new LinkedHashMap<>();
	Map<String, List<SafetyReq>> compTypeNameToSafetyReqs = new LinkedHashMap<>();
	Map<String, List<SafetyRel>> compTypeNameToSafetyRels = new LinkedHashMap<>();
	Map<String, List<CyberMission>> compTypeNameToMissions = new LinkedHashMap<>();
	Map<ComponentImplementation, List<Connection>> sysImplToConns = new LinkedHashMap<>();
	/**
	 * @param inputVDM
	 * @param stem directory 
	 * @param soteria directory
	 * @param model name i.e., project directory name 
	 * 1. Populate the data structure TODO: update
	 * 2. Build tables for STEM:
	 *        ScnCompProps.csv, ScnConnections.csv, ScnBusBindings.csv
	 * 3. Build tables for Soteria++:
	 *        CompDep.csv, Mission.csv, CompSaf.csv,
	 *        Events.csv, ScnCompProps.csv, ScnConnections.csv
	 * 4. Output the csv files
	 * */
	public void execute(File inputVdm, String stemDir, String soteriaDir, String modelName) {
		Table scnCompPropsTable = new Table();
		Table eventsTable = new Table("Comp", "Event", "Probability");
		Table compSafTable = new Table("Comp", "InputPortOrEvent", "InputIAOrEvent", "OutputPort", "OutputIA");
		Table compDepTable = new Table("Comp", "InputPort", "InputCIA", "OutputPort", "OutputCIA");
		Table missionTable =  new Table("ModelVersion", "MissionReqId", "MissionReq", "ReqId","Req", "MissionImpactCIA",
                "Effect", "Severity", "CompInstanceDependency", "CompOutputDependency", "DependentCompOutputCIA", "ReqType");
		Table scnConnTable = new Table();
		Table scnBusBindingsTable = new Table();
		Model vdm = VdmTranslator.unmarshalFromXml(inputVdm);
		updateListsMapsWithDataFromVDM(vdm, modelName, eventsTable, compDepTable,compSafTable, missionTable);
		updateTablesWithDataFromListsMaps(scnCompPropsTable, eventsTable, compSafTable, compDepTable, missionTable, scnConnTable);

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
	 * Parse the VDM and populate corresponding lists/maps
	 * @param inputVDM
	 * @param eventsTable 
	 * @param compDepTable 
	 */
	private void updateListsMapsWithDataFromVDM(Model inputVDM, String modelName, Table eventsTable, Table compDepTable, Table compSafTable, Table missionTable) {	
		List<ComponentType> compTypes = inputVDM.getComponentType();
		processComponentTypes(compTypes, eventsTable, compDepTable, compSafTable);
		List<ComponentImpl> compImpls = inputVDM.getComponentImpl();
		processMissionReqs(inputVDM.getMission(), missionTable, modelName);
		System.out.println("Updated all tables.");
	}
	private void processMissionReqs(List<Mission> missionReqs, Table missionTable, String scenario) {
		for(Mission missionReq : missionReqs) {
			missionTable.addValue(scenario);
			missionTable.addValue(sanitizeValue(missionReq.getId()));
			missionTable.addValue(""); // MissionReq
		}
	}
	private void processComponentTypes(List<ComponentType> compTypes, Table eventsTable, Table compDepTable, Table compSafTable) {
		//each ComponentType contains id, name, compCateg, List<Port>, ContractSpec, List<CyberRel>, List<SafetyRel>, List<Event>
		for (ComponentType compType : compTypes) {
			String compTypeName = compType.getName();
			System.out.println("Processing Component Type "+compTypeName);
			//populate compTypeNameToEvents,  compTypeNameToCyberRels, compTypeNameToSafetyRels
			updateEventsTable(eventsTable, compTypeName, compType.getEvent());
			updateCompDepTable(compDepTable, compTypeName, compType.getCyberRel());
			updateCompSafTable(compSafTable, compTypeName, compType.getSafetyRel());
			System.out.println("Updated Safety Relations Table");
		}
		System.out.println("Processed All Component Types.");
	}
	private void updateCompSafTable(Table compSafTable, String compTypeName,
			List<verdict.vdm.vdm_model.SafetyRel> safetyRels) {
		for(verdict.vdm.vdm_model.SafetyRel safetyRel : safetyRels) {
			updateCompSafTable(compSafTable, compTypeName, safetyRel.getFaultSrc(), safetyRel.getOutput());
		}
	}
	private void updateCompSafTable(Table compSafTable, String compTypeName,
			verdict.vdm.vdm_model.SafetyRelExpr safetyRelExpr, IAPort outputIAPort) {
		if(safetyRelExpr.getKind()==null) {
			if(safetyRelExpr.getFault()!=null) {
				compSafTable.addValue(compTypeName);
				compSafTable.addValue(safetyRelExpr.getFault().getEventName());
				compSafTable.addValue(safetyRelExpr.getFault().getHappens().toString());
				compSafTable.addValue(outputIAPort.getName());
				compSafTable.addValue(outputIAPort.getIa().name());
				compSafTable.capRow();
			} else if(safetyRelExpr.getPort()!=null){
				compSafTable.addValue(compTypeName);
				compSafTable.addValue(safetyRelExpr.getPort().getName());
				compSafTable.addValue(safetyRelExpr.getPort().getIa().name());
				compSafTable.addValue(outputIAPort.getName());
				compSafTable.addValue(outputIAPort.getIa().name());
				compSafTable.capRow();
			} else {
				System.out.println("WARNING: Safety Expression has null values for expr-kind and event-happens/fault");
			}
		} else if (safetyRelExpr.getKind().toString().equalsIgnoreCase("Or")){
			List<SafetyRelExpr> subInpSafList =safetyRelExpr.getOr().getExpr();
			for (SafetyRelExpr subInpSafExpr: subInpSafList) {
				updateCompSafTable(compSafTable, compTypeName, subInpSafExpr, outputIAPort);
			}
		} else {
			System.out.println("WARNING: Expression used as Safety Relation input is not supported.");
		}
	}
	
	private void updateCompDepTable(Table compDepTable, String compTypeName,
			List<verdict.vdm.vdm_model.CyberRel> cyberRels) {
    	for(verdict.vdm.vdm_model.CyberRel cyberRel : cyberRels) {
    		if(cyberRel.getInputs()!=null) {
	    		CyberExpr inputCyberExpr = cyberRel.getInputs();
	    		if(inputCyberExpr.getKind()==null) {//expression is not an AND, OR, NOT expression
	    			compDepTable.addValue(compTypeName);
    				CIAPort inpCIAPort = inputCyberExpr.getPort();
    				compDepTable.addValue(inpCIAPort.getName());
    				compDepTable.addValue(inpCIAPort.getCia().name());
    				compDepTable.addValue(cyberRel.getOutput().getName());
    				compDepTable.addValue(cyberRel.getOutput().getCia().name());
    				compDepTable.capRow();
	    		} else if (inputCyberExpr.getKind().toString().equalsIgnoreCase("Or")) {
	    			List<CyberExpr> subInpCyberList =inputCyberExpr.getOr().getExpr();
	    			for (CyberExpr subInpCyberExpr: subInpCyberList) {
	    				compDepTable.addValue(compTypeName);
	    				CIAPort inpCIAPort = subInpCyberExpr.getPort();
	    				compDepTable.addValue(inpCIAPort.getName());
	    				compDepTable.addValue(inpCIAPort.getCia().name());
	    				compDepTable.addValue(cyberRel.getOutput().getName());
	    				compDepTable.addValue(cyberRel.getOutput().getCia().name());
	    				compDepTable.capRow();
	    			}
	    		} else {
	    			System.out.println("WARNING: Expression used as Cyber Relation input is not supported.");
	    		}
    		} else {
    			compDepTable.addValue(compTypeName);
    			compDepTable.addValue("");
				compDepTable.addValue("");
				compDepTable.addValue(cyberRel.getOutput().getName());
				compDepTable.addValue(cyberRel.getOutput().getCia().name());
				compDepTable.capRow();
    		}
			
    	}
		
	}
	private void updateEventsTable(Table eventsTable, String compTypeName, List<verdict.vdm.vdm_model.Event> events) {
		for(verdict.vdm.vdm_model.Event event : events) {
			eventsTable.addValue(sanitizeValue(compTypeName));
			eventsTable.addValue(sanitizeValue(event.getId()));
			eventsTable.addValue(sanitizeValue(event.getProbability()));
    		eventsTable.capRow();
    	}
		
	}
	private void updateTablesWithDataFromListsMaps(Table scnCompPropsTable, Table eventsTable,
			Table compSafTable, Table compDepTable, Table missionTable, Table scnConnTable) {
		//fill in tables
	}
    /**
     * @author Paul Meng
     * To make sure the input is not null
     * */
    String sanitizeValue(String val) {
    	return val == null ? "" : val;
    }
}
