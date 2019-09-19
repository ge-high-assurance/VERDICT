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

//this class stores the contents of a "Path" element in the MBAS .xml
public class PathAttributes implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;
	private String likelihood;
	private List<ComponentAttributes> components = new ArrayList<ComponentAttributes>();

	public void setLikelihood(String str) {
		likelihood = str;
	}

	public String getLikeihood() {
		return likelihood;
	}

	public void setComponents(List<ComponentAttributes> comps) {
		components = comps;
	}

	public List<ComponentAttributes> getComponents() {
		return components;
	}
}