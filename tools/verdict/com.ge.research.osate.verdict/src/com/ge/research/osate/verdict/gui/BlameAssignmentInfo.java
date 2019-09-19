package com.ge.research.osate.verdict.gui;

import java.util.ArrayList;
import java.util.List;

/**
*
* Author: Soumya Talukder
* Date: Jul 18, 2019
*
*/

//this class stores contents related to Blame-assignment (used by CRV Results viewer-tab)
public class BlameAssignmentInfo {
	private String threatDescription = "";
	private List<String> components = new ArrayList<>();
	private List<String> links = new ArrayList<>();
	private List<String> componentsUncompromised = new ArrayList<>();
	private List<String> linksUncompromised = new ArrayList<>();

	private String buildStringList(List<String> list) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < list.size() - 1; i++) {
			builder.append(list.get(i));
			builder.append(", ");
		}
		if (list.size() > 0) {
			builder.append(list.get(list.size() - 1));
		}
		return builder.toString();
	}

	public void addComponent(String str) {
		components.add(str);
	}

	public String getComponents() {
		return buildStringList(components);
	}

	public void addLink(String str) {
		links.add(str);
	}

	public String getLinks() {
		return buildStringList(links);
	}

	public void addComponentUncompromised(String str) {
		componentsUncompromised.add(str);
	}

	public String getComponentsUncompromised() {
		return buildStringList(componentsUncompromised);
	}

	public void addLinkUncompromised(String str) {
		linksUncompromised.add(str);
	}

	public String getLinksUncompromised() {
		return buildStringList(linksUncompromised);
	}

	public void setThreat(String str) {
		threatDescription = str;
	}

	public String getThreat() {
		return threatDescription;
	}
}