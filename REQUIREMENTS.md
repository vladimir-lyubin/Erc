# Exchange Rate Management System — requirements breakdown

> This document is based on the assessment brief.
> It captures my understanding of what needs to be built as a structured checklist,
> plus a list of open questions at the end.

## 0. Task summary

Design and implement an **end-to-end exchange rate management system**:

- backend API (Java/Spring Boot),
- a daily data-collection scheduler,
- an Angular frontend,
- a small AI feature (generate a textual insight about the rate trend).

The task is intentionally open-ended: there is no single correct architecture.
**Not only the result is evaluated, but also HOW I work with AI tools** —
this is a separate and very significant part (25%).

**Submission format:**
- A GitHub repository (public, or private with access granted to the recruiter) with a meaningful commit history;
- A short screen recording (3–5 minutes) demonstrating the working application and at least one AI-agent session (live or a voice-over walkthrough of the recording).

## 1. Fixed stack (cannot be changed)

| Layer | Technologies |
|-------|--------------|
| Backend | Java 17+, Spring Boot, Maven, Hibernate / Spring Data JPA, any relational DB |
| Frontend | Angular v15+, TypeScript everywhere |
| AI | Spring AI (preferred) or LangChain4j + any open-source LLM (Ollama / local model / OpenAI-compatible endpoint) |
| AI development tools | At least one AI assistant explicitly embedded in the workflow (Claude Code, Cursor, GitHub Copilot, etc.) |
| API documentation | Swagger / OpenAPI |

Everything else (project structure, libraries, patterns) is up to me.

Rate source: **https://fixer.io/** (free subscription). Fetch rates **once per day** and store them locally so the application does not depend on the external API on every request.

## 2. Backend — requirements

### 2.1 Data Collection
- A scheduled task that pulls the latest rates from Fixer.io **once per day at 12:05 AM GMT** and writes them to the DB.
- Each record stores: **currency code, rate value, and the rate calculation date — exactly the one returned by the API, not the system date** at fetch time.
- Duplicate rates for the same currency and date must be handled correctly (upsert).
- Account for the service potentially running in **multiple instances** in production — the scheduler must behave correctly in such an environment. The approach and its justification matter more than the specific mechanism (e.g. ShedLock / distributed lock / leader election).

### 2.2 Exchange Rate API
- A REST endpoint accepting: source currency, target currency, and an **optional date**; returns the spread-adjusted rate for the pair.
- The calculation uses only local data from the DB.
- If no date is provided — use the most recent available rates.
- If there are no rates for the requested date — return an appropriate HTTP error (per the brief — 404).
- Each **successful** request increments a usage counter for **each of the two** involved currencies. The increment must be **thread-safe** under concurrent requests.
- Calculation formula — see section 4.

### 2.3 Analytics Endpoint
- An endpoint returning usage statistics: at minimum the **number of requests per currency** and the **dates on which requests were made**.
- The response shape is up to me, but it must support the frontend analytics tab (see 3.3).

### 2.4 Manual Refresh (optional)
- An additional endpoint to manually trigger a fetch + upsert of rates **without touching the usage counters** — an optional extension.

## 3. Frontend (Angular) — requirements

A clean, navigable SPA consuming the backend API. **Three required tabs.**

### 3.1 Rate calculator
- Select two currencies, optionally a date, and show the spread-adjusted rate from the API.
- The form behaves sensibly: field validation, clear error messages on API failure, and a visible loading indicator during the request.

### 3.2 Historical rates & trend chart
- Select a currency pair and date range; show **two things** side by side: a table of the "raw" rates over the period and a **line chart** of the rate movement over time.
- The chart does not need to be fancy — trend clarity is what matters.
- Next to / below the chart — an **AI trend insight** (see section 5).

### 3.3 Analytics dashboard
- Visualise data from the analytics endpoint: which currencies are queried most often, over which periods, and any visible patterns.
- The visualisation approach is up to me.

### 3.4 Frontend standards
- The app runs via `ng serve`, pointing at the backend through a **configurable environment variable** — so a reviewer can run it locally without editing code.
- Quality is evaluated: component design, separation of concerns, type safety, and adherence to Angular best practices.

## 4. Spread-adjusted rate calculation

### 4.1 Formula
The spread of whichever currency in the pair is **higher** is applied:

```
SpreadAdjustedRate = (toRate / fromRate) × (1 − MAX(toSpread, fromSpread) / 100)
```

> ⚠️ In the PDF the formula line is truncated to `MAX(toSpread, fromSpread)) / 100)`.
> The form above is **reconstructed from the worked example** (below) and required confirmation — see question Q1.

### 4.2 Worked example
| | EUR | PLN |
|--|-----|-----|
| Rate to USD | 0.8 | 3.7 |
| Spread | 1% | 4% |

Expected response from Appendix A: `EUR → PLN = 4.4405487565413254`.

