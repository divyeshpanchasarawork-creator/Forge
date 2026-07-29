# Forge — Build Instructions

## Project
Personal engineering companion. Single user. Java 21 + Spring Boot 4.0.7 backend, React 19 + TypeScript frontend.

## Architecture
Layered monolith. Packages by feature (auth, topic, problem, revision, recommendation, journal, analytics, dashboard, scheduler, common, config, security).

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
- Jackson 3 (ships with Boot 4)
- JWT in httpOnly cookie (`forge_token`) + Authorization header fallback
- Refresh token in separate httpOnly cookie (`forge_refresh`)
- Rate limiting on `/api/auth/**` (5 req/min/IP)
- SM-2 spaced repetition fields on Topic: `easinessFactor`, `repetitionInterval`, `lastQuality`
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
- Memory page surfaces fading concepts, patterns, mistakes, and insights from journal entries
- KpiCard component simplified (no trend/trendValue props)

## Key Dependencies
- Spring Boot 4.0.7, Spring Security 7, Spring Data JPA
- PostgreSQL (prod), H2 (dev)
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
