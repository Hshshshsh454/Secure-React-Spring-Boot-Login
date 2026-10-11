import http from 'k6/http';
import { check, fail, group } from 'k6';
import { Rate } from 'k6/metrics';

const apiBaseUrl = (__ENV.API_BASE_URL || '').replace(/\/+$/, '');
const vus = Number(__ENV.K6_VUS || 1000);
const runId = __ENV.K6_RUN_ID || String(Date.now());
const password = 'Load-Test-Only-Password-2026';
const flowFailures = new Rate('auth_flow_failures');
const maxFailureRate = Number(__ENV.K6_MAX_FAILURE_RATE || 0.01);
const maxP95Ms = Number(__ENV.K6_MAX_P95_MS || 2500);
const setupBatchSize = Number(__ENV.K6_SETUP_BATCH_SIZE || 20);

export const options = {
  scenarios: {
    login_and_session: {
      executor: 'per-vu-iterations',
      vus,
      iterations: 1,
      maxDuration: '10m',
    },
  },
  thresholds: {
    auth_flow_failures: [`rate<${maxFailureRate}`],
    http_req_duration: [`p(95)<${maxP95Ms}`],
  },
};

function requireStagingTarget() {
  if (__ENV.K6_CONFIRM_STAGING !== 'YES') {
    fail('Refusing to run: set K6_CONFIRM_STAGING=YES only for an isolated staging environment.');
  }
  if (!apiBaseUrl || !apiBaseUrl.endsWith('/api')) {
    fail('Set API_BASE_URL to the staging origin ending in /api.');
  }
  if (!Number.isInteger(vus) || vus < 1 || vus > 1000) {
    fail('K6_VUS must be an integer from 1 through 1000.');
  }
  if (!Number.isInteger(setupBatchSize) || setupBatchSize < 1 || setupBatchSize > 25) {
    fail('K6_SETUP_BATCH_SIZE must be an integer from 1 through 25.');
  }
}

export function setup() {
  requireStagingTarget();

  const csrfResponse = http.get(`${apiBaseUrl}/auth/csrf`, {
    tags: { phase: 'seed-csrf' },
  });
  if (csrfResponse.status !== 200) {
    fail(`Could not obtain the CSRF token (HTTP ${csrfResponse.status}).`);
  }
  const csrfToken = csrfResponse.json('token');
  if (!csrfToken) fail('The CSRF response did not include a token.');

  const accounts = Array.from({ length: vus }, (_, index) => ({
    email: `k6-${runId}-${index + 1}@load-test.invalid`,
    password,
  }));

  for (let offset = 0; offset < accounts.length; offset += setupBatchSize) {
    const batch = accounts.slice(offset, offset + setupBatchSize).map((account) => [
      'POST',
      `${apiBaseUrl}/auth/register`,
      JSON.stringify(account),
      {
        headers: {
          'Content-Type': 'application/json',
          'X-XSRF-TOKEN': csrfToken,
          Cookie: `XSRF-TOKEN=${csrfToken}`,
        },
        tags: { phase: 'seed-account' },
      },
    ]);

    const responses = http.batch(batch);
    for (const response of responses) {
      if (response.status !== 202) {
        fail(`Synthetic account setup failed (HTTP ${response.status}); stop and inspect staging before retrying.`);
      }
    }
  }

  console.log(`Seeded ${accounts.length} synthetic accounts. Cleanup run ID: ${runId}`);
  return { accounts };
}

function recordCheck(passed, checks, name) {
  flowFailures.add(!passed);
  check(passed, { [name]: (result) => result === true }, { phase: checks });
  return passed;
}

export default function (data) {
  const account = data.accounts[__VU - 1];
  if (!account) fail(`No seeded account was assigned to virtual user ${__VU}.`);

  group('login and session lifecycle', () => {
    const csrfResponse = http.get(`${apiBaseUrl}/auth/csrf`, {
      tags: { phase: 'user-csrf' },
    });
    const csrfToken = csrfResponse.status === 200
      ? csrfResponse.json('token')
      : undefined;
    if (!recordCheck(Boolean(csrfToken), 'csrf', 'csrf token issued')) return;

    const headers = {
      'Content-Type': 'application/json',
      'X-XSRF-TOKEN': csrfToken,
    };
    const login = http.post(
      `${apiBaseUrl}/auth/login`,
      JSON.stringify(account),
      { headers, tags: { phase: 'login' } },
    );
    if (!recordCheck(login.status === 200, 'login', 'login accepted')) return;

    const profile = http.get(`${apiBaseUrl}/auth/me`, {
      tags: { phase: 'profile' },
    });
    const profileMatches = profile.status === 200
      && profile.json('email') === account.email;
    if (!recordCheck(profileMatches, 'profile', 'session returns its account')) return;

    const logout = http.post(
      `${apiBaseUrl}/auth/logout`,
      null,
      { headers, tags: { phase: 'logout' } },
    );
    recordCheck(logout.status === 204, 'logout', 'session invalidated');
  });
}
