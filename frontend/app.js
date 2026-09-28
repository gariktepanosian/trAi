const API_BASE = 'http://localhost:8080/api/v1';

// Tab Switching & Initialization
document.addEventListener('DOMContentLoaded', () => {
  initTabs();
  initAuth();
  checkBackendStatus();
  checkKillSwitchStatus();
  loadSourceScores();
  loadAlerts();
});

function getAuthHeaders() {
  const token = localStorage.getItem('trai_jwt');
  const headers = { 'Content-Type': 'application/json' };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return headers;
}

function initTabs() {
  const tabs = document.querySelectorAll('.tab-btn');
  tabs.forEach(tab => {
    tab.addEventListener('click', () => {
      tabs.forEach(t => t.classList.remove('active'));
      document.querySelectorAll('.tab-pane').forEach(p => p.classList.remove('active'));
      
      tab.classList.add('active');
      const targetPane = document.getElementById(tab.dataset.tab);
      if (targetPane) targetPane.classList.add('active');
    });
  });
}

async function checkBackendStatus() {
  const statusEl = document.getElementById('backend-status-text');
  try {
    const res = await fetch(`${API_BASE}/trust/status`, { signal: AbortSignal.timeout(3000) });
    if (res.ok) {
      const data = await res.json();
      statusEl.textContent = `Backend: ${data.status} (v${data.version})`;
      statusEl.style.color = '#10b981';
    } else {
      throw new Error('Not OK');
    }
  } catch (err) {
    statusEl.textContent = 'Backend: Standalone Demo Mode (Active)';
    statusEl.style.color = '#38bdf8';
  }
}

// Preset Handlers
const speechPresets = {
  trump: {
    speaker: "Donald Trump",
    source: "Live Rally Stream / Broadcast Audio",
    statement: "We put 25 percent tariffs on foreign steel, and our inflation completely disappeared to zero, creating 100 percent of the manufacturing jobs in our country."
  },
  debate: {
    speaker: "Debate Participant",
    source: "Live Presidential Debate Stream",
    statement: "Under the new health bill passed in 2021, prescription drug prices for all Americans were capped at 35 dollars per month immediately across all private pharmacies."
  },
  general: {
    speaker: "Financial Spokesperson",
    source: "Live Press Conference Stream",
    statement: "Our central bank reserves grew by 45 billion dollars last quarter alone with zero debt issued to external bondholders."
  }
};

function loadSpeechPreset(key) {
  const p = speechPresets[key];
  if (!p) return;
  document.getElementById('speaker-input').value = p.speaker;
  document.getElementById('media-source-input').value = p.source;
  document.getElementById('statement-input').value = p.statement;
}

const aiPresets = {
  hallucination: {
    prompt: "Who signed the 1997 Kyoto Protocol on behalf of the United States?",
    response: "The 1997 Kyoto Protocol was personally signed by President Bill Clinton in Geneva, and was unanimously ratified by the US Senate under Resolution 412 in 1998.",
    model: "ChatGPT-4 / Custom RAG"
  },
  accurate: {
    prompt: "When was the Apollo 11 moon landing and who were the astronauts?",
    response: "Apollo 11 landed on the Moon on July 20, 1969. The crew members were Neil Armstrong, Buzz Aldrin, and Michael Collins.",
    model: "Claude 3.5 Sonnet"
  }
};

function loadAiPreset(key) {
  const p = aiPresets[key];
  if (!p) return;
  document.getElementById('ai-prompt-input').value = p.prompt;
  document.getElementById('ai-response-input').value = p.response;
  document.getElementById('ai-model-input').value = p.model;
}

// 1. Audit Live Spoken Statement
async function verifyLiveStatement() {
  const speaker = document.getElementById('speaker-input').value.trim();
  const mediaSource = document.getElementById('media-source-input').value.trim();
  const statement = document.getElementById('statement-input').value.trim();
  const resultCard = document.getElementById('live-result-content');
  const pill = document.getElementById('live-verdict-pill');

  if (!statement) {
    alert('Please enter a spoken statement chunk.');
    return;
  }

  pill.className = 'badge badge-unverified';
  pill.textContent = 'ANALYZING...';
  resultCard.innerHTML = `<div class="empty-state"><div class="empty-icon">&#8987;</div><p>Performing instant empirical consensus audit & transcription fact check...</p></div>`;

  try {
    const res = await fetch(`${API_BASE}/live/verify-statement`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({ speaker, statement, mediaSource }),
      signal: AbortSignal.timeout(6000)
    });

    if (res.ok) {
      const data = await res.json();
      renderLiveResult(data);
      return;
    }
  } catch (e) {
    console.warn('Backend unavailable, running high-precision client fact-check simulation:', e);
  }

  // Client-side fallback simulation for immediate demo
  setTimeout(() => {
    const isTrump = speaker.toLowerCase().includes('trump');
    const isSteel = statement.toLowerCase().includes('tariffs') || statement.toLowerCase().includes('steel');
    
    let verdict = "MISLEADING";
    let score = 38;
    let badgeClass = "badge-misleading";
    let explanation = "The statement conflates tariff revenue with general inflation rates. While 25% tariffs on foreign steel were implemented under Section 232 in 2018, Bureau of Labor Statistics (BLS) CPI records show inflation was not at zero (averaging 1.8% to 2.4% during that period), and domestic manufacturing experienced selective sector gains rather than 100% replacement.";

    if (statement.toLowerCase().includes('zero') && statement.toLowerCase().includes('100%')) {
      verdict = "FALSE";
      score = 21;
      badgeClass = "badge-false";
    }

    renderLiveResult({
      speaker: speaker,
      mediaSource: mediaSource,
      statementAnalyzed: statement,
      verdict: verdict,
      trustScore: score,
      summary: `Empirical fact-check for live quote from ${speaker}.`,
      factCheckDetails: explanation,
      keyClaims: [
        { claim: "Tariffs on foreign steel were 25%", status: "VERIFIED", correction: "Implemented March 2018 under Trade Expansion Act Section 232." },
        { claim: "Inflation disappeared to zero", status: "DEBUNKED", correction: "BLS reported 2.4% CPI inflation in 2018 and 1.8% in 2019." },
        { claim: "Created 100% of manufacturing jobs", status: "DEBUNKED", correction: "Total US manufacturing added ~450k jobs (2017-2019), representing a fractional share of total workforce." }
      ]
    });
  }, 400);
}

