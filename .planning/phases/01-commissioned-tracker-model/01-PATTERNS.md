# Phase 01: Commissioned tracker model - Pattern Map

**Mapped:** 2026-08-07
**Files analyzed:** 9
**Analogs found:** 9 / 9

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `backend/src/main/resources/db/migration/V300000238__add_memory_tracker_commissioned.sql` | migration | transform | `V300000232__add_health_remove_empty_folders_default.sql` (boolean ADD) + `V100000000__baseline.sql:369` (unique key) + `V300000237__…sql` (memory_tracker ALTER) | exact (compose) |
| `backend/src/main/java/.../entities/MemoryTracker.java` | model | CRUD | same file — `spelling` boolean field | exact |
| `backend/src/main/java/.../repositories/MemoryTrackerRepository.java` | repository | CRUD / request-response | same file — `byUserIdFrom` + due/batch native queries | exact |
| `backend/src/main/java/.../repositories/NoteRepository.java` | repository | CRUD | same file — `joinMemoryTracker` + `MemoryTracker.JPA_WHERE_NOTE_LEVEL_TRACKER` | exact |
| `backend/src/test/java/.../builders/MemoryTrackerBuilder.java` | test utility | CRUD | same file — `.spelling()` / `.removedFromTracking()` fluent flags | exact |
| `backend/src/test/java/.../controllers/RecallsControllerTests.java` | test | request-response | same file — `Repeat.shouldExcludeMemoryTrackersForDeletedNotesFromRecallLists` | exact |
| `backend/src/test/java/.../controllers/AssimilationControllerTests.java` (recommended) | test | request-response | same file — `Next.countsAreCorrect` / note-level tracker fixtures | role-match |
| `docs/database-erd.md` | docs | transform | regenerate via `database-erd` skill / `pnpm export:database-erd` | exact (process) |
| Generated OpenAPI/TS `MemoryTracker` (if Springdoc picks up field) | config / generated | transform | `pnpm generateTypeScript` — do not hand-edit | exact (process) |

**Likely unmodified (call-chain context only — filter at repository):**

| File | Role | Why unchanged in Phase 1 |
|------|------|--------------------------|
| `UserService.java` | service | Pass-through to `findAllByUserAndNextRecallAt…`; SQL exclusion is enough |
| `RecallService.java` | service | Maps repository stream → `MemoryTrackerLite`; no commissioned field needed on lite in Phase 1 |
| `MemoryTrackerService.java` | service | Assimilate coexistence short-circuit is **Phase 2**; prove coexistence via makeMe + unique key |
| `MakeMe.java` | test utility | `aMemoryTrackerFor(note)` already returns builder; only builder needs `.commissioned()` |

## Pattern Assignments

### `V300000238__add_memory_tracker_commissioned.sql` (migration, transform)

**Analog (boolean column):** `backend/src/main/resources/db/migration/V300000232__add_health_remove_empty_folders_default.sql`

**Core pattern** (lines 1-3):
```sql
-- Persist user-level Health run option default: Remove empty folders.
ALTER TABLE `user`
  ADD COLUMN `health_remove_empty_folders_default` tinyint(1) NOT NULL DEFAULT 0;
```

**Analog (memory_tracker ALTER style):** `V300000237__add_memory_tracker_next_recall_at_index.sql` lines 1-3:
```sql
-- Speed due-item lookups: filter by user_id and range/order by next_recall_at.
ALTER TABLE `memory_tracker`
  ADD KEY `idx_memory_tracker_user_next_recall_at` (`user_id`, `next_recall_at`);
```

**Analog (unique key to rebuild):** `V100000000__baseline.sql` lines 365-369:
```sql
  `spelling` tinyint(1) NOT NULL DEFAULT '0',
  `deleted_at` timestamp NULL DEFAULT NULL,
  `property_key` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_note_spelling_active` (`user_id`,`note_id`,`spelling`,`property_key`,(if((`deleted_at` is null),1,NULL))),
```

**Compose for Phase 1** (no prior DROP UNIQUE migration in repo — invent from baseline + boolean ADD):
```sql
ALTER TABLE `memory_tracker`
  ADD COLUMN `commissioned` tinyint(1) NOT NULL DEFAULT 0;

