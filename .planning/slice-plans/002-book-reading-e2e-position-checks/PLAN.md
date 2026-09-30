# Book reading E2E position checks do not sit on a page boundary

**Identity:** SEED-059#story-21
**Source:** [story](../../seeds/SEED-059-book-reading-uat-fixes.md#story-21), refined with the owner's
2026-09-30 decisions: analysis by arithmetic first, no multi-size run, no new position-step conventions,
stop with a finding when nothing else is at risk, and no removal of risk-free page-indicator checks.

## Goal and scope

A developer can trust the book reading E2E scenarios to fail because of reading behavior, not because a
page-position assertion sits within pixels of a page boundary at the scenario's fixed window size.

Included: measure, for every scenario that reads layout geometry, the margin to the nearest page boundary
at its window size; repair only scenarios whose margin is too small, using the assertion pattern
already used in `5206b08370` (start position after landing, current block after scrolling).

Excluded: a supported-window-size list; running features at several sizes in the suite; new position-step
conventions; changing `START_AT_TOP_TOLERANCE_PX`; removing page-indicator checks that carry no risk;
any product change, including the page indicator.

Assumption: the scenario currently repaired (`phone_reading.feature`, "Choosing a book block closes the
book layout and moves the book there") stays as repaired; slice 1 re-confirms it rather than changing it.

## Decisive premises

| Premise | Observation | Result |
| --- | --- | --- |
| The page indicator shows the page under the middle of the reader | `sed -n 50,115p frontend/src/lib/book-reading/pdfViewerViewportTopYDown.ts` | `centerY = (containerRect.top + containerRect.bottom) / 2` picks the anchor page |
| The blank fixture has letter pages | `python3` search for `/MediaBox` in `e2e_test/fixtures/book_reading/blank_5_pages.pdf` | `[0 0 612 792]`, five pages |
| Which scenarios read the page indicator and at what fixed size | `grep -n "viewport should be on page\|I am on a window\|book reading viewport" e2e_test/features/book_reading/*.feature`; default `viewportWidth/Height` in `e2e_test/config/common.ts` | Inventory table below; default window is 1200 × 800 |
| Book reading specs run in a linked worktree | `grep -n book_reading scripts/isolated-cypress-active-specs.mjs` | `book_browsing`, `phone_reading`, `epub_book`, `reading_record`, `reorganize_layout` are active, so `pnpm cy:run --spec` accepts them |
| The margin method reproduces the known failure | Unobserved until slice 1 (probe) | |

## Inventory of page-position checks (from the feature files)

The default window 1200 × 800 applies unless a row says otherwise. "Middle-based" means the step reads the
page indicator, so the margin is the distance of the reader's middle from the nearest page boundary.

| Scenario (feature) | Step | Window | Position when read |
| --- | --- | --- | --- |
| Book block jumps the PDF to the anchored page (`book_browsing`) | on page 2 | 1200 × 800 | block 2.2 start (89/1000, page 2) at the reader top |
| Bookmark blocks show, land where they point, and follow scrolling (`book_browsing`) | on page 4, on page 5 | 1200 × 800 | block 4 start; then 0/1000 of page 5 at the top |
| Scrolling the PDF updates the current block (`book_browsing`) | on page 2 | 1200 × 800 | 89/1000 of page 2 at the top |
| Short viewport keeps the layout aside on the current block (`book_browsing`) | on page 2 | 1200 × 280 | 89/1000 of page 2 at the top |
| Same-page scroll moves the current block (`book_browsing`) | on page 1 | 1200 × 800 | 252/1000 + 37.8% of a page below the page 1 top |
| Scrolling past an unmarked block (`reading_record`) | on page 2 | 1200 × 800 | 89/1000 of page 2 at the top |
| Tapping outside the layout keeps the place (`phone_reading`) | on page 1 | 390 × 844 | top of page 1 |
| PDF pages use the screen width, beginning visible (`book_browsing`, `phone_reading`) | on page 1 | 1200 × 800; 390 × 844 | top of page 1 |
| Choosing a block on a phone (`phone_reading`) | top at 89 of 1000 down page 2 | 390 × 844 | already repaired; position step, not middle-based |

Other geometry-reading steps to read in slice 2 for pixel tolerance: `expectPdfPositionAtTopOfReader` (15 px),
`scrollPdfBookReaderDownWithinSamePageForNextBbox` (37.8% of the rendered page height), the EPUB scroll steps
in `bookReadingEpubMethods.ts`, and the `epub_book.feature` sizes (line 92 examples, 1280 × 560, 1440 × 900,
390 × 844).

Hypothesis to test, not a finding: the middle can reach a page boundary only when the reader is tall
relative to the page (roughly reader height above 1.8 × page height for a start near 9% down its page),
which the desktop windows are not and a phone-width window can be.

## Outside-in proof

| Story promise | Owning slice | Observable signal |
| --- | --- | --- |
| The analysis states each scenario's window size and margin, and which are at risk | 1, 2 | Margin table in this plan (scenario, window, reader height, page height, margin in px, verdict), with how each was obtained |
| A scenario that chooses a PDF block asserts where the start is, and passes at 390 × 844 and 390 × 900 | 1 (already true for the repaired scenario), 3 (any other found at risk) | `pnpm cy:run --spec e2e_test/features/book_reading/phone_reading.feature` green at 390 × 844, and green with the window changed to 390 × 900 by a local edit that is reverted |
| A scenario that scrolls past a heading asserts the current block, not the page holding the middle | 3, only when slice 2 finds one at risk | The repaired scenario's assertions name the block or the start position; its `cy:run` is green |
| Nothing at risk: stop with the finding | 2 | The margin table, and `git diff` shows no test change |

## Ordered slices

### 1. The margin method reproduces the known phone failure
Type: Structure
Status: done (revised: see Learnings)
Size: about 10 minutes.
Proof: a temporary local probe (an uncommitted step or spec that logs the reader's middle, the page 2
boundary and the margin after landing block 2.2) run through `pnpm cy:run --spec
e2e_test/features/book_reading/phone_reading.feature` at 390 × 844 reports a margin of about 1.5 px, and at
390 × 900 a margin that fails the same way (the seed's reproduction); the repaired scenario is green at both
sizes. The margin observed at the two heights also gives the shift a height change causes at the reader's
middle, which sets the decision threshold below. Explained empty change: no product or test file changes;
the probe is reverted (`git status` clean apart from this plan).

Probe: if the probe cannot reproduce the seed's 1.5 px, stop dependent slices and revise this plan.

Decision threshold (revised after slice 1 data): a scenario is at risk when its margin is smaller than
about 37 px, the 28 px shift the reader's middle moves between the 390 × 844 and 390 × 900 windows (half
the height difference, confirmed) plus the roughly 9 px landing offset seen locally. A negative margin
(middle already past the boundary) is at risk by definition.

Enables slice 2.

### 2. Every page-position scenario has a known margin and verdict
Type: Structure
Status: done (margin table below; three rows at risk, so slice 3 applies)
Probe: add the geometry log inside `expectCurrentPage` (and `expectPdfPositionAtTopOfReader`) in
`e2e_test/start/pageObjects/bookReadingPdfMethods.ts`, appending one JSON line per call to a file with
`cy.writeFile` (headless Cypress hides `console.log`), `cy.wait(1500)` before measuring, and one run per
feature; the window comes from the "I am on a window W * H" step (`cy.viewport`); reader height is window
height minus 55 px.
Size: about 10 minutes.
Proof: the inventory rows are each run once with the same temporary probe at their own window size; the plan
gains the margin table and one verdict per row, and the other geometry-reading steps listed above are read
for pixel tolerances with a verdict each. Explained empty change: probe reverted, plan updated only.

Stop rule: if no row other than the already repaired scenario is at risk, record that finding in this plan,
change no test, and skip slice 3.

Enables slice 3 when any row is at risk.

### 3. At-risk scenarios assert position by start or current block
Type: Behavior
Status: done (`cy:run` green: `book_browsing` 9 at 1200 × 800 and 1200 × 856, `phone_reading` 7 at 390 × 844 and 390 × 900; `book_browsing:52` drops the racy page step, the current-block assertion retries; `phone_reading:11` and `:55` assert the start position)
At risk (from the margin table): `book_browsing.feature:52` (page-indicator step after the same-page
scroll; replace with the current-block assertion) and `phone_reading.feature:11` and `:55` at 390 × 844
(page 1 at the top; assert the start position `expectPdfPositionAtTopOfReader(1, 0)` or the existing
top-of-page step).
Size: about 10 minutes, per at-risk scenario group.
Proof: for each scenario slice 2 marked at risk, the middle-based assertion is replaced by "the top of the
PDF book reader should be at Y of 1000 down page N" after landing, or by the current-block assertion after
scrolling; the scenario's journey and its other assertions stay. `pnpm cy:run --spec` on each touched
feature is green at its window size, and once green at its height plus 56 px by a local edit that is
reverted. Follows the `e2e-authoring` skill (page object owns the check; feature steps stay
behavior-level).

Behavior: an at-risk scenario, given its window size and PDF block landing, → the assertion no longer depends
on which page holds the middle of the reader → the scenario passes at the window size and 56 px taller.

## Margin table (slice 2)

Measured locally with a temporary probe (reader rect, page rect, indicator text after 1500 ms), one
`pnpm cy:run --spec` per feature (`book_browsing` 9, `reading_record` 9, `phone_reading` 7 passing). Margin
is the reader middle's distance to the nearest boundary of the page holding it; negative means already past.
Reader height is the window height minus 64 px (desktop) or 55 px (phone).

| Scenario (feature) | Step | Window | Reader h | Page h | Margin px | Verdict |
| --- | --- | --- | --- | --- | --- | --- |
| See layout and beginning (`book_browsing`) | on page 1 | 1200 × 800 | 736 | 992 | 377 | safe |
| Book block jumps to anchored page (`book_browsing`) | on page 2 | 1200 × 800 | 736 | 992 | 465.5 | safe |
| Scrolling the PDF updates the current block (`book_browsing`) | on page 2 | 1200 × 800 | 736 | 992 | 457 | safe |
| Short viewport (`book_browsing`) | on page 2 | 1200 × 280 | 216 | 992 | 197 | safe |
| **Same-page scroll (`book_browsing`:52)** | on page 1 | 1200 × 800 | 736 | 992 | -10 | **at risk** |
| Bookmark blocks (`book_browsing`:73, :76) | on page 4, on page 5 | 1200 × 800 | 736 | 1084 | 450.5, 368 | safe |
| Scrolling past an unmarked block (`reading_record`:42) | on page 2 | 1200 × 800 | 736 | 992 | 457 | safe |
| **PDF page as wide as the phone screen (`phone_reading`:11)** | on page 1 | 390 × 844 | 789 | 433 | 29.5 | **at risk** |
| **Tapping outside the layout (`phone_reading`:55)** | on page 1 | 390 × 844 | 789 | 433 | 29.5 | **at risk** |
| Choosing a block on a phone (`phone_reading`) | start position | 390 × 844 | 789 | 433 | offset 0.04 of 15 px | repaired |

Other geometry steps: `expectPdfPositionAtTopOfReader` offsets are 0 to 0.2 px against 15 px (safe);
`scrollPdfBookReaderDownWithinSamePageForNextBbox` scales with page height, not window height, margin about
23 px on a 992 px page, upper margin unchecked (safe from window height); EPUB steps use 8 px element-span
checks driven by epub.js, no page indicator, no evidence of risk (read, not measured).

Notes: `book_browsing`:52 passes only because `expectCurrentPage(1)` matches the indicator before the scroll
handler updates it (a race); with a 1500 ms wait first it fails, since the scroll lands the top at about
630/1000 of page 1 and the middle is in page 2. The phone rows swing 28 px per 56 px of window height; at
390 × 900 page 1's margin is about 1.5 px, matching the seed's number.

## Current decisions

- Analysis is arithmetic on measured geometry, not a size matrix; the probe is local and never committed.
- The 15 px start-at-top tolerance is not re-verified or changed.
- Page-indicator checks with a safe margin stay, including those redundant with a start-position
  assertion.

## Learnings

- Slice 1 (local, `phone_reading.feature`, block 2.2 landed): at 390 × 844 the reader is 789 px high, page 2
  spans 7.5–440.5 px (page height 433, page divs overlap by 7 px), the reader middle is 449.5 px, so the
  middle is 9 px past page 2's bottom and the indicator reads "3 / 5"; the old middle-based step fails
  locally at 844. At 390 × 900 the middle is 477.5 px (37 px past). The seed's 1.5 px margin was not
  reproduced (local landing is about 9 px above the reader top, inside the 15 px tolerance; CI differed).
  The method and the 28 px shift are confirmed; the repaired scenario is green at 844 and 900.
- The plan's "reproduces 1.5 px" promise was not met literally; the plan was revised, not the method:
  the qualitative failure (indicator on the wrong page) reproduces and the threshold now allows the
  observed landing variance.

## Execution complete

Product advice: no follow-up story; the outcome is met. Residual gaps: the same-page scroll's upper margin and the EPUB steps were judged by reading, not measured.
