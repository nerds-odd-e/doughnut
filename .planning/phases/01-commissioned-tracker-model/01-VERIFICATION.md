---
phase: 01-commissioned-tracker-model
verified: 2026-08-07T15:26:54Z
status: passed
score: 6/6 must-haves verified
behavior_unverified: 0
overrides_applied: 0
re_verification: false
---

# Phase 1: Commissioned tracker model Verification Report

**Phase Goal:** Persist a commissioned memory tracker variant and keep it out of ordinary due-recall selection, without changing any user-visible path yet.

**Verified:** 2026-08-07T15:26:54Z  
**Status:** passed  
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
| --- | ------- | ---------- | -------------- |
| 1 | SC1: Existing assimilation and recall backend unit suites still pass (`pnpm backend:verify`) | ✓ VERIFIED | `CURSOR_DEV=true nix develop -c pnpm backend:verify` exited 0 (2026-08-07); full backend unit suite green |
| 2 | SC2: Domain model can represent a commissioned memory tracker coexisting with ordinary trackers on the same note | ✓ VERIFIED | `MemoryTrackerType.COMMISSIONED` enum; UK tip `V300000239`; `AssimilationControllerTests#understandingAndCommissionedTrackersCanCoexistOnSameNote` asserts `findByUserAndNote` size 2 — targeted suite green |
| 3 | SC3: Due-recall selection never returns commissioned trackers when ordinary + commissioned are due | ✓ VERIFIED | `byUserIdFrom` has `AND rp.type <> 'COMMISSIONED'`; `RecallsControllerTests#shouldExcludeCommissionedMemoryTrackersFromOrdinaryRecallLists` asserts `getToRepeat()` size 1 via `controller.recalling` |
| 4 | Ordinary-only `totalAssimilatedCount`: `countByUserNotRemoved` / recalling count excludes COMMISSIONED (shared `byUserIdFrom`) | ✓ VERIFIED | Same SC3 test asserts `totalAssimilatedCount == 1`; `RecallService.totalAssimilatedCount` → `countByUserNotRemoved` uses `byUserIdFrom` |
| 5 | A note with only a commissioned tracker still appears in the ordinary assimilation queue | ✓ VERIFIED | `joinMemoryTracker` uses `JPA_WHERE_NOT_COMMISSIONED_TRACKER`; `AssimilationControllerTests$Next#commissionedOnlyNoteStillAppearsInOrdinaryAssimilationQueue` asserts next unit + unassimilated count 1 |
| 6 | Batch question-generation candidates never include due COMMISSIONED trackers | ✓ VERIFIED | Native `AND mt.type <> 'COMMISSIONED'` beside SPELLING; `QuestionGenerationBatchCandidateMemoryTrackersTest#excludesCommissionedTracker` asserts id absent from candidates |

**Score:** 6/6 truths verified (0 present, behavior-unverified)

### Required Artifacts

| Artifact | Expected | Status | Details |
| -------- | ----------- | ------ | ------- |
| `MemoryTrackerRepository.java` | `byUserIdFrom` + batch exclude COMMISSIONED | ✓ VERIFIED | Literal filters on due/count fragment and `findBatchQuestionGenerationCandidatesByUser`; `byUserIdWhere` / `findByUserAndNote` / `findLast100*` intentionally unfiltered |
| `RecallsControllerTests.java` | SC3 controller-boundary proof | ✓ VERIFIED | `shouldExcludeCommissionedMemoryTrackersFromOrdinaryRecallLists` |
| `NoteRepository.java` | `joinMemoryTracker` ignores COMMISSIONED | ✓ VERIFIED | Appends `MemoryTracker.JPA_WHERE_NOT_COMMISSIONED_TRACKER` |
| `NotePropertyIndexRepository.java` | Target-note gate ignores COMMISSIONED | ✓ VERIFIED | `tmtBlock` uses `JPA_WHERE_NOT_COMMISSIONED_TARGET_TRACKER` |
| `MemoryTracker.java` | Shared JPQL NOT_COMMISSIONED fragments | ✓ VERIFIED | `JPA_WHERE_NOT_COMMISSIONED_TRACKER` / `_TARGET_TRACKER` |
| `AssimilationControllerTests.java` | Coexistence + queue proofs | ✓ VERIFIED | Coexistence + `commissionedOnlyNoteStillAppearsInOrdinaryAssimilationQueue` |
| `QuestionGenerationBatchCandidateMemoryTrackersTest.java` | Batch exclusion proof | ✓ VERIFIED | `excludesCommissionedTracker` |

