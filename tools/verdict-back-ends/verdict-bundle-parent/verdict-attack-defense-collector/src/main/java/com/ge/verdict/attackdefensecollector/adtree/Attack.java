package com.ge.verdict.attackdefensecollector.adtree;

import com.ge.verdict.attackdefensecollector.IndentedStringBuilder;
import com.ge.verdict.attackdefensecollector.NameResolver;
import com.ge.verdict.attackdefensecollector.Prob;
import com.ge.verdict.attackdefensecollector.model.CIA;
import com.ge.verdict.attackdefensecollector.model.SystemModel;
import java.util.Locale;
import java.util.Objects;

/** An attack on a system, a fundamental unit of the attack-defense tree. */
public class Attack extends ADTree {
    /** The system to which the attack applies. */
    private NameResolver<SystemModel> system;
    /** The name of the attack. */
    private String attackName;
    /** The description of the attack. */
    private String attackDescription;
    /** The probability of attack success. */
    private Prob prob;
    /** The CIA concern affected by the attack. */
    private CIA cia;

    /**
     * Construct an attack.
     *
     * @param system the system to which the attack applies
     * @param attackName the name of the attack
     * @param attackDescription the description of the attack
     * @param prob the probability of attack success
     * @param cia the CIA concern affected by the attack
     */
    public Attack(
            NameResolver<SystemModel> system,
            String attackName,
            String attackDescription,
            Prob prob,
            CIA cia) {
        this.system = system;
        this.attackName = attackName;
        this.attackDescription = attackDescription;
        this.prob = prob;
        this.cia = cia;
    }

    /** @return the name of the attack */
    public String getName() {
        return attackName;
    }

    /** @return the system to which the attack applies */
    public SystemModel getSystem() {
        return system.get();
    }

    /** @return the description of the attack */
    public String getDescription() {
        return attackDescription;
    }

    /** @return the CIA concern of the attack */
    public CIA getCia() {
        return cia;
    }

    @Override
    public ADTree crush() {
        // Fundamental, so no crushing to do
        return this;
    }

    @Override
    public Prob compute() {
        return prob;
    }

    @Override
    public void prettyPrint(IndentedStringBuilder builder) {
        // systemName:cia:attackName
        builder.append(
                String.format(Locale.US, "%s:%s:%s", system.getName(), cia.toString(), attackName));
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Attack) {
            Attack otherAttack = (Attack) other;
            return otherAttack.system.getName().equals(system.getName())
                    && otherAttack.attackName.equals(attackName)
                    && otherAttack.attackDescription.equals(attackDescription)
                    && otherAttack.prob.equals(prob)
                    && ((otherAttack.cia == null && cia == null)
                            || otherAttack.cia.equals(cia)); // can be bleached
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(system.getName(), attackName, attackDescription, prob, cia);
    }
}
