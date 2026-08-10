# Session State — Phase 2 Complete

> Note: Sprint 1's cookie-based auth description below is **superseded** — see Sprint 8 (bearer-only JWT + server-side revocable refresh tokens).

## Completed
- **Sprint 1**: Security + Cold Start
  - JWT secret defaults removed (fails fast if not set)
  - Password validation: 8+ chars with uppercase, lowercase, digit, special
  - Rate limiting: 5 req/min/IP on `/api/auth/**`
  - httpOnly cookies for JWT (`forge_token`) + refresh token (`forge_refresh`)
  - `/api/auth/refresh` endpoint
  - `/api/auth/logout` endpoint (clears cookies)
  - JwtAuthenticationFilter reads from cookie then Authorization header
  - Frontend: sessionStorage + refresh token fallback; 3x retry with backoff on 502/503/504
  - `spring.main.lazy-initialization=true` in prod profile

- **Sprint 2**: N+1 Queries + Indexes
  - Revision repository: all queries use `JOIN FETCH r.topic`
  - Problem repository: `@EntityGraph(attributePaths = {"topics"})` on finder methods
  - Revision `findByIdWithTopic()` for the completion flow
  - V3 migration: `idx_revisions_user_id`, `idx_recommendations_user_id`

- **Sprint 3**: SM-2 Scheduling
  - `easinessFactor`, `repetitionInterval`, `lastQuality` fields on Topic entity
  - V4 migration for SM-2 columns
  - `SpacedRepetitionService` implements SM-2 algorithm
  - `completeRevision` accepts quality param (0-5), uses SM-2 for interval/boost
  - Mastery boost formula: `min(10, 3 + quality*1.5 - revisionCount*0.3)`

- **Sprint 4**: AI Review Engine
  - Enhanced `RecommendationEngine` with priority scoring + milestone recommendations
  - Next milestone: rounds to nearest 50, 25-Medium thresholds
  - Frontend Roadmap page with priority badges (High/Medium/Low), dismiss button
  - Roadmap link in sidebar + route in App.tsx

- **Sprint 5**: KPI Cards
  - `KpiCard` component with icon, value, label, tooltip, trend support
  - Dashboard uses KpiCards with tooltip explanations

- **Sprint 6**: Code Consolidation
  - `LoadingSkeleton` component for loading states
  - `parseApiError` utility for consistent error messages
  - LoginPage updated to use `parseApiError`

- **Sprint 7**: Documentation
  - AGENTS.md updated with all new conventions
  - SESSION_STATE.md created (this file)

- **Sprint 8**: Backend Audit Remediation (9 chunks, all committed)
  - Chunk 1 (`1a200aa`): Revived revision scheduling; fixed knowledge-graph concept matching + diversity signal
  - Chunk 2 (`2b95660`): Fixed stale suggestion/topic cleanup, scheduler isolation, atomic daily generation quota
  - Chunk 3 (`429ce41`): Hardened edge cases + timezone handling; V21 (unique email, optimistic lock)
  - Chunk 4 (`d8e89d9`): Memoized per-tag scoring per context (killed the breakdown storm)
  - Chunk 5 (`531ab37`): Single-query streak, V22 hot-path indexes, JDBC batching, readOnly tx on read APIs
  - Chunk 6 (`542cc92`): **Bearer-only auth** (no cookies). Access + refresh tokens in sessionStorage; refresh tokens SHA-256 hashed in `refresh_tokens` (V23), rotated on refresh, revoked on login/logout
  - Chunk 7 (`b6de756`): `/api/auth/register` gated to `dev` profile; `/api/internal/**` requires `ROLE_ADMIN` (V24 role column); explicit 401/403 entry points
  - Chunk 8 (`5501cd6`): Removed dead code — `TopicController` CRUD, `GET /api/journals/today`, `GET /api/journals/recent`, `GET /api/recommendations`, unused repo methods + frontend exports
  - Chunk 9 (`5684b72`): Added unit tests for revision completion path + SM-2 algorithm
  - Final suite: **128 tests green** (`./mvnw test`)

## Deployment Notes
- Backend: Render uses Dockerfile multi-stage build
- Frontend: Vercel, `VITE_API_URL` must be set to `https://forge-api-a4uy.onrender.com/api`
- `jwt.secret` env var must be set on Render (no default)
- Cold start: ~30s on Render free tier; frontend retries 3x with 5s/10s/15s backoff
