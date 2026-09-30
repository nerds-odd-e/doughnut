# Reading Control Panel after a one-paragraph EPUB block

Work item: **SEED-059#story-18**
([story](../../seeds/SEED-059-book-reading-uat-fixes.md#story-18)).
Status: **story refined; one Behavior slice planned.**
Story 16's delivery is on trunk; nothing blocks execution.

## Goal and scope

A reader of an EPUB block with one paragraph gets the Reading Control Panel,
placed after that paragraph, as they do for blocks with more text.

Excluded (see the story): placing the panel at the real end of a block's text
when its last paragraph has no id (a separate, unqueued story); any PDF change;
any backend or generated API change.

## Known architecture

- **One function decides.** `lastDirectContentLocator`
  (`frontend/src/lib/book-reading/bookBlockDirectContent.ts`) is the only source
  of "the block's last text" for both consumers: `useReadingPanelTarget`
  (`updateLastDirectContentGeometry`, which sets `lastContentBottomVisible` and
  `geometryEverVisibleForSelection`) and `useReadingPanelAnchor`. Change it to
  agree with `hasNoTextOfItsOwn` in the same file: `null` when the block has no
  text of its own, otherwise its last locator (`null` for a block with no
  locator at all). No consumer changes.
- **Why a one-paragraph EPUB block has one locator.** In
  `EpubSpineContent`, a table-of-contents entry with content takes its start
  from its first payload (`payloads.getFirst().put("fragment", …)`); only an
  entry with no content gets a `beginning_anchor` payload. Headings are not
  payloads. So heading + one paragraph gives one locator, of type `text`.
- **Where the panel lands.** `useEpubLocatorGeometry.resolveLocatorRect` uses
  the element the locator's fragment names, else the whole `<body>`. Accepted
  for now: for a block whose last paragraph has no id, that is the heading
  (when the table of contents names it) or the end of the spine file.
- **PDF unchanged.** A PDF block's first locator is its heading, so length 1
  stays `null` (`hasNoTextOfItsOwn` true) and longer blocks keep their last
  locator.

## Premises observed

- **Story 16 is on trunk** (merged at `1fbd78d609`; observed at `origin/main`
  when this plan was landed). `useReadingPanelTarget` uses `hasNoTextOfItsOwn`, so
  a one-paragraph EPUB block counts as having text and (selected, with a
  successor) is offered only when `geometryEverVisibleForSelection` is true;
  `updateLastDirectContentGeometry` returns early for a `null` locator, so it
  never is. Observed by reading both files. Before that delivery the
  panel appeared when the next block became current.
- `epub_valid_minimal.epub`: `nav.xhtml` lists `chapter1.xhtml` (no fragment)
  for "Part One"; `chapter1.xhtml` holds `<h1>` and one `<p>` without an id.
  From `EpubSpineContent` this yields one locator `{href: chapter1.xhtml,
  fragment: null}` of type `text`, so `hasNoTextOfItsOwn` is false and its
  element is the body, which ends at the paragraph.
- `lastDirectContentLocator` has three callers, all in `frontend/src`
  (`useReadingPanelTarget`, `useReadingPanelAnchor`, its own file); none in
  `frontend/tests`, `e2e_test` or `cli`. Searched by name.
- `e2e_test/features/book_reading/epub_book.feature` names "Part One" only in
  the layout table and the opening-text check, and `phone_reading.feature`
  only in the opening-text check; no scenario expects the panel to be absent
  for it. The existing step "the EPUB Reading Control Panel should be
  content-anchored" checks the panel is visible with
  `data-panel-placement="anchored"`.
- Real books (Project Gutenberg *Alice's Adventures in Wonderland*, *On the
  Origin of Species*, checked 2026-09-30 by reading their tables of contents and
  spine files): no one-paragraph blocks, no paragraph ids, mostly one block per
  file. The fix has no effect on them; the fixture is the proof.

## Outside-in proof

- E2E, `epub_book.feature`: choose "Part One" → the text is shown and the
  Reading Control Panel is shown anchored (existing steps "I choose the book
  block", "I should see the text … in the EPUB reader", and "the EPUB Reading
  Control Panel should be content-anchored"). Expected to fail before the
  change (panel never shown, by the reading above) and to pass after.
- Preserved by existing proof: the "Chapter Alpha" anchored scenario, the mark
  scenarios, and the PDF panel specs in
  `frontend/tests/pages/BookReadingPage.readingPanelTarget.spec.ts` and
  `BookReadingPage.readingControlPanel.*.spec.ts`. Run `epub_book.feature` and
  `phone_reading.feature` (the changed panel can now show on the opening block)
  and those specs.

## Ordered slices

### 1. A reader gets the panel after a one-paragraph EPUB block
Type: Behavior
Status: planned
Proof: new scenario "EPUB reading control panel is content-anchored after a
one-paragraph block" in `epub_book.feature`, written first and seen failing.
Focused check: `epub_book.feature` and `phone_reading.feature` through the
project's E2E runner, plus the `BookReadingPage.*` specs named above.

Behavior: an EPUB is open and "Part One" (a heading and one paragraph) is
chosen → its paragraph is shown and the Reading Control Panel is offered
beneath it. A block with several paragraphs, and a block with no text of its own,
behave as before.

Change `lastDirectContentLocator` as described under Known architecture and
update its comment. About 5 min. Stated reason it is one slice: one function
serves both the target and the anchor, and the scenario is the only observable.

## Current decisions

- Accept the imprecise position for a last paragraph without an id (owner
  decision, 2026-09-30); record it as a separate finding, not extra scope.
- No unit test of the helper on its own: the E2E scenario reaches it through the
  panel, and the page specs cover the PDF path.

## Learnings

None yet.
