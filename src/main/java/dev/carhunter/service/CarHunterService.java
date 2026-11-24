package dev.carhunter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.carhunter.agent.AgentGateway;
import dev.carhunter.domain.CarCandidate;
import dev.carhunter.domain.InvestigationReport;
import dev.carhunter.domain.SearchRequest;
import dev.carhunter.domain.TitleStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class CarHunterService {
    private static final String SCOUT = """
            You are SCOUT, a US retail vehicle-listing discovery agent.
            Search ONLY the United States. Find ordinary dealer/private-party retail listings, never auctions.
            The source must be a direct vehicle detail page on Cars.com, Autotrader.com or CarGurus.com.
            Return ONLY valid JSON, no markdown, in this exact shape:
            {"candidates":[{"id":"stable-short-id","makeModel":"...","title":"...","sourceUrl":"https://...","country":"US","state":"CA","price":12345,"currency":"USD","year":2022,"mileageMiles":50000,"fuel":"hybrid","transmission":"automatic","sellerClaims":"...","titleStatus":"CLEAN","titleEvidenceQuote":"exact short quote from the listing explicitly saying clean title or clear title","titleEvidenceUrl":"https://same-listing-url","sellerType":"dealer","vin":"17-character-VIN-or-null"}]}
            HARD RULES:
            - Only return a vehicle when the listing itself explicitly states Clean Title or Clear Title.
            - titleEvidenceQuote must be a short verbatim quote from the listing. Do not infer title status from absence of salvage wording.
            - titleEvidenceUrl must be the same direct listing URL as sourceUrl.
            - Exclude salvage, rebuilt, reconstructed, flood, junk, non-repairable, parts-only, auction, Copart and IAAI listings.
            - country must be US and state must be a valid US state abbreviation.
            - sellerType must be dealer or private. Include VIN only when visible in reliable listing details.
            - Never invent a listing, URL, quote, price, year, mileage or other field. Use null for unknown fields.
            - Return only the requested number of distinct vehicles. If there are not enough eligible results, return fewer. If none qualify, return {"candidates":[]}.
            """;
    private static final String INVESTIGATOR = """
            You are INVESTIGATOR, an automotive due-diligence researcher.
            For each US listing, research model/engine/gearbox reliability, known recurring faults, recalls if discoverable, title-history verification options and market-price context.
            Cite source URLs inline. Separate verified facts, seller claims, general model-level risks and unknowns.
            The Java layer has fetched the public listing HTML and checked whether the quoted wording appears in visible page text. Treat that as confirmation of the page text only, not independent title-history verification. Never claim mileage, ownership, accident or title history is verified unless a trustworthy vehicle-specific source proves it.
            """;
    private static final String SKEPTIC = """
            You are SKEPTIC, an independent adversarial reviewer. Do not repeat the investigator's conclusions.
            Try to disprove the strongest claims, identify stale or weak sources, check whether the listing is a direct US retail listing, and challenge any unsupported Clean Title claim. A successful page fetch proves only that the quote appeared in the retrieved HTML, not that the title brand is legally clean.
            Start your response with exactly one token: SUFFICIENT or NEEDS_MORE_EVIDENCE.
            If more evidence is needed, list up to three precise follow-up searches. If evidence remains unavailable, say so explicitly. Do not manufacture certainty.
            """;
    private static final String DECISION = """
            You are the BUYER'S DECISION AGENT. Produce a concise ranked shortlist with a score out of 100 and explain the trade-offs.
            Include price/value context, reliability concerns, listing evidence quality, title-claim limitations, unknowns, and 3-5 questions for the seller or mechanic.
            Make clear that Clean Title in a listing is not a substitute for an NMVTIS/vehicle-history check and independent inspection.
            Distinguish verified facts from seller claims and unknowns. Do not present scores as objective guarantees. End with the most important next step before paying a deposit.
            """;

    private static final Set<String> US_STATES = Set.of("AL","AK","AZ","AR","CA","CO","CT","DE","FL","GA","HI","ID","IL","IN","IA","KS","KY","LA","ME","MD","MA","MI","MN","MS","MO","MT","NE","NV","NH","NJ","NM","NY","NC","ND","OH","OK","OR","PA","RI","SC","SD","TN","TX","UT","VT","VA","WA","WV","WI","WY","DC");
    private static final Set<String> ALLOWED_HOSTS = Set.of("cars.com","www.cars.com","autotrader.com","www.autotrader.com","cargurus.com","www.cargurus.com");

    private final AgentGateway agents;
    private final ObjectMapper mapper;
    private final UsListingQueryPlanner queryPlanner;
    private final ListingEligibility titleEligibility;
    private final ListingDeduplicator deduplicator;
    private final ListingPageVerifier pageVerifier;

    public CarHunterService(AgentGateway agents, ObjectMapper mapper,
                            UsListingQueryPlanner queryPlanner, ListingEligibility titleEligibility, ListingDeduplicator deduplicator, ListingPageVerifier pageVerifier) {
        this.agents = agents;
        this.mapper = mapper;
        this.queryPlanner = queryPlanner;
        this.titleEligibility = titleEligibility;
        this.deduplicator = deduplicator;
        this.pageVerifier = pageVerifier;
    }

    public InvestigationReport investigate(SearchRequest request) {
        List<String> activity = new ArrayList<>();
        String criteria = mapper.valueToTree(request).toPrettyString();
        List<String> queries = queryPlanner.plan(request);

        activity.add("SCOUT: searching US retail marketplaces with three targeted queries");
        String scoutOutput = agents.run("SCOUT", SCOUT,
                "Buyer criteria (JSON):\n" + criteria + "\n\nExecute these marketplace-specific searches:\n- "
                        + String.join("\n- ", queries));
        List<CarCandidate> discovered = deduplicator.deduplicate(parseCandidates(scoutOutput, request))
                .stream().limit(request.candidateLimit()).toList();
        if (discovered.isEmpty()) {
            throw new IllegalStateException("Scout found no listings that meet the US retail and evidenced Clean Title requirements. Try widening the vehicle criteria or budget.");
        }

        List<CarCandidate> candidates = new ArrayList<>();
        List<String> verificationFailures = new ArrayList<>();
        for (CarCandidate candidate : discovered) {
            ListingPageVerifier.Verification verification = pageVerifier.verify(
                    candidate.sourceUrl(), candidate.titleEvidenceQuote());
            if (verification.verified()) {
                candidates.add(candidate);
            } else {
                verificationFailures.add(candidate.sourceUrl() + " — " + verification.reason());
            }
        }
        if (candidates.isEmpty()) {
            throw new IllegalStateException("No listing passed independent page verification. " +
                    String.join(" ", verificationFailures));
        }
        activity.add("SCOUT: independently fetched " + discovered.size() + " listing pages; "
                + candidates.size() + " contained the exact seller title-evidence quote");
        if (!verificationFailures.isEmpty()) {
            activity.add("SCOUT: excluded " + verificationFailures.size()
                    + " listings because page evidence could not be independently confirmed");
        }

        activity.add("INVESTIGATOR: researching reliability, recalls, title checks and price context");
        String research = agents.run("INVESTIGATOR", INVESTIGATOR,
                "Buyer criteria:\n" + criteria + "\n\nEligible listings:\n" + mapper.valueToTree(candidates).toPrettyString());
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
                            + "\n\nListings:\n" + mapper.valueToTree(candidates).toPrettyString());
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
                "Criteria:\n" + criteria + "\n\nListings:\n"
                        + mapper.valueToTree(candidates).toPrettyString()
                        + "\n\nInvestigation:\n" + research + "\n\nSkeptic review:\n" + skeptic);
        activity.add("DECISION: evidence-backed report generated");
        return new InvestigationReport(UUID.randomUUID(), Instant.now(), request,
                candidates, research, skeptic, recommendation, List.copyOf(activity));
    }

    private List<CarCandidate> parseCandidates(String raw, SearchRequest request) {
        try {
            String json = raw.trim();
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start < 0 || end <= start) throw new IllegalArgumentException("Scout did not return JSON.");
            JsonNode root = mapper.readTree(json.substring(start, end + 1));
            List<CarCandidate> candidates = new ArrayList<>();
            for (JsonNode node : root.path("candidates")) {
                String sourceUrl = text(node, "sourceUrl");
                String evidenceUrl = text(node, "titleEvidenceUrl");
                String quote = text(node, "titleEvidenceQuote");
                String state = text(node, "state");
                String country = text(node, "country");
                String titleStatus = text(node, "titleStatus");
                if (!isAllowedListingUrl(sourceUrl) || !sourceUrl.equals(evidenceUrl)) continue;
                if (!"US".equalsIgnoreCase(country) || state == null || !US_STATES.contains(state.toUpperCase(Locale.ROOT))) continue;
                if (!"CLEAN".equalsIgnoreCase(titleStatus) || quote == null) continue;
                if (titleEligibility.classifyTitleClaim(quote, true) != TitleStatus.CLEAN_EVIDENCED) continue;
                String sellerType = normalizeSellerType(text(node, "sellerType"));
                if (sellerType == null || !matchesSellerType(request, sellerType)) continue;
                String currency = text(node, "currency");
                if (!"USD".equalsIgnoreCase(currency)) continue;
                java.math.BigDecimal price = decimal(node, "price");
                Integer year = integer(node, "year");
                Integer miles = integer(node, "mileageMiles");
                String makeModel = text(node, "makeModel");
                if (!matchesBuyerConstraints(request, makeModel, price, year, miles)) continue;
                candidates.add(new CarCandidate(
                        text(node, "id"), makeModel, text(node, "title"), sourceUrl,
                        "US", state.toUpperCase(Locale.ROOT), price, "USD",
                        year, titleEligibility.mileageMilesToKm(miles), text(node, "fuel"),
                        text(node, "transmission"), text(node, "sellerClaims"), "CLEAN", quote, evidenceUrl,
                        sellerType, normalizeVin(text(node, "vin"))));
            }
            return List.copyOf(candidates);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not parse Scout results: " + ex.getMessage(), ex);
        }
    }

    private String normalizeSellerType(String value) {
        if (value == null) return null;
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "dealer" -> "dealer";
            case "private", "private party", "private-party" -> "private";
            default -> null;
        };
    }

    private boolean matchesSellerType(SearchRequest request, String sellerType) {
        return request.sellerTypes() == null || request.sellerTypes().isEmpty()
                || request.sellerTypes().stream().anyMatch(type -> type.equalsIgnoreCase(sellerType));
    }

    private String normalizeVin(String value) {
        if (value == null) return null;
        String vin = value.trim().toUpperCase(Locale.ROOT);
        return vin.matches("[A-HJ-NPR-Z0-9]{17}") ? vin : null;
    }

    private boolean matchesBuyerConstraints(SearchRequest request, String makeModel,
                                             java.math.BigDecimal price, Integer year, Integer mileageMiles) {
        if (price == null || price.signum() <= 0 || price.compareTo(request.budgetAmount()) > 0) return false;
        if (request.minYear() != null && (year == null || year < request.minYear())) return false;
        if (request.maxYear() != null && (year == null || year > request.maxYear())) return false;
        if (request.maxMileageMiles() != null
                && (mileageMiles == null || mileageMiles < 0 || mileageMiles > request.maxMileageMiles())) return false;
        if (request.make() != null && !request.make().isBlank()
                && (makeModel == null || !makeModel.toLowerCase(Locale.ROOT).contains(request.make().toLowerCase(Locale.ROOT)))) return false;
        if (request.model() != null && !request.model().isBlank()
                && (makeModel == null || !makeModel.toLowerCase(Locale.ROOT).contains(request.model().toLowerCase(Locale.ROOT)))) return false;
        return true;
    }

    private boolean isAllowedListingUrl(String value) {
        if (value == null) return false;
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                    && ALLOWED_HOSTS.contains(uri.getHost().toLowerCase(Locale.ROOT))
                    && uri.getPath() != null && uri.getPath().length() > 1;
        } catch (IllegalArgumentException ex) {
            return false;
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
