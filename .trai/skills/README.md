# TrAI Skill System

This directory contains all **AI agent skill files** for the TrAI platform.

## How it works

When a developer (human or AI agent) needs to work on TrAI, they read `GATEWAY.md` first.
`GATEWAY.md` routes them to the correct skill file based on their task.

Each skill file contains:
- What this subsystem does and why
- Exact file paths and class names
- Coding patterns and conventions to follow
- What Playwright tests must cover after changes
- Common mistakes to avoid

## File map

| File | Covers |
|---|---|
| `GATEWAY.md` | **Start here** — routes any request to the right skill |
| `SKILL-backend-architecture.md` | Spring Boot structure, packages, naming, patterns |
| `SKILL-ai-engine.md` | AI model wiring (Grok, Gemini, ChatGPT), LangChain4j |
| `SKILL-multi-agent-pipeline.md` | Dual-agent parallel validation, AgentOutput, consensus |
| `SKILL-twitter-country-monitoring.md` | X (formerly Twitter) API v2, CountryMonitoringService, XUserRankingService, scheduler |
| `SKILL-push-notifications.md` | FCM, PushNotificationService, Android + iOS setup |
| `SKILL-flutter-mobile.md` | Flutter screens, services, state, pubspec conventions |
| `SKILL-security-auth.md` | JWT, Spring Security, rate limiting, input sanitizer |
| `SKILL-google-cloud-deployment.md` | Cloud Run, Artifact Registry, API Gateway, GCS, CI/CD |
| `SKILL-playwright-testing.md` | Playwright tests, coverage rules, AI test generation |
