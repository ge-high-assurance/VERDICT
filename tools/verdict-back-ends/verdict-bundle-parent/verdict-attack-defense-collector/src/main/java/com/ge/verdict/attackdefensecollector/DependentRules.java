package com.ge.verdict.attackdefensecollector;

import com.ge.verdict.attackdefensecollector.adtree.ADOr;
import com.ge.verdict.attackdefensecollector.adtree.ADTree;
import com.ge.verdict.attackdefensecollector.adtree.DefenseCondition;
import com.ge.verdict.attackdefensecollector.model.Attackable;
import com.ge.verdict.attackdefensecollector.model.ConnectionModel;
import com.ge.verdict.attackdefensecollector.model.SystemModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * These are hardcoded rules that replicate parts of the STEM rules to encode the possibility of an
 * attack being dependent upon the presence of a defense. The hope is to replace these hardcoded
 * rules with some kind of output from STEM in the future.
 */
public class DependentRules {
    public static Optional<ADTree> getComponentDependence(
            SystemModel component, String attackName) {
        List<ADTree> paths = new ArrayList<>();
        switch (attackName) {
            case "CAPEC-21":
                for (ConnectionModel connection : component.getIncomingConnections()) {
                    if ("Untrusted".equals(connection.getAttributes().get("connectionType"))) {
                        // Vul-CAPEC-21-1
                        paths.add(
                                new DefenseCondition(
                                        connection.getAttackable(), "deviceAuthentication", 1));
                    }
                }
                for (ConnectionModel connection : component.getOutgoingConnections()) {
                    if ("Untrusted".equals(connection.getAttributes().get("connectionType"))) {
                        // Vul-CAPEC-21-2
                        paths.add(
                                new DefenseCondition(
                                        connection.getAttackable(), "deviceAuthentication", 1));
                    }
                }
                return mkRet(component.getAttackable(), attackName, paths);
            case "CAPEC-112":
                for (ConnectionModel connection : component.getIncomingConnections()) {
                    // Vul-CAPEC-112-1, Vul-CAPEC-112-3, Vul-CAPEC-112-5
                    paths.add(
                            new DefenseCondition(
                                    connection.getAttackable(), "deviceAuthentication", 1));
                    // Vul-CAPEC-112-2, Vul-CAPEC-112-4, Vul-CAPEC-112-6
                    paths.add(
                            new DefenseCondition(
                                    connection.getAttackable(), "encryptedTransmission", 1));
                }
                return mkRet(component.getAttackable(), attackName, paths);
            case "CAPEC-114":
                for (ConnectionModel connection : component.getIncomingConnections()) {
                    // Vul-CAPEC-114-1, Vul-CAPEC-114-2, Vul-CAPEC-114-3
                    paths.add(
                            new DefenseCondition(
                                    connection.getAttackable(), "deviceAuthentication", 1));
                }
                return mkRet(component.getAttackable(), attackName, paths);
            case "CAPEC-115":
                for (ConnectionModel connection : component.getIncomingConnections()) {
                    // Vul-CAPEC-115-1, Vul-CAPEC-115-2, Vul-CAPEC-115-3
                    paths.add(
                            new DefenseCondition(
                                    connection.getAttackable(), "deviceAuthentication", 1));
                }
                return mkRet(component.getAttackable(), attackName, paths);
            case "CAPEC-390":
                paths.add(
                        new DefenseCondition(
                                component.getAttackable(), "physicalAccessControl", 1));
                return mkRet(component.getAttackable(), attackName, paths);
            default:
                return Optional.empty();
        }
    }

    public static Optional<ADTree> getConnectionDependence(
            ConnectionModel connection, String attackName) {
        List<ADTree> paths = new ArrayList<>();
        // There are currently no such rules
        switch (attackName) {
            default:
                return Optional.empty();
        }
    }

    private static Optional<ADTree> mkRet(
            Attackable attackable, String attackName, List<ADTree> paths) {
        if (paths.isEmpty()) {
            System.err.println(
                    "Strange. Did not find a possible defense dependency for attack "
                            + attackName
                            + " on component/connection "
                            + attackable.getParentName());
            return Optional.empty();
        } else {
            return Optional.of(new ADOr(paths));
        }
    }
}
