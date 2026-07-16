# PLAN — Exchange Rate Management System

> Planning artefact prepared with an AI agent (Cursor) **before** implementation began.
> Based on the assessment brief and the breakdown in `REQUIREMENTS.md`.
> A living document: updated as work progresses and as answers to open questions are clarified.

## 1. Goal and principles

Build an end-to-end exchange rate system: backend (Spring Boot) + scheduler + Angular SPA + AI trend insight.
Key principles derived from the rubric:

- **A working end-to-end slice beats over-engineering.** A simple but complete solution scores higher.
- **AI workflow = 25%** (on par with backend). Planning, config files, `[AI]` commit history, and critical usage are mandatory.
- **Numerical correctness.** All monetary math uses `BigDecimal`, never `double`.
- **A readable commit history** in small logical steps, with AI phases prefixed `[AI]`.

## 2. Accepted assumptions (defaults instead of waiting for answers)

Until the questions in `REQUIREMENTS.md` §11 are answered, we work with these defaults and record them in the README "Assumptions":

| # | Question | Accepted decision |
|---|----------|-------------------|
| Q1 | Formula | ✅ Confirmed: `(toRate / fromRate) × (1 − MAX(toSpread, fromSpread)/100)`, spread subtracted, max of the two taken. |
| Q2 | Cross rate | ✅ Confirmed: `from→to = toRatePerEUR / fromRatePerEUR`. If a currency is missing on the date — **404**, the frontend shows "not found". A historical range without points — `points: []`. |
| Q3 | Rounding | ✅ Confirmed: return it as is, at full `BigDecimal` precision. Internal divisions — scale 12, `HALF_EVEN`. |
| Q4 | Fixer.io | ✅ Confirmed: free key via env `FIXER_API_KEY`; historical data mocked for the demo (seeder, flag `app.seed.enabled`). |
| Q5 | Currency set | ✅ Confirmed: main currencies only — **EUR (base), USD, GBP, AED** (`app.fixer.symbols`); the seeder and frontend selectors are limited to the same set. |
| Q6 | AI | Spring AI + Ollama (local model, e.g. `llama3.1` / `qwen2.5`). Provider configurable via env. |
| Q7 | AI assistant | Cursor (the current environment) → config in `.cursor/rules`. |
| Q8 | DB | H2 (file/mem) by default — one-click startup for the reviewer; a `postgres` profile + docker-compose as an alternative. |
| Q9 | Auth | None (internal open API), CORS open for dev. |
| Q11 | Repository | Monorepo: `backend/` + `frontend/`. |
| Q12 | Docker | `docker-compose.yml` for a full local run (Postgres + Ollama), but the app also runs without Docker (H2 + external Ollama). |

> These defaults do not block the start; we adjust them once answers arrive.

## 3. Architecture (high level)

```
Fixer.io ──(daily 12:05 GMT)──► Scheduler ──► RateService ──► [ DB: exchange_rate ]
                                                    ▲                    │
Angular SPA ──HTTP──► REST Controllers ──► Services ┘                    │
   3 views            /exchange /analytics /exchange/insight             │
                                     │                                    │
                                     └──► CurrencyUsage counters ◄────────┘
                                     └──► Spring AI ChatClient ──► Ollama (LLM)
```

### Backend layers
- `web` — REST controllers + DTOs + error handling (`@RestControllerAdvice`).
- `service` — business logic: spread calculation, data collection, analytics, insight.
- `domain` — JPA entities (`ExchangeRate`, `CurrencyUsage`).
- `repository` — Spring Data JPA + named queries.
- `client` — Fixer.io integration (`RestClient`/`WebClient`).
- `ai` — Spring AI wiring (ChatClient, prompts).
- `scheduler` — `@Scheduled` task + distributed lock.
- `config` — spread config, CORS, OpenAPI, scheduler.

### Data model
- `exchange_rate(id, currency_code, rate, rate_date, base_currency, created_at)`
  - uniqueness: `(currency_code, rate_date)` → the basis for upsert.
- `currency_usage(currency_code PK, query_count, last_queried_date)`
  - incremented via an atomic UPDATE query (not read-modify-write).

## 4. Spread calculation

```
spread(ccy):
  base            -> 0.00%
  JPY|HKD|KRW     -> 3.25%
  MYR|INR|MXN     -> 4.50%
  RUB|CNY|ZAR     -> 6.00%
  else            -> 2.75%

rate(from,to,date) = (toRatePerEUR / fromRatePerEUR) * (1 - MAX(spread(from),spread(to))/100)
```
- Implementation: `SpreadCalculator` (a pure class, easy to test), spreads from `application.yml` → `@ConfigurationProperties`.
- Tests: worked example EUR→PLN = 4.44…, spread group boundaries, base = 0%.

