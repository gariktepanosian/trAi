# TrAI — User Guide & Platform Presentation

> **Your personal truth engine.** Real-time fact-checking, AI auditing, and propaganda-free news — all in one platform.

---

## What Is TrAI?

**TrAI** (Trust AI) is a platform that answers one simple question before you act on any piece of information:

**Is this actually true?**

Whether you are watching a live political speech, reading a news article, using an AI assistant at work, or tracking markets — TrAI verifies the information for you in seconds and tells you exactly what is confirmed, what is false, and what you need to know.

TrAI is built for journalists, researchers, analysts, business professionals, and anyone who needs to make decisions based on reliable information.

---

## Who Is TrAI For?

| User Type | How They Use TrAI |
|---|---|
| **Journalists & Editors** | Verify quotes, strip propaganda from wire articles, confirm source credibility before publishing |
| **Researchers & Academics** | Audit AI-generated content for hallucinations, check factual claims in literature |
| **Business Professionals** | Verify market-moving news before making trading or investment decisions |
| **News Consumers** | Understand whether what a politician just said is true, false, or misleading |
| **Enterprise AI Teams** | Audit internal LLM outputs before they reach customers |
| **Policy Analysts** | Get politically neutral, propaganda-stripped summaries of geopolitical events |
| **Students** | Verify facts from AI tools (ChatGPT, Gemini, Copilot) before submitting work |

---

## How to Access TrAI

TrAI is available on three platforms:

| Platform | How to Open | Best For |
|---|---|---|
| **Web App** | Open `index.html` in your browser, or visit the hosted URL | Full feature access on desktop |
| **Mobile App** | Install from the app store (iOS/Android) or open in browser | Quick fact-checks on the go |
| **API Integration** | Register as a B2B partner — connect TrAI directly to your system | Automated enterprise workflows |

### First-Time Setup (Web App)

1. Open the TrAI web application in any modern browser (Chrome, Firefox, Edge, Safari)
2. You will see the main dashboard with a dark interface and 9 feature tabs at the top
3. The status bar shows whether the platform is `OPERATIONAL` or in `Demo Mode`
4. **No account required** to try TrAI — you can use demo mode immediately
5. To save your history and access all features, click **Login / Register** in the top right

---

## The Dashboard — 9 Feature Tabs

The TrAI dashboard is organized into 9 tabs. Here is what each one does:

---

### Tab 1 — Live Speech Fact-Checker

**What it does:** Verifies spoken statements from politicians, executives, and public figures. Tells you exactly which claims are true, false, or misleading — with the evidence.

**How to use it:**

1. Type or paste a statement into the text box
   - *Example:* "We put 25 percent tariffs on foreign steel and our inflation dropped to zero"
2. Set the speaker name (e.g. "Donald Trump") and the source (e.g. "Live Campaign Rally")
3. Or click one of the **preset buttons** to load a ready-made example:
   - Donald Trump (Economic Claim)
   - Presidential Debate Statement
   - General Broadcast Example
4. Click **"Audit Live Statement"**
5. Results appear in 1–3 seconds

**What the results show:**

```
VERDICT: MISLEADING
Trust Score: 38 / 100
Speaker: Donald Trump
Source: Live Campaign Speech

CLAIM BREAKDOWN:
✅ VERIFIED  — "25% tariffs on foreign steel" — Section 232 tariffs imposed March 2018
❌ DEBUNKED  — "Inflation dropped to zero" — BLS CPI data shows 2.4% in 2018, 1.8% in 2019
❌ DEBUNKED  — "100% of manufacturing jobs" — BLS records +450,000 jobs, a fraction of total
```

**Verdict types explained:**
- `VERIFIED TRUE` — confirmed by independent empirical sources
- `FALSE` — directly contradicted by data
- `MISLEADING` — partially true but omits critical context
- `UNVERIFIED CLAIM` — not enough public data to confirm or deny

**When to use this:** Watching a live debate or press conference, or trying to verify a claim you just heard on the news.

