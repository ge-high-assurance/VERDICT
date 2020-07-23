/* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.

   @author: M. Fareed Arif
*/

package edu.uiowa.clc.verdict.vdm.utest;

import com.ge.verdict.vdm.VdmTranslator;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import verdict.vdm.vdm_model.Model;

public class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws IOException, URISyntaxException {
        // Check that we have two arguments
        if (args.length != 2) {
            File jarFile =
                    new File(App.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            LOGGER.error("Usage: java -jar {} <input file> <output file>", jarFile.getName());
        } else {
            // Get the input and output files
            String intputFile = args[0]; // "hawkeye-UAV/iml/hawkeyeUAV_model_D.iml"
            File outputFile = new File(args[1]); // "hawkeye-UAV/xml/hawkeyeUAV_model_D-new.xml"

            // Translate the model from IML to VDM
            Model vdm_model = ResourceTest.setup(intputFile);

            // Save the VDM model
            VdmTranslator.marshalToXml(vdm_model, outputFile);
        }
    }
}
