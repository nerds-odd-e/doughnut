# Narrow the property key presets and keep them off the value field

**Identity:** SEED-064#story-2
**Source:** [story](../../seeds/SEED-064-note-properties-fixes.md#story-2), refined 2026-09-30 from the owner's
review answers, taken as proposed when the owner asked for this plan: contains-match on typed text, a phone outcome
without a fixed mechanism, real-iPad keyboard as a manual check, Tab order deferred.

## Goal and scope

An iPad or phone user who types a property key sees only the matching presets and can tap the value field, or Choose
image, with one tap while the list is showing.

Included: filtering by the text typed since the key field took focus; options that stay inside their key panel; a list
that does not cover the value field or Choose image at 375 px nor the value column at 820 px; one key field shared by
the add form and the existing row (the fix is made once).

Excluded (story decisions): keyboard-aware positioning, Tab order into the presets, the numbered `url 2` entry (SEED-063
boundary), changes to which presets exist or their order, the row's density (story 1), the draft-row add flow
(story 6). No stored-Markdown behavior changes, so ADR 0004 is unaffected. No Accepted ADR or North Star topic governs
component layout or key entry, so this is an ordinary plan with no new direction.

Assumption: story 6 replaces the add form with a draft row and story 5 merges the read-only list into the row. Both
reuse the key field extracted in slice 1, which is why the fix belongs there and not in `RichFrontmatterInsertForm.vue`.

## Existing solutions (PFE)

Nothing new is built. Reused: `richModeKeyDropdownPresetKeysForPropertyRows` for the available presets (unchanged);
the two existing copies of the key-field logic are merged into one component; the existing component-test helpers
`expectPresetOptions` and `selectPresetKey` in `frontend/tests/components/form/propertiesTestDom.ts`. Story 1 landed
the viewport step (`I am on a window {int} * {int}`, `e2e_test/step_definitions/sidebar.ts`) and
`e2e_test/features/note_topology/note_property_layout.feature`; this plan extends them and creates no second step.

## Decisive premises

| Premise | Observation | Result |
| --- | --- | --- |
| Only the row and the add form use the preset list | `grep -rn "KeyPresets" frontend/src` | only `RichFrontmatterEditablePropertyRow.vue` and `RichFrontmatterInsertForm.vue` import it |
| The open/close/select logic is duplicated word for word | read both `<script setup>` blocks | `presetPanelOpen`, `onKeyPresetWrapperFocusOut`, `onPresetSelected` exist in both; the row also emits `row-focus` on focus and `commit` on blur, the add form focuses the value on Enter |
| The list never sees the typed text | read `RichFrontmatterPropertyKeyPresets.vue` | props are `listId`, `propertyRows`, `excludeRowIndex`; no text |
| Focusing an existing key shows every available preset today and a test relies on it | read `RichMarkdownEditor.propertyEntry.spec.ts`, "key presets" | focus on row key `custom` expects all presets; this fixes "filter on typed text only, not on the current text" |
| No e2e scenario covers presets | `grep -rni "preset" e2e_test` | no match; `noteRichPropertyMethods.ts` types keys through `rich-note-property-key` and the row key input, so those test ids must stay |
| Story 1 is on trunk | `ls e2e_test/features/note_topology \| grep layout`; `grep -rn "on a window" e2e_test/step_definitions` (2026-09-30, after story 1 landed) | `note_property_layout.feature` exists; the step is `I am on a window {int} * {int}`; story 1 changed `EditablePropertyRow.vue` (+/-2 lines) and `ScalarPropertyValue.vue` |
| The list sits below the key input in a `relative` wrapper and options are `daisy-btn` (single line) | read `KeyPresets.vue` and both wrappers | `absolute left-0 right-0 top-full w-full`; the options have no wrap rule, which is the likely cause of the 250 px width inside a 158 px panel (a hypothesis until slice 3's scenario fails) |
| The `sm` breakpoint (640 px) is where the add form's key and value sit side by side | read `InsertForm.vue` classes | `w-full sm:w-auto` on the key label and `w-full sm:flex-1` on the value label |
| An element covering a target can be detected in Cypress | not observed | slice 3's scenario asserts `document.elementFromPoint` at the target's centre returns the target or a descendant; the first red run confirms it |
| Story 1's row layout is small | `git diff 6c6642a4fa HEAD --stat` on the row components | one-line edits only; slice 3 still reads the row's classes before choosing the list rule |

## Outside-in proof

| Promise | Owner | Observable proof |
| --- | --- | --- |
| Add form and existing row behave as before | slice 1 | `pnpm frontend:test` for `RichMarkdownEditor.propertyEntry.spec.ts`, `RichMarkdownEditor.propertyRowEditing.spec.ts`, `RichMarkdownEditor.frontmatter.spec.ts` stay green with no assertion changed |
| Typing narrows the presets; no match hides the list; clearing restores it | slice 2 | component tests in `RichMarkdownEditor.propertyEntry.spec.ts`: `ur` → `url`; `UR` → `url`; `of` → `example of`; `mo` → no list; cleared → all available |
| Existing row: focus shows all, typing narrows, choosing closes the list and focuses the row's value | slice 2 | same spec, extending the existing "key presets" test |
| Value field and Choose image not covered at 375 px | slice 3 | Cypress scenario in `note_property_layout.feature`: type `url`, then `image`; `elementFromPoint` at the value field's and the Choose image button's centres returns them |
| Options stay inside the key panel at 375 and 820 px; value column not covered at 820 px | slice 3 | same scenario: with `question` typed, the list's right edge is at most the key panel's right edge and the option has no horizontal overflow |
| Real iPad check | owner, after slice 3 | manual observation with the software keyboard; not a slice |

jsdom has no layout, so slices 1 and 2 cannot prove positioning or width; slice 3's Cypress scenario does. Existing
`note_property.feature` and `note_frontmatter_image.feature` type keys through these fields and are run once after
slice 3, because the field's markup changed in slice 1 and its behavior in slices 2 and 3.

## Ordered slices

Size target is about 10 minutes each including proof. Slice 3 takes longer only by the first Cypress stack start.
Story 1 has landed, so there is no ordering constraint.

### 1. The add form and an existing row share one key field with its presets
Type: Structure
Status: done
Proof: the three frontend specs named above are green with no assertion changed; `pnpm frontend:test` for
`frontend/tests/components/form`.

Internal change: one component (input, preset list, open/close on focus and focus-out, choosing a preset) replaces the
duplicated logic in `RichFrontmatterEditablePropertyRow.vue` and `RichFrontmatterInsertForm.vue`. Test ids
`rich-note-property-key` and `rich-note-property-row-key-input` are kept by props. The row still emits `row-focus` and
`commit`, the add form still moves to its value on Enter. Enables slice 2's filtering and slice 3's layout to be
written once.

### 2. Typing a property key narrows the presets
Type: Behavior
Status: done
Proof: the component tests above, red before the change and green after.

Behavior: the add form or an existing row's key field is focused → the user types `ur` → only `url` is listed; `mo`
lists nothing; clearing the text lists every available preset; on an existing row, focusing without typing lists every
available preset; choosing one closes the list and moves focus to that row's value.

Change: the key field keeps the text typed since it took focus (cleared on focus and on choosing a preset) and passes
the case-insensitive contains-filter of the available presets' displayed names to the list. The existing list already
hides itself when empty.

### 3. The preset list stays inside its panel and off the value field
Type: Behavior
Status: done
Proof: `pnpm cy:run --spec e2e_test/features/note_topology/note_property_layout.feature` runs the new scenarios, red
before the change and green after; then `note_property.feature` and `note_frontmatter_image.feature` once.

Behavior: the owner's note opened at 375 px, Add property → typing `url` leaves the value field uncovered, typing
`image` leaves Choose image uncovered; at 820 px and 375 px typing `question` shows `question_generation_instruction`
wrapped inside the key panel; at 820 px the value column is not covered. The same width rule holds for an existing
row's key at 820 px.

Change: options wrap inside the panel (`h-auto`, `whitespace-normal`, `text-left`, `break-all`); the list is in the
normal flow where the value drops under the key and stays an overlay where they sit side by side, following the
merged row's breakpoint. If the scenario shows another rule is simpler (for example an in-flow list at all widths),
use it. Probe: if the first red run shows the covering is not caused by the list, stop and re-read the layout before
changing anything.

## Current decisions

- Filter on text typed since focus, not on the field's current text, so a row's existing key does not hide the other
  presets (the current "key presets" test requires this).
- Match the displayed preset name (contains, case-insensitive). The `url 2` entry is matched by its name like any
  other and is not tested (SEED-063 boundary).
- Layout scenarios extend `note_property_layout.feature` and the step `I am on a window {int} * {int}`; no second step is defined.
- Layout is proved at fixed widths in Cypress. Keyboard behavior is a manual check by the owner, not an assertion.

## Learnings

- Slice 1 (accepted): `RichFrontmatterPropertyKeyField.vue` (props `modelValue`, `inputId`, `listId`, `label`, `testId`, `propertyRows`, `excludeRowIndex`; emits `update:modelValue`, `focus`, `blur`, `enter`, `select`). Proof: `pnpm frontend:test tests/components/form` 21 files / 241 tests green, `vue-tsc --noEmit` clean. The add form's key input now also carries the row's `min-w-[8rem] text-ellipsis` (no visible change expected; layout is slice 3's proof).
- Slice 2 (accepted): `RichFrontmatterPropertyKeyField` keeps `typedText` (reset on focus) and passes `name-filter` to `RichFrontmatterPropertyKeyPresets` (case-insensitive contains). Proof: new describe "key preset narrowing" in `RichMarkdownEditor.propertyEntry.spec.ts` (red: 5 failures with 8 options listed; green), `tests/components/form` 21 files / 248 tests, `vue-tsc` clean. The reset of typed text on focus has no separate test.
- Slice 3 (accepted): the list is `sm:absolute` (in flow below 640 px, overlay from 640 px) with wrapping options (`h-auto whitespace-normal break-all text-left`). The `elementFromPoint` centre check never went red (the list ends about 4 px above the field's centre), so the scenarios assert box overlap with the value field / Choose image and the option's right edge against the key panel's right edge; that was red on 4 examples, then green. Proof: `pnpm cy:run --spec e2e_test/features/note_topology/note_property_layout.feature` 11/11, `note_property.feature` 10, `note_frontmatter_image.feature` 4, form tests 248, `vue-tsc` clean. Known, not exercised by a scenario: the existing row is a grid at all widths, so below 640 px its list is now in flow (it grows the key cell and covers nothing).

## Real iPad check (owner)

After slice 3, on a real iPad with the software keyboard, in both orientations: tap Add property, type `ur`, and
confirm the list and the value field are both visible; tap the value field. Report what still runs off the screen.
