package com.ge.verdict.gsn;

import java.util.ArrayList;
import java.util.List;

public class GsnFragment {

	/**
	 * A GsnFragment starts at a rootNode
	 */
    protected GsnNode rootNode;
    

//	/**
//	 * A GsnFragment has a list of subfragments
//	 */
//    protected List<GsnFragment> subFragments;

    
    /**
     * Gets the value of the rootNode.
     *
     * @return possible object is {@link GsnNode }
     */
    public GsnNode getRootNode() {
        return rootNode;
    }

    /**
     * Sets the value of the rootNode.
     *
     * @param value allowed object is {@link GsnNode }
     */
    public void setRootNode(GsnNode value) {
        this.rootNode = value;
    }
    
    
//    /**
//     * Gets the value of the subFragments field.
//     *
//     * <p>This accessor method returns a reference to the live list, not a snapshot. Therefore any
//     * modification you make to the returned list will be present inside the JAXB object. This is
//     * why there is not a <CODE>set</CODE> method for the typeDeclaration property.
//     *
//     * <p>For example, to add a new item, do as follows:
//     *
//     * <pre>
//     *    getSubFragments().add(newItem);
//     * </pre>
//     *
//     * <p>Objects of the following type(s) are allowed in the list {@link GsnFragment }
//     */
//    public List<GsnFragment> getSubFragments() {
//        if (subFragments == null) {
//        	subFragments = new ArrayList<GsnFragment>();
//        }
//        return this.subFragments;
//    }

    
    
    
}
