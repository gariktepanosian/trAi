# SKILL: Playwright Testing
# TrAI Platform — UI + API End-to-End Tests

## What this skill covers

The complete Playwright testing strategy: test file structure, naming conventions,
how to write API tests, UI tests, AI mock patterns, and the rule that every feature
must have corresponding Playwright coverage.

---

## Testing Stack

| Tool | Version | Purpose |
|---|---|---|
| Playwright | ^1.47.0 | E2E test framework |
| `@playwright/test` | ^1.47.0 | Test runner + assertions |
| TypeScript | ^5.x | All test files written in TypeScript |
| Node.js | 20+ | Runtime |

---

## Project Structure

```
TrAI/
└── tests/
    ├── playwright.config.ts       # Global config — base URLs, timeouts, retries
    ├── package.json               # Test dependencies
    ├── tsconfig.json              # TypeScript config for tests
    │
    ├── api/                       # Backend API tests (no browser — API context)
    │   ├── auth.spec.ts
    │   ├── ai-live-factcheck.spec.ts
    │   ├── ai-news-normalize.spec.ts
    │   ├── ai-glitch-audit.spec.ts
    │   ├── ai-multi-agent.spec.ts
    │   ├── country-subscribe.spec.ts
    │   ├── country-trigger-check.spec.ts
    │   ├── alerts.spec.ts
    │   ├── notifications.spec.ts
    │   ├── sanitizer.spec.ts
    │   ├── rate-limiting.spec.ts
    │   └── health.spec.ts
    │
    ├── ui/                        # Frontend web UI tests (browser-based)
    │   ├── home.spec.ts
    │   ├── live-fact-check.spec.ts
    │   ├── news.spec.ts
    │   ├── alerts-ui.spec.ts
    │   └── settings-ui.spec.ts
    │
    └── helpers/
        ├── ai-mock.ts             # Shared AI response mock helpers
        ├── auth-helper.ts         # Login + get JWT token helper
        └── fixtures.ts            # Shared test data
```

---

## `playwright.config.ts`

```typescript
import { defineConfig } from '@playwright/test';

export default defineConfig({
    testDir: './tests',
    timeout: 60_000,          // 60s per test (AI calls can be slow)
    retries: 1,               // Retry once on failure
    reporter: [['html', { open: 'never' }], ['list']],
    use: {
        baseURL: process.env.BASE_URL || 'http://localhost:8080',
        extraHTTPHeaders: { 'Content-Type': 'application/json' },
    },
    projects: [
        {
            name: 'API Tests',
            testDir: './tests/api',
            use: { /* no browser */ },
        },
        {
            name: 'UI Tests (Chromium)',
            testDir: './tests/ui',
            use: {
                browserName: 'chromium',
                baseURL: process.env.FRONTEND_URL || 'http://localhost:3000',
            },
        },
    ],
});
```

---

## API Test Pattern

All API tests use Playwright's `request` context (no browser).

```typescript
// tests/api/health.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Health Check', () => {

    test('GET /api/v1/trust/status returns OPERATIONAL', async ({ request }) => {
        const response = await request.get('/api/v1/trust/status');
        expect(response.status()).toBe(200);

        const body = await response.json();
        expect(body.status).toBe('OPERATIONAL');
        expect(body.version).toBe('2.0.0');
        expect(body.aiModels).toHaveLength(3);
        expect(body.cloudProvider).toContain('Google Cloud');
    });

    test('GET /api/v1/trust/status returns kill switch state', async ({ request }) => {
        const response = await request.get('/api/v1/trust/status');
        const body = await response.json();
        expect(body.killSwitchActive).toBe(false);
    });
});
```

---

## Auth Helper Pattern

```typescript
// tests/helpers/auth-helper.ts
import { APIRequestContext } from '@playwright/test';

export async function getAuthToken(request: APIRequestContext): Promise<string> {
    // Register a test user (idempotent — ignore 409)
    await request.post('/api/v1/auth/register', {
        data: { username: 'testuser', password: 'TestPass123!', email: 'test@trai.dev' }
    });

    const loginRes = await request.post('/api/v1/auth/login', {
        data: { username: 'testuser', password: 'TestPass123!' }
    });
    const body = await loginRes.json();
    return body.token;
}

// Usage in tests:
// const token = await getAuthToken(request);
// const response = await request.post('/api/v1/...', {
//     headers: { 'Authorization': `Bearer ${token}` },
//     data: { ... }
// });
```

---

## AI Mock Pattern

AI model calls (to `https://api.x.ai/v1`, etc.) must be intercepted in tests.
Use Playwright's `page.route()` or `request.on('requestFailed')` patterns.

For API-only tests (no browser), use the backend's **fallback mode**:
- Start backend with dummy API keys (`XAI_API_KEY=demo-key`)
- The AI engine fails → falls back to heuristic → returns deterministic response
- Test the fallback path

