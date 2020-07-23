/* See LICENSE in project directory */
package com.ge.verdict.test.instrumentor;

import com.ge.verdict.vdm.VdmTranslator;
import java.io.File;
import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import verdict.vdm.vdm_model.Model;

public class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws URISyntaxException {
        if (args.length != 2) {
            File jarFile =
                    new File(App.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            LOGGER.error("Usage: java -jar {} <input file> <output file>", jarFile.getName());
        } else {
            // Get the input file
            File inputFile = new File(args[0]);
            File outputFile = new File(args[1]);

            Model vdmModel = VdmTranslator.unmarshalFromXml(inputFile);

            VerdictTestInstrumentor atg = new VerdictTestInstrumentor(vdmModel);
            atg.instrumentTests();

            VdmTranslator.marshalToXml(vdmModel, outputFile);
        }
    }
}
