import { defineConfig } from '@playwright/test';

/**
 * TrAI Playwright Configuration
 *
 * Environment variables:
 *   BASE_URL       Backend API base URL (default: http://localhost:8080)
 *   FRONTEND_URL   Frontend web URL     (default: http://localhost:3000)
 */
export default defineConfig({
    testDir: './',
    timeout: 60_000,       // 60s per test — AI calls can be slow
    retries: process.env.CI ? 1 : 0,
    reporter: [
        ['html', { open: 'never', outputFolder: 'playwright-report' }],
        ['list'],
    ],

    use: {
        // Default API context base URL
        baseURL: process.env.BASE_URL || 'http://localhost:8080',
        extraHTTPHeaders: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
        },
        trace: 'on-first-retry',
    },

    projects: [
        {
            name: 'API Tests',
            testDir: './api',
            use: {
                baseURL: process.env.BASE_URL || 'http://localhost:8080',
            },
        },
        {
            name: 'UI Tests (Chromium)',
            testDir: './ui',
            use: {
                browserName: 'chromium',
                baseURL: process.env.FRONTEND_URL || 'http://localhost:3000',
                viewport: { width: 1280, height: 800 },
                screenshot: 'only-on-failure',
            },
        },
    ],
});
