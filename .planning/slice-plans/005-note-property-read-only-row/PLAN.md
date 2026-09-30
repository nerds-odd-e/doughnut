# Make a read-only wiki-link property value a link and render both views with one property row

**Identity:** SEED-064#story-5
**Source:** [story](../../seeds/SEED-064-note-properties-fixes.md#story-5), refined 2026-09-30 from the owner's
answers: image display excluded (new capability), one merged row wanted over reader-only fixes, a dead wiki link in
the read-only view does nothing. A Chromium measurement made during refinement found no ragged columns within a view,
so cross-view alignment is not a promise.

## Goal and scope

A reader of a shared or subscribed note can follow a property whose whole value is a wiki link, and the read-only view
becomes a mode of the editable row so the row layout lives in one place.

Included: a scalar value that is one whole wiki link is a link in the read-only view; the read-only view is rendered by
the row component in a read-only mode (key as text, the compact value display used today, `dl`/`dt`/`dd` kept); the
read-only list component is deleted; the row and list names stop saying "Editable"; the read-only view keeps no chevron,
panel, Assimilate, Skip, remove control, inputs or presets, and a property visit still highlights and scrolls to the row.

Excluded (story decisions): rendering an image for an `image` value; a dead wiki link doing anything in the read-only
view; linking text inside a longer value; cross-view pixel alignment or a blank chevron column; any unique-key rule
(SEED-063 boundary); changes to the editable view. No stored-Markdown behavior changes (ADR 0004 unaffected). No
Accepted ADR or North Star topic governs component layout, so this is an ordinary plan with no new direction.

## Existing solutions (PFE)

Nothing new is built. `WikiLinkToken.vue` already renders a whole wiki link as a router link and anything else as its
plain text; list values of `overlaps` use it today. The row, its value components and `useFocusedNoteProperty` already
exist; the read-only value display (relation label, Wikidata ID with link, URL with link, list, plain text) moves from
`RichFrontmatterReadOnlyList.vue` into a value component reused by the row rather than being rewritten.
Test reuse: `createRichMarkdownEditorTestHarness` (`mountEditor` with `readonly: true`), `propertiesTestDom.ts`
(`propertyRowSelector`), `mountEditorOnNoteShow`, `wikiLinkFromAuthoredToken` as used in
`RichMarkdownEditor.propertyWikiLinks.spec.ts`.

## Decisive premises

| Premise | Operation that consumes it | Observation | Result |
| --- | --- | --- | --- |
| A read-only whole wiki-link value shows as plain text today | slice 1's red run | throwaway Chromium spec, `related: "[[Other]]"`, `readonly: true` (`CURSOR_DEV=true nix develop -c pnpm -C frontend exec vitest run tests/components/form/tmpProbe.spec.ts`, deleted afterwards) | row text is `related[[Other]]`, no anchor |
| Rows are already aligned within each view, so no layout slice is needed | the story's "one row" claim | same probe, five rows at one width, offsets of `dt`/`dd`/inputs from the row's left edge | read-only: `dt` 0 and `dd` 182 on every row; editable: key input 58 and value 232 on every row |
| `WikiLinkToken` gives a link for a whole token and text otherwise | slice 1's fix | read `WikiLinkToken.vue`: `parseWholeWikiLinkItem(token.trim())`, router link when a note id resolves, `<a href="#">` with the unresolved class otherwise, the raw token when it is not a whole link | confirmed by reading; slice 1's spec observes it |
| The read-only list is used in one place | slice 4's deletion | `rg "ReadOnlyList" frontend` | only `RichFrontmatterProperties.vue` |
| The e2e read-only scenario reads `dd` under `[data-testid=rich-note-property-row][data-property-key]` | slice 4's proof | read `notePropertyLocationMethods.ts` `expectRichNotePropertyValueVisible` | `find('dd')` inside the row: the merged row must keep `dt`/`dd` and those attributes |
| Existing read-only specs already protect list `url` links, compact lists, the visit highlight and `dl` text | slice 2's boundary | `rg -n "readonly: true" frontend/tests/components/form`: `listProperties.spec.ts` (`dl` links, compact list), `propertyLocation.spec.ts` (highlight, scroll), `frontmatter.spec.ts` (`dl` text) | present; none covers Wikidata, scalar URL, relation label, plain text, or the absence of editing controls |
| The row file is edited by story 2 (Taken, branch `exec/seed-064-story-2`) | slice 4's start | `git worktree list`; story 2's plan extracts the shared key field from the row | slices 1 to 3 do not touch the row; slice 4 starts only after story 2 is on trunk |
| Component tests run in Chromium and `console.log` fails a run | all slices | the probe run above | confirmed |

## Outside-in proof

| Promise | Owner | Observable proof |
| --- | --- | --- |
| Read-only `related: "[[Other]]"` (note `Other` visible) is a link to that note; the editable view is unchanged | slice 1 | component test in a new `RichMarkdownEditor.readOnlyProperties.spec.ts`, red before and green after; existing `RichMarkdownEditor.propertyWikiLinks.spec.ts` green |
| Read-only shows plain text, Wikidata ID with link, URL with link, relation label as today, and no toggle, remove, key or value input, panel, Assimilate or Skip | slice 2 | same spec, green before and after the merge |
| A property visit highlights and scrolls the read-only row | slice 4 (kept) | existing `RichMarkdownEditor.propertyLocation.spec.ts` unchanged |
| Long-key values stay visible and nothing scrolls sideways at 820 and 375 px | slice 4 (kept) | `e2e_test/features/note_topology/note_property_layout.feature` read-only scenario, run once after slice 4 |
| Read-only list and row stay `dl`/`dt`/`dd` | slice 4 | `listProperties.spec.ts`, `frontmatter.spec.ts` and the e2e scenario above, unchanged |
| The editable view is unchanged | slices 3 to 5 | `RichMarkdownEditor.propertyEntry.spec.ts`, `propertyRowEditing.spec.ts`, `propertyValueDialog.spec.ts` green with no assertion changed |
| Real iPad check | owner, optional, after slice 1 | open a shared note with a wiki-link property and tap it |

## Ordered slices

Size target is about 10 minutes including proof.

### 1. A read-only whole wiki-link value is a link
Type: Behavior
Status: done
Proof: the new spec, red then green (`CURSOR_DEV=true nix develop -c pnpm -C frontend exec vitest run
tests/components/form/RichMarkdownEditor.readOnlyProperties.spec.ts tests/components/form/RichMarkdownEditor.propertyWikiLinks.spec.ts`).

Behavior: read-only note with `related: "[[Other]]"` and a wiki link to `Other` → the value is a link to that note;
`topic: training` stays plain text. Change: in the scalar branch of `RichFrontmatterReadOnlyList.vue`, render the value
through `WikiLinkToken` (passing `wikiLinks` and `lastSavedMarkdown`, with no dead-link handler). If the first run does
not fail on the missing link, stop and re-read the flow.

### 2. Read-only rows keep their values and show no editing controls
Type: Behavior
Status: done
Proof: extend the same spec; it is green on today's code and stays green through slices 3 to 5.

Behavior (already true, now pinned): read-only `wikidata_id: Q42` → `Q42` and its external link; scalar `url` → text and
link; a relation key → its label; a plain key → its text; the list contains no panel toggle, remove control, key or
value input, preset list, Assimilate or Skip. No product change; if an assertion fails on today's code the premise is
wrong, so report before changing the assertion.

### 3. The read-only value display is its own component
Type: Structure
Status: planned
Proof: slice 2's spec and `listProperties.spec.ts`, `frontmatter.spec.ts`, `propertyLocation.spec.ts` green, no assertion changed.

Move the relation / Wikidata / URL / list / scalar branches out of `RichFrontmatterReadOnlyList.vue` into
`RichFrontmatterReadOnlyPropertyValue.vue`, which the list renders inside each `dd`. Behavior is unchanged.

### 4. The row has a read-only mode and the read-only list is deleted
Type: Structure
Status: planned
Start: only after SEED-064#story-2 is on trunk (`git fetch origin`; the shared key field is in the row); otherwise stop
after slice 3 and report.
Proof: slice 2's spec, `listProperties.spec.ts`, `frontmatter.spec.ts`, `propertyLocation.spec.ts` and the editable specs
of the proof table green with no assertion changed; then `CURSOR_DEV=true nix develop -c pnpm cy:run --spec
e2e_test/features/note_topology/note_property_layout.feature` once.

Give `RichFrontmatterEditablePropertyRow.vue` a `readOnly` prop: in that mode the row renders the key as `dt`, the
value through the slice 3 component as `dd`, and no chevron, input, presets or panel; keep `data-testid`,
`data-property-key`, the focus highlight and the row ref. The list component renders every row and wraps them in a `dl`
in read-only mode; `RichFrontmatterProperties.vue` drops its read-only branch; delete `RichFrontmatterReadOnlyList.vue`.
If this runs past about 10 minutes, split at "row read-only mode exercised by a component test" and "switch the list and
delete".

### 5. The row and list names no longer say "Editable"
Type: Structure
Status: planned
Proof: `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vitest run tests/components/form` green, and the frontend
type check and lint of the changed files as the project's wrap-up requires.

Rename `RichFrontmatterEditablePropertyRow.vue` and `RichFrontmatterEditablePropertyList.vue` to names without
"Editable" (for example `RichFrontmatterPropertyRow.vue` and `RichFrontmatterPropertyList.vue`), update imports, and
change no behavior. Story 6 and SEED-062 add to the renamed row.

## Current decisions

- The link uses `WikiLinkToken` as list values already do; no new wiki-link rendering is built.
- Read-only keeps `dl`/`dt`/`dd`, because the e2e page object and three specs read them.
- A dead wiki link in the read-only view renders as an unresolved-styled anchor and does nothing when tapped, as an
  `overlaps` list value does today.
- Slice 4 waits for story 2; slices 1 to 3 do not touch the row.
- Tests do not call `console.log`.

## Learnings

- Slices 1 and 2 were delivered in one commit because they share one spec. Slice 1's red run failed on the missing
  `dd a`; the slice 2 assertions were green on today's code, so the premise held.
- `WikiLinkToken` lives in `@/components/notes/`. Existing read-only specs are named `RichMarkdownEditor.listProperties.spec.ts`,
  `RichMarkdownEditor.frontmatter.spec.ts` and `RichMarkdownEditor.propertyLocation.spec.ts`.
- The read-only external-link control is a `button` with `data-testid=rich-note-property-external-link`, not an anchor;
  `RichMarkdownEditor.readOnlyProperties.spec.ts` must stay green through slice 4.

## Real iPad check (owner, optional)

After slice 1, on a shared note with `related: "[[Some Note]]"`, open it read-only and tap the value.
