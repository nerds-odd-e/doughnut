---
phase: 3
slug: cancel-extraction-preview-generation
status: draft
nyquist_compliant: true
wave_0_complete: false
created: 2026-07-21
---

# Phase 3 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Vitest (browser mode / Playwright Chromium) |
| **Config file** | `frontend/vitest.config.ts` |
| **Quick run command** | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/NoteRefinement.extractionPreview.cancel.spec.ts` |
| **Full suite command** | `CURSOR_DEV=true nix develop -c pnpm frontend:verify` |
| **Estimated runtime** | ~10–30 seconds focused; full frontend verification under 120s feedback budget |

---

## Sampling Rate

- **After every task commit:** Run the focused extraction-preview cancel spec above.
- **After every plan wave:** Cancel spec + `NoteRefinement.extractNote.spec.ts` + layout cancel suite (ensure layout path untouched).
- **Before `/gsd-verify-work`:** `frontend:verify` must be green.
- **Max feedback latency:** 120 seconds.

---

## Per-Task Verification Map

Mapped to plans 03-01 / 03-02 / 03-03.

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 03-W0 | 01 | 1 | REFN-03, REFN-04 | T-03-01 / T-03-02 | Failing cancel suite locks pending Cancel, layout selection preserve, in-preview regenerate cancel, silent toast | browser component | quick run above (expect RED until product) | ❌ W0 | ⬜ pending |
| 03-impl | 02 | 2 | REFN-03, REFN-04 | T-03-01 / T-03-02 / T-03-03 | Cancelable preview call + status narrowing; Extract cancel stays on layout; retry-cancel keeps prior preview | browser component | quick run above | ❌ until product | ⬜ pending |
| 03-edges | 03 | 3 | REFN-04, scope | T-03-03 / T-03-04 | Late resolve ignored; create-note remains noncancelable; layout cancel suite still green | browser component + rg | quick run + extractNote + layout cancel; `rg "cancelable:\\s*true"` exclusivity | ❌ until product | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] Create `frontend/tests/components/recall/NoteRefinement.extractionPreview.cancel.spec.ts` covering REFN-03/04 (Extract cancel + retry-path cancel + late apply + silent toast).
- [ ] Extend `noteRefinementExtractionTestSupport.ts` (or layout-loading support) with pending-preview helpers: layout ready → select → Extract with deferred `extractNotePreview` gate; reuse `clickLoadingModalCancel` / `loadingModalMask`.
- [ ] Mock `vue-toastification` in the cancel spec (mirror Phase 2) to assert silence at the product seam.
- [ ] Framework install: none — existing Vitest browser stack is sufficient.

---

## Manual-Only Verifications

All Phase 3 behaviors have automated verification via Vitest browser-mode product specs. Targeted Cypress is optional and not required for the phase gate.

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 120s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
