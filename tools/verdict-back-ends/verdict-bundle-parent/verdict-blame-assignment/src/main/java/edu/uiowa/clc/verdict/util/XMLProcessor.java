/* Copyright (c) 2019-2020, Board of Trustees of the University of Iowa.
   All rights reserved.

   Use of this source code is governed by a BSD 3-Clause License that
   can be found in the LICENSE file.

   @author: M. Fareed Arif
*/

package edu.uiowa.clc.verdict.util;

import edu.uiowa.clc.verdict.blm.BlameAssignment;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XMLProcessor {

    private static Vector<VerdictProperty> result = new Vector<VerdictProperty>();

    public static Vector<VerdictProperty> praseXML(InputStream xmlStream) {

        //        Vector<VerdictProperty> property_result = new Vector<VerdictProperty>();

        try {

            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            Document doc = dBuilder.parse(xmlStream);

            //            System.out.println("Root element :" +
            // doc.getDocumentElement().getNodeName());

            if (doc.hasChildNodes()) {
                // printNote(doc.getChildNodes());
                VerdictProperty verdict_property = new VerdictProperty();
                parseProperty(doc.getElementsByTagName("Property"), verdict_property);
            }

            // doc.getElementsByTagName("Property");
            xmlStream.close();

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return result;
    }

    // private static void parseProperty() {}

    private static void parseProperty(NodeList nodeList, VerdictProperty verdict_property) {

        String wk_value = null;

        for (int count = 0; count < nodeList.getLength(); count++) {
            // System.out.println(">>>>> length :=" + nodeList.getLength());
            Node tempNode = nodeList.item(count);

            // make sure it's element node.
            if (tempNode.getNodeType() == Node.ELEMENT_NODE) {

                // Property
                String node_type = tempNode.getNodeName();

                // System.out.println("\nNode Name =" + node_type + " [OPEN]");
                // System.out.println("Node Value =" + tempNode.getTextContent());
                // name
                // line
                if (tempNode.hasAttributes()) {

                    // get attributes names and values
                    NamedNodeMap nodeMap = tempNode.getAttributes();

                    for (int i = 0; i < nodeMap.getLength(); i++) {

                        Node node = nodeMap.item(i);

                        String node_name = node.getNodeName();

                        if (node_type.equals("Property")) {
                            if (node_name.equals("name")) {
                                verdict_property = new VerdictProperty();
                                String Id = node.getNodeValue();
                                //                                System.out.println("Property: " +
                                // Id);
                                verdict_property.setId(Id);
                            }
                        } else if (node_type.equals("Answer")) {

                            String status = tempNode.getTextContent();

                            // answer = valid or answer = falsifiable.
                            //                            System.out.println("Answer: " +
                            // tempNode.getTextContent());

                            String t_source = node.getNodeValue();
                            //                            System.out.println("Source: " + t_source);
                            verdict_property.setSource(t_source);

                            if (status.equals("valid")) {
                                // Property is valid
                                verdict_property.setStatus(true);
                            } else {

                                if (t_source.equals("mcs")) {
                                    // parse weak assumptions
                                    //                                    System.out.println("Parse
                                    // Weak Assumption");
                                    //
                                    // parseWAMAX(node.getChildNodes());
                                } else {
                                    // parse counterExample
                                    //                                    System.out.println("Parse
                                    // Counter Example");
                                }
                            }

                        } else if (node_type.equals("Runtime")) {

                            if (node_name.equals("timeout")) {
                                boolean timeout = Boolean.valueOf(node.getNodeValue());

                                if (timeout == false) {
                                    String runTime = tempNode.getTextContent();
                                    //                                    float value =
                                    // Float.valueOf(runTime);
                                    //                                    System.out.println("Time :
                                    // " + runTime);
                                    verdict_property.setTime(runTime);
                                } else {
                                    // Solver Timed out
                                }
                            }
                        }
                        if (node_type.equals("WeakAssumption")) {
                            if (node_name.equals("name")) {
                                String value = node.getNodeValue();
                                wk_value = value.substring(0, value.indexOf(' '));
                            } else if (node_name.equals("satisfied")) {
                                boolean wk_status = Boolean.valueOf(node.getNodeValue());
                                verdict_property.addAssumption(wk_value, wk_status);
                            }
                        }
                    }
                }

                if (tempNode.hasChildNodes()) {
                    // Loop again if has child nodes
                    parseProperty(tempNode.getChildNodes(), verdict_property);

                    // Next Property
                }

                if (tempNode.getNodeName().equals("Property")) {
                    if (result.contains(verdict_property)) {
                        //                        int index = result.indexOf(verdict_property);
                        //                        VerdictProperty existing_property =
                        // result.get(index);
                        //
                        // existing_property.setWeakAssumptions(verdict_property.getWeakAssumptions());
                        // collect weak assumptions
                    } else {
                        result.add(verdict_property);
                        verdict_property = new VerdictProperty();
                    }
                }
                // System.out.println("Node Name =" + tempNode.getNodeName() + "
                // [CLOSE]");
            }
        }
    }

    // CEX
    public static void parseCEX(NodeList nodeList) {}

    public static void dumpXML(BlameAssignment bml, File outputFile) {
        try {
            // Create JAXB Context
            JAXBContext jaxbContext = JAXBContext.newInstance(BlameAssignment.class);

            // Create Marshaller
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            // Required formatting??
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            //            // Store XML to File
            //            File file = new File("employee.xml");

            // Writes XML file to file-system
            jaxbMarshaller.marshal(bml, outputFile);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public static void parseLog(File kind2Output) {

        try {

            FileInputStream logStream = new FileInputStream(kind2Output);

            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            Document doc = dBuilder.parse(logStream);

            //            System.out.println("Root element :" +
            // doc.getDocumentElement().getNodeName());

            if (doc.hasChildNodes()) {
                // Parse warning
                NodeList logNodeList = doc.getElementsByTagName("Log");

                for (int index = 0; index < logNodeList.getLength(); index++) {
                    Node nNode = logNodeList.item(index);

                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element eElement = (Element) nNode;

                        //                		System.out.println("Warning: " +
                        // eElement.getAttribute("warn"));
                        //                		System.out.println("Error: " +
                        // eElement.getAttribute("error"));
                        //
                        NodeList cNodes = eElement.getChildNodes();

                        nNode = cNodes.item(0);
                        String nodeName = eElement.getAttribute("class");
                        if (nodeName.equalsIgnoreCase("warn")) {
                            System.out.println("Warning: " + nNode.getTextContent());
                        } else if (nodeName.equalsIgnoreCase("error")
                                || nodeName.equalsIgnoreCase("fatal")) {
                            System.out.println("Failure: " + nNode.getTextContent());

                            if (eElement.hasAttributes()) {

                                // get attributes names and values
                                NamedNodeMap nodeMap = eElement.getAttributes();

                                for (int i = 0; i < nodeMap.getLength(); i++) {

                                    Node node = nodeMap.item(i);

                                    String node_name = node.getNodeName();
                                    if (node_name.contentEquals("class")
                                            || node_name.contentEquals("source")) {
                                        // skip
                                    } else {
                                        System.out.println(node_name + " : " + node.getNodeValue());
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // doc.getElementsByTagName("Property");
            logStream.close();

        } catch (FileNotFoundException exp) {
            // TODO Auto-generated catch block
            System.out.print("No error log file found.");
            //			System.exit(-1);
        } catch (IOException exp) {
            // TODO Auto-generated catch block
            System.out.print("Unable to read log file.");
            //			System.exit(-1);
        } catch (ParserConfigurationException exp) {
            System.out.print("Unable to parse error log file.");
        } catch (SAXException exp) {
            System.out.print("Log file is corrupted.");
        }
    }

    // def parse_ct(example, ct):
    //
    // for node in example:
    // if node.tag == 'Node':
    // # print(node.attrib['name'])
    // # ct.append([])
    // ct.append(node.attrib['name'])
    // parse_ct(node, ct)
    // if node.tag == 'Stream':
    // # if node.attrib['class'] == 'input' or node.attrib['class'] == 'output':
    // # print(node.attrib['name'],node.attrib['type'])
    // ct.append(node.attrib['name'])
    // ct.append(node.attrib['type'])
    // if node.attrib['class'] == 'input' or node.attrib['class'] == 'output' :
    // parse_ct(node, ct)
    // if node.tag == 'Value':
    // # print(node.attrib['instant'],node.text)
    // ct.append(node.attrib['instant'])
    // ct.append(node.text)
    // parse_ct(node, ct)
    //
    // return ct

}
