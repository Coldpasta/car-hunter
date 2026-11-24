# Landed-cost calculator foundations — v1

**Status:** implementation foundation; not a production destination calculator  
**Scope:** generic deterministic scenario aggregation and explicit FX provenance

## What this increment provides

- A validated low / expected / high monetary range.
- Independently itemized cost components with explicit currency and required/estimated flags.
- A pure aggregation service that sums components without calling an LLM or external service.
- An incomplete result with no total when required components are missing, currencies differ, or component codes are duplicated.

## FX rate records

`FxRateRecord` stores one sourced rate for one legal purpose and currency pair. Each record includes:
- Purpose: `CUSTOMS`, `EXCISE` or `IMPORT_VAT`.
- Base and quote ISO 4217 currencies. The numeric rate means quote-currency units for one unit of base currency.
- Positive rate value, HTTPS source URL, publication date and retrieval timestamp.
- Inclusive effective-from / effective-to dates.

`FxRateBook` resolves a rate only when purpose, pair and applicable date match exactly. No match returns an empty result; overlapping matches fail as ambiguous. It does not substitute a recent rate or reuse a rate from another tax purpose.

`FxRateRecord.convert` performs exact decimal multiplication and deliberately applies no rounding. Statutory rounding and conversion-date rules belong to the relevant destination tax calculation and must be implemented explicitly there.

The record model is only a provenance and resolution foundation. It does not fetch rates, establish which legal date applies, or prove that a stored rate is authoritative. The caller must obtain and persist the correct official record for the relevant tax.

## Deliberate boundaries

This layer does **not** calculate customs value, customs duty, excise, VAT, tax bases, or legal eligibility. Those rules belong in a versioned destination profile and must be backed by current authoritative sources.

All components passed to the scenario aggregator must already be expressed in the requested calculation currency. The aggregator rejects mismatched currencies; it does not silently convert them.

A complete arithmetic result means only that the supplied component set is internally consistent and all caller-declared required codes are present. It does not mean that the estimate is legally or commercially complete. A destination profile must decide which components are required and whether the rules and evidence are current.

## Next increments

1. Implement the Poland profile's vehicle classification, origin evidence, tariff lookup and statutory tax-base sequence, using the FX record model only after the applicable official rate/date rules are verified.
2. Add tax-specific calculation traces, statutory rounding and effective dates.
3. Add sourced logistics ranges and quote provenance.
4. Connect the profile result to the report and Import Analyst without allowing the LLM to alter numeric values.