---

### Tab 2 — AI Glitch & Hallucination Auditor

**What it does:** Checks whether an AI assistant (ChatGPT, Gemini, Copilot, or any LLM) gave you a fabricated, incorrect, or hallucinated answer.

**How to use it:**

1. In the **"Prompt"** field, type what you asked the AI
2. In the **"AI Response"** field, paste the AI's answer
3. Select the model name (e.g. "ChatGPT-4", "Gemini", "Copilot")
4. Or click a preset:
   - Fabricated Treaty Citation (Kyoto Protocol example)
   - Apollo Mission Date Error
5. Click **"Audit for Glitches & Hallucinations"**

**What the results show:**

```
Model Audited: ChatGPT-4
Glitch Detected: YES
Severity: HIGH
Reliability Score: 24 / 100
Safe to Publish: NO

Detected Hallucinations:
⚠️  "US Senate ratified the Kyoto Protocol" — INVENTED — Senate never ratified
⚠️  "Signed in Geneva" — INCORRECT — Signed in Kyoto, Japan
```

**Reliability Score:**
- 85–100: Safe to use, high confidence
- 60–84: Use with caution, minor issues possible
- 0–59: Do not publish or act on this — serious errors detected

**When to use this:** Before citing an AI answer in a report, article, legal document, or business decision. Also useful for teachers checking student AI-generated work.

---

### Tab 3 — News Propaganda Stripper

**What it does:** Takes a raw news article or media report and removes all emotional language, partisan framing, and propaganda rhetoric — leaving only verified, neutral facts.

**How to use it:**

1. Paste any news article text into the input box
2. Enter the source name (e.g. "State Media", "CNN", "Foreign Wire Service")
3. Optionally add the article URL
4. Click **"Strip Propaganda & Extract Facts"**

**What the results show:**

```
Source: State Media
Propaganda Score: 84 / 100

Flagged Phrases:
🚩 "heroic defense units" — emotionally charged military framing
🚩 "catastrophic annihilation" — propaganda amplification
🚩 "glorious liberation" — loaded political framing

Neutral Summary:
Military units engaged in combat operations in [region]. Casualties reported 
on both sides. Independent verification of territorial claims is pending.

Verified Facts:
✅ Armed conflict in [region] confirmed by UN observers on [date]

Unverified Claims:
❓ Casualty numbers — no independent confirmation
❓ Territorial control — conflicting reports from multiple sources
```

**Propaganda Score:**
- 0–30: Low bias, largely factual reporting
- 31–60: Moderate bias, some loaded language
- 61–80: High bias, significant framing issues
- 81–100: Extreme propaganda, treat all claims as unverified

**When to use this:** When reading news from any source you want to cross-check, or when you are comparing coverage of the same event from different outlets.

---

### Tab 4 — Source Credibility Index

**What it does:** Shows a real-time leaderboard of global media organizations ranked by their algorithmic credibility score — updated daily by TrAI's trust scoring engine.

**How to read the index:**

```
RANK  SOURCE          TRUST SCORE   TIER
  1   Reuters              94.5     TIER 1 — INSTITUTIONAL
  2   BBC                  91.0     TIER 1 — INSTITUTIONAL
  3   Associated Press     89.3     TIER 1 — INSTITUTIONAL
  4   The Guardian         81.7     TIER 2 — REPUTABLE
  5   RT (Russia Today)    41.2     TIER 4 — HIGH BIAS DETECTED
  6   Unnamed Blog         18.4     TIER 5 — DO NOT RELY
```

**Trust Score tiers:**
- **Tier 1 (85–100):** Institutional — primary source quality, consistently fact-based
- **Tier 2 (70–84):** Reputable — generally reliable with minor bias indicators
- **Tier 3 (50–69):** Use with caution — regular bias, verify independently
- **Tier 4 (25–49):** High bias — significant political or editorial slant detected
- **Tier 5 (0–24):** Do not rely — consistent misinformation, propaganda patterns

