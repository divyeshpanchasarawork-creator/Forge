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
| 2026-08 | Test suite grown from ~20 → **115 tests** (auth + revocation, security gating, revision/SM-2, analytics streak, journals, etc.). |
