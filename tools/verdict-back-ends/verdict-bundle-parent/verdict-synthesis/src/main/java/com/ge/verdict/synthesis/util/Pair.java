package com.ge.verdict.synthesis.util;

import java.util.Objects;

public final class Pair<L, R> {
    public final L left;
    public final R right;

    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Pair) {
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
