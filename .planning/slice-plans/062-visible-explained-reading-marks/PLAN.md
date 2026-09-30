# Make every reading mark visible and explained

Work item: **SEED-059#story-12**
([story](../../seeds/SEED-059-book-reading-uat-fixes.md#story-12)).
Status: **story refined; two Behavior slices planned.**
Nothing blocks execution.

## Goal and scope

A reader of a book, in PDF or EPUB and in either theme, can see every reading
mark in the book layout and tell what it means: a marked row's mark is visible
against the layout background, and hovering the row says Read, Skimmed, or
Skipped. An unmarked row shows neither.

Excluded (see the story; no follow-up story is queued): a chapter roll-up, an
end-of-book summary, a separate legend, a hover-free way to read a mark on touch
devices, a non-colour glyph; any backend, generated API, CLI, or E2E change.

## Known architecture

- **One place draws marks.** `BookReadingBookLayout.vue` sets
  `data-direct-content-read|skimmed|skipped` on each row from
  `dispositionForBlock(block.id)` and colours the row's right border in its
  scoped style: `success`, `warning`, `neutral`. PDF and EPUB share this layout.
- **PFE result: change in place.** No new mechanism. The row already knows its
  disposition (it renders the screen-reader "Marked as …" text from it), so the
  `title` comes from that same value with the labels the mark control already
  uses, and the Skipped colour becomes a
  theme-aware daisyUI token (`base-content` at partial opacity) instead of
  `neutral`.
- **No ADR or North Star topic applies.** Nothing there governs the reading
  surface.
- **Shared files with story 10.** Story 10 (`SEED-059#story-10`, in Taken) edits
  `BookReadingBookLayout.vue` and `BookReadingBookLayout.spec.ts` too. Different
  regions (button and row handlers vs. row title and mark colour); expect a
  routine merge, and rebase onto whichever lands first.

## Premises observed

- **Skipped is dark-on-dark in the dark theme.** A throwaway spec (deleted, not
  committed), run with
  `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vitest run <spec>`, mounted the layout
  with `dispositionForBlock: () => "SKIPPED"` and read computed styles in both
  themes (`document.documentElement.data-theme`): the right border was 4px
  `oklch(0.14 0.005 285.823)` in light and in dark, while the layout aside's
  background was `oklch(0.98 0 0)` (light) and `oklch(0.2326 0.014 253.1)`
  (dark). Lightness 0.14 on 0.23 is the defect; on 0.98 it is fine.
- **Layout specs run in a real browser with real styles**, so computed border
  and background colours are observable at the layout's own spec. Same evidence.
  Existing precedent: `frontend/tests/pages/HomePage.theme.spec.ts` switches
  themes with `data-theme` and compares computed colours.
- **Read and Skimmed are not the defect.** Same throwaway spec: light-theme
  right borders are `oklch(0.76 0.177 163.223)` (Read, `success`) and
  `oklch(0.82 0.189 84.429)` (Skimmed, `warning`) on the 0.98 background, and
  the dark aside background is 0.2326, so their colours are lighter than the
  dark background. Their light-theme gap (about 0.2) is why the visibility
  threshold below applies to Skipped only.
- **No test asserts the mark colours or a row title today.**
  `grep -rn "border-r-\|toHaveAttribute(\"title\"" frontend/tests
  e2e_test` finds nothing about book-layout marks. E2E steps only read the
  `data-direct-content-*` attributes (`bookReadingProgressMethods.ts`), so they
  are untouched.
- **Only the shell mounts the layout** (story 10's search:
  `BookReadingShell.vue` and the layout spec), so the change reaches PDF and EPUB
  alike.
- **`mountLayout` in the layout spec passes `dispositionForBlock: () =>
  undefined`.** New cases need a per-case disposition; add an optional
  `disposition` to `mountLayout`'s options, the only change to existing test
  setup.
- **Not run:** no E2E and no full frontend suite was run to plan this.

## Outside-in proof

The mark is a presentation of a disposition the layout receives, so its boundary
is the layout's own spec (real browser). No E2E is added: no scenario can fail
for a reason the layout spec would miss, and the existing scenarios keep proving
marks appear and persist.

- Layout spec, hover text: rows for Read, Skimmed and Skipped have `title`
  "Read", "Skimmed", "Skipped"; a row with no disposition has no `title`.
  Expected to fail before slice 1.
- Layout spec, visibility: for a Skipped row, in the light and in the dark
  theme, the right-border colour has a lightness clearly apart from the aside's
  background (gap at least 0.3 in OKLCH lightness; today's dark gap is about
  0.09). Expected to fail before slice 2 in dark. Read and Skimmed are not
  asserted: their colours do not change.
- Preserved: every existing case in `BookReadingBookLayout.spec.ts`, and the
  book-reading E2E features that read `data-direct-content-*`.

## Ordered slices

### 1. Hovering a marked row says which mark it has
Type: Behavior
Status: planned
Proof: layout spec case above, written first and seen failing. Focused check:
`BookReadingBookLayout.spec.ts`.

Behavior: a book block has a Read, Skimmed or Skipped record → the pointer
rests on its layout row → the row's hover text says "Read", "Skimmed" or
"Skipped". A row with no record has no hover text. Selection, the mark control
and the screen-reader text are unchanged.

Bind `:title` on the row from `dispositionForBlock(block.id)` (undefined when
unmarked so no attribute is drawn). The words "Read", "Skimmed", "Skipped"
already exist as `markOptions` labels in `BookBlockMarkControl.vue`; move that
mapping to `lib/book-reading/readBlockIdsFromRecords.ts` beside the
disposition type and use it from both, so there is one source for the labels.
Add `disposition` to the spec's `mountLayout`
options. About 5 min.

### 2. A Skipped mark stays visible in the dark theme
Type: Behavior
Status: planned
Proof: layout spec case above, written first and seen failing in dark. Focused check: `BookReadingBookLayout.spec.ts`.

Behavior: a block is marked Skipped → its layout row in the dark theme →
the mark is clearly visible against the layout background; in the light theme
it stays visible, and Read and Skimmed are unchanged.

Change the Skipped border colour from `neutral` to `base-content` at partial
opacity (a grey that follows the theme), keeping it distinct from the success
and warning colours. Choose the opacity so the lightness gap holds in both
themes. About 5 min. Stated reason it is separate from slice 1: different
observable outcome and different proof (computed colours vs. hover text), and
each is independently useful.

## Current decisions

- Narrow to visibility and hover text only (owner decision, 2026-09-30: current
  focus is bug fixing and technical debt; no new features).
- Native `title` for hover, no phone alternative and no glyph (deferred in the
  story).
- Prove colour at the layout spec with computed styles, not an E2E emulating a
  dark colour scheme; the layout spec already runs in a real browser.

## Learnings

None yet.
