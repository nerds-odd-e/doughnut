# Reopen an EPUB at the exact paragraph

Work item: **SEED-059#story-17**.
Source: [story 17](../../seeds/SEED-059-book-reading-uat-fixes.md#story-17)
(refined 2026-09-29; originally resplit from story 15's plan at `8e5d358124`,
part D).

## Goal and scope

A reader who stops part-way through a long EPUB block reopens the book at the
same paragraph, at any window width.

- The last-read EPUB position gains an optional exact place (epub.js CFI).
  It is saved while the reader scrolls within a block too.
- Reopening lands on that paragraph. The promise is paragraph precision, not
  the same line or pixel. The current block follows the existing rule.
- Choosing a block still lands at its start.
- A position without an exact place (saved before this story) reopens at its
  block start. An exact place that no longer resolves falls back through the
  existing display chain. This is a boundary assumption, with no dedicated proof.
- **Excluded:** PDF resume; a "resume here" prompt; position history;
  cross-device merging.

## Outside-in proof

| Promise (story key example) | Owning slice | Proof |
| --- | --- | --- |
| Mid-chapter at 1440 px, reopen at 390 px → same paragraph at top, chapter current | 3 | New E2E scenario in `epub_book.feature`, rule "Landing on the chosen place" |
| Choose a block, reopen without scrolling → its heading at top, as today | 3 | Existing scenario "Reopening resumes at the last place and shows its block in the book layout" stays green |
| Position saved before this story → reopens at block start | 2, 3 | `NotebookBooksGetReadingPositionControllerTest` still returns a stored locator without `cfi`. A locator without `cfi` displays at href#fragment, the path every block-choosing scenario already exercises |
| Saved while scrolling within a block | 3 | Debouncer unit test (differing `cfi` sends again) plus the new E2E, whose saved position comes only from scrolling inside Chapter One |
| The API keeps and returns the exact place | 2 | Controller tests: PATCH with `cfi` persists it; GET returns it |

## Decisive premises (observed 2026-09-29)

- **Backend locator.** `EpubLocator` is `record EpubLocator(href, fragment)`
  with no `@JsonInclude`. `PdfLocator` has `@JsonInclude(NON_NULL)`.
  - Constructor callers: `BookFormat.java:115`,
    `BookBlockEpubContentLocators.java:61`,
    `NotebookBooksControllerTestBase.java:212`,
    `NotebookBooksReadingPositionControllerTest.java:142`, and
    `NotebookBooksGetReadingPositionControllerTest.java:67`.
  - `BookFormat.EPUB.writeReadingPositionLocator` rebuilds
    `new EpubLocator(href, frag)`, so it would drop a new field unless it
    passes the field through.
  - Searched `backend/src/test`, `frontend/tests`, and `e2e_test`: no test
    asserts `"fragment": null` JSON, so adding `NON_NULL` is safe.
- **Frontend saving.**
  - `BookReadingEpubView.onEpubRelocated` calls `proposeReadingPosition()` on
    every `relocated`/`displayed` event. It sends the current block's first
    locator.
  - `debounceLastReadPositionPatch.sameLocator` compares only href and
    fragment for EPUB. So saving within a block needs only the CFI in the
    proposed locator and in that comparison; no new trigger is needed.
- **Frontend reopening.**
  - `EpubBookViewer` takes `initialLocator` as a pre-joined href string
    (`BookReadingEpubView.initialLocatorDisplayHref`). `openEpub` re-splits it
    and resolves the spine href separately.
  - Meanwhile `epubDisplayTarget(epub)` already does the same thing for
    `displayLocator`.
- **epub.js 0.3.93** (source read, not yet run):
  - `DefaultViewManager.scrolledLocation` uses
    `mapping.page(contents, cfiBase, start, end)`. `Mapping.findStart` returns
    the first element whose box crosses the view's top, as a text-start range.
    So `start.cfi` names the paragraph at the top.
  - `display(target)` scrolls to `view.locationOf(target).top` via `moveTo`.
  - Whether this holds in Donut's `flow: "scrolled"`, `manager: "continuous"`
    host, with the one-display-at-a-time wrapper and across a width change, is
    **not observed**. Slice 1 settles it.
- **E2E support.**
  - `e2e_test/fixtures/book_reading/epub_long_chapter_before_target.epub`
    has block "Chapter One" with 120 unique paragraphs ("Chapter One paragraph
    N. …").
  - `epubReaderElementsWithText(container, selector, text)`
    (`bookReadingShared.ts`) already backs the heading-offset helpers, so it
    serves `p` too.
  - Existing steps: "I set the book reading viewport to {int} by {int}"
    (`cy.viewport`), "I leave the EPUB reading view and return to it" (full
    remount after the PATCH flushes), and "the book block {string} should be
    the current block in the book reader" (reads layout row attributes, which
    the phone PDF scenarios already use while the layout is closed).

## PFE and direction

- Reuse the existing last-read position (`book_user_last_read_position`
  JSON, PATCH/GET reading-position endpoints) and the existing EPUB display
  target.
- No migration: the locator is stored as JSON.
- No North Star topic applies, and no new one is warranted.
- **Coordination:** SEED-059#story-19 (being prepared in parallel) redesigns
  the reading views, including `BookReadingEpubView.vue`. This story's change
  there is two lines, so rebase onto whichever lands first.

## Ordered slices

### 1. Probe: the CFI at the top lands the same paragraph at another width

Type: Probe (no product change; stops dependent slices on failure)
Status: done (passed 2026-09-29; see Learnings)
Proof: recorded observation in this plan's Learnings.

- **Steps:**
  1. In a real browser, open `epub_long_chapter_before_target` in the reading
     view at 1440×900.
  2. Scroll the reader so "Chapter One paragraph 60." is at the top, and read
     `rendition.currentLocation().start.cfi`.
  3. Scroll a little within the same paragraph and confirm the CFI still names
     it.
  4. Reopen at 390×844 and call `rendition.display(cfi)` through the viewer.
- **Pass:** paragraph 60 crosses the reader's top (its top is at or above the
  top, and its bottom is below it).
