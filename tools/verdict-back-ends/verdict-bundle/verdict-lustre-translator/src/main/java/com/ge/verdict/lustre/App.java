/* See LICENSE in project directory */
package com.ge.verdict.lustre;

import edu.uiowa.clc.verdict.lustre.VDM2Lustre;
import java.io.File;
import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import verdict.vdm.vdm_model.Model;

public class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws URISyntaxException {

        // Check that we have two arguments
        if (args.length == 2 || args.length == 3) {

            File inputFile = null;
            File vdm_outputFile = null;
            File lustre_outputFile = null;

            if (args.length == 3) {
                // Get the input and output files
                inputFile = new File(args[0]);

                // VDM File
                vdm_outputFile = new File(args[1]);

                // Lustre File
                lustre_outputFile = new File(args[2]);
            } else if (args.length == 2) {
                // Get the input and output files
                inputFile = new File(args[0]);
                // Lustre File
                lustre_outputFile = new File(args[1]);
            }

            // Determine whether we should translate from Lustre to VDM or from VDM to Lustre
            if (inputFile.getName().endsWith(".lus")) {
                Model verdictDataModel = VerdictLustreTranslator.unmarshalFromLustre(inputFile);
                VerdictLustreTranslator.marshalToXml(verdictDataModel, lustre_outputFile);
            } else {

                Model verdictDataModel = VerdictLustreTranslator.unmarshalFromXml(inputFile);

                VDM2Lustre vdm2Lustre = new VDM2Lustre(verdictDataModel);
                verdictDataModel = vdm2Lustre.translate();

                if (args.length == 3) {
                    VerdictLustreTranslator.marshalToXml(verdictDataModel, vdm_outputFile);
                }

                VerdictLustreTranslator.marshalToLustre(verdictDataModel, lustre_outputFile);

                //                PrettyPrinter pp = new PrettyPrinter();
                //                pp.printProgram(verdictDataModel.getDataflowCode(),
                // lustre_outputFile);
            }
        } else {
            File jarFile = new File(App.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
            LOGGER.error(
                    "Usage: java -jar {} <input file> <output file(1).xml> <output file(2).lus>", jarFile.getName());
        }
    }
}
