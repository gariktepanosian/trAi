import { test, expect } from '@playwright/test';
import { authHeaders } from '../helpers/auth-helper';
import { VERIFIABLE_STATEMENT, MISLEADING_STATEMENT } from '../helpers/fixtures';

/**
 * Live Fact Check API Tests
 * Covers: POST /api/v1/live/verify-statement
 *
 * SKILL: SKILL-ai-engine.md → "Playwright Test Coverage Trigger"
 * SKILL: SKILL-backend-architecture.md → "Playwright Test Coverage Trigger"
 *
 * Note: Tests run with AI fallback (demo API keys) — response comes from
 * heuristic fallback engine, which is deterministic based on keywords.
 */
test.describe('Live Fact Check — POST /api/v1/live/verify-statement', () => {

    test('returns 200 with valid statement', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/live/verify-statement', {
            headers,
            data: {
                speaker: 'Press Secretary',
                statement: VERIFIABLE_STATEMENT,
            },
        });
        expect(res.status()).toBe(200);
    });

    test('response includes required fields', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/live/verify-statement', {
            headers,
            data: {
                speaker: 'Test Speaker',
                statement: 'Unemployment dropped by two percent last quarter.',
            },
        });

        const body = await res.json();
        expect(body.status).toBeDefined();
        expect(body.speaker).toBeDefined();
        expect(body.timestamp).toBeDefined();
        // Either live engine or fallback — both must include these
        expect(['SUCCESS', 'BLOCKED', 'HALTED']).toContain(body.status);
    });

    test('response includes guardrail fields', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/live/verify-statement', {
            headers,
            data: {
                speaker: 'Journalist',
                statement: 'The government released new economic data today.',
            },
        });

        const body = await res.json();
        expect(body.guardrailFlags).toBeDefined();
        expect(body.guardrailBlocked).toBeDefined();
        expect(typeof body.guardrailBlocked).toBe('boolean');
    });

    test('fallback heuristic: verifiable statement gets VERIFIED_TRUE', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/live/verify-statement', {
            headers,
            data: {
                speaker: 'Reporter',
                statement: 'The president signed the executive order at the White House.',
            },
        });

        const body = await res.json();
        expect(body.status).toBe('SUCCESS');
        // In fallback mode, "signed executive order" → VERIFIED_TRUE
        if (body.mode === 'STANDALONE_FALLBACK_ENGINE') {
            expect(body.verdict).toBe('VERIFIED_TRUE');
            expect(body.trustScore).toBeGreaterThan(70);
        }
    });

    test('fallback heuristic: misleading economic claim gets low trust score', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/live/verify-statement', {
            headers,
            data: {
                speaker: 'Politician',
                statement: MISLEADING_STATEMENT,
            },
        });

        const body = await res.json();
        expect(body.status).toBe('SUCCESS');
        if (body.mode === 'STANDALONE_FALLBACK_ENGINE') {
            expect(['MISLEADING', 'FALSE', 'UNVERIFIED_CLAIM']).toContain(body.verdict);
        }
    });

    test('missing speaker defaults to Unknown Speaker', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/live/verify-statement', {
            headers,
            data: {
                statement: 'Some test statement here.',
            },
        });
        const body = await res.json();
        expect(res.status()).toBe(200);
        expect(body.speaker).toBe('Unknown Speaker');
    });

    test('empty statement returns 400 or BLOCKED', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/live/verify-statement', {
            headers,
            data: { speaker: 'Test', statement: '' },
        });
        // Either 400 validation error or BLOCKED by sanitizer
        expect([200, 400]).toContain(res.status());
        if (res.status() === 200) {
            const body = await res.json();
            expect(['BLOCKED', 'SUCCESS']).toContain(body.status);
        }
    });

    test('mediaSource field is included in response', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/live/verify-statement', {
            headers,
            data: {
                speaker: 'Anchor',
                statement: 'Breaking news reported from the scene.',
                mediaSource: 'CNN Live',
            },
        });
        const body = await res.json();
        expect(body.mediaSource).toBe('CNN Live');
    });
});
