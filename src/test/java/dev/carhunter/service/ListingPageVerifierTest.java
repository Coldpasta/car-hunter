package dev.carhunter.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ListingPageVerifierTest {
    private final ListingPageVerifier verifier = new ListingPageVerifier();

    @Test
    void refusesHostsOutsideMarketplaceAllowlistWithoutMakingRequest() {
        ListingPageVerifier.Verification result =
                verifier.verify("https://example.com/vehicle/123", "Clean title");

        assertFalse(result.verified());
        assertTrue(result.reason().contains("approved HTTPS marketplace hosts"));
        assertNotNull(result.retrievedAt());
    }

    @Test
    void refusesNonHttpsListingUrls() {
        ListingPageVerifier.Verification result =
                verifier.verify("http://cars.com/vehicledetail/123", "Clean title");

        assertFalse(result.verified());
        assertTrue(result.reason().contains("approved HTTPS marketplace hosts"));
    }
}