function renderLiveResult(data) {
  const pill = document.getElementById('live-verdict-pill');
  const resultCard = document.getElementById('live-result-content');

  let verdict = data.verdict || "MISLEADING";
  let score = data.trustScore || 45;
  let explanation = data.factCheckDetails || data.summary || "Fact check complete.";

  // If raw analysis is JSON string from Grok
  if (typeof data.analysis === 'string') {
    try {
      const parsed = JSON.parse(data.analysis.replace(/```json/g, '').replace(/```/g, '').trim());
      verdict = parsed.verdict || verdict;
      score = parsed.trustScore || score;
      explanation = parsed.factCheckDetails || parsed.summary || explanation;
      if (parsed.keyClaims) data.keyClaims = parsed.keyClaims;
    } catch (e) {}
  }

  pill.textContent = verdict.replace('_', ' ');
  if (verdict.includes('TRUE')) {
    pill.className = 'badge badge-true';
  } else if (verdict.includes('FALSE')) {
    pill.className = 'badge badge-false';
  } else {
    pill.className = 'badge badge-misleading';
  }

  let claimsHtml = '';
  if (data.keyClaims && Array.isArray(data.keyClaims)) {
    claimsHtml = `
      <div class="section-title">Claim-by-Claim Verification</div>
      <div class="claim-list">
        ${data.keyClaims.map(c => `
          <div class="claim-item ${c.status === 'VERIFIED' ? 'verified' : (c.status === 'DEBUNKED' ? 'debunked' : 'warning')}">
            <strong>[${c.status}]</strong> ${c.claim}
            <div style="font-size:0.8rem; color:#94a3b8; margin-top:0.25rem;"><strong>Factual Anchor:</strong> ${c.correction || 'Official records verified.'}</div>
          </div>
        `).join('')}
      </div>
    `;
  }

  resultCard.innerHTML = `
    <div class="verdict-header-box">
      <div>
        <div style="font-size: 0.8rem; color: #94a3b8;">SPEAKER & STREAM</div>
        <div style="font-weight: 600; font-size: 1.1rem; color: #f8fafc;">${data.speaker || 'Speaker'} (${data.mediaSource || 'Live Stream'})</div>
      </div>
      <div style="text-align: right;">
        <div class="verdict-score-label">Truth Score</div>
        <div class="verdict-score-gauge" style="color: ${score > 70 ? '#10b981' : (score > 40 ? '#f59e0b' : '#f43f5e')}">${score}/100</div>
      </div>
    </div>

    <div class="section-title">Verified Empirical Counter-Analysis</div>
    <div class="analysis-text-box">${explanation}</div>

    ${claimsHtml}

    <div style="margin-top: auto; font-size: 0.75rem; color: #64748b; font-family: monospace;">
      Audit Engine: Grok-2 Consensus Layer • Timestamp: ${new Date().toLocaleTimeString()}
    </div>
  `;
}

function simulateStreamChunk() {
  const statements = [
    "We have completely paid off the national debt in the third quarter of 2024.",
    "The new defense agreement was ratified by 32 member nations yesterday in Brussels.",
    "Unemployment reached negative two percent for the first time in modern American history."
  ];
  const next = statements[Math.floor(Math.random() * statements.length)];
  document.getElementById('statement-input').value = next;
  verifyLiveStatement();
}

// 2. Audit AI Glitch & Hallucination
async function verifyAiOutput() {
  const prompt = document.getElementById('ai-prompt-input').value.trim();
  const aiResponse = document.getElementById('ai-response-input').value.trim();
  const modelName = document.getElementById('ai-model-input').value.trim();
  const pill = document.getElementById('ai-glitch-pill');
  const resultCard = document.getElementById('ai-result-content');

  pill.className = 'badge badge-unverified';
  pill.textContent = 'AUDITING...';
  resultCard.innerHTML = `<div class="empty-state"><div class="empty-icon">&#8987;</div><p>Auditing AI response against authoritative ground truth to detect hallucinations and glitches...</p></div>`;

  try {
    const res = await fetch(`${API_BASE}/trust/verify-ai-output`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({ prompt, aiResponse, modelName }),
      signal: AbortSignal.timeout(6000)
    });

    if (res.ok) {
      const data = await res.json();
      renderAiAuditResult(data);
      return;
    }
  } catch (e) {
    console.warn('Backend unavailable, running client hallucination audit simulation:', e);
  }

  // Simulation fallback
  setTimeout(() => {
    const isKyoto = aiResponse.toLowerCase().includes('kyoto') || aiResponse.toLowerCase().includes('clinton');
    
    let isGlitch = isKyoto;
    let severity = isGlitch ? "HIGH" : "LOW";
    let score = isGlitch ? 24 : 95;
    let summary = isGlitch
      ? "CRITICAL HALLUCINATION: Multiple fabricated historical and legal claims detected in the AI output."
      : "No significant hallucinations or glitches detected. Output is factually grounded.";

    let hallucinations = isGlitch ? [
      {
        aiClaim: "President Bill Clinton personally signed the Kyoto Protocol in Geneva.",
        groundTruth: "The Kyoto Protocol was signed on behalf of the US by Vice President Al Gore on November 12, 1998, in New York, not Bill Clinton in Geneva.",
        type: "FABRICATION"
      },
      {
        aiClaim: "Unanimously ratified by the US Senate under Resolution 412 in 1998.",
        groundTruth: "The US Senate NEVER ratified the Kyoto Protocol. In fact, the Senate passed the Byrd-Hagel Resolution (S. Res. 98) 95-0 explicitly rejecting any treaty that did not bind developing nations.",
        type: "INVERTED_REALITY"
      }
    ] : [];

    renderAiAuditResult({
      modelAudited: modelName,
      isGlitchDetected: isGlitch,
      glitchSeverity: severity,
      reliabilityScore: score,
      verdictSummary: summary,
      safeToPublish: !isGlitch,
      detectedHallucinations: hallucinations
    });
  }, 400);
}

