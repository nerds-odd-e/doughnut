---
phase: 10
slug: resolve-incremental-pull-story-3
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-08-03
---

# Phase 10 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Vitest (CLI units); Cypress + cucumber (CLI E2E) |
| **Config file** | `cli/vitest.config.ts`; `e2e_test/config/ci.ts` |
| **Quick run command** | `CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/applyPull.test.ts` |
| **Full suite command** | Units above + `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_sync_pull.feature` |
| **Estimated runtime** | ~30–120 seconds (units); E2E depends on SUT |

---

## Sampling Rate

- **After every task commit:** Run quick vitest `applyPull.test.ts`
- **After plan complete:** applyPull units + targeted `cli_sync_pull.feature` E2E
- **Before `/gsd-verify-work`:** Targeted CLI E2E `cli_sync_pull.feature` green
- **Max feedback latency:** ~120 seconds for units

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 10-01-T2 | 01 | 1 | EXP-03 | T-10-01 | Classify→write only safe paths; reject never written | unit | `vitest run tests/applyPull.test.ts` | ✅ extend | ⬜ pending |
| 10-01-T2 | 01 | 1 | EXP-03 | T-10-02 | Baseline write only after mutate ≥1; no churn on no-op | unit | applyPull units (baseline cases) | ✅ extend | ⬜ pending |
| 10-01-T3 | 01 | 1 | EXP-03 | — | Integration create flip + update + local-only + no-op + baseline | e2e | `cypress run --spec e2e_test/features/cli/cli_sync_pull.feature` | ✅ extend | ⬜ pending |
| — | — | — | HYG-02 | — | No Terry/YS rewrites of classify | process | Diff excludes `previewPullActions.ts` | manual | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] Unit: create remote-only note (invert anti-create)
- [ ] Unit: update intersecting path
- [ ] Unit: identity move (write new + unlink old)
- [ ] Unit: reject reserved / duplicate — reported, not written
- [ ] Unit: baseline written after mutate; untouched on no-op / rejects-only
- [ ] Unit: local-only preserved; `@perfSync` budget still holds
- [ ] E2E: invert `No new local file for a remote-only note` → create proof
- [ ] E2E: keep update, local-only, no-op, `@perfSync`; add baseline mutate/no-op where feasible
- [ ] During Task 2 (before Task 3 E2E invert): tag the anti-create E2E scenario `@wip` so CI does not fail mid-plan

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| No Terry Yin / Tan Yeong Sheng rewrites | HYG-02 | Authorship gate | Review phase diff; do not edit `cli/src/sync/previewPullActions.ts` |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 120s for units
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
