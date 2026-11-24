package dev.carhunter.imports;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Pure arithmetic for aggregating already-classified cost components.
 * Tax bases, rates, FX and legal rules remain the responsibility of a destination profile.
 */
public class CostScenarioCalculator {

    public CostScenarioResult aggregate(
            String calculationCurrency,
            List<CostScenarioComponent> components,
            Set<String> requiredComponentCodes) {
        if (calculationCurrency == null || calculationCurrency.isBlank()) {
            throw new IllegalArgumentException("Calculation currency is required");
        }

        String currency = calculationCurrency.toUpperCase(Locale.ROOT);
        List<CostScenarioComponent> safeComponents = components == null ? List.of() : List.copyOf(components);
        Set<String> required = requiredComponentCodes == null ? Set.of() : Set.copyOf(requiredComponentCodes);
        List<String> missing = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<String> seenCodes = new HashSet<>();

        BigDecimal low = BigDecimal.ZERO;
        BigDecimal expected = BigDecimal.ZERO;
        BigDecimal high = BigDecimal.ZERO;

        for (CostScenarioComponent component : safeComponents) {
            if (!seenCodes.add(component.code())) {
                errors.add("Duplicate component code: " + component.code());
                continue;
            }
            if (!currency.equalsIgnoreCase(component.currency())) {
                errors.add("Currency mismatch for " + component.code() + ": expected "
                        + currency + " but received " + component.currency());
                continue;
            }
            low = low.add(component.amount().low());
            expected = expected.add(component.amount().expected());
            high = high.add(component.amount().high());
        }

        for (String code : required) {
            if (!seenCodes.contains(code)) {
                missing.add(code);
            }
        }

        if (!errors.isEmpty() || !missing.isEmpty()) {
            return new CostScenarioResult(currency, "INCOMPLETE", null, safeComponents, missing, errors);
        }

        return new CostScenarioResult(
                currency,
                "COMPLETE",
                new CostRange(low, expected, high),
                safeComponents,
                List.of(),
                List.of());
    }
}