For testing specific AI responses, use a **mock backend** or test with the fallback output:

```typescript
// tests/helpers/ai-mock.ts
export const MOCK_VERIFIED_RESPONSE = JSON.stringify({
    verdict: 'VERIFIED_TRUE',
    trustScore: 92,
    statementAnalyzed: 'Test statement',
    summary: 'Mock verified summary'
});

export const MOCK_CRITICAL_MULTI_AGENT_RESPONSE = {
    finalVerdict: 'VERIFIED_TRUE',
    consensusTrustScore: 78,
    criticalAlert: true,
    criticalLevel: 'HIGH',
    requiresPushNotification: true,
    country: 'Armenia',
    processingTimeMs: 1200,
    agent1: { agentId: 'AGENT_1_COLLECTOR', aggregatedScore: 80 },
    agent2: { agentId: 'AGENT_2_VALIDATOR', aggregatedScore: 76 }
};
```

---

## UI Test Pattern

```typescript
// tests/ui/home.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Home Screen', () => {

    test('app loads and shows 4 navigation tabs', async ({ page }) => {
        await page.goto('/');
        await expect(page).toHaveTitle(/TrAI/);

        // Bottom navigation tabs
        await expect(page.getByText('Live Check')).toBeVisible();
        await expect(page.getByText('News')).toBeVisible();
        await expect(page.getByText('Alerts')).toBeVisible();
        await expect(page.getByText('Settings')).toBeVisible();
    });

    test('status indicator shows backend state', async ({ page }) => {
        await page.goto('/');
        // Backend status chip in AppBar
        const statusChip = page.locator('[data-testid="backend-status"]');
        await expect(statusChip).toBeVisible({ timeout: 5000 });
    });

    test('clicking News tab loads news content', async ({ page }) => {
        await page.goto('/');
        await page.getByText('News').click();
        // News content should appear
        await expect(page.locator('[data-testid="news-feed"]')).toBeVisible({ timeout: 8000 });
    });
});
```

---

## `data-testid` Convention

Add `data-testid` attributes to key UI elements in the **web frontend** (`frontend/app.js`).
Flutter widget tests use `Key()` — not Playwright.

Required `data-testid` values for each tab:

| Element | `data-testid` |
|---|---|
| Backend status chip | `backend-status` |
| News feed container | `news-feed` |
| Alerts list container | `alerts-list` |
| Alert severity badge | `alert-severity-<id>` |
| Resolve button | `resolve-btn-<id>` |
| Country filter badge | `country-indicator` |
| Live check form | `live-check-form` |
| Audit submit button | `audit-submit-btn` |
| Result card | `result-card` |
| Trust score display | `trust-score` |

---

## Feature → Playwright Coverage Rule

**Every new feature that adds or changes:**
- A REST endpoint → add/update `tests/api/<feature>.spec.ts`
- A UI component → add/update `tests/ui/<screen>.spec.ts`
- An AI engine → update the corresponding `tests/api/ai-*.spec.ts` with mock

This is enforced by the skill file at the end of every `SKILL-*.md` under the heading
**"Playwright Test Coverage Trigger"**.

---

## After Adding a New Feature (Checklist)

When a skill file (e.g., `SKILL-twitter-country-monitoring.md`) says its
"Playwright Test Coverage Trigger" section requires a test file, the AI agent MUST:

1. Create the test file at the path listed in that skill's coverage trigger section
2. Cover at minimum: happy path, error/fallback path, and edge cases
3. Run `npx playwright test tests/api/<new-file>.spec.ts` (API tests need no browser)
4. Ensure all existing tests still pass: `npx playwright test`
5. Update the skill file's coverage trigger section if new cases were discovered

---

## Running Tests

```bash
# Install dependencies (first time)
cd tests && npm install

# Run all API tests
npx playwright test tests/api/ --project="API Tests"

# Run all UI tests (requires frontend running on localhost:3000)
npx playwright test tests/ui/ --project="UI Tests (Chromium)"

# Run a specific test file
npx playwright test tests/api/ai-multi-agent.spec.ts

# Run with UI debugger
npx playwright test --ui

# Generate HTML report
npx playwright show-report
```

---

## CI Integration

Playwright tests are run in the CI pipeline (`.github/workflows/ci.yml`).
Add a 6th job to run API tests against the deployed Cloud Run URL:

```yaml
playwright-smoke-tests:
    name: Playwright Smoke Tests (Post-Deploy)
    needs: deploy-to-google-cloud
    runs-on: ubuntu-latest
    steps:
        - uses: actions/checkout@v4
        - uses: actions/setup-node@v4
          with: { node-version: '20' }
        - run: cd tests && npm ci
        - run: cd tests && npx playwright install chromium
        - run: cd tests && npx playwright test tests/api/health.spec.ts
          env:
            BASE_URL: ${{ steps.deploy.outputs.url }}
```
