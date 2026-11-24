package dev.carhunter.service;

import dev.carhunter.domain.CarCandidate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListingDeduplicatorTest {
    private final ListingDeduplicator deduplicator = new ListingDeduplicator();

    private CarCandidate candidate(String url, String vin, String id) {
        return new CarCandidate(id, "Toyota RAV4", "RAV4", url, "US", "CA",
                new BigDecimal("24000"), "USD", 2022, 80000, "hybrid", "automatic",
                "Clean title", "CLEAN", "Clean title", url, "dealer", vin);
    }

    @Test
    void removesTrackingParameterAndWwwDuplicatesByCanonicalUrl() {
        var first = candidate("https://www.cars.com/vehicle/123?utm_source=x", null, "first");
        var duplicate = candidate("https://cars.com/vehicle/123/?ref=search", null, "second");
        var result = deduplicator.deduplicate(List.of(first, duplicate));
        assertEquals(1, result.size());
        assertEquals("first", result.getFirst().id());
    }

    @Test
    void removesCrossMarketplaceDuplicatesWhenVinMatches() {
        var first = candidate("https://cars.com/vehicle/123", "1HGCM82633A004352", "first");
        var duplicate = candidate("https://autotrader.com/cars-for-sale/456", "1hgcm82633a004352", "second");
        var result = deduplicator.deduplicate(List.of(first, duplicate));
        assertEquals(1, result.size());
        assertEquals("first", result.getFirst().id());
    }

    @Test
    void doesNotMergeDifferentVehiclesWithoutMatchingVinOrUrl() {
        var first = candidate("https://cars.com/vehicle/123", null, "first");
        var second = candidate("https://cars.com/vehicle/456", null, "second");
        assertEquals(2, deduplicator.deduplicate(List.of(first, second)).size());
    }
}
