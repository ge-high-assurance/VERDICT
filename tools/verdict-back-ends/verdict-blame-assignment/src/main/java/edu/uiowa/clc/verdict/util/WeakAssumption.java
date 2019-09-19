/* See LICENSE in project directory */
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
