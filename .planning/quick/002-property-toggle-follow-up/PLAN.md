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

(none remaining from this plan)

**Weak / leftover tests**

- Several scenarios `open assimilation settings` only to then drive the
  property toggle (Return to sequence / Remove from recall). Skip no
  longer opens settings just to look at the list.
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
  tracker` (listed as a possible independent failure; passed during
  slice 1).
- Unifying Skip and Remove from recall onto one Revive button.
- Hiding *active* property trackers from the note list (still the path
  to the property tracker page).

## Slices

### 1. Skipped property trackers leave the note Memory Trackers table

Type: Behavior
Status: done

List omits `removedFromTracking` property trackers via
`isNoteLevelMemoryTracker` (note-level skipped rows stay). Skip E2E
asserts **Return to sequence** on the toggle only; Remove from recall
asserts **Revive** then table absence. Unit:
`should omit skipped property memory trackers from the table`.

Learning: table filter reuses `isNoteLevelMemoryTracker`; do not treat
list absence as the Skip outcome. Out-of-scope “removing tracked
property” E2E passed in this run.

### 2. Next property to assimilate does not open Assimilation settings

Type: Behavior
Status: done

`openForNote` / `resetForNote` share `openSettingsUnlessPropertyPending`
(close settings if already open when `pendingPropertyKey` is set).
Note-level next still opens settings. Queue E2E asserts pending
`example of` and `I should not see assimilation settings` (progress
`1/2/2` dropped — that triple only lives inside the settings panel).

Learning: keep `expectAssimilationSettingsAbsent` when slice 3 deletes
the unused pending-absent helper.

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
  `expectPendingAssimilationPropertyAbsent`. Keep
  `I should not see assimilation settings` /
  `expectAssimilationSettingsAbsent` (slice 2 behavior).

Existing pending E2E and
`RichMarkdownEditor.propertyAssimilation.spec.ts` auto-expand tests
must still pass.

## Out of scope

- Note-level Assimilate / Skip / Revive / spelling popup location.
- Backend / backfill of dummy skipped property trackers (they can remain
  in the DB; they just must not appear in the note table).
- The pre-existing “removing tracked property deletes tracker” E2E
  failure.
