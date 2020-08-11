package com.ge.verdict.gsn;

import java.io.*;
import javax.xml.parsers.*;
import org.xml.sax.SAXException;

/** @author Saswata Paul */
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

        if (args.length != 5) {
            System.out.println("Argument Error: Invalid number of arguments provided.");
        } else {
            String userInput = args[0];
            String gsnOutputDir = args[1];
            String soteriaOutputDir = args[2];
            String caseAadlPath = args[3];
            String modelName = args[4];

            boolean securityCaseFlag = true;
            boolean xmlFlag = false;

            // calling the security gsn creating interface
            SecurityGSNInterface interfaceObj = new SecurityGSNInterface();

            interfaceObj.runGsnArtifactsGenerator(
                    userInput,
                    gsnOutputDir,
                    soteriaOutputDir,
                    caseAadlPath,
                    securityCaseFlag,
                    xmlFlag,
                    modelName);
        }
    }
}
