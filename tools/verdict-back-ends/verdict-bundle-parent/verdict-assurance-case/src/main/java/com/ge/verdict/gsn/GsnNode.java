package com.ge.verdict.gsn;

import java.util.ArrayList;
import java.util.List;

/** @author Saswata paul */
public class GsnNode {

    /** An integer describing the level of a node */
    protected int nodeLevel;

    /** A unique Id for each node */
    protected String nodeId;

    /** Can be one of: ["goal", "strategy", "solution", "context"] */
    protected String nodeType;

    /** To store relevant information based on nodeType */
    protected Goal goal;

    protected Strategy strategy;
    protected Solution solution;
    protected Context context;

    /** List of nodes which support a node */
    protected List<GsnNode> supportedBy;

    /** List of context nodes of a node */
    protected List<GsnNode> inContextOf;

    /**
     * Gets the value of the nodeLevel property.
     *
     * @return possible object is {@link int }
     */
    public int getNodeLevel() {
        return nodeLevel;
    }

    /**
     * Sets the value of the nodeLevel property.
     *
     * @param value allowed object is {@link int }
     */
    public void setNodeLevel(int value) {
        this.nodeLevel = value;
    }

    /**
     * Gets the value of the nodeId property.
     *
     * @return possible object is {@link String }
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Sets the value of the nodeId property.
     *
     * @param value allowed object is {@link String }
     */
    public void setNodeId(String value) {
        this.nodeId = value;
    }

    /**
     * Gets the value of the nodeType property.
     *
     * @return possible object is {@link String }
     */
    public String getNodeType() {
        return nodeType;
    }

    /**
     * Sets the value of the nodeType property.
     *
     * @param value allowed object is {@link String }
     */
    public void setNodeType(String value) {
        this.nodeType = value;
    }

    /**
     * Gets the value of the goal property.
     *
     * @return possible object is {@link Goal }
     */
    public Goal getGoal() {
        return goal;
    }

    /**
     * Sets the value of the goal property.
     *
     * @param value allowed object is {@link Goal }
     */
    public void setGoal(Goal value) {
        this.goal = value;
    }

    /**
     * Gets the value of the strategy property.
     *
     * @return possible object is {@link Strategy }
     */
    public Strategy getStrategy() {
        return strategy;
    }

    /**
     * Sets the value of the strategy property.
     *
     * @param value allowed object is {@link Strategy }
     */
    public void setStrategy(Strategy value) {
        this.strategy = value;
    }

    /**
     * Gets the value of the solution property.
     *
     * @return possible object is {@link Solution }
     */
    public Solution getSolution() {
        return solution;
    }

    /**
     * Sets the value of solution the property.
     *
     * @param value allowed object is {@link Solution }
     */
    public void setSolution(Solution value) {
        this.solution = value;
    }

    /**
     * Gets the value of the context property.
     *
     * @return possible object is {@link Context }
     */
    public Context getContext() {
        return context;
    }

    /**
     * Sets the value of the property.
     *
     * @param value allowed object is {@link Context }
     */
    public void setContext(Context value) {
        this.context = value;
    }

    /**
     * Gets the value of the supportedBy field.
     *
     * <p>This accessor method returns a reference to the live list, not a snapshot. Therefore any
     * modification you make to the returned list will be present inside the JAXB object. This is
     * why there is not a <CODE>set</CODE> method for the supportedBy property.
     *
     * <p>For example, to add a new item, do as follows:
     *
     * <pre>
     *    getSupportedBy().add(newItem);
     * </pre>
     *
     * <p>Objects of the following type(s) are allowed in the list {@link GsnNode }
     */
    public List<GsnNode> getSupportedBy() {
        if (supportedBy == null) {
            supportedBy = new ArrayList<GsnNode>();
        }
        return this.supportedBy;
    }

    /**
     * Gets the value of the incontextOf field.
     *
     * <p>This accessor method returns a reference to the live list, not a snapshot. Therefore any
     * modification you make to the returned list will be present inside the JAXB object. This is
     * why there is not a <CODE>set</CODE> method for the inContextOf property.
     *
     * <p>For example, to add a new item, do as follows:
     *
     * <pre>
     *    getInContextOf().add(newItem);
     * </pre>
     *
     * <p>Objects of the following type(s) are allowed in the list {@link GsnNode }
     */
    public List<GsnNode> getInContextOf() {
        if (inContextOf == null) {
            inContextOf = new ArrayList<GsnNode>();
        }
        return this.inContextOf;
    }
}
