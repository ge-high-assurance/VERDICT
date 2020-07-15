package com.ge.verdict.gsn;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import java.io.*;

import java.util.ArrayList;
import java.util.List;
import verdict.vdm.vdm_model.*;
import com.ge.verdict.vdm.VdmTranslator;

public class CreateGSN { 
	
	/**
	 * THIS MAIN METHOD IS FOR TESTING PURPOSES ONLY
	 * @param args
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */
	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException{
		System.out.println("Entered CreateGSn.Main()!");
		
		
		gsnCreator();
		
	}

    /**
     * Entry method for the CreateGSN class
     *
     * @author Saswata Paul
     * @throws ParserConfigurationException 
     * @throws IOException 
     * @throws SAXException 
     */
    public static void gsnCreator() throws ParserConfigurationException, SAXException, IOException {
    	
    	//The files
		File testXml = new File("/Users/212807042/Desktop/DeliveryDroneFiles/DeliveryDroneVdm.xml");
		File cyberOutput = new File("/Users/212807042/Desktop/DeliveryDroneFiles/soteria_outputs/ImplProperties.xml");
		File safetyOutput = new File("/Users/212807042/Desktop/DeliveryDroneFiles/soteria_outputs/ImplProperties-safety.xml");
    	
    	//Fetch the DeliveryDrone model from the XML
		Model xmlModel = VdmTranslator.unmarshalFromXml(testXml);
    	
		//Get Document Builder for DOM parser
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();

		//Build CyberOutput Document
		Document cyberSoteria = builder.parse(cyberOutput);
		cyberSoteria.getDocumentElement().normalize();
		NodeList nListCyber = cyberSoteria.getElementsByTagName("Requirement");
		
		//Build SafetyOutput Document
		Document safetySoteria = builder.parse(safetyOutput);
		safetySoteria.getDocumentElement().normalize();
    	NodeList nListSafety = safetySoteria.getElementsByTagName("Requirement");

    	//get a list of all GSN fragments from model
    	List<GsnNode> gsnFragments = getFragments(xmlModel, nListCyber, nListSafety);
    	
    		
    }

    /**
     * Produces a GSN fragment for each mission requirement a GSN Node and returns a list
     * @param model
     * @param cyberResults
     * @param safetyResults
     * @return
     */
    public static List<GsnNode> getFragments(Model model, NodeList cyberResults, NodeList safetyResults) {
        // List of gsn nodes
        List<GsnNode> fragments = new ArrayList<>();

        for (Mission mission : model.getMission()) {
            fragments.add(populateMissionNode(mission, model, cyberResults, safetyResults));
        }

        return fragments;
    }

    /**
     *  Creates a GsnNode for the given mission 
     * @param mission
     * @param model
     * @param cyberResults
     * @param safetyResults
     * @return
     */
    public static GsnNode populateMissionNode(Mission mission, Model model, NodeList cyberResults, NodeList safetyResults) {
        
        //GsnNode to pack mission rootnode
        GsnNode missionNode = new GsnNode(); 

        //Populate the rootnode with the mission goal
        missionNode.setNodeType("goal");
        missionNode.setNodeId(mission.getId());
        //to set goal of missionNode
        Goal missionGoal = new Goal();
        missionGoal.setDisplayText(mission.getDescription());
        missionNode.setGoal(missionGoal);
        
//INCOMPLETE: add contexts to missionNode
           
        //create a strategy node to support the mission goal
        GsnNode strategyNode = new GsnNode();
        strategyNode.setNodeType("strategy");
        strategyNode.setNodeId("Strategy::"+mission.getId());
        
        String strategyText = "By conjunction of subgoals: ";
        
    	//add requirements to supportedBy of srategyNode
        for (String subReqId : mission.getCyberReqs()) {
        	strategyText= strategyText+ subReqId + " ";
        	
            //check if reqId is Cyber or Safety req
            for (CyberReq cyberReq : model.getCyberReq()) {
            	if (cyberReq.getId().equals(subReqId)) {
            		strategyNode.getSupportedBy().add(populateCyberRequirementNode(cyberReq, model, cyberResults));	
            	} else continue;
            }
            for (SafetyReq safetyReq : model.getSafetyReq()) {
            	if (safetyReq.getId().equals(subReqId)) {
            		strategyNode.getSupportedBy().add(populateSafetyRequirementNode(safetyReq, model, safetyResults));	
            	} else continue;
            }
        }
        
        //to pack strategy for strategyNode
        Strategy strat = new Strategy();
        
        //add strategy status
        strat.setStatus(true);
        for (GsnNode subNode : strategyNode.getSupportedBy()) {
        	if (!subNode.getGoal().getStatus()) {
        		strat.setStatus(false);
        	}
        }
        
        //add strategy text to display
        strat.setDisplayText(strategyText);
        
        //add strategy to strategyNode
        strategyNode.setStrategy(strat);
        
        
//INCOMPLETE: add contexts to strategyNode
        
        
        //add strategyNode to supportedBy of missionNode
        missionNode.getSupportedBy().add(strategyNode);
        
        return missionNode;
    }
 
    
    
