---
phase: 13
slug: resolve-safe-push-story-6
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-08-03
---

# Phase 13 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Vitest (CLI units); Cypress + cucumber (CLI E2E, dry-run non-regression only) |
| **Config file** | `cli/vitest.config.ts`; `e2e_test/config/ci.ts` |
| **Quick run command** | `CURSOR_DEV=true nix develop -c bash -c 'cd cli && pnpm exec vitest run tests/pushArgument.test.ts'` |
| **Full suite command** | Units above (+ optional `previewPush.test.ts`) + `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_push_dry_run.feature` |
| **Estimated runtime** | ~10–30 seconds (units); E2E depends on SUT |

---

## Sampling Rate

- **After every task commit:** Run quick vitest on `pushArgument.test.ts` (and confirm `cli_push.feature` absent)
- **After every plan wave:** Units + targeted `cli_push_dry_run.feature` (non-regression of Phase 12 surface)
- **Before `/gsd-verify-work`:** Absence proofs + dry-run non-regression green (not full E2E suite)
- **Max feedback latency:** ~30 seconds for units

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 13-01-01 | 01 | 1 | PUSH-02 | T-13-01 | No mutate push; WIP E2E gone; dry-run still requires `--dry-run` | unit + absence + e2e smoke | vitest `pushArgument.test.ts` + `test ! -f cli_push.feature` + cypress `cli_push_dry_run.feature` | ✅ existing | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

Existing infrastructure covers the phase. Execution tasks must:

- [ ] `trash` (prefer) `e2e_test/features/cli/cli_push.feature`
- [ ] Confirm no Story-6-only orphan glue remains
- [ ] Optional D-04: durable dry-run-only help copy (no “so far” mutate promise)
- [ ] Prove: file absent; no `@ignore` Story 6 E2E under `features/cli/`; `parsePushArgument` still requires `--dry-run`; no `applyPush`
- [ ] Non-regression: `pushArgument.test.ts` (+ optional dry-run E2E)
- [ ] Mark PUSH-02 **removed cleanly** in REQUIREMENTS/ROADMAP/STATE

---

## Manual-Only Verifications

All phase behaviors have automated verification (absence checks + units ± targeted dry-run E2E).

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
