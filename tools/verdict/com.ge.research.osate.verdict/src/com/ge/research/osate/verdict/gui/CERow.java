package com.ge.research.osate.verdict.gui;

import java.util.ArrayList;
import java.util.List;

/** Author: Soumya Talukder Date: Jul 18, 2019 */

// this class stores content of a row in counter-example viewer-tab
public class CERow {
    private List<String> rowContents = new ArrayList<String>();

    public void addRow(String str) {
        rowContents.add(str);
    }

    public List<String> getRowContents() {
        return rowContents;
    }
}
