package com.ge.verdict.synthesis;

import com.ge.verdict.attackdefensecollector.Prob;
import com.ge.verdict.attackdefensecollector.adtree.Attack;
import com.ge.verdict.attackdefensecollector.model.CIA;
import com.ge.verdict.attackdefensecollector.model.SystemModel;
import com.ge.verdict.synthesis.VerdictSynthesis.Approach;
import com.ge.verdict.synthesis.dtree.ALeaf;
import com.ge.verdict.synthesis.dtree.DAnd;
import com.ge.verdict.synthesis.dtree.DLeaf;
import com.ge.verdict.synthesis.dtree.DOr;
import com.ge.verdict.synthesis.dtree.DTree;
import com.ge.verdict.synthesis.util.Pair;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.math3.fraction.Fraction;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Arrays;
import org.junit.Test;

public class VerdictSynthesisTest {
    private <T> String stringOfIterable(Iterable<T> it) {
        StringBuilder res = new StringBuilder();
        res.append("[");
        for (T t : it) {
            res.append(t.toString());
            res.append(",");
        }
        res.append("]");
        return res.toString();
    }

    private <T> String stringOfArray(T[] arr) {
        return stringOfIterable(Arrays.asList(arr));
    }

    private void performSynthesisTestInternal(int[] costs, String[] selected, Approach approach) {
        // selected contains component names

        if (costs.length != 3) {
            throw new RuntimeException("hey!");
        }

        DLeaf.Factory factory = new DLeaf.Factory();

        DLeaf cd1 = factory.createIfNeeded("C1", "D1", "A1", costs[0]);
        DLeaf cd2 = factory.createIfNeeded("C2", "D2", "A2", costs[1]);
        DLeaf cd3 = factory.createIfNeeded("C3", "D3", "A3", costs[2]);

        DTree tree = new DOr(new DAnd(cd1, cd2), new DAnd(cd2, cd3), new DAnd(cd1, cd3));

        Optional<Pair<Set<DLeaf>, Double>> output =
                VerdictSynthesis.performSynthesis(tree, factory, approach);

        Assertions.assertThat(output.isPresent()).isTrue();
        Assertions.assertThat(output.get().left.size()).isEqualTo(selected.length);
        for (String comp : selected) {
            Assertions.assertThat(output.get().left.stream())
                    .withFailMessage(
                            "Expected: "
                                    + stringOfArray(selected)
                                    + ", output: "
                                    + stringOfIterable(output.get().left)
                                    + ", does not contain component: "
                                    + comp
                                    + ", approach: "
                                    + approach.toString())
                    .anyMatch(leaf -> leaf.component.equals(comp));
        }
    }

    @Test
    public void performSynthesisTest() {
        for (Approach approach : Approach.values()) {
            performSynthesisTestInternal(new int[] {1, 2, 3}, new String[] {"C1", "C2"}, approach);
            performSynthesisTestInternal(new int[] {3, 2, 1}, new String[] {"C3", "C2"}, approach);
            performSynthesisTestInternal(new int[] {1, 3, 2}, new String[] {"C1", "C3"}, approach);
        }
    }

    @Test
    public void decimalCostsTest() {
        CostModel costs =
                new CostModel(new File(getClass().getResource("decimalCosts.xml").getPath()));

        DLeaf.Factory factory = new DLeaf.Factory();

        List<DLeaf> leaves = new ArrayList<>();
        DLeaf leafA = factory.createIfNeeded("A", "A", "A", costs.cost("A", "A", 1));
        DLeaf leafB = factory.createIfNeeded("B", "B", "B", costs.cost("B", "B", 1));
        DLeaf leafC = factory.createIfNeeded("C", "C", "C", costs.cost("C", "C", 1));
        leaves.add(leafA);
        leaves.add(leafB);
        leaves.add(leafC);

        Assertions.assertThat(leafA.cost).isEqualByComparingTo(new Fraction(42, 10));
        Assertions.assertThat(leafB.cost).isEqualByComparingTo(new Fraction(35, 1000));
        Assertions.assertThat(leafC.cost).isEqualByComparingTo(new Fraction(1077, 100));

        int costLcm = VerdictSynthesis.normalizeCosts(leaves);

        Assertions.assertThat(costLcm).isEqualTo(200);

        Assertions.assertThat(leafA.normalizedCost).isEqualTo(840);
        Assertions.assertThat(leafB.normalizedCost).isEqualTo(7);
        Assertions.assertThat(leafC.normalizedCost).isEqualTo(2154);
    }

    @Test
    public void unmitigatedTest() {
        DLeaf.Factory factory = new DLeaf.Factory();
        SystemModel system = new SystemModel("S1");
        DTree dtree =
                new ALeaf(
                        new Attack(
                                system.getAttackable(), "A1", "An attack", Prob.certain(), CIA.I));

        for (Approach approach : Approach.values()) {
            Assertions.assertThat(VerdictSynthesis.performSynthesis(dtree, factory, approach))
                    .isEmpty();
        }
    }

    @Test
    public void unmitigatedMixedTest() {
        DLeaf.Factory factory = new DLeaf.Factory();
        SystemModel system = new SystemModel("S1");
        DLeaf dleaf = factory.createIfNeeded("S1", "D1", "A2", 5);
        DTree dtree =
                new DOr(
                        new ALeaf(
                                new Attack(
                                        system.getAttackable(),
                                        "A1",
                                        "An attack",
                                        Prob.certain(),
                                        CIA.I)),
                        dleaf);

        for (Approach approach : Approach.values()) {
            Optional<Pair<Set<DLeaf>, Double>> result =
                    VerdictSynthesis.performSynthesis(dtree, factory, approach);
            Assertions.assertThat(result.isPresent());
            Assertions.assertThat(result.get().left).hasSize(1);
            Assertions.assertThat(result.get().left).contains(dleaf);
            Assertions.assertThat(result.get().right).isEqualTo(5);
        }
    }
}
