# Phase 01: Commissioned tracker model - Pattern Map

**Mapped:** 2026-08-07
**Files analyzed:** 7
**Analogs found:** 7 / 7

> **Supersedes** prior `01-PATTERNS.md` that documented boolean `commissioned` column / V300000238 unique-key rebuild. Quick **006** already shipped `MemoryTrackerType.COMMISSIONED`, `type` column, UK on `type`, and `.commissioned()`. Phase 1 = **selection filters + proofs only**.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `MemoryTrackerRepository.java` (`byUserIdFrom` + due query) | repository | CRUD / request-response | same file — `byUserIdFrom` + `AND mt.type <> 'SPELLING'` in batch query | exact |
| `MemoryTrackerRepository.java` (`findBatchQuestionGenerationCandidatesByUser`) | repository | batch | same file lines 98–126 — SPELLING native filter | exact |
| `NoteRepository.java` (`joinMemoryTracker`) | repository | CRUD | same file + `MemoryTracker.JPA_WHERE_NOTE_LEVEL_TRACKER`; JPQL enum filter from `NotePropertyIndexRepository` | exact |
| `MemoryTracker.java` (optional shared JPQL fragment) | model / utility | transform | same file — `JPA_WHERE_NOTE_LEVEL_TRACKER` / `JPA_WHERE_NOTE_LEVEL_TARGET_TRACKER` | exact |
| `NotePropertyIndexRepository.java` (optional target gate) | repository | CRUD | same file — `unassimilatedJoinPropertyTracker` SPELLING exclusion + `targetNoteKeyGateWhere` | exact |
| `RecallsControllerTests.java` (SC3) | test | request-response | same file — `Repeat.shouldExcludeMemoryTrackersForDeletedNotesFromRecallLists` + `.commissioned()` | exact |
| `AssimilationControllerTests.java` (queue proof) | test | request-response | same file — `Next.countsAreCorrect` / `Next.returns_*`; service analog `AssimilationServiceSubscriptionQueueTest.WhenNoteHasOnlyPropertyTracker` | exact |
| `QuestionGenerationBatchCandidateMemoryTrackersTest.java` | test | batch | same file — `excludesSpellingTracker` | exact |

**Already done (do not recreate — reference only):**

| File | Status |
|------|--------|
| `MemoryTrackerType.java` | COMMISSIONED present |
| `MemoryTracker.java` `type` field | `@Enumerated(STRING)` present |
| `MemoryTrackerBuilder.commissioned()` | present |
| `AssimilationControllerTests.understandingAndCommissionedTrackersCanCoexistOnSameNote` | SC2 green |
| Flyway tip `V300000239` | **no new migration** |

**Likely unmodified (filter at repository seams):**

| File | Why unchanged |
|------|---------------|
| `RecallService` / `UserService` | Consume due stream; SQL exclusion is enough |
| `MemoryTrackerAssimilation` | Create / ignore-commissioned-on-create is **Phase 2** |
| `QuestionGenerationBatchLocalPlanningTest` | Prefer candidate-list analog; optional follow-on if planning path asserted |
| Migrations / ERD / OpenAPI regen | No schema or DTO signature change |

## Pattern Assignments

### `MemoryTrackerRepository.java` — due / count exclusion via `byUserIdFrom` (repository, CRUD)

**Analog:** same file — shared fragment + SPELLING literal filter style in batch query

**Imports / interface shape** (lines 1–11):
```java
package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.MemoryTracker;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface MemoryTrackerRepository extends CrudRepository<MemoryTracker, Integer> {
```

