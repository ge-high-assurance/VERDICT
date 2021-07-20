package com.ge.verdict.mbaas.synthesis.model;

public class Attack {
    private CIA cia;
    private String name;
    private String qualifedName;

    public Attack(String qualifiedName, String name, CIA cia) {
        this.qualifedName = qualifiedName;
        this.name = name;
        this.cia = cia;
    }

    public Attack(String name, CIA cia) {
        this.name = name;
        this.cia = cia;
    }

    public String getQualifedName() {
        return qualifedName;
    }

    public void setQualifedName(String qualifedName) {
        this.qualifedName = qualifedName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CIA getCia() {
        return cia;
    }

    public void setCia(CIA cia) {
        this.cia = cia;
    }
}
