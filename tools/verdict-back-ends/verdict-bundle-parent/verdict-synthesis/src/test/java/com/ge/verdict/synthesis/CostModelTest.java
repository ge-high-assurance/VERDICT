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
}
