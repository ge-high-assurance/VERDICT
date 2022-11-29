package com.ge.research.osate.verdict.gui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Author: Soumya Talukder Date: Jul 18, 2019 */

// this class stores a information related to a counter example as read from CRV .xml
public class CENode implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;
    private String varName;
    private String varType;
    private String varClass;
    private List<String> varValue = new ArrayList<String>();
    private List<String> varInst = new ArrayList<String>();

    public void setVarName(String str) {
        varName = str;
    }

    public String getVarName() {
        return varName;
    }

    public void setVarType(String str) {
        varType = str;
    }

    public String getVarType() {
        return varType;
    }

    public void setVarClass(String str) {
        varClass = str;
    }

    public String getVarClass() {
        return varClass;
    }

    public void setVarValue(List<String> str) {
        varValue = str;
    }

    public List<String> getVarValue() {
        return varValue;
    }

    public void setVarInst(List<String> str) {
        varInst = str;
    }

    public List<String> getVarInst() {
        return varInst;
    }
}
