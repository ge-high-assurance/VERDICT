package com.ge.verdict.synthesis;

import com.ge.verdict.synthesis.dtree.DLeaf;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class DLeafTest {
    @Test
    public void testUnique() {
        DLeaf leaf1 = DLeaf.createIfNeeded("A", "A", "A", 0);
        DLeaf leaf1Dup = DLeaf.createIfNeeded("A", "A", "A", 0);
        DLeaf leaf2 = DLeaf.createIfNeeded("A", "B", "A", 0);
        DLeaf leaf3 = DLeaf.createIfNeeded("B", "A", "A", 0);

        // should be aliases because the whole point is to uniquely identify instances
        Assertions.assertThat(leaf1 == leaf1Dup).isTrue();
        Assertions.assertThat(leaf2 == leaf1).isFalse();
        Assertions.assertThat(leaf3 == leaf1).isFalse();
    }

    @Test
    public void testLookup() {
        DLeaf leaf1 = DLeaf.createIfNeeded("A", "A", "A", 0);
        DLeaf leaf2 = DLeaf.createIfNeeded("B", "B", "A", 0);

        Assertions.assertThat(DLeaf.fromId(leaf1.id) == leaf1).isTrue();
        Assertions.assertThat(DLeaf.fromId(leaf2.id) == leaf2).isTrue();
        Assertions.assertThat(DLeaf.fromId(leaf1.id) == leaf2).isFalse();
    }
}
