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

## Decision log

| Date | Decision |
|---|---|
| — | All shadows removed except `--shadow-glow` / landing pulse. |
| — | Recommendations not auto-generated on dashboard load; only on explicit sync / manual generate / scheduled window. |
| — | Delete-after-fetch in LeetCode sync (delete only after successful fetch+save). |
| — | Readiness logic shared via `ReadinessCalculator` (Dashboard, Analytics, RecommendationEngine). |
| — | Consistency definition: ONLY `computeConsistency` (14 days, attempts+revisions+journals). |
| — | Keep recharts on Analytics; accept the 417 KB lazy chunk (PERF-13 deferred). |
