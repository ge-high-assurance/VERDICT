package com.ge.verdict.mbaas.synthesis.adtree;

import com.ge.verdict.mbaas.synthesis.model.Entity;
import verdict.vdm.vdm_model.CIA;

public class AttackNode extends AttackTree {
    private String attackName;
    private Entity attackableEntity;
    private CIA cia;

    public AttackNode(String name, Entity entity, CIA cia) {
        this.setAttackName(name);
        this.setAttackableEntity(entity);
        this.setCia(cia);
    }

    public String getAttackName() {
        return attackName;
    }

    public void setAttackName(String attackName) {
        this.attackName = attackName;
    }

    public Entity getAttackableEntity() {
        return attackableEntity;
    }

    public void setAttackableEntity(Entity attackableEntity) {
        this.attackableEntity = attackableEntity;
    }

    public CIA getCia() {
        return cia;
    }

    public void setCia(CIA cia) {
        this.cia = cia;
    }
}
