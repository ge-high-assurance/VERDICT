package com.ge.research.osate.verdict.gui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
*
* Author: Soumya Talukder
* Date: Jul 18, 2019
*
*/

//this class stores the attributes of a counter-example element extracted from CRV .xml
public class CounterExampleAttributes implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;
	private String nodeName;
	private List<CENode> nodeAttr = new ArrayList<CENode>();

	public void setNodeName(String str) {
		nodeName = str;
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeAttr(List<CENode> ceNodes) {
		nodeAttr = ceNodes;
	}

	public List<CENode> getNodeAttr() {
		return nodeAttr;
	}
}