# CAR HUNTER

An autonomous AI car-buying investigator. Give it your requirements; it searches the web, collects candidate listings, investigates reliability and price context, challenges its own findings, and produces an evidence-backed shortlist.

Hacksgiving MVP · Java 21 · Spring Boot · OpenAI Responses API · Tavily · Fly.io Sprites Connectors

## What it does

1. Tavily searches public sources for vehicle listings in the requested countries.
2. Scout selects individual vehicle listings and extracts structured candidates. Candidate URLs are accepted only when they exactly match a URL returned by Tavily.
3. Tavily performs model/reliability searches for the selected candidates.
4. Investigator analyzes listing evidence, reliability, price context and unknowns.
5. Skeptic independently challenges claims and can request one bounded follow-up Tavily search.
6. Decision ranks the candidates and explains trade-offs, evidence and questions to ask sellers.

The agents are specialized roles orchestrated sequentially by a bounded workflow. They share one investigation context; they are not four permanently running processes.

## Run locally

Requirements: Java 21 and Maven 3.9+.

Set OPENAI_BASE_URL=https://api.openai.com, OPENAI_API_KEY to your OpenAI project key, and TAVILY_BASE_URL to a reachable Tavily API-compatible endpoint. Then run:

    mvn spring-boot:run

Open http://localhost:8080.

## Fly.io Sprites Connectors

1. Add OpenAI and Tavily Connectors in the Fly.io dashboard.
2. Grant both Connectors access to the Sprite label used by CAR HUNTER.
3. Leave OPENAI_API_KEY unset in the Sprite; Fly injects the credential at the OpenAI gateway.
4. Set OPENAI_BASE_URL to the OpenAI Connector gateway base URL and OPENAI_MODEL to a model enabled for your OpenAI project.
5. Set TAVILY_BASE_URL to the Tavily Connector URL ending in /tavily/<connector-id>. Do not append /search; the application adds that path.
6. Do not store your raw Tavily API key in the Sprite; Fly injects it through the Connector.

The application calls POST /search on the Tavily Connector for discovery and research. The OpenAI Connector is used only for model reasoning through /v1/responses; it does not request the unsupported hosted web_search tool.

## API

POST /api/investigations

Example request:

    {
      "budget": 100000,
      "currency": "PLN",
      "countries": ["Poland", "Germany"],
      "bodyType": "family SUV",
      "fuel": ["petrol", "hybrid"],
      "transmission": "automatic",
      "minYear": 2021,
      "maxMileageKm": 150000,
      "candidateLimit": 5
    }

GET /api/health returns application status.

## Search cost

The MVP uses one basic Tavily search per requested country, one model/reliability search per selected candidate, and at most one additional search if the Skeptic requests follow-up evidence. Keep country and candidate limits focused while using the free Tavily quota.

## Important limitations

- Search results and listing details depend on publicly accessible pages and search coverage.
- A listing appearing in a report does not verify accident history, mileage or ownership.
- Candidate URLs must originate in the Tavily response, but the listing's content and seller claims still require independent verification.
- CAR HUNTER distinguishes reported claims from independently corroborated facts and labels unknowns.
- Scores are decision support, not guarantees or professional mechanical inspections.
- The MVP does not contact sellers, purchase reports or make offers.

## Quality

GitHub Actions runs mvn clean verify on every push and pull request.

## License

MIT. See LICENSE.