ALTER TABLE `memory_tracker`
  DROP INDEX `user_note_spelling_active`,
  ADD UNIQUE KEY `user_note_spelling_active`
    (`user_id`,`note_id`,`spelling`,`property_key`,`commissioned`,(if((`deleted_at` is null),1,NULL)));
```

**Rules:** New file only; version `> 300000237`; never edit baseline in place (`db-migration.mdc`). Keep index name `user_note_spelling_active` unless an optional rename is desired.

---

### `MemoryTracker.java` (model, CRUD)

**Analog:** same file — `spelling` / `removedFromTracking` boolean discriminators

**Imports pattern** (lines 1-16):
```java
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
```

**Core field pattern** (lines 78-86) — mirror for `commissioned`:
```java
  @Column(name = "removed_from_tracking")
  @Getter
  @Setter
  private Boolean removedFromTracking = false;

  @Column(name = "spelling")
  @Getter
  @Setter
  private Boolean spelling = false;
```

**Add beside spelling:**
```java
  @Column(name = "commissioned")
  @Getter
  @Setter
  private Boolean commissioned = false;
```

**JPQL fragment pattern** (lines 93-98) — extend if NoteRepository join needs ordinary-only:
```java
  public static final String JPA_WHERE_NOTE_LEVEL_TRACKER =
      "(rp.propertyKey IS NULL OR rp.propertyKey = '')";
```
Recommended Phase 1 extension for ordinary note-level join (planner discretion on exact constant name):
```java
  // e.g. AND (rp.commissioned IS NULL OR rp.commissioned = FALSE)
```

**Factory defaults:** `buildMemoryTrackerForNote` / `buildMemoryTrackerForProperty` leave booleans at field defaults (`false`) — no change required unless constructors set flags explicitly.

**Serialization:** Field will appear on API `MemoryTracker` JSON by default (like `spelling`). Accept default `false` as Structure; optionally `@JsonIgnore` only if team freezes wire (RESEARCH A4 prefers serialize).

---

### `MemoryTrackerRepository.java` (repository, CRUD / request-response)

**Analog:** same file — shared fragments + due/batch queries

**Imports pattern** (lines 1-9):
```java
import com.odde.doughnut.entities.MemoryTracker;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
```

**Shared filter fragment** (lines 63-67) — **primary exclusion seam** (also feeds `countByUserNotRemoved`):
```java
  String byUserIdFrom =
      " FROM memory_tracker rp "
          + " WHERE rp.user_id = :userId "
          + "   AND rp.removed_from_tracking IS FALSE "
          + "   AND rp.deleted_at IS NULL ";
```

**Extend to:**
```java
          + "   AND rp.removed_from_tracking IS FALSE "
          + "   AND rp.deleted_at IS NULL "
          + "   AND rp.commissioned IS FALSE ";
```

**Due query** (lines 26-33) uses `byUserIdFrom` — inherits exclusion automatically:
```java
  @Query(
      value =
          "SELECT rp.* "
              + byUserIdFrom
              + " AND rp.next_recall_at <= :nextRecallAt ORDER BY rp.next_recall_at, IFNULL(rp.spelling, 0) DESC",
      nativeQuery = true)
  Stream<MemoryTracker> findAllByUserAndNextRecallAtLessThanEqualOrderByNextRecallAt(
      @Param("userId") Integer userId, @Param("nextRecallAt") Timestamp nextRecallAt);
```

**Batch candidates** (lines 98-126) — duplicate filter (does **not** use `byUserIdFrom`); add `AND mt.commissioned IS FALSE` next to existing `mt.spelling IS FALSE`:
```java
              + "  AND mt.removed_from_tracking IS FALSE "
              + "  AND mt.deleted_at IS NULL "
              + "  AND mt.spelling IS FALSE "
              + "  AND mt.next_recall_at <= :dueBy "
