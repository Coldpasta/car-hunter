package dev.carhunter.imports;

import java.math.BigDecimal;
import java.util.Objects;

/** Low/expected/high amounts expressed in one currency. */
public record CostRange(BigDecimal low, BigDecimal expected, BigDecimal high) {
    public CostRange {
        Objects.requireNonNull(low, "low");
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(high, "high");
        if (low.signum() < 0 || expected.signum() < 0 || high.signum() < 0) {
            throw new IllegalArgumentException("Cost amounts cannot be negative");
        }
        if (low.compareTo(expected) > 0 || expected.compareTo(high) > 0) {
            throw new IllegalArgumentException("Cost range must satisfy low <= expected <= high");
        }
    }
}
