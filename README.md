# CAR HUNTER

CAR HUNTER is an AI-assisted vehicle discovery and import due-diligence application. It searches ordinary used-car listings in the United States, applies deterministic eligibility checks, investigates vehicle and listing evidence, and is being extended toward transparent landed-cost estimates for a user-selected destination country.

**Current scope:** US source market · dealer and private-seller retail listings · Clean Title evidence gate · Java 21 · Spring Boot · Maven

> **Project status:** early v0.2 implementation. Listing discovery and validation are implemented in part. A complete, production-supported destination profile and end-to-end landed-cost estimate are not yet available. Poland has a researched draft profile, but it is explicitly not production-supported.

## Product scope

The v0.2 product direction is defined in [the product specification](docs/spec-v0.2.md).

- **Source market:** United States only.
- **Listings:** ordinary used-car retail listings from dealers and private sellers.
- **Title scope:** Clean Title candidates only. Missing or ambiguous title evidence remains unverified; branded-title vehicles are excluded.
- **Destinations:** selected by the user using an ISO 3166-1 alpha-2 country code. A destination must not be treated as fully supported until its rules and required cost inputs are implemented and verified.
- **Out of scope:** Copart/IAAI and other auction sourcing, salvage/rebuilt/flood vehicles, seller contact, negotiation, offers, deposits, payments and vehicle purchases.

A marketplace's Clean/Clear Title statement independently confirmed in listing-page text is not proof of legal title history, accident-free condition or mechanical condition. A vehicle-history check and independent inspection are still necessary.

## Investigation workflow

The application orchestrates bounded specialist roles in one investigation:

1. **Tavily** searches the three approved US marketplaces using criteria-specific queries.
2. **Scout** extracts individual vehicle candidates only from the URLs and snippets returned by Tavily.
3. **Deterministic validation** requires an exact URL match to Tavily results, checks listing eligibility, seller type, location, price and other required fields, deduplicates candidates and verifies title wording against accessible listing-page text.
4. **Tavily + Investigator** gather model-level reliability, recall and price-context evidence; the Investigator analyzes the supplied evidence.
5. **Skeptic** challenges weak claims, contradictions and missing information; it can trigger at most one bounded Tavily follow-up search.
6. **Decision** produces a risk-aware shortlist and explains trade-offs.

AI-generated output is advisory. Hard eligibility checks and monetary arithmetic belong in deterministic Java code. Source coverage and public-page availability can vary; the application must disclose missing or conflicting evidence rather than invent facts.

## Import-cost calculation status

The destination-profile interface and registry are in place. Poland's first research draft is available at [docs/destination-profiles/poland-v1.md](docs/destination-profiles/poland-v1.md).

For the hackathon demo, a separate **illustrative Poland estimator** is available in the UI and at `POST /api/demo/import-estimates`. It accepts explicit USD/PLN and duty/excise/VAT scenario assumptions, then returns an itemized PLN breakdown. Its response is marked `ILLUSTRATIVE_ONLY`; it does not query live tariffs or calculate statutory tax bases. See [hackathon MVP scope](docs/hackathon-mvp.md). This demo endpoint must not be presented as a production Poland destination profile.

The generic landed-cost foundation currently includes:
- Validated low / expected / high cost ranges.
- Itemized cost components with an explicit currency and required/estimated flags.
- Deterministic aggregation of components already converted to the calculation currency.
- An incomplete result instead of a total when required components are missing, currencies do not match, or component codes are duplicated.

This foundation does **not** yet calculate customs value, customs duty, excise, VAT, FX conversion, statutory tax bases or vehicle-specific compliance costs. Those rules must be implemented in a versioned destination profile with authoritative sources, effective dates and explicit evidence. See [landed-cost calculator foundations](docs/landed-cost-calculator-v1.md).

## Run locally

Requirements:
- Java 21
- Maven 3.9+

Copy `.env.example` to `.env` and configure the OpenAI settings. For local development, use the OpenAI API base URL and your own project API key:

    OPENAI_BASE_URL=https://api.openai.com
    OPENAI_API_KEY=your-project-api-key
    OPENAI_MODEL=gpt-4.1-mini

Then start the application:

    mvn spring-boot:run

The API listens on port 8080 by default. Set `SERVER_PORT` to change it.

## Fly.io Sprites Connector

For a Fly.io Sprite deployment using the OpenAI Connector:

1. In the Fly.io dashboard, open your organization → Sprites → Connectors.
2. Add the OpenAI connector using a project API key.
3. Grant access to the Sprite running CAR HUNTER.
4. Set `OPENAI_BASE_URL=https://api.sprites.dev/v1/gateway/openai`.
5. Leave `OPENAI_API_KEY` unset in the Sprite; the gateway injects the credential.
6. Set `OPENAI_MODEL` to a model enabled for your OpenAI project.

Connector requests are billed to your OpenAI project. Keep API keys and other secrets out of source control and logs.

## Tavily search integration

