# Move property memory-tracker controls into the property-toggle UI

## Goal

Per-property memory tracker controls (Assimilate / Skip / Revive / Return to
sequence / Remove from recall) currently live in the "Assimilation settings"
panel (`AssimilationSettings.vue`). Move them into the property row's own
toggle-options area in the rich note editor
(`RichFrontmatterEditablePropertyRow.vue`, currently just a "Remove property"
button behind a chevron toggle). Note-level (no-`propertyKey`) controls stay
in `AssimilationSettings.vue`.

"Next to assimilate" navigation must land on the note with the target
property's toggle options auto-expanded, scrolled into view, and highlighted
— reusing the existing `usePendingAssimilationProperty` /
`useAssimilationView` mechanism, repointed at the new location.

## Decisions (confirmed with user)

1. `AssimilationSettings.vue`'s "Properties" section: remove the per-property
   rows entirely; keep only whole-note (no `propertyKey`) controls.
2. "Go to next to assimilate" should land the user somewhere the toggle UI is
   visible and usable (see discovery below — in practice this needs no
   explicit "enter edit mode" step for the common case).
3. The toggle-expanded state becomes externally controllable (driven by the
   pending-assimilation mechanism), not purely local click state.

## Key discovery (changes slice 4 from what was assumed when asking the user)

`RichFrontmatterEditablePropertyRow.vue` is NOT gated behind a manual
"edit mode" the user has to switch into. `RichFrontmatterProperties.vue`
picks it over the read-only list purely based on the `readOnly` prop, and
that prop is threaded from `NoteShow.vue`'s `readonly(noteRealm)` —
`!currentUser || notebookRealm.readonly === true` — i.e. it reflects
*write permission*, not a UI mode toggle. Any user who can edit the note
already sees `RichFrontmatterEditablePropertyRow` rows at all times. So
"auto-enter edit mode" is a non-issue for the normal case (the user assimilating
their own note already has write access); it only matters for the edge case
of a genuinely read-only notebook, where memory-tracker actions aren't
actionable anyway. No special edit-mode-forcing logic is needed — slice 4
only needs to wire the existing pending-property composable into the row.

## Architecture note for slice 1

`AssimilationSettings.vue` currently gets `noteRecallInfo` from `NoteInfoBar`
(mounted inside itself) and its parent `AssimilationPanel.vue` owns all the
action handlers (`processAssimilate`, `processSkip`, `processRevive`,
`processReturnToSequence`, `processRemoveFromRecall`) plus the spelling
verification popup flow. `RichFrontmatterEditablePropertyRow.vue` lives in a
completely separate component tree (`NoteTextContent` → ... , a sibling of
`NoteToolbar` under `NoteShow.vue`), so it has no access to that state today.

Rather than duplicating the fetch and the action logic in two places, slice 1
extracts them into a shared composable that `NoteShow.vue` (the nearest
common ancestor of `NoteToolbar` and `NoteTextContent`) provides once per
note, and both `AssimilationSettings.vue` (note-level) and
`RichFrontmatterEditablePropertyRow.vue` (property-level) consume via
inject. This keeps "the memory-tracker action logic" in one place per
CLAUDE.md's cohesion principle, and avoids a duplicate `noteRecallInfo`
network fetch.

## Slices

### 1. [Structure] Extract shared memory-tracker action composable, provided per-note

Extract `noteRecallInfo` fetch/reload plus the five action handlers
(assimilate incl. spelling-verification flow, skip, revive, return-to-sequence,
remove-from-recall) out of `AssimilationPanel.vue` into a composable (e.g.
`useMemoryTrackerActions(note)`). `NoteShow.vue` calls it once and provides
the result; `AssimilationPanel.vue` is rewritten to consume it via inject
instead of owning the state itself.

No behavior change — `AssimilationSettings.vue` continues to work exactly as
today (verified by existing tests). This is purely groundwork so
step 2 can reach the same state/actions without a second fetch or duplicated
logic.

Status: done.

