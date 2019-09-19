/* See LICENSE in project directory */
package edu.uiowa.clc.verdict.vdm.utest;

import com.ge.verdict.vdm.VdmTranslator;
import java.io.File;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import verdict.vdm.vdm_model.Model;

public class AppClass {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppClass.class);

    public static void main(String[] args) throws IOException {
        // Check that we have two arguments
        if (args.length != 2) {
            LOGGER.error(
                    "Usage: java -jar verdict-vdm2iml-translator-1.0.jar <input file> <output file>");
        } else {
            // Get the input and output files
            String intputFile = args[0]; // "hawkeye-UAV/iml/hawkeyeUAV_model_D.iml"
            File outputFile = new File(args[1]); // "hawkeye-UAV/xml/hawkeyeUAV_model_D-new.xml"

            // Translate the model from IML to VDM
            Model vdm_model = ResourceTest.setup(intputFile);

            // Save the VDM model
            VdmTranslator translator = new VdmTranslator();

            translator.marshalToXml(vdm_model, outputFile);
        }
    }
}
