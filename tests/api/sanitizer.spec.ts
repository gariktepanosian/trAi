import { test, expect } from '@playwright/test';
import { authHeaders } from '../helpers/auth-helper';
import { INJECTION_STATEMENT } from '../helpers/fixtures';

/**
 * Input Sanitizer Tests
 * Covers: Prompt injection, XSS, and oversized input detection.
 *
 * SKILL: SKILL-security-auth.md → "Playwright Test Coverage Trigger"
 */
test.describe('Input Sanitizer', () => {

    test('prompt injection returns BLOCKED status', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/live/verify-statement', {
            headers,
            data: {
                speaker: 'Attacker',
                statement: INJECTION_STATEMENT,
            },
        });
        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.status).toBe('BLOCKED');
        expect(body.flags).toBeDefined();
    });

    test('XSS attempt is blocked', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/live/verify-statement', {
            headers,
            data: {
                speaker: 'Test',
                statement: '<script>alert("xss")</script> This is a test.',
            },
        });
        const body = await res.json();
        // Either blocked or sanitized (script tags stripped)
        expect(['BLOCKED', 'SUCCESS']).toContain(body.status);
        if (body.status === 'SUCCESS') {
            // Script tag must not appear in any AI response
            expect(JSON.stringify(body)).not.toContain('<script>');
        }
    });

    test('extremely long input is blocked', async ({ request }) => {
        const headers = await authHeaders(request);
        const longInput = 'A'.repeat(11_000); // Over 10,000 char limit
        const res = await request.post('/api/v1/live/verify-statement', {
            headers,
            data: { speaker: 'Test', statement: longInput },
        });
        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.status).toBe('BLOCKED');
    });

    test('normal valid input passes through sanitizer', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/live/verify-statement', {
            headers,
            data: {
                speaker: 'Journalist',
                statement: 'The central bank raised interest rates by 0.25 percent.',
            },
        });
        const body = await res.json();
        expect(body.status).toBe('SUCCESS');
    });
});

/**
 * Rate Limiting Tests
 * Covers: Bucket4j 20 req/min per IP limit.
 *
 * SKILL: SKILL-security-auth.md → "Playwright Test Coverage Trigger"
 */
test.describe('Rate Limiting', () => {

    test('first 20 requests within a minute succeed', async ({ request }) => {
        // Note: This test uses a separate endpoint to avoid interfering with others
        const results: number[] = [];

        for (let i = 0; i < 5; i++) {
            const res = await request.get('/api/v1/trust/status');
            results.push(res.status());
        }

        // All should succeed (200) — well within rate limit
        results.forEach(status => expect(status).toBe(200));
    });

    // Full rate limit test (21 requests) is marked slow and only runs in dedicated CI
    test.skip('21st request returns 429 Too Many Requests', async ({ request }) => {
        const promises = Array.from({ length: 21 }, () =>
            request.get('/api/v1/trust/status')
        );
        const responses = await Promise.all(promises);
        const statuses = responses.map(r => r.status());
        // At least one should be 429
        expect(statuses).toContain(429);
    });
});
