# CAR HUNTER

An autonomous AI car-buying investigator. Give it your requirements; it searches the web, collects candidate listings, investigates reliability and price context, challenges its own findings, and produces an evidence-backed shortlist.

Hacksgiving MVP · Java 21 · Spring Boot · OpenAI Responses API · Fly.io Sprites Connectors

## What it does

1. Scout searches the web for real vehicle listings and extracts structured candidates with source URLs.
2. Investigator researches model-specific reliability, common faults, market price context and missing information.
3. Skeptic independently challenges claims and identifies gaps or contradictions. It can request one bounded follow-up research round.
4. Decision ranks the candidates and explains trade-offs, evidence and questions to ask sellers.

The agents are specialized roles orchestrated sequentially by a bounded workflow. They share one investigation context; they are not four permanently running processes.

## Run locally

Requirements: Java 21 and Maven 3.9+.

Copy .env.example to .env and set OPENAI_BASE_URL=https://api.openai.com plus OPENAI_API_KEY for local development. Then run:

    mvn spring-boot:run

Open http://localhost:8080.

## Fly.io Sprites Connector

1. In the Fly.io dashboard, open your organization → Sprites → Connectors.
2. Add the OpenAI connector using a project API key.
3. Grant access to the Sprite running CAR HUNTER.
4. Keep OPENAI_API_KEY unset in the Sprite. The app calls the Fly gateway at https://api.sprites.dev/v1/gateway/openai/v1/responses; Fly injects the credential at the gateway.
5. Set OPENAI_MODEL to a model enabled for your OpenAI project.

Connector requests are billed to your OpenAI project. The connector keeps the raw API key outside the Sprite.

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

## Important limitations

- Search results and listing details depend on publicly accessible pages and search coverage.
- A listing appearing in a report does not verify accident history, mileage or ownership.
- CAR HUNTER distinguishes reported claims from independently corroborated facts and labels unknowns.
- Scores are decision support, not guarantees or professional mechanical inspections.
- The MVP does not contact sellers, purchase reports or make offers.

## Quality

GitHub Actions runs mvn clean verify on every push and pull request.

## License

MIT. See LICENSE.