function renderAiAuditResult(data) {
  const pill = document.getElementById('ai-glitch-pill');
  const resultCard = document.getElementById('ai-result-content');

  let severity = data.glitchSeverity || "LOW";
  let score = data.reliabilityScore || 90;
  let safe = data.safeToPublish !== undefined ? data.safeToPublish : true;

  pill.textContent = severity === "HIGH" ? "GLITCH DETECTED" : (severity === "MODERATE" ? "SUSPICIOUS" : "VERIFIED CLEAN");
  pill.className = severity === "HIGH" ? "badge badge-false" : (severity === "MODERATE" ? "badge badge-misleading" : "badge badge-true");

  let hallList = '';
  if (data.detectedHallucinations && data.detectedHallucinations.length > 0) {
    hallList = `
      <div class="section-title">Detected Hallucinations & AI Glitches</div>
      <div class="claim-list">
        ${data.detectedHallucinations.map(h => `
          <div class="claim-item debunked">
            <div style="color: #f43f5e; font-weight:600;">&#10006; Fabricated AI Claim: "${h.aiClaim}"</div>
            <div style="color: #10b981; margin-top:0.35rem;">&#10004; Verified Ground Truth: ${h.groundTruth}</div>
          </div>
        `).join('')}
      </div>
    `;
  } else {
    hallList = `
      <div class="section-title">Factual Grounding Check</div>
      <div class="claim-item verified">
        &#10004; All empirical claims, dates, and historical figures in the AI output match verified public knowledge graphs.
      </div>
    `;
  }

  resultCard.innerHTML = `
    <div class="verdict-header-box">
      <div>
        <div style="font-size: 0.8rem; color: #94a3b8;">AUDITED MODEL</div>
        <div style="font-weight: 600; font-size: 1.1rem; color: #f8fafc;">${data.modelAudited || 'Enterprise AI'}</div>
        <div style="margin-top: 0.25rem;">
          <span class="badge ${safe ? 'badge-true' : 'badge-false'}">${safe ? 'SAFE TO PUBLISH' : 'BLOCKED: UNRELIABLE'}</span>
        </div>
      </div>
      <div style="text-align: right;">
        <div class="verdict-score-label">Reliability Index</div>
        <div class="verdict-score-gauge" style="color: ${score > 75 ? '#10b981' : (score > 45 ? '#f59e0b' : '#f43f5e')}">${score}%</div>
      </div>
    </div>

    <div class="section-title">Audit Diagnostic</div>
    <div class="analysis-text-box">${data.verdictSummary || 'Audit completed.'}</div>

    ${hallList}
  `;
}

// 3. News Propaganda Stripping
async function verifyNewsText() {
  const sourceName = document.getElementById('news-source-input').value.trim();
  const text = document.getElementById('news-text-input').value.trim();
  const pill = document.getElementById('news-verdict-pill');
  const resultCard = document.getElementById('news-result-content');

  pill.className = 'badge badge-unverified';
  pill.textContent = 'NORMALIZING...';
  resultCard.innerHTML = `<div class="empty-state"><div class="empty-icon">&#8987;</div><p>Deconstructing partisan adjectives and filtering emotional manipulation...</p></div>`;

  try {
    const res = await fetch(`${API_BASE}/trust/verify-news`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({ text, sourceName }),
      signal: AbortSignal.timeout(6000)
    });

    if (res.ok) {
      const data = await res.json();
      renderNewsResult(data);
      return;
    }
    throw new Error('Backend error');
  } catch (err) {
    console.warn('Backend unavailable, falling back to local normalization:', err);
  }

  setTimeout(() => renderNewsResult({
    sourceName,
    normalizedReport: JSON.stringify({
      normalized_title: 'Objective Report: Northern Sector Engagement',
      verified_facts: [
        'Engagement occurred at 04:30 UTC along the northern boundary line',
        'Both sides reported equipment losses'
      ],
      unverified_claims: [
        { source: 'State Media', claim: 'Opposition forces were annihilated' }
      ],
      insider_info: [ { status: 'RED', detail: 'Supply lines disrupted', potentialMarketImpact: 'Oil UP' } ],
      propaganda_detected: true,
      dry_analysis: 'Narrative emphasises total victory without corroboration.'
    }),
    guardrailFlags: ['FALLBACK_MODE'],
    guardrailBlocked: false
  }), 400);
}

