# Keep every note property value visible and easy to tap on an iPad and a phone

**Identity:** SEED-064#story-1
**Source:** [story](../../seeds/SEED-064-note-properties-fixes.md#story-1), refined 2026-09-30 with the owner's
decision that the density guard is a reported number, not a gate.

## Goal and scope

An iPad or phone user reads every value of a note's properties, in the read-only and the editable view, even with a
long key or value, and taps row controls of at least 44 px without hitting a neighbour.

Included: the value-visible fix in `RichFrontmatterReadOnlyList.vue` and `RichFrontmatterEditablePropertyRow.vue`
(with `RichFrontmatterScalarPropertyValue.vue` if the value field needs it); one touch-size rule for the properties
section keyed on `pointer: coarse`; a reported density number.

Excluded (story decisions): key and value hierarchy (iPad I3), dialog close buttons (iPad D5), add form and preset
list (story 2), "show all" collapse, tap-to-expand cue, width-based touch rule, a hard density gate, merging the
read-only list into the editable row (story 5). No stored-Markdown behavior changes, so ADR 0004 is unaffected; no
Accepted ADR or North Star topic governs component layout, so this is an ordinary plan with no new direction.

Assumption: the read-only wrapping fix is throwaway once story 5 deletes `RichFrontmatterReadOnlyList.vue`. It is kept
because it is a one-class change and its scenario carries over.

## Existing solutions (PFE)

Nothing new is built. Reused: the daisyUI `daisy-*-sm` sizes and Tailwind 4.3.3 utilities (`pointer-coarse:` variant
exists in the installed `tailwindcss`); `note_property.feature`'s Bazaar-subscription steps for a read-only view; the
existing `I have a note ... with content:` step. Gap: no generic viewport step (only
`I set the book reading viewport to {int} by {int}`) and no touch-device step; both are added as test support.

## Decisive premises

| Premise | Observation | Result |
| --- | --- | --- |
| Properties components have not changed since the UAT baseline commit | `git diff --stat 1986473b79 HEAD -- frontend/src/components/form/RichFrontmatter* frontend/src/components/form/PropertyValueField.vue frontend/src/components/notes` | empty, so the UAT figures are a valid-revision reference, but they were taken with Playwright device mode, so the density comparison is re-measured in Cypress (slice 1) |
| No generic viewport or touch step exists in e2e | `grep -rn "viewport\|coarse\|setTouchEmulation\|hasTouch" e2e_test` | only the book-reading viewport step and `cy.viewport` in `bookReadingLayoutMethods.ts`; no touch emulation |
| A read-only properties view is reachable in e2e | read `note_property.feature` scenario "Visiting a read-only property location focuses the property value" | reached by another user's notebook shared to the Bazaar and subscribed |
| Tailwind has a `pointer-coarse:` variant | `grep -o "pointer-coarse" frontend/node_modules/tailwindcss/dist/lib.js` | present (4.3.3) |
| Chromium makes `(pointer: coarse)` match under touch emulation | one Playwright Chromium run: default context, `hasTouch`, `hasTouch`+`isMobile`, and CDP `Emulation.setEmitTouchEventsForMouse` alone | true with `hasTouch`; false in the default context and false with only `setEmitTouchEventsForMouse` |
| The same holds inside Cypress | not observed | probe: slice 3 |
| The editable value field (contenteditable `daisy-input`) can wrap and grow | not observed | settled by slice 2's failing scenario; adjust slice 2 if it cannot |
| The editable row breaks at 820 px, not only at 375 px | not observed | settled by slice 2's scenario (the expectation is asserted at both widths) |

## Outside-in proof

| Promise | Owner | Observable proof |
| --- | --- | --- |
| Read-only value visible with a long key at 820 and 375 px, no sideways scroll | slice 1 | Cypress scenario `note_property_layout.feature`: value element is visible with width above 100 px and `document.documentElement.scrollWidth <= clientWidth` |
| Editable value visible, value field shows all text, cut key has an ellipsis, controls do not overlap | slice 2 | same feature: value field height shows all wrapped text; key input computed `text-overflow` is `ellipsis`; row controls' boxes do not intersect |
| Touch device is available to e2e | slice 3 | step asserts `matchMedia('(pointer: coarse)').matches` is true after it runs |
| Controls at least 44 px on touch, unchanged on desktop | slice 4 | scenario: each of chevron, edit, remove, external link, key and value field has `height >= 44` on a touch device and `< 44` without |
| Density reported before and after | slices 1 and 4 | numbers written under Learnings, not asserted |
| Real iPad check | owner, after slice 4 | owner's manual observation; not a slice |

jsdom has no layout, so no component test proves any promise here; existing component tests
(`RichMarkdownEditor.frontmatter.spec.ts`) must stay green and are run once after slice 2 and slice 4.

## Ordered slices

Size target is about 10 minutes each including proof. Slices that boot the Cypress stack take longer only by the
external wait for the first stack start.

### 1. Read-only properties keep a long-key note's value visible on an iPad and a phone
Type: Behavior
Status: done
Proof: `pnpm cy:run --spec e2e_test/features/note_topology/note_property_layout.feature` runs the read-only scenario
at 820 and 375 px, red before the change and green after.

Behavior: another user's shared notebook holds a note with a 76-character key without spaces (value `short`) and a
76-character key with spaces (200-character value), subscribed by the learner → view the note at 820 px, then at
375 px → both values are visible, the key wraps inside its column, and the page has no sideways scroll.

Steps, in order:
1. Baseline, before any product change: a 19-row note (properties like the UAT: 14-item aliases, `wikidata_id`, long
   key, wiki-link values) at 820 px, read-only and editable; run a throwaway Cypress measurement (not committed) that
   logs the top of the note body; write both numbers under Learnings.
2. Add the generic `I set the viewport to {int} by {int}` step and the scenario; watch it fail.
3. Bound the read-only key column (`minmax(6rem,40%)`) and add `break-words` to key and value in
   `RichFrontmatterReadOnlyList.vue`; remove `truncate` from the URL and Wikidata value spans there so text wraps.
4. Green; run the wider `note_property.feature` once (the read-only scenario there uses this component).

### 2. Editable properties keep a long-key note's value visible and cut keys are cued
Type: Behavior
Status: done
Proof: the same feature file, editable scenario at 820 and 375 px, red then green; then `pnpm frontend:test` for
`RichMarkdownEditor.frontmatter.spec.ts`.

Behavior: the owner's own note with the same long-key rows → open it editable at 820 px and 375 px → the value field
shows the whole 200-character text wrapped, the key field ends in an ellipsis, the chevron, edit and remove controls
do not overlap, and the page has no sideways scroll.

Change: at narrow widths the value drops under the key (one wrapping rule in `RichFrontmatterEditablePropertyRow.vue`
replacing the fixed three-column grid); the value field grows in height (`h-auto`, wrapping); the key input gets
`text-ellipsis`. If slice 1 or the first run shows the row already fine at 820 px, assert only what fails.

### 3. A touch device can be selected in a scenario
Type: Structure
Status: done
Proof: a scenario step `I use a touch device` passes its own assertion that `matchMedia('(pointer: coarse)')`
matches; existing scenarios remain unaffected because the step is opt-in.

Internal change: test support only (Cypress-side CDP `Emulation.setTouchEmulationEnabled`, run before the app loads
or with a reload). Enables slice 4's touch-size proof.

Probe: this settles the unobserved premise. If Cypress cannot make `(pointer: coarse)` match, stop slices 3 and 4, and
report to the owner that the touch-size rule can only be proved on a real iPad; the owner decides between shipping the
rule with that proof or dropping the increment. Slices 1 and 2 stand alone.

### 4. Touch devices get controls of at least 44 px in the properties section
Type: Behavior
Status: planned
Proof: the touch-size scenario is red then green; `pnpm frontend:test` for the properties component tests stays green;
`pnpm cy:run --spec e2e_test/features/note_topology/note_property_layout.feature` passes; the density numbers are
measured again with the throwaway step from slice 1 and recorded.

Behavior: a note with properties opened on a touch device → chevron, edit, remove, external-link, key and value field
are each at least 44 px high; on a non-touch device the same controls stay under 44 px.

Change: one `pointer-coarse:` rule on the properties section (`RichFrontmatterProperties.vue`) targeting
`.daisy-btn-sm` and `.daisy-input-sm` within it, and the value field's fixed height, not per-component class edits. The
row panel's Assimilate and Skip are inside the section and so are included; no other screen changes.

## Current decisions

- One Cypress feature file, `e2e_test/features/note_topology/note_property_layout.feature`, capability-named; no
  planning numbers in it.
- Touch rule is `pointer: coarse`, not width, so an iPad in landscape is included (owner: iPad is the primary device).
- Density is measured with the same tool before and after and reported, never asserted.

## Learnings

- Density baseline (Cypress, 820x1000, 19-row note, top of `.ql-editor` in viewport px): editable 864, read-only 608.
  Slice 4 measures again the same way.
- Slice 1 reused the existing step `I am on a window {int} * {int}` (sidebar.ts) instead of adding a viewport step;
  only slice 3's touch step is still new. The feature file must be listed in
  `scripts/isolated-cypress-active-specs.mjs`; slice 1 added it.
- The `minmax(6rem,40%)` key column makes short read-only keys sit in a wider column than before; not asserted, for the
  owner's iPad check.
- Slice 2: at 820 and 375 px the editable row's controls and page width already held, so the planned grid change
  ("value drops under the key") was not needed; only `h-auto` on the value field and `text-ellipsis` on the key input
  were. The controls check covers the panel-open state and the spaces-key row only.
- Slice 3 probe passed: `Cypress.automation('remote:debugger:protocol', {command: 'Emulation.setTouchEmulationEnabled',
  ...})` makes `(pointer: coarse)` match in Cypress with no reload; without the step it is false. Support lives in
  `e2e_test/start/touchDevice.ts` (`use`, `reset`, `expectCoarsePointer`); an `After` hook resets it. Slice 4 reuses
  `I use a touch device` and its "unchanged without touch" check also proves the reset.
- The two long-key rows live in the feature's Background (another user's shared notebook); slice 2's editable scenario
  can reuse the same rows with the owner logged in.

## Real iPad check (owner)

After slice 4, on a real iPad, in both orientations: open the long-key note read-only and editable; tap chevron, edit
and remove; report the density number for a 19-row note.
