---
phase: 12
slug: resolve-push-dry-run-story-5
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-08-03
---

# Phase 12 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Vitest (CLI units); Cypress + cucumber (CLI E2E) |
| **Config file** | `cli/vitest.config.ts`; `e2e_test/config/ci.ts` |
| **Quick run command** | `CURSOR_DEV=true nix develop -c bash -c 'cd cli && pnpm exec vitest run tests/previewPush.test.ts'` |
| **Full suite command** | Units above + `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_push_dry_run.feature` |
| **Estimated runtime** | ~30–90 seconds (units); E2E depends on SUT |

---

## Sampling Rate

- **After every task commit:** Run quick vitest on `previewPush.test.ts` (and any new helper test file)
- **After every plan wave:** Run units + targeted `cli_push_dry_run.feature`
- **Before `/gsd-verify-work`:** Targeted CLI push dry-run E2E must be green (not full E2E suite)
- **Max feedback latency:** ~90 seconds for units

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 12-01-01 | 01 | 1 | PUSH-01 | T-12-01 | Dry-run does not write sync metadata; no mutate push | unit + e2e | vitest `previewPush.test.ts` + cypress `cli_push_dry_run.feature` | ✅ flip | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

Existing infrastructure covers the phase. Execution tasks must:

- [ ] Flip/remove units that assert dry-run writes baseline (`seeds…`, `advances…`, `keeps the baseline…`)
- [ ] Reseed directional units via `savePushBaseline` or export — not priming `preview(...)`
- [ ] Flip unit `leaves a note missing from the workspace out of the report` → remote-only create
- [ ] Add units: local-only create; create vs update; conflict ≠ update; dry-run does not create/alter `.doughnut-sync`
- [ ] E2E: invert Feature blurb + *The preview's only addition is its own baseline file*
- [ ] E2E: Rule *A later preview…* Background — export (or pull) prime instead of dry-run
- [ ] E2E: add create (local-only and/or remote-only) scenarios; keep conflict + `.md`/Doughnut non-mutation

---

## Manual-Only Verifications

All phase behaviors have automated verification.

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 90s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