Check: `(3.7 / 0.8) × (1 − max(1%, 4%)) = 4.625 × 0.96 = 4.44` — consistent
(the values 0.8 and 3.7 in the table are rounded for display, hence the small discrepancy with the full number).

### 4.3 Spread reference table (Appendix B)
| Currency group | Spread % |
|----------------|----------|
| Base currency (as returned by your Fixer.io key) | 0.00% |
| JPY, HKD, KRW | 3.25% |
| MYR, INR, MXN | 4.50% |
| RUB, CNY, ZAR | 6.00% |
| All other currencies | 2.75% |

**Precision note:** the use of `BigDecimal` (not `double`) and correct numerical precision is evaluated.

## 5. AI trend insight

### 5.1 What we build
- When the user views the "Historical rates & trend chart" tab (3.2), a **short textual insight** about the rate trend over the selected period is generated and shown.
- The insight is produced by an LLM via **Spring AI** (or LangChain4j), with the **historical rate data for the period passed into the prompt as context**.
- The text must be concise and readable; financial accuracy is not required. The key point is that the LLM actually analyses the provided numbers rather than producing a generic answer.

### 5.2 Technical expectations
- Use the Spring AI chat client abstraction (or equivalent) with a local open-source model (Ollama is the simplest) or any OpenAI-compatible endpoint. The model is my choice.
- Key points:
  - the period's data is **injected into the prompt** (the model reads real numbers rather than guessing);
  - the **system prompt** is designed to constrain the output to a concise, relevant insight (prompt quality is evaluated);
  - the backend exposes an endpoint the frontend calls to obtain the insight;
  - the frontend displays the insight cleanly, with a loading indicator;
  - the model setup is documented in the README so a reviewer can run it locally without guessing.

### 5.3 What is NOT expected
- No financial accuracy, fine-tuned models, or production-grade RAG.
- What is expected: correct Spring AI wiring, a well-thought-out prompt, and a **genuinely working end-to-end** feature. A well-integrated simple solution is valued over an over-engineered but non-working one.

## 6. AI-augmented development (REQUIRED, 25% weight)

Using AI tools is a mandatory requirement of the role. The **quality of usage** is evaluated, not the mere fact: AI as "smart autocomplete" vs AI as a full workflow-automation layer.

### 6.1 What must be in the repository (Required Evidence)
1. **`PLAN.md`** (or equivalent) — a planning artefact created with AI **before** implementation began.
2. **AI tool configuration files** committed: `CLAUDE.md`, `.cursor/rules`, Copilot workspace config, custom slash commands, etc. An empty/missing config signals shallow usage.
3. **A README "AI Workflow" section**: which tool I used, how I configured it, and **at least one example** where the agent produced something I disagreed with — and what I did about it.
4. **A commit history** where AI-assisted phases are visible — use a single prefix, e.g. `[AI]`, so the reviewer can trace the contribution without reading every diff.

### 6.2 What will be evaluated
The repository is viewed as a window into my everyday working style: AI as a workflow tool or as a crutch? Are there traces of genuine agentic use (multi-step, context-aware, iterative) rather than one-off generation? Did I override the AI and can I explain why.

## 7. Submission requirements

- A GitHub repository with a meaningful commit history.
- **README** covering: local setup and run, architecture overview, an "AI Workflow" section (see 6.1), assumptions made, and known trade-offs.
- **A 3–5 minute screen recording**: the working application + at least one AI-agent session (live or voice-over walkthrough). The process matters as much as the product.

## 8. Scoring rubric (where the points are)

| Area | Weight | Breakdown |
|------|--------|-----------|
| **BACKEND** | **25%** | Core API correctness (Spring MVC/REST) 8%; Data persistence & scheduler 6%; Concurrency & thread safety 5%; Code quality (BigDecimal, layer separation, named queries) 6% |
| **FRONTEND** | **20%** | Calculator view 6%; Historical rates & trend chart 8%; Analytics dashboard 6% |
| **AI TREND INSIGHT** | **20%** | Spring AI wiring 8%; Prompt design 7%; Frontend integration 5% |
| **AI-AUGMENTED WORKFLOW** | **25%** | Planning artefact 5%; Tool configuration 8%; Agentic workflow evidence 7%; Critical AI use 5% |
| **OVERALL ENGINEERING** | **10%** | API documentation (Swagger) 3%; Testing (JUnit/Mockito/Angular, formula coverage + ≥1 integration test) 4%; README & docs quality 3% |
| **TOTAL** | **100%** | |

**Interview threshold: 60%.**
Notes:
- A strong AI workflow can compensate for incomplete features.
- A candidate who completes the entire backend+frontend but leaves no meaningful AI-workflow evidence **cannot exceed 75%**.
- Test suites are **expected to be AI-generated**.

## 9. API response formats (Appendix A)

`GET /exchange`
```json
{
  "from": "EUR",
  "to": "PLN",
  "exchange": 4.4405487565413254,
  "date": "2024-03-15",
  "fromQueryCount": 142,
  "toQueryCount": 37
}
```

