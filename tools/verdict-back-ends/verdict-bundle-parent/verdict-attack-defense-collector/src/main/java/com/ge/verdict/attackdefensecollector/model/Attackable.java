package com.ge.verdict.attackdefensecollector.model;

import com.ge.verdict.attackdefensecollector.Pair;
import com.ge.verdict.attackdefensecollector.adtree.Attack;
import com.ge.verdict.attackdefensecollector.adtree.Defense;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Attackable {
    /* either system or connection is populated */
    private Optional<SystemModel> system;
    private Optional<ConnectionModel> connection;

    /** Attacks that apply to this attackable. */
    private List<Attack> attacks;
    /** Defenses that apply to attacks that apply to this attackable. */
    private List<Defense> defenses;

    /** Map from attack name/CIA pairs to attacks for this attackable. */
    private Map<Pair<String, CIA>, Attack> attackMap;
    /** Map from attack name/CIA pairs to defenses for this attackable. */
    private Map<Pair<String, CIA>, Defense> defenseMap;

    public Attackable(SystemModel system) {
        this.system = Optional.of(system);
        connection = Optional.empty();
        attacks = new ArrayList<>();
        defenses = new ArrayList<>();
        attackMap = new LinkedHashMap<>();
        defenseMap = new LinkedHashMap<>();
    }

    public Attackable(ConnectionModel connection) {
        system = Optional.empty();
        this.connection = Optional.of(connection);
        attacks = new ArrayList<>();
        defenses = new ArrayList<>();
        attackMap = new LinkedHashMap<>();
        defenseMap = new LinkedHashMap<>();
    }

    public boolean isSystem() {
        return system.isPresent();
    }

    public SystemModel getSystem() {
        return system.get();
    }

    public boolean isConnection() {
        return connection.isPresent();
    }

    public ConnectionModel getConnection() {
        return connection.get();
    }

    public String getParentName() {
        return isSystem() ? getSystem().getName() : getConnection().getName();
    }

    public Map<String, String> getParentAttributes() {
        return isSystem() ? getSystem().getAttributes() : getConnection().getAttributes();
    }

    public List<Attack> getAttacks() {
        return attacks;
    }

    /**
     * Adds an attack to the attackable.
     *
     * @param attack
     */
    public void addAttack(Attack attack) {
        attacks.add(attack);
        attackMap.put(new Pair<>(attack.getName(), attack.getCia()), attack);
    }

    /**
     * Adds a defense to the attackable. Expects that the defense refers to an attack which has been
     * (or will be) added to the attackable.
     *
     * @param defense
     */
    public void addDefense(Defense defense) {
        defenses.add(defense);
        defenseMap.put(
                new Pair<>(defense.getAttack().getName(), defense.getAttack().getCia()), defense);
    }

    /**
     * Gets the previously-added attack with the specified name and CIA, or the empty optional if no
     * attack with the specified name and CIA has been added.
     *
     * @param name the name of the attack
     * @param cia the CIA of the attack
     * @return the attack, or empty
     */
    public Attack getAttackByNameAndCia(String name, CIA cia) {
        return attackMap.get(new Pair<>(name, cia));
    }

    public List<Defense> getDefenses() {
        return defenses;
    }

    /**
     * Gets the previously-added defense corresponding to the attack with the specified name and
     * CIA, or the empty optional if no such defense has been added.
     *
     * @param attackName the name of the attack to which the defense corresponds
     * @param cia the CIA of the attack to which the defense corresponds
     * @return the defense, or empty
     */
    public Defense getDefenseByAttackAndCia(String attackName, CIA cia) {
        return defenseMap.get(new Pair<>(attackName, cia));
    }
}
