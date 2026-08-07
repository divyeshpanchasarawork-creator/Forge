# Forge — Conventions

## Commands

| Task | Command |
|---|---|
| Backend tests | `./mvnw test` (expect 115 tests, `BUILD SUCCESS`) |
| Frontend typecheck + build | `npx tsc -b` then `npm run build` (in `frontend/`) |
| Frontend lint | `npm run lint` (oxlint) |
| Dev backend | `./mvnw spring-boot:run` (port 8080) |
| Dev frontend | `npm run dev` (Vite, proxies `/api` → localhost:8080) |

## Coding rules (backend)

- **Constructor injection only** (`@RequiredArgsConstructor`). No field `@Autowired`.
- **No business logic in controllers** — thin, delegate to services.
- **DTO pattern** for all request/response; `ApiResponse.success(...)` wrapper (`com.forge.common.dto.ApiResponse`).
- **Repository pattern**; global exception handling via `@RestControllerAdvice`.
- **UUID PKs** on all entities; audit `createdAt`/`updatedAt`.
- **Flyway only** for schema (`db/migration/V{n}__*.sql`). Never `ddl-auto=create`. Test it against both H2 (dev) and Postgres (prod).
- **JPA query hygiene**:
  - `JOIN FETCH` on all revision repository queries for `topic`.
  - `@EntityGraph(attributePaths = {"topics"})` on problem repository queries.
  - Prefer projection/count queries over loading full collections (`findByUserIdAll`) — see LEDGER `PERF-2`.
  - `@Transactional(readOnly = true)` on read-only service paths; JDBC batching on (`batch_size=20`, `order_inserts`/`order_updates`).
  - Never iterate lazy collections outside a transaction (OSIV is off).
- **Time**: all "today"/date-window logic must use the **user's timezone** via `com.forge.common.util.TimezoneUtil.resolve(user)` (fallback `UTC`). Server `LocalDate.now()` is forbidden for user-facing day boundaries.
- **Security**: Spring Security 7 lambda DSL. **Bearer-only**: JWT access token in `Authorization: Bearer` header only (no cookies). Refresh tokens are server-side revocable — SHA-256 hashed in `refresh_tokens`; login/logout revoke prior tokens, refresh rotates the pair. Rate limit on `/api/auth/**` (5 req/min/IP). `/api/auth/register` exists only in the `dev` profile. `/api/internal/**` requires `ROLE_ADMIN`; unauthenticated → 401, authenticated-but-forbidden → 403 (explicit entry points).
- **No comments unless asked**; keep code self-explanatory. Match existing style (Lombok, `@Slf4j`, streams).
- Never log secrets (JWT secret, passwords, tokens).

## Frontend conventions

- React 19 + TS strict; function components; `memo` for heavy cards (e.g., `InsightCard`).
- **KpiCard** for all metric displays (with tooltip prop). **LoadingSkeleton** for loading states. **parseApiError** for consistent API error messages.
- Keyboard shortcuts must live in **always-mounted** components (e.g., Cmd+K listener in `AppLayout`), NOT in lazy-loaded pages.
- React Query: use the shared `queryClient` defaults; don't set `gcTime: Infinity`.
- Styling: Tailwind v4, CSS variables (`--color-card`, `--color-primary`, ...). Shadow tokens: only `--shadow-glow` exists — do not reintroduce `--shadow-soft`/`--shadow-2xl`.

## Git / deploy rules

- Only commit/push when explicitly asked.
- Push to `main` auto-deploys: Render (backend) + Vercel (frontend). Verify after deploy via `https://forge-api-a4uy.onrender.com/api/health` and the Vercel bundle-hash check (`dist/index.html` asset name vs served HTML).
- **Keep-warm rule**: before restarting/pushing the backend, wait for any running local `./mvnw spring-boot:run` processes (8080/8081) to exit so Flyway's `success` flag persists and new migrations apply cleanly.
- Do not `git pull` before a successful `git fetch` (keep-warm cron is unreliable).

## Environment variables

| Var | Notes |
|---|---|
| `jwt.secret` | Required (prod). Base64 HMAC key. No default in prod. |
| `DB_HOST/DB_PORT/DB_NAME/DATABASE_USERNAME/DATABASE_PASSWORD` | Postgres (Render sets via render.yaml). Presence forces prod profile (`ForgeApplication.assertProdProfileWhenDatabaseEnvPresent`). |
| `cors.allowed-origins` | Comma-separated origins. |
| `VITE_API_URL` | Frontend API base URL. |
| `JAVA_OPTS` | Set in render.yaml — see RENDER-BUDGET.md. |

## Gotchas

- **Phantom `0` pattern**: `{x && …}` short-circuits to literal `0` when `x` is a number that is falsy. Always use explicit `!= null && > 0` guards.
- **Snapshot staleness**: analytics reads today's `DailyMetric`; recompute today's snapshot on each analytics load (upsert), don't skip-if-exists, or the card goes stale within the day.
- **Consistency definitions**: ONLY `computeConsistency` (14-day, attempts+revisions+journals) defines "consistency". Heatmap must use the same sources. `Journal Streak` is journal-only by design. Do not introduce competing definitions.
- **Flyway + H2**: dev runs H2 in-memory; migrations must be H2-compatible.
- **H2 console** is dev-only. **springdoc** is disabled in prod.
- **Lazy init** is on in prod (`spring.main.lazy-initialization=true`) — don't remove it; it is load-bearing for cold starts.