## 5. Concurrency and multi-instance

- **Counters:** an atomic `@Modifying` UPDATE (`UPDATE ... SET count = count + 1`), not loading the entity into memory. Both currencies incremented in one transaction.
- **Scheduler across multiple instances:** ShedLock (JdbcTemplate lock provider) — only one instance runs the task within the window. Rationale in the README (simplicity + reliability vs leader election / external orchestrator).
- **Upsert:** unique index `(currency_code, rate_date)` + conflict handling (insert-or-update), idempotency of re-running for the same day.

## 6. AI insight (Spring AI)

- `ChatClient` (Spring AI) → Ollama.
- Prompt: the system prompt constrains the output to 1–2 sentences, based only on the data; the user prompt contains **real (date, rate) pairs** for the period.
- Endpoint `GET /exchange/insight?from&to&fromDate&toDate`.
- Insight cache keyed on (from,to,fromDate,toDate) — to avoid calling the LLM repeatedly (optional).
- Fallback: if the LLM is unavailable — a clear error/degradation, the frontend shows a message.

## 7. Frontend (Angular)

- Standalone components, routing across 3 tabs, typed models, `HttpClient` + interceptors (loading/error).
- `environment.ts` / `environment.development.ts` → `apiBaseUrl` from env.
- Tabs:
  1. **Calculator** — reactive form, validation, loading, 404 handling.
  2. **Historical** — pair + range selection, table + line chart (ng2-charts/Chart.js), AI insight panel with loading.
  3. **Analytics** — usage visualisation (top currencies, dates).
- A service layer (`ExchangeApiService`) with DTO types matching the backend.

## 8. Testing

- Backend: JUnit5 + Mockito. Required: unit tests for `SpreadCalculator`; an integration test for `/exchange` (MockMvc + H2). A counter atomicity test. An upsert idempotency test.
- Frontend: basic component/service specs (Jasmine/Karma).
- Test suites are generated with AI, then reviewed/fixed (an example fix is recorded in the README "AI Workflow").

## 9. Documentation and submission

- Swagger UI (`springdoc-openapi`) — all endpoints.
- README: setup/run (with and without Docker), architecture, "AI Workflow", assumptions, trade-offs.
- A 3–5 min screen recording: the working application + an AI-agent session.

## 10. Work order (iterations / commits)

1. [x] `[AI]` Planning: PLAN.md, .cursor/rules, repo skeleton, README skeleton.
2. [x] Backend bootstrap: pom, application.yml, main class, H2, Swagger, health (actuator).
3. [x] Domain + repositories + schema migration (unique index).
4. [x] Fixer client + scheduler + ShedLock + upsert + demo seeder.
5. [x] `SpreadCalculator` + `/exchange` + counters (atomic) + formula/integration/concurrency tests.
6. [x] `/analytics` + test.
7. [x] Spring AI + Ollama + `/exchange/insight` + prompt design.
8. [x] Frontend bootstrap, env config, API service, routing.
9. [x] Calculator tab.
10. [x] Historical tab + chart + insight panel.
11. [x] Analytics tab.
12. [~] Polishing: README finalised ✔, Swagger ✔, test coverage ✔;
    backend build/tests run locally (JDK 17 + Maven 3.9.11) — **BUILD SUCCESS, all tests green** ✔;
    Maven wrapper (`mvnw`) added ✔;
    frontend built and tested locally (Node 22 + Angular 18) — **build OK, all specs green** ✔;
    screen recording — pending (manual step).

Each AI-assisted phase → a commit prefixed `[AI]`.

## 11. Risks / trade-offs

- **Fixer free = EUR base only and no historical.** → we accumulate history ourselves + a seeder for the demo; the README states the limitation honestly.
- **Ollama requires a local model** (size/download time). → the README gives exact `ollama pull` commands; the provider is configurable so it can be switched to an OpenAI-compatible endpoint.
- **H2 vs Postgres** — H2 simplifies startup, but ShedLock/upsert are also verified on Postgres (profile + docker-compose).
- **Time.** Prioritisation per the rubric: backend correctness + AI workflow evidence first.

## 12. Open questions

The list is maintained in `REQUIREMENTS.md` §11 (Q1–Q12). Until answers arrive, the defaults from §2 of this plan apply.
