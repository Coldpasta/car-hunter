package dev.carhunter.imports;

import dev.carhunter.domain.EvidenceReference;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** A reviewed CN/TARIC classification; never inferred from a marketplace body-style label. */
public record VehicleClassificationAssessment(
        VehicleClassificationStatus status,
        String classificationCode,
        String rationale,
        List<EvidenceReference> evidence,
        Instant reviewedAt) {

    public VehicleClassificationAssessment {
        Objects.requireNonNull(status, "status");
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        if (rationale == null || rationale.isBlank()) {
            throw new IllegalArgumentException("Classification rationale is required");
        }
        if (status == VehicleClassificationStatus.CONFIRMED) {
            classificationCode = normalizeCode(classificationCode);
            if (evidence.isEmpty()) {
                throw new IllegalArgumentException("Confirmed classification requires evidence");
            }
            Objects.requireNonNull(reviewedAt, "reviewedAt");
        } else if (classificationCode != null && !classificationCode.isBlank()) {
            classificationCode = normalizeCode(classificationCode);
        } else {
            classificationCode = null;
        }
    }

    public boolean isConfirmed() {
        return status == VehicleClassificationStatus.CONFIRMED;
    }

    static String normalizeCode(String value) {
        if (value == null) {
            throw new IllegalArgumentException("CN/TARIC classification code is required");
        }
        String normalized = value.replace(" ", "");
        if (!normalized.matches("87\\d{6}|87\\d{8}")) {
            throw new IllegalArgumentException("Vehicle classification code must be an 8-digit CN or 10-digit TARIC code in Chapter 87");
        }
        return normalized;
    }
}
