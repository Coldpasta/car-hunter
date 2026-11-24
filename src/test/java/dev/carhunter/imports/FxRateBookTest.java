package dev.carhunter.imports;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FxRateBookTest {
    private final FxRateBook rateBook = new FxRateBook();

    @Test
    void resolvesOnlyRateMatchingPurposePairAndEffectiveDate() {
        var customs = rate(FxRatePurpose.CUSTOMS, "USD", "PLN", "4.00",
                LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-30"));
        var excise = rate(FxRatePurpose.EXCISE, "USD", "PLN", "3.95",
                LocalDate.parse("2026-11-10"), LocalDate.parse("2026-11-10"));

        assertEquals(Optional.of(customs), rateBook.findApplicable(
                List.of(customs, excise), FxRatePurpose.CUSTOMS, "usd", "pln",
                LocalDate.parse("2026-11-24")));
        assertEquals(Optional.of(excise), rateBook.findApplicable(
                List.of(customs, excise), FxRatePurpose.EXCISE, "USD", "PLN",
                LocalDate.parse("2026-11-10")));
    }

    @Test
    void doesNotReuseRateOutsideItsEffectivePeriod() {
        var record = rate(FxRatePurpose.CUSTOMS, "USD", "PLN", "4.00",
                LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-30"));

        assertTrue(rateBook.findApplicable(List.of(record), FxRatePurpose.CUSTOMS,
                "USD", "PLN", LocalDate.parse("2026-12-01")).isEmpty());
    }

    @Test
    void refusesAmbiguousOverlappingRates() {
        var first = rate(FxRatePurpose.CUSTOMS, "USD", "PLN", "4.00",
                LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-30"));
        var second = rate(FxRatePurpose.CUSTOMS, "USD", "PLN", "4.10",
                LocalDate.parse("2026-11-15"), LocalDate.parse("2026-12-15"));

        assertThrows(IllegalStateException.class, () -> rateBook.findApplicable(
                List.of(first, second), FxRatePurpose.CUSTOMS, "USD", "PLN",
                LocalDate.parse("2026-11-24")));
    }

    @Test
    void conversionUsesQuoteUnitsPerOneBaseUnitWithoutImplicitRounding() {
        var record = rate(FxRatePurpose.EXCISE, "USD", "PLN", "3.98765",
                LocalDate.parse("2026-11-24"), null);

        assertEquals(new BigDecimal("398.76500"),
                record.convert(new BigDecimal("100")));
    }

    @Test
    void rejectsInvalidRateAndNonHttpsSource() {
        assertThrows(IllegalArgumentException.class, () -> new FxRateRecord(
                FxRatePurpose.CUSTOMS, "USD", "PLN", BigDecimal.ZERO,
                "https://example.com/rates", LocalDate.now(), LocalDate.now(), null, Instant.now()));
        assertThrows(IllegalArgumentException.class, () -> new FxRateRecord(
                FxRatePurpose.CUSTOMS, "USD", "PLN", BigDecimal.ONE,
                "http://example.com/rates", LocalDate.now(), LocalDate.now(), null, Instant.now()));
    }

    @Test
    void rejectsInvalidEffectiveDateRange() {
        assertThrows(IllegalArgumentException.class, () -> rate(
                FxRatePurpose.CUSTOMS, "USD", "PLN", "4.00",
                LocalDate.parse("2026-11-30"), LocalDate.parse("2026-11-01")));
    }

    private FxRateRecord rate(FxRatePurpose purpose, String base, String quote, String amount,
                              LocalDate from, LocalDate to) {
        return new FxRateRecord(purpose, base, quote, new BigDecimal(amount),
                "https://example.com/official-fx-rates", from, from, to,
                Instant.parse("2026-11-24T10:00:00Z"));
    }
}
