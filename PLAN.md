# 🔥 FORGE — Build Plan

> Your Personal Engineering Companion.

---

## Status: MVP COMPLETE ✅

Backend compiles. Frontend builds. Ready for testing.

---

## Execution Tracker

### Phase 1: Foundation ✅
- [x] 1.1 Fix pom.xml dependencies (PostgreSQL, JWT, springdoc 3.0.3)
- [x] 1.2 Create application profiles (dev/prod)
- [x] 1.3 Create BaseEntity + JPA auditing
- [x] 1.4 Flyway V1__init.sql (all tables + indexes)
- [x] 1.5 Common DTOs, exceptions, response wrapper
- [x] 1.6 Verify app compiles

### Phase 2: Security & Auth ✅
- [x] 2.1 JWT Token Provider (jjwt 0.12.6)
- [x] 2.2 JWT Authentication Filter
- [x] 2.3 SecurityConfig (Spring Security 7 lambda DSL)
- [x] 2.4 Auth module (controller, service, DTOs, repository)
- [x] 2.5 Flyway V2__seed_default_user.sql

### Phase 3: Core Modules ✅
- [x] 3.1 Topic module (CRUD + weak/strong/revision-needed)
- [x] 3.2 Problem module (CRUD + M2M with topics)
- [x] 3.3 Revision module (today, pending, complete with mastery update)
- [x] 3.4 Recommendation module (CRUD + dismiss)
- [x] 3.5 Journal module (CRUD + upsert + recent)
- [x] 3.6 Analytics module (full analytics + weekly progress)
- [x] 3.7 Dashboard module (aggregated dashboard)

### Phase 4: Scheduler ✅
- [x] 4.1 MorningScheduler (08:00 IST)
- [x] 4.2 EveningScheduler (22:00 IST - retention update)
- [x] 4.3 WeeklyScheduler (Sunday - weekly report)

### Phase 5: Recommendation Engine ✅
- [x] 5.1 Deterministic rules (low confidence, overdue, no activity, mastery threshold)

### Phase 6: Frontend Setup ✅
- [x] 6.1 Vite + React 19 + TypeScript
- [x] 6.2 TailwindCSS (dark mode first)
- [x] 6.3 React Query + React Router
- [x] 6.4 API client with JWT interceptor
- [x] 6.5 Auth context

