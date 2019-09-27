/* See LICENSE in project directory */
package com.ge.verdict.vdm;

import java.io.File;
import java.io.IOException;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.xmlunit.assertj.XmlAssert;
import verdict.vdm.vdm_model.Model;

public class VdmTranslatorTest {

    @Test
    public void testMarshalToXml() throws IOException {
        Model controlModel = VdmTest.createControlModel();

        File testFile = File.createTempFile("vdm-model", ".xml");
        testFile.deleteOnExit();
        VdmTranslator translator = new VdmTranslator();
        translator.marshalToXml(controlModel, testFile);
        Assertions.assertThat(testFile).exists();

        File controlFile = new File("src/test/resources/vdm-model.xml");
        XmlAssert.assertThat(testFile).and(controlFile).normalizeWhitespace().areIdentical();
    }

    @Test
    public void testUnmarshalFromXml() throws IOException {
        File testFile = new File("src/test/resources/vdm-model.xml");
        VdmTranslator translator = new VdmTranslator();
        Model testModel = translator.unmarshalFromXml(testFile);

        Model controlModel = VdmTest.createControlModel();
        Assertions.assertThat(testModel).usingRecursiveComparison().isEqualTo(controlModel);
    }
}
