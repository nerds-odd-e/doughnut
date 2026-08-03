---
phase: 11
slug: resolve-workspace-lint-story-4
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-08-03
---

# Phase 11 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Vitest (CLI units); Cypress + cucumber (CLI E2E) |
| **Config file** | `cli/vitest.config.ts`; `e2e_test/config/ci.ts` |
| **Quick run command** | `CURSOR_DEV=true nix develop -c bash -c 'cd cli && pnpm exec vitest run tests/lintWorkspace.test.ts'` |
| **Full suite command** | Units above + `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_lint_workspace.feature` |
| **Estimated runtime** | ~30–90 seconds (units); E2E depends on SUT |

---

## Sampling Rate

- **After every task commit:** Run quick vitest on `lintWorkspace.test.ts` (and any new helper test file)
- **After every plan wave:** Run units + targeted `cli_lint_workspace.feature`
- **Before `/gsd-verify-work`:** Targeted CLI lint E2E must be green (not full E2E suite)
- **Max feedback latency:** ~90 seconds for units

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 11-01-01 | 01 | 1 | LINT-01 | — | Read-only lint; no Terry/YS rewrites | unit | vitest `lintWorkspace.test.ts` | ✅ | ⬜ pending |
| 11-01-02 | 01 | 1 | LINT-01 | — | E2E proofs for four gaps + conformant success | e2e | cypress `cli_lint_workspace.feature` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

Existing infrastructure covers the phase. Execution tasks must:

- [ ] Invert unit: broken local link → error (D-03)
- [ ] Invert unit: missing `index.md` → error (D-03)
- [ ] Add units: duplicate `doughnut_id`; unsafe path; wiki broken target
- [ ] Cascade `index.md` into CONFORMS fixtures
- [ ] Fix E2E conformant workspace (`banana.md` + indexes)
- [ ] Add E2E scenarios: duplicate id, broken link, missing index, unsupported/unsafe path

*No new test framework install required.*

---

## Manual-Only Verifications

All phase behaviors have automated verification.

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 90s for units
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