**When to use this:** Before deciding whether to trust or cite a news source.

---

### Tab 5 — Market Impact Analytics

**What it does:** After a breaking news event is verified by TrAI, the Market Impact Engine predicts the short-term direction of key financial assets.

**How to use it:**

1. Type a brief description of a verified event
   - *Example:* "US Federal Reserve raised interest rates by 50 basis points"
   - *Example:* "Major earthquake disrupts oil production in the Middle East"
2. Click **"Analyze Market Impact"**
3. Results show gauges for four asset classes

**What the results show:**

```
Event: "US Federal Reserve raised interest rates by 50bps"

GOLD        ↑ UP       78% confidence   "Rate hike drives safe-haven demand"
BTC         ↓ DOWN     61% confidence   "Tightening liquidity reduces risk appetite"
BRENT OIL   → NEUTRAL  55% confidence   "No direct energy policy impact expected"
USD         ↑ UP       82% confidence   "Higher rates strengthen dollar fundamentals"
Time Horizon: 24–72 hours
```

**Confidence levels:**
- 80–100%: High confidence — well-supported by historical analogues
- 60–79%: Moderate confidence — directional signal, not guaranteed
- Below 60%: Low confidence — too many variables, use as one signal among many

**Important:** TrAI market predictions are informational signals, not financial advice. Always conduct your own analysis before making investment decisions.

**When to use this:** When a major news event breaks and you want a rapid assessment of potential market impact before acting.

---

### Tab 6 — B2B Webhook Dashboard

**What it does:** Enterprise registration portal for organizations that want to connect their own systems to TrAI via the API.

**For individual users:** This tab shows your organization's integration status if you are accessing TrAI through an enterprise subscription.

**For business users:** Use this tab to:
1. Register your company as a TrAI B2B partner
2. Get your API credentials (Partner ID, API Key, Signing Secret)
3. Track the status of verification requests your system has submitted
4. View response times and processing history

**How to register:**
1. Enter your company name and technical contact email
2. Enter a callback URL where TrAI will send results
3. Click **"Register Partner"**
4. Save the credentials shown — the Signing Secret is only shown once

---

### Tab 7 — Incident & Alert Center

**What it does:** Shows real-time security and truth integrity alerts from the TrAI platform — including detected disinformation campaigns, AI hallucination surges, and system status updates.

**Alert severity levels:**

| Icon | Level | Meaning |
|---|---|---|
| 🔴 | CRITICAL | Active coordinated disinformation detected — immediate attention required |
| 🟠 | HIGH | Significant truth integrity issue — review recommended |
| 🔵 | INFO | Routine platform update or recalibration complete |

**Example alerts you might see:**

```
🔴 CRITICAL — Coordinated disinformation campaign detected
   12 correlated articles from 3 state-affiliated outlets pushing identical false narrative
   Topic: [Election / Economic / Military]
   Action: All related articles flagged with UNVERIFIED until independently confirmed

🟠 HIGH — AI hallucination surge in financial data feeds
   847 responses from a major LLM flagged with fabricated quarterly data
   Recommendation: Enable human review gate for financial AI outputs

🔵 INFO — Daily trust score recalibration complete
   243 sources updated | 7 tier changes applied
```

**When to use this:** Check this tab when you want to know about active disinformation campaigns or platform-wide issues that might affect your verification results.

---

### Tab 8 — Multi-Language Truth Feeds

**What it does:** Delivers verified, propaganda-stripped news summaries in three languages — English, Russian, and Armenian.

**How to use it:**
1. Select your preferred language from the dropdown (EN / RU / HY)
2. Browse the verified truth feed — each article has been processed through the propaganda stripper
3. Click any article to see the full neutral summary with flagged claims highlighted

**Language options:**
- **English (EN)** — Global and Western market coverage
- **Russian (RU)** — Russian-language news and Eastern European coverage
- **Armenian (HY)** — Armenian and Caucasus regional news

