---
phase: 01-commissioned-tracker-model
reviewed: 2026-08-07T22:00:00Z
depth: standard
files_reviewed: 8
files_reviewed_list:
  - backend/src/main/java/com/odde/doughnut/entities/MemoryTracker.java
  - backend/src/main/java/com/odde/doughnut/entities/repositories/MemoryTrackerRepository.java
  - backend/src/main/java/com/odde/doughnut/entities/repositories/NotePropertyIndexRepository.java
  - backend/src/main/java/com/odde/doughnut/entities/repositories/NoteRepository.java
  - backend/src/main/java/com/odde/doughnut/entities/repositories/NoteStructuralPeerQueries.java
  - backend/src/test/java/com/odde/doughnut/controllers/AssimilationControllerTests.java
  - backend/src/test/java/com/odde/doughnut/controllers/RecallsControllerTests.java
  - backend/src/test/java/com/odde/doughnut/services/QuestionGenerationBatchCandidateMemoryTrackersTest.java
findings:
  critical: 0
  warning: 2
  info: 2
  total: 4
status: issues_found
---

# Phase 1: Code Review Report

**Reviewed:** 2026-08-07T22:00:00Z
**Depth:** standard
**Files Reviewed:** 8
**Status:** issues_found

## Summary

Reviewed COMMISSIONED exclusion seams (due-recall `byUserIdFrom`, assimilation `joinMemoryTracker`, property target gate, batch candidates) plus the incidental `NoteStructuralPeerQueries` extract. Intended filters are present, parameterized (no SQLi), and covered by boundary tests. Two warnings remain: ordinary daily-assimilation counting still includes COMMISSIONED, and the new queue join is inconsistent with assimilate create (still treats COMMISSIONED as a blocking note-level tracker).

## Narrative Findings (AI reviewer)

## Warnings

### WR-01: Daily assimilation selection still includes COMMISSIONED

**File:** `backend/src/main/java/com/odde/doughnut/entities/repositories/MemoryTrackerRepository.java:14-21`
**Issue:** `findAllByUserAndAssimilatedAtGreaterThan` has no `type <> 'COMMISSIONED'` filter. `AssimilationService.getNotesAssimilatedToday()` → `UserService.getRecentMemoryTrackers()` uses this query for `assimilatedCountOfTheDay` and subscription daily-cap accounting. Phase 1 made ordinary due-recall / `totalAssimilatedCount` ordinary-only via `byUserIdFrom`, but daily assimilation counts remain mixed. Once Phase 2 (or any path) persists COMMISSIONED with `assimilated_at`, those rows inflate ordinary daily caps and “assimilated today” note-id lists.
**Fix:** Mirror the ordinary-only policy on this query (or extract a shared native fragment used by both due and recent-assimilation paths):

```java
@Query(
    value =
        "SELECT rp.* FROM memory_tracker rp "
            + " WHERE rp.user_id = :userId "
            + "   AND rp.assimilated_at > :since "
            + "   AND rp.removed_from_tracking IS FALSE "
            + "   AND rp.deleted_at IS NULL"
            + "   AND rp.type <> 'COMMISSIONED'",
    nativeQuery = true)
List<MemoryTracker> findAllByUserAndAssimilatedAtGreaterThan(
    @Param("userId") Integer userId, @Param("since") Timestamp since);
```

If product later decides commissioned assimilation *should* consume the daily cap, document that exception next to A1 in research/ADR — do not leave it as an accidental asymmetry with `totalAssimilatedCount`.

### WR-02: Assimilation queue join excludes COMMISSIONED, but create still treats it as assimilated

**File:** `backend/src/main/java/com/odde/doughnut/entities/repositories/NoteRepository.java:154-160` (join change); create path `MemoryTrackerAssimilation.java:50-65` (unchanged, called from assimilate)
**Issue:** `joinMemoryTracker` now ignores COMMISSIONED so a commissioned-only note correctly appears in `AssimilationController.next` / unassimilated counts (proven by `commissionedOnlyNoteStillAppearsInOrdinaryAssimilationQueue`). But `MemoryTrackerAssimilation.assimilate` still loads all note-level trackers via `findByUserAndNote` and, when any note-level tracker exists (including COMMISSIONED), returns empty / spelling-only without creating UNDERSTANDING:

```java
List<MemoryTracker> existingNoteLevelTrackers =
    existingTrackers.stream().filter(MemoryTracker::isNoteLevelTracker).toList();
// ...
if (!existingNoteLevelTrackers.isEmpty()) {
  return List.of();
}
```

So the Structure filter advertises ordinary work the create path cannot complete. Planned for Phase 2, but it is a live correctness hole as soon as COMMISSIONED rows exist.
**Fix:** In Phase 2 (or sooner if COMMISSIONED can be persisted), ignore COMMISSIONED when deciding ordinary note-level existence — e.g. filter `existingNoteLevelTrackers` with `mt.getType() != MemoryTrackerType.COMMISSIONED` (and keep coexistence UK). Add a controller-boundary test: commissioned-only note → `assimilate(note)` creates UNDERSTANDING (and leaves COMMISSIONED).

## Info

### IN-01: SC3 assertion does not pin which due tracker is returned

**File:** `backend/src/test/java/com/odde/doughnut/controllers/RecallsControllerTests.java:136-148`
**Issue:** `shouldExcludeCommissionedMemoryTrackersFromOrdinaryRecallLists` asserts `getToRepeat()` size 1 and `totalAssimilatedCount == 1`, but not that the lite is the ordinary tracker (id/type). A regressing filter that dropped UNDERSTANDING and kept COMMISSIONED would still pass size/count.
**Fix:** Capture the ordinary tracker id and assert `dueMemoryTrackers.getToRepeat().get(0).getMemoryTrackerId()` (or equivalent) equals it; optionally assert type/spelling flags.

### IN-02: Native `'COMMISSIONED'` literals diverge from JPQL constants

**File:** `backend/src/main/java/com/odde/doughnut/entities/repositories/MemoryTrackerRepository.java:68,106` vs `MemoryTracker.java:131-138`
**Issue:** JPQL ordinary joins use `JPA_WHERE_NOT_COMMISSIONED_*` constants; native due/batch filters hard-code `'COMMISSIONED'` (same pattern as SPELLING). Rename or typo drift between enum and native SQL would fail silently at runtime for native paths only.
**Fix:** Acceptable as-is given SPELLING precedent. Optionally add a short comment on `byUserIdFrom` / batch query pointing at `MemoryTrackerType.COMMISSIONED` so greps stay linked; avoid inventing a second boolean column.

## Security notes (no findings)

- Type filters are code literals / JPQL enum constants; `userId` / timestamps remain `@Param` — no injection surface introduced.
- Selection remains scoped by authenticated `userId`; no new authz bypass in these repositories.
- `NoteStructuralPeerQueries` extract is a pure move of existing parameterized native peer queries; no COMMISSIONED logic and no new trust boundary.

---

_Reviewed: 2026-08-07T22:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