**Core pattern — shared `byUserIdFrom` used by due list + count** (lines 23–33, 63–67):
```java
  @Query(value = "SELECT count(*) " + byUserIdFrom, nativeQuery = true)
  int countByUserNotRemoved(@Param("userId") Integer userId);

  @Query(
      value =
          "SELECT rp.* "
              + byUserIdFrom
              + " AND rp.next_recall_at <= :nextRecallAt ORDER BY rp.next_recall_at, (rp.type = 'SPELLING') DESC",
      nativeQuery = true)
  Stream<MemoryTracker> findAllByUserAndNextRecallAtLessThanEqualOrderByNextRecallAt(
      @Param("userId") Integer userId, @Param("nextRecallAt") Timestamp nextRecallAt);

  String byUserIdFrom =
      " FROM memory_tracker rp "
          + " WHERE rp.user_id = :userId "
          + "   AND rp.removed_from_tracking IS FALSE "
          + "   AND rp.deleted_at IS NULL ";
```

**Phase 1 change shape** — append literal type filter to `byUserIdFrom` only (not `byUserIdWhere`):
```java
  String byUserIdFrom =
      " FROM memory_tracker rp "
          + " WHERE rp.user_id = :userId "
          + "   AND rp.removed_from_tracking IS FALSE "
          + "   AND rp.deleted_at IS NULL "
          + "   AND rp.type <> 'COMMISSIONED' ";
```

**Keep:** `ORDER BY … (rp.type = 'SPELLING') DESC` unchanged. Leave `byUserIdWhere` / `findLast100ByUser` / `findByUserAndNote` unfiltered.

**Error / safety:** Native filters use **literal** enum names (`'COMMISSIONED'`); user-bound values stay `@Param` — never concatenate user input into the type clause.

---

### `MemoryTrackerRepository.java` — batch candidates (repository, batch)

**Analog:** same file lines 98–126 — existing SPELLING exclusion

**Core pattern** (lines 98–126):
```java
  @Query(
      value =
          "SELECT mt.* FROM memory_tracker mt "
              + "WHERE mt.user_id = :userId "
              + "  AND mt.removed_from_tracking IS FALSE "
              + "  AND mt.deleted_at IS NULL "
              + "  AND mt.type <> 'SPELLING' "
              + "  AND mt.next_recall_at <= :dueBy "
              // … NOT EXISTS recall_prompt / batch_request …
              + "ORDER BY mt.next_recall_at",
      nativeQuery = true)
  List<MemoryTracker> findBatchQuestionGenerationCandidatesByUser(
      @Param("userId") Integer userId, @Param("dueBy") Timestamp dueBy);
```

**Phase 1 change shape** — add beside SPELLING:
```java
              + "  AND mt.type <> 'SPELLING' "
              + "  AND mt.type <> 'COMMISSIONED' "
```

---

### `NoteRepository.java` — `joinMemoryTracker` ordinary-only (repository, CRUD)

**Analog:** same file `joinMemoryTracker` + entity JPQL fragment; enum exclusion from `NotePropertyIndexRepository`

**Current join** (NoteRepository.java:154–158):
```java
  String joinMemoryTracker =
      " LEFT JOIN n.memoryTrackers rp ON rp.user.id = :userId"
          + " AND rp.deletedAt IS NULL"
          + " AND "
          + MemoryTracker.JPA_WHERE_NOTE_LEVEL_TRACKER;
```

**JPQL enum filter analog** (NotePropertyIndexRepository.java:13–17):
```java
  String unassimilatedJoinPropertyTracker =
      " LEFT JOIN n.memoryTrackers mt ON mt.user.id = :userId"
          + " AND mt.deletedAt IS NULL"
          + " AND mt.type <> com.odde.doughnut.entities.MemoryTrackerType.SPELLING"
          + " AND mt.propertyKey = i.propertyKey";
```

**Phase 1 change shape** — exclude COMMISSIONED on note-level assimilation detection:
```java
  String joinMemoryTracker =
      " LEFT JOIN n.memoryTrackers rp ON rp.user.id = :userId"
          + " AND rp.deletedAt IS NULL"
          + " AND "
          + MemoryTracker.JPA_WHERE_NOTE_LEVEL_TRACKER
          + " AND rp.type <> com.odde.doughnut.entities.MemoryTrackerType.COMMISSIONED";
```

Prefer extending `MemoryTracker.JPA_WHERE_*` (or a sibling constant) if planner wants one representation for ordinary note-level trackers.

