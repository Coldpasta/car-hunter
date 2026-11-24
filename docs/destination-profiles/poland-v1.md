# Poland destination profile — research v1

**Status:** Research draft; not production-supported  
**Profile:** `PL`  
**Research snapshot:** 2026-11-24  
**Source market:** United States  
**Initial vehicle scope:** Used passenger cars classified as CN 8703 and treated as passenger cars for Polish excise purposes  
**Currency:** PLN

> This document records the legal and calculation research required before implementing a supported profile. It is not tax or customs advice. A customs authority's classification, customs valuation and origin determination prevail. Do not expose this profile as a complete estimate until the open items below are resolved and verified against the live TARIC/ISZTAR4 tariff for the actual import date.

## 1. Scope and exclusions

Included in the initial profile:
- Used passenger cars imported from the United States into Poland for release for free circulation in the EU.
- Passenger cars classified under CN heading 8703, subject to confirmation of the vehicle's actual CN/TARIC classification.
- Import customs duty, Polish excise, import VAT, and separately itemized registration/compliance costs.

Not included in v1:
- Pickups, vans and goods vehicles potentially classified under CN 8704.
- Motorcycles, motorhomes, historic/collector vehicles and vehicles under special customs procedures.
- Returned goods, temporary admission, inward/outward processing, relocation/transfer-of-residence relief, diplomatic or other exemptions.
- VAT recovery/deduction for business importers.
- A guaranteed road-legal conversion or homologation price.

A listing's marketplace body-style label is not a customs or Polish excise classification. Do not infer CN 8703 or passenger-car status from the listing title alone.

## 2. Customs classification, origin and duty

### 2.1 Required inputs

The tariff decision requires at least:
- Exact CN/TARIC code for the vehicle and its objective characteristics.
- Non-preferential country of origin.
- Customs procedure and date of acceptance of the customs declaration / relevant date for the tariff measure.
- Evidence supporting declared origin.
- Any applicable trade-defence, restriction or additional measure.

The country from which a vehicle is shipped or purchased is not, by itself, its country of origin. The Polish Ministry of Finance customs guide explicitly distinguishes the country of manufacture from the country of dispatch and says customs authorities may require proof of origin.

### 2.2 EU–US measure effective 2026

Regulation (EU) 2026/1455, Article 1(1), sets customs duties at 0% for goods classified under CN codes listed in Annex I and originating in the United States. The regulation entered into force on 1 July 2026 and applies until 31 December 2029, subject to its provisions, including the possibility of suspension.

Annex I explicitly lists CN Chapter 87 ("Vehicles other than railway or tramway rolling stock, and parts and accessories thereof"), which includes heading 8703 passenger cars. This confirms the chapter-level scope of the Regulation for passenger cars; it does not replace exact classification or the live tariff check. The exact CN/TARIC code, origin, import date and any currently applicable/suspended measure must still be checked in TARIC/ISZTAR4 before the engine applies a rate.

The Commission's Article 59a Q&A (updated 16 October 2026) adds an important operational condition: the declarant must hold sufficient evidence of US origin and direct transport when lodging the declaration. A "Made in US" statement, invoice statement, Certificate of Origin or decoded VIN is not sufficient on its own. If transported through a third country, evidence must show customs supervision and no disallowed alteration.

**Implementation rule:**
- Never apply 0% merely because the vehicle was bought, listed, exported or shipped from the US.
- Apply a US-origin measure only when origin is affirmatively established under the applicable non-preferential origin rules and the required evidence is available.
- If origin is unknown, conflicting or unsupported, return `INCOMPLETE_ORIGIN`; do not assume US origin.
- Query the current tariff measure for the exact CN/TARIC code and import date. Do not hardcode a general 10% or 0% duty as a timeless country constant.
- If tariff lookup is unavailable, mark customs duty and the total estimate incomplete.

