/* See LICENSE in project directory */
package com.ge.verdict.mbas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import verdict.vdm.vdm_model.Model;

import java.io.File;
import java.net.URISyntaxException;

public class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    public static boolean isValidDir(String dirName) {
        if (dirName == null) {
            System.out.println("Input directory name is null!");
            return false;
        }

        File dir = new File(dirName);

        if (!dir.exists()) {
            System.out.println("Directory: " + dirName + " does not exist!");
            return false;
        }
        if (!dir.isDirectory()) {
            System.out.println(dirName + " is not a directory!");
            return false;
        }
        return true;
    }

    public static boolean isValidXMLFile(String fileName) {
        if (fileName == null) {
            System.out.println("Input directory name is null!");
            return false;
        }

        File file = new File(fileName);

        if (!file.exists()) {
            System.out.println("File: " + fileName + " does not exist!");
            return false;
        }
        if (!file.isFile()) {
            System.out.println(fileName + " is not a file!");
            return false;
        }
        return true;
    }

    public static void main(String[] args) throws URISyntaxException {
        // Check that we have three arguments
        // args[0]: input aadl file path, args[1]: output STEM inputs, args[2]: output Soteria++
        // inputs
        if (args.length != 3) {
            File jarFile =
                    new File(App.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            LOGGER.error(
                    "Usage: java -jar {} <input file> <STEM output folder> <Soteria++ output folder>",
                    jarFile.getName());
        } else {
            if (isValidXMLFile(args[0])) {
                if (isValidDir(args[1]) && isValidDir(args[2])) {
                    File inputFile = new File(args[0]);

                    // Translate VDM to Mbas inputs
                    Model model = VDM2CSV.unmarshalFromXml(inputFile);
                    VDM2CSV.marshalToMbasInputs(model, args[0], args[1], args[2]);
                }
            }
        }
    }
}
