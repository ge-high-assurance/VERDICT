package com.ge.verdict.synthesis;

import java.io.File;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class CostModelTest {
    @Test
    public void testLoad() {
        CostModel costs =
                new CostModel(new File(getClass().getResource("sampleCosts.xml").getPath()));
        Assertions.assertThat(costs.cost("D1", "C1", 1)).isEqualTo(1);
        Assertions.assertThat(costs.cost("D1", "C1", 2)).isEqualTo(2);
        Assertions.assertThat(costs.cost("D2", "C2", 3)).isEqualTo(3);
    }

    @Test
    public void testDefaults() {
        CostModel costs =
                new CostModel(new File(getClass().getResource("defaultCosts.xml").getPath()));
        Assertions.assertThat(costs.cost("A", "A", 1)).isEqualTo(16);
        Assertions.assertThat(costs.cost("A", "B", 1)).isEqualTo(15);
        Assertions.assertThat(costs.cost("B", "A", 1)).isEqualTo(14);
        Assertions.assertThat(costs.cost("A", "A", 2)).isEqualTo(26);
        Assertions.assertThat(costs.cost("B", "B", 1)).isEqualTo(12);
        Assertions.assertThat(costs.cost("A", "B", 2)).isEqualTo(22);
        Assertions.assertThat(costs.cost("B", "A", 2)).isEqualTo(20);
        Assertions.assertThat(costs.cost("B", "B", 2)).isEqualTo(18);
    }
}
