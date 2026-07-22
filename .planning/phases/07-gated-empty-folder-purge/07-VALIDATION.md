---
phase: 7
slug: gated-empty-folder-purge
status: draft
nyquist_compliant: true
wave_0_complete: true
created: 2026-07-22
---

# Phase 7 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Backend: JUnit 5 + Spring Boot Test; Frontend: Vitest; E2E: Cypress + Cucumber |
| **Config file** | Backend Spring test profile; Frontend Vitest; `e2e_test/config/ci.ts` (`not @wip` in CI) |
| **Quick run command** | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` and `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/notebook/NotebookHealthPanel.spec.ts` |
| **Full suite command** | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` + Health panel frontend specs + targeted `notebook_health` E2E (no full E2E suite) |
| **Estimated runtime** | Unit: ~30–90s targeted; E2E: ~1–3 min for feature |

---

## Sampling Rate

- **After every task commit:** Targeted backend and/or frontend tests for the touched slice
- **After every plan wave:** 07-01 → `backend:test_only` green; 07-02 → Health panel frontend specs green
- **Before phase seal:** Targeted `notebook_health` E2E green; `@wip` removed only when purge scenario passes
- **Max feedback latency:** Prefer targeted runs; do **not** run full E2E suite unless required

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|----------------|-----------------|-----------|-------------------|-------------|--------|
| 07-01-01 | 01 | 1 | AFIX-03, AFIX-04, AFIX-05 | T-07-01..T-07-05 | RED: purge CASCADE-safe + fix opt-in/auth tests | unit | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` | ✅ in 07-01 T1 | ⬜ pending |
| 07-01-02 | 01 | 1 | AFIX-02..05 | T-07-01..T-07-05 | GREEN: EmptyFolderBulkPurge + health/fix + OpenAPI regen | unit | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` | ✅ in 07-01 T2 | ⬜ pending |
| 07-02-01 | 02 | 2 | AFIX-02, AFIX-03 | T-07-02, T-07-07 | Prefill gate Fix + fix→re-lint; no optimistic clear | unit | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/notebook/NotebookHealthPanel.spec.ts` | ✅ in 07-02 T1 | ⬜ pending |
| 07-02-02 | 02 | 2 | AFIX-02..05 | T-07-04, T-07-05 | Gated purge E2E; empty gone, readme-only remains | E2E | `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/notebooks/notebook_health.feature` | ✅ in 07-02 T2 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

Wave 0 test gaps are covered **inside** plan TDD tasks (no separate Wave 0 plan; no `<automated>MISSING</automated>` refs):

- [x] `EmptyFolderBulkPurgeTest` + controller Fix cases — 07-01 Task 1 (RED)
- [x] Extend `NotebookHealthPanel.spec.ts` Fix enablement / call order — 07-02 Task 1 (RED then GREEN)
- [x] E2E page object `applyFix()` + expectations — 07-02 Task 2
- [x] E2E steps for Fix + post-purge assertions — 07-02 Task 2
- [x] New scenario in `notebook_health.feature` tagged `@wip` until green — 07-02 Task 2
- [x] OpenAPI regen after fix endpoint — 07-01 Task 2

*Existing JUnit + Vitest + Cypress infrastructure — extend Health tests only.*

---

## Manual-Only Verifications

| Item | Why manual | Deferred? |
|------|------------|-----------|
| Action-bar wrap with four controls on narrow width | UI-SPEC overflow backstop | Optional visual check during E2E; not blocking automation |

All core phase behaviors have automated verification.

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references (none; tests authored in TDD tasks)
- [x] No watch-mode flags
- [x] Feedback latency documented; full E2E suite not required
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** pending plan-checker
