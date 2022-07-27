/* See LICENSE in project directory */
package com.ge.verdict.lustre;

import com.ge.verdict.vdm.VdmTest;
import com.ge.verdict.vdm.VdmTranslator;
import java.io.File;
import java.io.IOException;
import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;
import verdict.vdm.vdm_model.Model;

public class VerdictLustreTranslatorTest {

    @Test
    public void testMarshalToLustre1() throws IOException {
        Model controlModel = VdmTranslator.unmarshalFromXml(new File("src/test/resources/vdm-input1.xml"));

        File testFile = File.createTempFile("vdm-model", ".lus");
        testFile.deleteOnExit();
        VerdictLustreTranslator.marshalToLustre(controlModel, testFile);
        Assertions.assertThat(testFile).exists();

        File controlFile = new File("src/test/resources/lustre-output1.lus");
        Assertions.assertThat(testFile).hasSameTextualContentAs(controlFile);
    }

    @Test
    public void testMarshalToLustre2() throws IOException {
        Model controlModel = VdmTranslator.unmarshalFromXml(new File("src/test/resources/vdm-input2.xml"));

        File testFile = File.createTempFile("vdm-model", ".lus");
        testFile.deleteOnExit();
        VerdictLustreTranslator.marshalToLustre(controlModel, testFile);
        Assertions.assertThat(testFile).exists();

        File controlFile = new File("src/test/resources/lustre-output2.lus");
        Assertions.assertThat(testFile).hasSameTextualContentAs(controlFile);
    }

    @Ignore
    @Test
    public void testUnmarshalFromLustre() throws IOException {
        File testFile = new File("src/test/resources/vdm-model.lus");
        Model testModel = VerdictLustreTranslator.unmarshalFromLustre(testFile);

        Model controlModel = VdmTest.createControlModel();

        Assertions.assertThat(testModel).usingRecursiveComparison().isEqualTo(controlModel);
    }

    @Ignore
    @Test
    public void testUnmarshalFromInclude() throws IOException {
        File testFile = new File("src/test/resources/include-vdm-model.lus");
        Model testModel = VerdictLustreTranslator.unmarshalFromLustre(testFile);
        testModel.setName("vdm-model.lus");

        Model controlModel = VdmTest.createControlModel();

        Assertions.assertThat(testModel).usingRecursiveComparison().isEqualTo(controlModel);
    }
}
