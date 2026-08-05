---
phase: 10
slug: overlap-alias-append-util
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-08-05
---

# Phase 10 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
> Structure-only: util + Vitest; no E2E / no UI.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Vitest 4.x (frontend) |
| **Config file** | frontend Vitest config (existing) |
| **Quick run command** | `CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/utils/appendOverlapWikiLinkToNoteContent.spec.ts` |
| **Full suite command** | Same targeted util spec (Structure; no E2E this phase) |
| **Estimated runtime** | ~15–60 seconds (targeted) |

---

## Sampling Rate

- **After every task commit:** Run quick Vitest command above
- **After every plan wave:** Same util spec green
- **Before phase close:** Util green; no UI/dialog diffs; accidental-match / OVERLAP chrome untouched
- **Max feedback latency:** ~60 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 10-01-01 | 01 | 1 | — (Structure) | T-10-01 | Wiki-link token via named util (not plain alias) | unit | Quick run above | ❌ Wave 0 TDD creates | ⬜ pending |
| 10-01-02 | 01 | 1 | — (Structure) | T-10-01 / T-10-02 | Merge/null/cross-NB/mixed; no UI scope creep | unit | Quick run above | ✅ after Task 1 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] Create `frontend/tests/utils/appendOverlapWikiLinkToNoteContent.spec.ts` (TDD tracer Task 1)
- [ ] Create `frontend/src/utils/appendOverlapWikiLinkToNoteContent.ts` (TDD tracer Task 1)

No separate Wave 0 plan — tracer creates both files.

---

## Manual-Only Verifications

None — Structure; UI unchanged verified by diff fence (D-07 / UI-SPEC covered).

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 120s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
