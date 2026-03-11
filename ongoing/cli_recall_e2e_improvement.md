# CLI Recall E2E Test Improvement

## Time Measurements

Measured with `RECORD_E2E_TIMING=1 pnpm cypress run --spec e2e_test/features/cli/cli_recall.feature`. Aggregate with `node e2e_test/aggregate_timing.mjs`.

**Latest run (2025-03-11):**

| Part | Total | Count | Avg |
|------|-------|-------|-----|
| db-reset | 1.1s | 10 | 109ms |
| token-nav | 3.9s | 10 | 394ms |
| token-generateToken | 4.9s | 10 | 490ms |
| token-cli-add | 1.6s | 10 | 163ms |
| token-setup | 10.5s | 10 | 1.05s |
| assimilate-note | 3.5s | 11 | 316ms |
| cli-run | 2.7s | 13 | 208ms |

**Total measured:** 28.2s (sum of parts; overlaps with Cypress overhead). **Full spec:** 10 passing in ~19s.

### By share of token-setup time

- **token-generateToken** — 47% (490ms avg)
- **token-nav** — 38% (394ms avg)
- **token-cli-add** — 16% (163ms avg)

---

## Token Setup Breakdown

Token setup is split into sub-steps (run with `RECORD_E2E_TIMING=1` and aggregate to see):

| Sub-step | What it does | Likely bottlenecks |
|----------|--------------|---------------------|
| **token-nav** | `router.push('/d/generate-token', 'manageAccessTokens')` + `pageIsNotLoading()` | Direct nav to tokens page; first scenario may `cy.visit()` |
| **token-generateToken** | Click "Generate Token", fill Label, submit, wait for `[data-testid="token-result"]` | API call to create token, form submit, wait for token DOM |
| **token-cli-add** | `runCliWithConfig(['-c', '/add-access-token ${token}'])` | Spawn Node process, run CLI bundle, API call to validate token |

### What makes token setup slow

1. **Navigation + page load** — `token-nav` went to `/d/notebooks` then Account → Manage Access Tokens. **Optimized**: now uses direct `router.push('/d/generate-token', 'manageAccessTokens')` to skip notebooks + Account + link clicks.
2. **Token generation API** — `token-generateToken` triggers backend to create a token; we wait for it to appear in the DOM.
3. **Redundant work** — Same flow runs 10× (once per scenario). Token could be generated once and reused.
4. **CLI spawn** — `token-cli-add` spawns a new Node process each time (~200ms typical for cli-run).

### Direct router optimization

Replaced `mainMenu() → userOptions() → manageAccessTokens()` (nav to notebooks, click Account, click link) with:
```
start.routerPush('/d/generate-token', 'manageAccessTokens', {})
start.pageIsNotLoading()
```
Skips ~2 page transitions and 2 clicks per scenario.

**Measured impact (after optimization):**

| Label | Total | Count | Avg |
|-------|-------|-------|-----|
| token-nav | 3.9s | 10 | 394ms |
| token-generateToken | 4.9s | 10 | 490ms |
| token-cli-add | 1.6s | 10 | 163ms |
| **token-setup** | **10.5s** | 10 | **1.05s** |

**Before** (mainMenu flow): token-setup ~12.2s total, ~1.22s avg  
**After** (direct router): token-setup 10.5s total, ~1.05s avg  
**Acceleration: ~1.7s total (~14% faster) for token-setup across 10 scenarios**

Full spec: 10 passing in ~19s.

---

## Current Statistics

### cli_recall.feature

| Metric | Count |
|--------|-------|
| Total scenarios | 10 |
| Scenarios with assimilation | 9 |
| Scenarios with no assimilation | 1 (Recall status shows zero) |
| Total "I assimilate the note" calls | 11 |
| Assimilation with spelling option | 1 |
| Scenarios running CLI 2+ times | 3 |

### Assimilation breakdown by scenario

| Scenario | Assimilations | CLI runs |
|----------|---------------|----------|
| Recall status shows count when notes are due | 1 | 1 |
| Recall status shows zero when no notes are due | 0 | 1 |
| Recall next Just Review - answer yes | 1 | 1 |
| Recall next shows zero after recalling | 1 | 2 |
| Recall next Just Review shows markdown | 1 | 1 |
| Recall next MCQ - choose correct answer | 1 | 1 |
| Recall next MCQ - down arrow and Enter | 1 | 1 |
| Recall session - complete all due notes | 2 | 1 |
| Recall load more from next 3 days | 2 | 2 |
| Recall next spelling | 1 (spelling) | 3 |

