---
phase: 8
slug: resolve-pull-export-story-1
status: ready
nyquist_compliant: false
wave_0_complete: false
created: 2026-08-03
---

# Phase 8 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
> Seeded from `08-RESEARCH.md` ## Validation Architecture; expanded by planner.

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
| 08-01-01 | 01 | 1 | EXP-01 | T-08-01, T-08-02, T-08-03, T-08-04 | Auth before zip; same-notebook wiki resolve; canonical attachment rewrite; no secrets in zip | unit | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` | ✅ extend | ⬜ pending |
| 08-01-02 | 01 | 1 | EXP-01 | T-08-01, T-08-02 | Unresolved wiki safe; relative paths; no attachment blobs | unit | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` | ✅ extend | ⬜ pending |
| 08-01-03 | 01 | 1 | EXP-01 | T-08-04 | Allowlist HYG-02 / D-07 | unit + review | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` | ✅ | ⬜ pending |
| 08-02-01 | 02 | 2 | EXP-01 | T-08-03 | @wip scenarios seed no credentials | e2e | `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_export.feature` | ✅ extend | ⬜ pending |
| 08-02-02 | 02 | 2 | EXP-01 | T-08-03, T-08-04 | On-disk identity/link/attachment proofs; inventory stays secret-free | e2e | `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_export.feature` | ✅ extend | ⬜ pending |
| 08-02-03 | 02 | 2 | EXP-01 / HYG-02 | T-08-* | Jidoka allowlist + nyquist sign-off | unit + e2e + review | backend:test_only + cli_export.feature | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] Extend `NotebookZipBuilderTest` — identity merge (with and without author FM) — **08-01-01 / 08-01-02**
- [ ] Extend `NotebookZipBuilderTest` — wiki relative link + unresolved fallback + nested relativize — **08-01-01 / 08-01-02**
- [ ] Extend `NotebookZipBuilderTest` — attachment absolute URL; zip has no attachment entries — **08-01-01**
- [ ] Extend `cli_export.feature` — scenarios for `doughnut_id`, ordinary MD link, absolute attachment URL (`@wip` until green) — **08-02-01 / 08-02-02**

*Existing infrastructure covers frameworks; Wave 0 is test-case gaps, not new tooling. Mark checked during execute.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Terry/YS hunks untouched | HYG-02 | Author attribution | At Jidoka (08-02-03): review diff authors; do not rewrite Terry Yin / Tan Yeong Sheng owned hunks |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references (mapped to plan tasks)
- [x] No watch-mode flags
- [x] Feedback latency < 180s
- [ ] `nyquist_compliant: true` set in frontmatter *(set after Wave 0 checkboxes green in execute)*

**Approval:** pending execute
