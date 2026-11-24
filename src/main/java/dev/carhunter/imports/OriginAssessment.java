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
        Instant reviewedAt) {

    public OriginAssessment {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(directTransportStatus, "directTransportStatus");
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
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
            Objects.requireNonNull(reviewedAt, "reviewedAt");
        }
    }

    public boolean isConfirmed() {
        return status == OriginAssessmentStatus.CONFIRMED;
    }

    public boolean hasRequiredUsTransportEvidence() {
        return directTransportStatus == DirectTransportStatus.DIRECT_US_TO_EU_PROVEN
                || directTransportStatus == DirectTransportStatus.THIRD_COUNTRY_UNDER_CUSTOMS_SUPERVISION_PROVEN;
    }
}
