# CAR HUNTER — Product Specification v0.2

**Status:** Draft for implementation  
**Version:** 0.2  
**Product direction:** US vehicle discovery and cross-border import decision support  
**Supersedes:** v0.1 product scope where it conflicts with this document

## 1. Product vision

CAR HUNTER helps a buyer find a suitable used vehicle in the United States, investigate the vehicle and seller evidence, and estimate the cost and risks of importing it into a destination country selected by the user.

**Positioning:** Find it in America. Understand the real cost of bringing it home.

CAR HUNTER is an investigation and decision-support tool. It is not a broker, customs declarant, vehicle inspector, seller representative, or purchasing agent.

## 2. Decisions locked for v0.2

| Decision | v0.2 choice |
|---|---|
| Source market | United States only |
| Vehicle condition/title scope | Clean Title only |
| Destination country | Selected by the user per investigation |
| Listing type | Ordinary used-car listings from dealers and private sellers |
| Salvage / rebuilt / flood / junk titles | Excluded from candidate recommendations |
| Auction inventory (Copart, IAAI) | Out of scope |
| First-party purchase, seller contact or offer | Out of scope |

A listing whose title status is missing, ambiguous, or not verifiable must not be presented as confirmed Clean Title. It may be shown only as **Title status unverified** and must not enter the clean-title recommended shortlist until evidence is obtained.

## 3. Target user and primary job

A person considering buying a used vehicle in the US and importing it to another country. The user wants to answer:

1. Which real US listings are worth considering?
2. Is the vehicle plausibly a clean-title vehicle, and what evidence supports that?
3. What is known, unknown, or contradictory about its history and condition?
4. What could the vehicle cost after transport, shipping, customs, taxes, compliance work and registration in my destination country?
5. Which candidate offers the best risk-adjusted value, and what should I verify before paying?

## 4. Core user journey

1. **Define the purchase:** budget, currency, vehicle preferences and destination country.
2. **Discover US listings:** search public listing sources and collect direct listing URLs.
3. **Normalize and validate:** extract listing facts, deduplicate, check required fields and filter out non-US, non-individual, auction and non-clean-title candidates.
4. **Investigate:** assess title/history evidence, vehicle-specific reliability, price context, seller/listing credibility and missing facts.
5. **Estimate import cost:** calculate a transparent landed-cost estimate for the selected destination using a versioned country ruleset and explicit assumptions.
6. **Challenge:** independently review the candidate, calculations, evidence and contradictions.
7. **Decide:** return a ranked shortlist with a cost breakdown, risk flags, evidence links, confidence and questions to ask the seller.

## 5. Functional requirements

### 5.1 Investigation input

Required:
- Destination country (ISO 3166-1 alpha-2 code; user-selectable).
- Budget and budget currency.
- At least one vehicle preference: make/model, body type, fuel, transmission, year range or mileage ceiling.

Optional:
- Make and model.
- Body type.
- Fuel type.
- Transmission.
- Minimum/maximum model year.
- Maximum mileage.
- Preferred US state/region or ZIP code and search radius.
- Maximum number of candidates.
- Whether to include private sellers, dealers or both (default: both).

The source market is fixed to the US in v0.2 and is not a user-selectable country.

### 5.2 US listing discovery

- Search public, accessible US vehicle listing pages.
- Start with configurable source adapters/query profiles for major US marketplaces (for example Cars.com, Autotrader and CarGurus); do not assume every site is crawlable or that search results grant permission to bypass access controls.
- Preserve the original listing URL, source domain, retrieval timestamp and evidence snippet.
- Prefer a direct vehicle detail page over category/search result pages.
- Search queries must include US context and relevant vehicle filters; use make/model when supplied.
- Do not fabricate a listing, seller, price, VIN, mileage, location or URL.
- Clearly distinguish a listing page from an editorial article, dealer inventory landing page, category page or auction lot.
- If source coverage is weak, return fewer candidates and explain the limitation rather than padding the result.

### 5.3 Listing normalization and deterministic validation

Normalize, when available:
- Make, model, trim, model year.
- Asking price and currency.
- Mileage and original unit (miles); retain normalized kilometers as a derived value.
- VIN (17 characters where applicable).
- Seller type and seller/dealer name.
- Listing location (city, state, ZIP).
- Fuel, transmission, body type.
- Title-status claim and its source.
- Listing URL, source domain, observed date.