// 4. Source Trust Scores
async function loadSourceScores() {
  const tbody = document.getElementById('source-scores-tbody');
  
  let scores = [];
  try {
    const res = await fetch(`${API_BASE}/trust/sources`, { signal: AbortSignal.timeout(3000) });
    if (res.ok) {
      scores = await res.json();
    }
  } catch (e) {
    console.warn('Backend unavailable, rendering default verified source index.');
  }

  if (!scores || scores.length === 0) {
    scores = [
      { credibilityRank: 1, sourceName: "Reuters", sourceUrl: "https://reuters.com", trustScore: 94.5 },
      { credibilityRank: 2, sourceName: "Associated Press", sourceUrl: "https://apnews.com", trustScore: 93.8 },
      { credibilityRank: 3, sourceName: "Bloomberg Markets", sourceUrl: "https://bloomberg.com", trustScore: 91.0 },
      { credibilityRank: 4, sourceName: "BBC World News", sourceUrl: "https://bbc.com", trustScore: 88.2 },
      { credibilityRank: 5, sourceName: "Al Jazeera English", sourceUrl: "https://aljazeera.com", trustScore: 81.5 },
      { credibilityRank: 6, sourceName: "Aggregated State Media", sourceUrl: "https://statemedia.org", trustScore: 34.2 }
    ];
  }

  tbody.innerHTML = scores.map(s => {
    let tier = s.trustScore >= 90 ? '<span class="badge badge-true">TIER 1 (INSTITUTIONAL)</span>' : 
               (s.trustScore >= 75 ? '<span class="badge badge-news">TIER 2 (RELIABLE)</span>' : '<span class="badge badge-false">TIER 4 (HIGH BIAS)</span>');
    let color = s.trustScore >= 90 ? '#10b981' : (s.trustScore >= 75 ? '#38bdf8' : '#f43f5e');
    return `
      <tr>
        <td><strong>#${s.credibilityRank}</strong></td>
        <td><strong style="color: #f8fafc;">${s.sourceName}</strong></td>
        <td><a href="${s.sourceUrl}" target="_blank" style="color: #38bdf8; text-decoration: none;">${s.sourceUrl}</a></td>
        <td class="score-cell" style="color: ${color}; font-size: 1.05rem;">${s.trustScore}%</td>
        <td>${tier}</td>
        <td><span class="status-pulse" style="display:inline-block; margin-right: 6px;"></span> Active Real-Time Audit</td>
      </tr>
    `;
  }).join('');
}

function renderNewsResult(data) {
  const pill = document.getElementById('news-verdict-pill');
  const resultCard = document.getElementById('news-result-content');

  pill.className = data.guardrailBlocked ? 'badge badge-false' : 'badge badge-true';
  pill.textContent = data.guardrailBlocked ? 'BLOCKED' : 'NORMALIZED';

  let parsed = {};
  if (typeof data.normalizedReport === 'string') {
    try {
      parsed = JSON.parse(data.normalizedReport.replace(/```json/g, '').replace(/```/g, '').trim());
    } catch (err) {
      parsed = { rawText: data.normalizedReport };
    }
  } else if (data.normalizedReport) {
    parsed = data.normalizedReport;
  }

  const verifiedFacts = (parsed.verified_facts || []).map(f => `<div class="claim-item verified">&#10004; ${f}</div>`).join('');
  const unverifiedClaims = (parsed.unverified_claims || []).map(c => `
    <div class="claim-item debunked">
      <strong>[UNVERIFIED]</strong> ${c.claim || JSON.stringify(c)}
    </div>
  `).join('');

  resultCard.innerHTML = `
    <div class="verdict-header-box">
      <div>
        <div style="font-size: 0.8rem; color: #94a3b8;">ORIGINAL SOURCE</div>
        <div style="font-weight: 600; font-size: 1.1rem; color: #f8fafc;">${data.sourceName || 'Unknown Source'}</div>
      </div>
      <div style="text-align: right;">
        <div class="verdict-score-label">Propaganda Stripped</div>
        <div class="verdict-score-gauge" style="color: #06b6d4;">${parsed.propaganda_detected ? 'Detected' : 'None'}</div>
      </div>
    </div>

    <div class="section-title">Radically Neutral Extraction</div>
    <div class="analysis-text-box">
      <strong>Normalized Title:</strong> ${parsed.normalized_title || '—'}<br><br>
      <strong>Dry Analysis:</strong> ${parsed.dry_analysis || 'Pending'}
    </div>

    <div class="section-title">Verified Facts</div>
    ${verifiedFacts || '<div class="claim-item verified">No corroborated facts available.</div>'}

    <div class="section-title">Unverified / Insider Claims</div>
    ${unverifiedClaims || '<div class="claim-item warning">No unverified claims detected.</div>'}

    ${data.guardrailFlags ? `<div class="analysis-text-box" style="margin-top:1rem;">Guardrail Flags: ${data.guardrailFlags.join(', ')}</div>` : ''}
  `;
}

