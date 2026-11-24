# CAR HUNTER — v0.2 Implementation Backlog

This backlog implements [Product Specification v0.2](./spec-v0.2.md). It is intentionally ordered by dependencies. Tasks are implementation-ready starting points; split them into GitHub issues as needed.

## Release objective

Deliver an end-to-end workflow that discovers ordinary used vehicles in the US, only recommends candidates with evidence of Clean Title, investigates the vehicle, and estimates landed cost for a user-selected supported destination country.

## Epic 1 — Domain and API: source market + destination

### CH-201 — Redesign investigation request for import use case
**Priority:** P0 · **Size:** M

- Add required destination country (ISO 3166-1 alpha-2).
- Represent source market as US (fixed/default in v0.2).
- Separate budget amount/currency from destination country.
- Add optional make/model, seller type, US region/ZIP and existing vehicle preferences.
- Validate country code, budget, candidate limit and mutually inconsistent filters.
- Update API examples and validation tests.

**Acceptance:** API rejects missing/invalid destination country; old `countries` source-search semantics are removed or explicitly deprecated.

### CH-202 — Introduce normalized vehicle listing model
**Priority:** P0 · **Size:** M

- Create a normalized listing representation for make/model/trim/year/price/mileage/VIN/location/seller/fuel/transmission/title claim.
- Preserve source URL, domain, retrieved-at timestamp and raw evidence snippet.
- Preserve original mileage unit and normalized km.
- Add validation and serialization tests.

**Acceptance:** all candidate sources map into the same domain model without losing provenance or original units.

## Epic 2 — US market discovery

### CH-203 — Add US marketplace search profiles
**Priority:** P0 · **Size:** M

- Add configurable query profiles for Cars.com, Autotrader and CarGurus.
- Build queries from make/model and vehicle filters; include US context.
- Search only ordinary dealer/private-seller inventory.
- Keep source adapters configurable so blocked/unavailable sources can be disabled without code changes.
- Record searched sources and retrieval timestamps.

**Acceptance:** Scout returns direct US listing URLs or explicitly reports insufficient coverage; it does not pad results with articles or generic category pages.

### CH-204 — Normalize, validate and deduplicate listing results
**Priority:** P0 · **Size:** L

- Extract and normalize listing facts.
- Validate direct listing URL and US location.
- Detect auction pages and exclude them.
- Deduplicate by VIN, then canonical URL/fallback fingerprint.
- Add deterministic hard filters for year, mileage, price and required preferences.
- Keep rejected candidates with machine-readable rejection reasons for diagnostics.

**Acceptance:** same VIN from multiple sources appears once; invalid/non-US/auction/category pages cannot enter the shortlist.

### CH-205 — Implement Clean Title eligibility gate
**Priority:** P0 · **Size:** L

- Model title assessment states: `CLEAN_EVIDENCED`, `UNVERIFIED`, `NON_CLEAN`, `CONFLICTING`.
- Extract title claims and attach source/evidence metadata.
- Exclude explicit salvage, rebuilt, flood, junk, non-repairable and equivalent branded statuses.
- Never infer clean status from missing negative evidence.
- Prevent unverified/conflicting candidates from receiving a Clean Title recommendation.

**Acceptance:** unit tests cover every title state and prove that only `CLEAN_EVIDENCED` passes the clean-title recommendation gate.

## Epic 3 — Vehicle due diligence

### CH-206 — Add VIN and vehicle-history research
**Priority:** P1 · **Size:** L

- Research title/brand, odometer, theft and reported total-loss/flood indicators where available.
- Add source availability and coverage metadata.
- Keep claims distinct from verified facts.
- Treat no-result as unknown, not as a clean record.
- Surface independent inspection recommendation.

**Acceptance:** report identifies which checks ran, which sources returned data and which checks were unavailable.

### CH-207 — Add recall and model-specific reliability investigation
**Priority:** P1 · **Size:** M

- Research recalls and model/year/engine-specific common faults.
- Separate vehicle-specific evidence from model-level reliability context.
- Cite sources and retrieval dates.
- Identify contradictions between listing claims and research.

**Acceptance:** report does not present generic model issues as confirmed defects on the specific vehicle.

### CH-208 — Add US price context
**Priority:** P1 · **Size:** M

- Compare candidates with similar US listings where enough data exists.
- Normalize currency, year, mileage and trim where possible.
- Flag outliers and low-confidence comparisons.
- Avoid unsupported claims that a price is a bargain.

**Acceptance:** price assessment includes comparable evidence or clearly says comparison data is insufficient.

## Epic 4 — Destination profiles and landed-cost engine

### CH-209 — Define destination ruleset interface and versioning
**Priority:** P0 · **Size:** M

- Define a common destination profile contract.
- Require country code, ruleset version, effective date, currency and supported cost components.
- Keep country-specific tax rules isolated from generic arithmetic.
- Add explicit unsupported/incomplete estimate status.
- Add tests for profile selection and missing components.

**Acceptance:** a destination without a complete profile cannot return a complete landed-cost total.

### CH-210 — Implement first supported destination profile
**Priority:** P0 · **Size:** L

- Select the first destination profile based on verified official sources.
- Document duty, VAT, excise/other taxes, registration and compliance assumptions as applicable.
- Store source URLs, retrieval date and effective date for each rule.
- Review country-of-origin and tariff-classification dependencies; do not assume purchase location determines tariff treatment.
- Add boundary and example calculation tests.

