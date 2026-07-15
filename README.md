# Exchange Rate Management System

Internal platform that collects live exchange rates, applies a spread-adjusted calculation, serves a
REST API, surfaces the data through an Angular dashboard, and provides an AI-generated trend insight.

> Full requirement breakdown: [`REQUIREMENTS.md`](./REQUIREMENTS.md) · Implementation plan: [`PLAN.md`](./PLAN.md)

## Repository layout

```
.
├── backend/          # Java 17 · Spring Boot · JPA · Spring AI (Maven)
├── frontend/         # Angular 15+ · TypeScript (workspace generated via `ng new`)
├── docker-compose.yml# Postgres + Ollama for full local run (optional)
├── .cursor/rules/    # AI tool configuration (Cursor)
├── PLAN.md           # AI-assisted planning artefact
└── REQUIREMENTS.md   # Parsed brief
```

## Tech stack

- **Backend:** Java 17, Spring Boot 3, Spring Data JPA / Hibernate, H2 (default) or PostgreSQL, ShedLock, springdoc-openapi.
- **Frontend:** Angular (standalone components), TypeScript.
- **AI:** Spring AI + Ollama (local LLM) — swappable for an OpenAI-compatible endpoint.

## Prerequisites

- JDK 17+ and Maven (or the Maven wrapper once generated)
- Node.js LTS + Angular CLI (`npm i -g @angular/cli`)
- (Optional) Docker for Postgres + Ollama
- A free Fixer.io API key

## Running the backend

```bash
cd backend
export FIXER_API_KEY=your_key_here          # Windows PowerShell: $env:FIXER_API_KEY="..."
mvn spring-boot:run
```

- Default DB is file-based H2 (no setup). For PostgreSQL: `mvn spring-boot:run -Dspring-boot.run.profiles=postgres` (with `docker compose up postgres`).
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 console: http://localhost:8080/h2-console

### AI / LLM setup (Ollama)

```bash
docker compose up -d ollama
docker exec -it <ollama_container> ollama pull llama3.1
# configure via env if needed:
export OLLAMA_BASE_URL=http://localhost:11434
export OLLAMA_MODEL=llama3.1
```

## Running the frontend

Angular 18 standalone workspace (calculator, historical + chart + AI insight, analytics).

```bash
cd frontend
npm install
npm start   # ng serve on http://localhost:4200
```

Backend URL is configured in `src/environments/environment*.ts` (`apiBaseUrl`) — no code changes needed to repoint it.

## API overview

| Method | Path                 | Description |
|--------|----------------------|-------------|
| GET    | `/exchange`          | Spread-adjusted rate for a pair (latest or on a date). 404 if missing. |
| GET    | `/exchange/historical` | Rates over a date range (table + chart data). |
| GET    | `/exchange/insight`  | AI-generated trend insight for a period. |
| GET    | `/analytics`         | Usage statistics per currency. |
| POST   | `/admin/refresh`     | (Optional) manual fetch + upsert, no counter side effects. |
| GET    | `/actuator/health`   | Liveness/readiness health check. |

## Architecture overview

See [`PLAN.md`](./PLAN.md) §3. Briefly: a daily ShedLock-guarded scheduler pulls rates from Fixer.io
and upserts them (keyed on currency + API-reported date); `/exchange` computes the spread-adjusted
cross rate on `BigDecimal` and atomically increments per-currency usage counters; `/exchange/insight`
injects the period's real rate points into a Spring AI prompt.

## AI Workflow

<!-- REQUIRED (Section 8.2 of the brief). To be filled in as work progresses. -->
- **Tool used:** Cursor (agent). Configuration lives in `.cursor/rules/` (`project.mdc`, `backend.mdc`, `frontend.mdc`).
- **How it was configured:** project-wide invariants (BigDecimal-only, API-reported date, atomic counters, spread formula) encoded as always-applied rules; language-specific rules scoped by globs.
- **Planning:** `PLAN.md` was produced with the agent before implementation.
- **Commit convention:** AI-assisted commits are prefixed `[AI]`.
- **Examples of overriding / correcting the AI:**
  - The agent initially generated a spread-calculator test that tied the worked example (EUR→PLN)
    to the Appendix B default spread of 2.75%. That contradicts the brief, whose worked example uses
    illustrative spreads (1% / 4%). The test was rewritten to verify the formula's arithmetic identity
    with the correct 4% factor and to test the Appendix B lookups separately.
  - The agent first referenced the pre-GA Spring AI starter (`spring-ai-ollama-spring-boot-starter`).
    Since the project pins Spring AI 1.0.0, this was corrected to the GA artifact
    `spring-ai-starter-model-ollama`.
  - A `BigDecimal` equality assertion (`isEqualTo`) was changed to `isEqualByComparingTo` after
    recognising that H2 returns values at the column scale (12), which breaks scale-sensitive equals.

## Assumptions

See `PLAN.md` §2 (Q1–Q5 confirmed by the customer). Highlights:
- Spread is **subtracted** using the **max** of the two currencies' spreads (confirmed); `exchange` is
  returned with full `BigDecimal` precision (no forced rounding).
- Fixer free plan → base currency is EUR and there is no history endpoint; history is **mocked for the
  demo** via a deterministic seeder (`app.seed.enabled`).
- Collected currencies are limited to the **main set: EUR (base), USD, GBP, AED** (`app.fixer.symbols`);
  the seeder and the UI selectors use the same set.
- Missing rate for a requested pair/date → **HTTP 404**, surfaced as a message on the frontend.
- H2 by default for one-command startup; PostgreSQL profile provided.
- No authentication (internal API); CORS open to the Angular dev server.

## Known trade-offs

See `PLAN.md` §11.

## Testing

```bash
cd backend && mvn test
cd frontend && npm test
```

Backend coverage (tests run against in-memory H2 via the `test` profile, AI mocked):
- `SpreadCalculatorTest` — spread groups, base 0%, higher-spread selection, worked-example arithmetic.
- `ExchangeApiIntegrationTest` — `/exchange` happy path + counter increments, 404 on missing date,
  400 on unknown currency / missing required param / malformed date, `/analytics` reflecting usage (MockMvc).
- `UsageCounterConcurrencyTest` — 50 concurrent queries yield an exact counter (atomic increment).
- `RateCollectionServiceTest` — upsert idempotency and value update on re-run for the same date.
