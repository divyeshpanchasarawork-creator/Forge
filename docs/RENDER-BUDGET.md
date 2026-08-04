# Forge — Render Free-Tier Budget

Deployment: Render free web service (Docker) + Render free Postgres + Vercel (frontend). Hard limits: **0.1 vCPU (shared)**, **512 MB RAM**, 15-min idle sleep, 100 GB/mo bandwidth (API only — frontend is on Vercel).

## Current resource settings (render.yaml)

`JAVA_OPTS=-Xms64m -Xmx256m -XX:MaxMetaspaceSize=192m -XX:CompressedClassSpaceSize=64m -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError`

Rationale:
- **`-Xmx256m` heap** — fits in 512 MB alongside metaspace + threads + pools.
- **`-Xms64m`** — don't commit the full heap at boot; grow as needed (lazy init already slims startup).
- **`-XX:MaxMetaspaceSize=192m` + `CompressedClassSpaceSize=64m`** — Spring/Hibernate/Jackson metaspace is the silent memory grower; cap it to stay under the limit.
- **`-XX:+UseSerialGC`** — 0.1 vCPU is single-threaded in effect; G1's parallel GC threads waste CPU and RAM. Serial is correct here.
- **`-XX:+ExitOnOutOfMemoryError`** — Render restarts a dead process instead of serving a wedged one.

## prod.properties (server.tomcat + datasource)

- Tomcat: `threads.max=10`, `threads.min-spare=2`, `accept-count=20`, `max-connections=40`. Default is 200 threads — pure waste for a low-traffic app; each thread reserves stack.
- Hikari: `maximum-pool-size=3`, `minimum-idle=1`, `connection-timeout=5000`. Keeps us under the free Postgres connection cap; 5 s timeout fails fast during cold-start DB races.
- gzip: `server.compression.enabled=true`, `min-response-size=1KB`.
- `hibernate.use_sql_comments=false`, `format_sql=false` (overhead only).

## Estimated committed memory

Heap ≤256 MB + metaspace ≤192 MB + Tomcat/Hikari/JIT ≈ **320–380 MB** total — comfortable under 512 MB with headroom for spikes.

## Capacity model

| Dimension | Headroom | Binding? |
|---|---|---|
| RAM | Plenty for dozens of concurrent requests | No |
| CPU (0.1 vCPU) | ~2–10 API req/s sustained; analytics page = 4–8 queries + snapshot recompute | **Yes — the ceiling** |
| Postgres free | 1 GB storage, limited connections (Hikari=3 respects it) | Yes (storage at scale) |
| Schedulers | Run serially across all users (evening snapshot, 30-min analysis) | **Yes — first wall past ~300 users** |

Practical answer: **~5–15 concurrent users** with acceptable latency; **~100–300 registered accounts** comfortable for a sporadic-usage personal app.

## Scaling triggers (in order)

1. Scheduler CPU cost (make `generateForUser` cheaper / stagger per-user) → most impactful before ~300 users.
2. Postgres storage growth → prune `ProblemAttempt`/`DailyMetric` history or move DB.
3. Sustained concurrency → paid web instance (more CPU), then connection pool up.

## Keep-warm

Free tier sleeps after 15 min idle; a GitHub Actions cron pings `/api/health` periodically. Sleep is fine — cold start ~30–60 s.
