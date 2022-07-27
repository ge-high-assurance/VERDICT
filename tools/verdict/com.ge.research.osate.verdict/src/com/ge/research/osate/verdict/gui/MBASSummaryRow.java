package com.ge.research.osate.verdict.gui;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Author: Soumya Talukder
 * Date: Jul 18, 2019
 *
 */

// this class stores contents of a row of the MBAS Results table
public class MBASSummaryRow {
    private List<String> rowContents = new ArrayList<String>();
    private List<PathAttributes> paths = null;

    public void addToRow(String str) {
        rowContents.add(str);
    }

    public List<String> getRowContents() {
        return rowContents;
    }

    public void setPaths(List<PathAttributes> p) {
        paths = p;
    }

    public List<PathAttributes> getPaths() {
        return paths;
    }
}
