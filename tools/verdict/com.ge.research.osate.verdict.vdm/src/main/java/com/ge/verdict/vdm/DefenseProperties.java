package com.ge.verdict.vdm;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DefenseProperties {
    // TODO perhaps further break down by connection vs component
    private static final String[] props = {
        "encryptedTransmission",
        "deviceAuthentication",
        "sessionAuthenticity",
        "antiJamming",
        "auditMessageResponses",
        "dosProtection",
        "encryptedStorage",
        "failSafe",
        "heterogeneity",
        "inputValidation",
        "logging",
        "memoryProtection",
        "physicalAccessControl",
        "remoteAttestation",
        "removeIdentifyingInformation",
        "resourceAvailability",
        "resourceIsolation",
        "secureBoot",
        "staticCodeAnalysis",
        "strongCryptoAlgorithms",
        "supplyChainSecurity",
        "systemAccessControl",
        "tamperProtection",
        "userAuthentication",
        "zeroize",
    };

    /**
     * This is a hard-coded list of defense properties. It is kind of disgusting, but it is the best
     * solution for now. In the future, STEM etc. should make the list accessible to Java through
     * some kind of API.
     *
     * <p>This list does need to be updated if CASE_Consolidated_Properties.aadl changes!
     */
    public static final List<String> MBAA_DEFENSE_PROPERTIES_LIST =
            Collections.unmodifiableList(Arrays.asList(props));

    public static final Set<String> MBAA_DEFENSE_PROPERTIES_SET =
            Collections.unmodifiableSet(new HashSet<>(MBAA_DEFENSE_PROPERTIES_LIST));
}
