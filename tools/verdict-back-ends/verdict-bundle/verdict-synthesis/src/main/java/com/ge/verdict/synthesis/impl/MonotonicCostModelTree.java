package com.ge.verdict.synthesis.impl;

import static javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION;

import com.ge.verdict.synthesis.ICostModel;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.math3.fraction.Fraction;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * A cost model composite tree for cost lookup based on component, defense, dal Supports discrete
 * costs for DALs, enforces monotonic properties
 *
 * <p>Trees will always be in Component, Defense, DAL order but not necessarily perfect e.g.
 * component and defense may be on the same tree level if component is not provided e.g.
 *
 * <p>  default (cost:1) 
 * <p>      ├── Component A (cost:2) 
 * <p>      │        ├── Defense B (cost:4) 
 * <p>      │        │       └── DAL 2 (cost:8) 
 * <p>      │        └── DAL 2 (cost:6) 
 * <p>      ├── Defense G (cost:2) 
 * <p>      │        └── DAL 3 (cost:6) 
 * <p>      └── DAL 5 (cost:6)
 */
public class MonotonicCostModelTree implements ICostModel {

    private static class MonotonicCostTreeException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public MonotonicCostTreeException(final String message) {
            super(message);
        }

        public MonotonicCostTreeException(final Exception parent) {
            super(parent);
        }

