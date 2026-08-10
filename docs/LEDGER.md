# Forge — Bug & Optimization Ledger

Single source of truth. Status values: `planned` → `in progress` → `done` (→ `deferred` if intentionally shelved). Update this file as work lands.

## Bugs

| ID | Area | Description | Status |
|---|---|---|---|
| BUG-1 | Analytics | Consistency insight card is stale within the day. Fixed: always recompute/upsert today's snapshot (TTL-cached). | done |
| BUG-2 | Analytics | Heatmap vs insight card mismatch. Fixed: GET /api/analytics/heatmap merges attempts+revisions+journals; frontend heatmap uses it. | done |
| BUG-3 | Analytics | Misleading copy. Fixed: tooltip + heatmap copy now say practice/revise/journal. | done |
| BUG-4 | Analytics | Day-boundary timezone drift. Fixed: TimezoneUtil shared w/ AnalysisScheduler; analytics/snapshot use user zone. | done |
| BUG-5 | Practice (fixed) | Phantom `0` on practice cards from `{problem.attempts && …}` short-circuiting to literal `0`. Fixed with `attempts != null && attempts > 0`. | done |
| BUG-6 | UI (fixed) | Dashboard KPI tooltip rendered under the sibling card below it. Fixed with z-index/pointer-events on KpiCard tooltip. | done |
| BUG-7 | UI (fixed) | Cmd+K failed on first press because the listener was in a lazy-loaded component. Moved to always-mounted AppLayout. | done |
| BUG-8 | Revision | Revision scheduling was dormant — revived the completion path: SM-2 fields applied, today/pending queries timezone-aware, eager topic fetch. | done |
| BUG-9 | Recommendation | Stale suggestions/cold-start topics were not cleaned up correctly — fixed cleanup ordering (stale suggestions, then topics). | done |
| BUG-10 | Scheduler | Schedulers shared mutable state across runs — isolated per-user processing + serial execution. | done |
| BUG-11 | Recommendation | Daily generation quota was not atomic (concurrent runs could exceed the limit) — atomic check-and-increment. | done |
| BUG-12 | Auth | Cookie-based JWT → **bearer-only** header auth; refresh tokens now SHA-256 hashed in `refresh_tokens`, rotated on refresh, revoked on login/logout. | done |
| BUG-13 | Security | `/api/auth/register` was reachable in prod → gated to the `dev` profile; `/api/internal/**` now requires `ROLE_ADMIN`. | done |
| BUG-14 | Security | Rate limiter trusted a client-supplied `X-Forwarded-For` IP for bucket keying. Now keys on `request.getRemoteAddr()` resolved by the framework's ForwardedHeaderFilter (prod `server.forward-headers-strategy=framework`), with a capped key. | done |
| BUG-15 | Security | Fail-open defaults: missing/misspelled profile silently ran dev (known creds + dev secret), and the dev seed force-reset and logged the password. Now: no default profile; register/swagger/h2/actuator permitted only in dev; `ProdSecurityGuard` fails startup on missing/short/known-dev `jwt.secret`. | done |
| BUG-16 | Scheduler | Analysis window was fully-inclusive so a 15-min offset preference could fire in two adjacent runs and drop on the boundary. Fixed with a half-open 30-min window (`isInWindow`). | done |
| BUG-17 | LeetCode | Sync could wipe existing tag stats / topics when the API returned a partial response (missing or empty tags). Fail-closed guards abort sync before delete when data is missing while existing data is present. | done |
| BUG-18 | Recommendation | `syncRecProblemsToSuggestions` built the "already suggested" set from the pre-delete list, so regenerating the queue never re-added any previously-saved RECOMMENDATION problem — the queue starved after every regeneration. Existing slugs are now queried after the delete. | done |
| BUG-19 | Intelligence | `skillFromTopics` divided by `max(1, totalAttempts)` and weighted a fresh user's stored ratings into a non-zero average despite zero attempts. Now returns `INITIAL_RATING` when there are no attempts. | done |
| BUG-20 | Frontend | Cold-start retry (502/503/504) re-executed non-idempotent POSTs (submit attempt, journal, LeetCode sync, generate), risking double submission. Automatic retries now apply only to idempotent methods. | done |
| BUG-21 | Frontend | A transient refresh failure (5xx / network / timeout) or a slow refresh call cleared an otherwise valid session. Refresh now has a 10s timeout and only a definitive 401 invalidates the session. | done |
| BUG-22 | Analytics | Revision metrics (weekly, heatmap, activity days) were counted by `scheduledDate` instead of actual `completionDate`, miscounting days when revisions completed late. Now counted by `completionDate` in user zone. | done |
| BUG-23 | Timezone | Business timestamps mixed server clock and user zone across practice, revisions, analytics, memory, recommendations, cold-start, mastery, and forgetting-curve. Centralized via `TimezoneUtil` (`now`/`today`/`dayStart`/`dayEnd`). | done |
| BUG-24 | Revision | `nextRevision` was TIMESTAMP written in the user's zone but compared against server/DB clocks, so overdue detection drifted across zones. Now day-granular `DATE` (V27); due queries take an explicit user-zone `today`; the scheduler resolves the cutoff per user. | done |
| BUG-25 | Auth | Refresh validated only the JWT, not the stored row. Lookup now also enforces `revoked = false AND expires_at > now` (defense-in-depth matching schema intent). | done |
| BUG-26 | Practice | `GET /api/practice/attempts` serialized the entity directly, leaking internal calibration fields `signals_json`/`predicted_score`. Now `@JsonIgnore`'d. | done |

