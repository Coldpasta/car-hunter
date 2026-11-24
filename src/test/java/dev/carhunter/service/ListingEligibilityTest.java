package dev.carhunter.service;

import dev.carhunter.domain.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ListingEligibilityTest {
    private final ListingEligibility eligibility = new ListingEligibility();

    private VehicleListing listing(TitleStatus status, String titleClaim) {
        return new VehicleListing("1", "Toyota", "RAV4", null, 2022, new BigDecimal("24000"),
                "USD", 50000, 80467, "1HGCM82633A004352", "dealer", "Example Motors",
                "Los Angeles", "CA", "90001", "hybrid", "automatic", "SUV",
                "https://example.com/vehicle/1", "example.com", Instant.now(), titleClaim,
                new TitleAssessment(status, titleClaim, "test", List.of()), List.of());
    }

    @Test
    void onlyExplicitlyEvidencedCleanTitlePasses() {
        assertTrue(eligibility.isEligibleForCleanTitleShortlist(listing(TitleStatus.CLEAN_EVIDENCED, "Clean title")));
        assertFalse(eligibility.isEligibleForCleanTitleShortlist(listing(TitleStatus.UNVERIFIED, null)));
        assertFalse(eligibility.isEligibleForCleanTitleShortlist(listing(TitleStatus.CONFLICTING, "Clean title")));
        assertFalse(eligibility.isEligibleForCleanTitleShortlist(listing(TitleStatus.NON_CLEAN, "Salvage title")));
    }

    @Test
    void nonCleanTitleBrandsOverrideCleanClaim() {
        assertEquals(TitleStatus.NON_CLEAN, eligibility.classifyTitleClaim("Clean title - rebuilt after salvage", true));
        assertEquals(TitleStatus.UNVERIFIED, eligibility.classifyTitleClaim("Title status not reported", false));
        assertEquals(TitleStatus.CLEAN_EVIDENCED, eligibility.classifyTitleClaim("Clean title", true));
    }

    @Test
    void convertsMilesToKilometersAndHandlesMissingValues() {
        assertEquals(80467, eligibility.mileageMilesToKm(50000));
        assertNull(eligibility.mileageMilesToKm(null));
        assertNull(eligibility.mileageMilesToKm(-1));
    }
}
