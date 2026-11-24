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
    private final CarHunterService service = new CarHunterService(agents, new ObjectMapper());

    private SearchRequest criteria() {
        return new SearchRequest(new BigDecimal("100000"), "PLN", List.of("Poland"),
                "family SUV", List.of("petrol", "hybrid"), "automatic", 2021, 150000, 5);
    }

    @Test
    void runsFourRolesAndReturnsReportWhenSkepticAccepts() {
        when(agents.run(eq("SCOUT"), anyString(), anyString())).thenReturn("""
                {"candidates":[{"id":"car-1","makeModel":"Toyota RAV4","title":"2022 Toyota RAV4 Hybrid",
                "sourceUrl":"https://example.com/listing/1","country":"Poland","price":99000,
                "currency":"PLN","year":2022,"mileageKm":80000,"fuel":"hybrid","transmission":"automatic",
                "sellerClaims":"Full service history"}]}
                """);
        when(agents.run(eq("INVESTIGATOR"), anyString(), anyString())).thenReturn("Reliability research with sources.");
        when(agents.run(eq("SKEPTIC"), anyString(), anyString())).thenReturn("SUFFICIENT\nNo critical contradiction found; vehicle history remains unverified.");
        when(agents.run(eq("DECISION"), anyString(), anyString())).thenReturn("1. Candidate one — inspect before purchase.");

        InvestigationReport report = service.investigate(criteria());

        assertEquals(1, report.candidates().size());
        assertEquals("https://example.com/listing/1", report.candidates().getFirst().sourceUrl());
        assertTrue(report.recommendation().contains("inspect"));
        assertEquals(8, report.activity().size());
        verify(agents, times(1)).run(eq("SCOUT"), anyString(), anyString());
        verify(agents, times(1)).run(eq("INVESTIGATOR"), anyString(), anyString());
        verify(agents, times(1)).run(eq("SKEPTIC"), anyString(), anyString());
        verify(agents, times(1)).run(eq("DECISION"), anyString(), anyString());
    }

    @Test
    void skepticCanTriggerOnlyOneFollowUpRound() {
        when(agents.run(eq("SCOUT"), anyString(), anyString())).thenReturn("""
                {"candidates":[{"id":"car-1","title":"SUV","sourceUrl":"https://example.com/car",
                "price":90000,"currency":"PLN"}]}
                """);
        when(agents.run(eq("INVESTIGATOR"), anyString(), anyString()))
                .thenReturn("Initial research.", "Follow-up research.");
        when(agents.run(eq("SKEPTIC"), anyString(), anyString()))
                .thenReturn("NEEDS_MORE_EVIDENCE\nCheck recall information.", "SUFFICIENT\nStill cannot verify accident history.");
        when(agents.run(eq("DECISION"), anyString(), anyString())).thenReturn("Proceed to independent inspection.");

        InvestigationReport report = service.investigate(criteria());

        assertTrue(report.investigation().contains("FOLLOW-UP RESEARCH"));
        assertEquals(10, report.activity().size());
        verify(agents, times(2)).run(eq("INVESTIGATOR"), anyString(), anyString());
        verify(agents, times(2)).run(eq("SKEPTIC"), anyString(), anyString());
    }

    @Test
    void rejectsListingsWithoutDirectHttpsSource() {
        when(agents.run(eq("SCOUT"), anyString(), anyString())).thenReturn("""
                {"candidates":[{"id":"fake","title":"No source","sourceUrl":"http://example.com/car"}]}
                """);
        assertThrows(IllegalStateException.class, () -> service.investigate(criteria()));
        verify(agents, never()).run(eq("INVESTIGATOR"), anyString(), anyString());
    }
}
