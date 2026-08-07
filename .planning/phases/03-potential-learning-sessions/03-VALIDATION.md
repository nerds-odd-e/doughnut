---
phase: 3
slug: potential-learning-sessions
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-08-08
---

# Phase 3 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit (backend) + Vitest (frontend) + Cypress/Cucumber (E2E) |
| **Config file** | backend Gradle tests; `frontend/vitest.config.ts`; `e2e_test/config/ci.ts` |
| **Quick run command** | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` and targeted `pnpm frontend:test` |
| **Full suite command** | `CURSOR_DEV=true nix develop -c pnpm backend:verify`; E2E: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/learning_session/commissioned_learning_session.feature` |
| **Estimated runtime** | ~60–180 seconds (targeted) |

---

## Sampling Rate

- **After every task commit:** Run targeted backend and/or frontend test for the touched surface
- **After every plan wave:** Run `pnpm backend:test_only` + relevant frontend specs + learning_session E2E `--spec` when scenarios exist
- **Before `/gsd-verify-work`:** Those greens; remove `@wip` when both Phase 3 scenarios pass
- **Max feedback latency:** 180 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 03-01-01 | 01 | 1 | TRK-03 | T-03-01 | User-scoped due COMMISSIONED only | unit | `pnpm backend:test_only` (RecallsController) | ❌ W0 | ⬜ pending |
| 03-01-02 | 01 | 1 | POT-01 | T-03-02 | Vue text escape notebook names | unit | `pnpm frontend:test` (RecallProgressBar) | ❌ W0 | ⬜ pending |
| 03-02-01 | 02 | 2 | POT-01/02 | — | N/A | e2e | `pnpm cypress run --spec …/commissioned_learning_session.feature` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] Backend controller assertion for positive `dueCommissioned` payload
- [ ] `DueMemoryTrackersBuilder` + frontend fixture support for `dueCommissioned`
- [ ] Frontend unit test: potential session rows by notebook; ordinary `toRepeatCount` unchanged
- [ ] E2E: graduate two Phase 3 scenarios; Given/Then steps; page-object method
- [ ] Extend testability assimilate for `assimilateAsCommissioned: true` if needed for bulk Given

*Existing infrastructure covers frameworks — no new installs.*

---

## Manual-Only Verifications

All phase behaviors have automated verification.

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 180s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