**Acceptance:** sample estimates can be reproduced from documented inputs and rules; every tax/rate has a traceable source and date.

### CH-211 — Implement shipping, inland transport and port-cost assumptions
**Priority:** P1 · **Size:** M

- Model US inland transport, export/handling, freight, insurance and destination port costs.
- Support low/expected/high estimates and configurable assumptions.
- Clearly label estimates vs user-provided quotes.
- Avoid presenting illustrative values as live quotes.

**Acceptance:** report shows all shipping-related components separately with source/assumption and scenario range.

### CH-212 — Implement landed-cost calculator
**Priority:** P0 · **Size:** L

- Compose purchase price, logistics, duty, destination taxes, compliance and registration.
- Add FX conversion with rate source/date.
- Keep calculation currency explicit.
- Return itemized components, formulas, scenarios and missing inputs.
- Recalculate on changed destination or input values.

**Acceptance:** deterministic unit tests cover arithmetic, FX, rounding, tax base ordering and incomplete estimates.

## Epic 5 — Agent orchestration and final decision

### CH-213 — Rework Scout prompt/output for US individual listings
**Priority:** P0 · **Size:** M

- Require direct US vehicle listing URLs and structured facts.
- Explicitly reject auctions, generic search pages and non-US inventory.
- Ask Scout to report missing facts rather than infer them.
- Validate Scout output against the normalized listing model.

**Acceptance:** malformed or unsupported Scout output is rejected or marked incomplete by Java validation.

### CH-214 — Add Import Analyst role
**Priority:** P0 · **Size:** M

- Pass selected destination and destination-profile output into the role.
- Explain cost components, assumptions, effective dates and missing data.
- Do not allow the LLM to invent rates or override deterministic calculator output.

**Acceptance:** numeric totals come from the Java calculator; the agent can explain but not authoritatively change them.

### CH-215 — Update Investigator, Skeptic and Decision contracts
**Priority:** P1 · **Size:** M

- Investigator separates vehicle-specific facts from model-level reliability.
- Skeptic checks title conflicts, source quality, calculation assumptions, missing cost items and FX.
- Preserve at most one bounded follow-up research round.
- Decision ranks by fit, landed cost, evidence quality and risk; enforce Clean Title eligibility.

**Acceptance:** no unverified/non-clean title candidate can be described as a clean-title recommendation; unresolved issues appear in final report.

## Epic 6 — Report and UX

### CH-216 — Update search form for US-to-destination workflow
**Priority:** P0 · **Size:** M

- Add required destination-country selector.
- Keep US source market fixed and explain it in UI.
- Add make/model and optional US location filters.
- Show Clean Title-only scope.
- Validate and display unsupported destination state before presenting a misleading total.

**Acceptance:** form submits the v0.2 request shape and explains destination coverage limitations.

### CH-217 — Build landed-cost candidate report
**Priority:** P0 · **Size:** L

- Show listing facts, title status, evidence and direct listing link.
- Show itemized landed cost, currency, FX date and scenarios.
- Show risk flags, confidence, unknowns and seller questions.
- Show sources and timestamps.
- Add disclaimer about non-binding estimates and independent inspection.

**Acceptance:** a buyer can compare candidates on expected landed cost and evidence quality without opening raw agent output.

## Epic 7 — Reliability, tests and delivery

### CH-218 — Add deterministic validation/calculation test suite
**Priority:** P0 · **Size:** M

- Unit tests for listing normalization, units, title gates, URL/source validation, deduplication, country profile selection, FX and landed-cost calculations.
- Add service-level tests for partial provider failures.
- Keep most tests independent of live LLM/search calls.

**Acceptance:** `mvn clean verify` passes and core eligibility/calculation logic is covered by deterministic tests.

### CH-219 — Add evidence provenance and observability
**Priority:** P1 · **Size:** M

- Record source, URL, retrieved-at, claim type and confidence.
- Log stage-level success/failure and provider latency.
- Redact secrets and sensitive user inputs.
- Include source coverage and partial failure details in reports.

**Acceptance:** an investigation can be audited from the returned evidence metadata without exposing secrets.

### CH-220 — Update README and deployment configuration
**Priority:** P1 · **Size:** S

- Update product description, API example and limitations for v0.2.
- Document search-provider setup and supported destinations.
- Document that Clean Title requires evidence and that estimates are non-binding.
- Keep secrets out of tracked files and deployment logs.

**Acceptance:** README accurately describes the shipped v0.2 behavior, not planned features.

## Suggested delivery order

| Increment | Scope | Exit condition |
|---|---|---|
| A — Contract | CH-201, CH-202, CH-209 | Request, listing and destination-profile contracts are stable |
| B — Discovery | CH-203, CH-204, CH-205, CH-213 | Valid US Clean Title candidate pipeline works |
| C — Import economics | CH-210, CH-211, CH-212, CH-214 | One destination can produce a reproducible landed-cost estimate |
| D — Investigation | CH-206, CH-207, CH-208, CH-215 | Evidence-backed, risk-adjusted candidate decision works |
| E — Productization | CH-216, CH-217, CH-218, CH-219, CH-220 | End-to-end UI/report is testable and documented |

## Explicitly deferred

- Salvage/rebuilt vehicle mode and Copart/IAAI integration.
- Additional destination profiles beyond the first supported destination.
- Automated seller messaging, offers, payments or purchases.
- Shipping booking and live freight integrations.
- User accounts, saved searches and notifications.
