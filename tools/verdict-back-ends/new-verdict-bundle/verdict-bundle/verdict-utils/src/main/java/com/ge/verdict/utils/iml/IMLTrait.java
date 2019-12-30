package com.ge.verdict.utils.iml;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * */
public class IMLTrait {	
	String name;
	String qualifiedName;
	boolean isComponentRefinement;
	boolean isSystemRefinement;
	List<String> inDataPorts = new ArrayList<>();
	List<String> outDataPorts = new ArrayList<>();
	

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getQualifiedName() {
		return qualifiedName;
	}
	public void setQualifiedName(String qualifiedName) {
		this.qualifiedName = qualifiedName;
	}
	public boolean isComponentRefinement() {
		return isComponentRefinement;
	}
	public void setComponentRefinement(boolean isComponentRefinement) {
		this.isComponentRefinement = isComponentRefinement;
	}
	public boolean isSystemRefinement() {
		return isSystemRefinement;
	}
	public void setSystemRefinement(boolean isSystemRefinement) {
		this.isSystemRefinement = isSystemRefinement;
	}	
	public List<String> getInDataPorts() {
		return inDataPorts;
	}
	public void setInDataPorts(List<String> inDataPorts) {
		if(inDataPorts != null) {
			this.inDataPorts.addAll(inDataPorts);
		}
	}
	public List<String> getOutDataPorts() {
		return outDataPorts;
	}
	public void setOutDataPorts(List<String> outDataPorts) {
		if(outDataPorts != null) {
			this.outDataPorts.addAll(outDataPorts);
		}
	}	
}
