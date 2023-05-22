package com.ge.research.osate.verdict.gui;

import com.ge.research.osate.verdict.gui.PathAttributes.ComponentData;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Author: Soumya Talukder Date: Jul 18, 2019 */

// This class creates the row contents of Vulnerability/Defense tables, extracting from
// the contents read from the MBAS .xml output

public class CapecDefenseTable {

    private List<PathAttributes> pathList;
    private List<CapecDefenseRow> tableContents = new ArrayList<CapecDefenseRow>();

    public CapecDefenseTable(
            Display display,
            Shell lastShell,
            List<PathAttributes> list,
            Map<String, String> attackDesc,
            Map<String, String> defenseDesc) {
        pathList = list;
        tableContents = loadTableContents(pathList, attackDesc, defenseDesc);
    }

    private List<CapecDefenseRow> loadTableContents(
            List<PathAttributes> list,
            Map<String, String> attackDesc,
            Map<String, String> defenseDesc) {
        List<CapecDefenseRow> contents = new ArrayList<CapecDefenseRow>();
        for (int i = 0; i < list.size(); i++) {
            PathAttributes path = list.get(i);
            CapecDefenseRow newRow = new CapecDefenseRow();
            newRow.addToRow("Path # " + (i + 1));
            newRow.addToRow(path.getLikelihood());
            newRow.addToRow(path.attacks());
            newRow.addToRow(path.suggestedDefenses());
            newRow.addToRow(path.suggestedDefensesProfile());
            newRow.addToRow(path.implDefenses());

            // Find descriptions for all CAPECs and NISTS
            // We don't have the unformatted attacks/defenses, so we need to do a regex split
            // We ignore "(", ")", and whitespace, plus the words "and" and "or"
            for (ComponentData data : path.getComponentAttacks()) {
                String[] keys = data.getData().split("[\\s()]");
                for (String key : keys) {
                    if (key.length() == 0 || "and".equals(key) || "or".equals(key)) {
                        continue;
                    } else if (attackDesc.containsKey(key)) {
                        newRow.addAttackHoverText(key + ": " + attackDesc.get(key));
                    } else {
                        System.err.println("could not find attack key: " + key);
                    }
                }
            }
            for (ComponentData data : path.getComponentSuggestedDefensesProfile()) {
                String[] keys = data.getData().split("[\\s()]");
                for (String key : keys) {
                    if (key.length() == 0 || "and".equals(key) || "or".equals(key)) {
                        continue;
                    } else if (defenseDesc.containsKey(key)) {
                        newRow.addDefenseHoverText(key + ": " + defenseDesc.get(key));
                    } else {
                        System.err.println("could not find defense key: " + key);
                    }
                }
            }

            contents.add(newRow);
        }
        return contents;
    }

    public List<CapecDefenseRow> getTableContents() {
        return tableContents;
    }
}