**Consumed by:** `findByOwnershipWhereThereIsNoMemoryTracker`, `countByOwnership…`, `findByAncestorWhereThereIsNoMemoryTracker`, `countByAncestor…` — all share `joinMemoryTracker` + `recallWhereClause` (`rp IS NULL`).

---

### `MemoryTracker.java` — optional shared JPQL fragment (model / utility, transform)

**Analog:** same file lines 116–128

```java
  /**
   * JPQL fragment for joined alias {@code rp}; must stay aligned with {@link
   * #isNoteLevelTracker()}.
   */
  public static final String JPA_WHERE_NOTE_LEVEL_TRACKER =
      "(rp.propertyKey IS NULL OR rp.propertyKey = '')";

  /**
   * JPQL fragment for joined alias {@code tmt}; must stay aligned with {@link
   * #isNoteLevelTracker()}.
   */
  public static final String JPA_WHERE_NOTE_LEVEL_TARGET_TRACKER =
      "(tmt.propertyKey IS NULL OR tmt.propertyKey = '')";
```

**Phase 1 option:** add a sibling constant for “ordinary note-level” (property empty **and** not COMMISSIONED), or append COMMISSIONED exclusion next to existing constant usage. Do **not** re-add `type` field / enum / spelling column — already present (lines 85–113).

**Type field already shipped** (lines 85–88):
```java
  @Column(name = "type")
  @Enumerated(EnumType.STRING)
  @Getter
  private MemoryTrackerType type = MemoryTrackerType.UNDERSTANDING;
```

---

### `NotePropertyIndexRepository.java` — optional target-note gate (repository, CRUD)

**Analog:** same file `targetNoteKeyGateWhere` (lines 19–30) + SPELLING join filter

**Target gate today** (no COMMISSIONED exclusion on `tmtBlock`):
```java
  String targetNoteKeyGateWhere =
      " AND NOT EXISTS ("
          + " SELECT iBlock FROM NotePropertyIndex iBlock"
          + " JOIN iBlock.targetNote tBlock"
          + " LEFT JOIN tBlock.memoryTrackers tmtBlock ON tmtBlock.user.id = :userId"
          + " AND tmtBlock.deletedAt IS NULL"
          + " AND (tmtBlock.propertyKey IS NULL OR tmtBlock.propertyKey = '')"
          + " WHERE iBlock.note = n AND iBlock.propertyKey = i.propertyKey"
          // …
          + " AND tmtBlock IS NULL"
          + ") ";
```

**Phase 1 change shape** (if Wave 2 includes A2):
```java
          + " AND (tmtBlock.propertyKey IS NULL OR tmtBlock.propertyKey = '')"
          + " AND tmtBlock.type <> com.odde.doughnut.entities.MemoryTrackerType.COMMISSIONED"
```

Mirror SPELLING style already used on property-tracker join (`mt.type <> …SPELLING`).

---

### `RecallsControllerTests.java` — SC3 due exclusion (test, request-response)

**Analog:** `Repeat.shouldExcludeMemoryTrackersForDeletedNotesFromRecallLists` (lines 118–134) + helpers (38–44)

**Controller-boundary + makeMe pattern** (lines 25–44, 118–134):
```java
class RecallsControllerTests extends ControllerTestBase {
  @Autowired RecallsController controller;

  private Note ownedNote() {
    return makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
  }

  private MemoryTracker dueTracker(Note note, Timestamp nextRecallAt) {
    return makeMe.aMemoryTrackerFor(note).nextRecallAt(nextRecallAt).please();
  }

  @Nested
  class Repeat {
    @Test
    void shouldExcludeMemoryTrackersForDeletedNotesFromRecallLists() {
      Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
      testabilitySettings.timeTravelTo(currentTime);
      Note activeNote = ownedNote();
      Note deletedNote = ownedNote();
      dueTracker(activeNote, currentTime);
      dueTracker(deletedNote, currentTime);
      // … destroy deletedNote …
      DueMemoryTrackers dueMemoryTrackers = controller.recalling("Asia/Shanghai", 0);
      assertThat(dueMemoryTrackers.getToRepeat(), hasSize(1));
      assertEquals(1, dueMemoryTrackers.totalAssimilatedCount);
    }
  }
}
```

