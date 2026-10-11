# Secure Login

A React login experience backed by an independently runnable Spring Boot REST API and MySQL, with server-side sessions, CSRF protection, and account controls.

## Run & Operate

- `pnpm --filter @workspace/secure-login run dev` — run the React client
- `bash artifacts/api-server/spring/run.sh` — run the Spring Boot API (port 8080, path `/api`)
- `bash artifacts/api-server/spring/build.sh` — package the API
- `bash artifacts/api-server/spring/run-tests.sh` — run backend unit tests and any explicitly enabled MySQL integration tests
- `pnpm run typecheck` — full typecheck across all packages
- `pnpm run build` — typecheck + build all packages
- `pnpm --filter @workspace/api-spec run codegen` — regenerate API hooks and Zod schemas from the OpenAPI spec
- Required API environment: `MYSQL_URL`, `MYSQL_USER`, `MYSQL_PASSWORD`, and `SESSION_SECRET` (store as workspace secrets; never commit them)
- Local HTTP only: set `SESSION_COOKIE_SECURE=false`; keep it `true` behind HTTPS
- MySQL integration tests: set `RUN_MYSQL_INTEGRATION_TESTS=true` and point the same database variables at a disposable test database

## Stack

- Client: React, TypeScript, Vite, React Query, and generated OpenAPI hooks
- API: Java 21 and Spring Boot 3.5; Maven
- Database: MySQL 8 with Flyway migrations, Spring Data JPA, and JDBC-backed Spring sessions
- Security: BCrypt passwords, HttpOnly session cookies, CSRF tokens, generic authentication errors, and shared HMAC-keyed account throttles
- API contract: `lib/api-spec/openapi.yaml`; generated client packages live in `lib/api-client-react` and `lib/api-zod`

## Where things live

- `artifacts/secure-login/src` — browser UI, pages, and client-side validation
- `artifacts/api-server/spring/src/main/java` — REST API and security code
- `artifacts/api-server/spring/src/main/resources/db/migration` — ordered MySQL migrations
- `artifacts/api-server/spring/load-tests` — k6 scenario and synthetic-account cleanup SQL
- `lib/api-spec/openapi.yaml` — source-of-truth API contract

## Architecture decisions

- The browser and API remain independently runnable/deployable; the API is exposed under `/api`.
- Browser login uses server-stored sessions, not JWTs or browser local storage.
- Login/register throttles are shared in MySQL and keyed by HMAC digests of normalized emails; they are not a substitute for a trusted edge/IP limiter.
- Flyway owns schema changes; Hibernate validates the schema and does not create or drop tables.

## Product

- Register and sign in with an email and password.
- Keep the dashboard behind a server-validated session.
- Sign out by invalidating the server session.

## User preferences

The user asked for a fresh implementation because the old source code is unavailable. Do not claim that a production deployment or 1,000-user capacity has been validated unless those checks are run against staging.

## Gotchas

- The API will not start without MySQL settings and the workspace-provided `SESSION_SECRET`.
- Integration tests are skipped unless `RUN_MYSQL_INTEGRATION_TESTS=true`; they require a disposable MySQL database.
- The 1,000-VU k6 test must only be run against an isolated staging deployment, never production.
- Do not use `lib/db`'s generated PostgreSQL scaffold for this app; the auth API explicitly uses MySQL.

## Pointers

- See `artifacts/api-server/spring/README.md` for API configuration and local operation.
- See `artifacts/api-server/spring/load-tests/README.md` for the staging load-test process.
- See the `pnpm-workspace` skill for workspace structure, TypeScript setup, and package details.
