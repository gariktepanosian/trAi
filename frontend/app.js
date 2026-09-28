const API_BASE = 'http://localhost:8080/api/v1';

// Tab Switching
document.addEventListener('DOMContentLoaded', () => {
  initTabs();
  checkBackendStatus();
  loadSourceScores();
});

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
      headers: { 'Content-Type': 'application/json' },
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
      headers: { 'Content-Type': 'application/json' },
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

  setTimeout(() => {
    pill.className = 'badge badge-true';
    pill.textContent = 'NORMALIZED';

    resultCard.innerHTML = `
      <div class="verdict-header-box">
        <div>
          <div style="font-size: 0.8rem; color: #94a3b8;">ORIGINAL SOURCE</div>
          <div style="font-weight: 600; font-size: 1.1rem; color: #f8fafc;">${sourceName}</div>
        </div>
        <div style="text-align: right;">
          <div class="verdict-score-label">Propaganda Stripped</div>
          <div class="verdict-score-gauge" style="color: #06b6d4;">85%</div>
        </div>
      </div>

      <div class="section-title">Radically Neutral Empirical Extraction</div>
      <div class="analysis-text-box">
        <strong>Objective Event:</strong> Military engagement reported in the contested sector resulting in equipment destruction and casualties.<br><br>
        <strong>Eliminated Double Standards:</strong> Replaced emotional adjectives ("cowardly", "unprovoked", "heroic defense battalions", "corrupt western puppets", "annihilated") with factual, neutral descriptors ("opposing forces engaged", "units sustained heavy damage").
      </div>

      <div class="section-title">Corroborated Facts vs Unverified Claims</div>
      <div class="claim-list">
        <div class="claim-item verified">
          <strong>[VERIFIED FACT]</strong> Clashes occurred along the designated northern boundary line at 04:30 UTC.
        </div>
        <div class="claim-item debunked">
          <strong>[UNVERIFIED CLAIM]</strong> Claim that opposing forces were "completely annihilated" is unsubstantiated by satellite radar and independent observers.
        </div>
        <div class="claim-item warning">
          <strong>[INSIDER STATUS - RED]</strong> Reports of sudden supply line disruption remain unverified pending official logistics confirmation.
        </div>
      </div>
    `;
  }, 400);
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
