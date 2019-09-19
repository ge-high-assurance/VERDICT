package com.ge.research.osate.verdict.gui;

import java.util.ArrayList;
import java.util.List;

/**
*
* Author: Soumya Talukder
* Date: Jul 18, 2019
*
*/

//This class stores the contents of a row in Vulnerability/Defense table

public class CapecDefenseRow {
	private List<String> rowContents = new ArrayList<String>();

	public void addToRow(String str) {
		rowContents.add(str);
	}

	public List<String> getRowContents() {
		return rowContents;
	}
}