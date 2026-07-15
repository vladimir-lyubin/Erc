# PLAN — Exchange Rate Management System

> Планирующий артефакт, подготовленный с помощью AI-агента (Cursor) **до** начала реализации.
> Основан на `Marcura_Assessment_FullStack.pdf` и разборе в `REQUIREMENTS.md`.
> Живой документ: обновляется по мере продвижения и уточнения ответов на открытые вопросы.

## 1. Цель и принципы

Построить end-to-end систему курсов валют: backend (Spring Boot) + scheduler + Angular SPA + AI-инсайт по тренду.
Ключевые принципы, вытекающие из рубрики:

- **Работающий end-to-end важнее переусложнения.** Простое, но цельное решение оценивается выше.
- **AI-workflow = 25%** (наравне с backend). Планирование, конфиги, история коммитов `[AI]`, критическое использование — обязательны.
- **Числовая корректность.** Все денежные расчёты — на `BigDecimal`, без `double`.
- **Читаемая история коммитов** маленькими логическими шагами, AI-фазы с префиксом `[AI]`.

## 2. Принятые допущения (defaults вместо ожидания ответов)

Пока нет ответов на вопросы из `REQUIREMENTS.md` §11, работаем по этим defaults и фиксируем их в README «Assumptions»:

| # | Вопрос | Принятое решение |
|---|--------|------------------|
| Q1 | Формула | `(toRate / fromRate) × (1 − MAX(toSpread, fromSpread)/100)`, спред вычитается, берётся max из двух. Реконструировано из worked example. |
| Q2 | Кросс-курс | Fixer free отдаёт базу EUR. `from→to = toRatePerEUR / fromRatePerEUR`. Если валюты нет на дату — 404. |
| Q3 | Округление | Хранить и считать с полной точностью `BigDecimal`; в API отдавать без принудительного округления (как в примере). Внутренние деления — scale 12, `HALF_EVEN`. |
| Q4 | Fixer.io | Свой free-ключ через env `FIXER_API_KEY`. История копится со дня запуска; для демо — сидер, заполняющий N последних дней (флаг `app.seed.enabled`). |
| Q5 | Набор валют | Сохраняем все валюты, что вернул Fixer (symbols не ограничиваем). |
| Q6 | AI | Spring AI + Ollama (локальная модель, напр. `llama3.1` / `qwen2.5`). Провайдер конфигурируем через env. |
| Q7 | AI-ассистент | Cursor (текущая среда) → конфиг в `.cursor/rules`. |
| Q8 | БД | H2 (file/mem) по умолчанию — запуск ревьюером в один клик; профиль `postgres` + docker-compose как альтернатива. |
| Q9 | Auth | Нет (внутренний открытый API), CORS открыт для dev. |
| Q11 | Репозиторий | Монорепо: `backend/` + `frontend/`. |
| Q12 | Docker | `docker-compose.yml` для полного локального запуска (Postgres + Ollama), но приложение запускается и без Docker (H2 + внешняя Ollama). |

> Эти defaults не блокируют старт; при получении ответов корректируем.

## 3. Архитектура (высокоуровнево)

```
Fixer.io ──(daily 12:05 GMT)──► Scheduler ──► RateService ──► [ DB: exchange_rate ]
                                                    ▲                    │
Angular SPA ──HTTP──► REST Controllers ──► Services ┘                    │
   3 views            /exchange /analytics /exchange/insight             │
                                     │                                    │
                                     └──► CurrencyUsage counters ◄────────┘
                                     └──► Spring AI ChatClient ──► Ollama (LLM)
```

### Backend слои
- `web` — REST-контроллеры + DTO + обработка ошибок (`@RestControllerAdvice`).
- `service` — бизнес-логика: расчёт спреда, сбор данных, аналитика, инсайт.
- `domain` — JPA-сущности (`ExchangeRate`, `CurrencyUsage`).
- `repository` — Spring Data JPA + named queries.
- `client` — интеграция с Fixer.io (`RestClient`/`WebClient`).
- `ai` — обвязка Spring AI (ChatClient, промпты).
- `scheduler` — `@Scheduled` задача + распределённая блокировка.
- `config` — spread-конфиг, CORS, OpenAPI, планировщик.

### Модель данных
- `exchange_rate(id, currency_code, rate, rate_date, base_currency, created_at)`
  - уникальность: `(currency_code, rate_date)` → основа для upsert.
- `currency_usage(currency_code PK, query_count, last_queried_date)`
  - инкремент атомарным UPDATE-запросом (не read-modify-write).

## 4. Расчёт спреда

```
spread(ccy):
  base            -> 0.00%
  JPY|HKD|KRW     -> 3.25%
  MYR|INR|MXN     -> 4.50%
  RUB|CNY|ZAR     -> 6.00%
  else            -> 2.75%

rate(from,to,date) = (toRatePerEUR / fromRatePerEUR) * (1 - MAX(spread(from),spread(to))/100)
```
- Реализация: `SpreadCalculator` (чистый класс, легко тестируется), спреды из `application.yml` → `@ConfigurationProperties`.
- Тесты: worked example EUR→PLN = 4.44…, границы групп спредов, base=0%.

