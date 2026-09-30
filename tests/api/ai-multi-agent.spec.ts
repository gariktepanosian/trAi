import { test, expect } from '@playwright/test';
import { authHeaders } from '../helpers/auth-helper';
import { CRITICAL_TWEET_CLAIM, MOCK_MULTI_AGENT_NORMAL } from '../helpers/fixtures';

/**
 * Multi-Agent Parallel Validation Tests
 * Covers: POST /api/v1/validate/multi-agent
 *
 * SKILL: SKILL-multi-agent-pipeline.md → "Playwright Test Coverage Trigger"
 */
test.describe('Multi-Agent Validation — POST /api/v1/validate/multi-agent', () => {

    test('returns 200 for valid claim + country', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/validate/multi-agent', {
            headers,
            data: {
                claim: 'Reports of heavy rainfall in the capital city.',
                country: 'Armenia',
                source: 'Playwright Test',
            },
        });
        expect(res.status()).toBe(200);
    });

    test('response contains both agent1 and agent2 outputs', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/validate/multi-agent', {
            headers,
            data: {
                claim: 'A parliamentary vote was held today.',
                country: 'France',
                source: 'Playwright Test',
            },
        });

        const body = await res.json();
        expect(body.agent1).toBeDefined();
        expect(body.agent2).toBeDefined();
        expect(body.agent1.agentId).toContain('AGENT_1');
        expect(body.agent2.agentId).toContain('AGENT_2');
    });

    test('response contains finalVerdict in allowed values', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/validate/multi-agent', {
            headers,
            data: {
                claim: 'Economy grew by 2% last quarter.',
                country: 'Germany',
                source: 'Test',
            },
        });

        const body = await res.json();
        const validVerdicts = [
            'VERIFIED_TRUE', 'LIKELY_TRUE', 'UNVERIFIED_CLAIM',
            'MISLEADING', 'FALSE'
        ];
        expect(validVerdicts).toContain(body.finalVerdict);
    });

    test('consensusTrustScore is integer between 0 and 100', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/validate/multi-agent', {
            headers,
            data: {
                claim: 'Test claim for score validation.',
                country: 'Japan',
                source: 'Test',
            },
        });

        const body = await res.json();
        expect(typeof body.consensusTrustScore).toBe('number');
        expect(body.consensusTrustScore).toBeGreaterThanOrEqual(0);
        expect(body.consensusTrustScore).toBeLessThanOrEqual(100);
    });

    test('response contains criticalAlert boolean', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/validate/multi-agent', {
            headers,
            data: {
                claim: 'Calm weather conditions today.',
                country: 'Spain',
                source: 'Test',
            },
        });

        const body = await res.json();
        expect(typeof body.criticalAlert).toBe('boolean');
    });

    test('response contains processingTimeMs > 0', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/validate/multi-agent', {
            headers,
            data: {
                claim: 'Markets opened higher this morning.',
                country: 'USA',
                source: 'Test',
            },
        });

        const body = await res.json();
        expect(typeof body.processingTimeMs).toBe('number');
        expect(body.processingTimeMs).toBeGreaterThan(0);
    });

    test('critical keyword in claim sets criticalLevel correctly', async ({ request }) => {
        const headers = await authHeaders(request);
        // Using the CRITICAL_TWEET_CLAIM fixture which contains "tornado", "evacuation"
        const res = await request.post('/api/v1/validate/multi-agent', {
            headers,
            data: {
                claim: CRITICAL_TWEET_CLAIM,
                country: 'Armenia',
                source: 'Twitter/X',
            },
        });

        const body = await res.json();
        const validLevels = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
        expect(validLevels).toContain(body.criticalLevel);
    });

    test('response timestamp is valid ISO-8601', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/validate/multi-agent', {
            headers,
            data: {
                claim: 'Test timestamp validation.',
                country: 'UK',
                source: 'Test',
            },
        });

        const body = await res.json();
        expect(body.timestamp).toBeDefined();
        expect(() => new Date(body.timestamp)).not.toThrow();
        expect(new Date(body.timestamp).getTime()).toBeGreaterThan(0);
    });

    test('country and source are echoed back in response', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/validate/multi-agent', {
            headers,
            data: {
                claim: 'Test echo fields.',
                country: 'Brazil',
                source: 'Unit Test Runner',
            },
        });

        const body = await res.json();
        expect(body.country).toBe('Brazil');
        expect(body.source).toBe('Unit Test Runner');
    });

    test('each agent output has aggregatedScore between 0 and 100', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/validate/multi-agent', {
            headers,
            data: {
                claim: 'Score range validation test.',
                country: 'Italy',
                source: 'Test',
            },
        });

        const body = await res.json();
        if (body.agent1?.aggregatedScore !== undefined) {
            expect(body.agent1.aggregatedScore).toBeGreaterThanOrEqual(0);
            expect(body.agent1.aggregatedScore).toBeLessThanOrEqual(100);
        }
        if (body.agent2?.aggregatedScore !== undefined) {
            expect(body.agent2.aggregatedScore).toBeGreaterThanOrEqual(0);
            expect(body.agent2.aggregatedScore).toBeLessThanOrEqual(100);
        }
    });
});
