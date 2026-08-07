---
phase: 01
slug: commissioned-tracker-model
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-08-07
---

# Phase 01 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
> Foundation: quick 006 shipped `MemoryTrackerType` — Phase 1 is filter-only (no boolean column).

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Spring Boot `@SpringBootTest` / `@Transactional` |
| **Config file** | Spring `test` profile (existing controller tests) |
| **Quick run command** | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` |
| **Full suite command** | `CURSOR_DEV=true nix develop -c pnpm backend:verify` |
| **Estimated runtime** | ~60–180 seconds |

---

## Sampling Rate

- **After every task commit:** Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- **After every plan wave:** Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only` (or `backend:verify` at phase gate)
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** ~180 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 01-01-T1 | 01 | 1 | SC3 | T-01-02 | Literal `type <> 'COMMISSIONED'` + `@Param` only | unit | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` | ❌ W0 | ⬜ pending |
| 01-01-T2 | 01 | 1 | SC2 | T-01-03 | UK on type already (006); do not weaken | unit | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` | ✅ | ⬜ pending |
| 01-02-T1 | 02 | 2 | Phase2-ready queue | T-01-02 | JPQL enum filter, no user concat | unit | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` | ❌ W0 | ⬜ pending |
| 01-02-T2 | 02 | 2 | Phase2-ready batch | T-01-02 | Literal type filter in batch SQL | unit | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` | ❌ W0 | ⬜ pending |
| 01-02-T3 | 02 | 2 | SC1 | — | N/A | unit | `CURSOR_DEV=true nix develop -c pnpm backend:verify` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `RecallsControllerTests` — due ordinary + due commissioned → `toRepeat` size 1 (SC3)
- [ ] Assimilation queue — commissioned-only note still unassimilated for ordinary path
- [ ] `QuestionGenerationBatchLocalPlanningTest` — commissioned due tracker not in planned batch
- [x] `MemoryTrackerBuilder.commissioned()` — already exists (006)
- [x] Coexistence persist test — already exists
- [x] Framework / Flyway tip — no new migration; tip `V300000239`

*Existing infrastructure covers framework; Wave 0 is missing SC3 / queue / batch assertions only.*

---

## Manual-Only Verifications

All phase behaviors have automated verification.

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 180s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
