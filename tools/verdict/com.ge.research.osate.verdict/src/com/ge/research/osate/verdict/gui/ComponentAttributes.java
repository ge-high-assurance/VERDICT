package com.ge.research.osate.verdict.gui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * Author: Soumya Talukder
 * Date: Jul 18, 2019
 *
 */

// this class stores the attributes of a component extracted from MBAS .xml
public class ComponentAttributes implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;
    private String component;
    private List<String> capecs = new ArrayList<String>();
    private List<String> defenses = new ArrayList<String>();
    private List<String> description = new ArrayList<String>();

    public void setComponent(String str) {
        component = str;
    }

    public String getComponent() {
        return component;
    }

    public void addCapec(String str) {
        capecs.add(str);
    }

    public void addDefense(String str) {
        defenses.add(str);
    }

    public void addDescription(String str) {
        description.add(str);
    }

    public List<String> getCapecs() {
        return capecs;
    }

    public List<String> getDefenses() {
        return defenses;
    }

    public List<String> getDescriptions() {
        return description;
    }

    public void setDescriptions(List<String> list) {
        description = list;
    }
}
