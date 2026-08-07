---
phase: 01-commissioned-tracker-model
fixed_at: 2026-08-07T22:05:00Z
review_path: .planning/phases/01-commissioned-tracker-model/01-REVIEW.md
iteration: 1
findings_in_scope: 2
fixed: 2
skipped: 0
status: all_fixed
---

# Phase 1: Code Review Fix Report

**Fixed at:** 2026-08-07T22:05:00Z
**Source review:** `.planning/phases/01-commissioned-tracker-model/01-REVIEW.md`
**Iteration:** 1

**Summary:**
- Findings in scope: 2 (WR-01, WR-02; Info deferred)
- Fixed: 2
- Skipped: 0

**Verification:** Targeted `AssimilationControllerTests` ran in the isolated review-fix worktree via `CURSOR_DEV=true nix develop -c backend/gradlew …` (both findings green before commit).

## Fixed Issues

### WR-01: Daily assimilation selection still includes COMMISSIONED

**Files modified:** `backend/src/main/java/com/odde/doughnut/entities/repositories/MemoryTrackerRepository.java`, `backend/src/test/java/com/odde/doughnut/controllers/AssimilationControllerTests.java`
**Commit:** `6c91c27902`
**Applied fix:** Added `AND rp.type <> 'COMMISSIONED'` to `findAllByUserAndAssimilatedAtGreaterThan`. Added controller-boundary test `assimilatedCountOfTheDayExcludesCommissionedTrackers` proving COMMISSIONED assimilated today does not inflate `assimilatedCountOfTheDay`.

### WR-02: Assimilation queue join excludes COMMISSIONED, but create still treats it as assimilated

**Files modified:** `backend/src/main/java/com/odde/doughnut/services/MemoryTrackerAssimilation.java`, `backend/src/test/java/com/odde/doughnut/controllers/AssimilationControllerTests.java`
**Commit:** `3b58ca520e`
**Status:** fixed: requires human verification
**Applied fix:** When deciding ordinary note-level existence, ignore `MemoryTrackerType.COMMISSIONED` so a commissioned-only note can receive UNDERSTANDING via ordinary assimilate. Added controller-boundary test `assimilatingCommissionedOnlyNoteCreatesUnderstandingAndLeavesCommissioned`.

## Deferred (out of scope)

### IN-01: SC3 assertion does not pin which due tracker is returned
**Reason:** Info finding; `fix_scope` is critical_warning only.

### IN-02: Native `'COMMISSIONED'` literals diverge from JPQL constants
**Reason:** Info finding; acceptable as-is per review; not in scope.

---

_Fixed: 2026-08-07T22:05:00Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
