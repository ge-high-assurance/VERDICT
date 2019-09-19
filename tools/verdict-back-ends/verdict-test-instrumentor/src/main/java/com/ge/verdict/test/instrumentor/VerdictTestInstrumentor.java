/* See LICENSE in project directory */
package com.ge.verdict.test.instrumentor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import verdict.vdm.vdm_lustre.ContractItem;
import verdict.vdm.vdm_lustre.Expression;
import verdict.vdm.vdm_model.ComponentImpl;
import verdict.vdm.vdm_model.ComponentInstance;
import verdict.vdm.vdm_model.ComponentType;
import verdict.vdm.vdm_model.Model;

public class VerdictTestInstrumentor {
    private Model model;

    public VerdictTestInstrumentor(Model model) {
        this.model = model;
    }

    /**
     * Note: presently only supports one top-level system. If there are multiple, prints a warning
     * and chooses the first one.
     *
     * <p>Also note that instrumentation generates additional system types. But these are added
     * after the one we want. In this case the warning is erroneous.
     *
     * @return the top-level system type in model
     */
    private ComponentType getTopLevelSystemType() {
        Set<ComponentType> subcomps = new HashSet<>();

        for (ComponentImpl impl : model.getComponentImpl()) {
            if (impl.getBlockImpl() != null) {
                for (ComponentInstance comp : impl.getBlockImpl().getSubcomponent()) {
                    if (comp.getSpecification() != null) {
                        subcomps.add(comp.getSpecification());
                    } else if (comp.getImplementation() != null) {
                        subcomps.add(comp.getImplementation().getType());
                    } else {
                        throw new RuntimeException(
                                "ComponentInstance has neither specification nor implementation");
                    }
                }
            }
        }

        List<ComponentType> topLevels = new ArrayList<>();

        for (ComponentType comp : model.getComponentType()) {
            if (!subcomps.contains(comp)) {
                topLevels.add(comp);
            }
        }

        if (topLevels.isEmpty()) {
            throw new RuntimeException("Verdict ATG error: No top-level component found");
        }

        if (topLevels.size() > 1) {
            System.out.println(
                    "Verdict ATG Warning: Multiple top-level systems found, using first one (may be caused by instrumentation)");
        }

        return topLevels.get(0);
    }

    /**
     * Produce one affirmative and one negated guarantee for every cyber requirement in the
     * top-level system of the model.
     */
    public void instrumentTests() {
        ComponentType topLevel = getTopLevelSystemType();
        List<ContractItem> guarantees = topLevel.getContract().getGuarantee();
        List<ContractItem> replaceWith = new ArrayList<>();

        // For every guarantee, produce one positive (equivalent to guarantee)
        // and one negative (logical not of guarantee)

        for (ContractItem guarantee : guarantees) {
            String name = guarantee.getName();

            // Build negated expression
            Expression negExpr = Expression.builder().withNot(guarantee.getExpression()).build();

            ContractItem pos = ContractItem.copyOf(guarantee).withName("pos_" + name).build();
            ContractItem neg =
                    ContractItem.copyOf(guarantee)
                            .withName("neg_" + name)
                            .withExpression(negExpr)
                            .build();

            replaceWith.add(pos);
            replaceWith.add(neg);
        }

        // Remove old guarantees and use our own
        guarantees.clear();
        guarantees.addAll(replaceWith);
    }
}
