package com.ge.verdict.attackdefensecollector;

import java.util.HashSet;
import java.util.Set;

/** Logging. Used to prevent the same warning message being shown multiple times. */
public class Logger {
    /** Log of messages already sent. */
    private static Set<String> messages = new HashSet<>();

    /**
     * Print a message. No prevention of duplicate messages.
     *
     * @param message
     */
    public static void println(String message) {
        System.out.println(message);
    }

    /** Print a blank line. */
    public static void println() {
        println("");
    }

    /**
     * Print an object using obj.toString().
     *
     * @param obj
     */
    public static void println(Object obj) {
        println(obj.toString());
    }

    /**
     * Print a message if it hasn't been printed before.
     *
     * @param message
     */
    private static void showMessage(String message) {
        if (!messages.contains(message)) {
            messages.add(message);
            println(message);
        }
    }

    /**
     * Print a warning message if it hasn't been printed before.
     *
     * @param message
     */
    public static void showWarning(String message) {
        showMessage("Warning: " + message);
    }
}
