# Secure Login API

This is the independent Spring Boot REST API for the React client. It uses MySQL for users, distributed login throttling, and Spring Session JDBC storage.

## Local setup

Use Java 21 and Maven 3.8 or newer. Set the following environment variables in a local, uncommitted environment file or secret manager:

- `MYSQL_URL` — JDBC URL for a dedicated MySQL 8 database, for example `jdbc:mysql://127.0.0.1:3306/secure_login?serverTimezone=UTC`
- `MYSQL_USER`
- `MYSQL_PASSWORD`
- `SESSION_SECRET` — random secret with at least 32 bytes; used only to HMAC login-rate-limit identifiers

Optional tuning variables:

- `AUTH_BCRYPT_STRENGTH` — BCrypt work factor, default `12` (benchmark on production-sized hardware before selecting a production value)
- `AUTH_LOGIN_LIMIT` / `AUTH_LOGIN_WINDOW_SECONDS` — default 12 attempts per 300 seconds per normalized email
- `AUTH_REGISTER_LIMIT` / `AUTH_REGISTER_WINDOW_SECONDS` — default 8 attempts per 3600 seconds per normalized email
- `DB_POOL_MAX` / `DB_POOL_MIN_IDLE` — Hikari pool limits, defaults 30 / 5
- `SESSION_COOKIE_SECURE` — defaults to `true`; use `false` only for local plain-HTTP development

Run the API from the workspace root with:

```sh
bash artifacts/api-server/spring/run.sh
```

The service listens on `PORT` (8080 by default) and serves `/api`. Flyway applies additive, versioned migrations at startup. Hibernate is configured with `ddl-auto=validate`; it never creates or drops production tables. Back up the database before applying migrations in a live environment.

## API

- `GET /api/healthz` — readiness probe; checks MySQL and returns only `{ "status": "UP" }`
- `GET /api/auth/csrf` — creates/returns a CSRF token and sets the `XSRF-TOKEN` cookie
- `POST /api/auth/register` — creates accounts with a generic 202 acknowledgement to reduce email enumeration
- `POST /api/auth/login` — returns a public profile and sets the server-managed `JSESSIONID` cookie
- `GET /api/auth/me` — returns the signed-in user's public profile
- `POST /api/auth/logout` — invalidates the server session and expires the cookie

Unsafe requests require the `X-XSRF-TOKEN` header matching the `XSRF-TOKEN` cookie. The React client does this through the shared generated API fetcher. The session cookie is HttpOnly, Secure by default, SameSite=Lax, and never returned in JSON.

## Tests and build

```sh
# Unit tests (no database required)
bash artifacts/api-server/spring/build.sh

# Full MySQL integration test; point these variables at an isolated disposable database
RUN_MYSQL_INTEGRATION_TESTS=true MYSQL_URL=... MYSQL_USER=... MYSQL_PASSWORD=... \
  bash artifacts/api-server/spring/run-tests.sh
```

The HMAC key is not logged. The API does not trust forwarded client-IP headers; rate limiting is shared across instances but is currently keyed by normalized account identifier. Add a trusted edge/IP limiter at the load balancer before exposing this service to broad public traffic.
