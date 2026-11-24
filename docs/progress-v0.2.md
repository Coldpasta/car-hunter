# CAR HUNTER — v0.2 delivery status

Updated: 2026-11-24  
Baseline: `main` at `8822a0f` (PR #6)
Baseline: `main` at `49aced9` (PR #5)

This file tracks implementation evidence, not aspirational scope. A task is marked **partial** when the core structure exists but its acceptance criteria are not yet met end to end.

## Delivery increments

| Increment | Status | Evidence / remaining work |
|---|---|---|
| A — Contract | **Mostly complete** | PR #3 introduced the v0.2 request/domain contracts and destination-profile interface/registry. Destination rules and full estimate are not implemented. |
| B — Discovery | **In progress** | PR #4 added US marketplace query planning and deterministic US/title/seller/budget validation. PR #5 added seller filtering and VIN/URL deduplication. PR #6 is merged and independently verifies the seller's title quote against public listing-page HTML. Machine-readable diagnostics and source coverage are in PR #7. |
| B — Discovery | **In progress** | PR #4 added US marketplace query planning and deterministic US/title/seller/budget validation. PR #5 added seller filtering and VIN/URL deduplication. Direct listing-page evidence verification is in PR #6. Diagnostics, robust source adapters and broader acceptance coverage remain. |
| C — Import economics | **Not started end to end** | Destination profile architecture exists, but no researched country profile plus deterministic landed-cost calculator is available yet. |
| D — Investigation | **Partial** | Investigator, Skeptic and Decision roles exist with bounded follow-up. Vehicle-specific history, recall and structured evidence provenance are not implemented as deterministic product features. |
| E — Productization | **Early / partial** | Basic UI and API exist. Full landed-cost report, destination support states, observability, documentation and release hardening remain. |

## Backlog status by task

| Task | Status | Current evidence |
|---|---|---|
| CH-201 Request redesign | **Done** | PR #3 |
| CH-202 Normalized listing model | **Partial** | PR #3 domain contracts; source/retrieval provenance is not yet complete |
| CH-203 US marketplace profiles | **Partial** | PR #4 query planner for Cars.com, Autotrader and CarGurus; not yet configurable source adapters |
| CH-204 Normalize / validate / deduplicate | **Partial** | PR #4 deterministic checks; PR #5 seller normalization and VIN/canonical URL dedup; rejection diagnostics still missing |
| CH-205 Clean Title gate | **Partial** | Explicit quote and deterministic claim classification exist; PR #6 adds independent page-text confirmation; machine-readable rejection diagnostics are in PR #7; branded-title/history verification remains external |
| CH-205 Clean Title gate | **Partial** | Explicit quote and deterministic claim classification exist; PR #6 adds independent page-text confirmation; branded-title/history verification remains external |
| CH-206 VIN / vehicle history | **Not started** | No vehicle-specific history provider integration |
| CH-207 Recalls / reliability | **Partial** | Agent prompt exists; no structured source-backed result contract |
| CH-208 Price context | **Partial** | Agent prompt exists; no deterministic comparable-listing dataset |
| CH-209 Destination profile architecture | **Partial** | Interface/registry/unsupported destination handling introduced in PR #3 |
| CH-210 First destination profile research | **Not started** | Requires official source research and documented ruleset |
| CH-211 Landed-cost calculator | **Not started** | No deterministic end-to-end calculator |
| CH-212 FX and scenarios | **Not started** | No sourced FX integration or low/expected/high calculation |
| CH-213 Scout contract and validation | **Partial** | US retail scope, marketplace allowlist, title evidence and hard filters; page verification in PR #6 |
| CH-214 Import Analyst | **Not started** | No implemented role connected to a deterministic calculator |
| CH-215 Investigation agent contracts | **Partial** | Prompts and bounded skeptic retry exist; structured evidence contracts remain |
| CH-216 Search form | **Partial** | US source and destination selection exist; supported-country UX needs to reflect actual profiles |
| CH-217 Landed-cost report | **Not started** | No itemized landed-cost output |
| CH-218 Deterministic tests | **Partial** | CI runs `mvn clean verify`; tests cover core filters, title gate, deduplication and, in PR #6, page-verification gating |
| CH-219 Provenance / observability | **Not started** | Activity log is basic; durable source metadata, timestamps and rejection reasons remain |
| CH-220 README / deployment docs | **Partial** | Existing README/deployment setup predates complete v0.2 behavior |

## Merged implementation

- PR #2 — product spec and backlog.
- PR #3 — v0.2 domain/API contracts and destination-profile architecture.
- PR #4 — US Scout query planning and deterministic listing eligibility.
- PR #5 — seller-type normalization and listing deduplication.

## Current implementation in review

- PR #6 — independent listing-page evidence verification. The server fetches only HTTPS pages on the three approved marketplaces, does not follow redirects, and requires the exact Clean/Clear Title quote to appear in visible HTML text. Fetch failures or blocked/JS-only pages fail closed and are excluded. This verifies only that the listing page contains the quote; it does **not** verify legal title status or vehicle history.

## Recommended remaining order

1. Finish and merge PR #6 after CI passes.
2. Add machine-readable Scout rejection reasons and source-coverage diagnostics.
3. Research and document the first destination profile using official sources; do not select or mark a country supported until all required components have reliable rules.
4. Implement deterministic landed-cost calculations, FX date/source and uncertainty scenarios.
5. Connect Import Analyst to calculator output; LLM explains calculations but cannot author totals.
6. Add structured vehicle-history/recall evidence and improve final report/UI.
7. Complete end-to-end acceptance tests, provenance/observability and release documentation.

## Important product limitation

The current application is **not yet an end-to-end import-cost advisor**. It is an early US listing discovery and AI-assisted due-diligence prototype. It cannot yet produce a reliable, fully itemized landed-cost estimate for any destination country.
