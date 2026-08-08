---
phase: 5
slug: commission-learning-session
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-08-08
---

# Phase 5 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Vitest (frontend) + JUnit (backend regression) + Cypress/Cucumber (E2E) |
| **Config file** | `frontend/vitest.config.ts`; `e2e_test/config/ci.ts` |
| **Quick run command** | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/CommissionLearningSessionDialog.spec.ts` |
| **Full suite command** | `CURSOR_DEV=true nix develop -c pnpm frontend:test` + `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/learning_session/commissioned_learning_session.feature` |
| **Estimated runtime** | ~120 seconds |

---

## Sampling Rate

- **After every task commit:** Run targeted Vitest file(s) for touched components
- **After every plan wave:** Run `learning_session` Cypress spec + `pnpm frontend:test` for recall components
- **Before `/gsd-verify-work`:** Commission scenario without `@wip`; existing scenarios in same feature still green
- **Max feedback latency:** 120 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 05-01-01 | 01 | 1 | COM-01 | T-05-01 | assertAuthorization on commission | unit | `pnpm frontend:test CommissionLearningSessionDialog.spec.ts` | ❌ W0 | ⬜ pending |
| 05-01-02 | 01 | 1 | COM-02 | — | Request markdown substrings | unit + E2E | Vitest dialog spec + Cypress commission scenario | ❌ W0 | ⬜ pending |
| 05-01-03 | 01 | 1 | COM-03 | — | awaiting-report visible | unit + E2E | `data-test="learning-session-awaiting-report"` | ❌ W0 | ⬜ pending |
| 05-02-01 | 02 | 2 | Regression | — | Phase 3 E2E stay green | E2E | `pnpm cypress run --spec e2e_test/features/learning_session/commissioned_learning_session.feature` | ✅ | ⬜ pending |
| 05-02-02 | 02 | 2 | Optional | — | dueCommissioned exclusion | unit | `pnpm backend:test_only --tests '*RecallsControllerTests*'` | ❌ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `frontend/src/components/recall/CommissionLearningSessionDialog.vue`
- [ ] `frontend/tests/components/recall/CommissionLearningSessionDialog.spec.ts`
- [ ] `e2e_test/step_definitions/learning_session.ts`
- [ ] `e2e_test/start/pageObjects/recallPage.ts` — `commissionLearningSession(notebookTitle)` helper
- [ ] Commission scenario in `e2e_test/features/learning_session/commissioned_learning_session.feature` with `@wip` until green
- [ ] (Recommended) `RecallService` exclusion + `RecallsControllerTests` assertion
- [ ] Update `frontend-api.mdc` blocking inventory for commission mutation

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Copy button clipboard UX | COM-02 | Clipboard API unreliable in headless | Manual smoke: copy request, paste into editor |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 120s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
