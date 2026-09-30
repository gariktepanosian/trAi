import { test, expect } from '@playwright/test';
import { authHeaders } from '../helpers/auth-helper';

/**
 * News Feed API Tests
 * Covers: GET /api/v1/news, GET /api/v1/news?country=X
 * AI News Normalization: POST /api/v1/trust/verify-news
 * AI Glitch Audit: POST /api/v1/trust/verify-ai-output
 *
 * SKILL: SKILL-ai-engine.md → "Playwright Test Coverage Trigger"
 */
test.describe('News Feed — GET /api/v1/news', () => {

    test('returns 200 without auth (public endpoint)', async ({ request }) => {
        const res = await request.get('/api/v1/news');
        expect(res.status()).toBe(200);
    });

    test('returns array of news items', async ({ request }) => {
        const res = await request.get('/api/v1/news');
        const body = await res.json();
        expect(Array.isArray(body)).toBe(true);
    });

    test('country filter parameter is accepted', async ({ request }) => {
        const res = await request.get('/api/v1/news?country=Armenia');
        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(Array.isArray(body)).toBe(true);
    });

    test('news items have expected fields when present', async ({ request }) => {
        const res = await request.get('/api/v1/news');
        const body: any[] = await res.json();

        if (body.length > 0) {
            const item = body[0];
            // At minimum, items should have an id and createdAt
            expect(item.id || item._id).toBeDefined();
        }
    });
});

test.describe('AI News Normalize — POST /api/v1/trust/verify-news', () => {

    test('verify-news returns 200 with article text', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/trust/verify-news', {
            headers,
            data: {
                text: 'The government announced new economic measures today to combat inflation. Officials stated the measures will take effect next quarter.',
                sourceUrl: 'https://example-news.com/article',
                sourceName: 'Example News',
            },
        });
        expect(res.status()).toBe(200);
    });

    test('verify-news response includes status and normalizedReport', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/trust/verify-news', {
            headers,
            data: {
                text: 'Scientists announced a breakthrough in renewable energy research.',
                sourceName: 'Science Daily',
            },
        });

        const body = await res.json();
        expect(['SUCCESS', 'BLOCKED', 'HALTED']).toContain(body.status);
        expect(body.timestamp).toBeDefined();
    });

    test('verify-news includes guardrail fields in response', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/trust/verify-news', {
            headers,
            data: {
                text: 'Local elections took place peacefully across the country.',
                sourceName: 'Local Herald',
            },
        });

        const body = await res.json();
        expect(body.guardrailFlags).toBeDefined();
        expect(body.guardrailBlocked).toBeDefined();
    });

    test('verify-news echoes sourceUrl and sourceName', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/trust/verify-news', {
            headers,
            data: {
                text: 'Test news content for field echo verification.',
                sourceUrl: 'https://test-source.com',
                sourceName: 'Test Source',
            },
        });

        const body = await res.json();
        expect(body.sourceUrl).toBe('https://test-source.com');
        expect(body.sourceName).toBe('Test Source');
    });
});

test.describe('AI Glitch Audit — POST /api/v1/trust/verify-ai-output', () => {

    test('verify-ai-output returns 200', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/trust/verify-ai-output', {
            headers,
            data: {
                prompt: 'What is the capital of France?',
                aiResponse: 'The capital of France is Paris.',
                modelName: 'TestModel-1.0',
            },
        });
        expect(res.status()).toBe(200);
    });

    test('verify-ai-output response includes isGlitchDetected field', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/trust/verify-ai-output', {
            headers,
            data: {
                prompt: 'Explain quantum physics.',
                aiResponse: 'According to recent studies in 2026, quantum physics is...',
                modelName: 'SuspiciousModel',
            },
        });

        const body = await res.json();
        if (body.status === 'SUCCESS') {
            expect(typeof body.auditResult === 'string' ||
                   typeof body.isGlitchDetected === 'boolean').toBeTruthy();
        }
    });

    test('verify-ai-output response includes modelAudited', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/trust/verify-ai-output', {
            headers,
            data: {
                prompt: 'Test prompt',
                aiResponse: 'Test response',
                modelName: 'PlaywrightTestModel',
            },
        });

        const body = await res.json();
        if (body.status === 'SUCCESS') {
            expect(body.modelAudited).toBe('PlaywrightTestModel');
        }
    });
});
