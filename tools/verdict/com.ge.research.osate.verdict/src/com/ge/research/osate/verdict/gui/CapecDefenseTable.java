package com.ge.research.osate.verdict.gui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
*
* Author: Soumya Talukder
* Date: Jul 18, 2019
*
*/

//This class creates the row contents of Vulnerability/Defense tables, extracting from
// the contents read from the MBAS .xml output

public class CapecDefenseTable {

	private List<PathAttributes> pathList;
	private List<CapecDefenseRow> tableContents = new ArrayList<CapecDefenseRow>();

	public CapecDefenseTable(Display display, Shell lastShell, List<PathAttributes> list) {
		pathList = list;
		tableContents = loadTableContents(pathList);
	}

	private List<CapecDefenseRow> loadTableContents(List<PathAttributes> list) {
		List<CapecDefenseRow> contents = new ArrayList<CapecDefenseRow>();
		for (int i = 0; i < list.size(); i++) {
			PathAttributes path = list.get(i);
			CapecDefenseRow newRow = new CapecDefenseRow();
			newRow.addToRow("Path # " + (i + 1));
			newRow.addToRow(path.getLikelihood());
			newRow.addToRow(path.attacks());
			newRow.addToRow(path.applicableDefenses());
			newRow.addToRow(path.implDefenses());
			contents.add(newRow);
		}
		return contents;
	}

	public List<CapecDefenseRow> getTableContents() {
		return tableContents;
	}
}