### Phase 7: Frontend Pages ✅
- [x] 7.1 Login page
- [x] 7.2 Layout (Sidebar with navigation)
- [x] 7.3 Dashboard page (greeting, stats, revisions, recs, weak/strong topics)
- [x] 7.4 Topics page (grid view, add form, mastery bars, badges)
- [x] 7.5 Problems page (table view, difficulty badges, add form)
- [x] 7.6 Revision page (today's revisions, complete button)
- [x] 7.7 Journal page (form with energy/mood sliders, recent entries)
- [x] 7.8 Analytics page (Recharts: bar, radar charts + stats)
- [ ] 7.9 Settings page (placeholder)

### Phase 8: Sample Data ✅
- [x] 8.1 Flyway V3__sample_data.sql (40 topics, 20 problems, 10 revisions, 10 journals, 10 recs)

### Phase 9: Testing
- [ ] 9.1 Unit tests (pending)

### Phase 10: Deployment ✅
- [x] 10.1 render.yaml (backend + PostgreSQL)
- [x] 10.2 vercel.json (frontend SPA rewrites)
- [x] 10.3 README.md

### Phase 11: Polish
- [ ] 11.1 Settings page
- [ ] 11.2 Toast notifications
- [ ] 11.3 Error boundaries

---

## Files Created

### Backend (Java)
```
src/main/java/com/forge/
├── ForgeApplication.java
├── config/
│   ├── CorsConfig.java
│   ├── JpaConfig.java
│   ├── SchedulerConfig.java
│   └── SecurityConfig.java
├── security/
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   └── UserPrincipal.java
├── common/
│   ├── entity/BaseEntity.java
│   ├── dto/ApiResponse.java, PagedResponse.java
│   ├── exception/GlobalExceptionHandler.java, ResourceNotFoundException.java, BadRequestException.java
│   └── util/GreetingUtil.java
├── auth/
│   ├── controller/AuthController.java
│   ├── service/AuthService.java, CustomUserDetailsService.java
│   ├── repository/UserRepository.java
│   ├── entity/User.java
│   └── dto/LoginRequest.java, LoginResponse.java, RegisterRequest.java
├── topic/
│   ├── controller/TopicController.java
│   ├── service/TopicService.java
│   ├── repository/TopicRepository.java
│   ├── entity/Topic.java
│   ├── dto/TopicRequest.java, TopicResponse.java
│   └── mapper/TopicMapper.java
├── problem/
│   ├── controller/ProblemController.java
│   ├── service/ProblemService.java
│   ├── repository/ProblemRepository.java
│   ├── entity/Problem.java
│   ├── dto/ProblemRequest.java, ProblemResponse.java
│   └── mapper/ProblemMapper.java
├── revision/
│   ├── controller/RevisionController.java
│   ├── service/RevisionService.java
│   ├── repository/RevisionRepository.java
│   ├── entity/Revision.java
│   ├── dto/RevisionResponse.java
│   └── mapper/RevisionMapper.java
├── recommendation/
│   ├── controller/RecommendationController.java
│   ├── service/RecommendationService.java, RecommendationEngine.java
│   ├── repository/RecommendationRepository.java
│   ├── entity/Recommendation.java
│   ├── dto/RecommendationResponse.java
│   └── mapper/RecommendationMapper.java
├── journal/
│   ├── controller/JournalController.java
│   ├── service/JournalService.java
│   ├── repository/JournalRepository.java
│   ├── entity/Journal.java
│   ├── dto/JournalRequest.java, JournalResponse.java
│   └── mapper/JournalMapper.java
├── analytics/
│   ├── controller/AnalyticsController.java
│   ├── service/AnalyticsService.java
│   └── dto/AnalyticsResponse.java, WeeklyProgressResponse.java
├── dashboard/
│   ├── controller/DashboardController.java
│   ├── service/DashboardService.java
│   └── dto/DashboardResponse.java
└── scheduler/
    ├── MorningScheduler.java
    ├── EveningScheduler.java
    └── WeeklyScheduler.java
```

### Frontend (React)
```
frontend/src/
├── main.tsx, App.tsx, index.css
├── api/client.ts, index.ts
├── contexts/AuthContext.tsx
├── lib/utils.ts
├── types/index.ts
├── components/
│   ├── layout/Sidebar.tsx, AppLayout.tsx
│   └── ui/Card.tsx, Badge.tsx
└── pages/
    ├── LoginPage.tsx
    ├── DashboardPage.tsx
    ├── TopicsPage.tsx
    ├── ProblemsPage.tsx
    ├── RevisionPage.tsx
    ├── JournalPage.tsx
    └── AnalyticsPage.tsx
```

### Config
```
├── pom.xml (updated with PostgreSQL, JWT, springdoc 3.0.3)
├── application.properties (shared)
├── application-dev.properties (H2)
├── application-prod.properties (PostgreSQL)
├── render.yaml (Render deployment)
├── frontend/vercel.json (Vercel deployment)
├── AGENTS.md
├── PLAN.md
└── README.md
```

### Database Migrations
```
src/main/resources/db/migration/
├── V1__init.sql (schema)
├── V2__seed_default_user.sql (forge/forge123)
└── V3__sample_data.sql (40 topics, 20 problems, etc.)
```

---

## How to Run

### Backend
```bash
cd /Users/divyesh/Dev/projects/forge
./mvnw spring-boot:run
# API: http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui.html
# H2 Console: http://localhost:8080/h2-console
```

### Frontend
```bash
cd /Users/divyesh/Dev/projects/forge/frontend
npm install
npm run dev
# App: http://localhost:5173
```

### Default Login
- Username: `forge`
- Password: `forge123`
