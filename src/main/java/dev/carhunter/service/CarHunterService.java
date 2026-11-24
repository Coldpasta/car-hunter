package dev.carhunter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.carhunter.agent.AgentGateway;
import dev.carhunter.domain.CarCandidate;
import dev.carhunter.domain.InvestigationReport;
import dev.carhunter.domain.SearchRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CarHunterService {
    private static final String SCOUT = """
            You are SCOUT, a vehicle-listing discovery agent.
            Search the web for real used-car listings matching the buyer criteria.
            Return ONLY valid JSON, no markdown fences, with this shape:
            {"candidates":[{"id":"stable-short-id","makeModel":"...","title":"...","sourceUrl":"https://...","country":"...","price":12345,"currency":"PLN","year":2022,"mileageKm":80000,"fuel":"petrol","transmission":"automatic","sellerClaims":"..."}]}
            Rules: include direct listing URLs, never invent listings or fields, use null for unknown values, return at most the requested candidate limit, prefer distinct vehicles and multiple sources. If no reliable listing is found, return {"candidates":[]}.
            """;
    private static final String INVESTIGATOR = """
            You are INVESTIGATOR, an automotive due-diligence researcher.
            For each candidate, research model/engine/gearbox reliability, known recurring faults, recalls if discoverable, typical market price context, and what cannot be verified from public sources.
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
    private final ObjectMapper mapper;

    public CarHunterService(AgentGateway agents, ObjectMapper mapper) {
        this.agents = agents;
        this.mapper = mapper;
    }

    public InvestigationReport investigate(SearchRequest request) {
        List<String> activity = new ArrayList<>();
        String criteria = mapper.valueToTree(request).toPrettyString();

        activity.add("SCOUT: searching public web sources for matching listings");
        String scoutOutput = agents.run("SCOUT", SCOUT,
                "Buyer criteria (JSON):\n" + criteria);
        List<CarCandidate> candidates = parseCandidates(scoutOutput, request.candidateLimit());
        if (candidates.isEmpty()) {
            throw new IllegalStateException("Scout found no usable listings. Try widening the budget, countries or vehicle criteria.");
        }
        activity.add("SCOUT: collected " + candidates.size() + " listing candidates");

        activity.add("INVESTIGATOR: researching reliability, price context and evidence");
        String research = agents.run("INVESTIGATOR", INVESTIGATOR,
                "Buyer criteria:\n" + criteria + "\n\nCandidates:\n" + mapper.valueToTree(candidates).toPrettyString());
        activity.add("INVESTIGATOR: first research pass completed");

        activity.add("SKEPTIC: independently challenging claims and source quality");
        String skeptic = agents.run("SKEPTIC", SKEPTIC,
                "Criteria:\n" + criteria + "\n\nListings:\n" + mapper.valueToTree(candidates).toPrettyString()
                        + "\n\nInvestigation:\n" + research);
        if (skeptic.stripLeading().startsWith("NEEDS_MORE_EVIDENCE")) {
            activity.add("SKEPTIC: requested a bounded follow-up investigation");
            String followUp = agents.run("INVESTIGATOR", INVESTIGATOR,
                    "Perform only these targeted follow-up searches requested by the skeptic. Do not repeat completed research.\n"
                            + skeptic + "\n\nExisting investigation:\n" + research
                            + "\n\nCandidates:\n" + mapper.valueToTree(candidates).toPrettyString());
            research += "\n\nFOLLOW-UP RESEARCH:\n" + followUp;
            activity.add("INVESTIGATOR: follow-up round completed (maximum one retry)");
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

    private List<CarCandidate> parseCandidates(String raw, int limit) {
        try {
            String json = raw.trim();
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start < 0 || end <= start) throw new IllegalArgumentException("Scout did not return JSON.");
            JsonNode root = mapper.readTree(json.substring(start, end + 1));
            List<CarCandidate> candidates = new ArrayList<>();
            for (JsonNode node : root.path("candidates")) {
                String url = text(node, "sourceUrl");
                if (url == null || !url.startsWith("https://")) continue;
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
}
