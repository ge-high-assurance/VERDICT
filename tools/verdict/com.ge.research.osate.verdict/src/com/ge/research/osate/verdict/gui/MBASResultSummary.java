package com.ge.research.osate.verdict.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Author: Soumya Talukder Date: Jul 18, 2019 */

// this class creates the MBAS Result table contents from the stored contents of MBAS .xml
public class MBASResultSummary {

    private List<MissionAttributes> missions;

    public MBASResultSummary(String applicableDefense, String implProperty) {
        MBASReadXMLFile applicableDefenseReader = new MBASReadXMLFile(applicableDefense);
        MBASReadXMLFile implPropertyReader = new MBASReadXMLFile(implProperty);
        missions =
                loadTableContents(
                        applicableDefenseReader.getContent(), implPropertyReader.getContent());
    }

    public List<MissionAttributes> loadTableContents(
            List<MissionAttributes> missions1, List<MissionAttributes> missions2) {

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
        // the code below is added later to take care of 2 source xml files instead of
        // 1------------------
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

        // Assumption: mission1 and mission2 should have the same number of things
        checkSameSizeOfTwoLists(missions1, missions2);

        for (int i = 0; i < missions1.size(); i++) {
            List<MBASSummaryRow> tableContents1 = missions1.get(i).getTableContents();
            List<MBASSummaryRow> tableContents2 = missions2.get(i).getTableContents();

            checkSameSizeOfTwoLists(tableContents1, tableContents2);

            for (int j = 0; j < tableContents1.size(); j++) {
                List<PathAttributes> paths1 = tableContents1.get(j).getPaths();
                // the size of missions1.get(i).getRequirements() should be the same as the size of
                // tableContents1
                RequirementAttributes reqAttr1 = missions1.get(i).getRequirements().get(j);
                List<PathAttributes> paths2 = getPaths(missions1.get(i), reqAttr1, missions2);

                // assumption: same size of path1 and path2
                checkSameSizeOfTwoLists(paths1, paths2);

                for (int k = 0; k < paths1.size(); k++) {
                    PathAttributes path = paths1.get(k);
                    Optional<PathAttributes> other =
                            paths2.stream().filter(o -> path.compareCutset(o)).findFirst();
                    if (other.isPresent()) {
                        path.setComponentImplDefense(other.get().getComponentSuggestedDefenses());
                    } else {
                        System.err.println("Missing impl defenses");
                        path.setComponentImplDefense(Collections.emptyList());
                    }
                }
            }
        }
        // --------------------------------------------------------------------------------------
        return missions1;
    }

    /*
     * Try to find the matching path attributes of mission1 in mission2
     */
    private List<PathAttributes> getPaths(
            MissionAttributes mission1,
            RequirementAttributes reqAttr1,
            List<MissionAttributes> missions2) {
        String missionName = mission1.getMission();
        String req1Name = reqAttr1.getRequirement();

        for (int i = 0; i < missions2.size(); ++i) {
            if (missions2.get(i).getMission().equals(missionName)) {
                List<RequirementAttributes> mission2Reqs = missions2.get(i).getRequirements();

                for (int j = 0; j < mission2Reqs.size(); ++j) {
                    RequirementAttributes reqAttr2 = mission2Reqs.get(j);
                    if (reqAttr2.getRequirement().equals(req1Name)) {
                        return missions2.get(i).getTableContents().get(j).getPaths();
                    }
                }
            }
        }

        return new ArrayList<PathAttributes>();
    }

    private void checkSameSizeOfTwoLists(List<?> tableContents1, List<?> tableContents2) {
        if (tableContents1.size() != tableContents2.size()) {
            throw new RuntimeException(
                    "unexpected the size of two lists from Soteria++ is not the same!");
        }
    }

    public List<MissionAttributes> getMissions() {
        return missions;
    }

    public void updateMissionsWithSafety(Map<String, List<MBASSafetyResult>> safetyResults) {
        Set<String> coveredMissions = new HashSet<>();
        for (MissionAttributes mission : missions) {
            if (safetyResults.containsKey(mission.getMission())) {
                mission.updateSuccessWithSafety(safetyResults.get(mission.getMission()));
                coveredMissions.add(mission.getMission());
            }
        }
        for (String mission : safetyResults.keySet()) {
            if (!coveredMissions.contains(mission)) {
                MissionAttributes att = new MissionAttributes();
                att.setMission(mission);
                att.setRequirements(Collections.emptyList());
                att.setTableContents(Collections.emptyList());
                att.updateSuccessWithSafety(safetyResults.get(mission));
                missions.add(att);
            }
        }
    }
}
