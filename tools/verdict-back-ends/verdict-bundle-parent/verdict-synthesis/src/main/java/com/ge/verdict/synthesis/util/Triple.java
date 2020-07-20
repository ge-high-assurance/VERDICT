package com.ge.verdict.synthesis.util;

import java.util.Objects;

public final class Triple<L, M, R> {
    public final L left;
    public final M middle;
    public final R right;

    public Triple(L left, M middle, R right) {
        this.left = left;
        this.middle = middle;
        this.right = right;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Triple) {
            Triple<?, ?, ?> otherTriple = (Triple<?, ?, ?>) other;
            return left.equals(otherTriple.left)
                    && middle.equals(otherTriple.middle)
                    && right.equals(otherTriple.right);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, middle, right);
    }
}
