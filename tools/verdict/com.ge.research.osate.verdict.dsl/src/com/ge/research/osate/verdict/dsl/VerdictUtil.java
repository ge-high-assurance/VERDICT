package com.ge.research.osate.verdict.dsl;

import com.ge.research.osate.verdict.dsl.verdict.CyberRel;
import com.ge.research.osate.verdict.dsl.verdict.CyberRelInputLogic;
import com.ge.research.osate.verdict.dsl.verdict.CyberRelOutputLogic;
import com.ge.research.osate.verdict.dsl.verdict.CyberReq;
import com.ge.research.osate.verdict.dsl.verdict.CyberReqConditionLogic;
import com.ge.research.osate.verdict.dsl.verdict.Event;
import com.ge.research.osate.verdict.dsl.verdict.SafetyRel;
import com.ge.research.osate.verdict.dsl.verdict.SafetyRelInputLogic;
import com.ge.research.osate.verdict.dsl.verdict.SafetyRelOutputLogic;
import com.ge.research.osate.verdict.dsl.verdict.SafetyReq;
import com.ge.research.osate.verdict.dsl.verdict.SafetyReqConditionLogic;
import com.ge.research.osate.verdict.dsl.verdict.Statement;
import com.ge.research.osate.verdict.dsl.verdict.Verdict;
import com.ge.research.osate.verdict.dsl.verdict.VerdictContractSubclause;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.emf.ecore.EObject;
import org.osate.aadl2.AnnexSubclause;
import org.osate.aadl2.Classifier;
import org.osate.aadl2.ComponentType;
import org.osate.aadl2.DataPort;
import org.osate.aadl2.DefaultAnnexSubclause;
import org.osate.aadl2.DirectionType;
import org.osate.aadl2.EventDataPort;
import org.osate.aadl2.PublicPackageSection;
import org.osate.aadl2.SystemType;

/**
 * Utilities for validating cyber properties.
 */
