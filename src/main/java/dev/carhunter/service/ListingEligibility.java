package dev.carhunter.service;
import dev.carhunter.domain.TitleStatus;
import dev.carhunter.domain.VehicleListing;
import org.springframework.stereotype.Component;
import java.util.Locale;
import java.util.Set;
/** Deterministic eligibility checks; model output cannot override these rules. */
@Component
public class ListingEligibility {
 private static final Set<String> NON_CLEAN_TERMS = Set.of("salvage", "rebuilt", "rebuild", "reconstructed",
  "flood", "junk", "non-repairable", "nonrepairable", "parts only", "parts-only");
 public boolean isEligibleForCleanTitleShortlist(VehicleListing listing) {
  if (listing == null || listing.sourceUrl() == null || !listing.sourceUrl().startsWith("https://")) return false;
  if (listing.state() == null || listing.state().isBlank()) return false;
  return listing.titleAssessment() != null
   && listing.titleAssessment().status() == TitleStatus.CLEAN_EVIDENCED
   && !containsNonCleanBrand(listing.titleClaim());
 }
 public TitleStatus classifyTitleClaim(String claim, boolean credibleExplicitCleanEvidence) {
  if (claim == null || claim.isBlank()) return TitleStatus.UNVERIFIED;
  String normalized = claim.toLowerCase(Locale.ROOT);
  if (NON_CLEAN_TERMS.stream().anyMatch(normalized::contains)) return TitleStatus.NON_CLEAN;
  if (credibleExplicitCleanEvidence && (normalized.contains("clean") || normalized.contains("clear")))
   return TitleStatus.CLEAN_EVIDENCED;
  return TitleStatus.UNVERIFIED;
 }
 private boolean containsNonCleanBrand(String claim) {
  if (claim == null) return false;
  String normalized = claim.toLowerCase(Locale.ROOT);
  return NON_CLEAN_TERMS.stream().anyMatch(normalized::contains);
 }
 public Integer mileageMilesToKm(Integer miles) {
  if (miles == null || miles < 0) return null;
  return (int) Math.round(miles * 1.609344d);
 }
}
