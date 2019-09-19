/* See LICENSE in project directory */
package edu.uiowa.clc.verdict.util;

// import org.slf4j.Logger;

public class LOGGY {
    public static void info(final String message) {
        System.out.println(message);
        //        logger.info(message);
    }

    public static void warn(final String message) {
        System.err.println(message);
        //        logger.warn(message);
    }
}
