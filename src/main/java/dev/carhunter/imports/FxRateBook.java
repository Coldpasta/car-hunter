package dev.carhunter.imports;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Resolves exactly one sourced FX rate for a tax purpose, currency pair and legal date. */
public class FxRateBook {

    public Optional<FxRateRecord> findApplicable(
            List<FxRateRecord> records,
            FxRatePurpose purpose,
            String baseCurrency,
            String quoteCurrency,
            LocalDate applicableDate) {
        Objects.requireNonNull(purpose, "purpose");
        Objects.requireNonNull(applicableDate, "applicableDate");
        String base = normalizeCurrency(baseCurrency);
        String quote = normalizeCurrency(quoteCurrency);
        if (base.equals(quote)) {
            throw new IllegalArgumentException("Base and quote currencies must differ");
        }

        List<FxRateRecord> matches = records == null ? List.of() : records.stream()
                .filter(record -> record.purpose() == purpose)
                .filter(record -> record.baseCurrency().equals(base))
                .filter(record -> record.quoteCurrency().equals(quote))
                .filter(record -> record.isEffectiveOn(applicableDate))
                .toList();

        if (matches.size() > 1) {
            throw new IllegalStateException("Multiple FX rates apply for " + purpose + " "
                    + base + "/" + quote + " on " + applicableDate);
        }
        return matches.stream().findFirst();
    }

    private static String normalizeCurrency(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Currency is required");
        }
        return value.toUpperCase(Locale.ROOT);
    }
}