`GET /analytics` (shape suggested, design is mine)
```json
{
  "topCurrencies": [
    { "currency": "EUR", "totalCount": 142, "lastQueried": "2024-03-15" },
    { "currency": "USD", "totalCount": 98,  "lastQueried": "2024-03-14" }
  ]
}
```

`GET /exchange/insight` (shape suggested, design is mine)
```json
{
  "from": "EUR",
  "to": "GBP",
  "fromDate": "2024-02-01",
  "toDate": "2024-03-01",
  "insight": "EUR/GBP softened by approximately 1.8% over this period, with the steepest decline in the final week of February."
}
```

## 10. Final "Definition of Done" checklist

- [ ] Backend: scheduler at 12:05 AM GMT, upsert, date from the API, multi-instance-safe.
- [ ] Backend: `GET /exchange` with the spread formula on `BigDecimal`, optional date, 404 when missing, thread-safe counters.
- [ ] Backend: `GET /analytics`.
- [ ] Backend (optional): manual refresh without changing counters.
- [ ] Frontend: 3 tabs (calculator, history+chart+insight, analytics), env-configured backend URL.
- [ ] AI: Spring AI + local model, real data in the prompt, well-designed system prompt, endpoint + display with loading.
- [ ] Swagger/OpenAPI works, all endpoints documented.
- [ ] Tests: spread formula + ≥1 integration test (preferably AI-generated).
- [ ] `PLAN.md`, AI tool config files, README with an "AI Workflow" section, commits prefixed `[AI]`.
- [ ] README: setup/run, architecture, assumptions, trade-offs.
- [ ] 3–5 min screen recording.

---

## 11. Open questions (what I was missing)

> **Status:** Q1–Q5 confirmed by the customer (2026-07-15). Below is the original question + the accepted decision.

**On the formula and calculation:**
- **Q1.** The formula line in the PDF is truncated. Please confirm the exact form: `(toRate / fromRate) × (1 − MAX(toSpread, fromSpread)/100)` — is the spread **subtracted**? And is it applied once (max of the two) rather than as the sum of both spreads? (The worked example implies exactly the subtraction of the max.)
  - **✅ Resolved:** yes — the spread is **subtracted**, taking the **max** of the two (not the sum). Implemented in `SpreadCalculator`, covered by unit tests.
- **Q2.** Fixer.io free returns rates **with EUR as the base only**. Do I compute the cross rate `from→to` as `toRate/fromRate` (both relative to EUR)? What if a pair is requested where one currency is missing on the required date?
  - **✅ Resolved:** the cross rate is normalised to EUR (`toRatePerEUR / fromRatePerEUR`). If there is no rate for the date — the backend responds with **HTTP 404** (correct REST), and the frontend shows a message that no pair/data was found for the configuration. For a historical range without points — an empty `points: []` list and a UI message.
- **Q3.** Rounding of the final `exchange`: how many digits, which rounding mode? In the example the value is stored at full precision — return it "as is" (no rounding) or to N digits?
  - **✅ Resolved:** return it **as is, at full `BigDecimal` precision** (no forced rounding of the result). Internal divisions use scale 12, `HALF_EVEN`.

**On data and Fixer.io:**
- **Q4.** Is there already a Fixer.io API key, or should I register my own? The free plan has no historical endpoints and no base switching — is it OK that we accumulate history ourselves from launch day (which means little data for the chart)? Or is seeding/mocking of historical data needed for the demo?
  - **✅ Resolved:** we use the **free plan** (own key via `FIXER_API_KEY`); historical data is **mocked for the demo** (deterministic seeder `DemoDataSeeder`, flag `app.seed.enabled`).
- **Q5.** Which set of currencies should be collected — everything Fixer returns, or a fixed list?
  - **✅ Resolved:** only the **main currencies — EUR (base), USD, GBP, AED**. The restriction is configured via `app.fixer.symbols` (symbols request to Fixer); the seeder and the frontend selectors are limited to the same set.

**On AI/LLM:**
- **Q6.** Is there a preference for the tool (Spring AI vs LangChain4j) and for the model/provider (Ollama locally vs an OpenAI-compatible endpoint)? Any constraints (no internet access / no external APIs allowed)?
- **Q7.** Which AI development assistant should be used for evidence — Cursor (the current environment), Claude Code, Copilot? This determines the config files (`.cursor/rules`, etc.).

**On infrastructure and scope:**
- **Q8.** Specific relational DB: is H2 enough (in-memory for easy reviewer startup) or is PostgreSQL/MySQL needed (docker-compose)?
- **Q9.** Is authentication/authorization needed, or is this a purely internal open API?
- **Q10.** Time deadline and expected depth (MVP vs production-grade)? This affects prioritisation against the rubric.
- **Q11.** Monorepo (backend + frontend in one repository) or two separate repositories?
- **Q12.** Is Docker/`docker-compose` needed for a full local run (DB + Ollama + backend + frontend) as part of the "setup instructions work" evaluation?