### Key Link Verification

| From | To | Via | Status | Details |
| ---- | --- | --- | ------ | ------- |
| `RecallsController.recalling` | `findAllByUserAndNextRecallAtLessThanEqualOrderByNextRecallAt` | `RecallService` → `UserService.getMemoryTrackersNeedToRepeat` → `byUserIdFrom` | ✓ WIRED | Controller calls `recallService.getDueMemoryTrackers`; due stream + count both use filtered fragment. gsd-tools path probe false-negative (conceptual `from`); manual trace confirms |
| `AssimilationController.next` / `getUnassimilatedNotes` | `NoteRepository.joinMemoryTracker` | `AssimilationService` → `UserService.getUnassimilatedNotes` → `findByOwnershipWhereThereIsNoMemoryTracker` | ✓ WIRED | Ordinary queue uses join with COMMISSIONED exclusion; controller test proves commissioned-only notes remain unassimilated |
| `QuestionGenerationBatchPlanningService` | `findBatchQuestionGenerationCandidatesByUser` | `findCandidateMemoryTrackersForBatchGeneration` | ✓ WIRED | Service method returns repository query; unit test drives candidate ids |

### Data-Flow Trace (Level 4)

Structure/query phase — no UI render surfaces. Filters operate on real DB rows via native/JPQL repositories; tests persist COMMISSIONED trackers with `makeMe` and assert selection outcomes.

| Artifact | Data Variable | Source | Produces Real Data | Status |
| -------- | ------------- | ------ | ------------------ | ------ |
| Due recall list | `toRepeat` / `totalAssimilatedCount` | `memory_tracker` via `byUserIdFrom` | Yes — persisted fixtures | ✓ FLOWING |
| Assimilation next | `nextUnit` / unassimilated counts | notes left-join ordinary trackers | Yes — commissioned-only fixture still queued | ✓ FLOWING |
| Batch candidates | candidate tracker ids | `findBatchQuestionGenerationCandidatesByUser` | Yes — due commissioned id excluded | ✓ FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
| -------- | ------- | ------ | ------ |
| SC1 full backend suites | `CURSOR_DEV=true nix develop -c pnpm backend:verify` | exit 0, BUILD SUCCESSFUL | ✓ PASS |
| SC2/SC3/queue/batch named suites | `pnpm backend:test_only -- --tests …RecallsControllerTests --tests …AssimilationControllerTests --tests …QuestionGenerationBatchCandidateMemoryTrackersTest` | exit 0, BUILD SUCCESSFUL | ✓ PASS |

### Probe Execution

| Probe | Command | Result | Status |
| ----- | ------- | ------ | ------ |
| — | — | No phase-declared or migration probes | SKIPPED (N/A) |

### Requirements Coverage

Phase plans declare `requirements: []`. ROADMAP: *(none user-facing; unlocks TRK-\*)*. TRK-01..03 remain Pending for later phases — not Phase 1 deliverables.

| Requirement | Source Plan | Description | Status | Evidence |
| ----------- | ---------- | ----------- | ------ | -------- |
| *(none claimed)* | 01-01, 01-02 | Structure unlock only | N/A | Unlocks TRK-\*; no orphaned Phase-1 REQ IDs in plans |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
| ---- | ---- | ------- | -------- | ------ |
| — | — | No TBD/FIXME/XXX in phase-touched production/test files | — | — |

**Structure stop-safe checks:**

- Tip migration unchanged: `V300000239` (no new Flyway this phase)
- No frontend `commissioned` references
- `AssimilationRequestDTO` unchanged (no assimilate-as-commissioned create path)
- `byUserIdWhere` / `findByUserAndNote` / `findLast100*` remain unfiltered (coexistence + history paths intact)

### Human Verification Required

None. Structure phase; all must-haves have passing unit-test behavioral evidence. E2E N/A per ROADMAP (no product path touched).

### Gaps Summary

No gaps. Phase goal achieved: commissioned trackers persist (type enum + UK from quick 006), are excluded from ordinary due-recall / assimilated counts / assimilation-join detection / batch candidates, and do not yet change any user-visible create path.

---

_Verified: 2026-08-07T15:26:54Z_  
_Verifier: Claude (gsd-verifier)_