## Optimizations

| ID | Area | Description | Status |
|---|---|---|---|
| PERF-1 | Backend JVM | render.yaml: SerialGC, -Xms64m, metaspace + compressed-class caps, -XX:+ExitOnOutOfMemoryError. | done |
| PERF-2 | Backend config | prod.properties: Tomcat caps, Hikari 3/1/5s, gzip on, sql comments off. | done |
| PERF-3 | Backend queries | Kill bulk `findByUserIdAll` in analytics: weekly count query, first-Hard repo query, accuracy count queries. | done |
| PERF-4 | Backend | ProblemLoader precomputed slug→tag map; used by syncRecProblemsToSuggestions (no nested loop per generation). | done |
| PERF-5 | Backend | Short-TTL (120 s) in-memory cache dedupes today snapshot across analytics endpoints. | done |
| PERF-6 | Frontend | Analytics no longer pulls journalsApi.getAll(0,500); uses lightweight heatmap endpoint. | done |
| PERF-7 | Frontend | React Query gcTime: Infinity → 30 min. | done |
| PERF-8 | Frontend | index.html preconnect + dns-prefetch to Render API. | done |
| PERF-9 | Frontend | vite build.target esnext. | done |
| PERF-10 | Backend | `refreshUserRetentions`: skip `saveAll` when retention unchanged (reduced nightly write load). | deferred |
| PERF-11 | Docker | Slimmer JRE base image (alpine/distroless) for faster pull/cold start. | deferred |
| PERF-12 | Backend | AppCDS (class data sharing) for faster boot. | deferred |
| PERF-13 | Frontend | Replace recharts (417 KB min chunk) with hand-rolled SVG. **Decision: keep recharts** (lazy-loaded to Analytics only). | deferred |
| PERF-14 | Backend | Per-tag/problem scoring memoized per context — killed the tags×candidates breakdown storm in `RecommendationEngine`. | done |
| PERF-15 | Backend | Streak computed from a single bounded query (no per-day round trips); V22 hot-path indexes; JDBC batching (`order_inserts`/`order_updates`); `readOnly` tx on read APIs. | done |
| PERF-16 | Backend | V26 `idx_attempts_predicted_scan` composite index (`predicted_score`, `signals_json`, `attempted_at DESC`) for the calibration/engine-report snapshot scan. | done |
| PERF-17 | Backend | Bulk revision dedup: scheduler's per-topic `existsByTopicIdAndCompletedFalse` replaced with one `findTopicIdsWithPendingRevision()` set lookup. | done |
| PERF-18 | Backend | `findByUserId` returns `List<Topic>` (was `Page` — no count query); List-style topic queries across cold-start, knowledge-graph, forgetting-curve. | done |

## Decision log

| Date | Decision |
|---|---|
| — | All shadows removed except `--shadow-glow` / landing pulse. |
| — | Recommendations not auto-generated on dashboard load; only on explicit sync / manual generate / scheduled window. |
| — | Delete-after-fetch in LeetCode sync (delete only after successful fetch+save). |
| — | Readiness logic shared via `ReadinessCalculator` (Dashboard, Analytics, RecommendationEngine). |
| — | Consistency definition: ONLY `computeConsistency` (14 days, attempts+revisions+journals). |
| — | Keep recharts on Analytics; accept the 417 KB lazy chunk (PERF-13 deferred). |
| 2026-08 | **Bearer-only auth**: JWT in `Authorization: Bearer` only (no cookies). Access + refresh tokens in sessionStorage on the frontend. Refresh tokens server-side revocable (SHA-256 hash), rotated on refresh, revoked on login/logout. |
| 2026-08 | `/api/auth/register` is **dev-profile only** (`RegistrationController @Profile("dev")`); prod is single-user. |
| 2026-08 | `/api/internal/**` requires **ROLE_ADMIN** (sole user is ADMIN); explicit 401/403 entry points. |
| 2026-08 | Dead code removed: `TopicController` CRUD endpoints, `GET /api/journals/today`, `GET /api/journals/recent`, `GET /api/recommendations`, unused repository methods. |
| 2026-08 | `topics.next_revision` is a day-granular `DATE` (V27); "due" queries compare against the user's `today` (explicit param), never the server clock. |
| 2026-08 | All business timestamps read/written in the user's zone via `TimezoneUtil` (`now`/`today`/`dayStart`/`dayEnd`); day-boundary logic is deployment-zone independent. |
| 2026-08 | `GET /api/health` exposes only `status`/`db`/`timestamp` — no scheduler details. |
| 2026-08 | Test suite grown from ~20 → **128 tests** (auth + revocation, security gating, revision/SM-2, analytics streak, journals, timezone-safe milestone logic, etc.). |
