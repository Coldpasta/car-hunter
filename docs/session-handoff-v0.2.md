# Car Hunter — session handoff (v0.2)

Updated: 2026-11-24

This document is the restart point for the next development session. Read it together with:
- [Product specification](./spec-v0.2.md)
- [Implementation backlog](./backlog-v0.2.md)
- [Delivery status](./progress-v0.2.md)

## Product goal

Car Hunter is an AI-assisted US vehicle discovery and import due-diligence tool. The user selects a destination country; the source market remains the United States. The product must eventually provide a defensible, itemized landed-cost estimate, not just a vehicle shortlist.

## Non-negotiable product constraints

- Source market: US only for v0.2.
- Only ordinary dealer/private-party retail listings; no auctions, Copart or IAAI.
- Clean Title is eligible only when the listing explicitly states Clean/Clear Title and the exact quote is independently found in the public listing page HTML.
- Page-text confirmation is not legal title-history verification. A vehicle-history check (e.g. NMVTIS/provider) and independent inspection remain necessary.
- Never infer missing taxes, tariffs, rates, shipping costs or FX values. Unsupported destination profiles must return an incomplete/unsupported state, not a fabricated total.
- No automated seller contact, negotiation, deposits, payments or purchases.
- Respect marketplace access restrictions. Do not bypass authentication, CAPTCHAs or anti-bot controls.

## Implemented increments

- PR #2: product spec and backlog.
- PR #3: v0.2 request/domain contracts and destination-profile architecture.
- PR #4: US marketplace query planning and deterministic eligibility checks.
- PR #5: seller-type normalization and VIN/canonical URL deduplication.
- PR #6: independent public listing-page text verification. It uses HTTPS allowlisted hosts, does not follow redirects and confirms the seller quote appears in visible HTML. It does not prove legal title status.
- PR #7 was an initial diagnostics/hardening attempt. Its conflict resolution was defective and the PR is closed; review replacement PR #8 instead.

## Current repair branch

Branch: `fix/v0.2-scout-diagnostics-clean`

It is based directly on current `main` after PR #6 and replaces the broken PR #7 branch. It contains:
- `InvestigationReport.ScoutDiagnostics` and structured rejection records.
- Scout JSON parsing that reads both candidates and reported marketplace coverage.
- Deterministic rejection codes for invalid URL, non-US location, title evidence, seller type, currency, price, year, mileage, make and model.
- Diagnostics for duplicates, candidate-limit exclusions and failed listing-page verification.
- A single bounded HTML-fetch implementation (2 MiB maximum), HTTPS allowlist, no redirects, no URI user-info and only default/443 ports.
- Regression tests for rejection reasons and URL restrictions.
- Cleaned delivery-status documentation.

Important implementation semantics:
- `discoveredCount` is the raw number of Scout candidate objects.
- `eligibleCount` is the number that passed deterministic validation, deduplication, candidate limit and independent page-text verification.
- `searchedSources` contains only valid marketplace names reported by Scout. An empty list means Scout did not provide usable coverage metadata; do not silently replace it with planned query sources.
- Rejections are returned in `scoutDiagnostics.rejections` when a report is produced. If no candidate survives, the service currently throws an exception whose message summarizes rejection codes; consider whether the API should instead return a report with zero candidates in a later, separately tested change.

## Current verification state

Replacement PR: [#8](https://github.com/Coldpasta/car-hunter/pull/8). Its CI status must be checked before merging. Do not claim it is green until the latest workflow run for the branch HEAD completes successfully. Run `mvn --batch-mode --no-transfer-progress clean verify` locally/CI as the acceptance gate.

The prior broken PR #7 branch and its CI failure are not evidence about the replacement branch.

## Next work after this repair is merged

Recommended immediate next increment: research the first destination profile using official sources, then implement deterministic landed-cost arithmetic.

Suggested order:
1. Choose a candidate destination only after checking availability of authoritative official sources.
2. Record customs duty/tariff classification, VAT/excise/other taxes, tax base/order, registration, compliance/modification requirements, effective dates and country-of-origin dependencies.
3. Version the destination ruleset and store source URL, retrieval date and effective date per rule.
4. Add deterministic low/expected/high shipping and handling assumptions, clearly distinguishing quotes from estimates.
5. Add FX source/date and deterministic conversion/rounding.
6. Implement the landed-cost calculator with tests for tax-base ordering, boundary conditions, missing inputs and unsupported destinations.
7. Connect Import Analyst only to calculator output; the LLM may explain results but must not author numeric rates or totals.
8. Then expand vehicle-history/recall evidence, report UX and provenance.

## How to resume

Start by checking:
- the latest commit on `fix/v0.2-scout-diagnostics-clean`;
- its latest GitHub Actions CI run and job logs;
- PR #8 CI result and mergeability.

Review the three repaired classes first:
- `src/main/java/dev/carhunter/service/ListingPageVerifier.java`
- `src/main/java/dev/carhunter/service/CarHunterService.java`
- `src/main/java/dev/carhunter/domain/InvestigationReport.java`

Then inspect the tests before changing behavior. Preserve existing PR #6 page-verification behavior.
