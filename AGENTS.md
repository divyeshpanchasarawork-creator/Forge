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

## Testing
Run: `mvn test`

## Key Dependencies
- Spring Boot 4.0.7, Spring Security 7, Spring Data JPA
- PostgreSQL (prod), H2 (dev)
- Flyway, Lombok, Springdoc OpenAPI 3.0.2
- jjwt 0.12.6 for JWT

## Deployment
- Frontend: Vercel
- Backend: Render
- Database: PostgreSQL on Render
