package dev.carhunter.imports;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

/**
 * An auditable exchange rate. The rate means quote-currency units for one
 * unit of base currency. Tax-specific rounding is deliberately not performed.
 */
public record FxRateRecord(
        FxRatePurpose purpose,
        String baseCurrency,
        String quoteCurrency,
        BigDecimal rate,
        String sourceUrl,
        LocalDate publishedDate,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        Instant retrievedAt) {

    public FxRateRecord {
        Objects.requireNonNull(purpose, "purpose");
        baseCurrency = normalizeCurrency(baseCurrency, "baseCurrency");
        quoteCurrency = normalizeCurrency(quoteCurrency, "quoteCurrency");
        if (baseCurrency.equals(quoteCurrency)) {
            throw new IllegalArgumentException("Base and quote currencies must differ");
        }
        Objects.requireNonNull(rate, "rate");
        if (rate.signum() <= 0) {
            throw new IllegalArgumentException("FX rate must be greater than zero");
        }
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalArgumentException("FX source URL is required");
        }
        URI source;
        try {
            source = URI.create(sourceUrl);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("FX source URL must be a valid HTTPS URL", exception);
        }
        if (!source.isAbsolute() || !"https".equalsIgnoreCase(source.getScheme()) || source.getHost() == null) {
            throw new IllegalArgumentException("FX source URL must be a valid HTTPS URL");
        }
        Objects.requireNonNull(publishedDate, "publishedDate");
        Objects.requireNonNull(effectiveFrom, "effectiveFrom");
        Objects.requireNonNull(retrievedAt, "retrievedAt");
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("FX effectiveTo cannot be before effectiveFrom");
        }
    }

    public boolean isEffectiveOn(LocalDate date) {
        Objects.requireNonNull(date, "date");
        return !date.isBefore(effectiveFrom) && (effectiveTo == null || !date.isAfter(effectiveTo));
    }

    /** Converts a non-negative amount in the base currency without applying tax-specific rounding. */
    public BigDecimal convert(BigDecimal baseAmount) {
        Objects.requireNonNull(baseAmount, "baseAmount");
        if (baseAmount.signum() < 0) {
            throw new IllegalArgumentException("Amount to convert cannot be negative");
        }
        return baseAmount.multiply(rate);
    }

    private static String normalizeCurrency(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String normalized = value.toUpperCase(Locale.ROOT);
        try {
            Currency.getInstance(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(field + " must be a valid ISO 4217 currency code", exception);
        }
        return normalized;
    }
}
