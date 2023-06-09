package com.ge.verdict.vdm.synthesis;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.math3.fraction.Fraction;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * The result of the synthesis operation. This class is serializable and deserializable to XML. It
 * is located in the vdm project so that it can be shared between the bundle and plugin projects.
 * This class is also fully immutable.
 */
public class ResultsInstance {
    /** Whether synthesis was performed with partial solutions enabled. */
    public final boolean partialSolution;

    /** Whether synthesis was performed with merit assignment. */
    public final boolean meritAssignment;

    /** Whether the input to synthesis was SAT. */
    public final boolean inputSat;

    /** The total input cost (zero if not using partial solutions). */
    public final Fraction inputCost;

    /** The total output cost of all recommended items. */
    public final Fraction outputCost;

    /** The list of recommendations. */
    public final List<Item> items;

    // these are the names of the XML tags

    private static final String ROOT_TAG = "synthesis";
    private static final String ROOT_PARTIAL_SOLUTION = "partialSolution";
    private static final String ROOT_MERIT_ASSIGNMENT = "meritAssignment";
    private static final String ROOT_INPUT_SAT = "inputSat";
    private static final String ROOT_INPUT_COST = "inputCost";
    private static final String ROOT_OUTPUT_COST = "outputCost";

    private static final String ITEM_TAG = "item";
    private static final String ITEM_COMPONENT = "component";
    private static final String ITEM_DEFENSE_PROPERTY = "defenseProperty";
    private static final String ITEM_INPUT_DAL = "inputDal";
    private static final String ITEM_OUTPUT_DAL = "outputDal";
    private static final String ITEM_INPUT_COST = "inputCost";
    private static final String ITEM_OUTPUT_COST = "outputCost";

    /** A single synthesis recommendation. */
    public static class Item {
        /** The component/connection to which a recommendation applies. */
        public final String component;

        /** The defense property to which a recommendation applies. */
        public final String defenseProperty;

        /** The DAL of this component-defense pair in the synthesis input. */
        public final int inputDal;

        /** The recommended output DAL of this component-defense pair. */
        public final int outputDal;

        /**
         * The cost corresponding to the DAL of this component-defense pair in the synthesis input.
         */
        public final Fraction inputCost;

        /** The cost corresponding to the recommended output DAL of this component-defense pair. */
        public final Fraction outputCost;

