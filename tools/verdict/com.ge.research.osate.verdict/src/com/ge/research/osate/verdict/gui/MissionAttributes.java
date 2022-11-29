package com.ge.research.osate.verdict.gui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Author: Soumya Talukder Date: Jul 18, 2019 */

// this class stores the attributes of a "Mission" in MBAS .xml
public class MissionAttributes implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;
    private String mission;
    private boolean missionSuccess;
    private List<RequirementAttributes> requirements = new ArrayList<RequirementAttributes>();
    private List<MBASSummaryRow> tableContents = new ArrayList<MBASSummaryRow>();

    public void setMission(String str) {
        mission = str;
    }

    public String getMission() {
        return mission;
    }

    public String getMissionStatus() {
        return missionSuccess ? "Succeeded" : "Failed";
    }

    // determines whether mission has succeeded of failed
    public void setRequirements(List<RequirementAttributes> rq) {
        missionSuccess = true;
        requirements = rq;
        for (int i = 0; i < requirements.size(); i++) {
            if (!requirements.get(i).hasSucceeded()) {
                missionSuccess = false;
                break;
            }
        }
    }

    public void updateSuccessWithSafety(List<MBASSafetyResult> safetyResults) {
        if (missionSuccess) {
            for (MBASSafetyResult safetyResult : safetyResults) {
                if (!safetyResult.isSuccessful()) {
                    missionSuccess = false;
                    break;
                }
            }
        }
    }

    public List<RequirementAttributes> getRequirements() {
        return requirements;
    }

    public void setTableContents(List<MBASSummaryRow> list) {
        tableContents = list;
    }

    public List<MBASSummaryRow> getTableContents() {
        return tableContents;
    }
}
