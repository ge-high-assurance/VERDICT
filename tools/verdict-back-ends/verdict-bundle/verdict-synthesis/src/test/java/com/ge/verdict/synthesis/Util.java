package com.ge.verdict.synthesis;

import org.apache.commons.math3.fraction.Fraction;

public class Util {

    public static Fraction[] fractionCosts(double[] costs) {
        Fraction[] fractions = new Fraction[costs.length];
        for (int i = 0; i < costs.length; i++) {
            fractions[i] = ICostModel.parseCost(Double.toString(costs[i]));
        }
        return fractions;
    }

    public static Fraction[] fractionCosts(int[] costs) {
        Fraction[] fractions = new Fraction[costs.length];
        for (int i = 0; i < costs.length; i++) {
            fractions[i] = new Fraction(costs[i]);
        }
        return fractions;
    }
}
