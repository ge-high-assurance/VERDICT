package com.ge.research.osate.verdict.gui;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Author: Soumya Talukder
 * Date: Jul 18, 2019
 *
 */

// This class stores the contents of a row in Vulnerability/Defense table

public class CapecDefenseRow {
    private List<String> rowContents = new ArrayList<>();
    private List<String> attackHoverText = new ArrayList<>();
    private List<String> defenseHoverText = new ArrayList<>();

    public void addToRow(String str) {
        rowContents.add(str);
    }

    public List<String> getRowContents() {
        return rowContents;
    }

    public void addAttackHoverText(String hoverTextItem) {
        attackHoverText.add(hoverTextItem);
    }

    public List<String> getAttackHoverText() {
        return attackHoverText;
    }

    public void addDefenseHoverText(String hoverTextItem) {
        defenseHoverText.add(hoverTextItem);
    }

    public List<String> getDefenseHoverText() {
        return defenseHoverText;
    }
}