Hard validation rules:
- Listing is for a vehicle physically offered in the United States.
- URL is a direct, usable vehicle listing page.
- Listing is not an auction lot or salvage-auction inventory.
- Title is explicitly represented as Clean/clear by a credible source to qualify for the Clean Title shortlist.
- Exclude explicit Salvage, Rebuilt, Reconstructed, Flood, Junk, Non-repairable, Parts-only or equivalent branded-title states.
- Missing or conflicting title evidence results in **unverified**, not an assumed clean title.
- Invalid/missing price or implausible core data is flagged and cannot silently pass validation.
- Deduplicate by VIN first; fall back to canonical URL and normalized make/model/year/location when VIN is unavailable.

LLM output is advisory. Hard eligibility and arithmetic checks must be implemented deterministically in Java.

### 5.4 Vehicle and title due diligence

For each candidate, investigate:
- Title/brand and reported total-loss/flood/salvage indicators where accessible.
- Odometer readings and inconsistencies where accessible.
- Theft indicators where accessible.
- Recall information where accessible.
- Model/year/engine-specific reliability patterns and common failure modes.
- Price context compared with similar US vehicles.
- Seller/listing credibility signals and inconsistencies between listing and other sources.

Evidence policy:
- Every material claim must have a source URL and retrieval date where available.
- Label claims as **verified from source**, **seller-reported**, **inferred**, **not found**, or **conflicting**.
- Never infer a clean title from the absence of a negative result.
- VIN/history data availability is not guaranteed. A report is not a substitute for an independent inspection or a complete repair/ownership history.
- Recommend an independent pre-purchase inspection and verification of original title documents before payment.

### 5.5 Import cost estimation

Produce a transparent estimate for the destination selected by the user. Model the landed cost as a sum of separately displayed components:

- Vehicle purchase price.
- US domestic transport to export port.
- Export, documentation and handling fees.
- Ocean/air freight as applicable.
- Cargo insurance.
- Port, terminal, storage and destination handling fees.
- Customs duty.
- Destination-country VAT or equivalent tax.
- Destination-specific excise/registration taxes where applicable.
- Required modifications, homologation, inspection and compliance costs.
- Local transport and registration fees.
- Optional contingency allowance.

Rules:
- Keep the purchase currency, calculation currency and FX rate/date explicit.
- Show formula, source, effective date and assumptions for each tax/rate used.
- Do not hardcode one country's tax rules into generic calculation logic.
- Implement country-specific rules as isolated, versioned destination profiles behind a common interface.
- A destination may be selectable only if a profile exists for the required estimate components. If not, return **Destination not yet supported for a complete estimate** and identify missing components; do not produce a misleading all-in total.
- Separate confirmed user inputs, sourced rates, estimates and unknowns.
- Provide low / expected / high scenarios where shipping, handling or compliance costs are uncertain.
- Do not state a customs preference or tariff rate based only on where the car is purchased. Country of origin, tariff classification, trade measures and effective dates may matter and must be verified by the applicable ruleset.
- Recalculate when the destination, vehicle price, shipping assumptions or FX rate changes.

The first implementation must establish the country-profile architecture and one end-to-end supported destination profile. The initial profile should be selected based on reliable official sources and documented before it is marked supported. Adding more destinations is incremental configuration/rules work, not a promise that every country is fully supported at launch.

### 5.6 Agent responsibilities

The existing sequential, bounded agent workflow remains. Agents are roles in one investigation, not permanently running processes.

1. **Scout — US Market Discovery**
   - Find real US individual vehicle listings.
   - Return source URLs and structured candidate facts.
   - Never invent missing data.

2. **Investigator — Vehicle Due Diligence**
   - Research vehicle-specific reliability, listing consistency, title/history evidence and market price context.
   - Mark uncertainty and cite sources.

3. **Import Analyst — Destination Cost**
   - Apply the selected destination profile.
   - Explain each cost component, source, date and assumption.
   - Return incomplete status instead of guessing unsupported taxes.

4. **Skeptic — Independent Challenge**
   - Look for title/status conflicts, weak evidence, implausible prices, missing costs, FX/tax errors and unsupported assumptions.
   - May request at most one bounded follow-up research round.

5. **Decision — Risk-adjusted Shortlist**
   - Rank eligible candidates by fit, expected landed cost, evidence quality and risk.
   - Explain why candidates were included/excluded.
   - Do not rank unverified-title vehicles as clean-title recommendations.

