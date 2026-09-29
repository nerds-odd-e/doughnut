# Read a book on a phone

Work item: **SEED-059#story-2**.
Source: [refined story](../../seeds/SEED-059-book-reading-uat-fixes.md#story-2)
(owner decisions 2026-09-29).

## Goal and scope

A reader who attached a book at the desk can keep reading it on a phone: the
book uses the screen width, the reading panel works, and the book layout opens
as a drawer to choose where to go.

- Below 768 px the closed layout takes no space; EPUB text and PDF pages use
  the screen width; the "Now reading" bar and Read/Skim/Skip panel are fully
  visible.
- The *Book layout* toggle is reachable and opens the layout at 390 and 768 px.
- The drawer behaves like the notebook sidebar drawer: it opens below the main
  menu, tapping the backdrop closes it, and choosing a block closes it and
  moves the book there.
- PDF on a phone only has to be not broken (page as wide as the screen, today's
  zoom).

Excluded: changing the layout on a phone, anything extra for *AI Reorganize* or
creating blocks on a phone, a reflowed PDF mode, attaching from a phone, swipe
gestures, landscape tuning, font-size controls, and other SEED-059 defects seen
on a phone. Considered and excluded: making the main menu's "Menu" fallback fit
the top bar on every route without an active item; this story fixes the book
route only (see *Current decisions*).

## Architecture

- **PFE:**
  - Drawer: `useNotebookSidebarDrawer` already owns a phone drawer (768 px
    breakpoint, fixed below the main menu, closes on navigation) and
    `NotebookSidebarLayout` its backdrop. `BookReadingBookLayout` re-implements
    the same behavior with its own breakpoint (`bookReadingLayoutBreakpoint.ts`),
    classes, and backdrop, and that copy is the broken one. Slice 3 makes the
    book layout use one shared drawer behavior instead of a second copy; the
    notebook sidebar's observable behavior stays as it is.
  - Main menu: `useNavigationItems` decides the active item per route. Notebook
    pages count as the "Note" area, which keeps the collapsed menu within the
    88 px the top bar (`GlobalBar`) reserves. The book reader belongs to a
    notebook, so it joins that area (slice 2) rather than the top bar learning
    the menu's width.
- No North Star topic or ADR is affected.

## Decisive premises

| Premise | Observation | Result |
| --- | --- | --- |
| The phone panel keeps its width because `relative` beats `fixed` | Compiled `relative fixed -translate-x-full` with the repo's Tailwind (`compile` from `tailwindcss` 4.3.3) | Confirmed: `.relative` is emitted after `.fixed`. |
| The UAT measurements reproduce with the E2E fixture book | Temporary probe scenario (reverted): fake PDF "refactoring" book opened at 390×844 and 768×1024, `SUT_TIMEOUT_MS=360000 CURSOR_DEV=true nix develop -c pnpm cy:run --spec <probe>` | Confirmed: at 390 the aside is `position: relative` at x −288, main 102 px wide; at both sizes `elementFromPoint` at the toggle's centre is *Toggle menu*. |
| The toggle is covered because the book route has no active menu item | Same probe, also on a note page at 390×844; read `HorizontalMenu.vue` and `useNavigationItems.ts` | Confirmed: book page's collapsed menu is 112 px (wrapper 120 px) showing the "Menu" fallback; note page's is 88 px (wrapper 96 px) and its *Show sidebar* button is not covered. `bookReading` is not in the "Note" item's active routes. |
| Phone-width E2E is supported and runs in the isolated runner | `note_tree_view.feature` "Open sidebar on a narrow window" uses `I am on a window {int} * {int}`; `scripts/isolated-cypress-active-specs.mjs` | Confirmed; a new feature file must be added to the active-spec list. |
| EPUB attach and open steps work today | Plan 049's observation: 4 `epub_book.feature` scenarios pass when revived | Confirmed there; `epub_book.feature` itself stays `@ignore` (story 1 owns reviving it), so this plan's EPUB scenario lives in its own feature. |
| The initial open/closed state is decided at mount from the window width | Read `bookLayoutAsideInitiallyOpen` and `BookReadingContent.vue:218` | Confirmed: scenarios must set the viewport before opening the book. |

## Key examples → proof

New feature `e2e_test/features/book_reading/phone_reading.feature`, added to
`APPLICATION_ONLY_ACTIVE_SPECS`. It uses the existing fake "refactoring" PDF
book and `epub_valid_minimal.epub` (the UAT's Alice and Think Python are not in
the repository), and sets the viewport before opening the book.

| Promise | Slice | Proof |
| --- | --- | --- |
| At 390×844 a PDF page is as wide as the screen, not a thumbnail | 1 | E2E: the PDF content area is at least 90 % of the viewport width |
| At 390×844 EPUB text fills the screen width | 1 | E2E: the EPUB reader area is at least 90 % of the viewport width and shows its opening text |
| At 390×844 the Read/Skim/Skip panel is fully visible and can be tapped | 1 | E2E: scroll until the panel shows for "Code Refactoring", panel inside the window, mark it read → marked in the layout |
| At 390 and 768 px *Book layout* opens the layout | 2 | E2E at both sizes: a real (non-forced) click on the toggle → the layout is visible |
| Choosing a block closes the drawer and moves the book there | 3 | E2E at 390×844: open the layout, choose "2.2 …" → drawer closed, PDF on page 2, block selected |
| Tapping the backdrop closes the drawer and keeps the place | 3 | E2E at 390×844: open the layout, tap the backdrop → drawer closed, same page |
| The drawer opens below the main menu | 3 | E2E: the drawer's top edge is at or below the main menu's bottom edge |
| Desktop reader keeps working | 1–3 | `book_browsing.feature`, `reading_record.feature`, `reorganize_layout.feature` green; `BookReadingPage.spec.ts` green |
| Notebook sidebar drawer keeps working | 3 | `note_tree_view.feature` green; existing sidebar unit specs green |

E2E command:
`SUT_TIMEOUT_MS=360000 CURSOR_DEV=true nix develop -c pnpm cy:run --spec <feature>`.
Unit: `CURSOR_DEV=true nix develop -c pnpm frontend:test <changed spec files>`.

## Slices

### 1. On a phone the book uses the screen width
Type: Behavior
Status: done
Proof: `phone_reading.feature` PDF and EPUB width scenarios and the panel
scenario; `book_browsing.feature` and `BookReadingPage.spec.ts` stay green.

Behavior: a book opened at 390×844 → the reader shows it → the closed layout
takes no width, PDF pages and EPUB text use the screen width, and the reading
panel for the current block is fully visible and can be tapped. Remove the
`position` conflict so the phone branch is really out of the page flow.

### 2. The book layout toggle is reachable on phones and tablets
Type: Behavior
Status: done
Proof: `phone_reading.feature` toggle scenarios at 390×844 and 768×1024 with a
non-forced click.

Behavior: the book reader at 390 or 768 px wide → the reader taps *Book
layout* → the layout opens; the collapsed main menu shows the "Note" item and
does not cover the top bar. Add the book reading route to the "Note" area in
`useNavigationItems`.

### 3. The book layout drawer behaves like the notebook sidebar drawer
Type: Behavior
Status: done
Proof: `phone_reading.feature` choose-block, backdrop, and below-the-menu
scenarios; desktop book features and `note_tree_view.feature` green.

Behavior: the drawer open at 390×844 → the reader chooses a block → the drawer
closes and the book shows that block; or taps the backdrop → the drawer closes
and the place is unchanged. The drawer opens below the main menu. The book
layout uses the notebook sidebar's drawer behavior (breakpoint, placement,
backdrop, close on choice) instead of its own copy; remove the duplicate
breakpoint module if nothing else needs it. The E2E page object's
`chooseBookBlockByTitle` opens the drawer first when the layout is closed.

