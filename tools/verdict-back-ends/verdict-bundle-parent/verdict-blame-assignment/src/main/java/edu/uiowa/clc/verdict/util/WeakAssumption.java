/* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.

   @author: M. Fareed Arif
*/

package edu.uiowa.clc.verdict.util;

public class WeakAssumption {

    String wId;
    boolean status;

    public WeakAssumption(String wId, boolean status) {
        this.wId = wId;
        this.status = status;
    }

    public String getwId() {
        return wId;
    }

    public void setwId(String wId) {
        this.wId = wId;
    }

    public boolean isTrue() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public boolean getStatus() {
        return this.status;
    }
}
