package com.ge.verdict.synthesis;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ResultsInstance {
    public final boolean partialSolution, meritAssignment, inputSat;
    public final double inputCost, outputCost;
    public final List<Item> items;

    public static class Item {
        public final String component, defenseProperty;
        public final int inputDal, outputDal;
        public final double inputCost, outputCost;

        public Item(
                String component,
                String defenseProperty,
                int inputDal,
                int outputDal,
                double inputCost,
                double outputCost) {
            super();
            this.component = component;
            this.defenseProperty = defenseProperty;
            this.inputDal = inputDal;
            this.outputDal = outputDal;
            this.inputCost = inputCost;
            this.outputCost = outputCost;
        }
    }

    public ResultsInstance(
            boolean partialSolution,
            boolean meritAssignment,
            boolean inputSat,
            double inputCost,
            double outputCost,
            List<Item> items) {
        super();
        this.partialSolution = partialSolution;
        this.meritAssignment = meritAssignment;
        this.inputSat = inputSat;
        this.inputCost = inputCost;
        this.outputCost = outputCost;
        this.items = Collections.unmodifiableList(items);
    }

    public static ResultsInstance fromFile(File file)
            throws SAXException, IOException, ParserConfigurationException {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
        Element root = doc.getDocumentElement();

        List<Item> items = new ArrayList<>();

        NodeList nodes = root.getElementsByTagName("item");
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element) {
                Element elem = (Element) node;
                items.add(
                        new Item(
                                elem.getAttribute("component"),
                                elem.getAttribute("defenseProperty"),
                                Integer.parseInt(elem.getAttribute("inputDal")),
                                Integer.parseInt(elem.getAttribute("outputDal")),
                                Double.parseDouble(elem.getAttribute("inputCost")),
                                Double.parseDouble(elem.getAttribute("outputal"))));
            }
        }

        return new ResultsInstance(
                Boolean.parseBoolean(root.getAttribute("partial")),
                Boolean.parseBoolean(root.getAttribute("meritAssignment")),
                Boolean.parseBoolean(root.getAttribute("inputSat")),
                Double.parseDouble(root.getAttribute("inputCost")),
                Double.parseDouble(root.getAttribute("outputCost")),
                items);
    }

    private void toStreamResult(StreamResult target) {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element root = doc.createElement("synthesis");
            doc.appendChild(root);

            root.setAttribute("partialSolution", Boolean.toString(partialSolution));
            root.setAttribute("meritAssignment", Boolean.toString(meritAssignment));
            root.setAttribute("inputSat", Boolean.toString(inputSat));
            root.setAttribute("inputCost", Double.toString(inputCost));
            root.setAttribute("outputCost", Double.toString(outputCost));

            for (Item item : items) {
                Element elem = doc.createElement("item");
                root.appendChild(elem);

                elem.setAttribute("component", item.component);
                elem.setAttribute("defenseProperty", item.defenseProperty);
                elem.setAttribute("inputDal", Integer.toString(item.inputDal));
                elem.setAttribute("outputDal", Integer.toString(item.outputDal));
                elem.setAttribute("inputCost", Double.toString(item.inputCost));
                elem.setAttribute("outputCost", Double.toString(item.outputCost));
            }

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(doc), target);
        } catch (ParserConfigurationException | TransformerException e) {
            e.printStackTrace();
        }
    }

    public void toFile(File file) {
        toStreamResult(new StreamResult(file));
    }

    public void toStream(PrintStream stream) {
        toStreamResult(new StreamResult(stream));
    }
}
