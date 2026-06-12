# Assimilation queue restructure

**Status: complete** (all 8 phases done).

Follow-up to commit `d019af86fd` (property assimilation queue consumer). Addresses the
review findings: subscription-cap bug, eager-sort performance regression, count/queue
drift, construction shotgun surgery, Tell-Don't-Ask violations, naming, and file size.

## Key design decisions

- **Subscription daily budget covers all unit kinds.** A subscription's
  `needToLearnCountToday` budget limits the *combined* note + property units surfaced
  from that notebook per day (today only notes are capped — the bug).
- **Totals stay uncapped by design.** `totalUnassimilatedCount` is the full backlog;
  only the queue (and `dueCount`) apply daily caps. The count/queue "drift" to remove
  is the *parallel wiring*, not the cap difference.
- **Unified global ordering is intended** (level, createdAt, noteId, note-before-property,
  propertyKey) — it interleaves owned and subscribed notes, unlike the pre-`d019af86fd`
  subscription-first order. Pin it with regression tests rather than revert.
- **One candidate-source seam.** Notes and properties become two implementations of one
  `AssimilationUnitSource` concept (count + stream, for user scope and subscription
  scope), consumed by `AssimilationService` for both counts and queue. A third unit kind
  later means one new class.

## Discoveries that shape the work

- SQL ordering of every source stream already matches `AssimilationUnit.ORDER`:
  `NoteRepository.recallOrderByDate` = `level, createdAt, id`;
  `NotePropertyIndexRepository.unassimilatedOrderBy` = `level, createdAt, id, propertyKey`.
  → a lazy min-over-stream-heads "next" is correct without in-memory sorting.
