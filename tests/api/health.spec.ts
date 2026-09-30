import { test, expect } from '@playwright/test';

/**
 * Health Check Tests
 * Covers: GET /api/v1/trust/status
 *
 * SKILL: SKILL-backend-architecture.md → "Playwright Test Coverage Trigger"
 * SKILL: SKILL-google-cloud-deployment.md → "Playwright Test Coverage Trigger"
 */
test.describe('Health Check — GET /api/v1/trust/status', () => {

    test('returns 200 with OPERATIONAL status', async ({ request }) => {
        const res = await request.get('/api/v1/trust/status');
        expect(res.status()).toBe(200);

        const body = await res.json();
        expect(body.status).toBe('OPERATIONAL');
    });

    test('response includes application name and version', async ({ request }) => {
        const res = await request.get('/api/v1/trust/status');
        const body = await res.json();

        expect(body.application).toContain('TrAI');
        expect(body.version).toBe('2.0.0');
    });

    test('response lists exactly 3 AI models', async ({ request }) => {
        const res = await request.get('/api/v1/trust/status');
        const body = await res.json();

        expect(Array.isArray(body.aiModels)).toBeTruthy();
        expect(body.aiModels).toHaveLength(3);

        const modelNames = body.aiModels.join(' ');
        expect(modelNames).toContain('Grok');
        expect(modelNames).toContain('Gemini');
        expect(modelNames).toContain('GPT');
    });

    test('response shows Google Cloud as cloud provider', async ({ request }) => {
        const res = await request.get('/api/v1/trust/status');
        const body = await res.json();
        expect(body.cloudProvider).toContain('Google Cloud');
    });

    test('kill switch is inactive by default', async ({ request }) => {
        const res = await request.get('/api/v1/trust/status');
        const body = await res.json();
        expect(body.killSwitchActive).toBe(false);
    });

    test('features list includes push notifications and Twitter monitoring', async ({ request }) => {
        const res = await request.get('/api/v1/trust/status');
        const body = await res.json();

        const features: string[] = body.features;
        const featuresText = features.join(' ');
        expect(featuresText).toContain('Push Notification');
        expect(featuresText).toContain('Twitter');
        expect(featuresText).toContain('Multi-Agent');
    });

    test('pipeline field describes dual-agent architecture', async ({ request }) => {
        const res = await request.get('/api/v1/trust/status');
        const body = await res.json();
        expect(body.pipeline).toContain('Dual-Agent');
    });
});
