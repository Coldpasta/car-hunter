# CAR HUNTER — v0.2 delivery status

Updated: 2026-11-24  
Baseline: `main` at `44ed1ca` (PR #12)

This file tracks implementation evidence, not aspirational scope. A task is marked **partial** when the core structure exists but its acceptance criteria are not yet met end to end.

## Delivery increments

| Increment | Status | Evidence / remaining work |
|---|---|---|
| A — Contract | **Mostly complete** | PR #3 introduced the v0.2 request/domain contracts and destination-profile interface/registry. Destination rules and full estimate are not implemented. |
| B — Discovery | **In progress** | PR #4 added US marketplace query planning and deterministic eligibility; PR #5 added seller filtering and VIN/URL deduplication; PR #6 added independent listing-page text confirmation; PR #8 added structured rejection diagnostics and bounded HTML retrieval. |
| C — Import economics | **In progress — foundations only** | PR #9 added the Poland research draft (not production-supported). PR #10 added deterministic low/expected/high scenario aggregation. PR #11 adds sourced, purpose-specific, effective-dated FX rate records and lookup. The current branch adds reviewed vehicle-classification/origin contracts and exact date-specific tariff-record resolution. No live tariff feed or end-to-end tax calculation exists yet. |
| D — Investigation | **Partial** | Investigator, Skeptic and Decision roles exist with bounded follow-up. Vehicle-specific history, recall and structured evidence provenance are not implemented as deterministic product features. |
| E — Productization | **Early / partial** | Basic UI and API exist. Full landed-cost report, destination support states, observability and release hardening remain. README was refreshed in PR #10. |

## Backlog status by task

| Task | Status | Current evidence |
|---|---|---|
| CH-201 Request redesign | **Done** | PR #3 |
| CH-202 Normalized listing model | **Partial** | PR #3 domain contracts; source/retrieval provenance is not yet complete |
| CH-203 US marketplace profiles | **Partial** | PR #4 query planner for Cars.com, Autotrader and CarGurus; not yet configurable source adapters |
| CH-204 Normalize / validate / deduplicate | **Partial** | PR #4 deterministic checks; PR #5 seller normalization and VIN/canonical URL dedup; PR #8 adds rejection diagnostics |
| CH-205 Clean Title gate | **Partial** | Explicit quote and deterministic claim classification; PR #6 adds independent page-text confirmation; branded-title/history verification remains external |
| CH-206 VIN / vehicle history | **Not started** | No vehicle-specific history provider integration |
| CH-207 Recalls / reliability | **Partial** | Agent prompt exists; no structured source-backed result contract |
| CH-208 Price context | **Partial** | Agent prompt exists; no deterministic comparable-listing dataset |
| CH-209 Destination profile architecture | **Partial** | Interface/registry/unsupported destination handling introduced in PR #3 |
| CH-210 First destination profile research | **Partial** | PR #9 documents Poland rules and unresolved legal/tariff/origin/compliance gates; current branch adds classification/origin/tariff resolution foundations; PL is not production-supported |
| CH-211 Shipping and port-cost assumptions | **Not started** | No sourced shipping cost model |
| CH-212 Landed-cost calculator and FX | **Partial — foundations** | PR #10 adds scenario aggregation; PR #11 adds FX records and exact-purpose/date lookup; current branch adds exact tariff measure resolution. Live TARIC/ISZTAR4 ingestion, tax bases, legal formulas, statutory rounding and end-to-end composition remain |
| CH-213 Scout contract and validation | **Partial** | US retail scope, marketplace allowlist, title evidence and hard filters; PR #8 adds structured diagnostics |
| CH-214 Import Analyst | **Not started** | No implemented role connected to a deterministic calculator |
| CH-215 Investigation agent contracts | **Partial** | Prompts and bounded skeptic retry exist; structured evidence contracts remain |
| CH-216 Search form | **Partial** | US source and destination selection exist; supported-country UX needs to reflect actual profiles |
| CH-217 Landed-cost report | **Not started** | No itemized landed-cost output |
| CH-218 Deterministic tests | **Partial** | CI runs `mvn clean verify`; tests cover core filters, title gate, deduplication, listing-page verification, scenario aggregation, FX rate selection and tariff/origin guardrails |
| CH-219 Provenance / observability | **Partial** | Activity log and Scout diagnostics exist; durable end-to-end evidence provenance remains |
| CH-220 README / deployment docs | **Partial** | README refreshed in PR #10; deployment and end-to-end behavior still need final documentation |

## Merged implementation

- PR #2 — product spec and backlog.
- PR #3 — v0.2 domain/API contracts and destination-profile architecture.
- PR #4 — US Scout query planning and deterministic listing eligibility.
- PR #5 — seller-type normalization and listing deduplication.
- PR #6 — independent listing-page text verification.
- PR #8 — Scout diagnostics and listing verifier hardening.
- PR #9 — Poland destination-profile research draft (not production-supported).
- PR #10 — deterministic landed-cost scenario foundations and README refresh.
- PR #11 — explicit tax-specific FX rate records and effective-date lookup.

## Current work

- PR #11 was merged into main (commit 87065eaf).
- Branch feat/poland-origin-tariff-foundation adds reviewed vehicle classification/origin assessments, transport-evidence gating, exact tariff-measure records and date-specific resolution. It does not query live TARIC/ISZTAR4.

## Recommended next steps

1. Complete review and CI for the classification/origin/tariff foundation; do not merge until the exact latest head is green.
2. Integrate a reliable official TARIC/ISZTAR4 tariff data source or controlled official-data import process.
3. Implement the statutory customs, excise and import VAT tax-base sequence with tax-specific FX dates, rounding and calculation traces.
4. Add sourced shipping, port, broker and compliance ranges.
5. Connect Import Analyst only to deterministic calculator output; the LLM may explain results but must not author numeric rates or totals.
6. Add structured vehicle-history/recall evidence and improve final report/UI.
7. Complete end-to-end acceptance tests, provenance/observability and release documentation.

## Important product limitation

The current application is **not yet an end-to-end import-cost advisor**. It is an early US listing discovery and AI-assisted due-diligence prototype. It cannot yet produce a reliable, fully itemized landed-cost estimate for any destination country.


## Hackathon MVP increment (in progress)

Branch: `feature/hackathon-mvp-import-estimator`

- Added a separate `POST /api/demo/import-estimates` endpoint for an illustrative Poland scenario.
- Added a simple UI form with explicit USD/PLN and duty, excise and VAT assumptions.
- The estimator uses deterministic BigDecimal arithmetic and returns a line-item PLN breakdown, formulas, assumptions and limitations.
- The endpoint is intentionally not registered as a production `DestinationProfile`; it does not resolve live tariff measures, origin, CN/TARIC classification, statutory tax bases or tax-specific FX.
- Added estimator unit tests and `docs/hackathon-mvp.md` with a demo script and guardrails.

This increment is for presentation only. It must not be described as a production-supported Poland import calculator.
