/* See LICENSE in project directory */
package com.ge.verdict.lustre;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import verdict.vdm.vdm_model.Model;

public class VerdictLustreTranslatorIT {

    @Test
    public void testAllLustreFiles() throws IOException {
        // Contains some files too hard for our grammar to parse correctly
        // Path path = Paths.get("/VERDICT/synchrone/lustre-examples");

        // Uses reserved token "mode" as identifier in CruiseController.lus, otherwise no errors
        // Path path = Paths.get("/VERDICT/kind2-mc/kind2-webservices/src/main/resources/examples");

        // Has hundreds of errors still to be fixed
        // Path path = Paths.get("/VERDICT/kind2-mc/kind2/tests");

        // Uses "mode" as identifier
        // Path path = Paths.get("/VERDICT/coco-team/benchmarks/Lustre/multi_prop");

        // Has some files using automation, clock syntax too complicated for our grammar
        // Path path = Paths.get("/VERDICT/coco-team/benchmarks/Lustre/language_test");

        // Has no errors
        // Path path = Paths.get("/VERDICT/agacek/jkind-benchmarks/benchmarks/lustre");

        // Has array and record update syntax (jkind extension we don't need to support)
        // Path path = Paths.get("/VERDICT/agacek/jkind/testing");

        // Has no errors
        Path path = Paths.get("../../../../../materials/VerdictDemo/tmp/translated_models/model_d");

        // Test all Lustre files stored under the given path.
        Files.walk(path)
                .parallel()
                .filter(p -> p.getFileName().toString().endsWith(".lus"))
                .forEach(VerdictLustreTranslatorIT::testLustreFile);
    }

    public static void testLustreFile(Path path) {
        try {
            // Unmarshal a model from the original Lustre file
            Model originalModel = VerdictLustreTranslator.unmarshalFromLustre(path.toFile());

            // Marshal the original model to a temporary Lustre file
            File tempFile = File.createTempFile(path.getFileName().toString(), ".lus");
            VerdictLustreTranslator.marshalToLustre(originalModel, tempFile);
            Assertions.assertThat(tempFile).exists();
            tempFile.deleteOnExit();

            // Unmarshal another model from the temporary Lustre file and compare it to the original
            // model to check the fidelity of the marshalToLustre method
            Model anotherModel = VerdictLustreTranslator.unmarshalFromLustre(tempFile);
            anotherModel.setName(originalModel.getName());
            try {
                Assertions.assertThat(anotherModel)
                        .usingRecursiveComparison()
                        .isEqualTo(originalModel);
            } catch (AssertionError e) {
                System.err.println("Found non-fidelity of translation in " + path);
                // Save both temporary Lustre and temporary XML files for easier debugging
                tempFile = File.createTempFile(path.getFileName().toString(), ".lus");
                VerdictLustreTranslator.marshalToLustre(originalModel, tempFile);
                tempFile = File.createTempFile(path.getFileName().toString(), ".xml");
                VerdictLustreTranslator.marshalToXml(originalModel, tempFile);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
