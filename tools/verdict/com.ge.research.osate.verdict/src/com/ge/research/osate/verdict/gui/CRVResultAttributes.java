package com.ge.research.osate.verdict.gui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Author: Soumya Talukder Date: Jul 18, 2019 */

// this class stores the contents of a "Property" element in the CRV .xml
public class CRVResultAttributes implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;
    private String property;
    private String source;
    private String answer;
    private List<CounterExampleAttributes> cntExample = new ArrayList<CounterExampleAttributes>();
    private String validTill = "";
    private BlameAssignmentInfo blm;

    public void setProperty(String str) {
        property = str;
    }

    public String getProperty() {
        return property;
    }

    public void setAnswer(String str) {
        answer = str;
    }

    public String getAnswer() {
        return answer;
    }

    public void setCntExample(List<CounterExampleAttributes> ce) {
        cntExample = ce;
    }

    public List<CounterExampleAttributes> getCntExample() {
        return cntExample;
    }

    public void setValidTill(String str) {
        validTill = str;
    }

    public String getValidTill() {
        return validTill;
    }

    public void setBlameAssignment(BlameAssignmentInfo info) {
        blm = info;
    }

    public BlameAssignmentInfo getBlameAssignment() {
        return blm;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSource() {
        return source;
    }
}
