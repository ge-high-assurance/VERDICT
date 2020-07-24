package com.ge.verdict.gsn;

import java.io.*;
import javax.xml.parsers.*;
import org.xml.sax.SAXException;

/**
 * 
 * @author Saswata Paul
 */
public class App {

    /**
     *
     * @param args
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public static void main(String[] args)
            throws ParserConfigurationException, SAXException, IOException {

        // A user-specified requirement to create the GSN fragment for
        // String rootGoalId = "MReq01";
        // String rootGoalId = "MReq02";
        String rootGoalId = "CyberReq01";
        // String rootGoalId = "CyberReq02";
        // String rootGoalId = "SafetyReq01";
        // String rootGoalId = "SafetyReq02";

        // The files
        File testXml = new File("/Users/212807042/Desktop/DeliveryDroneFiles/DeliveryDroneVdm.xml");
        File cyberOutput =
                new File(
                        "/Users/212807042/Desktop/DeliveryDroneFiles/soteria_outputs/ImplProperties.xml");
        File safetyOutput =
                new File(
                        "/Users/212807042/Desktop/DeliveryDroneFiles/soteria_outputs/ImplProperties-safety.xml");
        String sourceDot =
                "/Users/212807042/Desktop/DeliveryDroneFiles/graphviz_examples/sample.dot";
        String destinationGraph =
                "/Users/212807042/Desktop/DeliveryDroneFiles/graphviz_examples/sample.svg";

        String aadlModelAddress =
                "/Users/212807042/Desktop/DeliveryDroneFiles/graphviz_examples/DeliveryDrone.aadl";

        GsnNode gsnFragment =
                CreateGSN.gsnCreator(
                        testXml, cyberOutput, safetyOutput, aadlModelAddress, rootGoalId);

        System.out.println("Created Gsn Fragment");

        // Create a file and print the GSN XML
        File gsnXmlFile =
                new File(
                        "/Users/212807042/Desktop/DeliveryDroneFiles/graphviz_examples/sample.xml");

        Gsn2Xml.convertGsnToXML(gsnFragment, gsnXmlFile);

        System.out.println("Created Gsn Xml");

        // Create a file and print the dot
        File gsnDotFile =
                new File(
                        "/Users/212807042/Desktop/DeliveryDroneFiles/graphviz_examples/sample.dot");
        Gsn2Dot.createDot(gsnFragment, gsnDotFile);

        System.out.println("Created Gsn dot");

        // generate the graphViz svg file
        Dot2GraphViz.generateGraph(sourceDot, destinationGraph);

        System.out.println("Created Gsn svg");
    }


    /**
     * The interface for creating GSN fragments
     * @param args
     * @throws IOException 
     * @throws SAXException 
     * @throws ParserConfigurationException 
     */
    public void runGsnArtifactsGenerator(String rootGoalId, String gsnOutputDir, String soteriaOutputDir, String caseAadlPath) throws IOException, ParserConfigurationException, SAXException {

        File modelXml = new File(gsnOutputDir, "modelXML.xml");
        File cyberOutput = new File(soteriaOutputDir, "ImplProperties.xml");
        File safetyOutput = new File(soteriaOutputDir, "ImplProperties-safety.xml");
                
        //create the GSN fragment
        GsnNode gsnFragment = CreateGSN.gsnCreator(modelXml, cyberOutput, safetyOutput, caseAadlPath, rootGoalId);
        System.out.println("Created Gsn Fragment");
        
        //Filenames
        String xmlFilename = rootGoalId + "_GsnFragment.xml";
        String dotFilename = rootGoalId + "_GsnFragment.dot";
        String svgFilename = rootGoalId + "_GsnFragment.svg";

        // Create a file and print the GSN XML
        File gsnXmlFile =new File(gsnOutputDir, xmlFilename);
        Gsn2Xml.convertGsnToXML(gsnFragment, gsnXmlFile);
        System.out.println("Created Gsn Xml");

        // Create a file and print the dot
        File gsnDotFile = new File(gsnOutputDir, dotFilename);
        Gsn2Dot.createDot(gsnFragment, gsnDotFile);
        System.out.println("Created Gsn dot");

        // generate the svg file using graphviz
        String graphDestination = gsnOutputDir +"/" + svgFilename;
        String dotFileSource = gsnDotFile.getAbsolutePath();
        Dot2GraphViz.generateGraph(dotFileSource, graphDestination);
        System.out.println("Created Gsn svg");
    	
    }
    
    
}
