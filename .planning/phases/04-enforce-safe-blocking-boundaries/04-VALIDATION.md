---
phase: 4
slug: enforce-safe-blocking-boundaries
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-07-21
---

# Phase 4 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Vitest 4.x (browser mode / Playwright Chromium) |
| **Config file** | `frontend/vitest.config.ts` |
| **Quick run command** | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/NoteRefinement.extractionPreview.cancel.edges.spec.ts` |
| **Full suite command** | `CURSOR_DEV=true nix develop -c pnpm frontend:verify` |
| **Estimated runtime** | ~60–180 seconds (quick); longer for verify |

---

## Sampling Rate

- **After every task commit:** Run quick create-boundary / cohesion Vitest (or allowlist `rg` for docs-only tasks)
- **After every plan wave:** Create Cancel-absent + extractNote success + layout cancel + preview cancel + allowlist check
- **Before `/gsd-verify-work`:** `frontend:verify` must be green
- **Max feedback latency:** ~180 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| TBD | TBD | TBD | REFN-05 | — | Create blocker has no Cancel | browser | edges create-note case | ✅ strengthen | ⬜ pending |
| TBD | TBD | TBD | REFN-05 | — | Successful create navigates once | browser | extractNote happy-path | ✅ | ⬜ pending |
| TBD | TBD | TBD | COHE-02 | — | Layout+preview remain cancelable | browser | layout + preview cancel suites | ✅ | ⬜ pending |
| TBD | TBD | TBD | COHE-02 | — | Inventory in frontend-api.mdc | docs | Read rule | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | COHE-02 | — | Only allowed cancelable sites | static/Vitest | `rg` or allowlist | ⚠️ optional | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] Extend `.cursor/rules/frontend-api.mdc` with full classification inventory (from RESEARCH)
- [ ] Promote/strengthen create-note Cancel-absent as explicit REFN-05 proof
- [ ] Optional: allowlist guard for production `cancelable: true`
- [ ] Framework install: none — existing Vitest browser stack is sufficient

---

## Manual-Only Verifications

All phase behaviors have automated verification (or docs review for the inventory table).

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 180s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