## Current decisions

- One drawer behavior shared by the notebook sidebar and the book layout (owner
  decision: behave like the notebook sidebar).
- The book reader counts as the "Note" area of the main menu. Other routes
  without an active item still show the wider "Menu" fallback; that is outside
  this story.
- PDF on a phone is "not broken" only: no reflow or new zoom.
- EPUB phone proof lives in `phone_reading.feature`, not the `@ignore`d
  `epub_book.feature`.

## Learnings

- Slice 1 (done): moving `relative` to the desktop-open branch of
  `BookReadingBookLayout.vue` lets the phone branch's `fixed` win. Accepted
  proof: `phone_reading.feature` 3/3 (the PDF viewer and EPUB
  `.epub-container` are at least 90 % of the window width; without the fix
  they are 102 px), `book_browsing.feature` 5/5, `reorganize_layout.feature`
  8/8, `BookReadingBookLayout.spec.ts` and `BookReadingPage.spec.ts` 14/14,
  and `vue-tsc` clean. The rendered PDF page itself is 334 of 390 px because
  pdf.js fits the page width inside the viewer's padding and scrollbar, so
  the proof measures the viewer.
- The panel scenario uses the block selected when the book opens ("Code
  Refactoring"), not "2.1 …": the desktop-tuned within-page scroll steps land
  on "2.2 …" at 390 px, and choosing a block through the layout on a phone
  needs slice 3's drawer. That scenario passes without the fix; the width
  scenarios are what catch the bug.
- The drag-to-indent pointer handling moved to
  `useBookLayoutBlockPointerDrag` so the layout component stays under the
  file-size limit. Slice 3 removes the component's drawer copy.
- `bookReadingShared.ts` has `expectUsesScreenWidth` and
  `expectFullyOnScreen`; slice 3 can reuse them for the below-the-menu check.
- Slice 2 (done): `bookReading` joined the "Note" item's active routes in
  `useNavigationItems`. Accepted proof: `phone_reading.feature` 5/5 (both new
  toggle scenarios failed with "being covered by another element" before the
  fix), `book_browsing.feature` 5/5, `MainMenu.spec.ts` `bookReading` row
  (red before the fix), `tests/toolbars/` 80/80, `vue-tsc` clean.
- At 768 px the layout starts open and uses the desktop (in-flow) layout, since
  the breakpoint counts 768 as desktop. The tablet scenario therefore closes and
  reopens it. Slice 3 must check that `useNotebookSidebarDrawer` uses the same
  `>= 768` rule, or change it on purpose.
- Page object: `clickBookLayoutToggle` (a real, non-forced click),
  `expectBookLayoutOpen` and `expectBookLayoutClosed`. The closed phone aside
  is only moved off-screen, so Cypress's `be.visible` may still pass; "closed"
  checks rely on `aria-expanded` or position.
- Slice 3 (done): `useSidebarDrawer` (768 px, `>= 768` is desktop, open state
  decided in setup) and `SidebarDrawer.vue` (backdrop and aside below the main
  menu) are the one drawer behavior. `useNotebookSidebarDrawer` wraps it for
  the notebook sidebar; the book layout closes on choosing a block below
  768 px. `bookReadingLayoutBreakpoint.ts` and its spec are deleted. Product
  code is 51 lines smaller. Accepted proof: `phone_reading.feature` 7/7 (the
  panel scenario now uses "2.1 …" chosen through the drawer),
  `book_browsing` 5/5, `reading_record` 6/6, `reorganize_layout` 8/8,
  `note_tree_view` 6/6, 388 unit tests, `vue-tsc` clean. No red check for the
  new drawer scenarios; the tap-outside scenario would also pass on the old
  code (its backdrop closed the drawer too).
- On a phone the book drawer now starts below the main menu and its backdrop
  no longer covers the menu, matching the notebook drawer.
- `BookReadingContent.vue` is 453 lines (was 474), over the 250-line
  file-size guideline; splitting its PDF reading orchestration is outside this
  story.

## Execution complete

Product advice: no change to backlog priorities. Phone reading now works for
PDF and EPUB, and the book layout shares the notebook sidebar's drawer
(`SidebarDrawer`). Later SEED-059 stories that touch the layout (story 7 hand
fixes, story 9 keyboard, story 10 EPUB tools) should build on that shared
drawer rather than add layout-specific open/close rules. EPUB close-on-choice
goes through the same layout component as PDF but has no phone E2E of its own;
add one only if a later EPUB story changes that path. `BookReadingContent.vue`
(453 lines) is over the file-size guideline; split it when a story next
changes its PDF reading orchestration, not as a separate story.
