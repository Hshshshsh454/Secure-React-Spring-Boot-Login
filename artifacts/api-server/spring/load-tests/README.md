# Staging load test

`1000-login-sessions.js` creates a run-scoped set of synthetic accounts and gives each virtual user one login → current-user → logout flow. The account seed is intentionally outside the measured login flow. It leaves the accounts in the database until the matching cleanup SQL is run.

Only run against an isolated staging environment that you control. Never point this scenario at production: even a single run creates accounts and performs up to 1,000 concurrent password verifications.

```sh
K6_CONFIRM_STAGING=YES \
API_BASE_URL=https://your-staging-host.example/api \
K6_VUS=1000 \
k6 run artifacts/api-server/spring/load-tests/1000-login-sessions.js
```

The script uses a starter threshold of 1% failed flows and a 2.5-second p95 request duration. These are example guardrails, not an accepted product SLO; adjust them for the actual staging hardware and agreed service objectives. A successful k6 run is evidence only for that exact environment, database size, build, and configuration.

After reviewing the printed `Cleanup run ID`, replace `REPLACE_WITH_RUN_ID` in `cleanup-k6-users.sql` and execute it against the same isolated staging database. The SQL only matches the run-scoped `k6-<run-id>-*@load-test.invalid` accounts and their sessions. Orphaned rate-limit digests are removed by the API cleanup job within a day.