**Fixture builder** (MemoryTrackerBuilder.java:58–60) — already exists:
```java
  public MemoryTrackerBuilder commissioned() {
    entity.setType(MemoryTrackerType.COMMISSIONED);
    return this;
  }
```

**Phase 1 SC3 shape** (capability-named; assert delta only — size 1 ordinary):
```java
    @Test
    void shouldExcludeCommissionedMemoryTrackersFromOrdinaryRecallLists() {
      Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
      testabilitySettings.timeTravelTo(currentTime);
      Note note = ownedNote();
      dueTracker(note, currentTime);
      makeMe.aMemoryTrackerFor(note).commissioned().nextRecallAt(currentTime).please();

      DueMemoryTrackers due = controller.recalling("Asia/Shanghai", 0);
      assertThat(due.getToRepeat(), hasSize(1));
      // if byUserIdFrom filters counts: assertEquals(1, due.totalAssimilatedCount);
    }
```

Drive `RecallsController.recalling` — do not unit-test the repository SQL string in isolation.

---

### `AssimilationControllerTests.java` — commissioned-only note still in ordinary queue (test, request-response)

**Coexistence already present** (SC2 — keep green) lines 125–134:
```java
    @Test
    void understandingAndCommissionedTrackersCanCoexistOnSameNote() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      makeMe.aMemoryTrackerFor(note).please();
      makeMe.aMemoryTrackerFor(note).commissioned().please();

      assertThat(
          memoryTrackerRepository.findByUserAndNote(currentUser.getUser().getId(), note.getId()),
          hasSize(2));
    }
```

**Queue / next analog at controller** — `Next.countsAreCorrect` / `returnsNullWhenNoNotesLeft` (lines 65–78):
```java
  @Nested
  class Next {
    @Test
    void countsAreCorrect() {
      ownedNote("note1");
      ownedNote("note2");

      AssimilationNextDTO result = controller.next("Asia/Shanghai");
      assertThat(result.getCounts().getDueCount(), equalTo(2));
      assertThat(result.getCounts().getTotalUnassimilatedCount(), equalTo(2));
    }
  }
```

**Service-level “non-ordinary tracker does not count as assimilated” analog** — `AssimilationServiceSubscriptionQueueTest` (lines 15–23):
```java
  @Nested
  class WhenNoteHasOnlyPropertyTracker {
    @Test
    void shouldAppearInUnassimilatedNotes() {
      Note note = makeMe.aNote("vitamins").notebookOwnedBy(user).please();
      makeMe.aMemoryTrackerFor(note).propertyKey("topic").assimilatedAt(day1).please();

      assertThat(
          userService.getUnassimilatedNotes(user).map(Note::getId).toList(), hasItem(note.getId()));
    }
  }
```

**Phase 1 queue proof shape** (prefer controller `next` / counts, or same service pattern):
```java
    @Test
    void commissionedOnlyNoteStillAppearsInOrdinaryAssimilationQueue() {
      Note note = ownedNote("commissioned-only");
      makeMe.aMemoryTrackerFor(note).commissioned().please();

      AssimilationNextDTO result = controller.next("Asia/Shanghai");
      assertThat(result.getNextUnit().getNoteId(), equalTo(note.getId()));
      // or: totalUnassimilatedCount includes note; do not assert assimilate() create path
    }
```

**Do not** assert `controller.assimilate(...)` creates UNDERSTANDING when COMMISSIONED exists — that is Phase 2 (`MemoryTrackerAssimilation`).

---

### `QuestionGenerationBatchCandidateMemoryTrackersTest.java` — exclude COMMISSIONED (test, batch)

**Analog:** `excludesSpellingTracker` (lines 84–94) — exact role + data-flow match