### Background (runs every scenario)

- I am logged in as an existing user
- **I have the CLI configured with a valid access token** — mainMenu → userOptions → manageAccessTokens → generateToken → runCliWithConfig add-access-token

### Assimilation flow used

- **Only** "I assimilate the note X" (note-page flow)
- Each call: `jumpToNotePage(X) → moreOptions() → assimilateNote()` = note page + assimilation page per note

---

## Comparison: CLI Recall vs Web Recall

### Web recall assimilation patterns

| Pattern | Usage | Cost |
|---------|-------|------|
| "I assimilate these in sequence" | recall_pages (4 notes), assimilating | 1 page load for N notes |
| "I assimilate X" | spaced_repetition | 1 assimilation page load |
| "I assimilate the note X" | recall_quiz_*, browse_answer, re_assimilate | 2 page loads per note |
| "On day X I recall ... and assimilate new ..." | spaced_repetition | recall page + assimilation page |

Web recall uses the cheaper assimilation-page flow for multi-note setups. CLI recall uses only the note-page flow.

### Key differences

| Factor | CLI Recall | Web Recall |
|--------|------------|------------|
| Token setup in Background | Yes (10×) | No |
| Assimilation flow | Note-page only (11×) | Mixed; batch via assimilation page when possible |
| CLI process spawn | 1–3 per scenario | N/A |

---

## Root causes of slowness

1. **CLI token setup** — Full UI flow (notebooks page, Account, Manage Access Tokens, Generate Token, add via CLI) runs **10 times**.
2. **Assimilation flow** — All assimilations use the note-page path; no use of "I assimilate these in sequence" or "I assimilate X" (assimilation-page flow).
3. **Multiple CLI invocations** — Some scenarios run the CLI 2–3 times; each run spawns a new Node process.

---

## Improvement ideas

1. **Cache token setup** — Generate token once per feature/file; reuse for scenarios that don’t need a fresh token.
2. **Switch assimilation to assimilation-page flow** — Where possible, use "I assimilate these in sequence" or "I assimilate X" instead of "I assimilate the note X" to avoid note-page navigations.
3. **Testability API for assimilated notes** — Add backend endpoint to create memory trackers for notes at specific timestamps; bypass UI assimilation entirely.
4. **Reduce waitForAssimilationReady timeout** — Currently 10s in `assimilationPage.ts`; consider lowering if it’s often unnecessary.
5. ~~**Investigate "Recall next MCQ - down arrow and Enter" CI outlier**~~ **Fixed** — Root cause: `script` (PTY) hangs in headless CI; code waited full 55s timeout before fallback to pipe mode. Fix: skip PTY in CI, use pipe+fallback directly; reduced timeout from 55s to 5s for non-CI.

---

## How to collect timing

```bash
# With services running (pnpm sut)
RECORD_E2E_TIMING=1 pnpm cypress run --spec e2e_test/features/cli/cli_recall.feature

# Aggregate results
node e2e_test/aggregate_timing.mjs
```

Output is written to `e2e_test/timing_log.jsonl` (gitignored).

---

## GitHub CI Profile (from job 60106047662, 2026-03-11)

Source: `7_End-to-End tests with Database (e2e_test_features_b_e2e_test_features_c_...)` log.

### cli_recall.feature in CI

| Scenario | Duration (CI) |
|----------|---------------|
| Recall status shows count when notes are due | 2449ms |
| Recall status shows zero when no notes are due | 1659ms |
| Recall next Just Review - answer yes | 2223ms |
| Recall next shows zero after recalling | 2514ms |
| Recall next Just Review shows markdown | 2175ms |
| Recall next MCQ - choose correct answer | 2377ms |
| **Recall next MCQ - down arrow and Enter** | **57297ms** |
| Recall session - complete all due notes | 2678ms |
| Recall load more from next 3 days | 3315ms |
| Recall next spelling | 3117ms |

**Full spec:** 10 passing in **1m 20s** (80s) — **~4× slower than local (~19s)**.

