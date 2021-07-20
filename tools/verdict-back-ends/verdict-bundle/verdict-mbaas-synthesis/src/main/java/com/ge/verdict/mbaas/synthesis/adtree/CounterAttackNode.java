package com.ge.verdict.mbaas.synthesis.adtree;

public class CounterAttackNode extends AttackTree {
    private AttackNode attackNode;
    private DefenseTree counterDefTree;

    public CounterAttackNode(AttackNode attackNode, DefenseTree counterDefTree) {
        this.setAttackNode(attackNode);
        this.setCounterDefTree(counterDefTree);
    }

    public AttackNode getAttackNode() {
        return attackNode;
    }

    public void setAttackNode(AttackNode attackNode) {
        this.attackNode = attackNode;
    }

    public DefenseTree getCounterDefTree() {
        return counterDefTree;
    }

    public void setCounterDefTree(DefenseTree counterDefTree) {
        this.counterDefTree = counterDefTree;
    }
}
