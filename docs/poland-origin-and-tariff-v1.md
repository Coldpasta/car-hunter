# Poland import: vehicle classification, US origin and tariff lookup — v1

**Status:** implementation foundation; no live TARIC integration and no production tariff quote  
**Research snapshot:** 2026-11-24  
**Scope:** US-to-Poland used vehicle import, initially passenger cars

## 1. Legal findings

### 1.1 EU–US tariff measure

Regulation (EU) 2026/1455 Article 1(1) sets the Common Customs Tariff duty to 0% for goods classified under CN codes listed in Annex I and originating in the United States. Annex I explicitly lists CN Chapter 87, "Vehicles other than railway or tramway rolling stock, and parts and accessories thereof"; this includes passenger cars under heading 8703, subject to the exact CN classification. Article 6 refers to the Union's non-preferential origin rules. Article 8 applies the measure from 1 July 2026 through 31 December 2029. Articles 3 and 4 allow suspension/safeguard measures.

This is not a general "cars shipped from the US = 0%" rule. The tariff measure is conditional on both the exact classification and qualifying US origin. A live tariff lookup is still required for the declaration date and exact code; never rely on a copied static rate.

### 1.2 What proves US origin

The European Commission's Q&A on Article 59a of Implementing Regulation 2015/2447 (revision dated 16 October 2026) clarifies:

- There is no single prescribed certificate or standard wording; free evidence applies.
- A third-party "Made in US" statement, invoice origin statement, self-certification or Certificate of Origin is not sufficient by itself.
- A decoded VIN is not sufficient by itself; it may only be supporting background evidence.
- The declarant must hold sufficient evidence of US origin and direct transport when lodging the customs declaration requesting the adjusted duty.
- Direct transport can be supported by transport documents (e.g. bill of lading), contracts and other relevant records.
- If the vehicle passes through a third country, evidence must show customs supervision and no disallowed alteration; transit documents or non-manipulation certificates may support this.
- The Q&A states that if the exporter cannot provide the necessary evidence, the adjusted duty cannot be claimed.

Origin is assessed under the EU non-preferential origin rules (UCC Article 60): wholly obtained in one country, or last substantial, economically justified processing in the relevant country. Purchase country, seller address, dispatch country and VIN decoder output do not independently establish origin.

**Product rule:** origin is never inferred from listing location, seller, invoice address, dispatch port or VIN alone. The system records evidence and a reviewed origin decision. It does not claim legal origin based on an automatic checklist. For the US measure, transport evidence is a separate mandatory gate.

### 1.3 Tariff source

The European Commission describes TARIC as the integrated EU database for tariff and commercial-policy measures, including third-country duty, preferences, quotas, trade-defence measures and restrictions. TARIC data is transmitted daily to national administrations. TARIC does not contain national VAT or excise rates.

ISZTAR4 is the Polish customs tariff information entry point. The implementation must resolve the exact code, origin, import date and applicable measures, not just look up a generic rate for a country.

## 2. Classification contract

A listing body-style string is not a customs classification. The classification decision must be based on objective vehicle characteristics and the applicable Combined Nomenclature / TARIC rules. In particular, passenger cars (heading 8703) must not be conflated with goods vehicles (heading 8704); pickups and converted vehicles require case-specific review.

The implementation stores a reviewed VehicleClassificationAssessment:
- status: CONFIRMED, UNRESOLVED, CONFLICTING or OUT_OF_SCOPE;
- exact classification code (8-digit CN or 10-digit TARIC);
- rationale, evidence references and review timestamp.

Only CONFIRMED classifications may enter tariff resolution. The resolver matches the exact stored code; it does not truncate a 10-digit code, expand a parent heading, or fall back to a similar vehicle class. The model validates code shape but does not decide the legal classification.

## 3. Origin and transport contract

OriginAssessment stores:
- country code (ISO alpha-2);
- review status: CONFIRMED, UNVERIFIED or CONFLICTING;
- evidence references;
- transport status: direct US-to-EU proven, third-country transit under customs supervision and without disallowed alteration proven, unverified or non-compliant;
- review timestamp.

A confirmed origin is a reviewed assertion with evidence, not an automatic conclusion from the presence of documents. For a measure that requires direct transport evidence, tariff resolution accepts only:
- direct US-to-EU transport proven; or
- transit through a third country with customs supervision and no disallowed alteration proven.

Missing or contradictory origin/transport evidence produces an incomplete tariff result, never a default US preference.

## 4. Tariff measure contract

TariffMeasureRecord represents one official, exact measure:
- exact CN/TARIC code;
- origin country;
- duty percentage;
- measure identifier;
- official source URL and retrieval timestamp;
- inclusive effective-from / effective-to dates;
- whether direct-transport evidence is required.

TariffMeasureBook resolves only an exact code + origin + import-date match. It rejects overlapping records as ambiguous and does not reuse expired rates or parent-code rates. The result explicitly distinguishes unresolved classification, unverified/conflicting origin, missing direct-transport evidence and no applicable tariff record.

The current implementation is a deterministic record/resolver foundation only. It does not query TARIC or ISZTAR4, import official tariff datasets, or establish the customs declaration's legal date. No 0% or 10% rate is hardcoded into the application.

## 5. Acceptance boundaries

- Unknown or conflicting vehicle classification never yields a tariff rate.
- Unknown or conflicting origin never yields a tariff rate.
- A US-origin measure requiring direct transport cannot resolve without proven direct transport or proven supervised third-country transit.
- VIN decoding alone cannot be represented as conclusive US-origin proof.
- An expired tariff measure is not reused.
- A parent CN heading is not used as a fallback for a more specific requested code.
- Overlapping exact measures fail as ambiguous.
- Missing live tariff data leaves customs duty and the landed-cost total incomplete.
- A resolved tariff record is evidence of the retrieved measure only; it does not itself certify that customs will accept the declared classification/origin.

## 6. Official sources

1. Regulation (EU) 2026/1455, Articles 1, 3, 4, 6, 8 and Annex I (CN Chapter 87): https://eur-lex.europa.eu/legal-content/EN/TXT/PDF/?uri=CELEX:32026R1455
2. European Commission, Q&A on Article 59a of Implementing Regulation 2015/2447, revised 16 October 2026: https://taxation-customs.ec.europa.eu/document/download/e6ea53a6-3eb6-4a2a-a2df-0b4828ae1188_en?filename=Q%26A+-+UCC-IA+Article+59a+-+rev2.pdf
3. European Commission, non-preferential rules of origin: https://taxation-customs.ec.europa.eu/customs/rules-origin-goods/non-preferential-rules-origin_en
4. Union Customs Code, Regulation (EU) No 952/2013, especially Articles 60–61: https://eur-lex.europa.eu/eli/reg/2013/952/oj
5. European Commission, EU Customs Tariff (TARIC): https://taxation-customs.ec.europa.eu/online-services/online-services-and-databases-customs/eu-customs-tariff-taric_en
6. Polish Ministry of Finance, ISZTAR4 / tariff calculator: https://www.podatki.gov.pl/narzedzia/kalkulator-taryfowy

## 7. Remaining work

- Integrate a reliable, current TARIC/ISZTAR4 data source or a controlled official-data import process.
- Define the legal import date used for tariff resolution and persist it with the customs declaration context.
- Add a reviewed classification workflow for CN 8703 vs 8704 and difficult vehicle configurations.
- Define evidence capture and reviewer workflow for origin and direct transport.
- Add integration tests against dated official tariff fixtures and tests for tariff suspension / safeguard changes.
- Only after those pieces are complete, connect the resolved duty to customs value and the statutory tax-base calculation.
