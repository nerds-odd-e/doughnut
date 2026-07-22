---
phase: 6
slug: user-level-defaults
status: approved
nyquist_compliant: true
wave_0_complete: true
created: 2026-07-22
---

# Phase 6 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Backend: JUnit 5 + Spring Boot Test; Frontend: Vitest; E2E: Cypress + Cucumber |
| **Config file** | Backend Spring test profile; Frontend Vitest; `e2e_test/config/ci.ts` (`not @wip` in CI) |
| **Quick run command** | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/notebook/NotebookHealthPanel.spec.ts` and focused backend `UserControllerTest` via `pnpm backend:test_only` when no migration in slice |
| **Full suite command** | `CURSOR_DEV=true nix develop -c pnpm backend:test` (includes migrate) + targeted frontend Health specs + targeted `notebook_health` E2E |
| **Estimated runtime** | Unit: ~30–90s targeted; E2E: ~1–3 min for feature |

---

## Sampling Rate

- **After every task commit:** Targeted frontend and/or backend tests for the touched slice
- **After every plan wave:** `backend:test` when migration involved + Health panel frontend specs green
- **Before phase seal:** Targeted `notebook_health` E2E green; `@wip` removed only when scenarios pass
- **Max feedback latency:** Prefer targeted runs; do **not** run full E2E suite unless required

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|----------------|-----------------|-----------|-------------------|-------------|--------|
| 06-01-01 | 01 | 1 | DFLT-01 | T-06-01 | RED: User default false + PATCH round-trip | unit | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` | ✅ in 06-01 T1 | ⬜ pending |
| 06-01-02 | 01 | 1 | DFLT-01 | T-06-01 | GREEN: column + UserDTO/updateUser mapping | unit | `CURSOR_DEV=true nix develop -c pnpm backend:test` | ✅ in 06-01 T2 | ⬜ pending |
| 06-02-01 | 02 | 2 | DFLT-02, DFLT-01 | T-06-02 | Prefill + Save as defaults; no lint on open/save | unit | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/notebook/NotebookHealthPanel.spec.ts` | ✅ in 06-02 T1 | ⬜ pending |
| 06-02-02 | 02 | 2 | DFLT-01, DFLT-02 | T-06-02 | Cross-notebook prefill E2E | E2E | `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/notebooks/notebook_health.feature` | ✅ in 06-02 T2 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

Wave 0 test gaps are covered **inside** plan TDD tasks (no separate Wave 0 plan; no `<automated>MISSING</automated>` refs):

- [x] Extend `UserControllerTest` — 06-01 Task 1 (RED)
- [x] Extend `NotebookHealthPanel.spec.ts` — 06-02 Task 1 (RED then GREEN)
- [x] E2E page object `saveAsDefaults()` + checkbox expectation — 06-02 Task 2
- [x] E2E steps for Save + checkbox assertion — 06-02 Task 2
- [x] New scenario in `notebook_health.feature` tagged `@wip` until green — 06-02 Task 2

*Existing JUnit + Vitest + Cypress infrastructure — extend Health/User tests only.*

---

## Manual-Only Verifications

All phase behaviors have automated verification.

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references (none; tests authored in TDD tasks)
- [x] No watch-mode flags
- [x] Feedback latency documented; full E2E suite not required
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** plan-checker 2026-07-22 — plans executable; Phase 6 goal covered
