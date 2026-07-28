# Forge Session State — Jul 28, 2026

## Current Status
- **GitHub repo:** https://github.com/divyeshpanchasarawork-creator/Forge.git (4 commits on `main`)
- **Render:** Blueprint deployed, auto-redeploying after latest push (`b9240d1` — flyway fix). Waiting for **Live** status.
- **Vercel:** Not started yet.
- **Tests:** `mvn test` passes, `npm run build` passes.

## What Was Done This Session
1. **Flame favicon** — replaced purple "F" with flame SVG, added `apple-touch-icon`
2. **Login page redesign** — split layout (branding left, auth card right), features section, footer
3. **Auth card fix** — fixed `min-height` to prevent card resizing on sign-in/register toggle
4. **Registration errors** — frontend now parses backend messages for "username taken" / "email in use"
5. **CORS** — env-driven via `cors.allowed-origins` property (defaults to `https://forge-psi.vercel.app`)
6. **Swagger disabled in prod** via `application-prod.properties`
7. **Health endpoint** — `/api/health` (public, no auth required)
8. **Security config** — added `/api/health` to `permitAll()`
9. **Migrations consolidated** — V1 (complete schema) + V2 (dev seed user). Deleted V3/V4/V5.
10. **render.yaml fixed** — `databases:` top-level key, env vars: `DB_HOST`/`DB_PORT`/`DB_NAME`/`DATABASE_USERNAME`/`DATABASE_PASSWORD`
11. **Dockerfile** — multi-stage build (Maven build + JRE runtime), uses `${JAVA_OPTS}` env var
12. **Dead code removed** — `App.css`, empty `assets/` dir
13. **Flyway fix** — added `flyway-database-postgresql` dependency (Render uses PostgreSQL 18.4)

## What's Next
1. **Check Render deploy** — go to Render dashboard → forge-api → check if it's **Live**
2. **Get the backend URL** — something like `https://forge-api-xxxx.onrender.com`
3. **Deploy frontend to Vercel:**
   - New Project → Import GitHub repo
   - Root Directory: `frontend/`
   - Framework: Vite (auto-detected)
   - Env var: `VITE_API_URL` = `https://forge-api-xxxx.onrender.com/api`
4. **Update CORS** — set `cors.allowed-origins` env var on Render to the actual Vercel URL (e.g., `https://forge-xxxx.vercel.app`)
5. **Verify** — register a user, login, test LeetCode sync

## Key Files
- `render.yaml` — Render Blueprint config (Docker-based)
- `Dockerfile` — multi-stage Java 21 build
- `.dockerignore` — excludes target/, node_modules/, .git
- `src/main/resources/application-prod.properties` — prod config (DB URL from `DB_HOST`/`DB_PORT`/`DB_NAME`)
- `src/main/resources/db/migration/V1__init.sql` — complete schema
- `src/main/resources/db/migration/V2__seed_default_user.sql` — dev seed (username: `forge`, password: `forge123`)
- `frontend/src/pages/LoginPage.tsx` — landing page + auth
- `frontend/src/api/index.ts` — all API calls including auth

## Architecture Notes
- Backend: Spring Boot 4.0.7, Java 21, Spring Security 7, PostgreSQL (prod) / H2 (dev)
- Frontend: React 19, TypeScript, Vite, TanStack Query
- Auth: JWT (jjwt 0.12.6), BCrypt passwords
- LeetCode: GraphQL client with 5-min cache, topic auto-mapper
- Deployment: Render (backend + PostgreSQL), Vercel (frontend SPA)
