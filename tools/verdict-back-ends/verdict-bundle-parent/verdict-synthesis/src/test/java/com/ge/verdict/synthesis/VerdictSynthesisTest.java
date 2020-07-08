package com.ge.verdict.synthesis;

import com.ge.verdict.attackdefensecollector.Prob;
import com.ge.verdict.attackdefensecollector.adtree.ADAnd;
import com.ge.verdict.attackdefensecollector.adtree.ADNot;
import com.ge.verdict.attackdefensecollector.adtree.ADOr;
import com.ge.verdict.attackdefensecollector.adtree.ADTree;
import com.ge.verdict.attackdefensecollector.adtree.Attack;
import com.ge.verdict.attackdefensecollector.adtree.Defense;
import com.ge.verdict.attackdefensecollector.model.CIA;
import com.ge.verdict.attackdefensecollector.model.SystemModel;
import com.ge.verdict.synthesis.VerdictSynthesis.Approach;
import com.ge.verdict.synthesis.dtree.ALeaf;
import com.ge.verdict.synthesis.dtree.DAnd;
import com.ge.verdict.synthesis.dtree.DLeaf;
import com.ge.verdict.synthesis.dtree.DLeaf.ComponentDefense;
import com.ge.verdict.synthesis.dtree.DOr;
import com.ge.verdict.synthesis.dtree.DTree;
import com.ge.verdict.synthesis.util.Pair;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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

        double[] doubleCosts0 = new double[10];
        double[] doubleCosts1 = new double[10];
        double[] doubleCosts2 = new double[10];
        for (int i = 0; i < 10; i++) {
            doubleCosts0[i] = costs[0];
            doubleCosts1[i] = costs[1];
            doubleCosts2[i] = costs[2];
        }

        int targetDal = 1;

        DLeaf cd1 = new DLeaf("C1", "D1", "A1", 0, targetDal, doubleCosts0, factory);
        DLeaf cd2 = new DLeaf("C2", "D2", "A2", 0, targetDal, doubleCosts1, factory);
        DLeaf cd3 = new DLeaf("C3", "D3", "A3", 0, targetDal, doubleCosts2, factory);

        DTree tree = new DOr(new DAnd(cd1, cd2), new DAnd(cd2, cd3), new DAnd(cd1, cd3));

        Optional<Pair<Set<ComponentDefense>, Double>> output =
                VerdictSynthesis.performSynthesisSingle(tree, targetDal, factory, approach);

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
                    .anyMatch(pair -> pair.component.equals(comp));
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

        int targetDal = 1;

        List<ComponentDefense> leaves = new ArrayList<>();
        DLeaf leafA = new DLeaf("A", "A", "A", 0, targetDal, costs, factory, false);
        DLeaf leafB = new DLeaf("B", "B", "B", 0, targetDal, costs, factory, false);
        DLeaf leafC = new DLeaf("C", "C", "C", 0, targetDal, costs, factory, false);
        leaves.add(leafA.componentDefense);
        leaves.add(leafB.componentDefense);
        leaves.add(leafC.componentDefense);

        Assertions.assertThat(leafA.componentDefense.dalToRawCost(targetDal))
                .isEqualByComparingTo(new Fraction(42, 10));
        Assertions.assertThat(leafB.componentDefense.dalToRawCost(targetDal))
                .isEqualByComparingTo(new Fraction(35, 1000));
        Assertions.assertThat(leafC.componentDefense.dalToRawCost(targetDal))
                .isEqualByComparingTo(new Fraction(1077, 100));

        int costLcm = VerdictSynthesis.normalizeCosts(leaves);

        Assertions.assertThat(costLcm).isEqualTo(200);

        Assertions.assertThat(leafA.componentDefense.dalToNormCost(targetDal)).isEqualTo(840);
        Assertions.assertThat(leafB.componentDefense.dalToNormCost(targetDal)).isEqualTo(7);
        Assertions.assertThat(leafC.componentDefense.dalToNormCost(targetDal)).isEqualTo(2154);
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
            Assertions.assertThat(
                            VerdictSynthesis.performSynthesisSingle(dtree, 1, factory, approach))
                    .isEmpty();
        }
    }

    @Test
    public void unmitigatedMixedTest() {
        DLeaf.Factory factory = new DLeaf.Factory();
        SystemModel system = new SystemModel("S1");
        int targetDal = 1;
        double[] costs = {5, 5, 5, 5, 5, 5, 5, 5, 5, 5};
        DLeaf dleaf = new DLeaf("S1", "D1", "A2", 0, targetDal, costs, factory);
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
            Optional<Pair<Set<ComponentDefense>, Double>> result =
                    VerdictSynthesis.performSynthesisSingle(dtree, targetDal, factory, approach);
            Assertions.assertThat(result.isPresent());
            Assertions.assertThat(result.get().left).hasSize(1);
            Assertions.assertThat(result.get().left).contains(dleaf.componentDefense);
            Assertions.assertThat(result.get().right).isEqualTo(5);
        }
    }

    @Test
    public void partialSolutionTest() {
        CostModel costModel =
                new CostModel(new File(getClass().getResource("partialCosts.xml").getPath()));
        int dal = 2;

        SystemModel system = new SystemModel("C1");

        Attack attack1 =
                new Attack(system.getAttackable(), "A1", "An attack", Prob.certain(), CIA.I);
        Defense defense1 = new Defense(attack1);
        defense1.addDefenseClause(
                Collections.singletonList(
                        new Defense.DefenseLeaf(
                                "D1",
                                Optional.of(
                                        new com.ge.verdict.attackdefensecollector.Pair<>(
                                                "D1", 1)))));

        ADTree adtree = new ADOr(new ADAnd(new ADNot(defense1), attack1));

        for (Approach approach : Approach.values()) {
            {
                DLeaf.Factory factoryPartial = new DLeaf.Factory();

                Optional<Pair<Set<ComponentDefense>, Double>> resultPartial =
                        VerdictSynthesis.performSynthesisSingle(
                                DTreeConstructor.construct(
                                        adtree, costModel, dal, true, factoryPartial),
                                dal,
                                factoryPartial,
                                approach);

                Assertions.assertThat(resultPartial.isPresent());
                Assertions.assertThat(resultPartial.get().right).isEqualTo(1);
            }

            {
                DLeaf.Factory factoryTotal = new DLeaf.Factory();

                Optional<Pair<Set<ComponentDefense>, Double>> resultTotal =
                        VerdictSynthesis.performSynthesisSingle(
                                DTreeConstructor.construct(
                                        adtree, costModel, dal, false, factoryTotal),
                                dal,
                                factoryTotal,
                                approach);

                Assertions.assertThat(resultTotal.isPresent());
                Assertions.assertThat(resultTotal.get().right).isEqualTo(2);
            }
        }
    }

    @Test
    public void multipleRequirementsTest() {
        DLeaf.Factory factory = new DLeaf.Factory();
        double[] costs = {2, 5, 9, 15, 16, 18, 20, 25, 30, 37};
        DLeaf leaf1 = new DLeaf("S1", "D1", "A2", 0, 3, costs, factory);
        DLeaf leaf2 = new DLeaf("S1", "D1", "A2", 0, 4, costs, factory);
        DTree dtree = new DAnd(leaf1, leaf2);

        Optional<Pair<List<Pair<ComponentDefense, Integer>>, Double>> result =
                VerdictSynthesis.performSynthesisMultiple(dtree, factory);
        Assertions.assertThat(result.isPresent());
        Assertions.assertThat(result.get().left).hasSize(1);
        Assertions.assertThat(result.get().left).contains(new Pair<>(leaf1.componentDefense, 4));
        Assertions.assertThat(result.get().right).isEqualTo(16);
    }
}
