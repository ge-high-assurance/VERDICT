/* See LICENSE in project directory */
package com.ge.verdict.test.instrumentor;

import com.ge.verdict.vdm.VdmTranslator;
import java.io.File;
import verdict.vdm.vdm_model.Model;

public class AppClass {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println(
                    "Usage: java -jar verdict-test-instrumentor-1.0-SNAPSHOT-capsule.jar <input file> <output file>");
        } else {
            // Get the input file
            File inputFile = new File(args[0]);
            File outputFile = new File(args[1]);

            VdmTranslator translator = new VdmTranslator();

            Model vdmModel = translator.unmarshalFromXml(inputFile);

            VerdictTestInstrumentor atg = new VerdictTestInstrumentor(vdmModel);
            atg.instrumentTests();

            translator.marshalToXml(vdmModel, outputFile);
        }
    }
}
