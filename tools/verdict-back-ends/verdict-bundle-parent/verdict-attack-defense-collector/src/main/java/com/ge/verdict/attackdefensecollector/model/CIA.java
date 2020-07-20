package com.ge.verdict.attackdefensecollector.model;

import java.util.Optional;

/** Confidentiality, Integrity, or Availability (the CIA triad). */
public enum CIA {
    C("Confidentiality"),
    I("Integrity"),
    A("Availability");

    private String fullName;

    private CIA(String fullName) {
        this.fullName = fullName;
    }

    /** @return the full name (e.g. "Integrity" instead of just "I"). */
    public String getFullName() {
        return fullName;
    }

    /**
     * Constructs a CIA from a string. Ignores case and accepts short form (e.g. "I") and long form
     * (e.g. "Integrity").
     *
     * @param str the string to parse
     * @return the parsed CIA, or empty
     */
    public static Optional<CIA> fromStringOpt(String str) {
        switch (str.toLowerCase()) {
            case "c":
            case "confidentiality":
                return Optional.of(C);
            case "i":
            case "integrity":
                return Optional.of(I);
            case "a":
            case "availability":
                return Optional.of(A);
            default:
                return Optional.empty();
        }
    }

    /**
     * Constructs a CIA from a string. Same as fromStringOpt(), except throws an exception instead
     * of returning an empty option.
     *
     * @param str the string to parse
     * @return the parsed CIA
     */
    public static CIA fromString(String str) {
        Optional<CIA> opt = fromStringOpt(str);
        if (opt.isPresent()) {
            return opt.get();
        } else {
            throw new RuntimeException("Invalid CIA string: " + str);
        }
    }

    /**
     * Constructs a CIA from the first valid CIA string in a sequence of strings. Throws an
     * exception if none of the strings is a valid CIA string.
     *
     * @param strs the possible strings to parse
     * @return the parsed CIA
     */
    public static CIA fromStrings(String... strs) {
        // Check all
        for (String str : strs) {
            Optional<CIA> opt = fromStringOpt(str);
            if (opt.isPresent()) {
                // Return first one found
                return opt.get();
            }
        }

        // None found
        StringBuilder msg = new StringBuilder();
        msg.append("No valid CIA found in strings: ");
        for (String str : strs) {
            msg.append(str);
            msg.append(",");
        }
        throw new RuntimeException(msg.toString());
    }
}
