package dev.carhunter.domain;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
/** Normalized US listing facts with source provenance retained. */
public record VehicleListing(
 String id, String make, String model, String trim, Integer modelYear,
 BigDecimal askingPrice, String priceCurrency, Integer mileageMiles, Integer mileageKm,
 String vin, String sellerType, String sellerName, String city, String state, String zipCode,
 String fuel, String transmission, String bodyType, String sourceUrl, String sourceDomain,
 Instant retrievedAt, String titleClaim, TitleAssessment titleAssessment, List<EvidenceReference> evidence
) {
 public VehicleListing { evidence = evidence == null ? List.of() : List.copyOf(evidence); }
 public boolean isCleanTitleEligible() { return titleAssessment != null && titleAssessment.eligibleForCleanTitleShortlist(); }
}
