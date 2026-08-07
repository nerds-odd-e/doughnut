---
phase: 2
slug: assimilate-as-commissioned
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-08-08
---

# Phase 2 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit (backend) + Vitest browser mode (frontend) + Cypress/Cucumber (E2E) |
| **Config file** | backend Gradle; frontend vitest; `e2e_test/config/ci.ts` (`not @wip` on CI) |
| **Quick run command** | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` and/or `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/AssimilationPanel.spec.ts` |
| **Full suite command** | `CURSOR_DEV=true nix develop -c pnpm backend:verify`; targeted E2E `cypress run --spec` for learning_session feature (not full suite) |
| **Estimated runtime** | ~60–180 seconds (targeted) |

---

## Sampling Rate

- **After every task commit:** Run targeted backend:test_only and/or the single frontend AssimilationPanel spec
- **After every plan wave:** Both unit layers green + WIP E2E run locally until green, then remove `@wip`
- **Before `/gsd-verify-work`:** Unit green; Phase 2 E2E scenario green without `@wip`
- **Max feedback latency:** 180 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 02-01-01 | 01 | 1 | TRK-01 | T-02-01 | Logged-in assimilate only; refuse property+commissioned | unit | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` | ✅ extend | ⬜ pending |
| 02-01-02 | 01 | 1 | TRK-01 / D-03 / D-06 | — | Menu + stay on note; ignore COMMISSIONED for disable | unit | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/AssimilationPanel.spec.ts` | ✅ extend | ⬜ pending |
| 02-01-03 | 01 | 1 | TRK-01 / TRK-02 | — | E2E assimilate as commissioned → see Commissioned | e2e | `cypress run --spec e2e_test/features/learning_session/*.feature` | ❌ W0 | ⬜ pending |
| 02-02-01 | 02 | 2 | TRK-02 / D-07 | — | Coexistence + Commissioned label | unit | backend:test_only + frontend AssimilationPanel / NoteInfo | ✅ extend | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `e2e_test/features/learning_session/` — graduate Phase 2 scenario `@wip`
- [ ] Step defs: `I assimilate it as commissioned`; `I should see a commissioned memory tracker for {string}`
- [ ] Page object: note-level caret + menu; expect Type `Commissioned`
- [ ] TS `MemoryTrackerBuilder.commissioned()` if missing for Vitest fixtures
- [ ] Frontend tests for disable-ignore-COMMISSIONED and no-navigate commissioned path

*Existing AssimilationControllerTests + AssimilationPanel.spec provide stable boundaries to extend.*

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
