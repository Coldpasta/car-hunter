package dev.carhunter.imports;

import java.util.Locale;
import java.util.Objects;

/** One independently auditable cost input for a landed-cost estimate. */
public record CostScenarioComponent(
        String code,
        String label,
        String currency,
        CostRange amount,
        boolean required,
        boolean estimated) {
    public CostScenarioComponent {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Component code is required");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Component label is required");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Component currency is required");
        }
        Objects.requireNonNull(amount, "amount");
        currency = currency.toUpperCase(Locale.ROOT);
    }
}
