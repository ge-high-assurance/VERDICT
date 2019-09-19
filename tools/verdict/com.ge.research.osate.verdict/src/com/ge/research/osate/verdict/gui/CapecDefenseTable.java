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
			List<ComponentAttributes> components = path.getComponents();
			for (int j = 0; j < components.size(); j++) {
				ComponentAttributes comp = components.get(j);
				List<String> capecs = comp.getCapecs();
				List<String> defenses = comp.getDefenses();
				List<String> descriptions = comp.getDescriptions();

				for (int k = 0; k < capecs.size(); k++) {
					CapecDefenseRow newRow = new CapecDefenseRow();
					if (k == 0) {
						if (j == 0) {
							newRow.addToRow("Path # " + (i + 1));
							newRow.addToRow(path.getLikeihood());
						} else {
							newRow.addToRow("-do-");
							newRow.addToRow("-do-");
						}
						newRow.addToRow(comp.getComponent());
					} else {
						newRow.addToRow("-do-");
						newRow.addToRow("-do-");
						newRow.addToRow("-do-");
					}
					newRow.addToRow(capecs.get(k));
					newRow.addToRow(defenses.get(k));
					newRow.addToRow(descriptions.get(k));
					contents.add(newRow);
				}
			}
		}
		return contents;
	}

	public List<CapecDefenseRow> getTableContents() {
		return tableContents;
	}
}