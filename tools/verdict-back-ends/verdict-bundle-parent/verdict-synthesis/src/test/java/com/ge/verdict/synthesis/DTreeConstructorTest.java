package com.ge.verdict.synthesis;

import com.ge.verdict.attackdefensecollector.AttackDefenseCollector;
import com.ge.verdict.attackdefensecollector.Pair;
import com.ge.verdict.attackdefensecollector.Prob;
import com.ge.verdict.attackdefensecollector.adtree.ADAnd;
import com.ge.verdict.attackdefensecollector.adtree.ADNot;
import com.ge.verdict.attackdefensecollector.adtree.ADOr;
import com.ge.verdict.attackdefensecollector.adtree.ADTree;
import com.ge.verdict.attackdefensecollector.adtree.Attack;
import com.ge.verdict.attackdefensecollector.adtree.Defense;
import com.ge.verdict.attackdefensecollector.model.CIA;
import com.ge.verdict.attackdefensecollector.model.CyberReq;
import com.ge.verdict.attackdefensecollector.model.SystemModel;
import com.ge.verdict.synthesis.dtree.ALeaf;
import com.ge.verdict.synthesis.dtree.DAnd;
import com.ge.verdict.synthesis.dtree.DLeaf;
import com.ge.verdict.synthesis.dtree.DNot;
import com.ge.verdict.synthesis.dtree.DOr;
import com.ge.verdict.synthesis.dtree.DTree;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.commons.math3.fraction.Fraction;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class DTreeConstructorTest {
    @Test
    public void testFlattenNot() {
        // Note: comparing the results of prettyPrint() instead of the trees directly
        // because we have not implemented equals for the DTree classes

        DLeaf.Factory factory = new DLeaf.Factory();

        Fraction[] costs = Util.fractionCosts(new double[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0});

        DTree leaf = new DLeaf("A", "A", "A", 0, 1, costs, factory);
        DTree dtree = new DNot(new DNot(leaf));
        Assertions.assertThat(dtree.prepare().get().prettyPrint()).isEqualTo(leaf.prettyPrint());

        DTree dtree2 =
                new DAnd(Collections.singletonList(new DOr(Collections.singletonList(dtree))));
        DTree dtree3 =
                new DAnd(Collections.singletonList(new DOr(Collections.singletonList(leaf))));
        Assertions.assertThat(dtree2.prepare().get().prettyPrint()).isEqualTo(dtree3.prettyPrint());
    }

    @Test
    public void testConstruct() {
        DLeaf.Factory factory = new DLeaf.Factory();

        CostModel dummyCosts =
                new CostModel(new File(getClass().getResource("dummyCosts.xml").getPath()));
        int dal = 5;

        SystemModel system = new SystemModel("S1");

        Attack attack1 =
                new Attack(system.getAttackable(), "A1", "An attack", Prob.certain(), CIA.I);
        Defense defense1 = new Defense(attack1);
        defense1.addDefenseClause(
                Collections.singletonList(new Defense.DefenseLeaf("D1", Optional.empty())));

        ADTree adtree = new ADOr(new ADAnd(new ADNot(defense1), attack1));

        Fraction[] costs = Util.fractionCosts(new double[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9});

        DTree dtree =
                new DAnd(
                        Collections.singletonList(
                                new DOr(
                                        Collections.singletonList(
                                                new DOr(
                                                        Collections.singletonList(
                                                                new DAnd(
                                                                        Collections.singletonList(
                                                                                new DLeaf(
                                                                                        "S1", "D1",
                                                                                        "A1", 0,
                                                                                        dal, costs,
                                                                                        factory)))))))));

        Assertions.assertThat(
                        DTreeConstructor.construct(adtree, dummyCosts, dal, false, false, factory)
                                .prettyPrint())
                .isEqualTo(dtree.prettyPrint());
    }

    @Test
    public void testUnmitigated() {
        DLeaf.Factory factory = new DLeaf.Factory();

        CostModel dummyCosts =
                new CostModel(new File(getClass().getResource("dummyCosts.xml").getPath()));
        int dal = 5;

        SystemModel system = new SystemModel("S1");

        Attack attack1 =
                new Attack(system.getAttackable(), "A1", "An attack", Prob.certain(), CIA.I);

        DTree dtree = new ALeaf(attack1);

        Assertions.assertThat(
                        DTreeConstructor.construct(attack1, dummyCosts, dal, false, false, factory)
                                .prettyPrint())
                .isEqualTo(dtree.prettyPrint());
    }

    @Test
    public void testUnmitigatedMixed() {
        DLeaf.Factory factory = new DLeaf.Factory();

        CostModel dummyCosts =
                new CostModel(new File(getClass().getResource("dummyCosts.xml").getPath()));
        int dal = 5;

        SystemModel system = new SystemModel("S1");

        Attack attack1 =
                new Attack(system.getAttackable(), "A1", "An attack", Prob.certain(), CIA.I);
        Attack attack2 =
                new Attack(system.getAttackable(), "A2", "An attack", Prob.certain(), CIA.I);
        Defense defense1 = new Defense(attack1);
        defense1.addDefenseClause(
                Collections.singletonList(new Defense.DefenseLeaf("D1", Optional.empty())));

        ADTree adtree = new ADOr(new ADNot(defense1), attack1, attack2);

        Fraction[] costs = Util.fractionCosts(new double[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9});

        DTree dtree =
                new DAnd(
                        Arrays.asList(
                                new DOr(
                                        Collections.singletonList(
                                                new DAnd(
                                                        Collections.singletonList(
                                                                new DLeaf(
                                                                        "S1", "D1", "A1", 0, dal,
                                                                        costs, factory))))),
                                new ALeaf(attack2)));

        Assertions.assertThat(
                        DTreeConstructor.construct(adtree, dummyCosts, dal, false, false, factory)
                                .prettyPrint())
                .isEqualTo(dtree.prettyPrint());
    }

    @Test
    public void partialSolutionTest() {
        DLeaf.Factory factory = new DLeaf.Factory();

        CostModel dummyCosts =
                new CostModel(new File(getClass().getResource("dummyCosts.xml").getPath()));
        int dal = 5;

        SystemModel system = new SystemModel("S1");

        Attack attack1 =
                new Attack(system.getAttackable(), "A1", "An attack", Prob.certain(), CIA.I);
        Attack attack2 =
                new Attack(system.getAttackable(), "A2", "An attack", Prob.certain(), CIA.A);
        Defense defense1 = new Defense(attack1);
        defense1.addDefenseClause(
                Collections.singletonList(
                        new Defense.DefenseLeaf("D1", Optional.of(new Pair<>("D1", 3)))));
        Defense defense2 = new Defense(attack2);
        defense2.addDefenseClause(
                Collections.singletonList(new Defense.DefenseLeaf("D2", Optional.empty())));

        ADTree adtree = new ADOr(new ADNot(defense1), attack1, new ADNot(defense2), attack2);

        Fraction[] costs = Util.fractionCosts(new double[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9});

        DTree dtree =
                new DAnd(
                        new DOr(new DAnd(new DLeaf("S1", "D1", "A1", 3, dal, costs, factory))),
                        new DOr(new DAnd(new DLeaf("S1", "D2", "A2", 0, dal, costs, factory))));

        Assertions.assertThat(
                        DTreeConstructor.construct(adtree, dummyCosts, dal, true, false, factory)
                                .prepare()
                                .get()
                                .prettyPrint())
                .isEqualTo(dtree.prettyPrint());
    }

    @Test
    public void multipleRequirementsTest() {
        DLeaf.Factory factory = new DLeaf.Factory();

        CostModel dummyCosts =
                new CostModel(new File(getClass().getResource("dummyCosts.xml").getPath()));

        SystemModel system = new SystemModel("S1");

        Attack attack1 =
                new Attack(system.getAttackable(), "A1", "An attack", Prob.certain(), CIA.I);
        Defense defense1 = new Defense(attack1);
        defense1.addDefenseClause(
                Collections.singletonList(new Defense.DefenseLeaf("D1", Optional.empty())));

        List<AttackDefenseCollector.Result> results =
                Arrays.asList(
                        new AttackDefenseCollector.Result(
                                system,
                                new CyberReq("req1", "mission1", 5, "port1", CIA.I),
                                new ADAnd(new ADNot(defense1), attack1),
                                Prob.certain()),
                        new AttackDefenseCollector.Result(
                                system,
                                new CyberReq("req1", "mission1", 7, "port1", CIA.I),
                                new ADAnd(new ADNot(defense1), attack1),
                                Prob.certain()));

        DTree dtree =
                new DAnd(
                        new DOr(
                                new DOr(
                                        new DAnd(
                                                new DLeaf(
                                                        "S1",
                                                        "D1",
                                                        "A1",
                                                        0,
                                                        5,
                                                        dummyCosts,
                                                        factory,
                                                        false,
                                                        false)))),
                        new DOr(
                                new DOr(
                                        new DAnd(
                                                new DLeaf(
                                                        "S1",
                                                        "D1",
                                                        "A1",
                                                        0,
                                                        7,
                                                        dummyCosts,
                                                        factory,
                                                        false,
                                                        false)))));

        Assertions.assertThat(
                        DTreeConstructor.construct(results, dummyCosts, false, false, factory)
                                .prepare()
                                .get()
                                .prettyPrint())
                .isEqualTo(dtree.prettyPrint());
    }
}
