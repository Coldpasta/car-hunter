package dev.carhunter.domain;

import java.math.BigDecimal;

public record CarCandidate(
        String id,
        String makeModel,
        String title,
        String sourceUrl,
        String country,
        String state,
        BigDecimal price,
        String currency,
        Integer year,
        Integer mileageKm,
        String fuel,
        String transmission,
        String sellerClaims,
        String titleStatus,
        String titleEvidenceQuote,
        String titleEvidenceUrl
) {}
