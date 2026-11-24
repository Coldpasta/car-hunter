package dev.carhunter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.carhunter.agent.AgentGateway;
import dev.carhunter.domain.InvestigationReport;
import dev.carhunter.domain.SearchRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CarHunterServiceTest {
    private final AgentGateway agents = mock(AgentGateway.class);
    private final UsListingQueryPlanner planner = new UsListingQueryPlanner();
    private final ListingEligibility eligibility = new ListingEligibility();
    private final ListingDeduplicator deduplicator = new ListingDeduplicator();
    private final ListingPageVerifier pageVerifier = mock(ListingPageVerifier.class);
    private final CarHunterService service = new CarHunterService(agents, new ObjectMapper(), planner, eligibility, deduplicator, pageVerifier);

    private void allowPageEvidence() {
        when(pageVerifier.verify(anyString(), anyString())).thenReturn(
                new ListingPageVerifier.Verification(true, "quote found", java.time.Instant.parse("2026-11-24T00:00:00Z")));
    }

    private SearchRequest criteria() {
        return new SearchRequest("US", "PL", new BigDecimal("25000"), "USD",
                "Toyota", "RAV4", "SUV", List.of("hybrid"), "automatic",
                2021, 2025, 90000, List.of("dealer", "private"), "CA", null, 5);
    }

    private String validScoutResult() {
        return """
                {"candidates":[{"id":"car-1","makeModel":"Toyota RAV4","title":"2022 Toyota RAV4 Hybrid",
                "sourceUrl":"https://www.cars.com/vehicledetail/123","country":"US","state":"CA","price":24000,
                "currency":"USD","year":2022,"mileageMiles":50000,"fuel":"hybrid","transmission":"automatic",
                "sellerClaims":"Dealer listing","titleStatus":"CLEAN",
                "titleEvidenceQuote":"Clean title","titleEvidenceUrl":"https://www.cars.com/vehicledetail/123",
                "sellerType":"dealer","vin":"1HGCM82633A004352"}],
                "diagnostics":{"searchedSources":["cars.com","autotrader.com","cargurus.com"]}}
                """;
    }

    @Test
    void runsFourRolesAndReturnsReportWhenSkepticAccepts() {
        allowPageEvidence();
        when(agents.run(eq("SCOUT"), anyString(), anyString())).thenReturn(validScoutResult());
        when(agents.run(eq("INVESTIGATOR"), anyString(), anyString())).thenReturn("Reliability research with sources.");
        when(agents.run(eq("SKEPTIC"), anyString(), anyString())).thenReturn("SUFFICIENT\\nNo critical contradiction found; listing wording is not independent title verification.");
        when(agents.run(eq("DECISION"), anyString(), anyString())).thenReturn("1. Candidate one — inspect before purchase.");

        InvestigationReport report = service.investigate(criteria());

        assertEquals(1, report.candidates().size());
        assertEquals("https://www.cars.com/vehicledetail/123", report.candidates().getFirst().sourceUrl());
        assertEquals(80467, report.candidates().getFirst().mileageKm());
        assertTrue(report.recommendation().contains("inspect"));
        assertEquals(8, report.activity().size());
        assertEquals(1, report.scoutDiagnostics().discoveredCount());
        assertEquals(1, report.scoutDiagnostics().eligibleCount());
        assertEquals(List.of("cars.com", "autotrader.com", "cargurus.com"), report.scoutDiagnostics().searchedSources());
        assertTrue(report.scoutDiagnostics().rejections().isEmpty());
        verify(agents, times(1)).run(eq("SCOUT"), anyString(), anyString());
        verify(agents, times(1)).run(eq("INVESTIGATOR"), anyString(), anyString());
        verify(agents, times(1)).run(eq("SKEPTIC"), anyString(), anyString());
        verify(agents, times(1)).run(eq("DECISION"), anyString(), anyString());
    }

    @Test
    void recordsStructuredRejectionReasonsWhenNoCandidatePasses() {
        when(agents.run(eq("SCOUT"), anyString(), anyString())).thenReturn("""
                {"candidates":[{"id":"fake","title":"No source","sourceUrl":"http://example.com/car"}],
                 "diagnostics":{"searchedSources":["cars.com"]}}
                """);
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.investigate(criteria()));
        assertTrue(error.getMessage().contains("INVALID_SOURCE_URL"));
        verify(agents, never()).run(eq("INVESTIGATOR"), anyString(), anyString());
    }

    @Test
    void rejectsUnknownTitleStatusEvenWhenSellerClaimsClean() {
        when(agents.run(eq("SCOUT"), anyString(), anyString())).thenReturn(validScoutResult().replace("\"CLEAN\"", "\"UNKNOWN\""));
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.investigate(criteria()));
        assertTrue(error.getMessage().contains("TITLE_NOT_EVIDENCED"));
    }

    @Test
    void rejectsNonUsListings() {
        String nonUs = validScoutResult().replace("\"US\"", "\"GB\"");
        when(agents.run(eq("SCOUT"), anyString(), anyString())).thenReturn(nonUs);
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.investigate(criteria()));
        assertTrue(error.getMessage().contains("NON_US_LOCATION"));
    }

    @Test
    void rejectsCandidatesOutsideBudgetConstraints() {
        String overBudget = validScoutResult().replace("\"price\":24000", "\"price\":26000");
        when(agents.run(eq("SCOUT"), anyString(), anyString())).thenReturn(overBudget);
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.investigate(criteria()));
        assertTrue(error.getMessage().contains("PRICE_OUT_OF_RANGE"));
    }

    @Test
    void rejectsSellerTypeOutsideRequestedFilters() {
        String privateSeller = validScoutResult().replace("\"sellerType\":\"dealer\"", "\"sellerType\":\"private\"");
        when(agents.run(eq("SCOUT"), anyString(), anyString())).thenReturn(privateSeller);
        SearchRequest dealerOnly = new SearchRequest("US", "PL", new BigDecimal("25000"), "USD",
                "Toyota", "RAV4", "SUV", List.of("hybrid"), "automatic",
                2021, 2025, 90000, List.of("dealer"), "CA", null, 5);
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.investigate(dealerOnly));
        assertTrue(error.getMessage().contains("SELLER_TYPE_MISMATCH"));
    }

    @Test
    void rejectsUserInfoAndNonStandardPortsInListingUrls() {
        String userInfo = validScoutResult().replace("https://www.cars.com/vehicledetail/123",
                "https://user@www.cars.com/vehicledetail/123");
        when(agents.run(eq("SCOUT"), anyString(), anyString())).thenReturn(userInfo);
        IllegalStateException userInfoError = assertThrows(IllegalStateException.class, () -> service.investigate(criteria()));
        assertTrue(userInfoError.getMessage().contains("INVALID_SOURCE_URL"));
    }

    @Test
    void plansMarketplaceSpecificUsSearches() {
        List<String> queries = planner.plan(criteria());
        assertEquals(3, queries.size());
        assertTrue(queries.get(0).contains("site:cars.com"));
        assertTrue(queries.get(1).contains("site:autotrader.com"));
        assertTrue(queries.get(2).contains("site:cargurus.com"));
        assertTrue(queries.get(0).contains("clean title"));
        assertTrue(queries.get(0).contains("under $25000"));
    }
}
