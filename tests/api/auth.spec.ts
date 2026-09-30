import { test, expect } from '@playwright/test';
import { getAuthToken, authHeaders, TEST_USER } from '../helpers/auth-helper';

/**
 * Authentication Tests
 * Covers: POST /api/v1/auth/register, POST /api/v1/auth/login
 * Protected endpoint access with valid and invalid tokens.
 *
 * SKILL: SKILL-security-auth.md → "Playwright Test Coverage Trigger"
 */
test.describe('Authentication — Register + Login + JWT', () => {

    test('register new user returns 200 or 409 (idempotent)', async ({ request }) => {
        const res = await request.post('/api/v1/auth/register', {
            data: TEST_USER,
        });
        // 200 = registered, 409 = already exists — both are valid
        expect([200, 201, 409]).toContain(res.status());
    });

    test('login with valid credentials returns JWT token', async ({ request }) => {
        // Ensure user exists
        await request.post('/api/v1/auth/register', { data: TEST_USER });

        const res = await request.post('/api/v1/auth/login', {
            data: {
                username: TEST_USER.username,
                password: TEST_USER.password,
            },
        });
        expect(res.status()).toBe(200);

        const body = await res.json();
        expect(body.token).toBeDefined();
        expect(typeof body.token).toBe('string');
        expect(body.token.split('.').length).toBe(3); // JWT has 3 parts
    });

    test('login with wrong password returns 401', async ({ request }) => {
        const res = await request.post('/api/v1/auth/login', {
            data: {
                username: TEST_USER.username,
                password: 'WRONG_PASSWORD_xyz_123',
            },
        });
        expect(res.status()).toBe(401);
    });

    test('login with non-existent user returns 401', async ({ request }) => {
        const res = await request.post('/api/v1/auth/login', {
            data: {
                username: 'nonexistent_user_xyz_abc',
                password: 'anypassword',
            },
        });
        expect(res.status()).toBe(401);
    });

    test('protected endpoint requires JWT token', async ({ request }) => {
        // Without token — should return 401
        const res = await request.post('/api/v1/live/verify-statement', {
            data: { speaker: 'Test', statement: 'Test statement' },
            // No Authorization header
        });
        expect(res.status()).toBe(401);
    });

    test('protected endpoint accessible with valid JWT', async ({ request }) => {
        const headers = await authHeaders(request);
        const res = await request.post('/api/v1/live/verify-statement', {
            headers,
            data: {
                speaker: 'Test Speaker',
                statement: 'The bill was passed unanimously by Congress.',
            },
        });
        // Should succeed (200) — may use fallback if AI key not configured
        expect(res.status()).toBe(200);
    });

    test('invalid JWT token returns 401', async ({ request }) => {
        const res = await request.post('/api/v1/live/verify-statement', {
            headers: { Authorization: 'Bearer invalid.token.here' },
            data: { speaker: 'Test', statement: 'Test' },
        });
        expect(res.status()).toBe(401);
    });

    test('expired or tampered JWT returns 401', async ({ request }) => {
        // A structurally valid but unsigned/wrong-secret JWT
        const fakeToken =
            'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJoYWNrZXIifQ.FAKESIGNATURE';
        const res = await request.post('/api/v1/live/verify-statement', {
            headers: { Authorization: `Bearer ${fakeToken}` },
            data: { speaker: 'Test', statement: 'Test' },
        });
        expect(res.status()).toBe(401);
    });
});
