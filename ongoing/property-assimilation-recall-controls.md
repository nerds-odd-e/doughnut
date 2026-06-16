# Property assimilation recall controls

Align property-level assimilation actions in **Assimilation settings → Properties** with the note-level **Assimilate** / **Skip recall** controls.

## Requirement analysis

### What the user asked for

| # | Requirement | Observable outcome |
|---|-------------|-------------------|
| 1 | Property row primary button labeled **Assimilate** | Property row primary button label matches note-level assimilation |
| 2 | Disable **Assimilate** when property is already assimilated | Button is disabled once a memory tracker exists for that `propertyKey` |
| 3 | Add **Skip recall** for each property, same as note | Secondary button; confirmation; creates a skipped property tracker so the property leaves the unassimilated queue |

### Current behavior (grounding)

- **Note-level** (`AssimilationButtons.vue` via `AssimilationPanel.vue`):
  - **Assimilate** → `POST /api/assimilation` with `{ noteId, skipMemoryTracking: false }`
  - **Skip recall** → confirm *"Confirm to hide this note from recalls in the future?"* → `{ noteId, skipMemoryTracking: true }`
  - **Assimilate** disabled when a note-level memory tracker already exists (`assimilateDisabled` in `AssimilationPanel.vue`), except the spelling-only add case
  - **Skip recall** stays enabled when note info is loaded (mirrors `AssimilationButtons.vue`: only `disabled`, not `assimilateDisabled`)
  - On success: updates assimilation counts and calls `goToNextAssimilation()`

- **Property-level** (`AssimilationSettings.vue` lines 66–73):
  - Primary button labeled **Assimilate** (via shared `AssimilationButtons`)
  - Calls `AssimilationController.assimilate({ noteId, propertyKey })` — no `skipMemoryTracking` on keep path
  - Disabled only while the in-flight request runs (`assimilatingPropertyKey`), plus when already assimilated (`assimilateDisabledForProperty`)
  - On success: reloads note info via `NoteInfoBar.reload()` — does **not** advance the assimilation queue on keep

- **Backend** (`MemoryTrackerService.assimilate`):
  - Already accepts `propertyKey` + `skipMemoryTracking` together
  - `createPropertyMemoryTracker` sets `removedFromTracking = skipMemoryTracking`
  - Idempotent: second assimilate for the same property returns empty (tracker already exists)
  - Skipped property trackers are treated as assimilated for queue purposes (`UnassimilatedPropertyServiceTest.does_not_count_when_property_tracker_is_skipped`)

### Design decisions

1. **Reuse `AssimilationButtons.vue`** on each property row (with a `size="sm"` prop) so labels, disabled rules, and `data-test="assimilate"` stay consistent with note-level.
2. **"Already assimilated"** for a property = any active memory tracker with matching `propertyKey`, including `removedFromTracking: true` (skipped). Same rule the backend uses for idempotency and unassimilated counts.
3. **Skip confirmation** — property-specific copy, e.g. *"Confirm to hide this property from recalls in the future?"* (parallel to the note message).
4. **Queue advancement on property skip** — note **Skip recall** advances via `goToNextAssimilation()`. Property skip does the same when assimilation succeeds, so a pending property unit in the assimilation walkthrough is cleared. Property **Assimilate** keeps reload-only behavior (no auto-advance).
5. **Note recall info in settings** — `AssimilationSettings` holds `NoteRecallInfo` (from the existing `noteRecallInfoLoaded` event / `NoteInfoBar`) to compute per-property disabled state. No new API.

### Tests to extend (capability-named)

| Layer | File |
|-------|------|
| Frontend unit | `frontend/tests/components/recall/AssimilationSettings.spec.ts` |
| Backend unit | `backend/.../MemoryTrackerServiceTest.java` (property + `skipMemoryTracking`) |
| E2E | `e2e_test/features/recall/property_memory_tracker.feature` |
| E2E page object | `e2e_test/start/pageObjects/assimilationPage/assimilationFlow.ts` |

---

## Phases (completed)

All phases delivered. Terminology unified on **Assimilate** / **Skip recall** at both note and property level.

## Out of scope (unless requested later)

- Renaming or changing note-level **Skip recall** behavior
- Auto-advancing the queue on property **Assimilate** (today only reloads note info)
- Spelling verification for properties (property trackers are normal MCQ only)
