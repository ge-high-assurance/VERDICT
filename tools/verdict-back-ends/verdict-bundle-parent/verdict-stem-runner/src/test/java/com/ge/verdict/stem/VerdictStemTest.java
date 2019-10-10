/* See LICENSE in project directory */
package com.ge.verdict.stem;

import java.io.File;
import java.io.IOException;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Runs unit tests on a VerdictStem object. */
public class VerdictStemTest {
    @Rule public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    public void testSTEM() throws IOException {
        File projectDir = new File("src/test/STEM");
        File outputDir = tmpFolder.getRoot();
        File graphsDir = outputDir;

        VerdictStem stem = new VerdictStem();
        stem.runStem(projectDir, outputDir, graphsDir);

        File testFile = new File(outputDir, "CAPEC.csv");
        Assertions.assertThat(testFile).exists();
        File controlFile = new File("src/test/STEM/Output/CAPEC.csv");
        Assertions.assertThat(testFile).hasSameContentAs(controlFile);

        testFile = new File(outputDir, "Defenses.csv");
        Assertions.assertThat(testFile).exists();
        controlFile = new File("src/test/STEM/Output/Defenses.csv");
        Assertions.assertThat(testFile).hasSameContentAs(controlFile);

        testFile = new File(graphsDir, "Run_sadl12.svg");
        Assertions.assertThat(testFile).exists();
    }
}
