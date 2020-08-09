package com.ge.verdict.gsn;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import verdict.vdm.vdm_model.*;

/**
 * This class has been designed for handling security assurance cases along with general assurance
 * cases It is still under development
 *
 * @author Saswata Paul
 */
public class CreateSecurityGSN {
    protected final String SEP = File.separator;
    // For naming the nodes uniformly
    private int strategyCounter = 1;
    private int contextCounter = 1;
    private int solutionCounter = 1;
    private int justificationCounter = 1;
    private int assumptionCounter = 1;
    private int securityGoalCounter = 1;
    private String soteriaCyberOutputAddr;
    private String soteriaSafetyOutputAddr;

    /**
     * creates a GsnNode and returns it
     *
     * @param xmlModel -- a VDM model
     * @param cyberOutput -- file object with soteria++ output for cyber properties
     * @param safetyOutput -- file object with soteria++ output for safety properties
     * @param addressForCASE -- string address to the CASE consolidated properties
     * @param rootGoalId -- the fragment's root requirement ID
     * @return a GsnNode
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public GsnNode gsnCreator(
            Model xmlModel,
            File cyberOutput,
            File safetyOutput,
            String addressForCASE,
            String rootGoalId,
            boolean securityCaseFlag)
            throws ParserConfigurationException, SAXException, IOException {

        // setting class variables
        soteriaCyberOutputAddr = cyberOutput.getAbsolutePath();
        soteriaSafetyOutputAddr = safetyOutput.getAbsolutePath();

        // The GsnNode to return
        GsnNode returnFragment = new GsnNode();

        // Get Document Builder for DOM parser
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // Build CyberOutput Document
        Document cyberSoteria = builder.parse(cyberOutput);
        cyberSoteria.getDocumentElement().normalize();
        NodeList nListCyber = cyberSoteria.getElementsByTagName("Requirement");

        // Build SafetyOutput Document
        Document safetySoteria = builder.parse(safetyOutput);
        safetySoteria.getDocumentElement().normalize();
        NodeList nListSafety = safetySoteria.getElementsByTagName("Requirement");

        // determine if rootGoalId is a mission, cyber, or safety requirement
        for (Mission aMission : xmlModel.getMission()) {
            if (aMission.getId().equalsIgnoreCase(rootGoalId)) {
                returnFragment =
                        populateMissionNode(
                                aMission,
                                xmlModel,
                                nListCyber,
                                nListSafety,
                                addressForCASE,
                                securityCaseFlag);
            }
        }
        for (CyberReq aCyberReq : xmlModel.getCyberReq()) {
            if (aCyberReq.getId().equalsIgnoreCase(rootGoalId)) {
                if (securityCaseFlag) {
                    returnFragment =
                            populateCyberSecurityRequirementNode(
                                    aCyberReq, xmlModel, nListCyber, addressForCASE);
                } else {
                    returnFragment =
                            populateCyberRequirementNode(
                                    aCyberReq, xmlModel, nListCyber, addressForCASE);
                }
            }
        }
        for (SafetyReq aSafetyReq : xmlModel.getSafetyReq()) {
            if (aSafetyReq.getId().equalsIgnoreCase(rootGoalId)) {
                returnFragment =
                        populateSafetyRequirementNode(
                                aSafetyReq, xmlModel, nListSafety, addressForCASE);
            }
        }

        // return the GSN gragment
        return returnFragment;
    }

    /**
     * Creates a GsnNode for a given mission
     *
     * @param mission
     * @param model
     * @param cyberResults
     * @param safetyResults
     * @return
     * @throws IOException
     */
    public GsnNode populateMissionNode(
            Mission mission,
            Model model,
            NodeList cyberResults,
            NodeList safetyResults,
            String addressForCASE,
            boolean securityCaseFlag)
            throws IOException {

        // GsnNode to pack mission rootnode
        GsnNode missionNode = new GsnNode();

        // Populate the rootnode with the mission goal
        missionNode.setNodeType("goal");
        missionNode.setNodeId("GOAL_" + mission.getId());

        // to set goal of missionNode
        Goal missionGoal = new Goal();
        missionGoal.setDisplayText(mission.getDescription());
        missionNode.setGoal(missionGoal);

        // add contexts to missionNode
        List<String> missionContext = new ArrayList<>();
        missionContext.add(model.getName());
        missionNode.getInContextOf().addAll(addGoalContexts(missionContext, model));

        // create a strategy node to support the mission goal
        GsnNode strategyNode = new GsnNode();
        strategyNode.setNodeType("strategy");

        String strategyId = "STRATEGY_" + Integer.toString(strategyCounter);
        strategyCounter++;
        strategyNode.setNodeId(strategyId);

        String strategyText = "Argument: By validity of subgoals:&#10;";

        // add requirements to supportedBy of StrategyNode
        for (String subReqId : mission.getCyberReqs()) {
            strategyText = strategyText + subReqId + " ";

            // check if reqId is Cyber or Safety req
            for (CyberReq cyberReq : model.getCyberReq()) {
                if (cyberReq.getId().equals(subReqId)) {
                    if (securityCaseFlag) {
                        strategyNode
                                .getSupportedBy()
                                .add(
                                        populateCyberSecurityRequirementNode(
                                                cyberReq, model, cyberResults, addressForCASE));
                    } else {
                        strategyNode
                                .getSupportedBy()
                                .add(
                                        populateCyberRequirementNode(
                                                cyberReq, model, cyberResults, addressForCASE));
                    }
                } else continue;
            }
            for (SafetyReq safetyReq : model.getSafetyReq()) {
                if (safetyReq.getId().equals(subReqId)) {
                    strategyNode
                            .getSupportedBy()
                            .add(
                                    populateSafetyRequirementNode(
                                            safetyReq, model, safetyResults, addressForCASE));
                } else continue;
            }
        }

        // to pack strategy for strategyNode
        Strategy strat = new Strategy();

        // add strategy status
        strat.setStatus(true);
        for (GsnNode subNode : strategyNode.getSupportedBy()) {
            if (!subNode.getGoal().getStatus()) {
                strat.setStatus(false);
            }
        }

        // add strategy text to display
        if (mission.getStrategy() != null) {
            // if user has specified a strategy
            strat.setDisplayText(mission.getStrategy());
        } else {
            strat.setDisplayText(strategyText);
        }

        // add strategy to strategyNode
        strategyNode.setStrategy(strat);

        // A context to add to strategyNode
        Context strategyContext = new Context();
        // A node to pack strategyContext
        GsnNode strategyContextNode = new GsnNode();
        strategyContextNode.setNodeType("context");
        String strategyContextId = "CONTEXT_" + Integer.toString(contextCounter);
        contextCounter++;
        strategyContextNode.setNodeId(strategyContextId);
        String strategyContextDisplayText = mission.getId() + " requires:&#10;";
        for (String requirement : mission.getCyberReqs()) {
            strategyContextDisplayText = strategyContextDisplayText + " " + requirement;
        }
        strategyContext.setDisplayText(strategyContextDisplayText);
        strategyContextNode.setContext(strategyContext);

        // add the context node to strategyNode
        strategyNode.getInContextOf().add(strategyContextNode);

        // Add a justification to justifiedBy of strategyNode
        if (mission.getJustification() != null) {
            strategyNode.getJustifiedBy().add(getJustificationNode(mission.getJustification()));
        }

        // add strategyNode to supportedBy of missionNode
        missionNode.getSupportedBy().add(strategyNode);

        // add any available assumption to missionNode
        if (mission.getAssumption() != null) {
            missionNode.getHasAssumptions().add(getAssumptionNode(mission.getAssumption()));
        }

        // setting reqNode status
        missionNode.getGoal().setStatus(strategyNode.getStrategy().getStatus());

        return missionNode;
    }

