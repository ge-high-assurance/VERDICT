/* See LICENSE in project directory */
package com.ge.verdict.stem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;

/** Runs unit tests on a VerdictStem object. */
public class VerdictStemTest {

    @Test
    @Ignore
    public void testSTEM() throws IOException {
        Path controlOutputDir = Paths.get("../../STEM/Output");
        Path testStemDir = Paths.get("target/test-classes/STEM");
        Path testOutputDir = testStemDir.resolve("Output");
        Path testGraphsDir = testStemDir.resolve("Graphs");

        // Remove the output and graphs directories first
        if (Files.exists(testOutputDir)) {
            for (Path path :
                    Files.walk(testOutputDir)
                            .sorted(Comparator.reverseOrder())
                            .collect(Collectors.toList())) {
                Files.delete(path);
            }
        }
        if (Files.exists(testGraphsDir)) {
            for (Path path :
                    Files.walk(testGraphsDir)
                            .sorted(Comparator.reverseOrder())
                            .collect(Collectors.toList())) {
                Files.delete(path);
            }
        }

        // Run SADL on the STEM test project
        VerdictStem stem = new VerdictStem();
        stem.runStem(testStemDir.toFile(), testOutputDir.toFile(), testGraphsDir.toFile());

        // Verify that SADL created some new files with expected contents
        Path controlFile = controlOutputDir.resolve("CAPEC.csv");
        Path testFile = testOutputDir.resolve("CAPEC.csv");
        Assertions.assertThat(testFile).exists();
        String controlData = new String(Files.readAllBytes(controlFile));
        String testData = new String(Files.readAllBytes(testFile));
        Assertions.assertThat(testData).isEqualToNormalizingNewlines(controlData);

        controlFile = controlOutputDir.resolve("Defenses.csv");
        testFile = testOutputDir.resolve("Defenses.csv");
        Assertions.assertThat(testFile).exists();
        controlData = new String(Files.readAllBytes(controlFile));
        testData = new String(Files.readAllBytes(testFile));
        Assertions.assertThat(testData).isEqualToNormalizingNewlines(controlData);

        controlFile = controlOutputDir.resolve("ConnDefenses.csv");
        testFile = testOutputDir.resolve("ConnDefenses.csv");
        Assertions.assertThat(testFile).exists();
        controlData = new String(Files.readAllBytes(controlFile));
        testData = new String(Files.readAllBytes(testFile));
        Assertions.assertThat(testData).isEqualToNormalizingNewlines(controlData);

        controlFile = controlOutputDir.resolve("Defenses2NIST.csv");
        testFile = testOutputDir.resolve("Defenses2NIST.csv");
        Assertions.assertThat(testFile).exists();
        controlData = new String(Files.readAllBytes(controlFile));
        testData = new String(Files.readAllBytes(testFile));
        Assertions.assertThat(testData).isEqualToNormalizingNewlines(controlData);

        controlFile = controlOutputDir.resolve("ArchMitigation.csv");
        testFile = testOutputDir.resolve("ArchMitigation.csv");
        Assertions.assertThat(testFile).exists();
        controlData = new String(Files.readAllBytes(controlFile));
        testData = new String(Files.readAllBytes(testFile));
        Assertions.assertThat(testData).isEqualToNormalizingNewlines(controlData);

        testFile = testGraphsDir.resolve("Run_sadl_graph.svg");
        Assertions.assertThat(testFile).exists();
    }
}
