package com.ge.verdict.synthesis;

import com.ge.verdict.synthesis.VerdictSynthesis.Approach;
import com.ge.verdict.synthesis.dtree.DAnd;
import com.ge.verdict.synthesis.dtree.DLeaf;
import com.ge.verdict.synthesis.dtree.DOr;
import com.ge.verdict.synthesis.dtree.DTree;
import java.util.Optional;
import java.util.Set;
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

        Optional<Set<DLeaf>> output = VerdictSynthesis.performSynthesis(tree, factory, approach);

        Assertions.assertThat(output.isPresent()).isTrue();
        Assertions.assertThat(output.get().size()).isEqualTo(selected.length);
        for (String comp : selected) {
            Assertions.assertThat(output.get().stream())
                    .withFailMessage(
                            "Expected: "
                                    + stringOfArray(selected)
                                    + ", output: "
                                    + stringOfIterable(output.get())
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
}
