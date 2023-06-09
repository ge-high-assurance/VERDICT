/* See LICENSE in project directory */
package com.ge.verdict.vdm;

import java.io.File;
import java.io.IOException;
import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;
import org.xmlunit.assertj.XmlAssert;
import verdict.vdm.vdm_model.Model;

public class VdmTranslatorTest {

    @Ignore
    @Test
    public void testMarshalToXml() throws IOException {
        Model controlModel = VdmTest.createControlModel();

        File testFile = File.createTempFile("vdm-model", ".xml");
        testFile.deleteOnExit();
        VdmTranslator.marshalToXml(controlModel, testFile);
        Assertions.assertThat(testFile).exists();

        File controlFile = new File("src/test/resources/vdm-model.xml");
        XmlAssert.assertThat(testFile).and(controlFile).normalizeWhitespace().areIdentical();
    }

    @Ignore
    @Test
    public void testUnmarshalFromXml() throws IOException {
        File testFile = new File("src/test/resources/vdm-model.xml");
        Model testModel = VdmTranslator.unmarshalFromXml(testFile);

        Model controlModel = VdmTest.createControlModel();
        Assertions.assertThat(testModel).usingRecursiveComparison().isEqualTo(controlModel);
    }

    @Test
    public void testXml() throws IOException {
        File controlFile = new File("src/test/resources/vdm-model.xml");
        Model controlModel = VdmTranslator.unmarshalFromXml(controlFile);

        File testFile = File.createTempFile("vdm-model", ".xml");
        testFile.deleteOnExit();
        VdmTranslator.marshalToXml(controlModel, testFile);
        Assertions.assertThat(testFile).exists();
    }
}
