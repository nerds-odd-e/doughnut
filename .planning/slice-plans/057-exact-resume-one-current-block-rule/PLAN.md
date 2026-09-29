# Resume exactly and track the current block and records the same way in EPUB and PDF

Work item: **SEED-059#story-15**.
Source: [story](../../seeds/SEED-059-book-reading-uat-fixes.md#story-15)
(owner decisions 2026-09-29 from the SEED-059#story-1 retrospective; the
marking-after-an-empty-block part moved from story 6). Owner instruction for
this plan: write it to judge the story's size; do not execute.

## Goal and scope

A reader reopens a book exactly where they stopped, sees the current block move
the same way in EPUB and PDF, and gets a record only for blocks they read or
chose to mark.

Four independent parts:

- **A. Auto-mark only blocks with no text of their own** (EPUB defect).
- **B. One current-block rule for EPUB and PDF**, including landing a chosen PDF
  block at the top and the chosen block winning a shared start.
- **C. Marking goes on after choosing a block with no text of its own** (UAT
  defect 15, PDF without bookmarks).
- **D. Exact EPUB resume** from epub.js's CFI.

Excluded: snap-back and the panel's wheel handling (story 5), changing or
clearing marks (story 6), the EPUB resume-test cleanup and rendered-view lookup
(story 14), reading progress display (story 12).

## Architecture

- **PFE:**
  - "Has text of its own": today three places use locator count
    (`useAutoMarkNoDirectContentPredecessor` `length === 1`,
    `bookBlockDirectContent.lastDirectContentLocator` `length <= 1`,
    `useBookReadingSnapBack.hasDirectContent` `length > 1`). Backend
    `BookBlockEpub/PdfContentLocators` always make the first content block
    locator 0, so the first payload's own content is never counted. PDF is
    right by accident (its first block is always a heading or a
    `beginning_anchor`); EPUB is wrong for a heading plus one paragraph. The
    frontend already receives `BookBlockFull.contentBlocks[].type`, unused
    today. Add one frontend predicate ("only a start": one locator and its
    content block is a `beginning_anchor` or a PDF heading) and use it in
    auto-mark and `lastDirectContentLocator`; snap-back goes with story 5.
  - Current block: evolve `currentBlockIdFromEpubView` into one format-neutral
    rule over "block start offset from the view top in px"; PDF supplies starts
    from page-div position plus `bbox[1]`, close to
    `usePdfLocatorGeometry.resolveLocatorRect`. Delete
    `currentBlockIdFromVisiblePage` and its 19 cases. Keep
    `pdfViewerViewportTopYDown` / `pdfViewerReadingPositionTopEdge` for the page
    bar and reading position.
  - PDF landing: `usePdfNavigation.applyNavigationTarget` uses
    `normalizedBboxToPdfJsXyzDestArray` with `SCROLL_TOP_PADDING_PDF = 40`
    points, leaving the start about 53–75 CSS px low, beyond the 24 px "at the
    top" tolerance. Land at the start, and re-derive the current block from the
    view after landing as EPUB does (`commitNow(currentBlockIdInView())`)
    instead of committing the chosen id.
  - Resume: `EpubLocator` gains an optional `cfi` (JSON column, no migration;
    `@JsonInclude(NON_NULL)` like `PdfLocator`); `BookFormat.EPUB` must pass it
    through (it rebuilds the record today); `debounceLastReadPositionPatch`
    compares it. The viewer emits `rendition.location.start.cfi`; the initial
    display tries the CFI and falls back to href#fragment.
- No ADR or North Star topic is affected.

## Decisive premises

| Premise | Observation | Result |
| --- | --- | --- |
| EPUB auto-marks a one-paragraph block | `useAutoMarkNoDirectContentPredecessor.ts:21-35`; `EpubSpineContent.java:86-126` (only `p`/`img`/`table` are payloads; first payload becomes the start) | Confirmed. `epub_book.feature:56-60` pins it: "Part One" has a paragraph |
| Frontend can tell "only a start" | `types.gen.ts:589-604` `contentBlocks{id,type}`; no frontend reader | Confirmed; no API change |
| PDF one-locator already means heading-only | `MineruContentListLayoutBuilder.java:51-64,120-131,154-161` | Confirmed |
| Shared rule can take px starts for PDF | `currentBlockIdFromEpubView.ts:12-60`; `usePdfLocatorGeometry.ts:13-43`; pdf.js keeps sized divs for all pages | Confirmed |
| PDF lands 40 pt below the start | `pdfOutlineV1Anchor.ts:7,104-117` | Confirmed; conflicts with the shared rule |
| Scenarios that change under the shared rule | `book_browsing.feature:52-66` (bookmark block 95/1000 below after "top of page 5") breaks; `reading_record.feature:25-29` needs landing fixed; `book_browsing.feature:37-43` margin needs recomputing | Observed by reading steps (`bookReadingPdfMethods.ts:41-111`), not run |
| Page specs rely on the midpoint rule | `BookReadingPage.readingControlPanel.marking.spec.ts`, `.readingPosition.spec.ts`, `.notebookSwitch.spec.ts`, `bookReadingPageInteractionTestSupport.ts` emit normalized viewports | Confirmed; they need a stubbed block-start source |
| No PDF fixture has two headings sharing a start | `mineru_output_for_*.json`, `refactoring.pdf` bookmarks | Confirmed; a fixture is needed |
| Defect 15 shape exists in a fixture | — | **Unverified**: no fixture with a "Chapter N" label block directly before a title block with introduction text was checked |
| epub.js gives and accepts a CFI | `epubjs/src/rendition.js:305-322,689-800`; `landOneDisplayAtATime` passes strings through | Confirmed in source |
| The CFI is precise in `flow: "scrolled"`, `manager: "continuous"` | — | **Unverified**; slice D1 probes it |
| Story 5 changes the same PDF pipeline | `053-pdf-smooth-scroll-after-choosing-block/PLAN.md` slice 1 removes the snap-back veto and the hold-window gate in `PdfBookViewer.vue:141` | Confirmed; B follows story 5 |
| Story 14 deletes the resume scroll steps D needs | `055-epub-resume-tests-and-rendered-view/PLAN.md` slice 1 | Confirmed; D follows story 14 |

## Key examples → proof

| Promise | Slice | Proof |
| --- | --- | --- |
| Leaving a one-paragraph EPUB block unread does not mark it; a heading-only block still is | A1 | E2E `epub_book.feature` (rewritten auto-mark scenario, plus a heading-only entry); `useBookReadingSelection.spec.ts` |
| A chosen PDF block's start lands at the top and it is current | B2 | New E2E in `book_browsing.feature` |
| Scrolling a PDF past a heading makes it current when it reaches the top, as in EPUB | B3 | `book_browsing.feature` scroll scenarios; shared rule spec |
| Choosing the first of two PDF headings sharing a start keeps it selected and current | B4 | New E2E on a bookmark fixture with a shared start |
| "Chapter 12" → scroll → "Tuples" current before 12.1, "Chapter 12" marked, panel offered for "Tuples" | C1 | New E2E in `reading_record.feature` on a no-bookmark fixture |
| Reopening an EPUB at another width shows the same paragraph at the top | D3 | New E2E in `epub_book.feature` |

Proof commands:
`SUT_TIMEOUT_MS=360000 CURSOR_DEV=true nix develop -c pnpm cy:run --spec <feature>`;
`CURSOR_DEV=true nix develop -c pnpm -C frontend test <specs>`;
`CURSOR_DEV=true nix develop -c pnpm backend:test_only`;
`CURSOR_DEV=true nix develop -c pnpm generateTypeScript` then `pnpm openapi:lint`.

## Slices

### A1. A block with text of its own is not auto-marked
Type: Behavior
Status: planned
Size: about 10 min (fixture entry, predicate, two callers, specs, E2E).
Proof: E2E `epub_book.feature` auto-mark scenarios; `useBookReadingSelection.spec.ts`.

Add a heading-only entry to `epub_valid_minimal.epub` (or the long-chapter
fixture) and rewrite "Entering the next EPUB block auto-marks a structural-only
predecessor as read" so it uses that entry, plus one scenario where "Part One"
(one paragraph) stays unmarked. Add the "only a start" predicate and use it in
auto-mark and `lastDirectContentLocator`. PDF `reading_record.feature:15-17`
stays green.

### B1. One current-block rule, format-neutral
Type: Structure
Status: planned
Size: about 5 min.
Proof: the rule's spec, renamed; EPUB E2E unchanged.

Move `currentBlockIdFromEpubView` to a neutral module taking a block-start
function; EPUB passes its locator geometry. No behavior change.

### B2. A chosen PDF block lands at the top and is current
Type: Behavior
Status: planned
Size: about 10 min. Depends on story 5 being delivered.
Proof: new E2E "Choosing a PDF block shows its start at the top" in
`book_browsing.feature`; `reading_record.feature` green.

Land with no top padding; after landing, derive the current block from the
view through the shared rule (PDF viewer exposes `viewBlockStarts()`).

### B3. PDF follows scrolling with the shared rule
Type: Behavior
Status: planned
Size: about 15 min (**over target**: page-spec stubs in four files and the
bookmark scenario's scroll step move together with the rule; splitting leaves
PDF on two rules).
Proof: `book_browsing.feature` and `reading_record.feature`; page specs.

Feed PDF viewport changes through `viewBlockStarts()`; delete
`currentBlockIdFromVisiblePage` and its spec; move the page specs to a stubbed
block-start source; make the bookmark scenario scroll to the block's start and
recompute the same-page helper's deltas.

### B4. A chosen PDF block keeps a shared start
Type: Behavior
Status: planned
Size: about 10 min (fixture: parent and child bookmark at one destination).
Proof: new E2E in `book_browsing.feature`.

### C1. Marking goes on after choosing a block with no text of its own
Type: Behavior
Status: planned
Size: about 10 min if the rule changes suffice; unknown otherwise.
Proof: new E2E in `reading_record.feature`.

Needs a no-bookmark MinerU fixture shaped like Think Python's "Chapter 12" →
"Tuples" → "12.1". Expected to need no rule beyond A1 and B3; if it does,
record why.

### D1. Probe: is the CFI precise in continuous scrolled mode?
Type: Structure (probe)
Status: planned
Size: about 10 min.
Proof: a written observation: `location.start.cfi` after scrolling mid-chapter
at two widths, and where `display(cfi)` lands. **Stop rule:** if it is not
precise, replan D with the owner before D2.

### D2. The saved EPUB position keeps a CFI
Type: Behavior
Status: planned
Size: about 10 min (record field, `BookFormat` pass-through, controller tests,
client regeneration).
Proof: `NotebookBooksReadingPositionControllerTest` and
`NotebookBooksGetReadingPositionControllerTest`: a PATCH with a CFI returns it.

### D3. Reopening an EPUB resumes at the exact place
Type: Behavior
Status: planned
Size: about 10 min. Depends on story 14 being delivered.
Proof: new E2E: scroll to the middle of a long chapter, leave, reopen at
another width → the same paragraph is at the top.

The viewer emits `location.start.cfi`; the position sends it; the initial
display tries the CFI, then href#fragment. Update the debounce comparison.

## Current decisions

- Four parts are independent; A has no dependency; B and C follow story 5; D
  follows story 14.
- One predicate for "only a start"; no API change for it.
- The shared current-block rule is EPUB's; PDF lands at the start to meet it.

## Learnings

(none yet)
