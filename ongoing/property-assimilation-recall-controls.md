# Property assimilation recall controls

Align property-level assimilation actions in **Assimilation settings → Properties** with the note-level **Keep for recall** / **Skip recall** controls.

## Requirement analysis

### What the user asked for

| # | Requirement | Observable outcome |
|---|-------------|-------------------|
| 1 | Rename property **Assimilate** → **Keep for recall** | Property row primary button label matches note-level assimilation |
| 2 | Disable **Keep for recall** when property is already assimilated | Button is disabled once a memory tracker exists for that `propertyKey` |
| 3 | Add **Skip recall** for each property, same as note | Secondary button; confirmation; creates a skipped property tracker so the property leaves the unassimilated queue |

### Current behavior (grounding)

- **Note-level** (`AssimilationButtons.vue` via `AssimilationPanel.vue`):
  - **Keep for recall** → `POST /api/assimilation` with `{ noteId, skipMemoryTracking: false }`
  - **Skip recall** → confirm *"Confirm to hide this note from recalls in the future?"* → `{ noteId, skipMemoryTracking: true }`
  - **Keep for recall** disabled when a note-level memory tracker already exists (`keepForRecallDisabled` in `AssimilationPanel.vue`), except the spelling-only add case
  - **Skip recall** stays enabled when note info is loaded (mirrors `AssimilationButtons.vue`: only `disabled`, not `keepForRecallDisabled`)
  - On success: updates assimilation counts and calls `goToNextAssimilation()`

- **Property-level** (`AssimilationSettings.vue` lines 66–73):
  - Single button labeled **Assimilate**
  - Calls `AssimilationController.assimilate({ noteId, propertyKey })` — no `skipMemoryTracking`
  - Disabled only while the in-flight request runs (`assimilatingPropertyKey`)
  - On success: reloads note info via `NoteInfoBar.reload()` — does **not** advance the assimilation queue

- **Backend** (`MemoryTrackerService.assimilate`):
  - Already accepts `propertyKey` + `skipMemoryTracking` together
  - `createPropertyMemoryTracker` sets `removedFromTracking = skipMemoryTracking`
  - Idempotent: second assimilate for the same property returns empty (tracker already exists)
  - Skipped property trackers are treated as assimilated for queue purposes (`UnassimilatedPropertyServiceTest.does_not_count_when_property_tracker_is_skipped`)

### Gaps

1. **Label** — property button still says "Assimilate" while note says "Keep for recall"
2. **Disabled state** — property keep is never disabled after assimilation; API no-ops but UI misleads
3. **Skip** — no UI path to `skipMemoryTracking: true` for a property

### Design decisions

1. **Reuse `AssimilationButtons.vue`** on each property row (with a `size="sm"` prop) so labels, disabled rules, and `data-test="keep-for-recall"` stay consistent with note-level.
2. **"Already assimilated"** for a property = any active memory tracker with matching `propertyKey`, including `removedFromTracking: true` (skipped). Same rule the backend uses for idempotency and unassimilated counts.
3. **Skip confirmation** — property-specific copy, e.g. *"Confirm to hide this property from recalls in the future?"* (parallel to the note message).
4. **Queue advancement on property skip** — note **Skip recall** advances via `goToNextAssimilation()`. Property skip should do the same when assimilation succeeds, so a pending property unit in the assimilation walkthrough is cleared. Property **Keep for recall** keeps today's behavior (reload note info only, no auto-advance) unless we later decide to align that too.
5. **Note recall info in settings** — `AssimilationSettings` must hold `NoteRecallInfo` (from the existing `noteRecallInfoLoaded` event / `NoteInfoBar`) to compute per-property disabled state. No new API.

### Tests to extend (capability-named)

| Layer | File |
|-------|------|
| Frontend unit | `frontend/tests/components/recall/AssimilationSettings.spec.ts` |
| Backend unit | `backend/.../MemoryTrackerServiceTest.java` (property + `skipMemoryTracking`) |
| E2E | `e2e_test/features/recall/property_memory_tracker.feature` |
| E2E page object | `e2e_test/start/pageObjects/assimilationPage/assimilationFlow.ts` |

---

## Phases and commit-sized sub-phases

**Commit discipline:** every sub-phase below is **one commit** that compiles and leaves all unit + touched E2E tests green (a sub-phase that authors an E2E scenario for not-yet-built behavior tags it `@wip` so it is green in CI). Push after each commit. The **deploy gate** (let CD deploy) applies at each **phase** boundary, not every sub-phase. Targeted E2E only: `cypress run --spec e2e_test/features/recall/property_memory_tracker.feature`.

---

### Phase 1 — Property button labeled "Keep for recall" *(Behavior)*

**Outcome:** Each property row shows **Keep for recall** (not "Assimilate"); clicking still assimilates the property.

- **1a — `AssimilationButtons` small size** *(structure)* — **Status: done**
  - Add optional `size` prop (`"default"` | `"sm"`) to `AssimilationButtons.vue`; when `sm`, apply `daisy-btn-sm`.
  - Note-level usage unchanged (default size).
  - **Green check:** existing frontend tests for note assimilation pass. No observable change. **Commit.**

