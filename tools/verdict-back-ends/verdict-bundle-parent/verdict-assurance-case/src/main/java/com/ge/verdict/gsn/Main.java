package com.ge.verdict.gsn;

import java.io.*;
import javax.xml.parsers.*;
import org.xml.sax.SAXException;

/**
 * THIS CLASS EXISTS FOR TESTING ONLY
 *
 * @author 212807042
 */
public class Main {

    /**
     * THIS MAIN METHOD IS FOR TESTING PURPOSES ONLY
     *
     * @param args
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public static void main(String[] args)
            throws ParserConfigurationException, SAXException, IOException {
        System.out.println("Entered CreateGSn.Main()!");

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

    /** FOR TESTING PURPOSES ONLY */
    public static void traverseGSN(GsnNode node) {

        if (node.getNodeType().equalsIgnoreCase("context")) {
            for (int i = 0; i <= node.getNodeLevel(); i++) {
                System.out.print('*');
            }
            System.out.println("Context:- " + node.getNodeId());
        } else if (node.getNodeType().equalsIgnoreCase("solution")) {
            for (int i = 0; i <= node.getNodeLevel(); i++) {
                System.out.print('*');
            }
            System.out.println("Solution:- " + node.getNodeId());
            if (!(node.getInContextOf() == null)) {
                for (GsnNode subNode : node.getInContextOf()) {
                    traverseGSN(subNode);
                }
            }
        } else if (node.getNodeType().equalsIgnoreCase("goal")) {
            for (int i = 0; i <= node.getNodeLevel(); i++) {
                System.out.print('*');
            }
            System.out.println("Goal:- " + node.getNodeId());
            if (!(node.getInContextOf() == null)) {
                for (GsnNode subNode : node.getInContextOf()) {
                    traverseGSN(subNode);
                }
            }
            if (!(node.getSupportedBy() == null)) {
                for (GsnNode subNode : node.getSupportedBy()) {
                    traverseGSN(subNode);
                }
            }
        } else if (node.getNodeType().equalsIgnoreCase("strategy")) {
            for (int i = 0; i <= node.getNodeLevel(); i++) {
                System.out.print('*');
            }
            System.out.println("Strategy:- " + node.getNodeId());
            if (!(node.getInContextOf() == null)) {
                for (GsnNode subNode : node.getInContextOf()) {
                    traverseGSN(subNode);
                }
            }
            if (!(node.getSupportedBy() == null)) {
                for (GsnNode subNode : node.getSupportedBy()) {
                    traverseGSN(subNode);
                }
            }
        }
    }
}
