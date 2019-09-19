/* See LICENSE in project directory */
package com.ge.verdict.stem;

import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Runs Verdict STEM on a project. */
public class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    /**
     * Runs Verdict STEM on the given project.
     *
     * @param args Command line arguments with path to project
     */
    public static void main(String[] args) {

        // Check that we have one argument
        if (args.length == 1) {

            // Get the project directory (check it exists and is writable)
            File projectDir = new File(args[0]);
            check(projectDir);

            // By convention, the output and graphs directories will be subdirectories of projectDir
            File outputDir = new File(projectDir, "Output");
            File graphsDir = new File(projectDir, "Graphs");

            // Run Verdict STEM on the project
            VerdictStem stem = new VerdictStem();
            stem.runStem(projectDir, outputDir, graphsDir);
        } else {
            LOGGER.error("Usage: java -jar verdict-stem-1.0-SNAPSHOT-capsule.jar <project dir>");
        }
    }

    /**
     * Checks the given directory exists and is writable.
     *
     * @param directory The path to the directory
     * @throws RuntimeException If the dir doesn't exist or is not writable
     */
    private static void check(File directory) {
        if (!directory.exists()) {
            throw new RuntimeException("Directory does not exist: " + directory);
        }

        if (!directory.canRead()) {
            throw new RuntimeException("Directory is not readable: " + directory);
        }

        if (!directory.canWrite()) {
            throw new RuntimeException("Directory is not writable: " + directory);
        }
    }
}
