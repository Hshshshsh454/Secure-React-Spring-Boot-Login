# Security and verification status

| Area | Coverage | Status |
| --- | --- | --- |
| Email normalization | Trim and locale-independent lowercase | PASS — unit tests |
| BCrypt input length | UTF-8 byte boundary around BCrypt's 72-byte limit | PASS — unit tests |
| Rate-limit key privacy | Deterministic HMAC keys and scope separation; short secret rejected | PASS — unit tests |
| Duplicate-account handling | MySQL duplicate-key detection and generic registration acknowledgement | PARTIAL — detection unit-tested; API behavior requires MySQL |
| Successful session lifecycle | Registration, login, protected profile, logout, and post-logout denial | NOT RUN — MySQL integration test requires a disposable MySQL database |
| Credential enumeration | Wrong password and unknown user share status and response text | NOT RUN — MySQL integration test requires a disposable MySQL database |
| CSRF, cookie flags, and protected endpoints | MySQL integration suite | NOT RUN — needs live test service |
| Database failure behavior | Generic 503 and readiness behavior | NOT RUN — no failure-injection environment configured |
| 1,000 concurrent login/session flows | k6 staging scenario included | NOT RUN — requires isolated staging and an explicit operator run |

Current automated backend run: 9 unit tests passed; 2 MySQL integration tests were skipped because `RUN_MYSQL_INTEGRATION_TESTS` was not enabled. No throughput or production-readiness claim is made.
