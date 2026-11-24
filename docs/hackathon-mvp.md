# Hackathon MVP — demo scope

**Status:** Hackathon prototype; not production-supported  
**Source market:** United States  
**Demo destination:** Poland  
**Currency:** PLN

## Demo objective

Tell one coherent story: discover a US retail listing, investigate its available evidence and risks, then estimate an illustrative import budget for Poland. The demo is designed to communicate the product idea, not to provide a legally reliable import quotation.

## Included

1. Existing AI-assisted US retail listing discovery and investigation flow.
2. Existing deterministic listing validation, title-quote verification, deduplication and skeptic review.
3. A separate, deterministic Poland import estimator at `POST /api/demo/import-estimates`.
4. A visible, itemized PLN cost breakdown and explicit assumptions/limitations.
5. User-provided USD/PLN FX and duty, excise and VAT scenario percentages. No tax rate is silently selected by the backend.

## Estimator model (deliberately simplified)

The estimator accepts vehicle price, US inland transport, freight and insurance in USD; destination handling, compliance/modification and registration costs in PLN; one user-entered USD/PLN rate; and user-entered duty, excise and VAT percentages.

- Demo customs value = purchase + US inland transport + freight + insurance, converted using the supplied USD/PLN rate.
- Demo duty = demo customs value × supplied duty percentage.
- Demo excise base = demo customs value + demo duty.
- Demo excise = demo excise base × supplied excise percentage.
- Demo import VAT base = demo customs value + demo duty + demo excise.
- Demo VAT = demo VAT base × supplied VAT percentage.
- Estimated buyer total = demo customs value + duty + excise + VAT + destination handling + compliance/modifications + registration.

Money is rounded to two decimal places using HALF_UP. The formulas are intentionally labelled as demo assumptions; they are not a complete statement of Polish customs/tax law.

## Explicitly not included

- Live TARIC/ISZTAR4 tariff lookup or automatic tariff selection.
- Legal determination of CN/TARIC classification, passenger-car status or country of origin.
- Validation of US-origin preference or direct-transport evidence.
- Separate statutory FX rates/dates for customs, excise and import VAT.
- Full customs valuation rules, eligible additions, tax-base adjustments or statutory rounding.
- Sourced live freight/port/broker/compliance quotes.
- Vehicle-specific homologation, modification or registration assessment.
- Commercial-grade reliability, vehicle-history or recall integrations.

The existing Poland research and origin/tariff foundations remain in place but must not be presented as a production-supported profile.

## Demo guardrails

- All tax percentages and FX values are user-entered scenario assumptions.
- The response status is `ILLUSTRATIVE_ONLY`.
- Every result must display the assumptions and limitations returned by the API.
- Do not describe the output as an exact landed cost, customs quotation, tax advice or guaranteed cost.
- Missing or unverified vehicle/history evidence remains unknown; it must not become a clean bill of health.
- Never imply that purchase/shipment from the US proves US origin or qualifies for 0% duty.

## Suggested presentation

1. Submit a focused US search for a common model (for example, Toyota RAV4 or Ford Mustang Mach-E).
2. Open one source listing and explain what the Clean/Clear Title quote check does and does not prove.
3. Walk through Investigator and Skeptic findings, separating seller claims, sourced facts and unknowns.
4. Open the Poland estimator and enter a plausible sample scenario with clearly labelled assumptions.
5. Show the itemized breakdown, formula assumptions and limitations.
6. Close with the product proposition: help a buyer decide whether a specific US car is worth pursuing, not just convert USD into PLN.

## Next steps after the hackathon

Only if there is evidence of user demand: validate workflows with importers and private buyers, research authoritative data integrations, implement a reviewed Poland ruleset, and add vehicle-specific history providers. Keep all legal calculations deterministic and source-backed; AI may explain evidence and calculations but must not invent rates or totals.
