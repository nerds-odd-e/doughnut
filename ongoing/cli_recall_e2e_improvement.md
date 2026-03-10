# CLI Recall E2E Test Improvement

## Time Measurements

Measured with `RECORD_E2E_TIMING=1 pnpm cypress run --spec e2e_test/features/cli/cli_recall.feature`. Aggregate with `node e2e_test/aggregate_timing.mjs`.

| Part | Total | Count | Avg |
|------|-------|-------|-----|
| db-reset | 1.1s | 10 | 111ms |
| token-setup | 12.2s | 10 | 1.2s |
| assimilate-note | 3.5s | 11 | 317ms |
| cli-run | 2.6s | 12 | 219ms |
| **Total (measured)** | **19.5s** | | |

**Note:** Total excludes login, notebook/notes setup, time travel, and Cypress overhead. Full spec run ~21–30s in measured conditions.

### By share of measured time

- **token-setup** — 62% (runs 10×, ~1.2s each)
- **assimilate-note** — 18% (11 assimilations, ~317ms each)
- **cli-run** — 13% (12 runs, ~219ms each)
- **db-reset** — 6% (10×, ~111ms each)

---

## Token Setup Breakdown

Token setup is split into sub-steps (run with `RECORD_E2E_TIMING=1` and aggregate to see):

| Sub-step | What it does | Likely bottlenecks |
|----------|--------------|---------------------|
| **token-nav** | `mainMenu()` → `navigateToNotebooksPage()` + `pageIsNotLoading()` | `router().push('/d/notebooks')` does `cy.visit()` on first scenario (full page load); `pageIsNotLoading()` waits for `.loading-bar` (10s timeout) |
| **token-account** | Click "Account" button | Fast (single click) |
| **token-manageTokens** | Click "Manage Access Tokens" link | Client-side navigation to tokens page |
| **token-generateToken** | Click "Generate Token", fill Label, submit, wait for `[data-testid="token-result"]` | API call to create token, form submit, wait for token DOM |
| **token-cli-add** | `runCliWithConfig(['-c', '/add-access-token ${token}'])` | Spawn Node process, run CLI bundle, API call to validate token |

### What makes token setup slow

1. **Navigation + page load** — `token-nav` goes to `/d/notebooks`. First scenario does full `cy.visit()`; others use Vue Router. `pageIsNotLoading()` waits for loading bar (10s timeout).
2. **Token generation API** — `token-generateToken` triggers backend to create a token; we wait for it to appear in the DOM.
3. **Redundant work** — Same flow runs 10× (once per scenario). Token could be generated once and reused.
4. **CLI spawn** — `token-cli-add` spawns a new Node process each time (~200ms typical for cli-run).

Run `RECORD_E2E_TIMING=1 pnpm cypress run --spec e2e_test/features/cli/cli_recall.feature` then `node e2e_test/aggregate_timing.mjs` to get actual sub-step timings.

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

---

## How to collect timing

```bash
# With services running (pnpm sut)
RECORD_E2E_TIMING=1 pnpm cypress run --spec e2e_test/features/cli/cli_recall.feature

# Aggregate results
node e2e_test/aggregate_timing.mjs
```

Output is written to `e2e_test/timing_log.jsonl` (gitignored).