The conventional third-country rate often cited for CN 8703 is 10%, but this document does not make it a hardcoded production rate. The exact code, date, origin and measures must be checked in TARIC/ISZTAR4.

### 2.3 Customs value

Use the transaction-value method where legally applicable, with the required additions and adjustments under the Union Customs Code. The customs value is not automatically just the winning bid or seller's invoice amount.

Potentially relevant additions include:
- Purchase price actually paid or payable.
- Auction/buyer fees and commissions where legally includible.
- Transport and insurance to the place of introduction into the EU.
- Loading, handling and other legally includible costs.
- Other adjustments required by customs valuation rules.

Keep every cost input separate, record whether it is already included in another amount, and prevent double counting. The precise treatment of auction fees, inland transport, export handling and other charges must be supported by the transaction documents and applicable customs valuation rules; escalate uncertain components rather than assuming.

## 3. Polish excise (akcyza)

### 3.1 Taxable event and base

The Excise Duty Act (Ustawa o podatku akcyzowym), consolidated text Dz.U. 2026 poz. 412:
- Treats import of a passenger car not previously registered in Poland as an excise-taxable event (Article 100).
- For import, sets the base as customs value plus customs duty (Article 104(1)(3)).
- Also includes commission, transport and insurance costs not already included, if incurred up to the first place of destination in Poland (Article 104(4)).
- The first place of destination is the place stated in the transport document or other import document.
- Requires foreign-currency base conversion using the NBP average exchange rate on the date the excise tax obligation arises; if no rate is published that day, use the last preceding published rate (Article 104(12)-(13)).

**Formula for the ordinary import path:**

`Excise base = customs value + customs duty + eligible additions not already included`

Eligible additions must be assessed under Article 104(4)-(5); do not duplicate amounts already included in customs value.

### 3.2 Rates

Article 105 of the Act provides:
- 18.6% for passenger cars with engine capacity above 2,000 cm³, subject to special categories below.
- 9.3% for specified hybrids with combustion engine capacity above 2,000 cm³ and not above 3,500 cm³.
- 1.55% for specified non-plug-in hybrid passenger cars with combustion engine capacity at or below 2,000 cm³.
- 3.1% for other passenger cars.

The Act also provides:
- Exemption for electric passenger vehicles and hydrogen-powered passenger vehicles under Article 109a.
- A time-limited exemption through 31 December 2029 for hybrid passenger cars with combustion engine capacity at or below 2,000 cm³ under Article 163a.

Do not implement a single generic "hybrid" flag. The profile needs separate powertrain classifications and engine capacity, including whether external charging is possible and whether the vehicle meets the statutory hybrid definition. The applicable rate/exemption must be derived from the statutory definition and exact vehicle evidence. Unknown powertrain classification means incomplete excise estimate.

### 3.3 Excise exclusions / classification caveat

Polish excise is specifically imposed on passenger cars. A vehicle described as a pickup or light truck may not be a passenger car for excise purposes. CN classification and the Polish statutory definition must be assessed independently; a marketplace label or US title/body type is not sufficient.

## 4. Import VAT

### 4.1 Rate

The Polish standard VAT rate applicable to ordinary passenger-car imports is 23% under the VAT Act's standard-rate provisions and applicable temporary rate provisions (including Article 146ea and subsequent applicable provisions). Revalidate the rate for the actual import date before a production calculation.

### 4.2 Tax base

VAT Act Article 30b(1) provides that the import VAT base is customs value plus customs duty; where the imported goods are subject to excise, it is customs value plus customs duty plus excise.

Article 30b(4)-(6) further includes, where not already included:
- Commission, packing, transport and insurance to the first place of destination in Poland.
- Costs of transport to another EU destination if that destination is known at import.
- Fees and other charges that customs authorities are required to collect on import.

**Ordinary import formula:**

`Import VAT base = customs value + customs duty + Polish excise + eligible additional costs not already included`

`Import VAT = import VAT base × applicable VAT rate`