public class VerdictUtil {
    /**
     * Get the root AST "Verdict" node from an annex.
     *
     * @param obj some object that might be a Verdict
     * @return the Verdict
     */
    public static Verdict getVerdict(Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof Verdict) {
            return (Verdict) obj;
        } else if (obj instanceof VerdictContractSubclause) {
            return ((VerdictContractSubclause) obj).getContract();
        } else if (obj instanceof DefaultAnnexSubclause) {
            return getVerdict(((DefaultAnnexSubclause) obj).getParsedAnnexSubclause());
        } else {
            throw new IllegalArgumentException("bad verdict: " + obj.getClass().getName());
        }
    }

    /**
     * Finds all Events for the system.
     *
     * Automatically detects if the ports should be input or output based
     * on the context (if possible).
     *
     * Requires: port must be an LPort or inside a CyberRel/CyberReq
     *
     * @param port the AST object from which to search up the tree
     * @return the ports info (see AvailablePortsInfo)
     */
    public static Set<String> getAvailableEventIds(ComponentType systemType) {
        Set<String> eventIds = new HashSet<String>();
        if (systemType != null) {
            for (AnnexSubclause annex : systemType.getOwnedAnnexSubclauses()) {
                if ("verdict".equals(annex.getName())) {
                    Verdict subclause = VerdictUtil.getVerdict(annex);

                    // Get all cyber req IDs
                    for (Statement statement : subclause.getElements()) {
                        if (statement instanceof Event) {
                            eventIds.add(statement.getId());
                        }
                    }
                }
            }
        }
        return eventIds;
    }

    public static ComponentType getHostingSystemType(EObject element) {
        ComponentType hostingSysType = null;
        EObject container = element;

        while (!(container instanceof ComponentType)) {
            if (container == null) {
                break;
            }
            container = container.eContainer();
        }
        if (container instanceof ComponentType) {
            hostingSysType = (ComponentType) container;
        }
        return hostingSysType;
    }

    /**
     * Finds all input/output ports for the system enclosing an LPort.
     *
     * Automatically detects if the ports should be input or output based
     * on the context (if possible).
     *
     * Requires: port must be an LPort or inside a CyberRel/CyberReq
     *
     * @param port the AST object from which to search up the tree
     * @return the ports info (see AvailablePortsInfo)
     */
    public static AvailablePortsInfo getAvailablePorts(EObject port, boolean allowSkipInput) {
        return getAvailablePorts(port, allowSkipInput, null);
    }

    /**
     * Finds all input/output ports for the system enclosing an LPort.
     *
     * Automatically detects if the ports should be input or output based
     * on the context (if possible).
     *
     * Requires: port must be an LPort or inside a CyberRel/CyberReq
     *
     * @param port the AST object from which to search up the tree
     * @return the ports info (see AvailablePortsInfo)
     */
    public static AvailablePortsInfo getAvailableSystemPorts(EObject port, boolean allowSkipInput) {
        return getAvailablePorts(port, allowSkipInput, null);
    }

    /**
     * Finds all input/output ports for the system enclosing an LPort.
     *
     * Automatically detects if the ports should be input or output based
     * on the context (if possible).
     *
     * Requires: port must be an LPort or inside a CyberRel/CyberReq
     *
     * @param port the AST object from which to search up the tree
     * @param allowSkipInput used in the proposal provider because model
     *        is not necessarily where we expect it to be
     * @return the ports info (see AvailablePortsInfo)
     */
    public static AvailablePortsInfo getAvailablePorts(
            EObject port, boolean allowSkipInput, DirectionType specifiedDir) {
        List<String> ports = new ArrayList<>();
        SystemType system = null;
        DirectionType dir = null;

        // Determine direction

        EObject container = port;
        while (!(container instanceof CyberRelInputLogic
                || container instanceof CyberRelOutputLogic
                || container instanceof CyberReqConditionLogic
                || container instanceof CyberRel
                || container instanceof CyberReq
                || container instanceof SafetyRelInputLogic
                || container instanceof SafetyRelOutputLogic
                || container instanceof SafetyReqConditionLogic
                || container instanceof SafetyRel
                || container instanceof SafetyReq
                || container instanceof SystemType
                || container instanceof PublicPackageSection)) {
            if (container == null) {
                break;
            }
            container = container.eContainer();
        }

        if (container instanceof CyberRelInputLogic) {
            dir = DirectionType.IN;
        } else if (container instanceof CyberRelOutputLogic) {
            dir = DirectionType.OUT;
        } else if (container instanceof CyberReqConditionLogic) {
            dir = DirectionType.OUT;
        } else if (container instanceof SafetyRelInputLogic) {
            dir = DirectionType.IN;
        } else if (container instanceof SafetyRelOutputLogic) {
            dir = DirectionType.OUT;
        } else if (container instanceof SafetyReqConditionLogic) {
            dir = DirectionType.OUT;
        } else {
            // If allowSkipInput is true, then we will simply collect both input and output
            if (!allowSkipInput) {
                throw new RuntimeException();
            }
        }

        // Determine if we are in a cyber relation or requirement

        while (!(container instanceof CyberRel
                || container instanceof CyberReq
                || container instanceof SafetyReq
                || container instanceof SafetyRel
                || container instanceof Event
                || container instanceof SystemType
                || container instanceof PublicPackageSection)) {
            if (container == null) {
                break;
            }
            container = container.eContainer();
        }
        boolean isCyberReq;
        if (container instanceof CyberReq) {
            isCyberReq = true;
        } else if (container instanceof CyberRel) {
            isCyberReq = false;
        } else if (container instanceof SafetyReq) {
            isCyberReq = false;
        } else if (container instanceof SafetyRel) {
            isCyberReq = false;
        } else if (container instanceof Event) {
            isCyberReq = false;
        } else {
            if (specifiedDir == null) {
                throw new RuntimeException();
            } else {
                dir = specifiedDir;
                isCyberReq = false;
            }
        }

        // Find the enclosing system

        while (!(container instanceof SystemType || container instanceof PublicPackageSection)) {
            if (container == null) {
                break;
            }
            container = container.eContainer();
        }
        if (container instanceof SystemType) {
            system = (SystemType) container;

            while (!(container instanceof SystemType || container instanceof PublicPackageSection)) {
                container = container.eContainer();
            }

            if (container instanceof SystemType) {
                // Find all data(event data) ports
                for (DataPort dataPort : ((SystemType) container).getOwnedDataPorts()) {
                    if ((dir != null && dataPort.getDirection().equals(dir))
                            || (dir == null
                                    && (dataPort.getDirection().equals(DirectionType.IN)
                                            || dataPort.getDirection().equals(DirectionType.OUT)))) {
                        ports.add(dataPort.getName());
                    }
                }
                for (EventDataPort eventDataPort : ((SystemType) container).getOwnedEventDataPorts()) {
                    if ((dir != null && eventDataPort.getDirection().equals(dir))
                            || (dir == null
                                    && (eventDataPort.getDirection().equals(DirectionType.IN)
                                            || eventDataPort.getDirection().equals(DirectionType.OUT)))) {
                        ports.add(eventDataPort.getName());
                    }
                }
            }
        }

        return new AvailablePortsInfo(ports, system, dir == null || dir.equals(DirectionType.IN), isCyberReq);
    }

    /**
     * Result of calling getAvailablePorts(). See field documentation.
     */
    public static class AvailablePortsInfo {
        /**
         * Never null, might be empty.
         *
         * Contains all ports present in the enclosing system of the correct
         * direction if that system was found. If a direction was not determined,
         * then contains ports of both directions.
         *
         * Note: in-out ports are not currently supported.
         */
        public List<String> availablePorts;

        /**
         * The enclosing system, or null if it could not be found.
         */
        public SystemType system;

        /**
         * True if this port should be an input port based on its context.
         * Also true if the system could not be found.
         */
        public boolean isInput;

        /**
         * True if the enclosing property is a cyber requirement,
         * false if a cyber relation.
         */
        public boolean isCyberReq;

        public AvailablePortsInfo(List<String> availablePorts, SystemType system, boolean isInput, boolean isCyberReq) {
            super();
            this.availablePorts = availablePorts;
            this.system = system;
            this.isInput = isInput;
            this.isCyberReq = isCyberReq;
        }
    }

    /**
     * Get the (linked) set of all cyber requirements in the AADL AST of which obj is part.
     *
     * @param obj
     * @return
     */
    public static Set<String> getAllReqs(EObject obj) {
        Set<String> reqs = new LinkedHashSet<>();

        // Find public package section
        EObject container = obj;
        while (container != null && !(container instanceof PublicPackageSection)) {
            container = container.eContainer();
        }

        PublicPackageSection pack = (PublicPackageSection) container;
        if (pack != null && pack.getOwnedClassifiers() != null) {
            // Find all systems
            for (Classifier cls : pack.getOwnedClassifiers()) {
                if (cls instanceof SystemType) {
                    SystemType system = (SystemType) cls;
                    // Get all verdict annexes for this system
                    for (AnnexSubclause annex : system.getOwnedAnnexSubclauses()) {
                        if ("verdict".equals(annex.getName())) {
                            Verdict subclause = VerdictUtil.getVerdict(annex);

                            // Get all cyber req IDs
                            for (Statement statement : subclause.getElements()) {
                                if (statement instanceof CyberReq) {
                                    reqs.add(statement.getId());
                                } else if (statement instanceof SafetyReq) {
                                    reqs.add(statement.getId());
                                }
                            }
                        }
                    }
                }
            }
        }

        return reqs;
    }

    /**
     * Get the (linked) set of all cyber requirements in the AADL AST of which obj is part.
     *
     * @param obj
     * @return
     */
    public static Set<String> getEvents(EObject obj) {
        Set<String> cyberReqs = new LinkedHashSet<>();

        // Find public package section
        EObject container = obj;
        while (container != null && !(container instanceof PublicPackageSection)) {
            container = container.eContainer();
        }

        PublicPackageSection pack = (PublicPackageSection) container;
        if (pack != null && pack.getOwnedClassifiers() != null) {
            // Find all systems
            for (Classifier cls : pack.getOwnedClassifiers()) {
                if (cls instanceof SystemType) {
                    SystemType system = (SystemType) cls;
                    // Get all verdict annexes for this system
                    for (AnnexSubclause annex : system.getOwnedAnnexSubclauses()) {
                        if ("verdict".equals(annex.getName())) {
                            Verdict subclause = VerdictUtil.getVerdict(annex);

                            // Get all cyber req IDs
                            for (Statement statement : subclause.getElements()) {
                                if (statement instanceof CyberReq) {
                                    cyberReqs.add(statement.getId());
                                }
                            }
                        }
                    }
                }
            }
        }

        return cyberReqs;
    }
}
