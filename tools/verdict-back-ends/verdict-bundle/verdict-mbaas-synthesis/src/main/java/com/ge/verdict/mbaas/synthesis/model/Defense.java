package com.ge.verdict.mbaas.synthesis.model;

public class Defense {
    private String qualifedName;
    private String name;
    private CIA cia;

    public Defense(String qualifiedName, String name, CIA cia) {
        this.qualifedName = qualifiedName;
        this.name = name;
        this.cia = cia;
    }

    public Defense(String name, CIA cia) {
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
