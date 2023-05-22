package com.ge.verdict.synthesis.impl;

import static com.ge.verdict.synthesis.ICostModel.parseCost;

import com.ge.verdict.synthesis.ICostModel;

import org.apache.commons.math3.fraction.Fraction;

import java.util.Optional;

/** Builder pattern ensuring node order for a {@link MonotonicCostModelTree} */
class MonotonicCostModelTreeBuilder {

    private static final String UNSET_COST = "Cost must be set before source";
    private static final String EMPTY_SOURCE = "Empty cost model element found";

    interface AddDef {
        AddDal withDefense(final String d);
    }

    interface AddDal {
        AddCost withDal(final String cost);
    }

    interface AddCost {
        AddSource withCost(final String cost);
    }

    interface AddSource {
        BuildTree withSource(final String source);
    }

    interface BuildTree {
        MonotonicCostModelTree build();
    }

    /** Enforce build order (Comp, Def, Dal layering) for tree creation */
    static class Builder implements AddDef, AddDal, AddCost, AddSource, BuildTree {

        private final MonotonicCostModelTree treeNode;
        private MonotonicCostModelTree currentNode;

        Builder() {
            this.treeNode = new MonotonicCostModelTree();
            this.currentNode = this.treeNode;
        }

        public AddDef withComponent(final String componentNameStr) {
            ICostModel.getComponentName(componentNameStr)
                    .filter(s -> !s.isEmpty())
                    .ifPresent(
                            c -> {
                                this.currentNode.components.put(c, new MonotonicCostModelTree());
                                this.currentNode = currentNode.components.get(c);
                            });
            return this;
        }

        public AddDal withDefense(final String defenseNameStr) {
            Optional.ofNullable(defenseNameStr)
                    .filter(s -> !s.isEmpty())
                    .ifPresent(
                            d -> {
                                this.currentNode.defenses.put(d, new MonotonicCostModelTree());
                                this.currentNode = currentNode.defenses.get(d);
                            });
            return this;
        }

        public AddCost withDal(final String dalStr) {
            Optional.ofNullable(dalStr)
                    .filter(s -> !s.isEmpty())
                    .map(ICostModel::parseDal)
                    .ifPresent(
                            d -> {
                                this.currentNode.dals.put(d, new MonotonicCostModelTree());
                                this.currentNode = currentNode.dals.get(d);
                            });
            return this;
        }

        public AddSource withCost(final String cost) {
            return this.withCost(parseCost(cost));
        }

        public AddSource withCost(final Fraction cost) {
            this.currentNode.cost = cost;
            return this;
        }

        public BuildTree withSource(final String componentNodeSource) {
            assert null != this.currentNode.cost : UNSET_COST;
            assert null != componentNodeSource && !componentNodeSource.isEmpty() : EMPTY_SOURCE;
            this.currentNode.source = componentNodeSource;
            return this;
        }

        public MonotonicCostModelTree build() {
            return this.treeNode;
        }
    }
}