        public Item(
                String component,
                String defenseProperty,
                int inputDal,
                int outputDal,
                Fraction inputCost,
                Fraction outputCost) {
            super();
            this.component = component;
            this.defenseProperty = defenseProperty;
            this.inputDal = inputDal;
            this.outputDal = outputDal;
            this.inputCost = inputCost;
            this.outputCost = outputCost;
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    component, defenseProperty, inputDal, outputDal, inputCost, outputCost);
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Item) {
                Item otherItem = (Item) other;
                return component.equals(otherItem.component)
                        && defenseProperty.equals(otherItem.defenseProperty)
                        && inputDal == otherItem.inputDal
                        && outputDal == otherItem.outputDal
                        && inputCost.equals(otherItem.inputCost)
                        && outputCost.equals(otherItem.outputCost);
            }
            return false;
        }
    }

    public ResultsInstance(
            boolean partialSolution,
            boolean meritAssignment,
            boolean inputSat,
            Fraction inputCost,
            Fraction outputCost,
            List<Item> items) {
        super();
        this.partialSolution = partialSolution;
        this.meritAssignment = meritAssignment;
        this.inputSat = inputSat;
        this.inputCost = inputCost;
        this.outputCost = outputCost;
        this.items = Collections.unmodifiableList(items);
    }

    /**
     * Deserialize a results instance from an XML file.
     *
     * @param file the XML file
     * @return the deserialized results instance
     * @throws SAXException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    public static ResultsInstance fromFile(File file)
            throws SAXException, IOException, ParserConfigurationException {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
        Element root = doc.getDocumentElement();

        List<Item> items = new ArrayList<>();

        NodeList nodes = root.getElementsByTagName(ITEM_TAG);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element) {
                Element elem = (Element) node;
                items.add(
                        new Item(
                                elem.getAttribute(ITEM_COMPONENT),
                                elem.getAttribute(ITEM_DEFENSE_PROPERTY),
                                Integer.parseInt(elem.getAttribute(ITEM_INPUT_DAL)),
                                Integer.parseInt(elem.getAttribute(ITEM_OUTPUT_DAL)),
                                parseCost(elem.getAttribute(ITEM_INPUT_COST)),
                                parseCost(elem.getAttribute(ITEM_OUTPUT_COST))));
            }
        }

        return new ResultsInstance(
                Boolean.parseBoolean(root.getAttribute(ROOT_PARTIAL_SOLUTION)),
                Boolean.parseBoolean(root.getAttribute(ROOT_MERIT_ASSIGNMENT)),
                Boolean.parseBoolean(root.getAttribute(ROOT_INPUT_SAT)),
                parseCost(root.getAttribute(ROOT_INPUT_COST)),
                parseCost(root.getAttribute(ROOT_OUTPUT_COST)),
                items);
    }

    /**
     * Serialize this results instance to a stream target.
     *
     * @param target the stream target
     */
    private void toStreamResult(StreamResult target) {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element root = doc.createElement(ROOT_TAG);
            doc.appendChild(root);

            root.setAttribute(ROOT_PARTIAL_SOLUTION, Boolean.toString(partialSolution));
            root.setAttribute(ROOT_MERIT_ASSIGNMENT, Boolean.toString(meritAssignment));
            root.setAttribute(ROOT_INPUT_SAT, Boolean.toString(inputSat));
            root.setAttribute(ROOT_INPUT_COST, Double.toString(inputCost.doubleValue()));
            root.setAttribute(ROOT_OUTPUT_COST, Double.toString(outputCost.doubleValue()));

            for (Item item : items) {
                Element elem = doc.createElement(ITEM_TAG);
                root.appendChild(elem);

                elem.setAttribute(ITEM_COMPONENT, item.component);
                elem.setAttribute(ITEM_DEFENSE_PROPERTY, item.defenseProperty);
                elem.setAttribute(ITEM_INPUT_DAL, Integer.toString(item.inputDal));
                elem.setAttribute(ITEM_OUTPUT_DAL, Integer.toString(item.outputDal));
                elem.setAttribute(ITEM_INPUT_COST, Double.toString(item.inputCost.doubleValue()));
                elem.setAttribute(ITEM_OUTPUT_COST, Double.toString(item.outputCost.doubleValue()));
            }

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(doc), target);
        } catch (ParserConfigurationException | TransformerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Serialize this results instance to an XML file.
     *
     * @param file the output XML file
     */
    public void toFileXml(File file) {
        toStreamResult(new StreamResult(file));
    }

    /**
     * Serialize this results instance as XML to a print stream (e.g. System.out).
     *
     * @param stream the print stream
     */
    public void toStreamXml(PrintStream stream) {
        toStreamResult(new StreamResult(stream));
    }

    /**
     * Pretty print this results instance to a print stream (e.g. System.out) in human-readable
     * form.
     *
     * @param stream the print stream
     */
    public void prettyPrint(PrintStream stream) {
        int maxLength =
                Math.min(
                        items.stream()
                                .map(
                                        item ->
                                                item.component.length()
                                                        + item.defenseProperty.length())
                                .reduce(0, Math::max),
                        50);

        for (Item item : items) {
            int extraSpaces =
                    Math.max(
                            maxLength - item.component.length() - item.defenseProperty.length(), 0);
            stream.println(
                    "("
                            + item.component
                            + ", "
                            + item.defenseProperty
                            + "): "
                            + new String(new char[extraSpaces]).replace("\0", " ")
                            + "DAL "
                            + item.inputDal
                            + " (cost "
                            + item.inputCost
                            + ") --> DAL "
                            + item.outputDal
                            + " (cost "
                            + item.outputCost
                            + ")");
        }

        stream.println();
        stream.println(
                "Input parameters - partialSolution: "
                        + partialSolution
                        + ", meritAssignment: "
                        + meritAssignment
                        + ", inputSat: "
                        + inputSat);
        stream.println("Total cost: " + inputCost + " --> " + outputCost);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                partialSolution, meritAssignment, inputSat, inputCost, outputCost, items);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof ResultsInstance) {
            ResultsInstance otherRes = (ResultsInstance) other;
            return partialSolution == otherRes.partialSolution
                    && meritAssignment == otherRes.meritAssignment
                    && inputSat == otherRes.inputSat
                    && inputCost.equals(otherRes.inputCost)
                    && outputCost.equals(otherRes.outputCost)
                    && items.equals(otherRes.items);
        }
        return false;
    }

    /**
     * Parses a decimal cost as a fraction.
     *
     * @param costStr the decimal in string representation
     * @return the parsed fractional cost
     */
    public static Fraction parseCost(String costStr) {
        return new Fraction(Double.parseDouble(costStr), 0.000001, 20);
    }
}