The calculator must model the first place of destination using the transport document, and must not add the same freight/insurance cost twice if it is already in customs value.

## 5. Currency conversion

Do not use one generic FX rate for all taxes.

- Customs valuation/duty: use the customs exchange rate applicable under EU customs valuation rules on the relevant date. The Polish Ministry of Finance customs guide explains that customs exchange rates are published monthly and remain applicable for the following month.
- Polish excise: use the NBP average exchange rate on the date the excise tax obligation arises, with the statutory fallback to the preceding published rate (Excise Duty Act Article 104(12)-(13)).
- Import VAT: apply the VAT Act / customs conversion rules applicable to the import VAT base and customs declaration. Do not assume the excise FX rate is automatically reusable.

Persist rate, source, publication date, effective period, currency pair and rounding rule separately for each tax calculation.

## 6. Registration and technical compliance

### 6.1 Registration deadline

The Ministry of Infrastructure states that the owner must submit a registration application within 30 days from release for free circulation by the National Revenue Administration for a vehicle imported from outside the EU. A vehicle imported from a non-EU country may be driven for 30 days after customs release under the Road Traffic Law, subject to the applicable conditions.

### 6.2 Registration documents and inspection

The gov.pl registration guidance identifies, among other items:
- Proof of ownership.
- Foreign registration/title documents where applicable.
- Customs clearance evidence.
- Positive technical inspection certificate where required.
- Polish translations of foreign-language documents, subject to the stated exceptions.

The authority may require additional documents depending on the case.

### 6.3 Homologation / individual approval

A US-market vehicle may not have EU type approval / an EU Certificate of Conformity. The exact approval route depends on vehicle category, approval status, modifications and applicable exemptions. The TDT and individual vehicle approval rules must be assessed for the actual vehicle.

Do not represent registration/compliance as a single fixed tax. Show separate cost items for:
- Technical inspection.
- Required lighting / rear fog lamp / speedometer or other adaptations, if applicable.
- Individual approval or other administrative procedure, if required.
- Translation and document handling.
- Registration fees and plates.

Until vehicle-specific requirements and reliable prices are available, these are estimates or unknowns, not sourced statutory amounts.

## 7. Deterministic calculation sequence

For a standard passenger-car import into Poland:

1. Validate passenger-car scope and determine the exact CN/TARIC classification.
2. Establish customs origin and evidence; retrieve the tariff measure for the import date.
3. Determine customs value using the applicable customs valuation method.
4. Convert customs valuation inputs using the applicable customs FX rate.
5. Calculate customs duty from the retrieved tariff measure.
6. Calculate Polish excise base under Article 104, including eligible costs to first destination not already included.
7. Convert the excise base using the statutory NBP rate and calculate excise using Article 105 / applicable exemptions.
8. Calculate import VAT base under VAT Act Article 30b, including customs value, duty, excise and eligible additional costs not already included.
9. Convert and calculate import VAT using the applicable VAT import rules and current rate.
10. Add separately estimated shipping, port, broker, compliance, inspection and registration costs to the buyer's landed cost, ensuring no tax-base component is double counted.
11. Return itemized costs, source references, effective dates, FX provenance, assumptions and missing inputs.

The sequence above is an implementation outline, not a substitute for checking the precise customs declaration, customs valuation, tax point and procedural facts for an individual import.

## 8. Minimum data contract inputs

Required before a complete estimate:
- Import date / intended customs declaration date.
- Vehicle purchase price, currency and supporting document.
- Auction / buyer fees and other purchase-related fees, each itemized.
- Vehicle category and evidence needed to establish passenger-car / CN classification.
- Make, model, year, VIN and country of manufacture/origin evidence.
- Engine type, displacement in cm³, hybrid type, external charging capability, or EV/hydrogen status.
- Inland US transport, export fees, international freight and insurance.
- EU entry point and first destination in Poland, evidenced by transport documents.
- Any transport to another EU destination known at import.
- Customs tariff lookup result and evidence of origin.
- Separate customs, excise and VAT exchange-rate records.