```java
  @Test
  void excludesSpellingTracker() {
    MemoryTracker spellingTracker =
        makeMe
            .aMemoryTrackerFor(makeMe.aNote().notebookOwnedBy(user).please())
            .spelling()
            .nextRecallAt(hoursFrom(currentTime, 1))
            .please();

    assertThat(candidateIds(), not(hasItem(spellingTracker.getId())));
  }
```

**Phase 1 shape:**
```java
  @Test
  void excludesCommissionedTracker() {
    MemoryTracker commissionedTracker =
        makeMe
            .aMemoryTrackerFor(makeMe.aNote().notebookOwnedBy(user).please())
            .commissioned()
            .nextRecallAt(hoursFrom(currentTime, 1))
            .please();

    assertThat(candidateIds(), not(hasItem(commissionedTracker.getId())));
  }
```

Uses existing boundary `planningService.findCandidateMemoryTrackersForBatchGeneration` + `candidateIds()` helpers (lines 144–150). Prefer this over enlarging `QuestionGenerationBatchLocalPlanningTest` (which asserts full planned batch size).

## Shared Patterns

### Native SQL type filter (literal enum name)
**Source:** `MemoryTrackerRepository.java:104` (`<> 'SPELLING'`)
**Apply to:** `byUserIdFrom`, `findBatchQuestionGenerationCandidatesByUser`
```java
"  AND mt.type <> 'COMMISSIONED' "
```

### JPQL enum constant filter
**Source:** `NotePropertyIndexRepository.java:16`
**Apply to:** `NoteRepository.joinMemoryTracker`; optional `targetNoteKeyGateWhere`
```java
" AND mt.type <> com.odde.doughnut.entities.MemoryTrackerType.SPELLING"
// → parallel COMMISSIONED exclusion with fully-qualified enum constant
```

### Shared JPQL fragment on entity
**Source:** `MemoryTracker.JPA_WHERE_NOTE_LEVEL_TRACKER` (lines 120–121)
**Apply to:** Prefer one constant for ordinary note-level join predicates if COMMISSIONED exclusion is reused

### makeMe `.commissioned()` fixtures
**Source:** `MemoryTrackerBuilder.java:58–60`
**Apply to:** All Phase 1 proofs — do not hand-set type soup
```java
makeMe.aMemoryTrackerFor(note).commissioned().nextRecallAt(now).please();
```

### Controller / service “small test” boundary
**Sources:** `RecallsControllerTests`, `AssimilationControllerTests`, `QuestionGenerationBatchCandidateMemoryTrackersTest`
**Apply to:** SC3 + queue + batch — one behavior per test; assert delta only (`hasSize(1)`, `not(hasItem(…))`)

### Auth / endpoints
**Apply to:** Phase 1 — **none new**. Reuse existing `RecallsController` / `AssimilationController` authz; no create path.

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| — | — | — | All Phase 1 filter/test files have exact in-repo analogs |

**Explicit non-goals (stale analogs — do not use):**

| Obsolete target | Why not |
|-----------------|---------|
| New Flyway boolean `commissioned` / unique-key rebuild | Done via `type` in quick 006 (`V300000238`–`239`) |
| `MemoryTrackerAssimilation` create COMMISSIONED | Phase 2 Behavior |
| OpenAPI / ERD regen | No schema/DTO change |

## Metadata

**Analog search scope:** `backend/.../repositories/MemoryTrackerRepository.java`, `NoteRepository.java`, `NotePropertyIndexRepository.java`, `entities/MemoryTracker.java`, `MemoryTrackerType.java`, `MemoryTrackerBuilder.java`, `RecallsControllerTests.java`, `AssimilationControllerTests.java`, `AssimilationServiceSubscriptionQueueTest.java`, `QuestionGenerationBatchCandidateMemoryTrackersTest.java`, `QuestionGenerationBatchLocalPlanningTest.java`, `.planning/quick/006-memory-tracker-type/PLAN.md`
**Files scanned:** ~15 focused
**Pattern extraction date:** 2026-08-07
**Foundation:** quick 006 done — Phase 1 patterns are WHERE/SQL/JPQL filters + tests only
