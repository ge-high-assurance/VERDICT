package com.ge.verdict.mbaas.synthesis.adtree;

import java.util.ArrayList;
import java.util.List;

public class AttackAndNode extends AttackTree {
    private List<AttackTree> attackTrees = new ArrayList<AttackTree>();

    public void addAttackTree(AttackTree attackTree) {
        attackTrees.add(attackTree);
    }

    public List<AttackTree> getAttackTrees() {
        return attackTrees;
    }

    public void setAttackTrees(List<AttackTree> attackTrees) {
        this.attackTrees = attackTrees;
    }
}