- **If it misses** (another paragraph, or the chapter start): stop and bring
  the options to the owner. Do not start slice 2.
- **How:** a scratch Cypress spec or a temporary debug hook, removed
  afterwards. About 10 min.

### 2. The reading-position API keeps an exact EPUB place

Type: Behavior
Status: done (accepted proof: `NotebookBooksReadingPositionControllerTest`
`keepsExactEpubPlaceAsCfi`; full `backend:test_only`, `openapi:lint`, and
frontend `vue-tsc --noEmit` passed)
Proof: `NotebookBooksReadingPositionControllerTest` (new case), the existing
`NotebookBooksGetReadingPositionControllerTest`, and `pnpm openapi:lint` after
regenerating the client.

Behavior: a notebook with an EPUB book → PATCH its reading position with an
`EpubLocator_Full` carrying `cfi` → a GET returns the same `cfi`. A locator
without `cfi` is stored and returned as today.

- Add an optional `cfi` to `EpubLocator` with `@JsonInclude(NON_NULL)`,
  matching `PdfLocator`. Block content locators then serialize without
  `"cfi": null`.
- `BookFormat.EPUB.writeReadingPositionLocator` passes the `cfi` through. The
  other constructor callers pass `null`.
- Regenerate with `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`.
- Kept separate from slice 3 as a safe delivery point: the field is optional
  and the API keeps its current behavior. About 10 min.

### 3. Reopening an EPUB at another width shows the same paragraph at the top

Type: Behavior
Status: planned
Proof: the new E2E scenario plus the existing "Reopening resumes at the last
place…" scenario, via
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/book_reading/epub_book.feature`;
and `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/lib/book-reading/debounceLastReadPositionPatch.spec.ts`.

Behavior: at 1440×900, open `epub_long_chapter_before_target`. Scroll until
"Chapter One paragraph 60." is at the top. Set the viewport to 390×844, then
leave and return. Result: that paragraph is at the top of the EPUB reader, and
"Chapter One" is the current block.

- **E2E:**
  - New scenario under rule "Landing on the chosen place".
  - New steps: "I scroll the EPUB reader until the paragraph {string} is at
    the top" and "the paragraph {string} should be at the top of the EPUB
    reader".
  - The assertion checks that the paragraph crosses the top: its top ≤ 8 px
    and its bottom > 8 px.
  - The scroll step parks the paragraph's top only a few pixels above the
    reader top, so the saved CFI's character offset stays in its first line
    (see Learnings).
  - Both steps generalize the heading-offset helper to a selector; they do not
    copy it.
- **Viewer:**
  - `EpubBookViewer` exposes the current `start.cfi`.
  - `epubDisplayTarget` prefers `epub.cfi` over href#fragment.
  - The opening display takes the stored locator and goes through
    `epubDisplayTarget`, with the existing fallback chain. This replaces the
    href-string prop and its separate split and resolve.
  - Remove `initialLocatorDisplayHref`, and `epubDisplayHref` if it has no
    other caller.
- **View and debouncer:**
  - `BookReadingEpubView.proposeReadingPosition` adds the viewer's CFI to the
    current block's locator.
  - `sameLocator` also compares `cfi`, with a unit case added to its spec.
- **Design note:** one rule ("a locator with an exact place displays there,
  otherwise at href#fragment") serves both the opening display and choosing a
  block. Block locators never carry `cfi`, so choosing stays at the block
  start without a special case.
- About 15 min, most of it E2E steps. This is over the ~10 min target for a
  stated reason: the scenario, the viewer's display rule, and saving the place
  form one proof loop, and none of them is observable alone.

## Current decisions

- Paragraph precision is the promise. The E2E asserts that the paragraph
  crosses the reader top, not an exact pixel.
- The exact place lives only on the reading-position locator. Block locators
  stay unchanged.

## Learnings

- **Slice 1 probe passed** (temporary `@focus` scenario in `epub_book.feature`
  plus a debug hook exposing the rendition, both removed; run with
  `SUT_TIMEOUT_MS=360000 CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/book_reading/epub_book.feature`).
  - At 1440×900 with paragraph 60 at the top (top −3 px),
    `currentLocation().start.cfi` was
    `epubcfi(/6/6!/4/2[chapter-one]/122/1:131)`: the paragraph element, its
    text node, and a character offset.
  - After resizing to 390×844 and leaving and returning,
    `rendition.display(cfi)` through the one-display-at-a-time wrapper put
    paragraph 60 at top −37 px, bottom +37 px, stable after 2 s. No wait was
    needed after the opening display.
- **The CFI changes within a paragraph.** A small scroll changed the offset
  (`:131` → `:0`) while the element path stayed. Once `sameLocator` compares
  `cfi`, small scrolls inside one paragraph send new PATCHes, limited by the
  debounce. That meets "saved while scrolling"; do not expect one save per
  paragraph.
- **`display(cfi)` lands on the character, not the paragraph top.** A saved
  offset in a paragraph's last line could put that paragraph's top well above
  the reader top. The E2E scroll step keeps the offset in the first line.
- **E2E runner:** `cy:run` accepts only known spec paths, so new scenarios go
  into an existing feature file. Worktrees share Mountebank's port 2525, so a
  concurrent E2E run in another worktree fails SUT readiness.
