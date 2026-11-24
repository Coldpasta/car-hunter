package dev.carhunter.imports;

import java.util.List;

/** Aggregated totals are null when required inputs are missing or currencies differ. */
public record CostScenarioResult(
        String currency,
        String status,
        CostRange total,
        List<CostScenarioComponent> components,
        List<String> missingComponents,
        List<String> errors) {
    public CostScenarioResult {
        components = components == null ? List.of() : List.copyOf(components);
        missingComponents = missingComponents == null ? List.of() : List.copyOf(missingComponents);
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public boolean isComplete() {
        return "COMPLETE".equals(status) && total != null;
    }
}
