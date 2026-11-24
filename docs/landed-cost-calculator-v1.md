# Landed-cost calculator foundations — v1

**Status:** implementation foundation; not a production destination calculator  
**Scope:** generic deterministic scenario aggregation

## What this increment provides

- A validated low / expected / high monetary range.
- Independently itemized cost components with explicit currency and required/estimated flags.
- A pure aggregation service that sums components without calling an LLM or external service.
- An incomplete result with no total when required components are missing, currencies differ, or component codes are duplicated.

## Deliberate boundaries

This layer does **not** calculate customs value, customs duty, excise, VAT, FX conversion, tax bases, or legal eligibility. Those rules belong in a versioned destination profile and must be backed by current authoritative sources.

All components passed to the aggregator must already be expressed in the requested calculation currency. The aggregator rejects mismatched currencies; it does not silently convert them.

A complete arithmetic result means only that the supplied component set is internally consistent and all caller-declared required codes are present. It does not mean that the estimate is legally or commercially complete. A destination profile must decide which components are required and whether the rules and evidence are current.

## Next increments

1. Introduce explicit, separately sourced FX-rate records for customs, excise and import VAT where the destination law requires different conversion rules.
2. Implement the Poland profile's vehicle classification, origin evidence, tariff lookup and statutory tax-base sequence.
3. Add tax-specific calculation traces and effective dates.
4. Add sourced logistics ranges and quote provenance.
5. Connect the profile result to the report and Import Analyst without allowing the LLM to alter numeric values.
