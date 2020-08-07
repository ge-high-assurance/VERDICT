package com.ge.research.osate.verdict.gui;

import java.util.ArrayList;
import java.util.List;

/**
*
* Author: Soumya Talukder
* Date: Jul 18, 2019
*
*/

//This class stores the contents of a row in the CRV Results tables
public class CRVSummaryRow {
	private String propertyName = "";
	private List<String> rowContents = new ArrayList<String>();
	private List<CounterExampleAttributes> counterExample = null;
	private List<CounterExampleAttributes> testCase = null;
	private String validTill = "";

	public void addRow(String str) {
		rowContents.add(str);
	}

	public List<String> getRowContents() {
		return rowContents;
	}

	public void setCounterExample(List<CounterExampleAttributes> ce) {
		counterExample = ce;
	}

	public List<CounterExampleAttributes> getCounterExample() {
		return counterExample;
	}

	public void setValidTill(String str) {
		validTill = str;
	}

	public String getValidTill() {
		return validTill;
	}

	public void setTestCase(List<CounterExampleAttributes> ce) {
		testCase = ce;
	}

	public List<CounterExampleAttributes> getTestCase() {
		return testCase;
	}

	public void setPropertyName(String name) {
		this.propertyName = name;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public boolean hasCounterExample() {
		return counterExample != null && !counterExample.isEmpty();
	}

	public boolean hasAttackType() {
		return rowContents.size() > 2 && rowContents.get(2).length() > 0;
	}

	public boolean hasCompromisedComponents() {
		return rowContents.size() > 3 && rowContents.get(3).length() > 0;
	}

	public boolean hasCompromisedLinks() {
		return rowContents.size() > 4 && rowContents.get(4).length() > 0;
	}

	public boolean hasUncompromisedComponents() {
		return rowContents.size() > 5 && rowContents.get(5).length() > 0;
	}

	public boolean hasUncompromisedLinks() {
		return rowContents.size() > 6 && rowContents.get(6).length() > 0;
	}
}