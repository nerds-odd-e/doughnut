---
phase: 12
slug: title-navigate-reopen-e2e-polish
status: ready
nyquist_compliant: true
wave_0_complete: false
created: 2026-08-05
---

# Phase 12 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Vitest (frontend) + Cypress/Cucumber (e2e_test) |
| **Config file** | `frontend/vitest.config.ts`; `e2e_test/config/` |
| **Quick run command** | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/RecallPage.spec.ts` (only if Plan 02 client fix) |
| **Full suite command** | `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/accidental_match_reveal.feature,e2e_test/features/recall/overlap_try_again.feature` |
| **Estimated runtime** | ~60–180 seconds (targeted E2E) |

---

## Sampling Rate

- **After every task commit:** Run accidental_match E2E under `@wip` until green; Vitest only if client files touched
- **After every plan wave:** accidental_match + overlap_try_again specs
- **Before `/gsd-verify-work`:** Both targeted specs green; `@wip` removed from reopen scenario
- **Max feedback latency:** 180 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 12-01-01 | 01 | 1 | AMR-05 | T-12-01 | Live graded matchedNotes only | e2e | `pnpm cypress run --spec e2e_test/features/recall/accidental_match_reveal.feature --env tags='@wip'` | ❌ W0 | ⬜ pending |
| 12-01-02 | 01 | 1 | AMR-05 / D-07 | T-12-01 | overlap uncoupled | e2e | same + `overlap_try_again.feature` | ✅ partial | ⬜ pending |
| 12-02-01 | 02 | 2 | AMR-05 | T-12-04 | Skip if 12-01 green | unit (optional) | `pnpm frontend:test tests/pages/RecallPage.spec.ts` | ❌ optional | ⬜ pending |
| 12-02-02 | 02 | 2 | AMR-05 | T-12-04 | Skip if 12-01 green | e2e | accidental_match + overlap_try_again | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] Capability-named E2E scenario for reopen-after-title-navigate in `accidental_match_reveal.feature` (no phase numbers) — Plan 01 Task 1
- [ ] `AnsweredQuestionPage` helpers: open resolve → click matched title → history back → reopen + assert same match titles — Plan 01 Task 1
- [ ] Optional KeepAlive Vitest only if Plan 02 client fix is introduced

*Existing open/dismiss coverage lives inside `expectAccidentalMatchReveal`; extend rather than rewrite.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| — | — | — | All phase behaviors have automated verification. |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 180s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** pending planner validation