**What makes this different from regular news:** Every article in this feed has been algorithmically stripped of propaganda, verified for factual claims, and given a bias score. You see what actually happened, not how any particular outlet wants you to perceive it.

---

### Tab 9 — Pipeline Flow Builder (Visual Architecture)

**What it does:** Shows an interactive visual diagram of exactly how TrAI processes your data — from input to verified output.

**For curious users:** This tab lets you see and understand what happens to your data at each step:

```
[Your Input]
    ↓
[Input Sanitizer] — removes malicious code, masks personal data
    ↓
[Kill Switch Check] — is the system in emergency halt mode?
    ↓
[AI Engine (Grok-2)] — fact verification against knowledge base
    ↓
[Output Guardrails] — blocks toxic content, caps risk score
    ↓
[Audit Logger] — records the full call for compliance
    ↓
[Redis Cache] — stores result for instant retrieval next time
    ↓
[Your Result]
```

**Interactive features:**
- Click on any node to see what that component does
- Watch animated data packets flow through the pipeline in real time
- Understand what happens to your data at every step

---

## Authentication — Creating Your Account

### Why create an account?
- Save your verification history
- Access authenticated API endpoints
- Use the full feature set without demo limitations
- Track your usage and audit logs (business accounts)

### How to register:
1. Click **"Login / Register"** in the top right corner of the dashboard
2. Click **"Register"** tab
3. Enter your username, email, and password
4. Click **"Create Account"**
5. You will receive a JWT token — the app stores this automatically

### How to log in:
1. Click **"Login / Register"**
2. Enter your username and password
3. Click **"Login"**
4. Your session is maintained until you log out or the token expires (24 hours by default)

### Account roles:
- **User** — standard access to all 9 features
- **Business** — B2B webhook access + partner dashboard
- **Admin** — kill switch control + full audit log access

---

## Mobile App

The TrAI mobile app gives you fact-checking on the go.

**Available on:**
- iOS (iPhone and iPad)
- Android phones and tablets
- Web browser (mobile-optimized)

**Features on mobile:**
- Live Speech Fact-Checker — paste any statement and get instant results
- Source Trust Index — check any news source's credibility score
- Push notifications for CRITICAL alerts from the Incident Center
- Offline mode — basic heuristic fact-checking works even without internet

**How to use (Quick Start):**
1. Open the app
2. Tap **"Live Fact Check"** on the bottom navigation bar
3. Type or paste the statement you want to verify
4. Tap **"Check Now"**
5. Results appear with verdict, trust score, and claim breakdown

---

## Understanding Your Results

### Trust Score (0–100)

The Trust Score tells you how much confidence TrAI has in the truthfulness of a statement or source:

| Score | What it Means | What to Do |
|---|---|---|
| 90–100 | Highly verified | Safe to cite and act upon |
| 75–89 | Mostly true | Minor caveats — read the claim breakdown |
| 50–74 | Mixed | Verify independently before relying |
| 25–49 | Mostly false or misleading | Do not rely on this without full investigation |
| 0–24 | False or fabricated | Treat as false — do not publish or act on |

### Reliability Score (AI Auditor)

Specific to the AI Hallucination Auditor — measures how much you can trust the AI's response:

| Score | What it Means |
|---|---|
| 85–100 | Safe to use |
| 60–84 | Use with caution |
| 0–59 | Do not publish — serious errors found |

### Propaganda Score (News Normalizer)

Measures the bias level of a news article:

| Score | What it Means |
|---|---|
| 0–30 | Low bias |
| 31–60 | Moderate bias |
| 61–80 | High bias |
| 81–100 | Extreme propaganda |

---

## Privacy & Data Handling

**What TrAI does with your data:**

- Submitted statements are processed to generate verification results
- Input is automatically scanned for personal information (emails, credit card numbers) and masked before processing
- Outputs are scanned to prevent any personal data from appearing in responses
- Audit logs record the type and timestamp of your request — not the full content (for registered users)
- TrAI does not sell your data to third parties
- Cached results (Redis) are stored for 10 minutes, then deleted

