package dev.carhunter.domain;
import java.math.BigDecimal;
import java.util.List;
public record DestinationCostComponent(String code, String label, BigDecimal amount, String currency,
 String calculation, String effectiveDate, List<EvidenceReference> evidence, boolean estimated) {
 public DestinationCostComponent { evidence = evidence == null ? List.of() : List.copyOf(evidence); }
}
