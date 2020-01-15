package com.ge.research.osate.verdict.gui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
*
* Author: Soumya Talukder
* Date: Jul 18, 2019
*
*/

//this class stores the contents of a "Path" element in the MBAS .xml
public class PathAttributes implements Serializable, Cloneable {
	
	public static class ComponentData {
		private String component;
		private String data;
		
		public ComponentData(String component, String data) {
			this.component = component;
			this.data = data;
		}
		
		public String getComponent() {
			return component;
		}
		
		public String getData() {
			return data;
		}
		
		public String getJoined() {
			return component + ":" + data;
		}
	}

	private static final long serialVersionUID = 1L;
	private String likelihood;
	private List<ComponentData> componentCapecs = new ArrayList<>();
	private List<ComponentData> componentApplicableDefenses = new ArrayList<>();
	private List<ComponentData> componentImplDefenses = new ArrayList<>();

	public void setLikelihood(String str) {
		likelihood = str;
	}

	public String getLikelihood() {
		return likelihood;
	}
	
	public void setComponentCapecs(List<ComponentData> componentCapecs) {
		this.componentCapecs = componentCapecs;
	}
	
	public List<ComponentData> getComponentCapecs() {
		return componentCapecs;
	}
	
	public void setComponentDefenses(List<ComponentData> componentDefenses) {
		this.componentApplicableDefenses = componentDefenses;
	}
	
	public List<ComponentData> getComponentDefenses() {
		return componentApplicableDefenses;
	}
	
	public void setComponentImplDefense(List<ComponentData> componentImplDefenses) {
		this.componentImplDefenses = componentImplDefenses;
	}
	
	public List<ComponentData> getComponentImplDefenses() {
		return componentImplDefenses;
	}
	
	private static String joinComponentDataList(List<ComponentData> data) {
		List<String> joined = data.stream()
				.map(ComponentData::getJoined)
				.collect(Collectors.toList());
		return String.join("\n ^ ", joined);
	}
	
	public String attacks() {
		return joinComponentDataList(componentCapecs);
	}
	
	public String applicableDefenses() {
		return joinComponentDataList(componentApplicableDefenses);
	}
	
	public String implDefenses() {
		return joinComponentDataList(componentImplDefenses);
	}
	
	public boolean compareCutset(PathAttributes other) {
		// We use this so that we can search for matching cutsets
		// between ApplicableDefenseProperties and ImplProperties.
		
		if (other.componentCapecs.size() != componentCapecs.size()) {
			return false;
		}
		for (int i = 0; i < componentCapecs.size(); i++) {
			if (!other.componentCapecs.get(i).getComponent().equals(componentCapecs.get(i).getComponent())
					|| !other.componentCapecs.get(i).getData().equals(componentCapecs.get(i).getData())) {
				return false;
			}
		}
		return true;
	}
}