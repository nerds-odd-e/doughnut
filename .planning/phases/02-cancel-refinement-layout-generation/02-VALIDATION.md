---
phase: 2
slug: cancel-refinement-layout-generation
status: draft
nyquist_compliant: true
wave_0_complete: false
created: 2026-07-21
---

# Phase 2 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Vitest (browser mode / Playwright Chromium) |
| **Config file** | `frontend/vitest.config.ts` |
| **Quick run command** | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/NoteRefinement.layoutGeneration.cancel.spec.ts` |
| **Full suite command** | `CURSOR_DEV=true nix develop -c pnpm frontend:verify` |
| **Estimated runtime** | ~10–30 seconds focused; full frontend verification under 120s feedback budget |

---

## Sampling Rate

- **After every task commit:** Run the focused cancel spec above (Plan 03 Task 2 also runs the frontend-api.mdc phrase `rg` checks).
- **After every plan wave:** Focused cancel spec + existing NoteRefinement loading specs that prove preview/create/remove were not accidentally made cancelable.
- **Before `/gsd-verify-work`:** `frontend:verify` must be green.
- **Max feedback latency:** 120 seconds.

---

## Per-Task Verification Map

Wave 0 work is Plan 01 (not a separate plan id). Plans: `02-01`, `02-02`, `02-03`.

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 02-01-01 | 01 | 1 | REFN-01, CANC-01 | T-02-01 | Pending-layout mount + Cancel/retry helpers; remove-layout loading stays green | browser component | `pnpm frontend:test tests/components/recall/NoteRefinement.removeLayout.loading.spec.ts` | ✅ support exists; helpers pending | ⬜ pending |
| 02-01-02 | 01 | 1 | REFN-01–02, CANC-01–04 | T-02-02 / T-02-03 | Failing cancel suite locks pending message+Cancel, silent cancel, concurrent survivor, empty retry | browser component | quick run above (expect RED until Plan 02) | ❌ W0 | ⬜ pending |
| 02-02-01 | 02 | 2 | REFN-01, CANC-01–03 | T-02-01 / T-02-02 | `loadRefinementLayout` opts into cancelable blocker + status narrowing | browser component | quick run above | ❌ until product | ⬜ pending |
| 02-02-02 | 02 | 2 | REFN-02 | T-02-04 | Empty/cancelled panel + `retry-refinement-layout`; dialog stays open | browser component | quick run above | ❌ until product | ⬜ pending |
| 02-03-01 | 03 | 3 | CANC-04, REFN-02 | T-02-03 / T-02-04 | Concurrent older-blocker survival + retry-interrupt edges green; extract/remove not cancelable | browser component + rg exclusivity | `pnpm frontend:test` cancel + removeLayout.loading + extractNote specs; `rg "cancelable:\\s*true" frontend/src` only NoteRefinement.vue + clientSetup.ts | ❌ until product | ⬜ pending |
| 02-03-02 | 03 | 3 | — (docs) | T-02-01 | `frontend-api.mdc` documents cancelable overload, status narrowing, silent cancel, identity-bound Cancel, client-only abort | docs grep | `rg` phrases in Plan 03 Task 2 `<verify>` | ✅ rule exists | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

Prep for Plan 01 (Wave 0 / TDD RED — not a separate plan):

- [ ] Create `frontend/tests/components/recall/NoteRefinement.layoutGeneration.cancel.spec.ts` covering REFN-01/02 and CANC-01–04 product outcomes.
- [ ] Extend `frontend/tests/components/recall/noteRefinementTestSupport.ts` with pending-layout mount, Cancel click, and retry-layout helpers.
- [ ] Optionally mock `vue-toastification` in the cancel spec to assert CANC-03 silence.
- [ ] Framework install: none — existing Vitest browser stack is sufficient.

---

## Manual-Only Verifications

All Phase 2 behaviors have automated verification via Vitest browser-mode product specs. Targeted Cypress is optional and not required for the phase gate.

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 120s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
