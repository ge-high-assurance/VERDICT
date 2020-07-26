package com.ge.verdict.gsn;

import java.io.*;
import javax.xml.parsers.*;
import org.xml.sax.SAXException;

/** @author Saswata Paul */
public class App {

    /**
     * @param args
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public static void main(String[] args)
            throws IOException, ParserConfigurationException, SAXException {

        if (args.length != 4) {
            System.out.println("Invalid number of arguments provided.");
        } else {
            String rootGoalId = args[0];
            String gsnOutputDir = args[1];
            String soteriaOutputDir = args[2];
            String caseAadlPath = args[3];

            // calling the function to create GSN artefacts
            CreateGSN createGsnObj = new CreateGSN();

            createGsnObj.runGsnArtifactsGenerator(
                    rootGoalId, gsnOutputDir, soteriaOutputDir, caseAadlPath);
        }
    }
}
