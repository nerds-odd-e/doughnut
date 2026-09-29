# Land on and track the chosen place in an EPUB

Work item: **SEED-059#story-1**.
Source: [refined story](../../seeds/SEED-059-book-reading-uat-fixes.md#story-1)
(owner decisions 2026-09-29). Owner instruction for this plan: finding the
cause of the chapter-landing offset is the first slice, as a probe.

## Goal and scope

An EPUB reader lands where they chose, and the content shown, the current
block, and the selection agree, from the first open onwards.

- Choosing a block, or following a link inside the book, puts that place's
  start at the top of the view at any viewport size.
- Every block has a place to go: a block with no content of its own goes to its
  table-of-contents target, and the chosen block stays selected and current
  even when another block starts at the same spot.
- First open: the current block matches what is shown; no block is marked.
- Reopening: resumes at the last place with the current block visible in the
  layout.

Excluded: anchorless blocks in books attached before this fix (owner decision;
re-attaching gives the fix), changing or clearing records already made
(story 6), the EPUB current-block bar (story 10), PDF changes, and any
migration of stored layouts.

## Architecture

- **PFE:**
  - Landing: `EpubBookViewer.displayLocator` is the single landing path for
    layout choices and the reading panel's advance. Change it; do not add a
    second landing path. Links inside the book go through epub.js directly;
    slice 1 finds whether they share the cause.
  - Current block: `currentBlockIdFromEpubView` is the one view →
    block rule, used for relocation, the post-advance commit, and reopen (the
    viewer's first report after display; href-based seeding is gone). Evolve it
    there, not per caller.
  - Anchorless blocks: the extractor already gives the `*beginning*` block a
    `beginning_anchor` payload that carries a start without contributing
    content. Reuse that idea for table-of-contents entries that receive no
    content; do not add a second locator representation.
  - Auto-marking: keep `useAutoMarkNoDirectContentPredecessor` unchanged. The
    spurious first-open record comes from the wrong current block, not from the
    auto-mark rule.
  - Layout following: `BookReadingBookLayout` already scrolls the current
    block's row into view when `currentBlockId` changes. Reopening must go
    through that same path.
- **One current-block rule** (slice 2, used by slices 3–6): the current block
  is the last block in reading order whose start is at or above the top of the
  view. Among blocks that share that start, the selected block wins. This
  replaces "last block in the bottom-most visible spine file". It is the same
  rule PDF readers already experience, and it depends on landing at the top.
- No North Star topic or ADR is affected: the Book stays private reading
  structure over its source attachment.

## Decisive premises

| Premise | Observation | Result |
| --- | --- | --- |
| EPUB behavior has running E2E proof today | `epub_book.feature` line 1; `git log -S@ignore`; `pnpm cy:run --spec …/epub_book.feature` | **False.** `@ignore` was added in `555991c33f` (2026-05-11, inside an unrelated folder refactor). The isolated runner refuses the spec because it is not in `scripts/isolated-cypress-active-specs.mjs`. No other feature covers EPUB. |
| The existing EPUB scenarios pass when re-enabled | Temporary local run with `@ignore` removed and the spec added to `APPLICATION_ONLY_ACTIVE_SPECS` (reverted afterwards): `SUT_TIMEOUT_MS=360000 CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/book_reading/epub_book.feature` | **False, and caused by this story's defect.** 4 pass, 6 fail, all at `chooseBookBlockByTitle`'s check that the chosen block becomes current (`data-current-block`). The screenshot shows "Chapter Beta" chosen, selected, and shown, but not current. |
| The current block is resolved per spine file from the bottom of the view | Read `currentBlockIdFromEpubLocation`, `EpubBookViewer.onRelocated`, `BookReadingEpubView.onEpubRelocated`; the fixture's `nav.xhtml` and chapters | Confirmed: it uses the bottom-most visible spine file (`location.end`) and picks the **last block in reading order** in that file. With the fixture's short chapters, choosing "Chapter Alpha" (chapter2) shows chapter3 too, so "Section Beta-Two" becomes current. The same rule explains first open (cover and contents visible → contents current) and the shared-start case (title → detailed contents). |
| Landing today does not aim at "start at top" | Read `EpubBookViewer.displayLocator` | Confirmed: after `display()` and a fixed 100 ms wait, it scrolls a heading's **next sibling** to the **centre**. |
| The landing offset comes from epub.js adding the previous section above after the scroll | Slice 1 probe (see *Learnings*) | **Partly.** Two viewer-side causes: Chrome scroll anchoring doubles epub.js's own correction when it prepends sections, and `displayLocator` centres the whole chapter wrapper. |
| Anchorless blocks come from table-of-contents entries that receive no content at attach time | Read `EpubStructureExtractor.extractContentForSpineFile` and `BookBlockEpubContentLocators` | Confirmed: an entry sharing another's start, or with no content before the next start, gets an empty payload list and so no locator, although its nav row has a target. |
| The first-open record is the auto-mark rule reacting to the wrong current block | Read `useAutoMarkNoDirectContentPredecessor` | Confirmed: when the current block changes, a predecessor with exactly one locator and no record is marked read. |
| The extractor can be unit-tested with EPUBs built in code | Read `EpubStructureExtractorTest` | Confirmed. |

## Key examples → proof

The UAT books are not in the repository. Besides the revived
`epub_valid_minimal.epub` scenarios, E2E uses one added fixture EPUB in
`e2e_test/fixtures/book_reading/`, built reproducibly by a script beside it (as
`regenerate_mineru_output_for_refactoring.sh` is). Each slice adds only the
parts it needs: a long chapter before another chapter and a contents link
(slice 2), a cover before the first entry (slice 4), an entry with no content
of its own and two entries sharing a start (slice 5), and enough entries to
overflow the layout at 1280×560 (slice 6).

| Promise | Slice | Proof |
| --- | --- | --- |
| Choosing a chapter after a long chapter puts its heading at the top (Origin IV, Alice III/XII) | 2 | E2E: heading at the top of the reader at 1440×900 and 1280×560 |
| Following a link inside the book lands on its heading (Origin "CHAPTER 2") | 3 | E2E: follow the fixture's contents link → heading at the top |
| The chosen block is current; scrolling on moves the current block | 2 | The six revived scenarios that fail today, green; "Current block updates on scroll…" green |
| First open: the current block holds what is shown; nothing is marked | 4 | E2E: attach the fixture with a cover, open → the cover's block is current, no block marked |
| A block with no content goes to its table-of-contents target (Alice licence) | 5 | `EpubStructureExtractorTest`: an entry with no content gets a start at its nav target; E2E: choose it → its text is shown |
| The chosen block stays selected and current when it shares a start (Origin title) | 5 | E2E: choose the first of two entries sharing a start → it is shown, selected, and current |
| Reopening shows the current block in the layout at 1280×560 | 6 | E2E: choose a late block, leave and return → its layout row is visible |
| Existing EPUB behavior keeps working | 2 onward | `epub_book.feature` green in the isolated runner |

Proof command for every E2E row:
`SUT_TIMEOUT_MS=360000 CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/book_reading/epub_book.feature`.
Focused unit proof: `CURSOR_DEV=true nix develop -c pnpm frontend:test` for the
changed spec files, and the backend extractor test via Gradle `--tests`.

## Slices

### 1. Find why choosing a chapter lands inside it (probe)
Type: Structure (probe; no product change)
Status: done
Proof: a written observation in *Learnings* that explains the measured
offsets.

Reproduce defect 1 in a real browser: *Origin of Species* (Project Gutenberg
EPUB, attached locally) chapter IV at 1440×900 and 1280×560, and Alice
chapter III. Measure where the heading ends up and what epub.js adds or
removes above it, and when, relative to `displayLocator`'s scroll. Check the
same for a link inside the book. Then try to reproduce the offset with a small
generated fixture that has a long chapter before the chosen one. If it
reproduces, keep the fixture and its generator script for slice 2.

Record the cause, the landing approach it implies, and whether links inside
the book share it. **Stop rule:** if the cause is not in how Donut scrolls after
`display()` (for example, an epub.js defect with no workaround at the viewer),
stop before slice 2 and replan it with the owner.

### 2. Choosing a block lands at the top, and the block at the top is current
Type: Behavior
Status: done
Proof: all of `epub_book.feature` green in the isolated runner except the
in-book link scenario (slice 3), including the six scenarios that failed
before and the choose-Chapter-Two outline at 1440×900 and 1280×560;
`currentBlockIdFromEpubLocation.spec.ts` for the new rule.

Behavior: an EPUB with a long chapter before "Chapter Two" → the reader chooses
"Chapter Two" in the layout → its heading is at the top of the reader, and it
is selected and current, even when later blocks are also visible → the reader
scrolls until another block's start passes the top → that block becomes
current while the selection stays.

Start from the parked second attempt, `slice-2-landing-attempt.patch` in this
folder (`git apply`; delete the file in this slice's commit). It already has:
landing through `rendition.display()` only with `overflow-anchor: none`;
`displayLocator` awaiting the opening display (`opened`); `startTopPx` /
`landingLimitPx` on the viewer; the rule in `currentBlockIdFromEpubLocation`
(last start ≤ top; selected wins between that start and the landing limit)
used for relocation, the post-advance commit, and reopen; its rewritten spec;
the page-object methods, steps, landing Rule, feature-level `@ignore` removed,
and the spec in `APPLICATION_ONLY_ACTIVE_SPECS`. Remaining for this slice:

- Define "at the top" with a named tolerance, so a block whose start sits a
  heading margin below the view top (+20 px measured for Contents on first
  open, from margin collapse through `div#contents`) counts as at the top.
- "Current block updates on scroll…": after choosing Chapter Alpha, Cell One is
  already in view, so its scroll step moves nothing. Change only its scroll
  step so a later block's start passes the top; keep its expectation.
- Move the in-book link scenario, `followEpubLinkInReader`, and its step to
  slice 3 (keep them out of this commit).

### 3. Following a link inside the book lands at the top and tracks
Type: Behavior
Status: planned
Proof: E2E "Following a link inside the book shows its heading at the top"
(1280×560, the fixture's "Go to Chapter Two"), heading at the top and Chapter
Two current, green on repeated runs.

Behavior: the reader follows the contents link to Chapter Two → its heading is
at the top, and Chapter Two is current. The parked attempt had it flaky: once
landing 20 px low, once at the top but not current (the link targets
`<a id="chap02">` inside the h2 while the block starts at `div#chapter-two`),
and once epub.js held only the chapter 2 view at `scrollTop` 0 for 30 s with no
prepend and no `relocated`. Find why that display hangs before fixing; if the
cause is inside epub.js with no workaround at the viewer, stop and replan with
the owner.

### 4. Opening a new EPUB marks nothing
Type: Behavior
Status: planned
Proof: E2E on the added fixture, extended with a cover before the first entry.

Behavior: a newly attached EPUB with a cover → the reader opens it for the
first time → the current block is the one holding the cover (`*beginning*`),
and no block is marked read. Expected to need no new rule beyond slice 2 (its "at the top" tolerance covers the heading-margin offset measured on first open); if
it does, record why in *Learnings*.

### 5. Every block has a place to go
Type: Behavior
Status: planned
Proof: `EpubStructureExtractorTest` for the stored start; E2E for choosing the
blocks.

Behavior: a newly attached EPUB whose table of contents has an entry with no
content of its own, and two entries sharing one start → the reader chooses
either → the book shows that entry's table-of-contents target, and the chosen
block is selected and current. The panel offers *Read* for the block that is
shown. Attach-time change only: books attached earlier are excluded.

### 6. Reopening shows the current block in the layout
Type: Behavior
Status: planned
Proof: E2E at 1280×560 on the added fixture, extended so its layout overflows.

Behavior: the reader has read to a late block → leaves the reading view and
returns at 1280×560 → the book resumes at the last place, and the current
block's row is visible in the layout.

## Current decisions

- Probe first (owner instruction). Slice 1's stop rule did not trigger: both
  causes are in the viewer.
- Landing target: the place's start at the top of the view, as in PDF.
- One current-block rule (top of view, selection wins ties) for relocation and
  reopen seeding, delivered with the landing change (slices 2 and 3
  consolidated after the slice 2 attempt). In-book links split into slice 3
  after the second attempt.
- One added fixture EPUB, generated by a committed script; the minimal fixture
  keeps its current role.
- Books attached before this fix get no stored start for anchorless blocks.

## Learnings

- **Slice 1 cause (probe, headless Chrome on a page replicating
  `EpubBookViewer` with the same epubjs 0.3.93 and `renderTo` options):**
  - A. `displayLocator` resolves the stored fragment to Gutenberg's
    `<div class="chapter" id=…>`, which wraps the whole chapter; not a heading,
    so it `scrollIntoView({block:"center"})`s the whole div, putting the heading
    about (div height − view height) / 2 above the top. The 100 ms wait is not
    the cause.
  - B. `ContinuousViewManager` prepends earlier sections after `display()` and
    `counter()` scrolls down by their height, while Chrome's scroll anchoring
    has already shifted by that height, so each prepended section counts twice
    (`scrollBy(0,5200)` with scrollTop already 5264). Nothing in `frontend/src`
    sets `overflow-anchor`. In-book links go through `rendition.display(href)`
    and share only this cause.
  - With `.epub-container { overflow-anchor: none }` alone, `display()` puts the
    target at 0 and it stays there for 3.5 s, for Origin IV, Alice III, and the
    "CHAPTER 2" link at 1440×900 and 1280×560. A post-display
    `scrollIntoView(start)` settles at +4 px, so rely on `display()` alone.
  - Measured today (heading top vs view top): Origin IV −6,273 (1440×900) and
    −7,487 (1280×560); Alice III −847 and −1,098; Origin "CHAPTER 2" link
    −3,422 and −4,428. The added fixture reproduces both: choosing Chapter Two
    −2,700 / −2,870, its link −5,420 / −5,760; 0 with anchoring off.
  - Not checked: WebKit, and whether anchoring also jumps the view when scrolling
    up into a prepended chapter (likely; the same CSS would cover it).
