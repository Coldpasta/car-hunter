package dev.carhunter.imports;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/** One exact, sourced tariff measure for one classification, origin and effective period. */
public record TariffMeasureRecord(
        String classificationCode,
        String originCountryCode,
        BigDecimal dutyRatePercent,
        String measureCode,
        String sourceUrl,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        Instant retrievedAt,
        boolean requiresUsDirectTransportEvidence) {

    public TariffMeasureRecord {
        classificationCode = VehicleClassificationAssessment.normalizeCode(classificationCode);
        Objects.requireNonNull(originCountryCode, "originCountryCode");
        originCountryCode = originCountryCode.toUpperCase(Locale.ROOT);
        if (!originCountryCode.matches("[A-Z]{2}") || !Arrays.asList(Locale.getISOCountries()).contains(originCountryCode)) {
            throw new IllegalArgumentException("Origin country must be an ISO alpha-2 code");
        }
        Objects.requireNonNull(dutyRatePercent, "dutyRatePercent");
        if (dutyRatePercent.signum() < 0 || dutyRatePercent.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Duty rate must be between 0 and 100 percent");
        }
        if (requiresUsDirectTransportEvidence && !"US".equals(originCountryCode)) {
            throw new IllegalArgumentException("US direct-transport evidence can only apply to US-origin measures");
        }
        if ("US".equals(originCountryCode) && dutyRatePercent.signum() == 0
                && !requiresUsDirectTransportEvidence) {
            throw new IllegalArgumentException("A zero-duty US-origin measure must require direct-transport evidence");
        }
        if (measureCode == null || measureCode.isBlank()) {
            throw new IllegalArgumentException("Tariff measure code is required");
        }
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalArgumentException("Tariff source URL is required");
        }
        URI source;
        try {
            source = URI.create(sourceUrl);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Tariff source URL must be a valid HTTPS URL", exception);
        }
        if (!source.isAbsolute() || !"https".equalsIgnoreCase(source.getScheme()) || source.getHost() == null) {
            throw new IllegalArgumentException("Tariff source URL must be a valid HTTPS URL");
        }
        Objects.requireNonNull(effectiveFrom, "effectiveFrom");
        Objects.requireNonNull(retrievedAt, "retrievedAt");
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("Tariff effectiveTo cannot be before effectiveFrom");
        }
    }

    public boolean isEffectiveOn(LocalDate date) {
        Objects.requireNonNull(date, "date");
        return !date.isBefore(effectiveFrom) && (effectiveTo == null || !date.isAfter(effectiveTo));
    }
}
