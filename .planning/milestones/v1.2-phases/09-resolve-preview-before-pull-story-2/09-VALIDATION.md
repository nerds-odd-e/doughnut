---
phase: 9
slug: resolve-preview-before-pull-story-2
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-08-03
---

# Phase 9 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Vitest (CLI units); Cypress + cucumber (CLI E2E) |
| **Config file** | `cli/vitest.config.ts`; `e2e_test/config/ci.ts` |
| **Quick run command** | `CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/previewPull.test.ts` |
| **Full suite command** | Units above + `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_sync_dry_run.feature` (+ `previewPush` units if `diffReport` touched) |
| **Estimated runtime** | ~30–120 seconds (units); E2E depends on SUT |

---

## Sampling Rate

- **After every task commit:** Run quick vitest `previewPull.test.ts` (and helper file if added)
- **After every plan wave:** previewPull + previewPush (if shared `diffReport` touched) units
- **Before `/gsd-verify-work`:** Targeted CLI E2E `cli_sync_dry_run.feature` green
- **Max feedback latency:** ~120 seconds for units

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 09-*-* | 01+ | 1+ | EXP-02 | T-09-01 | Reject unsafe zip paths; never write on dry-run | unit | `vitest run tests/previewPull.test.ts` | ✅ extend | ⬜ pending |
| 09-*-* | 01+ | 1+ | EXP-02 | T-09-02 | No `.doughnut-sync` writes; reject `.doughnut-sync/**` | unit + e2e | previewPull units + dry-run Rule | ✅ keep/extend | ⬜ pending |
| 09-*-* | 02 | 2 | EXP-02 | — | Integration action labels + diagnostics | e2e | `cypress run --spec e2e_test/features/cli/cli_sync_dry_run.feature` | ✅ extend | ⬜ pending |
| — | — | — | HYG-02 | — | No Terry/YS rewrites | process | Review diff authorship / scope | manual | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] Unit cases for **create** / **update** / **move** / **reject** labels (extend `cli/tests/previewPull.test.ts`)
- [ ] Unit case: reserved basename (`log.md` / `index.md`) → reject reason visible
- [ ] Unit case: duplicate zip entry paths via `buildZip` (not `zipOfNotes` Record)
- [ ] Unit case: unsafe path → reject (or documented throw) without workspace write
- [ ] Unit case: rejects-only does **not** return only `No changes to pull.`
- [ ] Unit case: missing `doughnut_id` → no move inference
- [ ] E2E scenarios for at least one action label beyond content overwrite + one reserved/duplicate/invalid finding
- [ ] Extend non-mutation Rule if new scenarios write fixtures

*Existing “changed note / local overwrite / no-diff / non-mutation” coverage remains — update assertions when headings gain action labels.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| No Terry Yin / Tan Yeong Sheng rewrites | HYG-02 | Authorship gate | Review phase diff authors; do not rewrite Terry/YS-owned hunks |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 120s for units
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
