# Forge — Build Instructions

## Project
Personal engineering companion. Single user. Java 21 + Spring Boot 4.0.7 backend, React 19 + TypeScript frontend.

## Architecture
Layered monolith. Packages by feature (auth, topic, problem, revision, recommendation, calibration, journal, analytics, dashboard, scheduler, common, config, security).

## Conventions
- Constructor injection only (`@RequiredArgsConstructor`)
- No field injection (`@Autowired` on fields)
- No business logic in controllers
- DTO pattern for all request/response
- Repository pattern
- Global exception handling via `@RestControllerAdvice`
- UUID primary keys on all entities
- Audit fields: createdAt, updatedAt
- Flyway for all migrations, never ddl-auto=create
- Spring Security 7 lambda DSL (no `.and()` chaining)
- Boot 4 wires Jackson 3 (`tools.jackson`); there is NO auto-configured `com.fasterxml.jackson.databind.ObjectMapper` bean — construct `new ObjectMapper()` directly (ProblemLoader pattern)
- Bearer-only auth: JWT access token in `Authorization: Bearer` header only (no cookies). Frontend keeps access + refresh tokens in sessionStorage
- Refresh tokens are server-side revocable: hashed (SHA-256) in `refresh_tokens`; login/logout revoke prior tokens, refresh rotates the pair; the DB lookup also requires `revoked = false AND expires_at > now`
- `POST /api/auth/register` exists only in the `dev` profile (`RegistrationController`); prod is single-user
- `/api/internal/**` requires `ROLE_ADMIN`; the sole user is ADMIN. Unauthenticated → 401, authenticated-but-forbidden → 403 (explicit entry points)
- Rate limiting on `/api/auth/**` (5 req/min/IP)
- SM-2 spaced repetition fields on Topic: `easinessFactor`, `repetitionInterval`, `lastQuality`; `nextRevision` is a day-granular `LocalDate`/`DATE` (interval in days) and "due" queries take an explicit `today` param
- All business timestamps read/written in the user's timezone via `TimezoneUtil` (`resolve(user)`, `now`, `today`, `dayStart`, `dayEnd`) — never the server clock, so day-boundary logic is deployment-zone independent
- `GET /api/health` exposes only `status`/`db`/`timestamp` — no scheduler details
- JOIN FETCH on all revision repository queries for topic
- `@EntityGraph(attributePaths = {"topics"})` on problem repository queries
- KpiCard component for all metric displays with tooltip prop
- parseApiError utility for consistent API error messages
- LoadingSkeleton component for loading states

## Testing
Run: `mvn test`

## Design Decisions & Refactors
- Delete-after-fetch in LeetCode sync: delete runs after successful fetch+save to avoid wiping topics on API failure
- Recommendations no longer auto-generated on dashboard load; only on explicit sync or manual generate
- Readiness score logic extracted into shared `ReadinessCalculator` utility (used by `DashboardService`, `AnalyticsService`, `RecommendationEngine`)
- `RewardModel` utility derives per-problem/per-tag reward from stored attempt quality (reward = quality/5); `ScoringContext.rewards` carries `RewardStats` for reward-aware UCB
- Memory page surfaces fading concepts, patterns, mistakes, and insights from journal entries
- KpiCard component simplified (no trend/trendValue props)
- Scoring self-containment: the 13 signal weights live in `SignalWeights` (order = `SIGNAL_NAMES`, same as the breakdown emits); `ScoringContext.weights` carries them per request; calibration is a single global `scorer_weights` row (single-user app, no per-user scope)
- `ScorerWeightsService` caches the active weight vector; `CalibrationJob` (nightly) least-squares fits weights against stored attempt snapshots and swaps only when MSE improves by >= max(1.0, 5%) on >= 30 samples
- `RecEngineEvaluator` is the shared pure-metric utility (MSE / binary log-loss / rank-AUC, reward = quality/5) used by calibration and the engine report
- Attempt snapshots for calibration: `PracticeService.submitAttempt` stores `signals_json` (the `ScoreItem` list) + `predicted_score` (breakdown total) before mastery updates
- `RecommendationResponse.score` mirrors `scoreBreakdown.total`; recommendation lists sort by score desc, then priority asc, then createdAt desc
- `SessionPlanner` uses marginal-gain selection: repeatedly pick the highest-score unused candidate that fits a remaining segment slot (REVISION > WARMUP > CHALLENGE > REINFORCE) instead of fixed sequential passes
- `RevisionScheduler` materializes due revisions per user via `findTopicsNeedingRevisionByUserId(userId, TimezoneUtil.today(user))` — the day-granular `next_revision` cutoff is resolved in each user's zone, never the server clock
- Engine health is surfaced via `GET /api/internal/engine-report` (stored-vs-live MSE/log-loss/rank-AUC over snapshots) and re-fit on demand via `POST /api/internal/calibration/run`; ProfilePage renders a KpiCard health card. Never run calibration automatically on request paths

## Key Dependencies
- Spring Boot 4.0.7, Spring Security 7, Spring Data JPA
- PostgreSQL (prod), H2 file (dev, `jdbc:h2:file:./data/forge` — persists across restarts)
- Flyway, Lombok, Springdoc OpenAPI 3.0.2
- jjwt 0.12.6 for JWT

## Deployment
- Frontend: Vercel
- Backend: Render
- Database: PostgreSQL on Render

## Environment Variables
- `jwt.secret` (required) — Base64-encoded HMAC key for JWT signing
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` — PostgreSQL connection
- `cors.allowed-origins` — comma-separated CORS origins
- `VITE_API_URL` — frontend API base URL
