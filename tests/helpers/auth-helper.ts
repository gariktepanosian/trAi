import { APIRequestContext } from '@playwright/test';

/** Shared test user credentials */
export const TEST_USER = {
    username: 'playwright_testuser',
    password: 'PlaywrightTest123!',
    email: 'playwright@trai.test',
};

/**
 * Register the test user (idempotent — 409 conflict is fine) and return a JWT token.
 */
export async function getAuthToken(request: APIRequestContext): Promise<string> {
    // Try register (may already exist)
    await request.post('/api/v1/auth/register', {
        data: TEST_USER,
    });

    const loginRes = await request.post('/api/v1/auth/login', {
        data: {
            username: TEST_USER.username,
            password: TEST_USER.password,
        },
    });

    if (!loginRes.ok()) {
        throw new Error(`Login failed: ${loginRes.status()} ${await loginRes.text()}`);
    }

    const body = await loginRes.json();
    if (!body.token) {
        throw new Error(`No token in login response: ${JSON.stringify(body)}`);
    }
    return body.token as string;
}

/** Convenience: return Authorization header object */
export async function authHeaders(
    request: APIRequestContext
): Promise<{ Authorization: string }> {
    const token = await getAuthToken(request);
    return { Authorization: `Bearer ${token}` };
}
