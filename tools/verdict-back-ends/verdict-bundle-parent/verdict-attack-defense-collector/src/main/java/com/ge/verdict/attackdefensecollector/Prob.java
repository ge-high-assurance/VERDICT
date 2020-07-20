package com.ge.verdict.attackdefensecollector;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Objects;

/**
 * A probability, typically represented only as an order of magnitude (e.g. 1, 1e-03, 1e-09).
 *
 * <p>This class does not accurately represent probabilistic calculations because it does not
 * account for event dependence. (For example, the probability of the disjunction of two independent
 * events is the sum of their probabilities; but the probability of the disjunction of two dependent
 * events, e.g. two events that are both certain, is clearly not the sum of their individual
 * probabilities, it is their sum minus the probability of them occurring together.)
 *
 * <p>We make no assumptions about the dependence of events and instead operate using "worst-case"
 * order-of magnitude ballparks. The probability of a conjunction is the minimum, and the
 * probability of a disjunction is a maximum. The only non-order-of-magnitude operation is negation,
 * which is implemented accurately as the difference between certainty and the input probability.
 * The actual details of this accuracy are likely obscured, however, by the scientific notation
 * representation of the probability that describes only the order of magnitude.
 */
public class Prob {
    /**
     * The number of decimal places to use in the representation.
     *
     * <p>This should be greater than the negative exponent in the most severe severity
     * (catastrophic, which is 1e-09).
     */
    private static final int SCALE = 10;
    /** Used for formatting probabilities in scientific-notation. */
    private static final DecimalFormat formatter;

    static {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        // Use lower-case "e" instead of capital "E"
        symbols.setExponentSeparator("e");
        // Use scientific notation with two digits in the exponent
        formatter = new DecimalFormat("0E00", symbols);
    }

    /** The probability. */
    private BigDecimal probability;

    private Prob(BigDecimal probability) {
        this.probability = probability;
    }

    /**
     * Construct a probability from a DAL.
     *
     * <p>Requiers that the DAL is non-negative.
     *
     * @param dal the DAL
     * @return the probability
     */
    public static Prob fromDal(int dal) {
        if (dal < 0) {
            throw new RuntimeException(
                    "Impossible probability from DAL, cannot be negative: " + dal);
        }
        // Perform exponentiation
        return new Prob(BigDecimal.ONE.setScale(SCALE).scaleByPowerOfTen(-dal));
    }

    /**
     * Construct a probability from a DAL string. Returns a default value for an empty string and
     * throws NumberFormatException for malformed numbers.
     *
     * @param dalStr the string to be parsed
     * @param def default value to return for empty string
     * @return the probability
     */
    public static Prob fromDal(String dalStr, Prob def) {
        if (dalStr.length() == 0) {
            return def;
        } else {
            return fromDal(Integer.parseInt(dalStr));
        }
    }

    /**
     * Construct a probability from a DAL string. Throws RuntimeException for empty string and
     * throws NumberFormatException for malformed numbers.
     *
     * @param dalStr the string to be parsed
     * @return the probability
     */
    public static Prob fromDal(String dalStr) {
        if (dalStr.length() == 0) {
            throw new RuntimeException("got empty DAL string");
        } else {
            return fromDal(Integer.parseInt(dalStr));
        }
    }

    public static int dalOfSeverity(String severity) {
        switch (severity.toLowerCase()) {
            case "catastrophic":
                return 9;
            case "hazardous":
                return 7;
            case "major":
                return 5;
            case "minor":
                return 3;
            case "none":
                return 0;
            default:
                throw new RuntimeException("Invalid severity: " + severity);
        }
    }

    /**
     * Construct a probability from a severity level (case-insensitive). Valid severity levels are:
     * Catastrophic, Hazardous, Major, Minor, and None.
     *
     * @param severity the severity string
     * @return the probability
     */
    public static Prob fromSeverity(String severity) {
        return fromDal(dalOfSeverity(severity));
    }

    /** @return the probability of zero (impossibility). */
    public static Prob impossible() {
        return new Prob(BigDecimal.ZERO.setScale(SCALE));
    }

    /** @return the probability of one (certainty). */
    public static Prob certain() {
        return new Prob(BigDecimal.ONE.setScale(SCALE));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Prob && ((Prob) other).probability.compareTo(probability) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(probability);
    }

    /**
     * Converts this probability to order-of-magnitude form/scientific notation, e.g. "1", "1e-03",
     * or "1e-09".
     *
     * <p>All probabilities have the exponent displayed except for certainty ("1") and impossibility
     * ("0").
     *
     * @return the probability string
     */
    @Override
    public String toString() {
        // In these special cases, drop the "e00" that would otherwise appear
        if (probability.compareTo(BigDecimal.ONE) == 0) {
            return "1";
        } else if (probability.compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        } else {
            return formatter.format(probability);
        }
    }

    /**
     * Probabilistic conjunction.
     *
     * @param a
     * @param b
     * @return the probability of a and b
     */
    public static Prob and(Prob a, Prob b) {
        return new Prob(a.probability.min(b.probability));
    }

    /**
     * Probabilistic disjunction.
     *
     * @param a
     * @param b
     * @return the probability of a or b
     */
    public static Prob or(Prob a, Prob b) {
        return new Prob(a.probability.max(b.probability));
    }

    /**
     * Probabilistic negation.
     *
     * @param p
     * @return the probability of not p.
     */
    public static Prob not(Prob p) {
        return new Prob(BigDecimal.ONE.setScale(SCALE).subtract(p.probability));
    }

    /**
     * The less-than-or-equal-to operator. If true, then b is at least as probable as a.
     *
     * @param a
     * @param b
     * @return true iff a <= b
     */
    public static boolean lte(Prob a, Prob b) {
        return a.probability.compareTo(b.probability) <= 0;
    }
}
