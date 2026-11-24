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
    private static final Set<String> US_STATES = Set.of("AL","AK","AZ","AR","CA","CO","CT","DE","FL","GA","HI","ID","IL","IN","IA","KS","KY","LA","ME","MD","MA","MI","MN","MS","MO","MT","NE","NV","NH","NJ","NM","NY","NC","ND","OH","OK","OR","PA","RI","SC","SD","TN","TX","UT","VT","VA","WA","WV","WI","WY","DC");
    private static final Set<String> ALLOWED_HOSTS = Set.of("cars.com","www.cars.com","autotrader.com","www.autotrader.com","cargurus.com","www.cargurus.com");
    private static final Set<String> REPORTED_SOURCES = Set.of("cars.com", "autotrader.com", "cargurus.com");

    private static final String SCOUT = """
            You are SCOUT, a US retail vehicle-listing discovery agent.
            Search ONLY the United States. Find ordinary dealer/private-party retail listings, never auctions.
            The source must be a direct vehicle detail page on Cars.com, Autotrader.com or CarGurus.com.
            Return ONLY valid JSON, no markdown, with this top-level shape:
            {"candidates":[{"id":"stable-short-id","makeModel":"...","title":"...","sourceUrl":"https://...","country":"US","state":"CA","price":12345,"currency":"USD","year":2022,"mileageMiles":50000,"fuel":"hybrid","transmission":"automatic","sellerClaims":"...","titleStatus":"CLEAN","titleEvidenceQuote":"exact short quote from the listing explicitly saying clean title or clear title","titleEvidenceUrl":"https://same-listing-url","sellerType":"dealer","vin":"17-character-VIN-or-null"}],
             "diagnostics":{"searchedSources":["cars.com","autotrader.com","cargurus.com"],"notes":["source-specific coverage or access limitation"]}}
            HARD RULES:
            - Only return a vehicle when the listing itself explicitly states Clean Title or Clear Title.
            - titleEvidenceQuote must be a short verbatim quote from the listing. Do not infer title status from absence of salvage wording.
            - titleEvidenceUrl must be the same direct listing URL as sourceUrl.
            - Exclude salvage, rebuilt, reconstructed, flood, junk, non-repairable, parts-only, auction, Copart and IAAI listings.
            - country must be US and state must be a valid US state abbreviation.
            - sellerType must be dealer or private. Include VIN only when visible in reliable listing details.
            - Never invent a listing, URL, quote, price, year, mileage or other field. Use null for unknown fields.
            - Return only the requested number of distinct vehicles. If there are not enough eligible results, return fewer. If none qualify, return an empty candidates array.
            - Diagnostics must report sources actually attempted and material access/coverage limitations. Do not claim a source was searched when it was not.
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

    private final AgentGateway agents;
    private final ObjectMapper mapper;
    private final UsListingQueryPlanner queryPlanner;
    private final ListingEligibility titleEligibility;
    private final ListingDeduplicator deduplicator;
    private final ListingPageVerifier pageVerifier;

    public CarHunterService(AgentGateway agents, ObjectMapper mapper,
                            UsListingQueryPlanner queryPlanner, ListingEligibility titleEligibility,
                            ListingDeduplicator deduplicator, ListingPageVerifier pageVerifier) {
        this.agents = agents;
        this.mapper = mapper;
        this.queryPlanner = queryPlanner;
        this.titleEligibility = titleEligibility;
        this.deduplicator = deduplicator;
        this.pageVerifier = pageVerifier;
    }

    public InvestigationReport investigate(SearchRequest request) {
        List<String> activity = new ArrayList<>();
        List<InvestigationReport.Rejection> rejections = new ArrayList<>();
        String criteria = mapper.valueToTree(request).toPrettyString();
        List<String> queries = queryPlanner.plan(request);

        activity.add("SCOUT: searching US retail marketplaces with three targeted queries");
        String scoutOutput = agents.run("SCOUT", SCOUT,
                "Buyer criteria (JSON):\n" + criteria + "\n\nExecute these marketplace-specific searches:\n- "
                        + String.join("\n- ", queries));
        ScoutPayload scout = parseScoutPayload(scoutOutput);
        List<CarCandidate> parsed = parseCandidates(scout.candidatesJson(), request, rejections);
        List<CarCandidate> deduplicated = deduplicator.deduplicate(parsed);
        int duplicateCount = Math.max(0, parsed.size() - deduplicated.size());
        for (int i = 0; i < duplicateCount; i++) {
            rejections.add(new InvestigationReport.Rejection(null, "DUPLICATE_LISTING",
                    "Candidate was removed because its VIN or canonical listing URL duplicates another candidate."));
        }
        List<CarCandidate> discovered = deduplicated.stream().limit(request.candidateLimit()).toList();
        int overLimit = Math.max(0, deduplicated.size() - discovered.size());
        for (int i = 0; i < overLimit; i++) {
            rejections.add(new InvestigationReport.Rejection(null, "CANDIDATE_LIMIT",
                    "Candidate was not processed because the requested candidate limit was reached."));
        }

        List<CarCandidate> candidates = new ArrayList<>();
        for (CarCandidate candidate : discovered) {
            ListingPageVerifier.Verification verification = pageVerifier.verify(
                    candidate.sourceUrl(), candidate.titleEvidenceQuote());
            if (verification.verified()) {
                candidates.add(candidate);
            } else {
                rejections.add(new InvestigationReport.Rejection(candidate.sourceUrl(),
                        "LISTING_PAGE_UNVERIFIED", verification.reason()));
            }
        }

        List<String> activitySources = scout.reportedSources().isEmpty()
                ? List.of()
                : scout.reportedSources();
        activity.add("SCOUT: independently fetched " + discovered.size() + " listing pages; "
                + candidates.size() + " contained the exact seller title-evidence quote");
        if (candidates.size() < discovered.size()) {
            activity.add("SCOUT: excluded " + (discovered.size() - candidates.size())
                    + " listings because page evidence could not be independently confirmed");
        }

        if (candidates.isEmpty()) {
            String summary = summarizeRejections(rejections);
            throw new IllegalStateException("Scout returned no eligible, independently verified listings. "
                    + summary);
        }

        activity.add("INVESTIGATOR: researching reliability, recalls, title checks and price context");
        String research = agents.run("INVESTIGATOR", INVESTIGATOR,
                "Buyer criteria:\n" + criteria + "\n\nEligible listings:\n"
                        + mapper.valueToTree(candidates).toPrettyString());
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

        InvestigationReport.ScoutDiagnostics diagnostics = new InvestigationReport.ScoutDiagnostics(
                scout.rawCandidateCount(), candidates.size(), activitySources, List.copyOf(rejections));
        return new InvestigationReport(UUID.randomUUID(), Instant.now(), request,
                candidates, research, skeptic, recommendation, List.copyOf(activity), diagnostics);
    }

    private ScoutPayload parseScoutPayload(String raw) {
        try {
            String json = raw.trim();
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start < 0 || end <= start) throw new IllegalArgumentException("Scout did not return JSON.");
            JsonNode root = mapper.readTree(json.substring(start, end + 1));
            JsonNode candidateNode = root.path("candidates");
            if (!candidateNode.isArray()) {
                throw new IllegalArgumentException("Scout JSON must contain a candidates array.");
            }
            return new ScoutPayload(candidateNode.toString(), candidateNode.size(),
                    parseSources(root.path("diagnostics").path("searchedSources")));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not parse Scout results: " + ex.getMessage(), ex);
        }
    }

    private List<String> parseSources(JsonNode node) {
        if (!node.isArray()) return List.of();
        List<String> valid = new ArrayList<>();
        for (JsonNode value : node) {
            String source = value.asText("").toLowerCase(Locale.ROOT).trim();
            if (REPORTED_SOURCES.contains(source) && !valid.contains(source)) valid.add(source);
        }
        return List.copyOf(valid);
    }

    private String summarizeRejections(List<InvestigationReport.Rejection> rejections) {
        return rejections.stream()
                .map(r -> r.reasonCode() + ": " + r.reason())
                .distinct().limit(8)
                .reduce((a, b) -> a + "; " + b)
                .orElse("No structured rejection details were returned.");
    }

    private List<CarCandidate> parseCandidates(String candidatesJson, SearchRequest request,
                                                List<InvestigationReport.Rejection> rejections) {
        try {
            JsonNode root = mapper.readTree(candidatesJson);
            List<CarCandidate> candidates = new ArrayList<>();
            for (JsonNode node : root) {
                String sourceUrl = text(node, "sourceUrl");
                String evidenceUrl = text(node, "titleEvidenceUrl");
                String quote = text(node, "titleEvidenceQuote");
                String state = text(node, "state");
                String country = text(node, "country");
                String titleStatus = text(node, "titleStatus");
                String rejection = validateCandidate(node, request, sourceUrl, evidenceUrl, quote, state, country, titleStatus);
                if (rejection != null) {
                    rejections.add(new InvestigationReport.Rejection(sourceUrl, rejection, rejectionMessage(rejection)));
                    continue;
                }
                String sellerType = normalizeSellerType(text(node, "sellerType"));
                String currency = text(node, "currency");
                java.math.BigDecimal price = decimal(node, "price");
                Integer year = integer(node, "year");
                Integer miles = integer(node, "mileageMiles");
                String makeModel = text(node, "makeModel");
                candidates.add(new CarCandidate(
                        text(node, "id"), makeModel, text(node, "title"), sourceUrl,
                        "US", state.toUpperCase(Locale.ROOT), price, currency,
                        year, titleEligibility.mileageMilesToKm(miles), text(node, "fuel"),
                        text(node, "transmission"), text(node, "sellerClaims"), "CLEAN", quote, evidenceUrl,
                        sellerType, normalizeVin(text(node, "vin"))));
            }
            return List.copyOf(candidates);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not parse Scout candidates: " + ex.getMessage(), ex);
        }
    }

    private String validateCandidate(JsonNode node, SearchRequest request, String sourceUrl,
                                     String evidenceUrl, String quote, String state, String country, String titleStatus) {
        if (!isAllowedListingUrl(sourceUrl) || !sourceUrl.equals(evidenceUrl)) return "INVALID_SOURCE_URL";
        if (!"US".equalsIgnoreCase(country) || state == null
                || !US_STATES.contains(state.toUpperCase(Locale.ROOT))) return "NON_US_LOCATION";
        if (!"CLEAN".equalsIgnoreCase(titleStatus) || quote == null
                || titleEligibility.classifyTitleClaim(quote, true) != TitleStatus.CLEAN_EVIDENCED) return "TITLE_NOT_EVIDENCED";
        String sellerType = normalizeSellerType(text(node, "sellerType"));
        if (sellerType == null || !matchesSellerType(request, sellerType)) return "SELLER_TYPE_MISMATCH";
        if (!"USD".equalsIgnoreCase(text(node, "currency"))) return "CURRENCY_NOT_USD";
        java.math.BigDecimal price = decimal(node, "price");
        Integer year = integer(node, "year");
        Integer miles = integer(node, "mileageMiles");
        String makeModel = text(node, "makeModel");
        if (price == null || price.signum() <= 0 || price.compareTo(request.budgetAmount()) > 0) return "PRICE_OUT_OF_RANGE";
        if (request.minYear() != null && (year == null || year < request.minYear())) return "YEAR_BELOW_MINIMUM";
        if (request.maxYear() != null && (year == null || year > request.maxYear())) return "YEAR_ABOVE_MAXIMUM";
        if (request.maxMileageMiles() != null && (miles == null || miles < 0 || miles > request.maxMileageMiles())) return "MILEAGE_OUT_OF_RANGE";
        if (request.make() != null && !request.make().isBlank()
                && (makeModel == null || !makeModel.toLowerCase(Locale.ROOT).contains(request.make().toLowerCase(Locale.ROOT)))) return "MAKE_MISMATCH";
        if (request.model() != null && !request.model().isBlank()
                && (makeModel == null || !makeModel.toLowerCase(Locale.ROOT).contains(request.model().toLowerCase(Locale.ROOT)))) return "MODEL_MISMATCH";
        return null;
    }

    private String rejectionMessage(String code) {
        return switch (code) {
            case "INVALID_SOURCE_URL" -> "URL is not a direct HTTPS listing on an approved marketplace, or the title evidence URL differs.";
            case "NON_US_LOCATION" -> "Listing location is not a recognized US state/DC.";
            case "TITLE_NOT_EVIDENCED" -> "Listing lacks an explicit, classifiable Clean/Clear Title quote.";
            case "SELLER_TYPE_MISMATCH" -> "Seller type is missing, unsupported or outside requested filters.";
            case "CURRENCY_NOT_USD" -> "Listing price is not explicitly in USD.";
            case "PRICE_OUT_OF_RANGE" -> "Price is missing, non-positive or above the buyer budget.";
            case "YEAR_BELOW_MINIMUM" -> "Year is missing or below requested minimum.";
            case "YEAR_ABOVE_MAXIMUM" -> "Year is missing or above requested maximum.";
            case "MILEAGE_OUT_OF_RANGE" -> "Mileage is missing, negative or above requested maximum.";
            case "MAKE_MISMATCH" -> "Make does not match buyer criteria.";
            case "MODEL_MISMATCH" -> "Model does not match buyer criteria.";
            default -> "Candidate failed deterministic eligibility validation.";
        };
    }

    private boolean matchesSellerType(SearchRequest request, String sellerType) {
        return request.sellerTypes() == null || request.sellerTypes().isEmpty()
                || request.sellerTypes().stream().anyMatch(type -> type.equalsIgnoreCase(sellerType));
    }

    private String normalizeSellerType(String value) {
        if (value == null) return null;
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "dealer" -> "dealer";
            case "private", "private party", "private-party" -> "private";
            default -> null;
        };
    }

    private String normalizeVin(String value) {
        if (value == null) return null;
        String vin = value.trim().toUpperCase(Locale.ROOT);
        return vin.matches("[A-HJ-NPR-Z0-9]{17}") ? vin : null;
    }

    private boolean isAllowedListingUrl(String value) {
        if (value == null) return false;
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                    && uri.getUserInfo() == null && (uri.getPort() == -1 || uri.getPort() == 443)
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

    private record ScoutPayload(String candidatesJson, int rawCandidateCount, List<String> reportedSources) {}
}
