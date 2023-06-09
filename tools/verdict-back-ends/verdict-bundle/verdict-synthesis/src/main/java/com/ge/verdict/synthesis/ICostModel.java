package com.ge.verdict.synthesis;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.math3.fraction.Fraction;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public interface ICostModel {

    String COMPONENT = "component";

    String DEFENSE = "defense";

    String DAL = "dal";

    String COST = "cost";

    String INFINITE = "INF";

    String COMPONENT_NAME_SEPARATOR = ":::";

    String UNSUPPORTED_DAL = "Unsupported DAL Value: %s";

    String UNSUPPORTED_INF = "Infinite cost not supported";

    String UNSUPPORTED_NEG = "Negative cost not supported: %s";

    String QUALIFIED_NAME_ERROR =
            "Invalid number of parts in qualified component name (should be 2): %s";

    String COST_MODEL_TAG = "Unrecognized cost model tag: %s";

    class CostModelException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public CostModelException(final String message) {
            super(message);
        }
    }

    /* FIXME: remove once fully qualified names are supported */
    static Optional<String> getComponentName(final String qualifiedComponentName) {
        final Optional<String> name = Optional.ofNullable(qualifiedComponentName);
        return name.filter(n -> n.contains(COMPONENT_NAME_SEPARATOR))
                .map(
                        n -> {
                            final String[] parts =
                                    qualifiedComponentName.split(COMPONENT_NAME_SEPARATOR);
                            if (parts.length != 2) {
                                throw new CostModelException(
                                        String.format(
                                                QUALIFIED_NAME_ERROR, qualifiedComponentName));
                            }
                            return Optional.of(parts[1]);
                        })
                .orElse(name)
                .filter(s -> !s.isEmpty());
    }

    /** Parse a DAL string into an integer. */
    static int parseDal(final String dalStr) {
        final int dal = Integer.parseInt(dalStr);
        if (dal < 0 || dal > 9) {
            throw new CostModelException(String.format(UNSUPPORTED_DAL, dalStr));
        }
        return dal;
    }

    /** Parse a decimal cost string into a fraction. */
    static Fraction parseCost(final String costStr) {
        if (INFINITE.equals(costStr)) {
            throw new CostModelException(UNSUPPORTED_INF);
        }
        final double costDouble = Double.parseDouble(costStr);
        if (costDouble < 0) {
            throw new CostModelException(String.format(UNSUPPORTED_NEG, costStr));
        }
        return new Fraction(costDouble, 0.000001, 20); // mitigate floating point error
    }

    static List<Element> extractCostElements(final Element xml) {
        final NodeList nodeList = xml.getElementsByTagName(COST);
        return IntStream.range(0, nodeList.getLength())
                .mapToObj(nodeList::item)
                .map(n -> n instanceof Element ? (Element) n : null)
                .collect(Collectors.toList());
    }

    static void validateRuleAttrs(final Element rule) {
        final int attrLength = rule.getAttributes().getLength();
        for (int i = 0; i < attrLength; i++) {
            final String name = rule.getAttributes().item(i).getNodeName();
            if (!COMPONENT.equals(name) && !DEFENSE.equals(name) && !DAL.equals(name)) {
                throw new CostModelException(String.format(COST_MODEL_TAG, name));
            }
        }
    }

    Fraction getCost(final String defense, final String component, final int dal);
}
