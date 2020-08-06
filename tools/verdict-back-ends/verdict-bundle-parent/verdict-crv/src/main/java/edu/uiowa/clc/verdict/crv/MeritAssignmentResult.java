package edu.uiowa.clc.verdict.crv;

import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MeritAssignmentResult {

    public static void readAndPrintInfo(File kind2OutputFile) {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(kind2OutputFile);
            doc.getDocumentElement().normalize();

            NodeList MESList = doc.getElementsByTagName("ModelElementSet");

            for (int i = 0; i < MESList.getLength(); ++i) {
                Node MESNode = MESList.item(i);

                if (MESNode.getNodeType() != Node.ELEMENT_NODE) continue;

                Element MESElement = (Element) MESNode;
                NodeList nList = MESElement.getElementsByTagName("Node");

                for (int j = nList.getLength() - 1; j >= 0; --j) {
                    Node nNode = nList.item(j);

                    if (nNode.getNodeType() != Node.ELEMENT_NODE) continue;

                    Element nodeElement = (Element) nNode;

                    String nodeName = nodeElement.getAttribute("name");
                    //                    nodeName = nodeName.replaceAll("_port_", ".");
                    nodeName = nodeName.replace("_dot_", ".");

                    System.out.println("Node " + nodeName + ":");

                    NodeList eList = nodeElement.getElementsByTagName("Element");

                    for (int k = 0; k < eList.getLength(); ++k) {
                        Node eNode = eList.item(k);

                        if (eNode.getNodeType() != Node.ELEMENT_NODE) continue;

                        Element elementElement = (Element) eNode;

                        System.out.print("  ");

                        String category = elementElement.getAttribute("category").replace('_', ' ');

                        System.out.print(category);

                        System.out.println(" '" + elementElement.getAttribute("name") + "'");

                        // System.out.print(" '" + elementElement.getAttribute("name") + "'");

                        //                        String line = elementElement.getAttribute("line");
                        //                        String column =
                        // elementElement.getAttribute("column");
                        //
                        //                        System.out.println(" at position [l" + line + "c"
                        // + column + "]");
                    }

                    System.out.println();
                }
            }

        } catch (Exception e) {
            System.err.println("Error reading .xml file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
