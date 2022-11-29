package com.ge.research.osate.verdict.gui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Author: Soumya Talukder Date: Jul 18, 2019 */
// this class stores the contents of a "Requirement" element in MBAS .xml
public class RequirementAttributes implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;
    private String requirement;
    private String calcLikelihood;
    private String accptLikelihood;
    private List<PathAttributes> minimalPaths = new ArrayList<PathAttributes>();
    private Boolean success;

    public void setRequirement(String str) {
        requirement = str;
    }

    public String getRequirement() {
        return requirement;
    }

    public void setLikelihood(String str1, String str2) {
        calcLikelihood = str1;
        accptLikelihood = str2;

        if (Double.parseDouble(calcLikelihood) > Double.parseDouble(accptLikelihood)) {
            success = false;
        } else {
            success = true;
        }
    }

    public String getCalcLikelihood() {
        return calcLikelihood;
    }

    public String getAccptLikelihood() {
        return accptLikelihood;
    }

    public void setPaths(List<PathAttributes> paths) {
        minimalPaths = paths;
    }

    public List<PathAttributes> getPaths() {
        return minimalPaths;
    }

    public Boolean hasSucceeded() {
        return success;
    }
}
