package com.ge.verdict.mbaas.synthesis.adtree;

import com.ge.verdict.mbaas.synthesis.model.Entity;
import verdict.vdm.vdm_model.CIA;

public class DefenseNode extends DefenseTree {
    private String defenseName;
    private Entity defendableEntity;
    private CIA cia;

    public DefenseNode(String name, Entity entity, CIA cia) {
        this.setDefenseName(name);
        this.setDefendableEntity(entity);
        this.setCia(cia);
    }

    public CIA getCia() {
        return cia;
    }

    public void setCia(CIA cia) {
        this.cia = cia;
    }

    public String getDefenseName() {
        return defenseName;
    }

    public void setDefenseName(String defenseName) {
        this.defenseName = defenseName;
    }

    public Entity getDefendableEntity() {
        return defendableEntity;
    }

    public void setDefendableEntity(Entity defendableEntity) {
        this.defendableEntity = defendableEntity;
    }
}