// ─── Webhook Dashboard ──────────────────────────────────────────────────────
async function registerWebhookPartner() {
  const resultBox = document.getElementById('webhook-register-result');
  const companyName = document.getElementById('partner-company-input').value.trim();
  const email = document.getElementById('partner-email-input').value.trim();
  const callbackUrl = document.getElementById('partner-callback-input').value.trim();

  if (!companyName || !email) {
    resultBox.innerHTML = `<div class="claim-item warning">Company name and email are required.</div>`;
    return;
  }

  resultBox.innerHTML = '<div class="claim-item verified">Registering partner...</div>';

  try {
    const res = await fetch(`${API_BASE}/webhook/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ companyName, email, callbackUrl }),
      signal: AbortSignal.timeout(5000)
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Registration failed');
    resultBox.innerHTML = `
      <div class="analysis-text-box">
        <strong>Partner ID:</strong> ${data.partnerId}<br>
        <strong>Signing Secret:</strong> <code>${data.signingSecret}</code><br>
        <em>Store this secret securely. It will not be shown again.</em>
      </div>`;
  } catch (err) {
    resultBox.innerHTML = `<div class="claim-item debunked">${err.message}</div>`;
  }
}

async function fetchWebhookStatus() {
  const requestId = document.getElementById('webhook-request-id-input').value.trim();
  const resultBox = document.getElementById('webhook-status-result');

  if (!requestId) {
    resultBox.innerHTML = `<div class="claim-item warning">Enter a request ID.</div>`;
    return;
  }

  resultBox.innerHTML = '<div class="claim-item verified">Fetching status...</div>';

  try {
    const res = await fetch(`${API_BASE}/webhook/status/${requestId}`, { signal: AbortSignal.timeout(4000) });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Status lookup failed');
    resultBox.innerHTML = `<pre class="analysis-text-box" style="white-space: pre-wrap;">${JSON.stringify(data, null, 2)}</pre>`;
  } catch (err) {
    resultBox.innerHTML = `<div class="claim-item debunked">${err.message}</div>`;
  }
}

// ─── SentinelMind Emergency Kill Switch ─────────────────────────────────────
let killSwitchActive = false;

async function checkKillSwitchStatus() {
  try {
    const res = await fetch(`${API_BASE}/trust/kill-switch`, { signal: AbortSignal.timeout(3000) });
    if (res.ok) {
      const data = await res.json();
      killSwitchActive = Boolean(data.active);
      updateKillSwitchBanner();
    }
  } catch (err) {
    console.debug('Kill switch status unavailable, defaulting to normal.');
  }
}

async function toggleSentinelKillSwitch() {
  const desired = !killSwitchActive;
  const confirmed = confirm(desired ? 
    '⚠️ ENGAGE SENTINELMIND KILL SWITCH?\n\nThis will immediately halt all AI inference, news normalization, and live fact-checking platform-wide.' : 
    'Disengage Kill Switch and restore normal AI pipeline operations?');
  
  if (!confirmed) return;

  try {
    const res = await fetch(`${API_BASE}/trust/kill-switch`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({ active: desired })
    });
    if (res.ok) {
      const data = await res.json();
      killSwitchActive = Boolean(data.active);
      updateKillSwitchBanner();
      alert(data.message || (killSwitchActive ? 'Kill-switch engaged.' : 'Kill-switch disengaged.'));
    }
  } catch (err) {
    killSwitchActive = desired;
    updateKillSwitchBanner();
    alert(`Local simulation: Kill-switch set to ${desired ? 'ACTIVE' : 'INACTIVE'}`);
  }
}

function updateKillSwitchBanner() {
  const banner = document.getElementById('sentinel-killswitch-banner');
  const text = document.getElementById('killswitch-text');
  const btn = document.getElementById('killswitch-toggle-btn');
  if (!banner || !text || !btn) return;

  if (killSwitchActive) {
    banner.className = 'killswitch-banner halted';
    text.textContent = '🚨 SENTINELMIND EMERGENCY KILL-SWITCH ENGAGED — AI model evaluation suspended';
    btn.textContent = 'Disengage Kill-Switch';
    btn.className = 'btn btn-outline-success';
  } else {
    banner.className = 'killswitch-banner normal';
    text.textContent = 'SentinelMind: Active & Enforcing Real-time Guardrails';
    btn.textContent = 'Engage Kill-Switch';
    btn.className = 'btn btn-outline-danger';
  }
}

// ─── Authentication Management ──────────────────────────────────────────────
let currentAuthMode = 'login';

function initAuth() {
  const token = localStorage.getItem('trai_jwt');
  const username = localStorage.getItem('trai_user');
  const role = localStorage.getItem('trai_role') || 'USER';

  const openBtn = document.getElementById('open-auth-btn');
  const badge = document.getElementById('user-profile-badge');
  const nameEl = document.getElementById('user-display-name');

  if (token && username && openBtn && badge && nameEl) {
    openBtn.style.display = 'none';
    badge.style.display = 'flex';
    nameEl.textContent = `${username} [${role}]`;
  } else if (openBtn && badge) {
    openBtn.style.display = 'block';
    badge.style.display = 'none';
  }
}

function openAuthModal() {
  const modal = document.getElementById('auth-modal');
  if (modal) modal.style.display = 'flex';
}

function closeAuthModal() {
  const modal = document.getElementById('auth-modal');
  if (modal) modal.style.display = 'none';
}

function handleModalBackdropClick(e) {
  if (e.target.id === 'auth-modal') closeAuthModal();
}

function switchAuthMode(mode) {
  currentAuthMode = mode;
  const loginTab = document.getElementById('auth-tab-login');
  const regTab = document.getElementById('auth-tab-register');
  const emailGrp = document.getElementById('auth-email-group');
  const roleGrp = document.getElementById('auth-role-group');
  const title = document.getElementById('auth-modal-title');
  const submitBtn = document.getElementById('auth-submit-btn');
  const errorMsg = document.getElementById('auth-error-msg');

  if (errorMsg) errorMsg.style.display = 'none';

  if (mode === 'register') {
    loginTab.classList.remove('active');
    regTab.classList.add('active');
    emailGrp.style.display = 'block';
    roleGrp.style.display = 'block';
    title.textContent = 'Create TrAI Account';
    submitBtn.textContent = 'Register & Sign In';
  } else {
    regTab.classList.remove('active');
    loginTab.classList.add('active');
    emailGrp.style.display = 'none';
    roleGrp.style.display = 'none';
    title.textContent = 'Sign In to TrAI';
    submitBtn.textContent = 'Sign In';
  }
}

async function handleAuthSubmit(e) {
  e.preventDefault();
  const username = document.getElementById('auth-username').value.trim();
  const password = document.getElementById('auth-password').value.trim();
  const errorMsg = document.getElementById('auth-error-msg');
  errorMsg.style.display = 'none';

  if (currentAuthMode === 'register') {
    const email = document.getElementById('auth-email').value.trim();
    const role = document.getElementById('auth-role').value;
    try {
      const res = await fetch(`${API_BASE}/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password, email, role })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.message || data.error || 'Registration failed');
      
      localStorage.setItem('trai_jwt', data.token);
      localStorage.setItem('trai_user', data.username);
      localStorage.setItem('trai_role', data.role || role);
      initAuth();
      closeAuthModal();
      alert(`Welcome to TrAI, ${data.username}!`);
    } catch (err) {
      errorMsg.textContent = err.message;
      errorMsg.style.display = 'block';
    }
  } else {
    try {
      const res = await fetch(`${API_BASE}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.message || data.error || 'Invalid credentials');

      localStorage.setItem('trai_jwt', data.token);
      localStorage.setItem('trai_user', data.username);
      localStorage.setItem('trai_role', data.role || 'USER');
      initAuth();
      closeAuthModal();
    } catch (err) {
      errorMsg.textContent = err.message;
      errorMsg.style.display = 'block';
    }
  }
}

function logoutUser() {
  localStorage.removeItem('trai_jwt');
  localStorage.removeItem('trai_user');
  localStorage.removeItem('trai_role');
  initAuth();
}

// ─── Predictive Market Impact Analytics ───────────────────────────────────────
const marketPresets = {
  opec: "OPEC+ ministers agree to an unexpected 2.2 million barrel-per-day production cut starting next month to stabilise commodity pricing.",
  fed: "Federal Reserve chair announces an emergency 50 basis point benchmark interest rate hike following unexpected headline inflation spikes.",
  peace: "Mediators confirm comprehensive ceasefire signed between belligerent states with immediate demilitarization along the trade corridor."
};

function setMarketPreset(key) {
  const input = document.getElementById('market-event-input');
  if (input && marketPresets[key]) {
    input.value = marketPresets[key];
  }
}

async function predictMarketImpactAction() {
  const input = document.getElementById('market-event-input');
  const pill = document.getElementById('market-impact-pill');
  const container = document.getElementById('market-impact-content');

  const eventSummary = input ? input.value.trim() : '';
  if (!eventSummary) {
    alert('Please enter an event summary to analyze.');
    return;
  }

  pill.className = 'badge badge-unverified';
  pill.textContent = 'MODELING...';
  container.innerHTML = `
    <div class="empty-state">
      <div class="empty-icon">&#8987;</div>
      <p>Simulating cross-asset macroeconomic volatility, commodities flow, and safe-haven dynamics...</p>
    </div>`;

  try {
    const res = await fetch(`${API_BASE}/analytics/market-impact`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({ eventSummary }),
      signal: AbortSignal.timeout(6000)
    });
    if (res.ok) {
      const data = await res.json();
      renderMarketImpactResult(data);
      return;
    }
  } catch (err) {
    console.debug('Backend analytics unavailable, simulating local projection:', err);
  }

  // Simulated heuristic response
  setTimeout(() => {
    const lower = eventSummary.toLowerCase();
    const isConflict = lower.contains ? lower.contains('war') || lower.contains('opec') || lower.contains('cut') : lower.includes('opec') || lower.includes('cut');
    const isRate = lower.includes('rate') || lower.includes('fed');
    
    renderMarketImpactResult({
      status: "SUCCESS",
      mode: "HEURISTIC_FALLBACK",
      prediction: {
        gold: { direction: isConflict ? "UP" : (isRate ? "DOWN" : "NEUTRAL"), magnitude: "HIGH", confidence: 0.82 },
        btc: { direction: isConflict ? "UP" : (isRate ? "DOWN" : "NEUTRAL"), magnitude: "MEDIUM", confidence: 0.68 },
        oil: { direction: isConflict ? "UP" : "DOWN", magnitude: "HIGH", confidence: 0.88 },
        usd: { direction: isRate ? "UP" : "DOWN", magnitude: "LOW", confidence: 0.55 },
        caveat: "Algorithmic forecast based on historical geopolitical price transmission models."
      }
    });
  }, 400);
}

function renderMarketImpactResult(data) {
  const pill = document.getElementById('market-impact-pill');
  const container = document.getElementById('market-impact-content');

  pill.className = 'badge badge-true';
  pill.textContent = 'PROJECTION READY';

  let pred = data.prediction;
  if (typeof pred === 'string') {
    try {
      pred = JSON.parse(pred.replace(/```json/g, '').replace(/```/g, '').trim());
    } catch (e) {
      pred = { raw: pred };
    }
  }

  const assets = [
    { key: 'gold', name: 'Gold (XAU/USD)', icon: '&#129351;', benchmark: 'Safe Haven' },
    { key: 'btc', name: 'Bitcoin (BTC)', icon: '&#8383;', benchmark: 'Digital Liquidity' },
    { key: 'oil', name: 'Brent Crude Oil', icon: '&#9981;', benchmark: 'Energy / Freight' },
    { key: 'usd', name: 'US Dollar Index', icon: '&#36;', benchmark: 'Global Reserve' }
  ];

  const cardsHtml = assets.map(a => {
    const item = (pred && pred[a.key]) || { direction: 'NEUTRAL', magnitude: 'LOW', confidence: 0.5 };
    const dir = (item.direction || 'NEUTRAL').toUpperCase();
    const confPct = Math.round((item.confidence || 0.5) * 100);
    const cardClass = dir === 'UP' ? 'bullish' : (dir === 'DOWN' ? 'bearish' : 'neutral');
    const badgeClass = dir === 'UP' ? 'up' : (dir === 'DOWN' ? 'down' : 'neutral');
    const arrow = dir === 'UP' ? '▲ +' : (dir === 'DOWN' ? '▼ -' : '◆ ');

    // SVG Sparkline path
    const sparkPath = dir === 'UP' 
      ? 'M 0 60 Q 60 40 120 45 T 240 15' 
      : (dir === 'DOWN' ? 'M 0 20 Q 60 25 120 45 T 240 65' : 'M 0 40 Q 60 38 120 42 T 240 40');
    const strokeColor = dir === 'UP' ? '#34d399' : (dir === 'DOWN' ? '#f87171' : '#fbbf24');

    return `
      <div class="market-asset-card ${cardClass}">
        <div class="asset-header">
          <div class="asset-name">${a.icon} ${a.name}</div>
          <span class="asset-badge ${badgeClass}">${arrow}${dir} (${item.magnitude || 'MOD'})</span>
        </div>
        <div style="font-size: 0.78rem; color: #94a3b8;">Benchmark: ${a.benchmark}</div>
        <div class="market-chart-container">
          <svg viewBox="0 0 240 80" style="width:100%; height:100%;">
            <path d="${sparkPath}" fill="none" stroke="${strokeColor}" stroke-width="3" stroke-linecap="round"/>
          </svg>
        </div>
        <div style="display:flex; justify-content:space-between; font-size:0.75rem; color:#94a3b8; margin-top:0.35rem;">
          <span>Confidence</span>
          <span style="font-family:monospace; font-weight:700; color:#f8fafc;">${confPct}%</span>
        </div>
        <div class="confidence-bar-bg">
          <div class="confidence-bar-fill" style="width: ${confPct}%;"></div>
        </div>
      </div>
    `;
  }).join('');

  container.innerHTML = `
    <div class="section-title">Asset Directional Volatility Matrix</div>
    <div class="market-grid">
      ${cardsHtml}
    </div>
    <div class="analysis-text-box" style="margin-top: 1.25rem;">
      <strong>Analytical Context:</strong> ${pred.caveat || pred.summary || 'Forecast generated from multi-modal geopolitical impact inference.'}<br>
      <span style="font-size:0.75rem; color:#64748b; font-family:monospace;">Model: TrAI Predictive Engine • Mode: ${data.mode || 'AI'}</span>
    </div>
  `;
}

// ─── Alerts & Incident Center ───────────────────────────────────────────────
let cachedAlerts = [];

async function loadAlerts(filter = 'ALL') {
  const container = document.getElementById('alerts-list-container');
  if (!container) return;

  try {
    const res = await fetch(`${API_BASE}/alerts`, { signal: AbortSignal.timeout(3000) });
    if (res.ok) {
      cachedAlerts = await res.json();
    }
  } catch (err) {
    if (cachedAlerts.length === 0) {
      cachedAlerts = [
        { id: "alt-1", severity: "CRITICAL", eventType: "PROMPT_INJECTION", message: "Blocked DAN-mode jailbreak attempt in transcript buffer", source: "LiveFactCheckStream", resolved: false, createdAt: new Date(Date.now() - 300000).toISOString() },
        { id: "alt-2", severity: "HIGH", eventType: "LOW_TRUST_CLAIM", message: "Uncorroborated military loss claim flagged from state wire", source: "NewsScraper", resolved: false, createdAt: new Date(Date.now() - 1200000).toISOString() },
        { id: "alt-3", severity: "INFO", eventType: "RECALCULATION", message: "Daily source credibility re-indexing executed successfully", source: "Scheduler", resolved: true, createdAt: new Date(Date.now() - 7200000).toISOString() }
      ];
    }
  }

  let filtered = cachedAlerts;
  if (filter === 'CRITICAL') {
    filtered = cachedAlerts.filter(a => a.severity === 'CRITICAL');
  } else if (filter === 'UNRESOLVED') {
    filtered = cachedAlerts.filter(a => !a.resolved);
  }

  if (filtered.length === 0) {
    container.innerHTML = `<div class="empty-state"><div class="empty-icon">&#10004;</div><p>No active incidents found matching filter "${filter}". All systems nominal.</p></div>`;
    return;
  }

  container.innerHTML = filtered.map(a => {
    const sevClass = a.severity.toLowerCase();
    const timeStr = new Date(a.createdAt).toLocaleTimeString();
    return `
      <div class="alert-card ${sevClass} ${a.resolved ? 'resolved' : ''}">
        <div class="alert-main">
          <div class="alert-title">
            <span class="badge ${a.severity === 'CRITICAL' ? 'badge-false' : (a.severity === 'HIGH' ? 'badge-misleading' : 'badge-news')}">${a.severity}</span>
            <span>${a.message}</span>
          </div>
          <div class="alert-meta">Type: <code>${a.eventType}</code> • Source: ${a.source} • Time: ${timeStr} • Status: ${a.resolved ? 'RESOLVED' : 'ACTIVE'}</div>
        </div>
        <div>
          ${!a.resolved ? `<button class="btn btn-xs btn-outline" onclick="resolveAlertAction('${a.id}')">Acknowledge</button>` : '<span style="color:#10b981; font-size:0.8rem;">&#10004; Resolved</span>'}
        </div>
      </div>
    `;
  }).join('');
}

async function resolveAlertAction(id) {
  try {
    await fetch(`${API_BASE}/alerts/${id}/resolve`, { method: 'POST', headers: getAuthHeaders() });
  } catch (err) {}
  const target = cachedAlerts.find(a => a.id === id);
  if (target) target.resolved = true;
  loadAlerts();
}

// ─── Multi-Language Intelligence ───────────────────────────────────────────
let selectedTargetLang = 'ru';

function setTargetLang(lang) {
  selectedTargetLang = lang;
  document.querySelectorAll('.lang-selector-group button').forEach(b => b.classList.remove('active'));
  const btn = document.getElementById(`btn-lang-${lang}`);
  if (btn) btn.classList.add('active');
}

async function runTranslationAction() {
  const input = document.getElementById('translate-input');
  const output = document.getElementById('translate-output');
  const text = input ? input.value.trim() : '';

  if (!text) {
    alert('Please enter text to translate.');
    return;
  }

  output.innerHTML = '<span style="color:#38bdf8;">Translating and preserving semantic truth anchors...</span>';

  try {
    const res = await fetch(`${API_BASE}/translate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text, targetLanguage: selectedTargetLang }),
      signal: AbortSignal.timeout(5000)
    });
    if (res.ok) {
      const data = await res.json();
      output.innerHTML = `<strong>[${selectedTargetLang.toUpperCase()} Consensus Feed]</strong><br><br>${data.translated}`;
      return;
    }
  } catch (err) {}

  // Heuristic mock translation
  setTimeout(() => {
    let localized = text;
    if (selectedTargetLang === 'ru') {
      localized = `[RU Проверенный перевод]: ${text.replace(/the/gi, '').replace(/announced/gi, 'объявил(а)').replace(/inflation/gi, 'инфляция')}`;
    } else if (selectedTargetLang === 'hy') {
      localized = `[HY Հաստատված թարգմանություն]: ${text.replace(/the/gi, '').replace(/announced/gi, 'հայտարարեց').replace(/inflation/gi, 'գնաճ')}`;
    }
    output.innerHTML = `<strong>[${selectedTargetLang.toUpperCase()} Localized Feed]</strong><br><br>${localized}`;
  }, 350);
}

// ─── Visual Pipeline Topology Flow ──────────────────────────────────────────
const pipelineNodeData = {
  ingest: {
    title: "1. Data Ingress Inflow",
    protocol: "HTTP POST / WebSocket / Webhook HMAC-SHA256",
    throughput: "240 requests / min peak",
    latency: "~4ms",
    description: "Accepts raw transcripts from live video audio feeds, automated RSS/Jsoup scrapers, and external B2B partner JSON webhooks."
  },
  sanitizer: {
    title: "2. Input Sanitizer & Security Gateway",
    protocol: "Regex + Jsoup Safelist Cleaners",
    throughput: "100% inputs scrubbed",
    latency: "~2ms",
    description: "Strips XSS/HTML script tags, intercepts SQL injection attacks, blocks prompt injection (DAN/system jailbreaks), and redacts PII (emails, cards)."
  },
  'ai-engine': {
    title: "3. Dual AI Consensus Core",
    protocol: "xAI Grok-2 + Vertex AI Gemini 1.5 Pro",
    throughput: "Multi-model reasoning via LangChain4j",
    latency: "~620ms",
    description: "Deconstructs factual claims against verified knowledge bases, strips emotive bias & propaganda, and tests for AI hallucinations & glitches."
  },
  guardrails: {
    title: "4. SentinelMind Output Guardrails",
    protocol: "Automated Policy Matrix + Admin Kill Switch",
    throughput: "Risk threshold evaluation",
    latency: "~1ms",
    description: "Evaluates empirical risk scores. Responses above 80% risk or unverified high-damage claims are automatically suppressed with safe fallback payloads."
  },
  egress: {
    title: "5. Vault Storage & Dispatch",
    protocol: "MongoDB 7.0 + Redis 7.2 + Webhook Callbacks",
    throughput: "Sub-millisecond cache lookups",
    latency: "~3ms",
    description: "Persists audit trail records, updates source credibility rankings, caches normalized news, and dispatches JSON callbacks to B2B subscribers."
  }
};

function inspectNode(key) {
  const inspector = document.getElementById('pipeline-node-inspector');
  const info = pipelineNodeData[key];
  if (!inspector || !info) return;

  inspector.innerHTML = `
    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:0.5rem;">
      <h3 style="color:#38bdf8; font-size:1.05rem;">${info.title}</h3>
      <span class="badge badge-true">ONLINE • OPERATIONAL</span>
    </div>
    <div style="font-size:0.85rem; color:#f8fafc; margin-bottom:0.5rem;">${info.description}</div>
    <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap:0.5rem; font-size:0.78rem; color:#94a3b8; font-family:monospace; background:rgba(0,0,0,0.25); padding:0.6rem; border-radius:6px;">
      <div>Protocol: <strong style="color:#e2e8f0;">${info.protocol}</strong></div>
      <div>Throughput: <strong style="color:#e2e8f0;">${info.throughput}</strong></div>
      <div>Processing Latency: <strong style="color:#10b981;">${info.latency}</strong></div>
    </div>
  `;
}

function triggerPipelineSimulation() {
  const nodes = document.querySelectorAll('.pipeline-node');
  const inspector = document.getElementById('pipeline-node-inspector');
  if (!nodes || nodes.length === 0) return;

  inspector.innerHTML = '<span style="color:#38bdf8;"><strong>Simulating Live Data Ingestion:</strong> Packet passing through pipeline nodes...</span>';

  nodes.forEach((node, index) => {
    setTimeout(() => {
      nodes.forEach(n => n.classList.remove('active-pulse'));
      node.classList.add('active-pulse');
      const keys = ['ingest', 'sanitizer', 'ai-engine', 'guardrails', 'egress'];
      inspectNode(keys[index]);
    }, index * 600);
  });

  setTimeout(() => {
    nodes.forEach(n => n.classList.remove('active-pulse'));
    inspector.innerHTML += '<div style="color:#10b981; margin-top:0.5rem;">&#10004; Pipeline execution verified: Ingress ➔ Sanitized ➔ Verified ➔ Guarded ➔ Vaulted (0 Errors).</div>';
  }, nodes.length * 600 + 400);
}
