---
phase: 04
slug: learning-session-request-builder
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-08-08
---

# Phase 04 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit (backend); Cypress 15.20.0 for regression only |
| **Config file** | backend Gradle; `e2e_test/features/learning_session/commissioned_learning_session.feature` |
| **Quick run command** | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` |
| **Full suite command** | `CURSOR_DEV=true nix develop -c pnpm backend:verify`; regression E2E: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/learning_session/commissioned_learning_session.feature` |
| **Estimated runtime** | ~120 seconds |

---

## Sampling Rate

- **After every task commit:** Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- **After every plan wave:** Run `CURSOR_DEV=true nix develop -c pnpm backend:verify` + targeted `learning_session` Cypress spec
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 120 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 04-01-01 | 01 | 1 | Structure SC2 | T-04-01 / — | Commission scoped to authorized notebook | unit | `pnpm backend:test_only` | ❌ W0 | ⬜ pending |
| 04-01-02 | 01 | 1 | Structure SC3 | — | Request markdown matches ADR 0005 | unit | `pnpm backend:test_only` | ❌ W0 | ⬜ pending |
| 04-02-01 | 02 | 2 | Structure SC1 | — | No user-visible regression | e2e | `cypress run --spec …/commissioned_learning_session.feature` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `V300000240__learning_session_and_session_item.sql`
- [ ] `LearningSession` / `SessionItem` entities + repositories
- [ ] `LearningSessionService` + `LearningSessionRequestMarkdownBuilder`
- [ ] `LearningSessionController` + DTOs + OpenAPI regen
- [ ] `LearningSessionControllerTests` — commission Spanish notebook fixture; assert markdown sections
- [ ] `makeMe.aLearningSession()` / builder for Phase 6 prep

---

## Manual-Only Verifications

*All phase behaviors have automated verification.*

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 120s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
