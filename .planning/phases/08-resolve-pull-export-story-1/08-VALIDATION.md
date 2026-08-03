---
phase: 8
slug: resolve-pull-export-story-1
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-08-03
---

# Phase 8 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
> Seeded from `08-RESEARCH.md` ## Validation Architecture.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (backend) + Vitest (CLI units) + Cypress/Cucumber (E2E) |
| **Config file** | `backend/build.gradle` / `e2e_test/config/ci.ts` |
| **Quick run command** | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` |
| **Full suite command** | Backend tests + `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_export.feature` |
| **Estimated runtime** | ~60–180 seconds (targeted) |

---

## Sampling Rate

- **After every task commit:** Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only` (or focused CLI vitest if CLI glue touched)
- **After every plan wave:** Backend tests + `cli_export.feature` Cypress spec
- **Before `/gsd-verify-work`:** Targeted suite above must be green (not full E2E unless CI/user requires)
- **Max feedback latency:** ~180 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 08-01-* | 01 | 1 | EXP-01 | T-08-* | No secrets in workspace; fail closed | unit | `pnpm backend:test_only` | ✅ extend | ⬜ pending |
| 08-02-* | 02 | 2 | EXP-01 | T-08-* | Export proves identity/links/attachments | e2e | `cypress run --spec …/cli_export.feature` | ✅ extend | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

Planner must expand rows to concrete task IDs when writing PLAN.md.

---

## Wave 0 Requirements

- [ ] Extend `NotebookZipBuilderTest` — identity merge (with and without author FM)
- [ ] Extend `NotebookZipBuilderTest` — wiki relative link + unresolved fallback + nested relativize
- [ ] Extend `NotebookZipBuilderTest` — attachment absolute URL; zip has no attachment entries
- [ ] Extend `cli_export.feature` — scenarios for `doughnut_id`, ordinary MD link, absolute attachment URL (`@wip` until green)

*Existing infrastructure covers frameworks; Wave 0 is test-case gaps, not new tooling.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Terry/YS hunks untouched | HYG-02 | Author attribution | At Jidoka: review diff authors; do not rewrite Terry Yin / Tan Yeong Sheng owned hunks |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 180s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