**Result:** Created `useMemoryTrackerActions.ts` (fetch/reload/provide/inject)
and `useMemoryTrackerActionHandlers.ts` (the five action handlers + spelling
flow — split out during refactor to stay under the file-size limit).
Injection key `memoryTrackerActionsKey` follows the existing typed
`InjectionKey` + `Symbol(...)` convention. `NoteShow.vue` provides it once;
`AssimilationPanel.vue` and `AssimilationSettings.vue` both consume via
`useInjectedMemoryTrackerActions` and now share one `noteRecallInfo` fetch —
`AssimilationPanel.vue` also `provide()`s the same instance so
`AssimilationSettings.vue` gets it correctly even in isolated component-mount
tests that don't nest under `NoteShow.vue`. `NoteInfoBar.vue` (and its spec)
were deleted as dead code once `AssimilationSettings.vue` switched to
rendering `NoteInfoComponent` directly. Slice 2 can now inject the same
composable into `RichFrontmatterEditablePropertyRow.vue` with no extra fetch.

### 2. [Behavior] Property's toggle-options area gets working memory-tracker controls

Render `AssimilationButtons` inside `RichFrontmatterEditablePropertyRow.vue`'s
expanded options area (next to the existing Remove button), wired to the
injected composable's per-property action handlers and
disabled/skipped/removable state (mirroring the logic currently in
`AssimilationSettings.vue`: `assimilateDisabledForProperty`,
`isSkippedForRecall`, `isSkippedFromAssimilationSequence`,
`showRemoveFromRecall`).

Observable behavior: a user expands a property's toggle in the rich editor
and can Assimilate / Skip / Revive / Return to sequence / Remove from recall
that property directly from there, with the same effect as using the old
panel controls.

Interim duplication is expected and fine: the equivalent controls still exist
in `AssimilationSettings.vue`'s Properties section until slice 3.

E2E: extend/add a scenario covering assimilating (or skipping) a property
from its toggle-options row.

Status: planned.

### 3. [Behavior] Remove per-property rows from Assimilation settings panel

Delete the "Properties" section's per-property `AssimilationButtons` rows
from `AssimilationSettings.vue` (the `<ul>` of `assimilation-property-row`
items), keeping only the whole-note controls at the bottom. Remove/update
tests that asserted property-level buttons existed in that panel.

Observable behavior: opening "Assimilation settings" for a note no longer
shows per-property action buttons there — only note-level actions remain.
Property-level actions are reachable solely via each property's own toggle
(slice 2). This is the point where the "move" is actually complete rather
than duplicated.

Status: planned.

### 4. [Behavior] "Next to assimilate" auto-expands and highlights the property's toggle

Wire `usePendingAssimilationProperty` (already built, already used for the
old panel) into `RichFrontmatterEditablePropertyRow.vue` /
`RichFrontmatterProperties.vue`: when the row's property key matches the
pending property for the current note, force its options open (OR the
existing local `optionsExpanded` click-state with `isPendingProperty(key)`),
scroll it into view, and apply the highlight style class currently used in
`AssimilationSettings.vue` (`rounded bg-primary/10 ring-1 ring-primary/30`).

Since `usePendingAssimilationProperty`'s pending state is already set by
`useGoToNextAssimilation.ts` on navigation, no changes are needed there —
this slice only repoints the *rendering* of "pending" from the old panel
location to the property row.

Observable behavior: clicking "assimilate next" (from wherever
`useGoToNextAssimilation` is triggered — home screen, nav menus, or after
completing the previous assimilation) navigates to the note and the target
property's row is already expanded, scrolled into view, and visually
highlighted, with no old panel involved.

E2E: extend the existing "assimilate next" scenario to assert the property
row (not the old panel row) ends up expanded/highlighted.

Status: planned.

## Out of scope

- Note-level memory tracker controls: unchanged, stay in
  `AssimilationSettings.vue`.
- No backend/API changes — `AssimilationController` and
  `MemoryTrackerController` are unaffected.
- No change to the spelling-verification popup UX beyond relocating which
  component triggers it (still `SpellingVerificationPopup.vue`, still
  Teleported to body).