- `note.level` is `tinyint NOT NULL DEFAULT 0` (baseline migration) → no comparator
  NPE risk; no null-safety change needed (review finding #5 closed, no action).
- `sorted().limit(n).findFirst()` with n > 0 ≡ `sorted().findFirst()` → the user
  daily-cap branch in `getNextAssimilationUnit()` is dead code (pre-existing).
- Local `cypress run` currently fails with a tsx/esbuild config error; phases needing
  E2E verification rely on CI or a fixed local Cypress.

## Phases

### Phase 1 — Subscription daily budget caps property units (behavior, bug fix) [done]

- Per subscription, build one merged unit stream: note units + property units, ordered
  by `AssimilationUnit.ORDER`, limited to `needToLearnCountToday`.
- Replace the separate `subscriptionNoteUnits()` / uncapped `subscriptionPropertyUnits()`
  in `AssimilationService.allCandidateUnits()`.
- Tests (extend `AssimilationServicePropertyUnitsTest`): subscription with `daily(1)`,
  one unassimilated note + one untracked property → only one unit surfaces today, note
  first; after the note is assimilated today, the property is *not* offered the same day;
  next day the property surfaces. `totalUnassimilatedCount` stays uncapped.
- Commit → deploy gate.

### Phase 2 — Pin queue ordering with regression tests (regression) [done]

Existing behavior, no automated test (skill: dedicated regression phase). Protects
Phases 3–4.

- In `AssimilationServiceTest` (or the property-units test where property cases fit):
  - Owned and subscribed notes interleave by `level, createdAt, id` (not
    subscription-first).
  - On the same note: note-level unit before property unit.
  - Multiple untracked properties on one note ordered by `propertyKey`.
- No production change expected. Commit.

### Phase 3 — One candidate-source seam; kill count/queue parallel wiring (structure) [done]

Enables Phase 4 and removes the "add a unit kind = touch five layers" shotgun.

- Introduce `AssimilationUnitSource` with two implementations (unassimilated notes,
  unassimilated properties), each exposing count and unit-stream for a `User` scope and
  a `Subscription` scope. `AssimilationService` iterates the sources for **both**
  `getCounts()` and the queue.
- Tell-Don't-Ask: sources take `User` / `Subscription` objects; stop digging
  `user.getOwnership().getId()` / `sub.getNotebook().getId()` at call sites
  (navigation happens once, inside the source/service that owns it).
- Renames (one concept, one name — "unassimilated"):
  `NotePropertyIndexRepository.countUntrackedProperties*` / `streamUntrackedProperties*`
  → `countUnassimilatedProperties*` / `streamUnassimilatedProperties*` (and the shared
  JPQL fragment constants).
- `AssimilationCounter`: `subscribedCount` → `subscribedUnitCount`, `userCount` →
  `ownedUnitCount`.
- Delete the dead daily-cap branch in `getNextAssimilationUnit()` and the now-misleading
  `unitsEligiblePastUserDailyCap` comment; the method becomes
  `allCandidateUnits().sorted(ORDER).findFirst()` (laziness restored in Phase 4).
- Verified externally unchanged: existing service/controller tests pass unmodified
  (except mechanical renames). Commit.

### Phase 4 — Lazy "next": stop materializing the backlog (structure, performance) [done]

- Replace `allCandidateUnits().sorted(ORDER).findFirst()` with a min-over-heads merge:
  take the head of each SQL-ordered source stream (per-subscription merged streams from
  Phase 1 included) and pick the minimum by `AssimilationUnit.ORDER`; close all streams.
- Correctness rests on SQL order = comparator order (verified, see Discoveries) and on
  Phase 2 regression tests.
- Note: per-subscription budget streams still need `assimilatedNoteIdsForToday()`; keep
  that single query, but the backlog itself is no longer fully materialized.
- Verified externally unchanged. Commit.

### Phase 5 — Collapse `AssimilationService` construction shotgun (structure) [done]

- Spring-managed `AssimilationServiceFactory` (collaborators injected once) with
  `create(User, Timestamp, ZoneId)`; `AssimilationController`, `UserController`, and the
  two test helpers use it. Adding a future collaborator touches one class.
- Delete the `AssimilationScope` record (its `user` field is dead; with the factory the
  record loses its purpose).
- DTO presentation (Tell-Don't-Ask): `AssimilationNextDTO.from(Optional<AssimilationUnit>,
  counts)` so the controller stops interrogating
  `nextUnit.filter(isPropertyLevel).map(propertyKey)`.
- Verified externally unchanged. Commit.

### Phase 6 — API models the assimilation unit as one object (behavior, API contract) [done]

- `AssimilationNextDTO`: replace flat `nextNoteId` + `nextPropertyKey` with a nullable
  `nextUnit { noteId, propertyKey? }` object, preserving the domain concept across the
  boundary.
- Regenerate OpenAPI/TS client (`generate-api-client` skill); update
  `useGoToNextAssimilation` (+ spec) to read `data.nextUnit`.
- E2E: run `e2e_test/features/recall/property_memory_tracker.feature` spec (CI if local
  Cypress still broken) — behavior identical, contract changed.
- Lowest-risk point to stop before: Phases 7–8 don't depend on it.
- Commit → deploy gate.

### Phase 7 — Frontend assimilation-view naming (structure) [done]

- `useAssimilationView`: `requestOnFor` → `openForNote(noteId, pendingPropertyKey?)`,
  `pendingOnForNoteId` → `targetNoteId`, `isOnForNote` → `isOpenForNote` (keep
  `pendingPropertyKey`, `dismiss`, `toggle`).
- Remove the test-only `resetAssimilationViewForTests` export; specs reset via the
  public API (`dismiss()`) or module reset.
- Update usages: `AssimilationSettings.vue`, `usePendingAssimilationProperty`,
  `useGoToNextAssimilation`, specs. Verified by existing frontend tests. Commit.

### Phase 8 — Split oversized files (structure) [done]

- `e2e_test/start/pageObjects/assimilationPage.ts` (373 lines) → cohesive modules by
  capability (e.g. assimilation flow vs memory-tracker expectations), stable import
  surface for step definitions.
- `AssimilationServiceTest.java` (314 lines) → split by capability (daily cap,
  subscription queue, ordering), names reflect behavior not phases.
- Related tests pass. Commit.

## Out of scope

- Capping `totalUnassimilatedCount` by subscription daily limits (totals are
  intentionally the full backlog).
- Comparator null-safety for `level` (column is NOT NULL — closed, no action).
- Reverting to subscription-first ordering (unified ordering is the intended design).