Vehicle discovery and evidence retrieval use a separate Tavily Connector. Configure \`TAVILY_BASE_URL\` with the connector gateway URL ending in \`/tavily/<connector-id>\`; do not append \`/search\`, because the application adds that path.

The Sprite must be granted access to the Tavily Connector. The connector injects the Tavily credential, so do not store a raw Tavily API key in the repository or application configuration.

The Java workflow:
- Executes the three marketplace-specific queries produced by \`UsListingQueryPlanner\` for Cars.com, Autotrader and CarGurus.
- Passes the returned result URLs and snippets to Scout. Scout cannot invoke an independent web-search tool.
- Accepts a candidate only when its exact URL appears in the Tavily listing results and it passes the existing HTTPS marketplace allowlist and deterministic eligibility checks.
- Independently fetches the listing page and checks the seller's Clean/Clear Title quote in visible page text. This is evidence of page wording only, not legal title-history verification.
- Uses Tavily for model-level reliability research and allows at most one Skeptic-requested follow-up search.

A URL appearing in Tavily results is not proof that a listing is genuine, eligible or legally clean. If Tavily is not configured, investigation requests fail with a configuration error; the application can still start.

## API

### Create an investigation

`POST /api/investigations`

The v0.2 request uses the US as its fixed source market and requires a destination country. Example:

    {
      "sourceMarket": "US",
      "destinationCountry": "PL",
      "budgetAmount": 25000,
      "budgetCurrency": "USD",
      "make": "Toyota",
      "model": "RAV4",
      "bodyType": "SUV",
      "fuel": ["hybrid"],
      "transmission": "automatic",
      "minYear": 2020,
      "maxYear": 2024,
      "maxMileageMiles": 90000,
      "sellerTypes": ["dealer", "private"],
      "usState": "NJ",
      "usZipCode": "07001",
      "candidateLimit": 5
    }

Required fields:
- `sourceMarket`: must be `US` in v0.2.
- `destinationCountry`: two-letter ISO 3166-1 alpha-2 code.
- `budgetAmount`: at least 1,000 in the selected budget currency.
- `budgetCurrency`: three-letter ISO 4217 code.
- `candidateLimit`: between 1 and 8.

Optional vehicle and location filters are validated by the API. `minYear` cannot exceed `maxYear`; `sellerTypes` accepts `dealer` and `private`.

### Health

`GET /api/health`

Returns the application health status.

## Configuration

See [.env.example](.env.example) for the tracked configuration template.

| Variable | Purpose |
|---|---|
| `OPENAI_BASE_URL` | OpenAI API base URL or Fly Sprite Connector gateway |
| `OPENAI_API_KEY` | Local development API key; leave unset when using the Fly connector |
| `OPENAI_MODEL` | Model enabled for the configured OpenAI project |
| `TAVILY_BASE_URL` | Fly.io Tavily Connector gateway URL, without the `/search` suffix; required for investigations |
| `SERVER_PORT` | HTTP port; defaults to 8080 |

Never commit a populated `.env` file or API credentials.

## Documentation

- [Product specification v0.2](docs/spec-v0.2.md) — product scope, user journey, API/domain direction and acceptance criteria.
- [Implementation backlog v0.2](docs/backlog-v0.2.md) — prioritized delivery tasks.
- [Delivery status v0.2](docs/progress-v0.2.md) — implemented versus partial/not-started work.
- [Session handoff](docs/session-handoff-v0.2.md) — technical restart notes and constraints.
- [Poland destination profile research v1](docs/destination-profiles/poland-v1.md) — legal and tax research; not production-supported.
- [Landed-cost calculator foundations v1](docs/landed-cost-calculator-v1.md) — current arithmetic scope and boundaries.
- [Hackathon MVP](docs/hackathon-mvp.md) — demo workflow, estimator formulas, explicit limitations and presentation script.

## Quality checks

Run the full Maven verification suite:

    mvn --batch-mode --no-transfer-progress clean verify

GitHub Actions runs the test workflow for pushes and pull requests. Deterministic validation and calculation logic should be tested independently of live search or LLM calls.

## Hackathon demo disclaimer

**CAR HUNTER is a hackathon prototype. Its Poland import calculator provides illustrative scenario estimates only — not a customs quotation, tax advice or guaranteed landed cost.** Duty, excise, VAT and USD/PLN rates are entered by the user as assumptions; the demo does not fetch or verify live tariff/tax rates, determine vehicle origin or classification, or apply the complete statutory customs valuation and tax rules. Actual costs depend on the vehicle, its origin and classification, customs valuation, applicable rules and shipment details. Verify all figures with the relevant authorities and qualified import/customs professionals before making a purchase.

## Important limitations

- Search coverage and listing-page access depend on third-party sites and publicly accessible content.
- A listing-page title statement is evidence of what the seller/listing says, not an authoritative title-history report.
- No-result from a history source must be treated as unknown, not as proof of a clean record.
- Import estimates are not customs rulings, tax advice or binding freight quotes.
- Unsupported destinations and missing mandatory inputs must not receive a fabricated all-in total.
- Registration, homologation, modifications and roadworthiness requirements are vehicle- and case-specific.
- CAR HUNTER does not contact sellers, negotiate, purchase reports, arrange shipping or buy vehicles.

## License

MIT. See [LICENSE](LICENSE).
