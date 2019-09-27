/* See LICENSE in project directory */
package com.ge.verdict.lustre;

import com.ge.verdict.vdm.VdmTest;
import java.io.File;
import java.io.IOException;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import verdict.vdm.vdm_model.Model;

public class VerdictLustreTranslatorTest {

    @Test
    public void testMarshalToLustre() throws IOException {
        Model controlModel = VdmTest.createControlModel();

        File testFile = File.createTempFile("vdm-model", ".lus");
        testFile.deleteOnExit();
        VerdictLustreTranslator translator = new VerdictLustreTranslator();
        translator.marshalToLustre(controlModel, testFile);
        Assertions.assertThat(testFile).exists();

        File controlFile = new File("src/test/resources/vdm-model-output.lus");
        Assertions.assertThat(testFile).hasSameContentAs(controlFile);
    }

    @Test
    public void testUnmarshalFromLustre() throws IOException {
        File testFile = new File("src/test/resources/vdm-model.lus");
        VerdictLustreTranslator translator = new VerdictLustreTranslator();
        Model testModel = translator.unmarshalFromLustre(testFile);

        Model controlModel = VdmTest.createControlModel();

        Assertions.assertThat(testModel).usingRecursiveComparison().isEqualTo(controlModel);
    }

    @Test
    public void testUnmarshalFromInclude() throws IOException {
        File testFile = new File("src/test/resources/include-vdm-model.lus");
        VerdictLustreTranslator translator = new VerdictLustreTranslator();
        Model testModel = translator.unmarshalFromLustre(testFile);
        testModel.setName("vdm-model.lus");

        Model controlModel = VdmTest.createControlModel();

        Assertions.assertThat(testModel).usingRecursiveComparison().isEqualTo(controlModel);
    }
}
