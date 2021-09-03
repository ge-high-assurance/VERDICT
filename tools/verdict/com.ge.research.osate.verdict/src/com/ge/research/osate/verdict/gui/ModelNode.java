package com.ge.research.osate.verdict.gui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
*
* Author: Daniel Larraz
* Date: Aug 6, 2020
*
*/

public class ModelNode implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;
	private String nodeName;
	private List<ModelElement> nodeElements = new ArrayList<ModelElement>();
	
	public void setNodeName(String name) {
		nodeName = name;
	}

	public String getNodeName() {
		return nodeName;
	}
	
	public void setNodeElements(List<ModelElement> els) {
		nodeElements = els;
	}

	public List<ModelElement> getNodeElements() {
		return nodeElements;
	}
	
	public boolean hasAssumption() {
		return nodeElements.stream().anyMatch(ModelElement::isAssumption);
	}
}
