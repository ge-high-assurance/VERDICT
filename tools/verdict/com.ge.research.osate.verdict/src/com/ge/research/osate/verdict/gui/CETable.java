package com.ge.research.osate.verdict.gui;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/** Author: Soumya Talukder Date: Jul 18, 2019 */

// this class creates contents of the counter-example table,
// extracting from the content read from CRV .xml
public class CETable {

    private List<CounterExampleAttributes> ceList;
    private List<CERow> tableContents = new ArrayList<CERow>();

    public CETable(Display display, Shell lastShell, List<CounterExampleAttributes> list) {
        ceList = list;
        tableContents = loadTableContents(ceList);
    }

    private List<CERow> loadTableContents(List<CounterExampleAttributes> list) {
        List<CERow> contents = new ArrayList<CERow>();
        for (int i = 0; i < list.size(); i++) {
            CounterExampleAttributes ce = list.get(i);
            String sysName = ce.getNodeName();
            List<CENode> nodeAttr = ce.getNodeAttr();
            for (int j = 0; j < nodeAttr.size(); j++) {
                CERow newRow = new CERow();
                newRow.addRow(sysName);
                CENode node = nodeAttr.get(j);
                newRow.addRow(node.getVarName());
                newRow.addRow(node.getVarClass());
                newRow.addRow(node.getVarType());
                for (int k = 0; k < node.getVarValue().size(); k++) {
                    newRow.addRow(node.getVarValue().get(k));
                }
                contents.add(newRow);
                sysName = "-do-";
            }
        }

        return contents;
    }

    public List<CERow> getTableContents() {
        return tableContents;
    }
}
