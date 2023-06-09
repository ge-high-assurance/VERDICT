package com.ge.verdict.synthesis.impl;

import com.ge.verdict.synthesis.ICostModel;
import com.ge.verdict.synthesis.util.Pair;
import com.ge.verdict.synthesis.util.Triple;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.math3.fraction.Fraction;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Represents a cost model loaded from XML. Allows for efficiently looking up the cost of any
 * component-defense-DAL triple.
 */
@Deprecated
public class CostModel implements ICostModel {
    /** Thrown if parsing fails due to an invalid cost model XML. */
    public static class ParseException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public ParseException(String message) {
            super(message);
        }

        public ParseException(Exception parent) {
            super(parent);
        }
    }

    // All combinations of (component, defense, DAL).
    // This whole many maps approach isn't very pretty, but no more
    // elegant approach immediately comes to mind so here we are.
    private Map<Triple<String, String, Integer>, Fraction> compDefDalModel;
    private Map<Pair<String, String>, Fraction> compDefModel;
    private Map<Pair<String, Integer>, Fraction> compDalModel;
    private Map<Pair<String, Integer>, Fraction> defDalModel;
    private Map<String, Fraction> compModel;
    private Map<String, Fraction> defModel;
    private Map<Integer, Fraction> dalModel;
    private Fraction defaultModel;

    /**
     * Load the cost model from the given XML file.
     *
     * @param costModelXml
     */
    public CostModel(File costModelXml) {
        compDefDalModel = new LinkedHashMap<>();
        compDefModel = new LinkedHashMap<>();
        compDalModel = new LinkedHashMap<>();
        defDalModel = new LinkedHashMap<>();
        compModel = new LinkedHashMap<>();
        defModel = new LinkedHashMap<>();
        dalModel = new LinkedHashMap<>();
        defaultModel = new Fraction(1);

        loadModel(costModelXml);
    }

    /**
     * Used for testing.
     *
     * @param costs (component, defense, costs) triples
     */
    @SafeVarargs
    public CostModel(Triple<String, String, Fraction[]>... costs) {
        compDefDalModel = new LinkedHashMap<>();
        compDefModel = new LinkedHashMap<>();
        compDalModel = new LinkedHashMap<>();
        defDalModel = new LinkedHashMap<>();
        compModel = new LinkedHashMap<>();
        defModel = new LinkedHashMap<>();
        dalModel = new LinkedHashMap<>();
        defaultModel = new Fraction(1);

        for (Triple<String, String, Fraction[]> triple : costs) {
            if (triple.right.length != 10) {
                throw new RuntimeException("invalid costs");
            }
            for (int dal = 0; dal < 10; dal++) {
                compDefDalModel.put(
                        new Triple<>(triple.left, triple.middle, dal), triple.right[dal]);
            }
        }
    }

    /**
     * Evaluate the cost function on the given inputs.
     *
     * @param defense
     * @param component
     * @param dal
     * @return
     */
    private Fraction cost(String defense, String component, int dal) {
        // If DAL is not specified, we default to using DAL to linearly scale cost

        // System.out.println("Loading cost: " + defense + ", " + component + ", " + dal);

        Fraction lookup = compDefDalModel.get(new Triple<>(component, defense, dal));
        if (lookup != null) {
            return lookup;
        }
        lookup = compDefModel.get(new Pair<>(component, defense));
        if (lookup != null) {
            return lookup.multiply(dal);
        }
        lookup = compDalModel.get(new Pair<>(component, dal));
        if (lookup != null) {
            return lookup;
        }
        lookup = defDalModel.get(new Pair<>(defense, dal));
        if (lookup != null) {
            return lookup;
        }
        lookup = compModel.get(component);
        if (lookup != null) {
            return lookup.multiply(dal);
        }
        lookup = defModel.get(defense);
        if (lookup != null) {
            return lookup.multiply(dal);
        }
        lookup = dalModel.get(dal);
        if (lookup != null) {
            return lookup;
        }
        return defaultModel.multiply(dal);
    }

    /** Print the cost function. Used for diagnostic purposes. */
    public void printMap() {
        for (Entry<Triple<String, String, Integer>, Fraction> mapping :
                compDefDalModel.entrySet()) {
            System.out.println(
                    "map "
                            + mapping.getKey().left
                            + ", "
                            + mapping.getKey().middle
                            + ", "
                            + mapping.getKey().right
                            + " to "
                            + mapping.getValue());
        }
        for (Entry<Pair<String, String>, Fraction> mapping : compDefModel.entrySet()) {
            System.out.println(
                    "map "
                            + mapping.getKey().left
                            + ", "
                            + mapping.getKey().right
                            + " to "
                            + mapping.getValue());
        }
        for (Entry<Pair<String, Integer>, Fraction> mapping : compDalModel.entrySet()) {
            System.out.println(
                    "map "
                            + mapping.getKey().left
                            + ", "
                            + mapping.getKey().right
                            + " to "
                            + mapping.getValue());
        }
        for (Entry<Pair<String, Integer>, Fraction> mapping : defDalModel.entrySet()) {
            System.out.println(
                    "map "
                            + mapping.getKey().left
                            + ", "
                            + mapping.getKey().right
                            + " to "
                            + mapping.getValue());
        }
        for (Entry<String, Fraction> mapping : compModel.entrySet()) {
            System.out.println("map " + mapping.getKey() + " to " + mapping.getValue());
        }
        for (Entry<String, Fraction> mapping : defModel.entrySet()) {
            System.out.println("map " + mapping.getKey() + " to " + mapping.getValue());
        }
        for (Entry<Integer, Fraction> mapping : dalModel.entrySet()) {
            System.out.println("map " + mapping.getKey() + " to " + mapping.getValue());
        }
        System.out.println("default: " + defaultModel);
    }

    private boolean isEmptyStr(String str) {
        return str.length() == 0;
    }

    /**
     * Loads the cost model from XML.
     *
     * @param costModelXml
     */
    private void loadModel(File costModelXml) {
        try {
            DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document xml = parser.parse(costModelXml);
            for (Element rule : extractElements(xml.getDocumentElement(), "cost")) {
                String component = rule.getAttribute("component");
                String defense = rule.getAttribute("defense");
                String dalStr = rule.getAttribute("dal");

                if (component.contains(":::")) {
                    // TODO this is temporary, until we support fully qualified names throughout the
                    // tool. basically we can just remove this once qualified names are supported
                    String[] parts = component.split(":::");
                    if (parts.length != 2) {
                        throw new RuntimeException(
                                "invalid number of parts in qualified component name (should be 2): "
                                        + component);
                    }
                    component = parts[1];
                }

                for (int i = 0; i < rule.getAttributes().getLength(); i++) {
                    String name = rule.getAttributes().item(i).getNodeName();
                    if (!name.equals("component")
                            && !name.equals("defense")
                            && !name.equals("dal")) {
                        throw new ParseException("Unrecognized tag: " + name);
                    }
                }

                Fraction cost = parseCost(rule.getTextContent());

                if (isEmptyStr(component) && isEmptyStr(defense) && isEmptyStr(dalStr)) {
                    defaultModel = cost;
                } else if (isEmptyStr(component) && isEmptyStr(defense)) {
                    dalModel.put(parseDal(dalStr), cost);
                } else if (isEmptyStr(component) && isEmptyStr(dalStr)) {
                    defModel.put(defense, cost);
                } else if (isEmptyStr(defense) && isEmptyStr(dalStr)) {
                    compModel.put(component, cost);
                } else if (isEmptyStr(component)) {
                    defDalModel.put(new Pair<>(defense, parseDal(dalStr)), cost);
                } else if (isEmptyStr(defense)) {
                    compDalModel.put(new Pair<>(component, parseDal(dalStr)), cost);
                } else if (isEmptyStr(dalStr)) {
                    compDefModel.put(new Pair<>(component, defense), cost);
                } else {
                    compDefDalModel.put(new Triple<>(component, defense, parseDal(dalStr)), cost);
                }
            }
        } catch (IOException | SAXException | ParserConfigurationException e) {
            throw new ParseException(e);
        }
    }

    private static List<Element> extractElements(Element xml, String tag) {
        List<Element> list = new ArrayList<>();
        NodeList nodeList = xml.getElementsByTagName(tag);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element) {
                list.add((Element) node);
            }
        }
        return list;
    }

    /**
     * Parse a DAL string into an integer.
     *
     * @param dalStr
     * @return
     */
    private static int parseDal(String dalStr) {
        int dal = Integer.parseInt(dalStr);
        if (dal < 0 || dal > 9) {
            throw new ParseException("invalid DAL: " + dalStr);
        }
        return dal;
    }

    /**
     * Parse a decimal cost string into a fraction.
     *
     * @param costStr
     * @return
     */
    public static Fraction parseCost(String costStr) {
        if ("INF".equals(costStr)) {
            // the idea is to support a notion of an infinite cost at some point
            throw new RuntimeException("INF not supported yet");
        }
        double costDouble = Double.parseDouble(costStr);
        if (costDouble < 0) {
            throw new RuntimeException("negative cost: " + costStr);
        }
        // this precision should mitigate any floating point error
        return new Fraction(costDouble, 0.000001, 20);
    }

    public static ICostModel load(final File costModelXml) {
        return new CostModel(costModelXml);
    }

    /**
     * Calculate the cost for a specified defense at a specified design assurance level
     *
     * @deprecated This class is no longer acceptable to compute DAL costs
     *     <p>Use {@link MonotonicCostModelTree#getCost(String, String, int)} instead.
     */
    @Override
    public Fraction getCost(final String defense, final String component, int dal) {
        return this.cost(defense, component, dal);
    }
}
