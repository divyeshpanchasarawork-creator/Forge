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
| Auth | JWT (jjwt 0.12.6) |
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

Uses Flyway for migrations:
- `V1__init.sql` — Schema (all tables)
- `V2__seed_default_user.sql` — Default user (forge/forge123)
- `V3__sample_data.sql` — Sample data (40 topics, 20 problems, etc.)

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
