# Forge — Your Personal Engineering Companion

## Overview

Forge is a personalized engineering companion that learns alongside you. It answers one question every morning:

> "What should I do today to become a better engineer?"

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 4.0.7, Spring Security 7, Spring Data JPA |
| Frontend | React 19, TypeScript, Vite, TailwindCSS, shadcn/ui |
| Database | PostgreSQL (prod), H2 (dev) |
| Auth | Bearer JWT (jjwt 0.12.6) + server-side refresh token rotation/revocation |
| Deployment | Backend: Render, Frontend: Vercel |

## Architecture

Layered monolith. Packages by feature:
- `auth` — Authentication & user management
- `topic` — Learning topics with mastery tracking
- `problem` — Solved problems with difficulty tracking
- `revision` — Spaced repetition scheduling
- `recommendation` — Deterministic rule-based engine
- `journal` — Daily engineering journal
- `analytics` — Learning analytics & charts
- `dashboard` — Aggregated dashboard view
- `scheduler` — Background jobs (morning, evening, weekly)

## Quick Start

### Backend
```bash
./mvnw spring-boot:run
```
API: http://localhost:8080
Swagger: http://localhost:8080/swagger-ui.html
H2 Console: http://localhost:8080/h2-console

### Frontend
```bash
cd frontend
npm install
npm run dev
```
App: http://localhost:5173

### Default Login
- Username: `forge`
- Password: `forge123`

## Environment Variables

### Backend
| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Profile (dev/prod) | dev |
| `DATABASE_URL` | PostgreSQL JDBC URL | — |
| `DATABASE_USERNAME` | DB username | — |
| `DATABASE_PASSWORD` | DB password | — |
| `JWT_SECRET` | Base64 encoded JWT secret | (dev default) |

### Frontend
| Variable | Description | Default |
|----------|-------------|---------|
| `VITE_API_URL` | Backend API URL | /api |

## Database

Uses Flyway for migrations (`src/main/resources/db/migration/`, V1–V27). Notable migrations:
- `V1__init.sql` — schema; `V2__seed_default_user.sql` — default user (forge/forge123)
- `V21__email_unique_and_optimistic_lock.sql` — unique email + optimistic locking
- `V22__query_indexes.sql` — hot-path indexes (attempts, revisions, recommendations, topics)
- `V23__refresh_token_revocation.sql` — hashed server-side refresh tokens
- `V24__user_role.sql` — user roles (`ROLE_ADMIN` / `ROLE_USER`)
- `V25__attempt_scan_index.sql` / `V26__predicted_attempt_scan_index.sql` — attempt time-series scan indexes
- `V27__next_revision_date_only.sql` — `topics.next_revision` narrowed from TIMESTAMP to DATE (SM-2 intervals are day-granular)

## Timezone Conventions

Business timestamps are written and read in the **user's timezone** (stored on the user profile),
never the server clock, so day-boundary logic stays self-consistent in any deployment zone:
- `TimezoneUtil.now(user)` / `today(user)` / `dayStart` / `dayEnd` resolve the user's `ZoneId`
- `topics.next_revision` is a `DATE`; "due" queries compare against the user's `today()`
- `revisions.scheduled_date` and `daily_metrics` are day-granular `DATE` columns
- `topics.last_revision`, `last_attempt_at`, and `problem_attempts.attempted_at` remain TIMESTAMP (instant-ish)
- Absolute durations (JWT access/refresh expiry, SM-2 intervals) are unaffected by zone

## Testing

```bash
# Backend — 128 tests (auth + refresh revocation, security gating, revision/SM-2, analytics streak, journals, ...)
./mvnw test

# Frontend — typecheck + production build, then lint
cd frontend
npx tsc -b
npm run build
npm run lint
```

## Deployment

### Backend (Render)
1. Connect GitHub repo
2. Render auto-detects `render.yaml`
3. Set environment variables
4. Deploy

### Frontend (Vercel)
1. Connect GitHub repo
2. Set root directory to `frontend`
3. Build command: `npm run build`
4. Output: `dist`
5. Set `VITE_API_URL` environment variable

## API Documentation

Once running, visit `/swagger-ui.html` for interactive API docs.

## License

Personal use only.
