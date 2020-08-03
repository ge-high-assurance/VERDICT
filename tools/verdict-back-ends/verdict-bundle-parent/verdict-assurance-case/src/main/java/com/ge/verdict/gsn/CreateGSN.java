package com.ge.verdict.gsn;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import verdict.vdm.vdm_model.*;

/** @author Saswata Paul */
public class CreateGSN {
    static final String SEP = File.separator;
    // For naming the nodes uniformly
    private int strategyCounter = 1;
    private int contextCounter = 1;
    private int solutionCounter = 1;
    private int justificationCounter = 1;
    private int assumptionCounter = 1;
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
            String rootGoalId)
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
                                aMission, xmlModel, nListCyber, nListSafety, addressForCASE);
            }
        }
        for (CyberReq aCyberReq : xmlModel.getCyberReq()) {
            if (aCyberReq.getId().equalsIgnoreCase(rootGoalId)) {
                returnFragment =
                        populateCyberRequirementNode(
                                aCyberReq, xmlModel, nListCyber, addressForCASE);
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
     */
    public GsnNode populateMissionNode(
            Mission mission,
            Model model,
            NodeList cyberResults,
            NodeList safetyResults,
            String addressForCASE) {

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
                    strategyNode
                            .getSupportedBy()
                            .add(
                                    populateCyberRequirementNode(
                                            cyberReq, model, cyberResults, addressForCASE));
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
     * Creates a GSN node for the given cyber subrequirement
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
        GsnNode solutionNode = populateSolutionNode(cyberReq.getId(), cyberResults, true);
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
        GsnNode solutionNode = populateSolutionNode(safetyReq.getId(), safetyResults, false);
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
     * Creates a GSN node for a solution
     *
     * @param safetyReq
     * @param model
     * @param safetyResults
     * @return
     */
    public GsnNode populateSolutionNode(String reqId, NodeList results, boolean cyberFlag) {
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
}
