package com.ge.verdict.mbaas.synthesis.adtree;

public class CounterDefenseNode extends DefenseTree {
    private DefenseNode defenseNode;
    private AttackTree attackTree;

    public CounterDefenseNode(DefenseNode defenseNode, AttackTree attackTree) {
        this.setDefenseNode(defenseNode);
        this.setAttackTree(attackTree);
    }

    public AttackTree getAttackTree() {
        return attackTree;
    }

    public void setAttackTree(AttackTree attackTree) {
        this.attackTree = attackTree;
    }

    public DefenseNode getDefenseNode() {
        return defenseNode;
    }

    public void setDefenseNode(DefenseNode defenseNode) {
        this.defenseNode = defenseNode;
    }
}