The orchestration layer must enforce role-specific input/output contracts and deterministic validation between agent stages.

## 6. Results / report

The report should include:
- Destination country and estimate currency.
- Search coverage: sources queried, timestamp and limitations.
- Candidate summary and direct listing link.
- Listing facts with source attribution.
- Title status: Clean Title evidenced / Unverified / Excluded, with reason.
- Vehicle-history and reliability findings.
- Import-cost breakdown by component.
- Low / expected / high landed-cost scenarios where applicable.
- FX rate and timestamp.
- Risk flags and confidence level with explanation.
- Questions to ask the seller and checks required before purchase.
- Clear disclaimer that estimates are not a customs/tax ruling or a binding shipping quote.

## 7. API and domain model direction

The investigation request should evolve from the v0.1 country-search model to a source-market/destination model. Suggested request concepts:
- `sourceMarket`: fixed to `US` in v0.2.
- `destinationCountry`: required ISO country code.
- `budgetAmount`, `budgetCurrency`.
- `vehiclePreferences`: make/model/body/fuel/transmission/year/mileage.
- `sellerTypes`, `usRegion`, `candidateLimit`.

Suggested domain concepts:
- `VehicleListing` — normalized listing facts and provenance.
- `TitleAssessment` — status, evidence, confidence and conflicts.
- `VehicleInvestigation` — history/reliability/market findings.
- `DestinationProfile` — supported destination and ruleset version.
- `ImportCostEstimate` — itemized components, scenarios, currency and assumptions.
- `EvidenceReference` — claim, source URL, source type and retrieval timestamp.

Keep API naming consistent with the existing codebase when implementing; this section describes target concepts, not a mandate to perform a broad rename in one commit.

## 8. Non-functional requirements

- Java 21, Spring Boot and Maven remain the implementation baseline.
- External search and AI calls have explicit timeouts, bounded retries and failure handling.
- No secrets in source control, logs, prompts or user-visible reports.
- Persist enough provenance to reproduce how a candidate and estimate were produced.
- Deterministic validation and calculation logic must be unit-tested independently of LLM calls.
- Use structured logs without exposing sensitive user data.
- Respect source access restrictions and robots/terms; do not bypass CAPTCHAs, authentication or anti-bot controls.
- Fail visibly and partially: a failed history source must not invalidate unrelated evidence, but must lower confidence and be disclosed.

## 9. Out of scope for v0.2

- Polish domestic or other non-US source-market search.
- Salvage/rebuilt/flood vehicles and Copart/IAAI auction sourcing.
- Automated seller contact, negotiation, deposits, payments or purchases.
- Booking inspections or shipping.
- Binding customs/tax advice, customs declarations or legal representation.
- Guaranteed VIN history, complete accident/ownership records or mechanical condition.
- Every destination country having complete tax/rate coverage at launch.
- User accounts, saved garages, subscriptions and multi-user collaboration unless separately prioritized.

## 10. MVP acceptance criteria

1. User can submit a search with a required destination country and US vehicle preferences.
2. Search results are US-based direct vehicle listings with traceable URLs and timestamps.
3. Auction lots and explicit non-clean title vehicles are excluded.
4. Missing/ambiguous title status is visibly marked unverified and cannot be recommended as Clean Title.
5. Candidate facts are normalized, unit conversions are correct and duplicates are removed.
6. Material investigation claims carry source references and uncertainty labels.
7. At least one destination profile produces a fully itemized end-to-end landed-cost estimate with documented rules, rates and dates.
8. Unsupported destinations never receive a fabricated all-in estimate.
9. Cost arithmetic, FX handling, title eligibility and hard filters are deterministic and covered by tests.
10. Skeptic can challenge evidence and calculations; the final report includes unresolved issues.
11. Existing CI (`mvn clean verify`) passes.

## 11. Product success measures

Track:
- Share of investigations returning at least one valid direct US listing.
- Share of candidates with VIN and explicit title evidence.
- Candidate deduplication and hard-filter rejection rates.
- Share of estimates with all required destination cost components supported.
- Difference between estimated and later observed import costs (when feedback becomes available).
- User usefulness rating for shortlist and cost breakdown.

Do not optimize for candidate count at the expense of listing validity or evidence quality.