## 5. Конкурентность и multi-instance

- **Счётчики:** атомарный `@Modifying` UPDATE (`UPDATE ... SET count = count + 1`), не загрузка сущности в память. Инкремент обеих валют в одной транзакции.
- **Scheduler в нескольких инстансах:** ShedLock (JdbcTemplate lock provider) — только один инстанс выполняет задачу в окне. Обоснование в README (простота + надёжность vs выбор лидера/внешний оркестратор).
- **Upsert:** уникальный индекс `(currency_code, rate_date)` + обработка конфликта (insert-or-update), идемпотентность повторного запуска за тот же день.

## 6. AI-инсайт (Spring AI)

- `ChatClient` (Spring AI) → Ollama.
- Prompt: system-промпт ограничивает вывод 1–2 предложениями, только по данным; user-промпт содержит **реальные пары (дата, курс)** за период.
- Эндпоинт `GET /exchange/insight?from&to&fromDate&toDate`.
- Кэш инсайта по ключу (from,to,fromDate,toDate) — чтобы не дёргать LLM повторно (опц.).
- Fallback: если LLM недоступен — понятная ошибка/деградация, фронт показывает сообщение.

## 7. Frontend (Angular)

- Standalone-компоненты, роутинг на 3 вкладки, типизированные модели, `HttpClient` + interceptors (loading/error).
- `environment.ts` / `environment.development.ts` → `apiBaseUrl` из env.
- Вкладки:
  1. **Calculator** — reactive form, валидация, loading, обработка 404.
  2. **Historical** — выбор пары + диапазона, таблица + линейный график (ng2-charts/Chart.js), панель AI-инсайта с loading.
  3. **Analytics** — визуализация usage (топ валют, даты).
- Сервисный слой (`ExchangeApiService`) с типами DTO, соответствующими backend.

## 8. Тестирование

- Backend: JUnit5 + Mockito. Обязательно: unit-тесты `SpreadCalculator`; интеграционный тест `/exchange` (MockMvc + H2). Тест атомарности счётчика. Тест upsert-идемпотентности.
- Frontend: базовые component/service specs (Jasmine/Karma).
- Тестовые сюиты генерируем с AI, затем ревьюим/правим (фиксируем пример правки в README «AI Workflow»).

## 9. Документация и сдача

- Swagger UI (`springdoc-openapi`) — все эндпоинты.
- README: setup/run (с Docker и без), архитектура, «AI Workflow», assumptions, trade-offs.
- Запись экрана 3–5 мин: работающее приложение + сессия AI-агента.

## 10. Порядок работ (итерации / коммиты)

1. [x] `[AI]` Планирование: PLAN.md, .cursor/rules, скелет репо, README-скелет.
2. [x] Backend bootstrap: pom, application.yml, main class, H2, Swagger, health (actuator).
3. [x] Домен + репозитории + миграция схемы (уникальный индекс).
4. [x] Fixer client + scheduler + ShedLock + upsert + сидер для демо.
5. [x] `SpreadCalculator` + `/exchange` + счётчики (атомарно) + тесты формулы/интеграции/конкуренции.
6. [x] `/analytics` + тест.
7. [x] Spring AI + Ollama + `/exchange/insight` + prompt design.
8. [x] Frontend bootstrap, env-конфиг, API-сервис, роутинг.
9. [x] Вкладка Calculator.
10. [x] Вкладка Historical + график + панель инсайта.
11. [x] Вкладка Analytics.
12. [~] Polishing: README финал ✔, Swagger ✔, покрытие тестами ✔;
    сборка/тесты backend прогнаны локально (JDK 17 + Maven 3.9.11) — **BUILD SUCCESS, 12/12 тестов зелёные** ✔;
    Maven wrapper (`mvnw`) добавлен ✔;
    frontend собран и протестирован локально (Node 22 + Angular 18) — **build OK, 7/7 specs зелёные** ✔;
    запись экрана — pending (ручной шаг).

Каждая AI-ассистированная фаза → коммит с префиксом `[AI]`.

## 11. Риски / trade-offs

- **Fixer free = только EUR-база и без historical.** → историю копим сами + сидер для демо; в README честно указать ограничение.
- **Ollama требует локальной модели** (вес/время загрузки). → в README точные команды `ollama pull`; провайдер конфигурируем, чтобы можно было переключить на OpenAI-совместимый endpoint.
- **H2 vs Postgres** — H2 упрощает запуск, но ShedLock/upsert проверяем и на Postgres (профиль + docker-compose).
- **Время.** Приоритет по рубрике: backend correctness + AI workflow evidence в первую очередь.

## 12. Открытые вопросы

Список ведётся в `REQUIREMENTS.md` §11 (Q1–Q12). До получения ответов действуют defaults из §2 этого плана.
