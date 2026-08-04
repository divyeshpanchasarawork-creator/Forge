# Forge — Architecture

Layered monolith, packages by feature. Constructor injection only. DTO pattern for all request/response. UUID primary keys everywhere, audit fields `createdAt`/`updatedAt`.

## Backend packages (`src/main/java/com/forge`)

| Package | Responsibility | Key files |
|---|---|---|
| `auth` | Register/login/refresh, JWT, user profile, scheduling fields, timezone | `User`, `AuthController`, `AuthService`, `JwtService` |
| `topic` | Topic CRUD, weak/strong queries, SM-2 fields, mastery inputs | `TopicRepository` |
| `problem` / `practice` | Problem attempts, practice queue, submission tracking | `ProblemAttempt`, `ProblemAttemptRepository` |
| `revision` | Revision scheduling, today's due list, complete flow | `Revision`, `RevisionRepository` (JOIN FETCH topic everywhere) |
| `recommendation` | Recommendation generation engine + persistence | `RecommendationEngine`, `RecommendationService` |
| `journal` | Journal entries (daily), streak source | `Journal`, `JournalRepository` |
| `analytics` | Dashboard-adjacent metrics: snapshotting, learning curve, insights, weekly | `AnalyticsService`, `MetricSnapshotService`, `DailyMetric` |
| `dashboard` | `/api/dashboard` aggregate | `DashboardService` |
| `memory` | Fading concepts/patterns/mistakes from journals | `MemoryService` |
| `roadmap` | Roadmap view | `RoadmapService` |
| `search` | Search across topics/problems | `SearchService` |
| `knowledge` | Concept prerequisites | `Concept` |
| `leetcode` | LeetCode profile sync + problem suggestions | `LeetCodeFetchService`, `LeetCodeSnapshot`, `ProblemSuggestion` |
| `intelligence` | Mastery, forgetting curve, skill rating, cold start | `ForgettingCurveService`, `SkillRatingService`, `MasteryService` |
| `scheduler` | Scheduled jobs + status | `AnalysisScheduler`, `EveningScheduler`, `WeeklyScheduler`, `SchedulerStatus` |
| `common` | DTOs (`ApiResponse`), exceptions, utils | `ReadinessCalculator`, `SecurityUtils`, `ProblemLoader`, `TimezoneUtil`, `TopicFilters`, `GreetingUtil` |
| `config` | Security, CORS, JPA auditing, scheduling, dev seed | `SecurityConfig`, `CorsConfig`, `JpaConfig`, `SchedulerConfig`, `DevSeedInitializer` |
| `security` | Security utilities/filter plumbing | `SecurityUtils` |

## Core entities & ownership

- `User` — owns everything; carries `targetLevel`, `preferredAnalysisTime`, `timezone`, `dailyGenerationsUsed`, `lastGenerationDate`.
- `Topic` — SM-2 fields (`easinessFactor`, `repetitionInterval`, `lastQuality`), mastery/confidence (0–100 / 0–10), `memoryStrength`, `estimatedRetention`, `lastRevision`, `lastAttemptAt`.
- `ProblemAttempt` — `outcome` in `SOLVED|PARTIAL|FAILED`, `attemptedAt`, `difficulty`, `problemSlug`, `problemTitle`.
- `Revision` — `scheduledDate`, `completed`, `priority`; always JOIN FETCH `topic`.
- `DailyMetric` — one row per user+date: `mastery, confidence, retention, skillRating, solvedDelta, revisionsDone, journalHours, consistency`.
- `LeetCodeSnapshot` — synced profile stats; `ProblemSuggestion` — curated problem queue; `LeetCodeTagStat` — per-tag solve counts.
- `Recommendation` — generated insights, `dismissed` flag, optional `problemSlug`.

## Key services & flows

### Auth
- `POST /api/auth/register|login|refresh|logout`. JWT (jjwt 0.12.6) in httpOnly `forge_token` cookie, refresh in `forge_refresh`. Rate-limited 5 req/min/IP. SecurityConfig uses Spring Security 7 lambda DSL (no `.and()`).