```

**Do not filter** `findByUserAndNote` (lines 35-42) — settings/Phase 2 need both ordinary and commissioned:
```java
  @Query(
      value =
          "SELECT rp.* FROM memory_tracker rp "
              + " WHERE rp.user_id = :userId "
              + "   AND rp.deleted_at IS NULL "
              + "   AND rp.note_id = :noteId",
      nativeQuery = true)
  List<MemoryTracker> findByUserAndNote(Integer userId, @Param("noteId") Integer noteId);
```

**`byUserIdWhere`** (lines 69-72) backs recent assimilations / recently recalled lists — RESEARCH open Q1: leave without commissioned filter in Phase 1 (default false = no behavior change until fixtures).

**Error handling:** Parameterized `@Param` only — never string-concat user input into SQL.

---

### `NoteRepository.java` (repository, CRUD)

**Analog:** same file — unassimilated join

**Core join pattern** (lines 148-158):
```java
  String recallWhereClause =
      " WHERE "
          + "   rp IS NULL "
          + "   AND COALESCE(n.recallSetting.skipMemoryTracking, FALSE) = FALSE "
          + "   AND n.deletedAt IS NULL ";

  String joinMemoryTracker =
      " LEFT JOIN n.memoryTrackers rp ON rp.user.id = :userId"
          + " AND rp.deletedAt IS NULL"
          + " AND "
          + MemoryTracker.JPA_WHERE_NOTE_LEVEL_TRACKER;
```

**Phase 1 change:** Restrict join to **ordinary** (non-commissioned) note-level trackers so a commissioned-only note remains in the assimilation queue (`rp IS NULL` still true for ordinary path). Prefer extending `MemoryTracker` JPQL constants and referencing them here (same cohesion as `JPA_WHERE_NOTE_LEVEL_TRACKER`).

**Consumers of the fragment** (lines 165-195): `findByOwnershipWhereThereIsNoMemoryTracker`, counts, and notebook-ancestor variants — one join fix covers all.

---

### `MemoryTrackerBuilder.java` (test utility, CRUD)

**Analog:** same file — fluent boolean helpers

**Imports / structure** (lines 1-13):
```java
import com.odde.doughnut.entities.*;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;

public class MemoryTrackerBuilder extends EntityBuilder<MemoryTracker> {
  public MemoryTrackerBuilder(MemoryTracker memoryTracker, MakeMe makeMe) {
    super(makeMe, memoryTracker);
    assimilatedAt(makeMe.aTimestamp().of(0, 0).please());
  }
```

**Flag helper pattern** (lines 43-56) — copy for `.commissioned()`:
```java
  public MemoryTrackerBuilder removedFromTracking() {
    entity.setRemovedFromTracking(true);
    return this;
  }

