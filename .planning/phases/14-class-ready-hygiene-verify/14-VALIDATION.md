---
phase: 14
slug: class-ready-hygiene-verify
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-08-03
---

# Phase 14 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Vitest (CLI) + Cypress 15.x + cucumber preprocessor |
| **Config file** | `cli/vitest.config.ts`; `e2e_test/config/ci.ts` |
| **Quick run command** | `CURSOR_DEV=true nix develop -c pnpm cli:test` |
| **Full suite command** | `pnpm cli:test` + five retained CLI E2E features (not full Cypress) |
| **Estimated runtime** | ~3–15 minutes (units fast; E2E multi-minute) |

---

## Sampling Rate

- **After every task commit:** Run full HYG-03 matrix (`cli:test` + five E2E features) — single-task phase
- **After every plan wave:** Same (one wave)
- **Before `/gsd-verify-work`:** HYG matrix green + WIP scan + HYG-02 audit recorded
- **Max feedback latency:** ~900 seconds (E2E-bound)

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 14-01-01 | 01 | 1 | HYG-01 | T-14-01 | Spent docs gone; no Story 1–6 WIP tags/features | other | filesystem + `rg` WIP scan | ✅ | ⬜ pending |
| 14-01-01 | 01 | 1 | HYG-02 | T-14-02 | No rewrite of Terry/YS surfaces this phase | other | git log/blame + SUMMARY audit | ✅ | ⬜ pending |
| 14-01-01 | 01 | 1 | HYG-03 | — | Retained units green | unit | `CURSOR_DEV=true nix develop -c pnpm cli:test` | ✅ | ⬜ pending |
| 14-01-01 | 01 | 1 | HYG-03 | — | Retained E2E green | e2e | `pnpm cypress run --spec` five features | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

Existing infrastructure covers all phase requirements — no new test stubs required.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| HYG-02 author/file audit table in SUMMARY/VERIFICATION | HYG-02 | git blame/log evidence is documentary | Record protected-file audit per CONTEXT D-05 |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency acceptable for E2E-bound phase
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
