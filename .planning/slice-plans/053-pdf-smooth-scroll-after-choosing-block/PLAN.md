# Scroll a PDF smoothly right after choosing a block

Work item: **SEED-059#story-5**.
Source: [refined story](../../seeds/SEED-059-book-reading-uat-fixes.md#story-5)
(owner decision 2026-09-29: remove snap-back).

## Goal and scope

A PDF reader's view moves only where they scroll it, from the first wheel step
after choosing a block, wherever the pointer rests: nothing pulls the view back
to an unmarked block, and wheeling over the Reading Control Panel scrolls the
book.

Excluded (story): the panel's position, look, and the text it covers; EPUB
scrolling; the current block at the layout's edge in short viewports.

Must keep working: PDF landing, the current block following scrolling, the
panel appearing at the block's end and after scrolling past it, the panel
moving on to the next block once the selected one is marked, Read/Skim/Skip,
resume, and layout editing.

## Architecture

- **PFE:** snap-back lives in `frontend/src/composables/useBookReadingSnapBack.ts`
  (`shouldSnapBack`, `performSnapBack`, attempt budget, `snapAnimationKey`),
  wired through `commitCurrentBlockId` in `BookReadingContent.vue`. Its only
  support is the wheel/touch suppression: `lib/book-reading/intervalScrollSuppression.ts`,
  `registerScrollSuppression` / `getScrollSuppression` / `isHoldWindowActive`
  in `PdfBookViewer.vue` and `bookReaderViewerRef.ts`, and the `checkEvent()`
  guards in `usePdfGestureZoom.ts`. The panel's `snap-attention` animation
  (`ReadingControlPanel.vue`, `data-snap-animating`) is driven only by
  `snapAnimationKey`. All of it goes; removal leaves no trace.
- The same composable also decides **which block the panel offers and when**
  (`confirmationTargetBlock`, `blockAwaitingConfirmation`,
  `updateLastDirectContentGeometry`, `lastContentBottomVisible`,
  `geometryEverVisibleForSelection`). That panel logic stays; name what remains
  for what it does (the reading panel's target), not for snap-back.
- The panel is an absolutely positioned overlay beside the PDF scroll container
  (`pdf-book-viewer`), so a wheel over its card reaches no scrollable ancestor.
  The fix sends a plain (non-Ctrl/Meta) wheel over the panel to the PDF scroll
  container; Ctrl/Meta wheel keeps its zoom meaning only over the PDF. EPUB is
  unchanged.
- No ADR or North Star topic is affected.

## Decisive premises (observed 2026-09-29)

- **Snap-back causes the backward jumps.** Read `useBookReadingSnapBack.ts`:
  when the current block moves into the successor of an unmarked selected block
  whose content bottom was seen, `performSnapBack` scrolls back — to the block
  top when the content fits (a screen-sized jump, the UAT's −800) or to the
  content bottom at reading clearance (a short jump, the UAT's −156 / −265) —
  at most twice per block (`snapbackAttempts < 2`), then ignores wheel/touch
  events for `SNAP_HOLD_MS = 500` (the UAT's "ignored step"). Matches the UAT
  pattern of two backward jumps per run. Confirmed by reading; the slice-1
  wheel observation settles whether any other jump remains.
- **Scroll suppression serves only snap-back.** `grep -rn "\.activate(\|scrollSuppression" frontend/src`:
  `activate` is called only in `useBookReadingSnapBack.ts`. Confirmed.
- **No E2E exercises snap-back.** `grep -rni snap e2e_test/features e2e_test/step_definitions e2e_test/start/pageObjects`:
  no book-reading hit. Snap-back is covered only by
  `frontend/tests/pages/BookReadingPage.snap.spec.ts` (with
  `bookReadingPageSnapTestSupport.ts`) and
  `frontend/tests/composables/useBookReadingSnapBack.spec.ts`
  (`snapBackNormalizedSpanFitsViewport`). Its `panel geometry` describe block
  tests panel behavior that stays. Confirmed.
- **E2E steps for the new scenario exist.** `reading_record.feature` /
  `book_browsing.feature` already use "I choose the book block …", "I scroll the
  PDF book reader until the Reading Control Panel shows for …", "I scroll the
  PDF book reader to bring page 2 into primary view", "the book reader PDF
  viewport should be on page 2", and "the book block … should be the current
  block in the book reader". Fixture `e2e_test/fixtures/book_reading/mineru_output_for_refactoring.json`:
  "2.1 Easier to Change—and Harder to Misuse" has direct content on page 1
  (y 677–883); "2.2 Refactoring as Strengthening the Code" starts page 2.
  Confirmed.
- **E2E scrolling uses `scrollTo`, not wheel events** (`bookReadingPdfMethods.ts`),
  and no `cypress-real-events` dependency exists (`e2e_test/package.json`).
  A wheel over the panel is dispatched with `trigger('wheel', …)`, which does
  not scroll natively — so it fails today and passes only when the panel sends
  the wheel on. Confirmed.

## Outside-in proof

| Promise | Owner | Signal |
| --- | --- | --- |
| Scrolling past an unmarked block stays where the reader scrolled | Slice 1 | New E2E scenario in `reading_record.feature`: choose 2.1, scroll until the panel shows for 2.1, scroll to bring page 2 into primary view → viewport on page 2, "2.2 …" is the current block, and the panel is still offered for 2.1. Red before the change (snap-back pulls back) |
| Every downward wheel step moves down after choosing a block | Slice 1 | Manual observation (dough-manual-testing) on the dev stack with the refactoring fixture book at 1440×900: choose 2.1, dispatch 8 real 400 px wheel steps at 120 ms over the PDF centre (Chromium DevTools protocol or Playwright `mouse.wheel`), record per-step `scrollTop` deltas — all positive. A remaining backward jump stops the plan for replanning |
| Wheeling over the panel scrolls the book; its buttons still work | Slice 2 | New E2E scenario: with the panel shown for 2.1, wheel down over the panel → the PDF viewer's `scrollTop` increases; existing "Mark a book block as read" scenarios keep the buttons proven |
| Must-keep-working behavior | Slices 1–2 | `e2e_test/features/book_reading/{book_browsing,reading_record,reorganize_layout,phone_reading}.feature` and remaining `frontend/tests/pages/BookReadingPage*.spec.ts` stay green |

## Slices

### 1. Nothing pulls a PDF reader back to an unmarked block

Type: Behavior
Status: done (manual wheel observation outstanding)
Proof: new E2E scenario red → green; manual wheel-delta observation; book
reading E2E features and `pnpm frontend:test` for book-reading specs green.

Behavior: a PDF reader has chosen "2.1 Easier to Change—and Harder to Misuse"
and seen the end of its content without marking it → they scroll into "2.2 …"
→ the view stays where they scrolled, "2.2 …" becomes the current block, no
wheel step is ignored, and the panel is still offered for 2.1.

Change: delete snap-back and everything only it uses (see Architecture): the
snap decision and scroll, the attempt budget and its reset on marking, the
scroll suppression library and its viewer/gesture wiring, `snapAnimationKey`
and the panel's snap animation, and their tests. `commitCurrentBlockId` no
longer vetoes a commit. Keep the panel-target logic and its `panel geometry`
tests, renamed to what they are about.

### 2. Wheeling over the Reading Control Panel scrolls the PDF

Type: Behavior
Status: done
Proof: new E2E scenario red → green; `reading_record.feature` mark scenarios
green.

Behavior: the Reading Control Panel is shown over the PDF for 2.1 → the reader
wheels down with the pointer on the panel → the PDF scrolls down; clicking
*Read* still marks the block.

Change: send plain wheel events over the PDF panel to the PDF scroll container
(one listener; no change to EPUB, panel placement, or zoom).

## Current decisions

- Snap-back is removed, not softened (owner, 2026-09-29).
- Once a block's content end was seen, the panel stays offered for it until it
  is marked or the selection changes; it no longer hides when the successor
  becomes current (coordinator, from the story's key example, 2026-09-29).
- When the fixed panel and the "Now reading" bar both show, the panel stacks
  just above the bar (owner, 2026-09-29); both used the same bottom slot and the
  bar covered the panel.
- The E2E wheel over the panel uses a synthetic `wheel` event; real-wheel
  timing is covered by the slice-1 manual observation, not a new E2E
  dependency.

## Accepted proof

- Slice 1: `reading_record.feature` "Scrolling past an unmarked book block stays
  where the reader scrolled" red (view pulled back to page 1) → green, including
  marking 2.1 from the panel above the "Now reading" bar; `pnpm frontend:test`
  (1922), book_browsing/reading_record/reorganize_layout/phone_reading 29/29,
  `epub_book.feature` 17/17 (EPUB panel moved into the shared
  `ReadingOverlayDock`).
- Slice 2: `reading_record.feature` "Wheeling over the Reading Control Panel
  scrolls the book" red (scrollTop unchanged) → green; one `@wheel` on the PDF
  `ReadingOverlayDock` forwards to the viewer's `scrollByWheel`, which ignores
  zoom wheels (`isZoomWheel`, shared with gesture zoom); reading_record,
  book_browsing, phone_reading 22/22; `pnpm frontend:test` 1920.
- **Outstanding:** the per-step real-wheel delta observation. Still needed
  before the story closes, in a headed browser on the dev stack.

## Learnings

- CDP `mouseWheel` sent through `Cypress.automation` reaches the DOM (not
  prevented) but does not natively scroll in headless Electron/Chrome, so it
  cannot measure wheel deltas; the persistent dev stack refuses linked
  worktrees.
- With snap-back gone, scrolling past an unmarked block is the normal path, and
  it exposed two older rules: the panel hid once the successor became current,
  and the fixed panel and the "Now reading" bar shared one bottom slot.

## Execution complete

Product advice: no backlog change. Before wrap-up closes the story, observe the
key example with a real wheel in a headed browser on the Development stack
(Think Python 8.1 and 14.6, Attention 3.1: every 400 px step moves down; wheel
over the panel keeps scrolling). Story 18 (EPUB panel anchoring) should start
from the shared `ReadingOverlayDock` that both readers now use.
