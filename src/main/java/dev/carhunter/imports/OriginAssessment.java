package dev.carhunter.imports;

import dev.carhunter.domain.EvidenceReference;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Human-reviewed non-preferential origin assessment.
 * Evidence presence alone never upgrades an unverified assessment to confirmed.
 */
public record OriginAssessment(
        String countryCode,
        OriginAssessmentStatus status,
        DirectTransportStatus directTransportStatus,
        List<EvidenceReference> evidence,
        List<EvidenceReference> transportEvidence,
        Instant reviewedAt) {

    public OriginAssessment {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(directTransportStatus, "directTransportStatus");
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        transportEvidence = transportEvidence == null ? List.of() : List.copyOf(transportEvidence);
        if (countryCode != null && !countryCode.isBlank()) {
            countryCode = countryCode.toUpperCase(Locale.ROOT);
            if (!countryCode.matches("[A-Z]{2}")) {
                throw new IllegalArgumentException("Origin country must be an ISO alpha-2 code");
            }
        } else {
            countryCode = null;
        }
        if (status == OriginAssessmentStatus.CONFIRMED) {
            if (countryCode == null) {
                throw new IllegalArgumentException("Confirmed origin requires a country code");
            }
            if (evidence.isEmpty()) {
                throw new IllegalArgumentException("Confirmed origin requires supporting evidence");
            }
            boolean vinOnly = evidence.stream().allMatch(reference ->
                    (reference.sourceType() != null && reference.sourceType().toLowerCase(Locale.ROOT).contains("vin"))
                    || (reference.claim() != null && reference.claim().toLowerCase(Locale.ROOT).contains("vin")));
            if (vinOnly) {
                throw new IllegalArgumentException("VIN decoding alone cannot confirm non-preferential origin");
            }
            Objects.requireNonNull(reviewedAt, "reviewedAt");
        }
        if ((directTransportStatus == DirectTransportStatus.DIRECT_US_TO_EU_PROVEN
                || directTransportStatus == DirectTransportStatus.THIRD_COUNTRY_SUPERVISED_AND_UNALTERED_PROVEN)
                && transportEvidence.isEmpty()) {
            throw new IllegalArgumentException("Proven direct/supervised transport requires transport evidence");
        }
    }

    public boolean isConfirmed() {
        return status == OriginAssessmentStatus.CONFIRMED;
    }

    public boolean hasRequiredUsTransportEvidence() {
        return (directTransportStatus == DirectTransportStatus.DIRECT_US_TO_EU_PROVEN
                || directTransportStatus == DirectTransportStatus.THIRD_COUNTRY_SUPERVISED_AND_UNALTERED_PROVEN)
                && !transportEvidence.isEmpty();
    }
}