    /**
     * Creates a GSN node for the given cyber subrequirement DOES NOT GENERATE THREAT LEVEL SECURITY
     * CASES USE populateCyberSecurityRequirementNode() for that
     *
     * @param cyberReq
     * @param model
     * @param cyberResults
     * @return
     */
    public GsnNode populateCyberRequirementNode(
            CyberReq cyberReq, Model model, NodeList cyberResults, String addressForCASE) {
        // GsnNode to pack requirement rootnode
        GsnNode reqNode = new GsnNode();

        // Populate the rootnode with the requirement subgoal
        reqNode.setNodeType("goal");

        reqNode.setNodeId("GOAL_" + cyberReq.getId());
        // to populate goal of node
        Goal reqNodeGoal = new Goal();
        reqNodeGoal.setDisplayText(cyberReq.getDescription());
        reqNode.setGoal(reqNodeGoal);

        // Add context to reqNode
        List<String> reqContextPorts = getCyberExprPorts(cyberReq.getCondition());
        reqNode.getInContextOf().addAll(addGoalContexts(reqContextPorts, model));

        // create a strategy node to support the requirement subgoal
        GsnNode strategyNode = new GsnNode();

        strategyNode.setNodeType("strategy");
        String strategyId = "STRATEGY_" + Integer.toString(strategyCounter);
        strategyCounter++;
        strategyNode.setNodeId(strategyId);

        String strategyText;

        if (cyberReq.getStrategy() != null) {
            strategyText = cyberReq.getStrategy();
        } else {
            strategyText = "Argument: By Soteria++ analysis &#10;of attack-defense trees";
        }

        // to populate Strategy of strategyNode
        Strategy strat = new Strategy();
        strat.setDisplayText(strategyText);

        // add a solution to the supportedBy of strategy
        GsnNode solutionNode =
                populateRequirementSolutionNode(cyberReq.getId(), cyberResults, true);
        strategyNode.getSupportedBy().add(solutionNode);

        // setting strategy status
        strat.setStatus(solutionNode.getSolution().getStatus());

        // adding strategy to strategyNode
        strategyNode.setStrategy(strat);

        // Add the requirement conditions as a context to strategyNode
        Context strategyContext = new Context();
        // A node to pack strategyContext
        GsnNode strategyContextNode = new GsnNode();
        strategyContextNode.setNodeType("context");
        String strategyContextId = "CONTEXT_" + Integer.toString(contextCounter);
        contextCounter++;
        strategyContextNode.setNodeId(strategyContextId);
        String strategyContextDisplayText = "A Condition and a Severity";
        String strategyContextHoverText =
                "Condition:= " + getCyberExprCondition(cyberReq.getCondition()) + "&#10;";
        // strategyContextDisplayText =strategyContextDisplayText+"CIA:= " +
        // cyberReq.getCia().toString().charAt(0);
        strategyContextHoverText =
                strategyContextHoverText + "Severity:= " + cyberReq.getSeverity();
        strategyContext.setDisplayText(strategyContextDisplayText);
        strategyContext.setExtraInfo(strategyContextHoverText);
        strategyContextNode.setContext(strategyContext);

        // add the context node to strategyNode
        strategyNode.getInContextOf().add(strategyContextNode);

        // Add the CASE consolidated properties as a context to strategyNode
        GsnNode strategyContextNode2 = getCASEContext(addressForCASE);

        // add the context node to strategyNode
        strategyNode.getInContextOf().add(strategyContextNode2);

        // Add a justification to justifiedBy of strategyNode
        if (cyberReq.getJustification() != null) {
            strategyNode.getJustifiedBy().add(getJustificationNode(cyberReq.getJustification()));
        }

        // add strategyNode to supportedBy of reqNode
        reqNode.getSupportedBy().add(strategyNode);

        // add any available assumption to reqNode
        if (cyberReq.getAssumption() != null) {
            reqNode.getHasAssumptions().add(getAssumptionNode(cyberReq.getAssumption()));
        }

        // setting reqNode status
        reqNode.getGoal().setStatus(strategyNode.getStrategy().getStatus());

        return reqNode;
    }

