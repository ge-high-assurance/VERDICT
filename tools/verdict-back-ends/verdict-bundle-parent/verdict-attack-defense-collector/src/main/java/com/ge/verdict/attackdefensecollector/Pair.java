package com.ge.verdict.attackdefensecollector;

import java.util.Objects;

/**
 * A generic pair. Correctly implements equals() and hashCode(), so may be used as a key to a hash
 * map. Note that the L and R types must themselves correctly implement equals() and hashCode() for
 * this to work correctly.
 *
 * @param <L> type of left value
 * @param <R> type of right value
 */
public class Pair<L, R> {
    public final L left;
    public final R right;

    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Pair<?, ?>) {
            Pair<?, ?> otherPair = (Pair<?, ?>) other;
            return left.equals(otherPair.left) && right.equals(otherPair.right);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }
}
