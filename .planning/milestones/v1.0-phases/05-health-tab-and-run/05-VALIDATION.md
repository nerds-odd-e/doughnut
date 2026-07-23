---
phase: 5
slug: health-tab-and-run
status: draft
nyquist_compliant: true
wave_0_complete: false
created: 2026-07-22
---

# Phase 5 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Vitest (frontend) + Cypress/Cucumber (E2E) |
| **Config file** | `frontend` vitest config; `e2e_test/config/ci.ts` (`not @wip` in CI) |
| **Quick run command** | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/NotebookPageView.spec.ts tests/components/notebook/NotebookHealthPanel.spec.ts` |
| **Full suite command** | `CURSOR_DEV=true nix develop -c pnpm frontend:test` (unit); targeted E2E only for phase |
| **Estimated runtime** | Unit: ~30–60s targeted; E2E: ~1–3 min per feature |

---

## Sampling Rate

- **After every task commit:** Targeted frontend specs for touched files
- **After every plan wave:** All new/updated Health frontend specs green
- **Before phase seal:** Frontend Health specs green; E2E `@wip` removed only when scenarios pass
- **Max feedback latency:** Prefer targeted runs; do **not** run full E2E suite

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|----------------|-----------------|-----------|-------------------|-------------|--------|
| 05-01-01 | 01 | 1 | HLTH-01, HLTH-02, AFIX-01 | T-05-01 | RED unit tests: Health tab notebook-only; idle; no API on open; no Fix | unit | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/NotebookPageView.spec.ts tests/components/notebook/NotebookHealthPanel.spec.ts tests/pages/FolderPage.healthTab.spec.ts` | ❌ W0 | ⬜ pending |
| 05-01-02 | 01 | 1 | HLTH-01, HLTH-02, AFIX-01 | T-05-01, T-05-02 | GREEN: tab shell + Run lint bodyless; checkbox UI-only | unit | same as 05-01-01 | ❌ W0 | ⬜ pending |
| 05-02-01 | 02 | 2 | HLTH-03 | T-05-03 | Expandable wire-shape findings; no v-html | unit | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/notebook/NotebookHealthPanel.spec.ts tests/pages/NotebookPageView.spec.ts tests/pages/FolderPage.healthTab.spec.ts` | ❌ W0 | ⬜ pending |
| 05-02-02 | 02 | 2 | HLTH-03, AFIX-01 | T-05-01, T-05-02 | E2E Run→groups; checkbox on no mutate; landmarks | E2E | `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/notebooks/notebook_health.feature` and `… workspace_surface_landmarks.feature` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `frontend/tests/components/notebook/NotebookHealthPanel.spec.ts` — idle, Run, findings, no API on mount, checkbox UI-only, no Fix
- [ ] Extend `frontend/tests/pages/NotebookPageView.spec.ts` — Health tab present; panel on Health; settings not shown
- [ ] `frontend/tests/pages/FolderPage.healthTab.spec.ts` — Health tab absent
- [ ] `e2e_test/features/notebooks/notebook_health.feature` — `@wip` until green
- [ ] Page object methods on `notebookPage` + landmark updates for Health tab
- [ ] `workspace_surface_landmarks.feature` / steps updated for notebook Health presence / folder Health absence

*Existing Vitest + Cypress infrastructure — only new Health specs / E2E feature needed.*

---

## Manual-Only Verifications

| Check | When |
|-------|------|
| Long finding lists scrollable; long labels wrap (UI overflow backstop) | After 05-02 Task 1 if fixtures available |

All core phase behaviors have automated verification.

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency documented; full E2E suite not required
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** pending execution