  public MemoryTrackerBuilder spelling() {
    entity.setSpelling(true);
    return this;
  }
```

**Add:**
```java
  public MemoryTrackerBuilder commissioned() {
    entity.setCommissioned(true);
    return this;
  }
```

**MakeMe entry** (unchanged) — `MakeMe.java` lines 92-97:
```java
  public MemoryTrackerBuilder aMemoryTrackerFor(Note note) {
    MemoryTracker memoryTracker = MemoryTracker.buildMemoryTrackerForNote(note);
    MemoryTrackerBuilder memoryTrackerBuilder = new MemoryTrackerBuilder(memoryTracker, this);
    memoryTrackerBuilder.entity.setNote(note);
    memoryTrackerBuilder.by(note.getNotebook().getOwnership().getUser());
    return memoryTrackerBuilder;
  }
```

---

### `RecallsControllerTests.java` (test, request-response)

**Analog:** same file — `Repeat` nested class exclusion / due fixtures

**Imports / harness** (lines 1-40):
```java
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.DueMemoryTrackers;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import java.sql.Timestamp;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RecallsControllerTests extends ControllerTestBase {
  @Autowired RecallsController controller;

  private Note ownedNote() {
    return makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
  }

  private MemoryTracker dueTracker(Note note, Timestamp nextRecallAt) {
    return makeMe.aMemoryTrackerFor(note).nextRecallAt(nextRecallAt).please();
  }
```

**Exclusion assertion pattern** (lines 118-134) — copy structure for commissioned:
```java
    @Test
    void shouldExcludeMemoryTrackersForDeletedNotesFromRecallLists() {
      Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
      testabilitySettings.timeTravelTo(currentTime);
      Note activeNote = ownedNote();
      Note deletedNote = ownedNote();
      dueTracker(activeNote, currentTime);
      dueTracker(deletedNote, currentTime);

      noteService.destroy(
          deletedNote, NoteDeleteReferenceHandling.LEAVE_DEAD_LINKS, currentUser.getUser());

      DueMemoryTrackers dueMemoryTrackers = controller.recalling("Asia/Shanghai", 0);

      assertThat(dueMemoryTrackers.getToRepeat(), hasSize(1));
      assertEquals(1, dueMemoryTrackers.totalAssimilatedCount);
    }
```

**Phase 1 coexistence + exclusion fixture** (from RESEARCH — drive `controller.recalling`):
```java
    @Test
    void shouldExcludeCommissionedMemoryTrackersFromDueRecall() {
      Timestamp now = makeMe.aTimestamp().of(0, 0).please();
      testabilitySettings.timeTravelTo(now);
      Note note = ownedNote();
      makeMe.aMemoryTrackerFor(note).nextRecallAt(now).please(); // ordinary
      makeMe.aMemoryTrackerFor(note).commissioned().nextRecallAt(now).please();

      DueMemoryTrackers due = controller.recalling("Asia/Shanghai", 0);

      assertThat(due.getToRepeat(), hasSize(1));
      // optionally: assert totalAssimilatedCount excludes commissioned if byUserIdFrom filtered
    }
```

**Auth pattern:** Existing tests assert `ResponseStatusException` when logged out — no new auth surface in Phase 1.

**Do not mock** repository — real DB + makeMe (`backend-testing.mdc` / `unit-testing.mdc`).

---

### `AssimilationControllerTests.java` (recommended test, request-response)

**Analog:** same file — `Next` queue / counts

**Core pattern** (lines 70-78) — assert commissioned-only note still counts as unassimilated for ordinary path:
```java
    @Test
    void countsAreCorrect() {
      ownedNote("note1");
      ownedNote("note2");

      AssimilationNextDTO result = controller.next("Asia/Shanghai");
      assertThat(result.getCounts().getDueCount(), equalTo(2));
      assertThat(result.getCounts().getAssimilatedCountOfTheDay(), equalTo(0));
      assertThat(result.getCounts().getTotalUnassimilatedCount(), equalTo(2));
    }
```

**Recommended Phase 1 scenario:**
```java
    @Test
    void commissionedOnlyNoteStillAppearsInAssimilationQueue() {
      Note note = ownedNote("commissioned-only");
      makeMe.aMemoryTrackerFor(note).commissioned().please();

      AssimilationNextDTO result = controller.next("Asia/Shanghai");
      assertThat(result.getNextUnit().getNoteId(), equalTo(note.getId()));
    }
```

---

### Call-chain context (do not rewrite for Phase 1)

**`RecallsController.recalling`** — lines 39-51:
```java
  @GetMapping(value = {"/recalling"})
  @Transactional
  public DueMemoryTrackers recalling(
      @RequestParam(value = "timezone") String timezone,
      @RequestParam(value = "dueindays", required = false) Integer dueInDays) {
    authorizationService.assertLoggedIn();
    ZoneId timeZone = TimezoneUtils.parseTimezone(timezone);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    return recallService.getDueMemoryTrackers(
        authorizationService.getCurrentUser(),
        currentUTCTimestamp,
        timeZone,
        dueInDays == null ? 0 : dueInDays);
  }
```

**`UserService.getMemoryTrackersNeedToRepeat`** — lines 65-69:
```java
  public Stream<MemoryTracker> getMemoryTrackersNeedToRepeat(
      User user, Timestamp currentUTCTimestamp, ZoneId timeZone) {
    final Timestamp timestamp = TimestampOperations.alignByHalfADay(currentUTCTimestamp, timeZone);
    return memoryTrackerRepository.findAllByUserAndNextRecallAtLessThanEqualOrderByNextRecallAt(
        user.getId(), timestamp);
  }
```

**`RecallService.getDueMemoryTrackers`** — lines 47-71: maps trackers to `MemoryTrackerLite` (id, spelling, propertyKey) and sets `totalAssimilatedCount` from `countByUserNotRemoved`. Filtering in `byUserIdFrom` updates both `toRepeat` and ordinary assimilated count without service edits.

**`MemoryTrackerService.assimilate` short-circuit** — lines 81-97 (Phase 2 only):
```java
    List<MemoryTracker> existingNoteLevelTrackers =
        existingTrackers.stream().filter(MemoryTracker::isNoteLevelTracker).toList();
    // ...
    if (!existingNoteLevelTrackers.isEmpty()) {
      return List.of();
    }
```
Phase 1 proves coexistence via makeMe + unique key, **not** by changing assimilate.

---

### `docs/database-erd.md` (docs, transform)

**Analog process:** After Flyway lands, regenerate — do not hand-edit Mermaid.

```bash
CURSOR_DEV=true nix develop -c pnpm export:database-erd
```

Current stub shows `memory_tracker` with only id/FKs (`docs/database-erd.md` ~132-136); regen picks up `commissioned`.

---

### Generated OpenAPI / TS client (optional)

If Springdoc exposes `MemoryTracker.commissioned`, regenerate — never hand-edit `packages/generated/`:

```bash
CURSOR_DEV=true nix develop -c pnpm generateTypeScript
```

`MemoryTrackerLite` (due list wire) has no commissioned field today — leave as-is for Phase 1.

## Shared Patterns

### Boolean discriminator on MemoryTracker
**Source:** `MemoryTracker.java` `spelling` / `removedFromTracking`
**Apply to:** Entity field, Flyway column, MakeMe builder helper
```java
@Column(name = "spelling")
@Getter
@Setter
private Boolean spelling = false;
```
SQL: `tinyint(1) NOT NULL DEFAULT 0`

### Soft-delete-aware uniqueness
**Source:** `V100000000__baseline.sql:369`
**Apply to:** Migration that adds `commissioned` into UNIQUE key with `(if((deleted_at is null),1,NULL))`
```sql
UNIQUE KEY `user_note_spelling_active` (`user_id`,`note_id`,`spelling`,`property_key`,(if((`deleted_at` is null),1,NULL)))
```

### Due / count exclusion via shared SQL fragment
**Source:** `MemoryTrackerRepository.byUserIdFrom`
**Apply to:** Ordinary due list + `countByUserNotRemoved` / `totalAssimilatedCount`
```java
"   AND rp.removed_from_tracking IS FALSE "
+ "   AND rp.deleted_at IS NULL "
+ "   AND rp.commissioned IS FALSE ";
```

### Controller-boundary Structure proofs
**Source:** `RecallsControllerTests` + `AssimilationControllerTests`
**Apply to:** Exclusion and unassimilated-queue tests
- Extend `ControllerTestBase`, use `makeMe` + real DB
- Assert deltas only (`hasSize(1)`, note id) — `unit-testing.mdc`
- No new E2E / `@wip` in Phase 1

### Auth on recalls (unchanged)
**Source:** `RecallsController` — `authorizationService.assertLoggedIn()`
**Apply to:** No new endpoints in Phase 1

### Flyway hygiene
**Source:** `db-migration.mdc` + `V300000232` / `V300000237`
**Apply to:** New `V{>237}__*.sql` only; capability-named description (`commissioned`, not phase number)

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| — | — | — | No net-new role without analog. Unique-key **rebuild** has no prior Flyway DROP UNIQUE sibling after squash — compose from baseline DDL + boolean ADD pattern. |

## Metadata

**Analog search scope:** `backend/src/main/java/com/odde/doughnut/{entities,repositories,services,controllers}`, `backend/src/test/java/.../{builders,controllers}`, `backend/src/main/resources/db/migration/`, `docs/database-erd.md`
**Files scanned:** ~25 (focused on MemoryTracker / recall / assimilation / Flyway tip)
**Pattern extraction date:** 2026-08-07
