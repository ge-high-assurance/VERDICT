package com.ge.research.osate.verdict.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
*
* Author: Soumya Talukder
* Date: Jul 18, 2019
*
*/

//this class stores content of a mission
public class MissionInfo {
	private String missionID;
	private Set<Integer> rowInTable = new HashSet<Integer>();
	private String verdictStatement;
	private List<String> cyberReqs = new ArrayList<String>();
	private String comment = null;
	private String description = null;

	public void setMissionID(String str) {
		missionID = str;
	}

	public String getMissionID() {
		return missionID;
	}

	public void addToRow(int i) {
		rowInTable.add(i);
	}

	public void removeRow(int i) {
		rowInTable.remove(i);
	}

	public Set<Integer> getRow() {
		return rowInTable;
	}

	public void setRow(Set<Integer> set) {
		rowInTable = set;
	}

	public String getVerdictStatement() {
		return verdictStatement;
	}

	public void setVerdictStatement(String str) {
		this.verdictStatement = str;
	}

	public void addToCyberReqs(String str) {
		cyberReqs.add(str);
	}

	public List<String> getCyberReqs() {
		return cyberReqs;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String str) {
		this.comment = str;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String str) {
		this.description = str;
	}
}