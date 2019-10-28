/* See LICENSE in project directory */
package com.ge.verdict.stem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.Test;

/** Runs unit tests on a VerdictStem object. */
public class VerdictStemTest {

    @Test
    public void testSTEM() throws IOException {
        File projectDir = new File("target/test-classes/STEM");
        Path outputDir = Paths.get(projectDir.getPath(), "Output");
        Path graphsDir = Paths.get(projectDir.getPath(), "Graphs");
        File sadlFile = new File(projectDir, "Run.sadl");

        // Remove the output and graphs directories first
        for (Path path :
                Files.walk(outputDir)
                        .sorted(Comparator.reverseOrder())
                        .collect(Collectors.toList())) {
            Files.delete(path);
        }
        for (Path path :
                Files.walk(graphsDir)
                        .sorted(Comparator.reverseOrder())
                        .collect(Collectors.toList())) {
            Files.delete(path);
        }

        // Run SADL on the STEM test project
        VerdictStem stem = new VerdictStem();
        stem.runStem(projectDir, sadlFile);

        // Verify that SADL created some new files with expected contents
        Path testFile = outputDir.resolve("CAPEC.csv");
        Assertions.assertThat(testFile).exists();

        Path controlFile = Paths.get("src/test/resources/STEM/Output/CAPEC.csv");
        String testData = new String(Files.readAllBytes(testFile));
        String controlData = new String(Files.readAllBytes(controlFile));
        Assertions.assertThat(testData).isEqualToNormalizingNewlines(controlData);

        testFile = outputDir.resolve("Defenses.csv");
        Assertions.assertThat(testFile).exists();

        controlFile = Paths.get("src/test/resources/STEM/Output/Defenses.csv");
        testData = new String(Files.readAllBytes(testFile));
        controlData = new String(Files.readAllBytes(controlFile));
        Assertions.assertThat(testData).isEqualToNormalizingNewlines(controlData);

        testFile = graphsDir.resolve("Run_sadl12.svg");
        Assertions.assertThat(testFile).exists();
    }
}
