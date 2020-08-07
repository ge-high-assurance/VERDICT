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

public class IVCNode implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;
	private String nodeName;
	private List<IVCElement> nodeElements = new ArrayList<IVCElement>();
	
	public void setNodeName(String name) {
		nodeName = name;
	}

	public String getNodeName() {
		return nodeName;
	}
	
	public void setNodeElements(List<IVCElement> els) {
		nodeElements = els;
	}

	public List<IVCElement> getNodeElements() {
		return nodeElements;
	}
	
	public boolean hasAssumption() {
		return nodeElements.stream().anyMatch(IVCElement::isAssumption);
	}
}
