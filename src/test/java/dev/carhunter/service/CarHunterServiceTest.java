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
    private final CarHunterService service = new CarHunterService(agents, new ObjectMapper(), planner, eligibility);

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
                "titleEvidenceQuote":"Clean title","titleEvidenceUrl":"https://www.cars.com/vehicledetail/123"}]}
                """;
    }

    @Test
    void runsFourRolesAndReturnsReportWhenSkepticAccepts() {
        when(agents.run(eq("SCOUT"), anyString(), anyString())).thenReturn(validScoutResult());
        when(agents.run(eq("INVESTIGATOR"), anyString(), anyString())).thenReturn("Reliability research with sources.");
        when(agents.run(eq("SKEPTIC"), anyString(), anyString())).thenReturn("SUFFICIENT\nNo critical contradiction found; listing wording is not independent title verification.");
        when(agents.run(eq("DECISION"), anyString(), anyString())).thenReturn("1. Candidate one — inspect before purchase.");

        InvestigationReport report = service.investigate(criteria());

        assertEquals(1, report.candidates().size());
        assertEquals("https://www.cars.com/vehicledetail/123", report.candidates().getFirst().sourceUrl());
        assertEquals(80467, report.candidates().getFirst().mileageKm());
        assertTrue(report.recommendation().contains("inspect"));
        assertEquals(8, report.activity().size());
        verify(agents, times(1)).run(eq("SCOUT"), anyString(), anyString());
        verify(agents, times(1)).run(eq("INVESTIGATOR"), anyString(), anyString());
        verify(agents, times(1)).run(eq("SKEPTIC"), anyString(), anyString());
        verify(agents, times(1)).run(eq("DECISION"), anyString(), anyString());
    }

    @Test
    void rejectsListingsWithoutDirectHttpsSourceOrTitleEvidence() {
        when(agents.run(eq("SCOUT"), anyString(), anyString())).thenReturn("""
                {"candidates":[{"id":"fake","title":"No source","sourceUrl":"http://example.com/car"}]}
                """);
        assertThrows(IllegalStateException.class, () -> service.investigate(criteria()));
        verify(agents, never()).run(eq("INVESTIGATOR"), anyString(), anyString());
    }

    @Test
    void rejectsUnknownTitleStatusEvenWhenSellerClaimsClean() {
        when(agents.run(eq("SCOUT"), anyString(), anyString())).thenReturn(validScoutResult().replace("\"CLEAN\"", "\"UNKNOWN\""));
        assertThrows(IllegalStateException.class, () -> service.investigate(criteria()));
    }

    @Test
    void rejectsNonUsAndUnapprovedMarketplaceListings() {
        String nonUs = validScoutResult().replace("\"US\"", "\"GB\"");
        when(agents.run(eq("SCOUT"), anyString(), anyString())).thenReturn(nonUs);
        assertThrows(IllegalStateException.class, () -> service.investigate(criteria()));
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
