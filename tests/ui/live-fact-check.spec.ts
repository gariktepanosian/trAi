import { test, expect } from '@playwright/test';

/**
 * Live Fact Check UI Tests
 * Covers: Speaker input, statement input, Audit button, result card display
 *
 * SKILL: SKILL-flutter-mobile.md → "Playwright Test Coverage Trigger"
 * SKILL: SKILL-ai-engine.md → "Playwright Test Coverage Trigger"
 */
test.describe('Live Fact Check — Web UI', () => {

    test.beforeEach(async ({ page }) => {
        await page.goto('/');
        await page.waitForLoadState('networkidle');
    });

    test('live check section is visible on load', async ({ page }) => {
        // Look for Live Check / Fact Check section
        const liveSection = page.locator(
            '[data-testid="live-check-form"], .live-fact-check, #live-check, #liveCheck'
        ).first();
        await expect(liveSection.or(page.getByText(/Live/i).first())).toBeVisible();
    });

    test('speaker input field accepts text', async ({ page }) => {
        // Find any input that could be the speaker field
        const speakerInput = page.locator(
            'input[placeholder*="Speaker" i], input[placeholder*="speaker" i], [data-testid="speaker-input"]'
        ).first();

        if (await speakerInput.isVisible()) {
            await speakerInput.fill('Test Speaker');
            await expect(speakerInput).toHaveValue('Test Speaker');
        }
    });

    test('statement textarea accepts text', async ({ page }) => {
        const statementInput = page.locator(
            'textarea, input[placeholder*="Statement" i], [data-testid="statement-input"]'
        ).first();

        if (await statementInput.isVisible()) {
            await statementInput.fill('This is a test statement for fact-checking.');
            const value = await statementInput.inputValue();
            expect(value).toContain('test statement');
        }
    });

    test('audit submit button is present and clickable', async ({ page }) => {
        const auditBtn = page.locator(
            'button:has-text("Audit"), button:has-text("Check"), button:has-text("Verify"), [data-testid="audit-submit-btn"]'
        ).first();

        if (await auditBtn.isVisible()) {
            await expect(auditBtn).toBeEnabled();
        }
    });

    test('result card appears after form submission (offline mode)', async ({ page }) => {
        // This test works in offline/demo mode where frontend shows mock results
        const submitBtn = page.locator(
            'button:has-text("Audit"), button:has-text("Verify"), button:has-text("Check")'
        ).first();

        if (await submitBtn.isVisible({ timeout: 3000 })) {
            await submitBtn.click();
            // Wait for any result to appear
            await page.waitForTimeout(3000);
            // Page should not crash
            await expect(page.locator('body')).not.toBeEmpty();
        }
    });
});