    /**
     * Creates a GSN node for the given safety subrequirement
     *
     * @param safetyReq
     * @param model
     * @param safetyResults
     * @return
     */
    public GsnNode populateSafetyRequirementNode(
            SafetyReq safetyReq, Model model, NodeList safetyResults, String addressForCASE) {
        // GsnNode to pack requirement rootnode
        GsnNode reqNode = new GsnNode();

        // Populate the rootnode with the requirement subgoal
        reqNode.setNodeType("goal");
        reqNode.setNodeId("GOAL_" + safetyReq.getId());
        // to populate goal of node
        Goal reqNodeGoal = new Goal();
        reqNodeGoal.setDisplayText(safetyReq.getDescription());
        reqNode.setGoal(reqNodeGoal);

        // Add context to reqNode
        List<String> reqContextPorts = getSafetyReqExprPorts(safetyReq.getCondition());
        reqNode.getInContextOf().addAll(addGoalContexts(reqContextPorts, model));

        // create a strategy node to support the requirement subgoal
        GsnNode strategyNode = new GsnNode();

        strategyNode.setNodeType("strategy");
        String strategyId = "STRATEGY_" + Integer.toString(strategyCounter);
        strategyCounter++;
        strategyNode.setNodeId(strategyId);

        String strategyText;

        if (safetyReq.getStrategy() != null) {
            strategyText = safetyReq.getStrategy();
        } else {
            strategyText = "Argument: By Soteria++ analysis &#10;of fault trees";
        }

        // to populate Strategy of strategyNode
        Strategy strat = new Strategy();
        strat.setDisplayText(strategyText);

        // add a solution to the supportedBy of strategy
        GsnNode solutionNode =
                populateRequirementSolutionNode(safetyReq.getId(), safetyResults, false);
        strategyNode.getSupportedBy().add(solutionNode);

        // setting strategy status
        strat.setStatus(solutionNode.getSolution().getStatus());

        // adding strategy to strategyNode
        strategyNode.setStrategy(strat);

        // Add the requirement conditions as a context to strategyNode
        Context strategyContext = new Context();
        // A node to pack strategyContext
        GsnNode strategyContextNode = new GsnNode();
        strategyContextNode.setNodeType("context");
        String strategyContextId = "CONTEXT_" + Integer.toString(contextCounter);
        contextCounter++;
        strategyContextNode.setNodeId(strategyContextId);
        String strategyContextDisplayText = "A Condition and a Target Probability";
        String strategyContextHoverText =
                "Condition:= " + getSafetyReqExprCondition(safetyReq.getCondition()) + "&#10;";
        strategyContextHoverText =
                strategyContextHoverText
                        + "Target Probability:= "
                        + safetyReq.getTargetProbability();
        strategyContext.setDisplayText(strategyContextDisplayText);
        strategyContext.setExtraInfo(strategyContextHoverText);
        strategyContextNode.setContext(strategyContext);

        // add the context node to strategyNode
        strategyNode.getInContextOf().add(strategyContextNode);

        // Add the CASE consolidated properties as a context to strategyNode
        GsnNode strategyContextNode2 = getCASEContext(addressForCASE);

        // add the context node to strategyNode
        strategyNode.getInContextOf().add(strategyContextNode2);

        // Add a justification to justifiedBy of strategyNode
        if (safetyReq.getJustification() != null) {
            strategyNode.getJustifiedBy().add(getJustificationNode(safetyReq.getJustification()));
        }

        // add strategyNode to supportedBy of reqNode
        reqNode.getSupportedBy().add(strategyNode);

        // add any available assumption to reqNode
        if (safetyReq.getAssumption() != null) {
            reqNode.getHasAssumptions().add(getAssumptionNode(safetyReq.getAssumption()));
        }

        // setting reqNode status
        reqNode.getGoal().setStatus(strategyNode.getStrategy().getStatus());

        return reqNode;
    }

