---
phase: 1
slug: commissioned-tracker-model
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-08-07
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
> Structure phase: no user-visible behavior change; prove model + due-recall exclusion.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Spring Boot `@SpringBootTest` / `@Transactional` (backend) |
| **Config file** | Spring `test` profile (existing controller tests) |
| **Quick run command** | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` |
| **Full suite command** | `CURSOR_DEV=true nix develop -c pnpm backend:verify` (when migration involved) |
| **Estimated runtime** | ~60–180 seconds (targeted); verify longer |

---

## Sampling Rate

- **After every task commit:** Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- **After every plan wave:** Run `CURSOR_DEV=true nix develop -c pnpm backend:verify` if migration touched, else `backend:test_only`
- **Before `/gsd-verify-work`:** Backend suite must be green; no new `@wip` E2E
- **Max feedback latency:** 180 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|---------|-----------------|-----------|-------------------|-------------|--------|
| TBD | 01 | 1 | SC2 coexistence | — | N/A (no new public create) | unit | `pnpm backend:test_only` | ❌ W0 | ⬜ pending |
| TBD | 01 | 1 | SC3 due-recall exclude | T-SQL | Parameterized queries only | unit | `pnpm backend:test_only` | ❌ W0 | ⬜ pending |
| TBD | 01 | 1 | SC1 regression | — | Authz paths unchanged | unit | `pnpm backend:test_only` / `backend:verify` | ✅ existing | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] Extend `RecallsControllerTests` — commissioned excluded from `toRepeat` while ordinary remains
- [ ] Coexistence persist test — ordinary + commissioned same note via `makeMe.aMemoryTrackerFor(note).commissioned()`
- [ ] `MemoryTrackerBuilder.commissioned()` helper
- [ ] Flyway migration file version `> 300000237`
- [ ] (Recommended) Assert unassimilated / assimilation queue still sees note when only commissioned tracker exists
- [ ] Framework install: none — existing infrastructure covers the phase

*Existing infrastructure covers framework; Wave 0 is focused tests + migration + makeMe helper.*

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
