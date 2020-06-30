package com.ge.verdict.synthesis;

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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CostModel {
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
    private Map<Triple<String, String, Integer>, Double> compDefDalModel;
    private Map<Pair<String, String>, Double> compDefModel;
    private Map<Pair<String, Integer>, Double> compDalModel;
    private Map<Pair<String, Integer>, Double> defDalModel;
    private Map<String, Double> compModel;
    private Map<String, Double> defModel;
    private Map<Integer, Double> dalModel;
    private double defaultModel;

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
        defaultModel = 1;

        load(costModelXml);
    }

    /**
     * Evaluate the cost function on the given inputs.
     *
     * @param defense
     * @param component
     * @param dal
     * @return
     */
    public double cost(String defense, String component, int dal) {
        // TODO currently no default linear scaling of DAL

        Double lookup = compDefDalModel.get(new Triple<>(component, defense, dal));
        if (lookup != null) {
            return lookup;
        }
        lookup = compDefModel.get(new Pair<>(component, defense));
        if (lookup != null) {
            return lookup;
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
            return lookup;
        }
        lookup = defModel.get(defense);
        if (lookup != null) {
            return lookup;
        }
        lookup = dalModel.get(dal);
        if (lookup != null) {
            return lookup;
        }
        return defaultModel;
    }

    /** Print the cost function. Used for diagnostic purposes. */
    public void printMap() {
        for (Entry<Triple<String, String, Integer>, Double> mapping : compDefDalModel.entrySet()) {
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
        for (Entry<Pair<String, String>, Double> mapping : compDefModel.entrySet()) {
            System.out.println(
                    "map "
                            + mapping.getKey().left
                            + ", "
                            + mapping.getKey().right
                            + " to "
                            + mapping.getValue());
        }
        for (Entry<Pair<String, Integer>, Double> mapping : compDalModel.entrySet()) {
            System.out.println(
                    "map "
                            + mapping.getKey().left
                            + ", "
                            + mapping.getKey().right
                            + " to "
                            + mapping.getValue());
        }
        for (Entry<Pair<String, Integer>, Double> mapping : defDalModel.entrySet()) {
            System.out.println(
                    "map "
                            + mapping.getKey().left
                            + ", "
                            + mapping.getKey().right
                            + " to "
                            + mapping.getValue());
        }
        for (Entry<String, Double> mapping : compModel.entrySet()) {
            System.out.println("map " + mapping.getKey() + " to " + mapping.getValue());
        }
        for (Entry<String, Double> mapping : defModel.entrySet()) {
            System.out.println("map " + mapping.getKey() + " to " + mapping.getValue());
        }
        for (Entry<Integer, Double> mapping : dalModel.entrySet()) {
            System.out.println("map " + mapping.getKey() + " to " + mapping.getValue());
        }
        System.out.println("default: " + defaultModel);
    }

    private boolean isEmptyStr(String str) {
        return str.length() == 0;
    }

    private void load(File costModelXml) {
        try {
            DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document xml = parser.parse(costModelXml);
            for (Element rule : extractElements(xml.getDocumentElement(), "cost")) {
                String component = rule.getAttribute("component");
                String defense = rule.getAttribute("defense");
                String dalStr = rule.getAttribute("dal");

                for (int i = 0; i < rule.getAttributes().getLength(); i++) {
                    String name = rule.getAttributes().item(i).getNodeName();
                    if (!name.equals("component")
                            && !name.equals("defense")
                            && !name.equals("dal")) {
                        throw new ParseException("Unrecognized tag: " + name);
                    }
                }

                double cost = parseCost(rule.getTextContent());

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

    private static int parseDal(String dalStr) {
        int dal = Integer.parseInt(dalStr);
        if (dal < 0 || dal > 9) {
            throw new ParseException("invalid DAL: " + dalStr);
        }
        return dal;
    }

    private static double parseCost(String costStr) {
        if ("INF".equals(costStr)) {
            throw new ParseException("INF not supported yet");
        }
        double cost = Double.parseDouble(costStr);
        if (cost < 0) {
            throw new ParseException("negative cost: " + costStr);
        }
        return cost;
    }
}
