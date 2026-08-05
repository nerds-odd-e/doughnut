---
phase: 7
slug: compact-result-resolve-dialog-shell
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-08-05
---

# Phase 7 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Vitest 4.1.10 (browser mode) + Cypress/Cucumber E2E |
| **Config file** | `frontend/vitest.config.ts`; E2E `e2e_test/config/ci.ts` (CI skips `@wip`) |
| **Quick run command** | `CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` |
| **Full suite command** | AccidentalMatch + Overlap unit files; targeted Cypress: `accidental_match_reveal.feature`, `overlap_try_again.feature` |
| **Estimated runtime** | ~60–180 seconds (unit); E2E targeted longer |

---

## Sampling Rate

- **After every task commit:** AccidentalMatch (+ Overlap) Vitest file(s)
- **After every plan wave:** Same + targeted Cypress specs above
- **Before `/gsd-verify-work`:** Targeted E2E green (non-`@wip`); no failing unit tests
- **Max feedback latency:** Prefer < 3 minutes for unit sampling

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|---------|-----------------|-----------|-------------------|-------------|--------|
| 07-01-T1 | 01 | 1 | AMR-01, AMR-02 | T-07-01 | Title text interpolation (no v-html) | unit | AccidentalMatch.spec (tracer RED→GREEN) | ✅ rewrite | ⬜ pending |
| 07-01-T2 | 01 | 1 | AMR-03 | T-07-01 | Dismiss-only Modal; no mutate | unit | AccidentalMatch + Overlap specs | ✅ update | ⬜ pending |
| 07-02-T1 | 02 | 2 | AMR-01, AMR-02, AMR-03 | T-07-05 | E2E asserts title text visibility | E2E | accidental_match_reveal Scenario 1 | ✅ update | ⬜ pending |
| 07-02-T2 | 02 | 2 | (regression / interim) | — | No resolve on OVERLAP; link @wip | E2E | reveal (non-@wip) + overlap_try_again | ✅ update | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

*Planner fills exact Task IDs when writing PLAN.md.*

---

## Wave 0 Requirements

- [ ] Rewrite assertions in `AnsweredSpellingQuestionAccidentalMatch.spec.ts` (exists but asserts old stacked UI)
- [ ] Update `AnsweredQuestionPage.ts` reveal/link helpers for CTA/dialog selectors
- [ ] Update `accidental_match_reveal.feature` scenario 1; `@wip` scenarios 2–3
- [ ] Tighten Overlap leak unit test + optionally page object “no resolve CTA”
- [ ] Optional: thin `AccidentalMatchResolveDialog.spec.ts` only if list logic grows — prefer AnsweredSpellingQuestion boundary

*Existing infrastructure covers frameworks; gaps are assertion/selector rewrites, not new infra.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| — | — | — | All phase behaviors have automated verification (unit and/or targeted E2E). |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency acceptable
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
