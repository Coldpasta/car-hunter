package dev.carhunter.domain;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
public record ImportCostEstimate(String destinationCountry, String calculationCurrency, String status,
 String rulesetVersion, Instant calculatedAt, BigDecimal lowTotal, BigDecimal expectedTotal,
 BigDecimal highTotal, List<DestinationCostComponent> components, List<String> missingComponents,
 List<String> assumptions) {
 public ImportCostEstimate {
  components = components == null ? List.of() : List.copyOf(components);
  missingComponents = missingComponents == null ? List.of() : List.copyOf(missingComponents);
  assumptions = assumptions == null ? List.of() : List.copyOf(assumptions);
 }
}
