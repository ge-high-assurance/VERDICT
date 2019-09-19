package com.ge.research.osate.verdict.gui;

import java.util.ArrayList;
import java.util.List;

/**
*
* Author: Soumya Talukder
* Date: Jul 18, 2019
*
*/

//this class creates the MBAS Result table contents from the stored contents of MBAS .xml
public class MBASResultSummary {

	private List<MissionAttributes> missions;

	public MBASResultSummary(String fileName1, String fileName2) {
		MBASReadXMLFile xmlReader1 = new MBASReadXMLFile(fileName1);
		MBASReadXMLFile xmlReader2 = new MBASReadXMLFile(fileName2);
		missions = loadTableContents(xmlReader1.getContent(), xmlReader2.getContent());
	}

	public List<MissionAttributes> loadTableContents(List<MissionAttributes> missions1,
			List<MissionAttributes> missions2) {

		for (int i = 0; i < missions1.size(); i++) {
			List<RequirementAttributes> requirements = missions1.get(i).getRequirements();
			List<MBASSummaryRow> tableContents = new ArrayList<MBASSummaryRow>();
			for (int j = 0; j < requirements.size(); j++) {
				MBASSummaryRow newRow = new MBASSummaryRow();
				newRow.addToRow(requirements.get(j).getRequirement());
				newRow.addToRow(requirements.get(j).getAccptLikelihood());
				newRow.addToRow(requirements.get(j).getCalcLikelihood());
				if (requirements.get(j).hasSucceeded()) {
					newRow.addToRow("Satisfied");
				} else {
					newRow.addToRow("Failed to Satisfy");
				}
				newRow.setPaths(requirements.get(j).getPaths());
				tableContents.add(newRow);
			}
			missions1.get(i).setTableContents(tableContents);
		}
		// the code below is added later to take care of 2 source xml files instead of 1------------------
		for (int i = 0; i < missions2.size(); i++) {
			List<RequirementAttributes> requirements = missions2.get(i).getRequirements();
			List<MBASSummaryRow> tableContents = new ArrayList<MBASSummaryRow>();
			for (int j = 0; j < requirements.size(); j++) {
				MBASSummaryRow newRow = new MBASSummaryRow();
				newRow.addToRow(requirements.get(j).getRequirement());
				newRow.addToRow(requirements.get(j).getAccptLikelihood());
				newRow.addToRow(requirements.get(j).getCalcLikelihood());
				if (requirements.get(j).hasSucceeded()) {
					newRow.addToRow("Satisfied");
				} else {
					newRow.addToRow("Failed to Satisfy");
				}
				newRow.setPaths(requirements.get(j).getPaths());
				tableContents.add(newRow);
			}
			missions2.get(i).setTableContents(tableContents);
		}

		for (int i = 0; i < missions1.size(); i++) {
			List<MBASSummaryRow> tableContents1 = missions1.get(i).getTableContents();
			List<MBASSummaryRow> tableContents2 = missions2.get(i).getTableContents();
			for (int j = 0; j < tableContents1.size(); j++) {
				List<PathAttributes> paths1 = tableContents1.get(j).getPaths();
				List<PathAttributes> paths2 = tableContents2.get(j).getPaths();
				for (int k = 0; k < paths1.size(); k++) {
					List<ComponentAttributes> components1 = paths1.get(k).getComponents();
					List<ComponentAttributes> components2 = paths2.get(k).getComponents();
					for (int ii = 0; ii < components1.size(); ii++) {
						components1.get(ii).setDescriptions(components2.get(ii).getDefenses());
					}
				}
			}
		}
		// --------------------------------------------------------------------------------------
		return missions1;
	}

	public List<MissionAttributes> getMissions() {
		return missions;
	}
}