        public MonotonicCostTreeException(final String message, final Exception parent) {
            super(message, parent);
        }
    }

    private static final String EMPTY = "";

    private static final String YES = "YES";

    private static final Fraction DEFAULT_COST = Fraction.ONE;

    private static final String DEFAULT_COST_SOURCE = "Default Cost:" + DEFAULT_COST;

    private static final String MISSING_XML = "Missing Cost Model XML";

    private static final String NON_MONOTONIC = "Non-monotonic costs found: %s > %s";
    
    private static final String DAL_RANGE_ERROR = "DAL must be an odd integer between 1 and 10 but found %s";

    private static final String COST_ERROR =
            "Could not calculate cost for component, defense, dal: %s %s %s";

    private static final String DUPLICATE_MODEL = "Duplicate cost model definitions found: %s %s";

    private static final String WRITER_CONFIGURATION_ERROR =
            "Unable to configure xml node to string writer";

    final Map<Integer, MonotonicCostModelTree> dals;
    final Map<String, MonotonicCostModelTree> components;
    final Map<String, MonotonicCostModelTree> defenses;

    // Nodes may have associated costs
    String source;
    Fraction cost;

    MonotonicCostModelTree() {
        dals = new HashMap<>();
        components = new HashMap<>();
        defenses = new HashMap<>();
    }

    public static MonotonicCostModelTreeBuilder.Builder builder() {
        return new MonotonicCostModelTreeBuilder.Builder();
    }

    private Optional<Fraction> getCost() {
        return Optional.ofNullable(this.cost);
    }

    private Optional<String> getSource() {
        return Optional.ofNullable(this.source);
    }

    /**
     * Validate a tree is monotonic: A tree is considered monotonic if all more generalized costs
     * are less or equal to this cost
     *
     * @throws MonotonicCostTreeException on first monotonic violation between cost model
     *     definitions
     */
    private static void validate(
            final MonotonicCostModelTree tree,
            final Map<Integer, MonotonicCostModelTree>
                    predecessorDalCosts // DesignAssuranceLevel:Cost of preceding nodes
            ) {

        if (null == tree) return;

        final Map<Integer, MonotonicCostModelTree> mutCostAcc = new HashMap<>(predecessorDalCosts);
        final Map<Integer, MonotonicCostModelTree> mutTreeDals = new HashMap<>(tree.dals);

        // Consider the base scaling factor if present
        tree.getCost().ifPresent(c -> mutTreeDals.put(0, tree));

        // General DAL costs should not be greater than more specific
        mutTreeDals.forEach(
                (key, value) -> {
                    
                    if(key > 0 && key % 2 == 0){
                        throw new MonotonicCostTreeException(
                                String.format(DAL_RANGE_ERROR, value.getSource().orElse(DEFAULT_COST_SOURCE)));
                    }
                    
                    mutCostAcc.merge(
                            key,
                            value,
                            (v1, v2) -> {

                                if (v1.getCost().orElse(Fraction.ZERO)
                                        .compareTo( value.getCost().orElse(Fraction.ZERO)) > 0) {

                                    throw new MonotonicCostTreeException(
                                            String.format(NON_MONOTONIC, v1.source, value.source));
                                }
                                return v2;
                            });
                });

        /* Sequential DALs should have sequential costs
        var assignment necessary to prevent compiler remove optimizations */
        final boolean prevent_jc_optimization =
                mutCostAcc.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey()) // sort by DAL before comparing costs
                        .reduce(
                                (a, b) -> {
                                    final Fraction aVal =
                                            a.getValue().getCost().orElse(Fraction.ZERO);
                                    final Fraction bVal =
                                            b.getValue().getCost().orElse(Fraction.ZERO);

                                    /* Base scaling factor * (first DAL-2 (next odd)) should not be greater than the first DAL cost */
                                    if (a.getKey() == 0
                                            && aVal.multiply(b.getKey() - 2).compareTo(bVal) > 0) {
                                        throw new MonotonicCostTreeException(
                                                String.format(
                                                        NON_MONOTONIC,
                                                        a.getValue().source,
                                                        b.getValue().source));

                                    } else if (aVal.compareTo(bVal) > 0) {
                                        throw new MonotonicCostTreeException(
                                                String.format(
                                                        NON_MONOTONIC,
                                                        a.getValue().source,
                                                        b.getValue().source));
                                    }

                                    return b;
                                })
                        .isPresent();

        tree.components.forEach((key, value) -> MonotonicCostModelTree.validate(value, mutCostAcc));
        tree.defenses.forEach((key, value) -> MonotonicCostModelTree.validate(value, mutCostAcc));
    }

    /**
     * Merge cost model tree values into this <remarks>not thread safe</remarks>
     *
     * @throws MonotonicCostTreeException if duplicate cost model definitions are found
     */
    private void merge(final MonotonicCostModelTree tree) {
        if (null == tree) return;

        tree.getSource()
                .ifPresent(
                        s -> {
                            // default costs can be overwritten
                            if (getSource().filter(src -> !src.equals(DEFAULT_COST_SOURCE)).isPresent()) {
                                throw new MonotonicCostTreeException(
                                        String.format(DUPLICATE_MODEL, s, this.source));
                            }
                            this.source = s;
                            // source check covers costs since build order is enforced
                            tree.getCost().ifPresent(c -> this.cost = c);
                        });

        tree.dals.forEach(
                (key, value) -> {
                    final MonotonicCostModelTree old = this.dals.put(key, value);
                    if (null != old) {
                        throw new MonotonicCostTreeException(
                                String.format(DUPLICATE_MODEL, value.source, old.source));
                    }
                });

        tree.components.forEach(
                (k, v) -> {
                    if (this.components.containsKey(k)) {
                        this.components.get(k).merge(v);
                    } else {
                        this.components.put(k, v);
                    }
                });
        tree.defenses.forEach(
                (k, v) -> {
                    if (this.defenses.containsKey(k)) {
                        this.defenses.get(k).merge(v);
                    } else {
                        this.defenses.put(k, v);
                    }
                });
    }

    /**
     * Evaluate the cost for a defense definition at a provided design assurance level (DAL) using this
     * cost model tree
     *
     * <p>Cost evaluation order and cost value:
     *
     * <ul>
     *   <li>1. component, defense, dal : cost
     *   <li>2. component, defense : scaling factor (cost) * dal
     *   <li>3. component, dal : cost
     *   <li>4. defense, dal : cost
     *   <li>5. component : scaling factor (cost) * dal
     *   <li>6. defense : scaling factor (cost) * dal
     *   <li>7. dal : cost
     *   <li>8. none : cost * (default) dal
     * </ul>
     * 
     * @param component the component identifier for a defense definition (can be empty)
     * @param defense the defense identifier for a defense definition (can be empty)
     * @param dal design assurance level to compute cost for (required)
     * @throws MonotonicCostTreeException if the cost cannot be computed
     */
    public Fraction getCost(final String defense, final String component, final int dal) {
        /* Synthesis takes the greatest DAL with the least cost to meet a def. req.
            Ceiling to the next odd DAL forces DLeaf synthesis to return a valid (odd) DAL */
        final int oddDal = dal % 2 == 1 || dal == 0 ? dal : dal + 1;
        return evaluateCost(component, defense, oddDal, new HashMap<>())
                .orElseThrow(() ->
                            new MonotonicCostTreeException(
                                    String.format(COST_ERROR, component, defense, dal)));
    }

    private Optional<Fraction> evaluateCost(
            final String component, final String defense, final int dal, 
            final Map<Integer,MonotonicCostModelTree> predecessorDALCostsAcc) {
        
        predecessorDALCostsAcc.putAll(this.dals);
        
        if (null != component && !component.isEmpty() && components.containsKey(component)) {
            return components
                    .get(component)
                    .evaluateCost(EMPTY, defense, dal, predecessorDALCostsAcc)
                    .map(Optional::of)
                    .orElse(this.getCost().map(c -> c.multiply(dal)));
        }

        if (null != defense && !defense.isEmpty() && defenses.containsKey(defense)) {
            return defenses.get(defense)
                    .evaluateCost(EMPTY, EMPTY, dal, predecessorDALCostsAcc)
                    .map(Optional::of)
                    .orElse(this.getCost().map(c -> c.multiply(dal)));
        }

        final Optional<MonotonicCostModelTree> foundDal =
                predecessorDALCostsAcc.entrySet().stream()
                        .sorted((a, b) -> b.getKey().compareTo(a.getKey()))
                        .filter(e -> e.getKey() <= dal)
                        .map(Map.Entry::getValue)
                        .filter(v -> v.getCost().isPresent())
                        .findFirst();

        return foundDal.isPresent()
                ? foundDal.get().getCost()
                : this.getCost().map(c -> c.multiply(dal));
    }

    /** Stringifies an XML element */
    private static String asString(final Element toStringify, final Transformer transformer)
            throws TransformerException {
        final StringWriter sw = new StringWriter();
        transformer.transform(new DOMSource(toStringify), new StreamResult(sw));
        return sw.toString();
    }

    /**
     * Deserializes an XML file to a {@link MonotonicCostModelTree} cost tree
     *
     * @param costModelXml xml file to deserialize
     * @return an {@link MonotonicCostModelTree} representing the xml configured costs
     * @throws MonotonicCostTreeException if the xml configuration could not be deserialized to a
     *     monotonic tree
     */
    public static MonotonicCostModelTree load(final File costModelXml) {
        assert null != costModelXml && costModelXml.exists() : MISSING_XML;
        try {
            final DocumentBuilder parser =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder();

            final Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OMIT_XML_DECLARATION, YES);

            final Document xml = parser.parse(costModelXml);
            final List<Element> elements = ICostModel.extractCostElements(xml.getDocumentElement());
            final MonotonicCostModelTree tree =
                    MonotonicCostModelTree.builder()
                            .withCost(DEFAULT_COST)
                            .withSource(DEFAULT_COST_SOURCE)
                            .build();

            for (final Element rule : elements) {

                ICostModel.validateRuleAttrs(rule);

                final MonotonicCostModelTree node =
                        MonotonicCostModelTree.builder()
                                .withComponent(rule.getAttribute(COMPONENT))
                                .withDefense(rule.getAttribute(DEFENSE))
                                .withDal(rule.getAttribute(DAL))
                                .withCost(rule.getTextContent())
                                .withSource(asString(rule, transformer))
                                .build();
                
                tree.merge(node);
            }
            validate(tree, new HashMap<>());
            return tree;

        } catch (final TransformerException e) {
            throw new MonotonicCostTreeException(WRITER_CONFIGURATION_ERROR, e);
        } catch (final IOException | SAXException | ParserConfigurationException e) {
            throw new MonotonicCostTreeException(e);
        }
    }
}
