---
phase: 7
slug: amend-recorded-session
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-08-08
---

# Phase 7 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit (backend) + Vitest (frontend) + Cypress/Cucumber (E2E) |
| **Config file** | `frontend/vitest.config.ts`; `e2e_test/config/ci.ts` |
| **Quick run command** | `CURSOR_DEV=true nix develop -c pnpm backend:test_only -- --tests com.odde.doughnut.controllers.LearningSessionControllerTests --tests com.odde.doughnut.algorithms.CommissionedLearningSessionFeedbackPolicyTest` |
| **Full suite command** | `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/learning_session/commissioned_learning_session.feature` |
| **Estimated runtime** | ~120 seconds (Cypress gate) |

---

## Sampling Rate

- **After every backend task:** Targeted JUnit (`LearningSessionControllerTests`, `CommissionedLearningSessionFeedbackPolicyTest`, `RecallsControllerTests`)
- **After every FE task:** Targeted Vitest (`RecallProgressBar.spec.ts`, `CommissionLearningSessionDialog.spec.ts`)
- **Behavior-phase gates:** Cypress on **07-01 T2** (`@wip` amend tracer) and **07-02 T3** (full feature graduation)
- **After wave 2:** Amend scenario without `@wip`; prior `learning_session` scenarios still green
- **Per-task feedback target:** JUnit/Vitest (seconds). **Gate max:** ~120s Cypress (accepted)

---

## Accepted exceptions

| Warning | Resolution |
|---------|------------|
| Plan-checker — Open Questions not marked resolved | **Resolved.** `07-RESEARCH.md` → `## Open Questions (RESOLVED)`. |
| Plan-checker — missing VALIDATION.md | **Resolved.** This file. |
| Plan-checker — 07-01 tracer acceptance undershoots E2E | **Resolved.** Acceptance includes day-3 zero potential sessions; verify runs full `@wip` scenario. |
| Plan-checker — two-user recordedSessions isolation | **Resolved.** `07-02-PLAN.md` Task 1 adds `recordedSessionsDoesNotLeakAcrossUsers`. |

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 07-01-01 | 01 | 1 | D-02 schema | T-07-SC | Flyway snapshot columns | decision | Human checkpoint before migration | ❌ W0 | ⬜ pending |
| 07-01-02 | 01 | 1 | AMD-01 tracer | T-07-01–03 | Amend API + snapshot re-grade | E2E + JUnit | Cypress `@wip` + controller/policy tests | ❌ W0 | ⬜ pending |
| 07-01-03 | 01 | 1 | D-01, D-13 | T-07-03 | No double recallCount | unit | `CommissionedLearningSessionFeedbackPolicyTest` | ✅ partial | ⬜ pending |
| 07-02-01 | 02 | 2 | D-06, D-07 | T-07-05 | recordedSessions strip + cross-user | unit + Vitest | `RecallsControllerTests` + RecallProgressBar spec | ❌ W0 | ⬜ pending |
| 07-02-02 | 02 | 2 | D-05, D-09, D-10 | — | Partial amend + scheduling | unit | `LearningSessionControllerTests` + `RecallsControllerTests` | ❌ | ⬜ pending |
| 07-02-03 | 02 | 2 | D-11 | — | Amend E2E graduation | E2E | Full `commissioned_learning_session.feature` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Gaps

- [ ] `V300000241__session_item_pre_session_snapshot.sql`
- [ ] `SessionItem` snapshot fields + `SessionItemBuilder` helpers
- [ ] `MemoryTracker.restorePreSessionSnapshot` + amend re-grade path
- [ ] `LearningSessionService.record` RECORDED amend branch
- [ ] `RecordedLearningSessionLite` + `DueMemoryTrackers.recordedSessions`
- [ ] `RecallProgressBar` recorded strip + `mode=amend` dialog
- [ ] `useRecallData` / `useRecallPageLoading` recorded sessions wiring
- [ ] `LearningSessionControllerTests.Record` amend nested tests
- [ ] `CommissionedLearningSessionFeedbackPolicyTest` snapshot-vs-compound cases
- [ ] E2E Given `I have recorded a learning session…` + amend scenario `@wip`
- [ ] `generate-api-client` after OpenAPI DTO change

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Copy-paste amend UX | AMD-01 | Automated E2E covers dialog path | Optional smoke: amend via strip after 07-02 |

---

## Phase Success Criteria → Evidence

| Criterion | Primary evidence |
|-----------|------------------|
| SC1 Re-paste updates Feedback | `LearningSessionControllerTests` amend cases; E2E tutor feedback assertion |
| SC2 Amended scores drive potential-session membership | Day-3 zero potential sessions E2E; `RecallsControllerTests` dueCommissioned empty |
| SC3 Recorded marking visible | `data-test=learning-session-recorded` after amend; recorded strip row |
