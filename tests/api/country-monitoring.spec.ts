import { test, expect } from '@playwright/test';
import { authHeaders } from '../helpers/auth-helper';

/**
 * Country Subscription & Monitoring Tests
 * Covers: POST /api/v1/country/subscribe, /unsubscribe, GET /monitored, POST /trigger-check
 *
 * SKILL: SKILL-twitter-country-monitoring.md → "Playwright Test Coverage Trigger"
 */
test.describe('Country Subscribe — POST /api/v1/country/subscribe', () => {

    const TEST_FCM_TOKEN = 'playwright-test-fcm-token-' + Date.now();
    const TEST_COUNTRY = 'TestLand-' + Date.now(); // unique per run

    test('subscribe returns SUBSCRIBED status', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/country/subscribe', {
            headers,
            data: {
                country: TEST_COUNTRY,
                fcmToken: TEST_FCM_TOKEN,
            },
        });
        expect(res.status()).toBe(200);

        const body = await res.json();
        expect(body.status).toBe('SUBSCRIBED');
        expect(body.country).toBe(TEST_COUNTRY);
    });

    test('subscribe response includes subscriberCount', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/country/subscribe', {
            headers,
            data: { country: TEST_COUNTRY, fcmToken: TEST_FCM_TOKEN },
        });

        const body = await res.json();
        expect(typeof body.subscriberCount).toBe('number');
        expect(body.subscriberCount).toBeGreaterThanOrEqual(1);
    });

    test('subscribe without country returns 400', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/country/subscribe', {
            headers,
            data: { fcmToken: TEST_FCM_TOKEN },
        });
        expect(res.status()).toBe(400);
    });

    test('subscribe without fcmToken returns 400', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/country/subscribe', {
            headers,
            data: { country: 'Armenia' },
        });
        expect(res.status()).toBe(400);
    });
});

test.describe('Country Unsubscribe — POST /api/v1/country/unsubscribe', () => {

    test('unsubscribe returns UNSUBSCRIBED status', async ({ request }) => {
        const headers = await authHeaders(request);
        const country = 'UnsubTest-' + Date.now();
        const token = 'unsub-token-' + Date.now();

        // First subscribe
        await request.post('/api/v1/country/subscribe', {
            headers,
            data: { country, fcmToken: token },
        });

        // Then unsubscribe
        const res = await request.post('/api/v1/country/unsubscribe', {
            headers,
            data: { country, fcmToken: token },
        });
        expect(res.status()).toBe(200);

        const body = await res.json();
        expect(body.status).toBe('UNSUBSCRIBED');
        expect(body.country).toBe(country);
    });
});

test.describe('Monitored Countries — GET /api/v1/country/monitored', () => {

    test('returns list of monitored countries', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.get('/api/v1/country/monitored', { headers });

        expect(res.status()).toBe(200);
        const body = await res.json();
        expect(body.countries).toBeDefined();
        expect(typeof body.total).toBe('number');
    });

    test('subscribed country appears in monitored list', async ({ request }) => {
        const headers = await authHeaders(request);
        const uniqueCountry = 'MonitoredTestLand-' + Date.now();

        await request.post('/api/v1/country/subscribe', {
            headers,
            data: { country: uniqueCountry, fcmToken: 'monitored-test-token' },
        });

        const res = await request.get('/api/v1/country/monitored', { headers });
        const body = await res.json();

        expect(body.countries).toContain(uniqueCountry);
    });
});

test.describe('Country Trigger Check — POST /api/v1/country/trigger-check', () => {

    test('trigger-check returns monitoring result', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/country/trigger-check', {
            headers,
            data: { country: 'Armenia' },
        });
        expect(res.status()).toBe(200);

        const body = await res.json();
        expect(body.country).toBe('Armenia');
        expect(body.verdict).toBeDefined();
        expect(body.timestamp).toBeDefined();
    });

    test('trigger-check response includes tweetsAnalyzed field', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/country/trigger-check', {
            headers,
            data: { country: 'Japan' },
        });

        const body = await res.json();
        expect(typeof body.tweetsAnalyzed).toBe('number');
        expect(body.tweetsAnalyzed).toBeGreaterThanOrEqual(0);
    });

    test('trigger-check response includes criticalAlert boolean', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/country/trigger-check', {
            headers,
            data: { country: 'France' },
        });

        const body = await res.json();
        expect(typeof body.criticalAlert).toBe('boolean');
    });

    test('trigger-check response includes criticalLevel', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/country/trigger-check', {
            headers,
            data: { country: 'Germany' },
        });

        const body = await res.json();
        const validLevels = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
        expect(validLevels).toContain(body.criticalLevel);
    });

    test('trigger-check without country returns 400', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/country/trigger-check', {
            headers,
            data: {},
        });
        expect(res.status()).toBe(400);
    });

    test('trigger-check trustScore is 0-100', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/country/trigger-check', {
            headers,
            data: { country: 'Ukraine' },
        });

        const body = await res.json();
        if (typeof body.trustScore === 'number') {
            expect(body.trustScore).toBeGreaterThanOrEqual(0);
            expect(body.trustScore).toBeLessThanOrEqual(100);
        }
    });
});
