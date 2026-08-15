# Skip Memory Tracking — leftover cohesion

**Status:** in-progress  

**Source:** Inspection of the shipped Skip Memory Tracking work (`7e19361038`…`89a3a32202`). No user-facing bugs. Remaining work is Structure only: redundant tests, a now-dead fixture helper, and repository method names that still say “no memory tracker” after the sequence filter grew.

**Inspected and not in this plan**

| Finding | Why not |
|---------|---------|
| No NULL / unbox bug on `skipMemoryTrackingEntirely` | Column is `tinyint NOT NULL DEFAULT 0`; JPQL `= false` and `if (getSkipMemoryTrackingEntirely())` are safe |
| Walkthrough E2E vs `omitsOwnedNotesFromNotebookWithSkipMemoryTracking` | Different surfaces (UI vs `/next` counts); keep both |
| Property and subscribed `/next` siblings | Unique query paths; keep |
| Subscribe UI CTA vs API 400 | Intentional two layers; collapsing would be cross-subsystem |
| `JPA_NOTEBOOK_NOT_SKIP_MEMORY_TRACKING` name | Names the setting that is filtered, not “whole sequence membership” (sequence-skip is a separate fragment) |
| Rename persisted `skipMemoryTrackingEntirely` | Still a deliberate later slice |
| Commissioned assimilate tests in their own class | File-size split already landed on a cohesive seam |

---

## Phase 1: Drop skip-flag tests that do not exercise a production branch

**Type:** Structure  
**Status:** done

**Done:** Removed `AssimilationControllerAssimilateTests.assimilateOnSkipMemoryTrackingNotebookCreatesUnderstandingTracker`, `UserMenuDataControllerTest.skipMemoryTrackingNoteTrackerAppearsInRecallDue`, and `NoteBuilder.skipMemoryTrackingEntirely`. Kept `NotebookBuilder.skipMemoryTrackingEntirely` and remaining `/next` / subscribe / E2E / Settings coverage.

**Learning:** No leftover NoteBuilder callers; file sizes stayed under 250. Phase 2 rename is still the remaining work.

---

## Phase 2: Name unassimilated sequence queries as unassimilated

**Type:** Structure  
**Status:** planned

**Structure change:** `NoteRepository` methods used by the assimilation sequence still say `WhereThereIsNoMemoryTracker`, but the shared `unassimilatedWhereClause` also excludes sequence-skip and **Skip Memory Tracking**. Rename the four methods to `Unassimilated` so they match `UserService` / `SubscriptionService` (`getUnassimilatedNotes` / `getUnassimilatedNoteCount`). Keep the `ByOwnership` / `ByAncestor` suffixes (do not rename Ancestor→Notebook in this phase).

**Immediate next:** none — leftover naming from the shipped filter; no further behavior in this plan.

**Likely files:** `NoteRepository.java`, `UserService.java`, `SubscriptionService.java`.

**Verify:** `CURSOR_DEV=true nix develop -c pnpm backend:test_only` (`backend:test_only` itself may exceed 10 minutes — stated good reason)
