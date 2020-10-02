package com.ge.verdict.synthesis;

import java.io.File;
import org.apache.commons.math3.fraction.Fraction;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class CostModelTest {
    @Test
    public void testLoad() {
        CostModel costs =
                new CostModel(new File(getClass().getResource("sampleCosts.xml").getPath()));
        Assertions.assertThat(costs.cost("D1", "C1", 1)).isEqualTo(new Fraction(1));
        Assertions.assertThat(costs.cost("D1", "C1", 2)).isEqualTo(new Fraction(2));
        Assertions.assertThat(costs.cost("D2", "C2", 3)).isEqualTo(new Fraction(3));
    }

    @Test
    public void testDefaults() {
        CostModel costs =
                new CostModel(new File(getClass().getResource("defaultCosts.xml").getPath()));
        Assertions.assertThat(costs.cost("A", "A", 1)).isEqualTo(new Fraction(16));
        Assertions.assertThat(costs.cost("A", "B", 1)).isEqualTo(new Fraction(15));
        Assertions.assertThat(costs.cost("B", "A", 1)).isEqualTo(new Fraction(14));
        Assertions.assertThat(costs.cost("A", "A", 2)).isEqualTo(new Fraction(26));
        Assertions.assertThat(costs.cost("B", "B", 1)).isEqualTo(new Fraction(12));
        Assertions.assertThat(costs.cost("A", "B", 2)).isEqualTo(new Fraction(22));
        Assertions.assertThat(costs.cost("B", "A", 2)).isEqualTo(new Fraction(20));
        Assertions.assertThat(costs.cost("B", "B", 2)).isEqualTo(new Fraction(18));
    }
}
