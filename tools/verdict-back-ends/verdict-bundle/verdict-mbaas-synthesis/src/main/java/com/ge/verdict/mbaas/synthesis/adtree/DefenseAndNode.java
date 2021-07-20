package com.ge.verdict.mbaas.synthesis.adtree;

import java.util.ArrayList;
import java.util.List;

public class DefenseAndNode extends DefenseTree {
    private List<DefenseTree> defenseTrees = new ArrayList<DefenseTree>();

    public void addDefenseTree(DefenseTree attackTree) {
        defenseTrees.add(attackTree);
    }

    public List<DefenseTree> getDefenseTrees() {
        return defenseTrees;
    }

    public void setAttackTrees(List<DefenseTree> defenseTrees) {
        this.defenseTrees = defenseTrees;
    }
}
