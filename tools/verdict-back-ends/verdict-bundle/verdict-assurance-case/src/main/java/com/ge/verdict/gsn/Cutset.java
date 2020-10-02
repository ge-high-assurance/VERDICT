package com.ge.verdict.gsn;

/**
 * @author Saswata Paul
 *     <p>Used to store details of a Soteria output cutset
 */
public class Cutset {

    protected String likelihood;
    protected String attack;
    protected String component;
    protected String defenses;

    /** @return the likelihood */
    protected String getLikelihood() {
        return likelihood;
    }
    /** @param likelihood the likelihood to set */
    protected void setLikelihood(String likelihood) {
        this.likelihood = likelihood;
    }
    /** @return the attack */
    protected String getAttack() {
        return attack;
    }
    /** @param attack the attack to set */
    protected void setAttack(String attack) {
        this.attack = attack;
    }
    /** @return the component */
    protected String getComponent() {
        return component;
    }
    /** @param component the component to set */
    protected void setComponent(String component) {
        this.component = component;
    }
    /** @return the defenses */
    protected String getDefenses() {
        return defenses;
    }
    /** @param defenses the defenses to set */
    protected void setDefenses(String defenses) {
        this.defenses = defenses;
    }
}
