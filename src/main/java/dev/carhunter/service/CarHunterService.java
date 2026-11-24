package dev.carhunter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.carhunter.agent.AgentGateway;
import dev.carhunter.agent.TavilySearchGateway;
import dev.carhunter.domain.CarCandidate;
import dev.carhunter.domain.InvestigationReport;
import dev.carhunter.domain.SearchRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CarHunterService {
    private static final String SCOUT = """
            You are SCOUT, a vehicle-listing discovery agent.
            You are given real Tavily search results. Select only actual vehicle listings from those results.
            Return ONLY valid JSON, no markdown fences, with this shape:
            {"candidates":[{"id":"stable-short-id","makeModel":"...","title":"...","sourceUrl":"https://...","country":"...","price":12345,"currency":"PLN","year":2022,"mileageKm":80000,"fuel":"petrol","transmission":"automatic","sellerClaims":"..."}]}
            Rules: sourceUrl MUST be copied exactly from one of the supplied search result URLs. Never invent listings, URLs or fields; use null for unknown values. Return at most the requested candidate limit, prefer distinct vehicles and multiple sources. Ignore search results that are not individual vehicle listings. If no reliable listing is found, return {"candidates":[]} .
            """;
    private static final String INVESTIGATOR = """
            You are INVESTIGATOR, an automotive due-diligence analyst.
            Use the supplied web search results as evidence. Research model/engine/gearbox reliability, known recurring faults, recalls if discoverable, typical market price context, and what cannot be verified.
            Cite source URLs inline. Separate verified facts, seller claims, general model-level risks and unknowns. Never imply a vehicle-specific defect without vehicle-specific evidence. Do not claim mileage or accident history is verified unless a trustworthy source proves it.
            """;
    private static final String SKEPTIC = """
            You are SKEPTIC, an independent adversarial reviewer. Do not repeat the investigator's conclusions.
            Try to disprove the strongest claims, identify stale or weak sources, check whether comparisons are like-for-like, and find missing evidence.
            Start your response with exactly one token: SUFFICIENT or NEEDS_MORE_EVIDENCE.
            If more evidence is needed, list up to three precise follow-up searches. If evidence remains unavailable, say so explicitly. Do not manufacture certainty.
            """;
    private static final String DECISION = """
            You are the BUYER'S DECISION AGENT. Produce a concise ranked shortlist with a score out of 100 and explain the trade-offs.
            Include: why each candidate ranks where it does; price/value context; reliability concerns; evidence quality; unknowns; and 3-5 questions the buyer should ask the seller or mechanic.
            Distinguish verified facts from claims and unknowns. Do not present scores as objective guarantees. End with the most important next step before paying a deposit.
            """;

    private final AgentGateway agents;
    private final TavilySearchGateway search;
    private final ObjectMapper mapper;

    public CarHunterService(AgentGateway agents, TavilySearchGateway search, ObjectMapper mapper) {
        this.agents = agents;
        this.search = search;
        this.mapper = mapper;
    }

    public InvestigationReport investigate(SearchRequest request) {
        List<String> activity = new ArrayList<>();
        String criteria = mapper.valueToTree(request).toPrettyString();

        activity.add("TAVILY: searching public listings for the requested countries and criteria");
        StringBuilder listingEvidence = new StringBuilder();
        Set<String> listingUrls = new LinkedHashSet<>();
        for (String country : request.countries()) {
            String response = search.search(listingQuery(request, country), 8);
            listingEvidence.append("\n\nCOUNTRY: ").append(country).append("\n")
                    .append(formatResults(response, listingUrls, 8));
        }
        if (listingUrls.isEmpty()) {
            throw new IllegalStateException("Tavily found no web results for the requested countries. Try broader criteria.");
        }
        activity.add("TAVILY: collected " + listingUrls.size() + " source URLs for Scout");

        activity.add("SCOUT: selecting real listing candidates from Tavily results");
        String scoutOutput = agents.run("SCOUT", SCOUT,
                "Buyer criteria (JSON):\n" + criteria + "\n\nTavily listing search results:\n" + listingEvidence
                        + "\n\nMaximum candidates: " + request.candidateLimit());
        List<CarCandidate> candidates = parseCandidates(scoutOutput, request.candidateLimit(), listingUrls);
        if (candidates.isEmpty()) {
            throw new IllegalStateException("Scout found no usable individual vehicle listings in Tavily results. Try broader criteria.");
        }
        activity.add("SCOUT: selected " + candidates.size() + " candidates whose URLs were present in Tavily results");

        activity.add("TAVILY: researching reliability and known issues for each candidate");
        StringBuilder vehicleResearch = new StringBuilder();
        for (CarCandidate candidate : candidates) {
            String model = firstNonBlank(candidate.makeModel(), candidate.title());
            String query = model + (candidate.year() == null ? "" : " " + candidate.year())
                    + " reliability common problems engine transmission recalls used car";
            vehicleResearch.append("\n\nCANDIDATE: ").append(model).append("\n")
                    .append(formatResults(search.search(query, 5), new LinkedHashSet<>(), 5));
        }
        activity.add("TAVILY: collected model-level research sources");

        activity.add("INVESTIGATOR: analyzing listing evidence, reliability and price context");
        String research = agents.run("INVESTIGATOR", INVESTIGATOR,
                "Buyer criteria:\n" + criteria + "\n\nCandidates:\n"
                        + mapper.valueToTree(candidates).toPrettyString()
                        + "\n\nOriginal listing evidence:\n" + listingEvidence
                        + "\n\nModel and reliability research from Tavily:\n" + vehicleResearch);
        activity.add("INVESTIGATOR: evidence analysis completed");

        activity.add("SKEPTIC: independently challenging claims and source quality");
        String skeptic = agents.run("SKEPTIC", SKEPTIC,
                "Criteria:\n" + criteria + "\n\nListings:\n" + mapper.valueToTree(candidates).toPrettyString()
                        + "\n\nWeb evidence and investigation:\n" + research
                        + "\n\nOriginal search results:\n" + listingEvidence + vehicleResearch);
        if (skeptic.stripLeading().startsWith("NEEDS_MORE_EVIDENCE")) {
            activity.add("SKEPTIC: requested a bounded follow-up web search");
            String followUpEvidence = search.search(
                    "Automotive due diligence: " + skeptic.replaceFirst("^\\s*NEEDS_MORE_EVIDENCE", "").strip(), 5);
            String formattedFollowUp = formatResults(followUpEvidence, new LinkedHashSet<>(), 5);
            String followUp = agents.run("INVESTIGATOR", INVESTIGATOR,
                    "Perform only this targeted follow-up analysis using the attached Tavily results. Do not repeat completed research.\n"
                            + "Skeptic request:\n" + skeptic + "\n\nTavily follow-up results:\n" + formattedFollowUp
                            + "\n\nExisting investigation:\n" + research);
            research += "\n\nFOLLOW-UP RESEARCH:\n" + followUp
                    + "\n\nFOLLOW-UP WEB EVIDENCE:\n" + formattedFollowUp;
            activity.add("INVESTIGATOR: follow-up analysis completed (maximum one retry)");
            skeptic = agents.run("SKEPTIC", SKEPTIC,
                    "Re-evaluate the updated evidence. No further research rounds are allowed.\n\n"
                            + "Previous review:\n" + skeptic + "\n\nUpdated investigation:\n" + research);
            activity.add("SKEPTIC: final review completed");
        } else {
            activity.add("SKEPTIC: evidence accepted for a cautious ranking");
        }

        activity.add("DECISION: ranking candidates and preparing buyer due-diligence questions");
        String recommendation = agents.run("DECISION", DECISION,
                "Criteria:\n" + criteria + "\n\nCandidates:\n"
                        + mapper.valueToTree(candidates).toPrettyString()
                        + "\n\nInvestigation:\n" + research + "\n\nSkeptic review:\n" + skeptic);
        activity.add("DECISION: evidence-backed report generated");
        return new InvestigationReport(UUID.randomUUID(), Instant.now(), request,
                candidates, research, skeptic, recommendation, List.copyOf(activity));
    }

    private String listingQuery(SearchRequest request, String country) {
        return "used cars for sale " + request.bodyType() + " " + String.join(" ", request.fuel())
                + " " + request.transmission() + " " + request.minYear() + " or newer under "
                + request.maxMileageKm() + " km in " + country + " listings";
    }

    private String formatResults(String raw, Set<String> collectedUrls, int limit) {
        try {
            JsonNode root = mapper.readTree(raw);
            StringBuilder formatted = new StringBuilder();
            int count = 0;
            for (JsonNode result : root.path("results")) {
                String url = text(result, "url");
                if (url == null || !url.startsWith("https://")) continue;
                collectedUrls.add(url);
                formatted.append("- URL: ").append(url).append("\n")
                        .append("  Title: ").append(valueOrUnknown(text(result, "title"))).append("\n")
                        .append("  Content: ").append(truncate(valueOrUnknown(text(result, "content")), 1800)).append("\n");
                if (++count >= limit) break;
            }
            if (count == 0) formatted.append("No usable HTTPS search results.\n");
            return formatted.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not parse Tavily search results: " + ex.getMessage(), ex);
        }
    }

    private List<CarCandidate> parseCandidates(String raw, int limit, Set<String> allowedUrls) {
        try {
            String json = raw.trim();
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start < 0 || end <= start) throw new IllegalArgumentException("Scout did not return JSON.");
            JsonNode root = mapper.readTree(json.substring(start, end + 1));
            List<CarCandidate> candidates = new ArrayList<>();
            for (JsonNode node : root.path("candidates")) {
                String url = text(node, "sourceUrl");
                if (url == null || !allowedUrls.contains(url)) continue;
                candidates.add(new CarCandidate(
                        text(node, "id"), text(node, "makeModel"), text(node, "title"), url,
                        text(node, "country"), decimal(node, "price"), text(node, "currency"),
                        integer(node, "year"), integer(node, "mileageKm"), text(node, "fuel"),
                        text(node, "transmission"), text(node, "sellerClaims")));
                if (candidates.size() >= limit) break;
            }
            return List.copyOf(candidates);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not parse Scout results: " + ex.getMessage(), ex);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }
    private java.math.BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.decimalValue() : null;
    }
    private Integer integer(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isIntegralNumber() ? value.asInt() : null;
    }
    private String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? valueOrUnknown(fallback) : first;
    }
    private String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "Unknown" : value;
    }
    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...";
    }
}
