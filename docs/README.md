# Forge — Project Docs

Personal engineering companion. Java 21 + Spring Boot 4.0.7 backend, React 19 + TypeScript frontend, PostgreSQL (prod) / H2 (dev). Single user in practice, but multi-user capable by design (everything is `userId`-scoped).

These docs exist to keep future sessions consistent with decisions, constraints, and conventions. **Read `CONVENTIONS.md` before touching code.**

## Index

| File | What it covers |
|---|---|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Backend + frontend layout, key services, scheduler jobs, auth flow |
| [CONVENTIONS.md](CONVENTIONS.md) | Coding rules, build/test/deploy commands, env vars, gotchas |
| [RENDER-BUDGET.md](RENDER-BUDGET.md) | Render free-tier constraints (0.1 vCPU / 512 MB), JVM flags, capacity model |
| [LEDGER.md](LEDGER.md) | Every identified bug + optimization with status (single source of truth) |

## Quick facts

- Backend deploys to Render (`forge-api-a4uy.onrender.com`), frontend to Vercel, DB to Render Postgres. Push to `main` auto-deploys both.
- Flyway is the ONLY schema management — never `ddl-auto=create`.
- JWT in `forge_token` httpOnly cookie + `Authorization` header fallback; refresh in `forge_refresh` cookie.
- API base: `VITE_API_URL` (frontend). Backend listens on 8080; Render passes `PORT`.
- The app sleeps after 15 min idle on free tier; a GitHub Actions keep-warm cron pings it.

See [LEDGER.md](LEDGER.md) for the current work in flight.
