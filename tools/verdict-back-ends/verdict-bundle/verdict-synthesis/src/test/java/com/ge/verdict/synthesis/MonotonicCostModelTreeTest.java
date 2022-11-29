package com.ge.verdict.synthesis;

import static org.apache.commons.math3.fraction.Fraction.ZERO;

import com.ge.verdict.synthesis.impl.MonotonicCostModelTree;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class MonotonicCostModelTreeTest {

    private static final String NON_MONOTONIC_PREF = "Non-monotonic";
    private static final String[] xmlAttrs =
            new String[] {" component=\"%s\"", " defense=\"%s\"", " dal=\"%s\"", ">%s</cost>"};

    private static File getTempFile(final String body) throws IOException {
        final File tmpFile = File.createTempFile("MonotonicTree", "Tests");
        tmpFile.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(tmpFile);
        fos.write(body.getBytes(StandardCharsets.UTF_8));
        return tmpFile;
    }

    /* attrs = {comp,def,dal,cost}; null to exclude */
    private static String toXmlStr(final String[] attrs) {
        return "<cost"
                + IntStream.range(0, attrs.length)
                        .filter(i -> null != attrs[i])
                        .mapToObj(i -> String.format(xmlAttrs[i], attrs[i]))
                        .collect(Collectors.joining());
    }

    private static File toXmlFile(final String[][] elements) {
        try {
            return getTempFile(
                    "<cost-model>"
                            + Arrays.stream(elements)
                                    .map(MonotonicCostModelTreeTest::toXmlStr)
                                    .collect(Collectors.joining())
                            + "</cost-model>");
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testDefaultCosts() {
        IntStream.range(1, 12)
                .forEach(
                        i -> {
                            final File payload =
                                    toXmlFile(
                                            new String[][] {{null, null, null, String.valueOf(i)}});
                            final ICostModel tree = MonotonicCostModelTree.load(payload);
                            Assertions.assertThat(tree.getCost("", "", 1).intValue()).isEqualTo(i);
                            Assertions.assertThat(tree.getCost("", "", 5).intValue())
                                    .isEqualTo(i * 5);
                            Assertions.assertThat(tree.getCost("", "", 9).intValue())
                                    .isEqualTo(i * 9);
                        });
    }

    @Test
    public void testComponentCosts() {
        IntStream.range(1, 10)
                .filter(i -> i % 2 == 1)
                .forEach(
                        i -> {
                            final String v = String.valueOf(i);
                            final File payload = toXmlFile(new String[][] {{v, null, null, v}});
                            final ICostModel tree = MonotonicCostModelTree.load(payload);
                            Assertions.assertThat(tree.getCost("", v, 1).intValue()).isEqualTo(i);
                            Assertions.assertThat(tree.getCost("", "", i).intValue()).isEqualTo(i);
                            Assertions.assertThat(tree.getCost("", "", 9).intValue()).isEqualTo(9);
                        });
    }

    @Test
    public void testDefenseCosts() {
        IntStream.range(1, 10)
                .filter(i -> i % 2 == 1)
                .forEach(
                        i -> {
                            final String v = String.valueOf(i);
                            final File payload = toXmlFile(new String[][] {{v, v, null, v}});
                            final ICostModel tree = MonotonicCostModelTree.load(payload);
                            Assertions.assertThat(tree.getCost(v, v, 1).intValue()).isEqualTo(i);
                            Assertions.assertThat(tree.getCost(v, "", i).intValue()).isEqualTo(i);
                            Assertions.assertThat(tree.getCost("", "", 9).intValue()).isEqualTo(9);
                        });
    }

    @Test
    public void testDalCosts() {
        IntStream.range(1, 10)
                .filter(i -> i % 2 == 1)
                .forEach(
                        i -> {
                            final String v = String.valueOf(i);
                            final File payload = toXmlFile(new String[][] {{v, v, v, v}});
                            final ICostModel tree = MonotonicCostModelTree.load(payload);
                            Assertions.assertThat(tree.getCost(v, v, 1).intValue()).isEqualTo(1);
                            Assertions.assertThat(tree.getCost(v, v, i).intValue()).isEqualTo(i);
                            Assertions.assertThat(tree.getCost(v, "", 5).intValue()).isEqualTo(5);
                            Assertions.assertThat(tree.getCost("", "", 9).intValue()).isEqualTo(9);
                        });
    }

    @Test
    public void testGetCostDal0() { // DAL zero is a scaling multiplier, not a default value
        final File payload = toXmlFile(new String[][] {{"COMP", "DEF", "", "40"}});
        final ICostModel tree = MonotonicCostModelTree.load(payload);
        Assertions.assertThat(tree.getCost("DEF", "COMP", 0)).isEqualTo(ZERO);
    }

    @Test
    public void testFallbackCosts() {
        final File payload =
                toXmlFile(
                        new String[][] {
                            {"COMP", "DEF", "9", "900"},
                            {"COMP", "DEF", "3", "200"},
                            {"COMP", "DEF", "", "100"},
                            {"COMP", "", "7", "600"},
                            {"COMP", "", "", "50"},
                            {"", "DEF", "7", "300"},
                            {"", "DEF", "3", "180"},
                            {"", "DEF", "", "40"},
                            {"", "", "7", "300"},
                            {"", "", "", "20"}
                        });

        final ICostModel tree = MonotonicCostModelTree.load(payload);
        Assertions.assertThat(tree.getCost("DEF", "COMP", 9).intValue()).isEqualTo(900);
        Assertions.assertThat(tree.getCost("DEF", "COMP", 5).intValue()).isEqualTo(200);
        Assertions.assertThat(tree.getCost("DEF", "COMP", 3).intValue()).isEqualTo(200);
        Assertions.assertThat(tree.getCost("DEF", "COMP", 1).intValue()).isEqualTo(100);

        Assertions.assertThat(tree.getCost("", "COMP", 9).intValue()).isEqualTo(600);
        Assertions.assertThat(tree.getCost("", "COMP", 5).intValue()).isEqualTo(250);
        Assertions.assertThat(tree.getCost("", "COMP", 3).intValue()).isEqualTo(50 * 3);
        Assertions.assertThat(tree.getCost("", "COMP", 1).intValue()).isEqualTo(50);

        Assertions.assertThat(tree.getCost("DEF", "", 9).intValue()).isEqualTo(300);
        Assertions.assertThat(tree.getCost("DEF", "", 7).intValue()).isEqualTo(300);
        Assertions.assertThat(tree.getCost("DEF", "", 3).intValue()).isEqualTo(180);
        Assertions.assertThat(tree.getCost("DEF", "", 1).intValue()).isEqualTo(40);
        Assertions.assertThat(tree.getCost("DEF", "", 0).intValue()).isEqualTo(0);

        Assertions.assertThat(tree.getCost("", "", 9).intValue()).isEqualTo(300);
        Assertions.assertThat(tree.getCost("", "", 7).intValue()).isEqualTo(300);
        Assertions.assertThat(tree.getCost("", "", 3).intValue()).isEqualTo(20 * 3);
        Assertions.assertThat(tree.getCost("", "", 1).intValue()).isEqualTo(20);
    }

    @Test
    public void testEvenDALFailures() {
        final String EXP_STR_ERROR = "DAL must be an odd integer between 1 and 10 but found";
        IntStream.range(1, 10)
                .filter(i -> i % 2 == 0)
                .mapToObj(
                        i -> {
                            final String v = String.valueOf(i);
                            return toXmlFile(new String[][] {{null, null, v, v}});
                        })
                .forEach(
                        payload ->
                                Assertions.assertThatThrownBy(
                                                () -> MonotonicCostModelTree.load(payload))
                                        .hasMessageContaining(EXP_STR_ERROR));
    }

    @Test
    public void testMonotonicFailures() {
        final String[][] payload =
                new String[][] {
                    {"", "", "7", "4"},
                    {"", "DEF", "7", "3"},
                    {"", "DEF", "3", "4"},
                    {"", "", "", "5"},
                    {"COMP", "", "", "4"},
                    {"COMP", "", "7", "3"},
                    {"COMP", "DEF", "", "4"},
                    {"COMP", "DEF", "9", "3"}
                };

        final String ERR_STR = "Expected monotonic error between %s & %s but none thrown";
        // Any sequential pair of the above should throw an error
        IntStream.range(1, payload.length)
                .forEach(
                        i -> {
                            final String[] hd = payload[i - 1];
                            final String[] tl = payload[i];
                            Assertions.assertThatThrownBy(
                                            () ->
                                                    MonotonicCostModelTree.load(
                                                            toXmlFile(new String[][] {tl, hd})),
                                            String.format(
                                                    ERR_STR,
                                                    Arrays.toString(tl),
                                                    Arrays.toString(hd)))
                                    .hasMessageContaining(NON_MONOTONIC_PREF);
                        });
    }

    @Test
    public void testMonotonicNonLinearScaleFactors() {
        final File payload =
                toXmlFile(
                        new String[][] {
                            {"COMP", "DEF", "", "300"},
                            {"COMP", "DEF", "3", "300"},
                            {"COMP", "DEF", "5", "300"},
                            {"COMP", "DEF", "7", "300"},
                            {"COMP", "DEF", "9", "300"},
                        });

        final ICostModel tree = MonotonicCostModelTree.load(payload);
        Assertions.assertThat(tree.getCost("DEF", "COMP", 1).intValue()).isEqualTo(300);
        Assertions.assertThat(tree.getCost("DEF", "COMP", 3).intValue()).isEqualTo(300);
        Assertions.assertThat(tree.getCost("DEF", "COMP", 5).intValue()).isEqualTo(300);
        Assertions.assertThat(tree.getCost("DEF", "COMP", 7).intValue()).isEqualTo(300);
        Assertions.assertThat(tree.getCost("DEF", "COMP", 9).intValue()).isEqualTo(300);
    }

    /* If the base scale factor is changed, subsequent factors should validate against it (enabling costs < 1) */
    @Test
    public void testScaleFactorsLessBase() {
        final File payload =
                toXmlFile(
                        new String[][] {
                            {"", "", "", ".01"},
                            {"COMP", "", "1", ".2"}
                        });

        final ICostModel tree = MonotonicCostModelTree.load(payload);
        Assertions.assertThat(tree.getCost("", "COMP", 1).doubleValue()).isEqualTo(.2);
    }

    /* If a scaling factor for a DAL in a more general def. definition is set it becomes the base scaling factor  */
    @Test
    public void testScaleFactorsFromGeneralDef() {
        final File payload =
                toXmlFile(
                        new String[][] {
                            {"", "", "", ".5"},
                            {"COMP", "", "3", ".5"},
                            {"COMP", "DEF", "7", "1"}
                        });

        final ICostModel tree = MonotonicCostModelTree.load(payload);
        Assertions.assertThat(tree.getCost("DEF", "COMP", 3).doubleValue()).isEqualTo(.5);
        Assertions.assertThat(tree.getCost("DEF", "COMP", 7).doubleValue()).isEqualTo(1);
    }
}