If any mandatory item is missing, return an incomplete estimate with named missing components. Do not silently default to a common engine size, origin, vehicle class, FX rate or tariff.

## 9. Official sources

Research snapshot date: 2026-11-24. Sources should be revalidated before every production ruleset release and tariff lookup should be live/date-specific.

1. Regulation (EU) 2026/1455, including Annex I, Articles 1, 3, 6 and 8: https://eur-lex.europa.eu/legal-content/PL/TXT/?uri=CELEX:32026R1455
2. Union Customs Code, Regulation (EU) No 952/2013: https://eur-lex.europa.eu/eli/reg/2013/952/oj
3. Polish Ministry of Finance / KAS, Investor Guide — Customs Law (legal status stated as 1 January 2026): https://www.podatki.gov.pl/media/k4npw0ao/przewodnik-inwestora-prawo-celne-2026.pdf
4. ISZTAR4 / tariff information and customs calculator: https://www.podatki.gov.pl/narzedzia/kalkulator-taryfowy
5. Excise Duty Act, consolidated text Dz.U. 2026 poz. 412: https://eli.gov.pl/api/acts/DU/2026/412/text/U/D20260412Lj.pdf
6. Polish VAT Act, consolidated text Dz.U. 2024 poz. 361, especially Articles 30b, 31a and 146ea / later applicable provisions: https://eli.gov.pl/api/acts/DU/2024/361/text.html
7. European Commission — VAT taxable amount on import, Articles 85–86 VAT Directive summary: https://taxation-customs.ec.europa.eu/taxation/vat/vat-directive/taxable-amount_en
8. Ministry of Infrastructure — vehicle registration, including non-EU import deadline: https://www.gov.pl/web/infrastruktura/rejestracja-pojazdow
9. Gov.pl — register a vehicle, document and inspection guidance: https://www.gov.pl/web/gov/zarejestruj-pojazd
10. Act on vehicle type approval systems and equipment, Dz.U. 2023 poz. 919: https://eli.gov.pl/api/acts/DU/2023/919/text.html
11. Transport Technical Supervision (TDT) — individual vehicle approval: https://www.tdt.gov.pl/zalatw-sprawe/homologacja-pojazdow/swiadectwo-krajowego-indywidualnego-dopuszczenia-pojazdu/

## 10. Open items before marking PL supported

- Verify the current live TARIC/ISZTAR4 measure for the exact CN/TARIC code, origin and actual customs date, including any suspension or safeguard action.
- Obtain authoritative, operational guidance for the evidence required to claim US origin under Regulation 2026/1455 and the applicable non-preferential origin rules; do not accept VIN alone as conclusive proof.
- Confirm the current 23% import VAT rate and all current statutory amendments on the release date.
- Validate the current customs exchange-rate publication endpoint and precise effective-date logic.
- Validate the treatment of auction fees, domestic transport, export charges, storage, port fees and inland EU transport in customs value vs VAT/excise bases to avoid omissions/double counting.
- Define vehicle data mapping for statutory hybrid definitions, plug-in capability, engine capacity and electric/hydrogen exemptions.
- Determine how to handle US vehicle categories and EU vehicle categories (M1/N1), especially pickups and conversions.
- Document the exact approval route for used US-market vehicles without EU CoC and identify which cases can be estimated vs must be marked unknown.
- Establish sourced ranges or quote inputs for shipping, port, broker, inspection, adaptation, translation and registration costs.
- Add a legal review checklist and a ruleset release process with effective-from / effective-to dates.

**Support decision:** PL v1 is not yet production-supported. The researched tax bases and rates are sufficient to design the calculator contract and tests, but tariff/origin evidence, vehicle-specific classification and compliance costs remain mandatory gates.
