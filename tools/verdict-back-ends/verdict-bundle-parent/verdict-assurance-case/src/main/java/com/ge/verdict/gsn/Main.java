package com.ge.verdict.gsn;

import static guru.nidi.graphviz.model.Factory.*;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import java.io.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import verdict.vdm.vdm_model.*;

/**
 * FOR TESTING ONLY
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

        // The files
        File testXml = new File("/Users/212807042/Desktop/DeliveryDroneFiles/DeliveryDroneVdm.xml");
        File cyberOutput =
                new File(
                        "/Users/212807042/Desktop/DeliveryDroneFiles/soteria_outputs/ImplProperties.xml");
        File safetyOutput =
                new File(
                        "/Users/212807042/Desktop/DeliveryDroneFiles/soteria_outputs/ImplProperties-safety.xml");

        CreateGSN.gsnCreator(testXml, cyberOutput, safetyOutput);

        // GraphViz parse dot file example
        try (InputStream dot =
                new FileInputStream(
                        "/Users/212807042/Desktop/DeliveryDroneFiles/graphviz_examples/sample.dot"); ) {
            MutableGraph g = new Parser().read(dot);
            // Graphviz.fromGraph(g).render(Format.SVG).toFile(new
            // File("/Users/212807042/Desktop/DeliveryDroneFiles/graphviz_examples/sample.svg"));
            Graphviz.fromGraph(g)
                    .render(Format.PNG)
                    .toFile(
                            new File(
                                    "/Users/212807042/Desktop/DeliveryDroneFiles/graphviz_examples/sample.png"));
        }
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
