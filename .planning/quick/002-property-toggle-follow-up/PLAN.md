# Property-toggle follow-up: list, next-property, leftovers

## Goal

Finish what moving per-property memory-tracker controls into the
property toggle left incomplete. Keep Skip vs Remove from recall as two
user-visible states (**Return to sequence** vs **Revive**). Do not show
skipped property trackers in the note Memory Trackers table. Do not open
Assimilation settings when the next unit is a property.

## Source

Inspected original plan (last copy at `5c7163c45f`) and commits
`ca68e7c554` … `f55d3878ca`.

## Decisions

1. Skip (never assimilated) stays **Return to sequence** on the property
   toggle. Remove from recall stays **Revive**. Do not collapse them.
2. The note Memory Trackers table is note-level inventory plus *active*
   property trackers (so “open this property’s tracker page” still works).
   Skipped (`removedFromTracking`) **property** trackers do not belong
   there — their undo lives on the property toggle. Note-level skipped
   rows stay (that is where note-level Revive lives).
3. After Skip, tests assert **Return to sequence** on the property toggle.
   They must not treat “no row in the Memory Trackers table” as the
   skip outcome (that only probes storage).

## Discoveries (inspection)

**Bugs**

- **Skipped property rows still listed.** `NoteInfoComponent` renders
  every `getNoteInfo` tracker, including `removedFromTracking` property
  rows (backfill dummies and Remove from recall). Test
  `should display all memory trackers including skipped ones` only covers
  note-level skipped rows; it never excluded property grain.
- **Next property still opens Assimilation settings.**
  `useAssimilationView.openForNote` always sets `activePanel` to
  `"assimilation"`. Original slice 4 said the pending property should
  land on the toggle “with no old panel involved.” Note-level next
  should still open the panel. `resetForNote` re-opens the panel when
  staying on the same note, so a property pending would also reopen
  settings after navigation.

**Weak / leftover tests**

- Skip scenario still opens Assimilation settings and asserts
  `the property memory tracker for "topic" should be absent`. Skip does
  not create a tracker, so that assertion is true for the wrong reason.
  The observable outcome is already “Return to sequence” on the toggle.
- Several scenarios `open assimilation settings` only to then drive the
  property toggle (Skip / Return to sequence / Remove from recall).
- Cucumber step `I should not see pending assimilation property` and
  `expectPendingAssimilationPropertyAbsent` have **no feature caller**.
- Unit skip/assimilate cases in
  `RichMarkdownEditor.propertyAssimilation.spec.ts` overlap E2E on the
  happy path; they still pin the API payload. Keep them. Do not add more
  of the same.

**Missed refactor / smells**

- `usePendingAssimilationProperty` is constructed **once per property
  row**, each with its own `Map` + `watch`. Only that row’s key is ever
  registered. One instance on the properties list is enough.
- `scrollPendingPropertyIntoView().catch(() => undefined)` swallows
  failures (`error-handling.mdc`).
- `RichFrontmatterProperties.vue` is 249 lines — hoisting pending state
  into it must not push it over 250; extract if needed.

**Out of scope**

- Pre-existing E2E `Removing tracked property deletes property memory
  tracker` (failed on main before this work).
- Unifying Skip and Remove from recall onto one Revive button.
- Hiding *active* property trackers from the note list (still the path
  to the property tracker page).

## Slices

### 1. Skipped property trackers leave the note Memory Trackers table

Type: Behavior
Status: planned

Pre-condition: a note has a property understanding tracker; the learner
has removed that property from recall (or a skipped property tracker
already exists).

Trigger: open Assimilation settings.

Post-condition: the Memory Trackers table has no strikethrough
`property: …` row. The property toggle shows **Revive**. Skip (never
assimilated) still shows **Return to sequence** on the toggle; that
scenario no longer asserts list absence and no longer opens settings
just to look at the list.

E2E: in `property_memory_tracker.feature`, rewrite Skip to assert
Return to sequence on the toggle only; extend Remove from recall so
after Revive is visible the property is absent from the table. Unit:
`NoteInfoComponent` — skipped property tracker omitted; skipped
note-level tracker still listed.

### 2. Next property to assimilate does not open Assimilation settings

Type: Behavior
Status: planned

Pre-condition: the next assimilation unit is a property (e.g. untracked
`example of` after the note itself is assimilated).

Trigger: start assimilation from the menu (or go to next after a
previous assimilation).

Post-condition: the target property row is pending (expanded,
highlighted). Assimilation settings is **not** shown. Note-level next
(no `propertyKey`) still opens settings.

`openForNote` / `resetForNote` must not force the panel open when
`pendingPropertyKey` is set. E2E: existing “Untracked example of
property appears in assimilation queue” asserts the pending row and
that `#assimilation-settings` (or `data-testid="assimilation-settings"`)
is absent. Unit: `useGoToNextAssimilation` / `useAssimilationView` —
property next leaves `showAssimilationSettings` false; note next still
true.

### 3. One pending-property owner; drop dead pending-absent E2E

Type: Structure
Status: planned

Unlocks nothing further; this is the missed wrap-up of original slice 4
(pending highlight wired per row, unused absent helper, swallowed
scroll).

- Construct `usePendingAssimilationProperty` once (properties list, not
  each row); pass pending + root-ref into the row. Stay under 250 lines
  (extract if `RichFrontmatterProperties.vue` would overflow).
- Propagate scroll failure; do not `.catch(() => undefined)`.
- Delete unused `I should not see pending assimilation property` and
  `expectPendingAssimilationPropertyAbsent`.

Existing pending E2E and
`RichMarkdownEditor.propertyAssimilation.spec.ts` auto-expand tests
must still pass.

## Out of scope

- Note-level Assimilate / Skip / Revive / spelling popup location.
- Backend / backfill of dummy skipped property trackers (they can remain
  in the DB; they just must not appear in the note table).
- The pre-existing “removing tracked property deletes tracker” E2E
  failure.
