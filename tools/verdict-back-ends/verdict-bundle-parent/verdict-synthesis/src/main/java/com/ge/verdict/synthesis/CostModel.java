package com.ge.verdict.synthesis;

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

    private Map<Triple<String, String, Integer>, Integer> model;

    /**
     * Load the cost model from the given XML file.
     *
     * @param costModelXml
     */
    public CostModel(File costModelXml) {
        model = new LinkedHashMap<>();
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
    public int cost(String defense, String component, int dal) {
        Integer lookup = model.get(new Triple<>(defense, component, dal));
        // default to linear model
        return lookup != null ? lookup : dal;
    }

    /** Print the cost function. Used for diagnostic purposes. */
    public void printMap() {
        for (Entry<Triple<String, String, Integer>, Integer> mapping : model.entrySet()) {
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
    }

    private void load(File costModelXml) {
        try {
            DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document xml = parser.parse(costModelXml);
            for (Element rule : extractElements(xml.getDocumentElement(), "cost")) {
                String defense = rule.getAttribute("defense");
                String component = rule.getAttribute("component");
                int dal = parseDal(rule.getAttribute("dal"));
                int cost = parseCost(rule.getTextContent());
                model.put(new Triple<>(defense, component, dal), cost);
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

    private static int parseCost(String costStr) {
        if ("INF".equals(costStr)) {
            throw new ParseException("INF not supported yet");
        }
        int cost = Integer.parseInt(costStr);
        if (cost < 0) {
            throw new ParseException("negative cost: " + costStr);
        }
        return cost;
    }
}