**Outlier:** "Recall next MCQ - down arrow and Enter" takes **57.3s** (71% of total). Uses `@usingMockedOpenAiService` and simulates down-arrow keypresses. Logs show ~55s gap between Mountebank POST at 00:34:23 and 00:35:18 — likely a long wait (timeout or slow key-interaction simulation) in CI. Other 9 scenarios: ~23s total, ~2.5s avg.

### mcp_services.feature in CI

| Scenario | Duration (CI) |
|----------|---------------|
| Adding note to a known parent note | 4868ms |
| AI agent learns from Doughnut (example #1) | 3261ms |
| AI agent learns from Doughnut (example #2) | 3226ms |
| AI agent learns from Doughnut (example #3) | 3204ms |
| AI agent respects different token limits (example #1) | 3298ms |
| AI agent respects different token limits (example #2) | 3385ms |

**Full spec:** 6 passing in **21s**. Avg ~3.5s per scenario. MCP server is bundled before each scenario (`@BundleFirstAndTerminateMCPServerWhenTeardown`).

### Comparison: cli_recall vs other specs (same CI run)

| Spec | Scenarios | Total | Avg/scenario |
|------|-----------|-------|--------------|
| cli_recall | 10 | **80s** | **8.0s** (2.5s excl. outlier) |
| mcp_services | 6 | 21s | 3.5s |
| cli_install_and_run | 8 | 29s | 3.6s |
| cli_access_token | 5 | 9s | 1.8s |
| circles/creating_circles | 3 | 5s | 1.7s |
| get_certificate_by_passing_assessment | 4 | 5s | 1.2s |

Without the outlier, cli_recall avg (~2.5s) is in line with other CLI specs but slower than simpler specs due to token setup + assimilation per scenario.

### Step-level profile (from CI log timestamps)

CI does not set `RECORD_E2E_TIMING`, so the structured labels (db-reset, token-nav, token-generateToken, etc.) are not present. The following is derived from timestamp deltas between log events.

**cli_recall.feature — event timeline**

| Event | Δ from prev | Notes |
|-------|-------------|-------|
| Spec start | — | 00:34:06 |
| Scenario: Recall status shows count | 1.7s | 00:34:10 |
| Scenario: Recall status shows zero | 1.7s | 00:34:12 |
| Scenario: Recall next Just Review | 2.2s | 00:34:14 |
| Scenario: Recall next shows zero | 2.2s | 00:34:16 |
| Scenario: Recall next markdown | 2.2s | 00:34:19 |
| MB setup (DELETE/POST/PUT stubs) | 1.0s | Before MCQ tests |
| Scenario: MCQ choose correct | 0.6s | OpenAI mock hit at 00:34:21 |
| MB setup for down-arrow scenario | 1.1s | 00:34:22 |
| **OpenAI mock invoked** | **55.5s** | **OUTLIER** — 00:34:23 → 00:35:18 |
| Scenario: MCQ down arrow | 0.2s | 00:35:18 |
| Scenario: Recall session | 2.6s | 00:35:21 |
| Scenario: Recall load more | 2.5s | 00:35:24 |
| Scenario: Recall next spelling | 2.6s | 00:35:28 |

**mcp_services.feature — event timeline**

| Event | Δ from prev | Notes |
|-------|-------------|-------|
| Spec start | — | 00:35:28 |
| MCP bundle | 2.2s | First bundle before scenario 1 |
| Scenario: Adding note | 3.3s | 00:35:34 |
| MCP bundle | 1.7s | Between scenarios |
| Scenario: AI agent learns #1 | 3.3s | 00:35:38 |
| MCP bundle | 1.7s | |
| Scenario: AI agent learns #2 | 3.2s | 00:35:41 |
| MCP bundle | 1.6s | |
| Scenario: AI agent learns #3 | 3.2s | 00:35:44 |
| MCP bundle | 1.5s | |
| Scenario: Token limits #1 | 3.3s | 00:35:47 |
| MCP bundle | 1.7s | |
| Scenario: Token limits #2 | 3.4s | 00:35:51 |

Each MCP scenario is preceded by an MCP server bundle (~27–38ms esbuild + spawn overhead). The 1.5–2.2s gaps include MCP connect/teardown and Cypress step transitions.