### Analytics (the most reworked area)
- `MetricSnapshotService.snapshotForUser(userId)` — computes and upserts today's `DailyMetric` (topics → mastery/confidence/retention/skill, attempts/revisions/journals → solvedDelta/revisionsDone/journalHours/consistency). Timezone-aware via `TimezoneUtil`.
- `MetricSnapshotService.computeConsistency(userId)` — fraction of the last **14 days** with any of: attempts, revisions, journals. **Single definition of "consistency".**
- `AnalyticsService.getAnalytics()` — snapshot today, then builds mastery/weak/strong, streak, readiness (`ReadinessCalculator`), insights.
- `AnalyticsService.getLearningCurve(days)` — daily metric points (7–90 days), milestones (mastery 50/80, skill 1100/1400, first Hard, streaks, gaps).
- `AnalyticsService.getActivityHeatmap(weeks)` — merges journals+attempts+revisions into per-day cells → `GET /api/analytics/heatmap`. Powers the frontend consistency heatmap (matches the 14-day insight definition).
- Insights: `MASTERY`, `SKILL`, `CONSISTENCY`, `ACCURACY`, `PROGRESS` (backend) + `Difficulty Mix`, `Journal Streak` (frontend-derived).

### Scheduler jobs
| Job | Cron (Asia/Kolkata unless noted) | Work |
|---|---|---|
| `AnalysisScheduler` | `0 */30 * * * *` (UTC) | For each user with `preferredAnalysisTime` within ±15 min of now (in the user's zone): `generateForUser` + refresh suggestions. Daily generation quota = 4. Records `SchedulerStatus`. |
| `EveningScheduler` | `0 0 22 * * *` | `refreshUserRetentions` + `snapshotForUser` per user (serial). |
| `WeeklyScheduler` | `0 0 20 * * 0` | `getWeeklyProgress` per user (log only). |

### Recommendation engine
`RecommendationEngine.generateForUser(userId, persist)` — low-confidence topics, overdue revisions, LeetCode gaps/milestones, tag-based problem picks via `ProblemScorer`. Problem catalog is `problems.json` loaded once by `ProblemLoader` (`slug→tag` precomputed map used by sync). Persist mode deletes old undismissed recs, saves new, syncs rec problems to suggestions.

### Intelligence
- `ForgettingCurveService` — exponential decay retention from last revision/attempt; `refreshUserRetentions` updates stored `estimatedRetention`.
- `SkillRatingService` — Elo-style rating from topic/attempt signals.
- `ReadinessCalculator` (shared util) — used by `DashboardService`, `AnalyticsService`, `RecommendationEngine`.

## Frontend (`frontend/src`)

| Area | Files |
|---|---|
| Entry | `main.tsx`, `App.tsx` (routes, lazy pages, QueryClient, AuthProvider) |
| API | `api/client.ts` (axios), `api/index.ts` (typed endpoint objects: `authApi`, `topicsApi`, `analyticsApi`, `journalsApi`, ...) |
| Types | `types/index.ts` (mirrors backend DTOs) |
| Contexts | `AuthContext.tsx`, `ThemeContext.tsx` |
| Layout | `AppLayout.tsx` (always-mounted → Cmd+K listener), `Sidebar`, `TopHeader`, `CommandPalette` (lazy), `ColdStartGate` |
| UI | `Card`, `Button`, `Badge`, `KpiCard` (metric displays, tooltip prop), `LoadingSkeleton` (`SkeletonCard`/`ChartSkeleton`), `ErrorBoundary`, `ThemeToggle` |
| Lib | `targetLevels.ts`, `error.ts` (`parseApiError`), `utils.ts` |
| Pages | Landing, Onboarding, Dashboard, Roadmap, Practice, Revision, Journal, Analytics, Memory, Profile |

- Routing is lazy (`React.lazy` per page). `PreloadCorePages` warms Dashboard/Practice/Revision after 1.5 s.
- React Query defaults: `staleTime 5 min`, `gcTime 30 min`, `retry 1`, no refetch on window focus.
- Bundling: `vite.config.ts` manual chunks — `recharts+d3 → charts` (loaded only on Analytics), `@tanstack → query`, `react-router/react → react-vendor`, `lucide → icons`, `axios → http`, rest `vendor`.
- Charts are recharts (kept deliberately; ~417 KB min in `charts-*.js` chunk). Chart components live in-page (AnalyticsPage).
