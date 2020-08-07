package com.ge.verdict.synthesis;

import com.ge.verdict.synthesis.dtree.DLeaf;
import org.apache.commons.math3.fraction.Fraction;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class DLeafTest {
    @Test
    public void testUnique() {
        DLeaf.Factory factory = new DLeaf.Factory();

        Fraction[] costs = Util.fractionCosts(new double[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0});

        DLeaf leaf1 = new DLeaf("A", "A", "A", 0, 1, costs, factory);
        DLeaf leaf1Dup = new DLeaf("A", "A", "A", 0, 1, costs, factory);
        DLeaf leaf2 = new DLeaf("A", "B", "A", 0, 1, costs, factory);
        DLeaf leaf3 = new DLeaf("B", "A", "A", 0, 1, costs, factory);

        // should be aliases because the whole point is to uniquely identify instances
        Assertions.assertThat(leaf1.componentDefense == leaf1Dup.componentDefense).isTrue();
        Assertions.assertThat(leaf2.componentDefense == leaf1.componentDefense).isFalse();
        Assertions.assertThat(leaf3.componentDefense == leaf1.componentDefense).isFalse();
    }

    @Test
    public void testLookup() {
        DLeaf.Factory factory = new DLeaf.Factory();

        Fraction[] costs = Util.fractionCosts(new double[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0});

        DLeaf leaf1 = new DLeaf("A", "A", "A", 0, 1, costs, factory);
        DLeaf leaf2 = new DLeaf("B", "B", "A", 0, 1, costs, factory);

        Assertions.assertThat(factory.fromId(leaf1.componentDefense.id) == leaf1.componentDefense)
                .isTrue();
        Assertions.assertThat(factory.fromId(leaf2.componentDefense.id) == leaf2.componentDefense)
                .isTrue();
        Assertions.assertThat(factory.fromId(leaf1.componentDefense.id) == leaf2.componentDefense)
                .isFalse();
    }

    @Test
    public void testMultipleRequirements() {
        DLeaf.Factory factory = new DLeaf.Factory();

        Fraction[] costs = Util.fractionCosts(new double[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0});

        DLeaf leaf1 = new DLeaf("A", "A", "A", 0, 1, costs, factory);
        DLeaf leaf2 = new DLeaf("A", "A", "A", 0, 2, costs, factory);

        Assertions.assertThat(leaf1.targetDal).isEqualTo(1);
        Assertions.assertThat(leaf2.targetDal).isEqualTo(2);
    }
}
