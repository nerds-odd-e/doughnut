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

- **After every FE-component task:** Targeted Vitest (primary fast per-task feedback, seconds)
- **After every backend-exclusion task:** Targeted JUnit (`RecallsControllerTests`)
- **Behavior-phase gates (accepted ~120s):** Cypress on **05-01 T1** (`@wip` commission tracer) and **05-02 T2** (full feature graduation) — intentional COM-01–03 observability, not the day-to-day loop
- **After every plan wave:** Run `learning_session` Cypress spec + `pnpm frontend:test` for recall components
- **Before `/gsd-verify-work`:** Commission scenario without `@wip`; existing scenarios in same feature still green
- **Per-task feedback target:** Vitest/JUnit (seconds). **Gate max:** ~120 seconds Cypress (accepted)

---

## Accepted exceptions

| Warning | Resolution |
|---------|------------|
| Plan-checker #1 `[key_links_planned]` — UI-SPEC listed RecallPage as dialog host | **Resolved.** `05-UI-SPEC.md` Component Inventory: dialog hosted in `RecallProgressBar.vue`; `RecallPage.vue` no change. |
| Plan-checker #2 `[research_resolution]` — Open Questions not marked resolved | **Resolved.** `05-RESEARCH.md` → `## Open Questions (RESOLVED)` with Q1–Q3 decisions. |
| Plan-checker #3 `[verify_command_format]` — weak grep\|awk on 05-01 T3 | **Resolved.** Verify uses `test "$(grep -c …)" -ge 1`. |
| Plan-checker #4 `[nyquist_compliance]` — Cypress ~120s on 05-01 T1 / 05-02 T2 | **Accepted.** Behavior-phase gate; Vitest (05-01 T2) and JUnit (05-02 T1) provide fast per-task feedback. Do not restructure verifies to drop Cypress from those tasks. |
| Plan-checker #5 `[architectural_tier_compliance]` — T-05-05 two-user test missing from 05-02 T1 | **Resolved.** `05-02-PLAN.md` Task 1 action + acceptance_criteria include two-user cross-leakage case. |

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
- [x] Feedback latency: Vitest/JUnit per-task; Cypress ~120s accepted for 05-01 T1 + 05-02 T2 behavior gates
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
