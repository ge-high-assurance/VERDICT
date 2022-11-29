package com.ge.research.osate.verdict.gui;

import java.util.Collections;
import java.util.List;

public class MBASSafetyResult {
    private String reqName, defenseType, computedLikelihood, acceptableLikelihood;
    private List<CutsetResult> cutsets;
    private boolean successful;

    public static class CutsetResult {
        public static class Event {
            private String component, eventName;

            public Event(String component, String eventName) {
                this.component = component;
                this.eventName = eventName;
            }

            public String getComponent() {
                return component;
            }

            public String getEventName() {
                return eventName;
            }
        }

        private String likelihood;
        private List<Event> events;

        public CutsetResult(String likelihood, List<Event> events) {
            this.likelihood = likelihood;
            this.events = Collections.unmodifiableList(events);
        }

        public String getLikelihood() {
            return likelihood;
        }

        public List<Event> getEvents() {
            return events;
        }
    }

    public MBASSafetyResult(
            String reqName,
            String defenseType,
            String computedLikelihood,
            String acceptableLikelihood,
            List<CutsetResult> cutsets) {
        this.reqName = reqName;
        this.defenseType = defenseType;
        this.computedLikelihood = computedLikelihood;
        this.acceptableLikelihood = acceptableLikelihood;
        this.cutsets = Collections.unmodifiableList(cutsets);

        try {
            double acceptable = Double.parseDouble(getAcceptableLikelihood());
            double computed = Double.parseDouble(getComputedLikelihood());
            successful = computed <= acceptable;
        } catch (NumberFormatException e) {
            System.out.println(
                    "Error parsing probablities: "
                            + getAcceptableLikelihood()
                            + ", "
                            + getComputedLikelihood());
            successful = false;
        }
    }

    public String getReqName() {
        return reqName;
    }

    public String getDefenseType() {
        return defenseType;
    }

    public String getComputedLikelihood() {
        return computedLikelihood;
    }

    public String getAcceptableLikelihood() {
        return acceptableLikelihood;
    }

    public List<CutsetResult> getCutsets() {
        return cutsets;
    }

    public boolean isSuccessful() {
        return successful;
    }
}
