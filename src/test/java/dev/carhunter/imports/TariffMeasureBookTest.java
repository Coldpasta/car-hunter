package dev.carhunter.imports;

import dev.carhunter.domain.EvidenceReference;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TariffMeasureBookTest {
    private static final LocalDate IMPORT_DATE = LocalDate.parse("2026-11-24");
    private static final Instant REVIEWED_AT = Instant.parse("2026-11-24T10:00:00Z");
    private final TariffMeasureBook book = new TariffMeasureBook();

    @Test
    void resolvesOnlyExactClassificationOriginAndEffectiveDate() {
        var usMeasure = measure("8703239000", "US", "0.00",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2029-12-31"), true);
        var jpMeasure = measure("8703239000", "JP", "10.00",
                LocalDate.parse("2020-01-01"), null, false);

        var result = book.resolve(List.of(usMeasure, jpMeasure),
                classification("8703239000"), origin("US", DirectTransportStatus.DIRECT_US_TO_EU_PROVEN),
                IMPORT_DATE);

        assertEquals(TariffResolutionStatus.FOUND, result.status());
        assertEquals(usMeasure, result.measure().orElseThrow());
    }

    @Test
    void refusesUnresolvedClassificationAndUnverifiedOrigin() {
        var measure = measure("8703239000", "US", "0.00",
                LocalDate.parse("2026-07-01"), null, true);

        assertEquals(TariffResolutionStatus.CLASSIFICATION_UNRESOLVED,
                book.resolve(List.of(measure), new VehicleClassificationAssessment(
                        VehicleClassificationStatus.UNRESOLVED, null, "Insufficient vehicle facts",
                        List.of(), null), origin("US", DirectTransportStatus.DIRECT_US_TO_EU_PROVEN),
                        IMPORT_DATE).status());

        assertEquals(TariffResolutionStatus.ORIGIN_UNVERIFIED,
                book.resolve(List.of(measure), classification("8703239000"),
                        new OriginAssessment("US", OriginAssessmentStatus.UNVERIFIED,
                                DirectTransportStatus.DIRECT_US_TO_EU_PROVEN, List.of(), null),
                        IMPORT_DATE).status());
    }

    @Test
    void refusesUsMeasureWithoutDirectOrControlledTransitEvidence() {
        var measure = measure("8703239000", "US", "0.00",
                LocalDate.parse("2026-07-01"), null, true);

        var result = book.resolve(List.of(measure), classification("8703239000"),
                origin("US", DirectTransportStatus.UNVERIFIED), IMPORT_DATE);

        assertEquals(TariffResolutionStatus.DIRECT_TRANSPORT_UNPROVEN, result.status());
        assertTrue(result.measure().isEmpty());
    }

    @Test
    void acceptsThirdCountryTransitOnlyWhenCustomsSupervisionIsProven() {
        var measure = measure("8703239000", "US", "0.00",
                LocalDate.parse("2026-07-01"), null, true);

        var result = book.resolve(List.of(measure), classification("8703239000"),
                origin("US", DirectTransportStatus.THIRD_COUNTRY_UNDER_CUSTOMS_SUPERVISION_PROVEN),
                IMPORT_DATE);

        assertEquals(TariffResolutionStatus.FOUND, result.status());
    }

    @Test
    void doesNotReuseExpiredMeasureOrFallBackToParentCode() {
        var expired = measure("8703239000", "US", "0.00",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2026-10-31"), true);
        var parentCodeOnly = measure("87032390", "US", "0.00",
                LocalDate.parse("2026-07-01"), null, true);

        var result = book.resolve(List.of(expired, parentCodeOnly), classification("8703239000"),
                origin("US", DirectTransportStatus.DIRECT_US_TO_EU_PROVEN), IMPORT_DATE);

        assertEquals(TariffResolutionStatus.NO_APPLICABLE_MEASURE, result.status());
    }

    @Test
    void refusesAmbiguousOverlappingMeasures() {
        var first = measure("8703239000", "US", "0.00",
                LocalDate.parse("2026-07-01"), null, true);
        var second = measure("8703239000", "US", "5.00",
                LocalDate.parse("2026-11-01"), null, true);

        assertThrows(IllegalStateException.class, () -> book.resolve(List.of(first, second),
                classification("8703239000"),
                origin("US", DirectTransportStatus.DIRECT_US_TO_EU_PROVEN), IMPORT_DATE));
    }

    @Test
    void rejectsInvalidTariffCodesRatesAndDateRanges() {
        assertThrows(IllegalArgumentException.class, () -> measure("8703", "US", "0.00",
                LocalDate.parse("2026-07-01"), null, true));
        assertThrows(IllegalArgumentException.class, () -> measure("8703239000", "US", "101",
                LocalDate.parse("2026-07-01"), null, true));
        assertThrows(IllegalArgumentException.class, () -> measure("8703239000", "US", "0",
                LocalDate.parse("2026-11-30"), LocalDate.parse("2026-11-01"), true));
    }

    @Test
    void confirmedClassificationAndOriginRequireEvidenceAndReviewTimestamp() {
        assertThrows(IllegalArgumentException.class, () -> new VehicleClassificationAssessment(
                VehicleClassificationStatus.CONFIRMED, "8703239000", "Reviewed", List.of(), REVIEWED_AT));
        assertThrows(NullPointerException.class, () -> new VehicleClassificationAssessment(
                VehicleClassificationStatus.CONFIRMED, "8703239000", "Reviewed",
                List.of(evidence("classification")), null));
        assertThrows(IllegalArgumentException.class, () -> new OriginAssessment(
                "US", OriginAssessmentStatus.CONFIRMED, DirectTransportStatus.DIRECT_US_TO_EU_PROVEN,
                List.of(), REVIEWED_AT));
    }

    @Test
    void rejectsVinAsAClassificationCodeAndInvalidOriginCountry() {
        assertThrows(IllegalArgumentException.class, () -> classification("1HGCM82633A004352"));
        assertThrows(IllegalArgumentException.class, () -> origin("USA", DirectTransportStatus.DIRECT_US_TO_EU_PROVEN));
    }

    private VehicleClassificationAssessment classification(String code) {
        return new VehicleClassificationAssessment(VehicleClassificationStatus.CONFIRMED, code,
                "Reviewed against vehicle characteristics", List.of(evidence("CN/TARIC classification")),
                REVIEWED_AT);
    }

    private OriginAssessment origin(String country, DirectTransportStatus transportStatus) {
        return new OriginAssessment(country, OriginAssessmentStatus.CONFIRMED, transportStatus,
                List.of(evidence("manufacturer production record")), REVIEWED_AT);
    }

    private TariffMeasureRecord measure(String code, String country, String rate,
                                       LocalDate from, LocalDate to, boolean directTransportRequired) {
        return new TariffMeasureRecord(code, country, new BigDecimal(rate), "TARIC-MEASURE-TEST",
                "https://taxation-customs.ec.europa.eu/taric", from, to, REVIEWED_AT,
                directTransportRequired);
    }

    private EvidenceReference evidence(String claim) {
        return new EvidenceReference(claim, "https://example.com/evidence", "official-document",
                REVIEWED_AT, "Reviewed by operator");
    }
}
