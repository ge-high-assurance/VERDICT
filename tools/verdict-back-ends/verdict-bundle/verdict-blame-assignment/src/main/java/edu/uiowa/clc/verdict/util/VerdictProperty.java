/* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.

   @author: M. Fareed Arif
*/

package edu.uiowa.clc.verdict.util;

import java.util.Vector;

public class VerdictProperty {

    private String pId;
    private String source;
    private boolean sat_status;
    private String time;

    private Vector<WeakAssumption> wk_assumptions;

    public VerdictProperty() {
        wk_assumptions = new Vector<WeakAssumption>();
        //        false_assumptions = new Vector<WeakAssumption>();
    }

    public String getId() {
        return pId;
    }

    public void setId(String pId) {
        this.pId = pId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isSAT() {
        return sat_status;
    }

    public void setStatus(boolean sat_status) {
        this.sat_status = sat_status;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Vector<WeakAssumption> getAllWeakAssumptions() {
        return this.wk_assumptions;
    }

    public Vector<String> getTrueWeakAssumptions() {

        Vector<String> wks = new Vector<String>();

        for (WeakAssumption wk : this.wk_assumptions) {
            if (wk.isTrue()) {
                String w_value = wk.getwId();
                wks.add(w_value);
            }
        }

        return wks;
    }

    public Vector<String> getFalseWeakAssumptions() {

        Vector<String> wks = new Vector<String>();

        for (WeakAssumption wk : this.wk_assumptions) {
            if (!wk.isTrue()) {
                String w_value = wk.getwId();
                wks.add(w_value);
            }
        }

        return wks;
    }

    public boolean isSatisfied(String wk_Id) {

        boolean sat_status = false;

        if (this.wk_assumptions.contains(wk_Id)) {
            for (WeakAssumption wk : this.wk_assumptions) {
                if (wk.isTrue()) {
                    return true;
                }
            }
        }
        return sat_status;
    }

    public void addAssumption(String wId, boolean status) {

        WeakAssumption wk = new WeakAssumption(wId, status);
        this.wk_assumptions.add(wk);
    }
}
