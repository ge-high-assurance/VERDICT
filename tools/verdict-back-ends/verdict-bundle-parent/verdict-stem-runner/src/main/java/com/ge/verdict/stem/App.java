/* See LICENSE in project directory */
package com.ge.verdict.stem;

import java.io.File;
import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Runs SADL on a Verdict STEM project. */
public class App {
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    /**
     * Runs SADL on the given Verdict STEM project.
     *
     * @param args Command line arguments with path to project
     */
    public static void main(String[] args) throws URISyntaxException {
        // Check that we have one argument
        if (args.length == 1) {

            // Get the Verdict STEM project directory (check it exists and is writable)
            File projectDir = new File(args[0]);
            checkDir(projectDir);

            // By convention, the output and graphs directories will be subdirectories of projectDir
            File outputDir = new File(projectDir, "Output");
            File graphsDir = new File(projectDir, "Graphs");

            // Run SADL on the Verdict STEM project
            VerdictStem stem = new VerdictStem();
            stem.runStem(projectDir, outputDir, graphsDir);
        } else {
            File jarFile =
                    new File(App.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            LOGGER.error("Usage: java -jar {} <project dir>", jarFile.getName());
        }
    }

    /**
     * Checks the given directory exists and is writable.
     *
     * @param directory The path to the directory
     * @throws RuntimeException If the directory doesn't exist or is not writable
     */
    private static void checkDir(File directory) {
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