    /**
     *  Creates a GSN node for the given cyber subrequirement
     * @param cyberReq
     * @param model
     * @param cyberResults
     * @return
     */
    public static GsnNode populateCyberRequirementNode(CyberReq cyberReq, Model model, NodeList cyberResults) {
        //GsnNode to pack requirement rootnode
        GsnNode reqNode = new GsnNode();
               
        //Populate the rootnode with the requirement subgoal
        reqNode.setNodeType("goal");
        reqNode.setNodeId(cyberReq.getId());
        //to populate goal of node
        Goal reqNodeGoal = new Goal();
        reqNodeGoal.setDisplayText(cyberReq.getDescription());     
        reqNode.setGoal(reqNodeGoal);
        

//INCOMPLETE: add contexts to reqNode --> WILL NEED TO DEVELOP CyberExpr and SafetyExpr parsers  
        
        //create a strategy node to support the requirement subgoal
        GsnNode strategyNode = new GsnNode();
        strategyNode.setNodeType("strategy");
        strategyNode.setNodeId("Strategy::"+cyberReq.getId());
        //to populate srategy of strategyNode
        Strategy strat = new Strategy();
        strat.setDisplayText("By Soteria++ analysis of attack-defense trees");
        
        //add a solution to the supportedBy of strategy
        GsnNode solutionNode = populateSolutionNode(cyberReq.getId(), cyberResults);
        strategyNode.getSupportedBy().add(solutionNode);
        
        //setting strategy status
        strat.setStatus(solutionNode.getSolution().getStatus());
        
        //adding strategy to strategyNode
        strategyNode.setStrategy(strat);
        
        //add strategyNode to supportedBy of reqNode
        reqNode.getSupportedBy().add(strategyNode);
        
        return reqNode;    	
    }

    
    
    /**
     *  Creates a GSN node for the given safety subrequirement
     * @param safetyReq
     * @param model
     * @param safetyResults
     * @return
     */
    public static GsnNode populateSafetyRequirementNode(SafetyReq safetyReq, Model model, NodeList safetyResults) {
        //GsnNode to pack requirement rootnode
        GsnNode reqNode = new GsnNode();
               
        //Populate the rootnode with the requirement subgoal
        reqNode.setNodeType("goal");
        reqNode.setNodeId(safetyReq.getId());
        //to populate goal of node
        Goal reqNodeGoal = new Goal();
        reqNodeGoal.setDisplayText(safetyReq.getDescription());     
        reqNode.setGoal(reqNodeGoal);
        

//INCOMPLETE: add contexts to reqNode --> WILL NEED TO DEVELOP CyberExpr and SafetyExpr parsers  
        
        //create a strategy node to support the requirement subgoal
        GsnNode strategyNode = new GsnNode();
        strategyNode.setNodeType("strategy");
        strategyNode.setNodeId("Strategy::"+safetyReq.getId());
        //to populate srategy of strategyNode
        Strategy strat = new Strategy();
        strat.setDisplayText("By Soteria++ analysis of attack-defense trees");
        
        //add a solution to the supportedBy of strategy
        GsnNode solutionNode = populateSolutionNode(safetyReq.getId(), safetyResults);
        strategyNode.getSupportedBy().add(solutionNode);
        
        //setting strategy status
        strat.setStatus(solutionNode.getSolution().getStatus());
        
        //adding strategy to strategyNode
        strategyNode.setStrategy(strat);
        
        //add strategyNode to supportedBy of reqNode
        reqNode.getSupportedBy().add(strategyNode);
        
        return reqNode;     	
    }    
  
    
    
    /**
     *  Creates a GSN node for a solution
     * @param safetyReq
     * @param model
     * @param safetyResults
     * @return
     */
	public static GsnNode populateSolutionNode(String reqId, NodeList results) {
	    //GsnNode to pack solution
	    GsnNode solutionNode = new GsnNode();
	    
        //Populate the rootnode with solution details
        solutionNode.setNodeType("solution");
        solutionNode.setNodeId("CMinCut::"+reqId);
        //to set solution of solutionNode
        Solution sol = new Solution();
        sol.setDisplayText("Soteria++ minimal cutset for "+reqId); 
	    
	    //Extract the probabilities from the soteria output xml
	    String computed_p = "";
	    String acceptable_p = "";
	    
	    for (int temp = 0; temp < results.getLength(); temp++)
		{
			 Node node = results.item(temp);
			 if (node.getNodeType() == Node.ELEMENT_NODE)
			 {
				Element eElement = (Element) node;
				if (eElement.getAttribute("label").equals(reqId)) {
					computed_p = eElement.getAttribute("computed_p");
					acceptable_p = eElement.getAttribute("acceptable_p");
				}		    	
			 }		
		} 

	    System.out.println(reqId+ " --------------------> Found probabilities");
    	//Check below if solution supports the requirement
  		if(Double.parseDouble(computed_p)<= Double.parseDouble(acceptable_p)) {
  			sol.setStatus(true);
  		} else {
  			sol.setStatus(false);
  		}
	  		
  		//add sol to solutionNode
  		solutionNode.setSolution(sol);
	    
	    return solutionNode;    	
	}
    
}