- **1b — Property row uses `AssimilationButtons` (keep only)** *(behavior)* — **Status: done**
  - Replace the property row `<button>Assimilate</button>` in `AssimilationSettings.vue` with `<AssimilationButtons size="sm" … @assimilate="(skip) => assimilateProperty(row.key, skip)" />` — initially only the `skip === false` path is reachable because skip button wiring comes in Phase 3.
  - **Tests (this commit):** update `AssimilationSettings.spec.ts` — row shows **Keep for recall**; click still calls API with `{ noteId, propertyKey }` (no `skipMemoryTracking`). **Commit.**

- **1c — E2E page object** *(behavior)* — **Status: done**
  - Update `assimilationFlow.ts` `assimilateProperty` to click **Keep for recall** instead of **Assimilate**.
  - **Green check:** run `property_memory_tracker.feature --spec`; all scenarios green. **Commit.** → **Phase 1 deploy gate.**

---

### Phase 2 — Disable property "Keep for recall" when assimilated *(Behavior)*

**Outcome:** Once a property memory tracker exists (kept or skipped), **Keep for recall** for that property is disabled. **Skip recall** (added in Phase 3) stays enabled when note info is loaded — matching note-level rules.

- **2a — Hold `noteRecallInfo` in settings** *(structure)* — **Status: done**
  - Add `noteRecallInfo` ref in `AssimilationSettings.vue`; set it on `noteRecallInfoLoaded` and after successful `noteInfoBarRef.reload()`.
  - No UI change yet.
  - **Green check:** existing tests pass unchanged. **Commit.**

- **2b — Per-property `keepForRecallDisabled`** *(behavior)* — **Status: planned**
  - `keepForRecallDisabledForProperty(key)` → `noteRecallInfo?.memoryTrackers?.some(mt => mt.propertyKey === key)`.
  - Pass `:keep-for-recall-disabled` per property row into `AssimilationButtons`.
  - **Tests (this commit):** `AssimilationSettings.spec.ts` — mock `getNoteInfo` with a `topic` property tracker; keep disabled for `topic`, still enabled for `url`. **Commit.**

- **2c — E2E disabled assertion** *(behavior)* — **Status: planned**
  - Add page-object helper `expectPropertyKeepForRecallDisabled(propertyKey)` (and optionally `expectPropertyKeepForRecallEnabled`).
  - Add step def + scenario beat in `property_memory_tracker.feature`: after Background assimilates `"topic"`, keep for `"topic"` is disabled.
  - **Green check:** targeted E2E green. **Commit.** → **Phase 2 deploy gate.**

---

### Phase 3 — Property "Skip recall" *(Behavior)*

**Outcome:** Each property row has **Skip recall**; confirming hides the property from future recalls, clears it from the unassimilated queue, and advances the assimilation walkthrough.

- **3a — Backend unit for property skip** *(behavior)* — **Status: planned**
  - `MemoryTrackerServiceTest`: assimilate with `propertyKey` + `skipMemoryTracking: true` → tracker created with `removedFromTracking: true`.
  - **Green check:** `backend:verify` (or `backend:test_only`). **Commit.**

- **3b — E2E scenario (red, `@wip`)** *(behavior, test-first)* — **Status: planned**
  - Add scenario to `property_memory_tracker.feature`: fresh note with one untracked property → skip recall on property → property tracker absent from recall queue / unassimilated count drops.
  - Add `skipRecallProperty(propertyKey)` to `assimilationFlow.ts` + step def (confirm dialog, same pattern as note skip).
  - Tag `@wip`.
  - **Green check:** `@wip` skipped in CI; run locally with `--spec` — fails for right reason (skip button missing). **Commit.**

- **3c — Wire property skip in UI** *(behavior)* — **Status: planned**
  - In `assimilateProperty(key, skipMemoryTracking)`: when `skip`, show confirm *"Confirm to hide this property from recalls in the future?"*; call API with `{ noteId, propertyKey, skipMemoryTracking: true }`; reload note info on success.
  - Keep path unchanged (`skipMemoryTracking: false`, no confirm).
  - **Tests (this commit):** `AssimilationSettings.spec.ts` — skip click → confirm → API body includes `skipMemoryTracking: true`. Rerun `@wip` scenario locally until skip + tracker semantics pass (queue advance not required yet). **Commit.**

- **3d — Queue advancement on property skip** *(behavior)* — **Status: planned**
  - On successful property skip: call `goToNextAssimilation()` (import from `useGoToNextAssimilation`, same as note skip in `AssimilationPanel`).
  - Do **not** add queue advance to property keep (out of scope).
  - **Tests (this commit):** frontend unit or extend `AssimilationSettings.spec.ts` to assert navigation hook called on skip success; rerun `property_memory_tracker.feature --spec` until all scenarios green; **remove `@wip`**. **Commit.** → **Phase 3 deploy gate.**

---

## Out of scope (unless requested later)

- Renaming or changing note-level assimilation behavior
- Auto-advancing the queue on property **Keep for recall** (today only reloads note info)
- Spelling verification for properties (property trackers are normal MCQ only)
