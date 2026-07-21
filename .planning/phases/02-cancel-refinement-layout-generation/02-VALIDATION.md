---
phase: 2
slug: cancel-refinement-layout-generation
status: draft
nyquist_compliant: false
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

- **After every task commit:** Run the focused cancel spec above.
- **After every plan wave:** Focused cancel spec + existing NoteRefinement loading specs that prove preview/create/remove were not accidentally made cancelable.
- **Before `/gsd-verify-work`:** `frontend:verify` must be green.
- **Max feedback latency:** 120 seconds.

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 02-W0-01 | 00 | 0 | REFN-01–02, CANC-01–04 | — | Wave 0 failing cancel spec stubs | browser component | quick run above | ❌ W0 | ⬜ pending |
| 02-01-01 | 01 | 1 | REFN-01, CANC-01 | T-02-01 | Pending layout shows blocking message + Cancel only via opt-in | browser component | quick run above | ❌ W0 | ⬜ pending |
| 02-01-02 | 01 | 1 | CANC-02, CANC-03, CANC-04 | T-02-02 / T-02-03 | Cancel aborts only layout request; silent cancel; concurrent blockers intact | browser component | quick run above | ❌ W0 | ⬜ pending |
| 02-02-01 | 02 | 2 | REFN-02 | T-02-04 | After cancel: dialog open, retry available, note content unchanged | browser component | quick run above | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

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
