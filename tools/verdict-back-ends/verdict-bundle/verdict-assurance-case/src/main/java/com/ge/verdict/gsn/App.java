package com.ge.verdict.gsn;

import org.xml.sax.SAXException;

import java.io.*;

import javax.xml.parsers.*;

/**
 * @author Saswata Paul
 */
public class App {

    /**
     * This main method can be used for independently using the security gsn interface
     *
     * @param args
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public static void main(String[] args)
            throws IOException, ParserConfigurationException, SAXException {

        if (args.length != 6) {
            System.out.println("Argument Error: Invalid number of arguments provided.");
        } else {
            String userInput = args[0];
            String gsnOutputDir = args[1];
            String soteriaOutputDir = args[2];
            String modelAadlPath = args[3];
            String soteriaOutputLinkPathPrefix = args[4];
            String hostSTEMDir = args[5];

            boolean securityCaseFlag = true;
            boolean xmlFlag = false;

            // calling the security gsn creating interface
            SecurityGSNInterface interfaceObj = new SecurityGSNInterface();

            interfaceObj.runGsnArtifactsGenerator(
                    userInput,
                    gsnOutputDir,
                    soteriaOutputDir,
                    modelAadlPath,
                    securityCaseFlag,
                    xmlFlag,
                    soteriaOutputLinkPathPrefix,
                    hostSTEMDir);
        }
    }
}