**For enterprise customers:** Full audit trails are available via the `/api/v1/audit/logs` endpoint and are compliant with GDPR data handling requirements.

---

## Demo Mode vs. Live Mode

| | Demo Mode | Live Mode (Backend Connected) |
|---|---|---|
| **Requires setup** | No | Yes (backend running) |
| **AI responses** | Simulated (pre-written examples) | Real AI (xAI Grok-2 + Gemini) |
| **Response time** | Instant (no API call) | 1–5 seconds |
| **Accuracy** | Fixed examples only | Real-time verified results |
| **Account required** | No | Optional (no) |
| **Best for** | Demos, evaluations | Production use |

The status badge at the top of the dashboard shows which mode is active:
- `Backend: OPERATIONAL` — Live mode, real AI responses
- `Backend: Standalone Demo Mode` — Demo mode, simulated responses

---

## Frequently Asked Questions

**Q: Is TrAI always right?**
A: TrAI gives you the best available evidence-based assessment at the time of the query. Every result includes a confidence score so you know how certain the verdict is. For low-confidence results, TrAI will tell you explicitly — it never fabricates certainty.

**Q: What languages does TrAI support?**
A: The platform supports English, Russian, and Armenian. More languages can be added for enterprise clients.

**Q: Can TrAI fact-check in real-time during a live broadcast?**
A: Yes. Integrate TrAI with a speech-to-text service (Whisper, Google Speech API, or Deepgram), pipe the transcription to `POST /api/v1/live/verify-statement`, and receive verdicts within 1–3 seconds per statement.

**Q: What happens when TrAI is not sure?**
A: Uncertain results return a verdict of `UNVERIFIED_CLAIM` with a low trust score. TrAI never forces a false verdict when evidence is insufficient.

**Q: Is my data secure?**
A: Yes. All inputs are sanitized before processing. PII (personal information) is automatically masked at input and redacted at output. Sessions use JWT tokens with configurable expiry. All data is encrypted in transit.

**Q: Can TrAI be wrong?**
A: Like any AI system, TrAI can make errors — particularly for very recent events not yet in its knowledge base, or for highly ambiguous claims. The trust score and `UNVERIFIED_CLAIM` verdict type exist precisely to communicate uncertainty. Always use TrAI as one input to your decision, not the only input.

**Q: How do I integrate TrAI into my company's system?**
A: Register as a B2B partner in Tab 6 to get your API credentials. The webhook API accepts POST requests with a JSON payload — see the Integration Guide or contact enterprise support for a dedicated onboarding session.

---

## Quick Reference Card

```
FEATURE                    TAB    WHAT IT DOES
─────────────────────────────────────────────────────────────────────
Live Speech Fact-Checker     1    Verifies spoken political/public statements
AI Hallucination Auditor     2    Detects AI fabrications in LLM responses
News Propaganda Stripper     3    Removes bias, returns neutral facts
Source Credibility Index     4    Media organization trust rankings
Market Impact Analytics      5    Predicts asset direction from verified events
B2B Webhook Dashboard        6    Enterprise API registration and tracking
Incident & Alert Center      7    Real-time disinformation and system alerts
Multi-Language Truth Feeds   8    Verified news in EN / RU / HY
Pipeline Flow Builder        9    Visual diagram of how your data is processed

TRUST SCORE GUIDE
──────────────────────────────
90–100   Highly verified — safe to cite
75–89    Mostly true — read the claim breakdown
50–74    Mixed — verify independently
25–49    Mostly false — do not rely without investigation
0–24     False or fabricated — do not use

VERDICT TYPES
──────────────────────────────
VERIFIED TRUE     Confirmed by ≥ 2 independent sources
FALSE             Directly contradicted by empirical data
MISLEADING        Partially true, omits critical context
UNVERIFIED CLAIM  Insufficient data — handle with caution
```