    /**
     * Creates a GSN node for cyber/safety requirement level solutions
     *
     * @param safetyReq
     * @param model
     * @param safetyResults
     * @return
     */
    public GsnNode populateRequirementSolutionNode(
            String reqId, NodeList results, boolean cyberFlag) {
        // GsnNode to pack solution
        GsnNode solutionNode = new GsnNode();

        // Populate the rootnode with solution details

        solutionNode.setNodeType("solution");
        String solutionId = "SOLUTION_" + Integer.toString(solutionCounter);
        solutionCounter++;
        solutionNode.setNodeId(solutionId);
        // to set solution of solutionNode
        Solution sol = new Solution();
        sol.setDisplayText("Soteria++ &#10;minimal cutset &#10;for " + reqId);

        // Extract the probabilities from the soteria output xml
        String computed_p = "";
        String acceptable_p = "";

        for (int temp = 0; temp < results.getLength(); temp++) {
            Node node = results.item(temp);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) node;
                if (eElement.getAttribute("label").equals(reqId)) {
                    computed_p = eElement.getAttribute("computed_p");
                    acceptable_p = eElement.getAttribute("acceptable_p");
                }
            }
        }

        // Check below if solution supports the requirement
        if (Double.parseDouble(computed_p) <= Double.parseDouble(acceptable_p)) {
            sol.setStatus(true);
        } else {
            sol.setStatus(false);
        }

        // setting hovering info for solution node
        if (cyberFlag) {
            String extraInfo =
                    "Computed Likelihood = "
                            + computed_p
                            + "&#10;Acceptable Likelihood = "
                            + acceptable_p;
            sol.setExtraInfo(extraInfo);
            sol.setUrl(soteriaCyberOutputAddr);
        } else {
            String extraInfo =
                    "Computed Probability = "
                            + computed_p
                            + "&#10;Acceptable Probability = "
                            + acceptable_p;
            sol.setExtraInfo(extraInfo);
            sol.setUrl(soteriaSafetyOutputAddr);
        }

        // add sol to solutionNode
        solutionNode.setSolution(sol);

        return solutionNode;
    }

    /**
     * Creates a Gsn node for a given cyber requirement for security cases
     *
     * @param cyberReq
     * @param model
     * @param cyberResults
     * @param addressForCASE
     * @return
     * @throws IOException
     */
    public GsnNode populateCyberSecurityRequirementNode(
            CyberReq cyberReq, Model model, NodeList cyberResults, String addressForCASE)
            throws IOException {
        // GsnNode to pack requirement rootnode
        GsnNode reqNode = new GsnNode();

        // Populate the rootnode with the requirement subgoal
        reqNode.setNodeType("goal");

        reqNode.setNodeId("GOAL_" + cyberReq.getId());
        // to populate goal of node
        Goal reqNodeGoal = new Goal();
        reqNodeGoal.setDisplayText(cyberReq.getDescription());
        reqNode.setGoal(reqNodeGoal);

        // Add context to reqNode
        List<String> reqContextPorts = getCyberExprPorts(cyberReq.getCondition());
        reqNode.getInContextOf().addAll(addGoalContexts(reqContextPorts, model));

        // create a strategy node to support the requirement subgoal
        GsnNode strategyNode = new GsnNode();

        strategyNode.setNodeType("strategy");
        String strategyId = "STRATEGY_" + Integer.toString(strategyCounter);
        strategyCounter++;
        strategyNode.setNodeId(strategyId);

        String strategyText;

        if (cyberReq.getStrategy() != null) {
            strategyText = cyberReq.getStrategy();
        } else {
            strategyText = "Argument: All subcomponents are secure";
        }

        // to populate Strategy of strategyNode
        Strategy strat = new Strategy();
        strat.setDisplayText(strategyText);

        // Populate strategyNode supportedBy
        List<Cutset> reqCutsets = getComponentThreatInfo(cyberResults, cyberReq.getId());

        List<String> components = getVulnerableComponentsFromCutsets(reqCutsets);

        // add componentsts to supportedBy of StrategyNode
        String acceptableProb = getAcceptableProb(cyberResults, cyberReq.getId());
        for (String componentId : components) {
            strategyNode
                    .getSupportedBy()
                    .add(
                            populateSubComponentSolutionNode(
                                    componentId, reqCutsets, acceptableProb, cyberReq.getId()));
        }

        // add strategy status
        strat.setStatus(true);
        for (GsnNode subNode : strategyNode.getSupportedBy()) {
            if (!subNode.getSolution().getStatus()) {
                strat.setStatus(false);
            }
        }

        // adding strategy to strategyNode
        strategyNode.setStrategy(strat);

        // Add a justification to justifiedBy of strategyNode
        if (cyberReq.getJustification() != null) {
            strategyNode.getJustifiedBy().add(getJustificationNode(cyberReq.getJustification()));
        }

        // add strategyNode to supportedBy of reqNode
        reqNode.getSupportedBy().add(strategyNode);

        // add any available assumption to reqNode
        if (cyberReq.getAssumption() != null) {
            reqNode.getHasAssumptions().add(getAssumptionNode(cyberReq.getAssumption()));
        }

        // setting reqNode status
        reqNode.getGoal().setStatus(strategyNode.getStrategy().getStatus());

        return reqNode;
    }

    /**
     * Populates subcomponent solution nodes for the highest level security GSN and also creates the
     * lower threat-level fragments
     *
     * @param subCompId
     * @param cutsets
     * @param acceptableProb
     * @param cyberReqId
     * @return
     * @throws IOException
     */
    public GsnNode populateSubComponentSolutionNode(
            String subCompId, List<Cutset> cutsets, String acceptableProb, String cyberReqId)
            throws IOException {
        // GsnNode to pack solution
        GsnNode subCompSolNode = new GsnNode();

        // Populate the rootnode with solution details

        subCompSolNode.setNodeType("solution");
        String solutionId = "SOLUTION_" + Integer.toString(solutionCounter);
        solutionCounter++;
        subCompSolNode.setNodeId(solutionId);
        // to set solution of solutionNode
        Solution sol = new Solution();
        sol.setDisplayText("Evidence that&#10;" + subCompId + "&#10;is secure");

        /** Create a GSN fragment that starts at this subcomponent and create artifacts for it */
        GsnNode subCompFragment = populateSubComponentNode(subCompId, cutsets, acceptableProb);
        SecurityGSNInterface interfaceObj = new SecurityGSNInterface();
        String svgDestination =
                interfaceObj.createArtifactFiles(subCompFragment, cyberReqId + "_" + subCompId);

        sol.setUrl(svgDestination);

        // get the status for sol from subCompFragment
        sol.setStatus(subCompFragment.getGoal().getStatus());

        // add sol to solutionNode
        subCompSolNode.setSolution(sol);

        // return node
        return subCompSolNode;
    }

    /**
     * Populates the root nodes of the threat-level security GSN fragments
     *
     * @param subCompId
     * @param cutsets
     * @param acceptableProb
     * @return
     */
    public GsnNode populateSubComponentNode(
            String subCompId, List<Cutset> cutsets, String acceptableProb) {
        // GsnNode to pack solution
        GsnNode subCompNode = new GsnNode();

        // Populate the rootnode with the requirement subgoal
        subCompNode.setNodeType("goal");
        String securityGoalId = "SG_" + Integer.toString(securityGoalCounter);
        securityGoalCounter++;
        subCompNode.setNodeId("GOAL_" + securityGoalId);
        // to populate goal of node
        Goal reqNodeGoal = new Goal();
        reqNodeGoal.setDisplayText(subCompId + " is secure");
        subCompNode.setGoal(reqNodeGoal);

        // create a strategy node to support the requirement subgoal
        GsnNode strategyNode = new GsnNode();

        strategyNode.setNodeType("strategy");
        String strategyId = "STRATEGY_" + Integer.toString(strategyCounter);
        strategyCounter++;
        strategyNode.setNodeId(strategyId);

        String strategyText = "Argument: All threats mitigated";

        // to populate Strategy of strategyNode
        Strategy strat = new Strategy();
        strat.setDisplayText(strategyText);

        // Populate strategyNode supportedBy
        for (Cutset cutset : cutsets) {
            if (cutset.getComponent().equalsIgnoreCase(subCompId)) {
                strategyNode
                        .getSupportedBy()
                        .add(populateThreatNode(subCompId, cutset, acceptableProb));
                // subCompNode.getSupportedBy().add(populateThreatNode(subCompId, cutset,
                // acceptableProb));
            }
        }

        // add strategy status
        strat.setStatus(true);
        for (GsnNode subNode : strategyNode.getSupportedBy()) {
            if (!subNode.getGoal().getStatus()) {
                strat.setStatus(false);
            }
        }

        // adding strategy to strategyNode
        strategyNode.setStrategy(strat);

        // add strategyNode to supportedBy of reqNode
        subCompNode.getSupportedBy().add(strategyNode);

        // setting reqNode status
        subCompNode.getGoal().setStatus(strategyNode.getStrategy().getStatus());

        return subCompNode;
    }

    /**
     * populates the threat-nodes of the threat-level GSN fragments
     *
     * @param componentId
     * @param cutset
     * @param acceptableProb
     * @return
     */
    public GsnNode populateThreatNode(String componentId, Cutset cutset, String acceptableProb) {
        // GsnNode to pack solution
        GsnNode threatNode = new GsnNode();

        // Populate the rootnode with the requirement subgoal
        threatNode.setNodeType("goal");
        String securityGoalId = "SG_" + Integer.toString(securityGoalCounter);
        securityGoalCounter++;
        threatNode.setNodeId("GOAL_" + securityGoalId);
        // to populate goal of node
        Goal reqNodeGoal = new Goal();
        reqNodeGoal.setDisplayText(cutset.getAttack() + " has been mitigated");
        threatNode.setGoal(reqNodeGoal);

        // add a solution to the supportedBy of strategy
        GsnNode solutionNode = populateThreatSolutionNode(cutset, acceptableProb);

        // setting strategy status

        // adding strategy to strategyNode

        // add strategyNode to supportedBy of reqNode
        threatNode.getSupportedBy().add(solutionNode);

        // setting reqNode status
        threatNode.getGoal().setStatus(solutionNode.getSolution().getStatus());
        return threatNode;
    }

    /**
     * populates the solutions nodes for threat-level gsn fragments
     *
     * @param CyberReqId
     * @param results
     * @return
     */
    public GsnNode populateThreatSolutionNode(Cutset cutset, String acceptableProb) {
        // GsnNode to pack solution
        GsnNode solutionNode = new GsnNode();

        // Populate the rootnode with solution details

        solutionNode.setNodeType("solution");
        String solutionId = "SOLUTION_" + Integer.toString(solutionCounter);
        solutionCounter++;
        solutionNode.setNodeId(solutionId);
        // to set solution of solutionNode
        Solution sol = new Solution();
        sol.setDisplayText("Soteria++ &#10;minimal cutset &#10;for " + cutset.getAttack());

        // Check below if solution supports the requirement
        if (Double.parseDouble(cutset.getLikelihood()) <= Double.parseDouble(acceptableProb)) {
            sol.setStatus(true);
        } else {
            sol.setStatus(false);
        }

        // setting hovering info for solution node
        String extraInfo =
                "Computed Likelihood = "
                        + cutset.getLikelihood()
                        + "&#10;Acceptable Likelihood = "
                        + acceptableProb
                        + "&#10;Implemented Defenses: "
                        + cutset.getDefenses();
        sol.setExtraInfo(extraInfo);
        sol.setUrl(soteriaCyberOutputAddr);

        // add sol to solutionNode
        solutionNode.setSolution(sol);

        return solutionNode;
    }

    /**
     * Creates a list of all port Ids from a CyberExpr
     *
     * @param cyberExpr
     * @return
     */
    public List<String> getCyberExprPorts(CyberExpr cyberExpr) {
        // to pack return list
        List<String> returnList = new ArrayList<>();

        if (!(cyberExpr.getPort() == null)) { // base case: if a port
            returnList.add(cyberExpr.getPort().getName());
        } else {
            if (cyberExpr.getKind().value().equalsIgnoreCase("or")) { // if an or expression
                for (CyberExpr subExpr : cyberExpr.getOr().getExpr()) {
                    returnList.addAll(getCyberExprPorts(subExpr));
                }
            } else if (cyberExpr
                    .getKind()
                    .value()
                    .equalsIgnoreCase("and")) { // if an and expression
                for (CyberExpr subExpr : cyberExpr.getAnd().getExpr()) {
                    returnList.addAll(getCyberExprPorts(subExpr));
                }
            } else if (cyberExpr.getKind().value().equalsIgnoreCase("not")) { // if a not expression
                returnList.addAll(getCyberExprPorts(cyberExpr.getNot()));
            }
        }
        return returnList;
    }

    /**
     * Creates a list of all portIds from a SafetyReqExpr
     *
     * @param safetyExpr
     * @return
     */
    public List<String> getSafetyReqExprPorts(SafetyReqExpr safetyExpr) {
        // to pack return list
        List<String> returnList = new ArrayList<>();

        if (!(safetyExpr.getPort() == null)) { // base case: if a port
            returnList.add(safetyExpr.getPort().getName());
        } else {
            if (safetyExpr.getKind().value().equalsIgnoreCase("or")) { // if an or expression
                for (SafetyReqExpr subExpr : safetyExpr.getOr().getExpr()) {
                    returnList.addAll(getSafetyReqExprPorts(subExpr));
                }
            } else if (safetyExpr
                    .getKind()
                    .value()
                    .equalsIgnoreCase("and")) { // if an and expression
                for (SafetyReqExpr subExpr : safetyExpr.getAnd().getExpr()) {
                    returnList.addAll(getSafetyReqExprPorts(subExpr));
                }
            } else if (safetyExpr
                    .getKind()
                    .value()
                    .equalsIgnoreCase("not")) { // if a not expression
                returnList.addAll(getSafetyReqExprPorts(safetyExpr.getNot()));
            }
        }

        return returnList;
    }

    /**
     * Creates and returns a list of GsnNodes that can be assigned as inContextOf for a goal
     *
     * @param contextNames
     * @param model
     * @return
     */
    public List<GsnNode> addGoalContexts(List<String> contextNames, Model model) {

        // A list of GsnNodes to contain the "inContextOf"
        List<GsnNode> inContextOf = new ArrayList<>();

        // If context is the main system, find owned subsystems
        if (contextNames.size() == 1 && contextNames.get(0).equalsIgnoreCase(model.getName())) {
            // a gsnNode for the context
            GsnNode contextNode = new GsnNode();

            // assigning values to the contextnode
            contextNode.setNodeType("context");
            String contextId = "CONTEXT_" + Integer.toString(contextCounter);
            contextCounter++;
            contextNode.setNodeId(contextId);

            // Find the owned subsystems
            List<String> ownedNames = new ArrayList<>();

            for (ComponentType cType : model.getComponentType()) {
                if (!(cType.getName().equalsIgnoreCase(contextNames.get(0)))) {
                    ownedNames.add(cType.getName());
                }
            }

            // pack the context
            Context nodeContext = new Context();
            nodeContext.setDisplayText(contextNames.get(0));
            if (!(ownedNames.isEmpty())) {
                String extraInfo = "Owns:";
                for (String owned : ownedNames) {
                    extraInfo = extraInfo + "&#10;" + owned;
                }
                nodeContext.setExtraInfo(extraInfo);
            }

            contextNode.setContext(nodeContext);

            // adding to the list
            inContextOf.add(contextNode);

        } else {

            // Removing duplicates from context_names
            List<String> duplicateFreeContextNames = new ArrayList<>(new HashSet<>(contextNames));

            for (String context : duplicateFreeContextNames) {

                // a gsnNode for the context
                GsnNode contextNode = new GsnNode();

                // assigning values to the contextnode
                contextNode.setNodeType("context");
                String contextId = "CONTEXT_" + Integer.toString(contextCounter);
                contextCounter++;
                contextNode.setNodeId(contextId);

                // Find the parent subsystems
                List<String> parentNames = new ArrayList<>();

                for (ComponentType cType : model.getComponentType()) {
                    for (Port port : cType.getPort()) {
                        if (port.getName().equalsIgnoreCase(context)) {
                            parentNames.add(cType.getName());
                        }
                    }
                }

                // pack the context
                Context nodeContext = new Context();
                nodeContext.setDisplayText(context);
                if (!(parentNames.isEmpty())) {
                    String extraInfo = "Owner:";
                    for (String parent : parentNames) {
                        extraInfo = extraInfo + "&#10;" + parent;
                    }
                    nodeContext.setExtraInfo(extraInfo);
                }

                contextNode.setContext(nodeContext);

                // adding to the list
                inContextOf.add(contextNode);
            }
        }

        return inContextOf;
    }

    /**
     * Gets the condition of a cyberExpr in preorder form
     *
     * @param cyberExpr
     * @return
     */
    public String getCyberExprCondition(CyberExpr cyberExpr) {
        // to pack return list
        String returnString = "";

        if (!(cyberExpr.getPort() == null)) { // base case: if a port
            returnString =
                    returnString
                            + " "
                            + cyberExpr.getPort().getName()
                            + ":"
                            + cyberExpr.getPort().getCia().toString().charAt(0);
        } else {
            if (cyberExpr.getKind().value().equalsIgnoreCase("or")) { // if an or expression
                returnString = returnString + "or (";
                for (CyberExpr subExpr : cyberExpr.getOr().getExpr()) {
                    returnString = returnString + " " + getCyberExprCondition(subExpr);
                }
                returnString = returnString + " )";
            } else if (cyberExpr
                    .getKind()
                    .value()
                    .equalsIgnoreCase("and")) { // if an and expression
                returnString = returnString + "and (";
                for (CyberExpr subExpr : cyberExpr.getAnd().getExpr()) {
                    returnString = returnString + " " + getCyberExprCondition(subExpr);
                }
                returnString = returnString + " )";
            } else if (cyberExpr.getKind().value().equalsIgnoreCase("not")) { // if a not expression
                returnString = returnString + "not ";
                returnString = returnString + " " + getCyberExprCondition(cyberExpr.getNot());
            }
        }

        return returnString;
    }

    /**
     * Gets the condition of a safetyReqExpr in preorder form
     *
     * @param safetyExpr
     * @return
     */
    public String getSafetyReqExprCondition(SafetyReqExpr safetyExpr) {
        // to pack return list
        String returnString = "";

        if (!(safetyExpr.getPort() == null)) { // base case: if a port
            returnString =
                    returnString
                            + " "
                            + safetyExpr.getPort().getName()
                            + ":"
                            + safetyExpr.getPort().getIa().toString().charAt(0);
        } else {
            if (safetyExpr.getKind().value().equalsIgnoreCase("or")) { // if an or expression
                returnString = returnString + "or (";
                for (SafetyReqExpr subExpr : safetyExpr.getOr().getExpr()) {
                    returnString = returnString + " " + getSafetyReqExprCondition(subExpr);
                }
                returnString = returnString + " )";
            } else if (safetyExpr
                    .getKind()
                    .value()
                    .equalsIgnoreCase("and")) { // if an and expression
                returnString = returnString + "and (";
                for (SafetyReqExpr subExpr : safetyExpr.getAnd().getExpr()) {
                    returnString = returnString + " " + getSafetyReqExprCondition(subExpr);
                }
                returnString = returnString + " )";
            } else if (safetyExpr
                    .getKind()
                    .value()
                    .equalsIgnoreCase("not")) { // if a not expression
                returnString = returnString + "not ";
                returnString = returnString + " " + getSafetyReqExprCondition(safetyExpr.getNot());
            }
        }

        return returnString;
    }

    /**
     * Creates a context node referring to CASE Properties
     *
     * @param addressForCASE
     * @return
     */
    public GsnNode getCASEContext(String addressForCASE) {
        GsnNode caseContextNode = new GsnNode();

        // a new Context to pack the context
        Context context = new Context();

        caseContextNode.setNodeType("context");
        String strategyContextId = "CONTEXT_" + Integer.toString(contextCounter);
        contextCounter++;
        caseContextNode.setNodeId(strategyContextId);
        context.setDisplayText("CASE Consolidated Properties");
        context.setExtraInfo("Address:&#10;" + addressForCASE);
        context.setUrl(addressForCASE);
        caseContextNode.setContext(context);

        return caseContextNode;
    }

    /**
     * Creates a justification Node
     *
     * @param justification
     * @return
     */
    public GsnNode getJustificationNode(String justificationText) {
        GsnNode justificationNode = new GsnNode();

        // a new Justification to pack the justification
        Justification justification = new Justification();

        justificationNode.setNodeType("justification");
        String justificationId = "JUSTIFICATION_" + Integer.toString(justificationCounter);
        justificationCounter++;
        justificationNode.setNodeId(justificationId);
        justification.setExtraInfo(justificationText);
        justificationNode.setJustification(justification);

        return justificationNode;
    }

    /**
     * Creates an assumption node
     *
     * @param assumptionText
     * @return
     */
    public GsnNode getAssumptionNode(String assumptionText) {
        GsnNode assumptionNode = new GsnNode();

        // a new Justification to pack the justification
        Assumption assumption = new Assumption();

        assumptionNode.setNodeType("assumption");
        String assumptionId = "ASSUMPTION_" + Integer.toString(assumptionCounter);
        assumptionCounter++;
        assumptionNode.setNodeId(assumptionId);
        assumption.setExtraInfo(assumptionText);
        assumptionNode.setAssumption(assumption);

        return assumptionNode;
    }

    /**
     * Takes a cyber requirement and the Soteria output and returns a list of cutsets
     *
     * @param cyberResults
     * @param cyberReqId
     * @return
     */
    public List<Cutset> getComponentThreatInfo(NodeList cyberResults, String cyberReqId) {
        // List of all Cutsets for a requirement
        List<Cutset> cutsets = new ArrayList<>();

        for (int i = 0; i < cyberResults.getLength(); i++) {
            Node req = cyberResults.item(i);

            // checking if this is the requirement we want
            if (req.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement0 = (Element) req;
                if (eElement0.getAttribute("label").equals(cyberReqId)) {
                    NodeList cutsetList = req.getChildNodes();
                    for (int j = 0; j < cutsetList.getLength(); j++) {
                        // to pack the cutset
                        Cutset packCutset = new Cutset();

                        Node cutset = cutsetList.item(j);
                        if (cutset.getNodeType() == Node.ELEMENT_NODE) {
                            Element eElement1 = (Element) cutset;
                            packCutset.setLikelihood(eElement1.getAttribute("likelihood"));
                        }
                        NodeList attackDefenseList = cutset.getChildNodes();

                        for (int k = 0; k < attackDefenseList.getLength(); k++) {
                            Node attackDefense = attackDefenseList.item(k);
                            NodeList componentList = attackDefense.getChildNodes();

                            for (int l = 0; l < componentList.getLength(); l++) {
                                Node component = componentList.item(l);

                                if (component.getNodeType() == Node.ELEMENT_NODE) {
                                    Element eElement2 = (Element) component;
                                    packCutset.setComponent(eElement2.getAttribute("comp"));
                                    if (eElement2.getAttribute("attack").length() > 0) {
                                        packCutset.setAttack(eElement2.getAttribute("attack"));
                                    }
                                    if (eElement2.getAttribute("suggested").length() > 0) {
                                        packCutset.setDefenses(eElement2.getAttribute("suggested"));
                                    }
                                }
                            }
                        }
                        // add the packet to the list if the likelihood is not null, i.e.,
                        // it is not an empty line
                        if (packCutset.getLikelihood() != null) {
                            cutsets.add(packCutset);
                        }
                    }
                }
            }
        }

        return cutsets;
    }

    /**
     * takes a list of cutsets and returns a list of components in the cutsets
     *
     * @param cutsets
     * @return
     */
    public List<String> getVulnerableComponentsFromCutsets(List<Cutset> cutsets) {
        List<String> components = new ArrayList<>();

        for (Cutset cutset : cutsets) {
            if (!components.contains(cutset.getComponent())) {
                components.add(cutset.getComponent());
            }
        }
        return components;
    }

    /**
     * returns acceptable probability for a requirement
     *
     * @param results
     * @param reqId
     * @return
     */
    public String getAcceptableProb(NodeList results, String reqId) {
        String acceptableProb = "";
        for (int temp = 0; temp < results.getLength(); temp++) {
            Node node = results.item(temp);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) node;
                if (eElement.getAttribute("label").equals(reqId)) {
                    acceptableProb = eElement.getAttribute("acceptable_p");
                }
            }
        }

        return acceptableProb;
    }
}
