---
id: SEED-059
status: dormant
planted: 2026-09-29
planted_during: owner selection of book reading fixes and improvements found by the two-hour manual UAT (SEED-054)
trigger_when: making reading a book in Donut complete, smooth, and stable
scope: large
---

# SEED-059: Make reading a book in Donut complete, smooth, and stable

## Why This Matters

A manual UAT of the supported book reading feature (2026-09-29, commit
`1f9bdae1bc`) found that core reading journeys break with real books: EPUB
navigation lands in the wrong place, phones cannot read at all, PDF layouts
extracted by MinerU are cluttered and flat, and AI reorganization fails on a
full-size book. Readers also cannot correct wrong layouts or wrong reading
records efficiently. The owner kept findings whose impact is real or that would
become technical debt, grouped them by similarity and priority into the stories
below, and left the rest out.

The full report (defect measurements, reproduction steps, evidence file names)
is recoverable at `4f2f230505:.planning/seeds/SEED-054-book-reading-uat.md`,
section `## UAT Findings`; "defect N" below refers to its numbering.

## Context

- **Books used:**
  - *Think Python 2e*, a 244-page PDF with 21 chapters and appendices, attached
    with the CLI `/attach` and real MinerU (167 s);
  - *Attention Is All You Need*, a 15-page arXiv PDF (MinerU 28 s);
  - Project Gutenberg EPUBs of *Alice's Adventures in Wonderland* and *On the
    Origin of Species*, attached on the web and with the CLI.
- **Environment:** Development stack with real OpenAI; headless Chromium at
  1440×900, 1280×560, and 390×844.
- **What already works and must keep working:**
  - PDF block choice lands on the exact page with the heading at the top.
  - The current block follows scrolling in both formats. At 1280×560 the
    layout keeps the current block visible while scrolling.
  - Read, Skim and Skip save at once and persist, and resume works in both
    formats.
  - PDF indent, outdent (with descendants), cancel, and drag work and persist.
    Records stay consistent through reorganizing.
  - AI reorganization of a 27-block paper previews in 3–6 s, and Cancel and
    Confirm work.
  - The DRM refusal is clear.
- **Findings left out by the owner** (low impact, and no technical debt if left
  unfixed):
  - the CLI runs the whole PDF extraction before refusing a notebook that
    already has a book (defect 9);
  - AI reorganization left 3.2.1–3.2.3 at the depth of 3.2 in one paper
    (defect 17);
  - no progress during MinerU extraction;
  - books named after the file instead of their title;
  - the reading panel and "Now reading" bar covering some book text;
  - the AI preview being hard to check row by row;
  - small visual items: current block at the layout's bottom edge in short
    viewports, a page indicator one page off, unexplained bbox colours, title
    alignment, *Read from here* scrolling back to the block start, the
    new-notebook name field label, the "Now reading" bar after creating a block,
    and white EPUB pages in the dark theme.

## Alternatives and Decision

The owner chose fixes to supported behavior before new capabilities, except
where a capability is the only way to recover from a defect's damage (changing
or clearing a wrong reading record). Stories are ordered by user impact on
reading a book end to end.

## Story Decomposition

Effort bands: S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery.

<a id="story-5"></a>

### Scroll a PDF smoothly right after choosing a block

**Identity:** SEED-059#story-5
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/053-pdf-smooth-scroll-after-choosing-block/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"539e46a16a7b820bf4bc9777266cd200f6c5ecf800bb1e6166388cfa44de8ed7","plan":"591165dbf17b9e072f0fd9ecb222f4d93d6b6aa8cedd90ee592aeb35dab88f4d"}}
```

**Goal:** A PDF reader's view moves only where they scroll it, from the first
wheel step after choosing a block, wherever the pointer rests. Today the start
of every reading run jerks back by up to a screen, and the view can seem frozen,
which makes reading a book in Donut feel broken rather than smooth.

**Scope**

- **Nothing pulls the reader back** (owner decision, 2026-09-29). The
  snap-back reminder is removed: scrolling past an unmarked block into the next
  one no longer moves the view back to it, and no wheel steps are ignored
  afterwards. Cause (read in code, 2026-09-29): snap-back pulls the view back
  up to twice per block and ignores wheel steps for 500 ms after each pull,
  which matches the UAT's two backward jumps per run. The Read/Skim/Skip panel,
  which already appears at the block's end and after scrolling past it, stays
  the only reminder.
- **Any other backward jump in the first second is in scope too.** The promise
  is that each downward wheel step moves the view down, whatever causes a jump.
- **The Reading Control Panel never stops scrolling.** Wheeling with the pointer
  over the PDF panel scrolls the book, as the EPUB panel already allows. Its
  buttons still work.
- **Deferred:** the panel's position, look, and the text it covers; EPUB
  scrolling; keeping the current block at the layout's edge in short viewports.
- **Must keep working:** PDF landing on the exact page with the heading at the
  top, the current block following scrolling, the panel appearing at the
  block's end and after scrolling past it, the panel moving on to the next block
  once the selected one is marked, Read/Skim/Skip and resume, and layout
  editing.

**Key examples**

- In *Think Python*, choose "8.1 A string is a sequence", then wheel down in
  400 px steps every 120 ms → every step moves the view down (today
  `+400, +400, −156, +400, −800, +400…`). The same after choosing 14.6, and 3.1
  in the *Attention* paper.
- Choose a block, scroll past its end into the next block without marking it →
  the view stays where the reader scrolled, and the panel is still offered for
  the chosen block.
- With the pointer resting at the centre of the PDF, wheel down until the panel
  arrives under the pointer, and keep wheeling → the book keeps scrolling
  (today 0 px for 12 steps); clicking *Read* on the panel still marks the block.

**UAT evidence:** defects 3 and 4 in the report linked under *Why This
Matters*. Snap-back has unit tests but no E2E scenario.

**Effort hypothesis:** S–M, medium confidence.
**Depends on:** none.

<a id="story-7"></a>

### Fix a book layout by hand in a few steps

**Identity:** SEED-059#story-7
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** A reader can undo a layout change and move a run of blocks in one
step.

**Observed friction**

- Outdenting makes the following blocks at its old level its children, because
  block order is fixed. One wrong Shift+Tab on "9.1" put the rest of chapter 9
  under it; restoring it took 8 operations.
- Cancelling the "Chapter 12" label moved "Tuples" and all 14 sections to the
  top level; nesting them again took 14 click-and-Tab steps.
- Each click to choose a block also moves the book there.

**Key examples**

- After a wrong outdent, one undo restores the previous layout.
- "Make the following blocks children of this one" nests a run of sections in
  one step.

**Effort hypothesis:** M, medium confidence. Open question: does undo need more
than the last change?
**Depends on:** none.

<a id="story-8"></a>

### Give new and existing blocks short, readable titles

**Identity:** SEED-059#story-8
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** A reader can create a block with a short typed title from any
paragraph, find the *New block* action, and rename a block.

**Observed defect and friction**

- **Paragraph-length titles (defect 12, Medium).** The title prompt appears only
  when the paragraph reaches 512 characters; 200- and 289-character paragraphs
  (Attention 3.2, Think Python 8.3) became titles directly, the 200-character
  one filling seven lines in the layout. No rename control exists.
- **The prompt is awkward.** Its default is the full 512 characters cut
  mid-word, not selected, so typing appends; Enter does not confirm.
- **The action is hidden.** The content boxes that show where a click creates a
  block fade about 2 s after a block is chosen, nothing hints that clicking a
  paragraph offers *New block*, and the callout covers the paragraph's text.
- Reproduction: in the Attention paper, choose "3.2 Attention", click the first
  paragraph, then *New block*.

**Key examples**

- Creating a block from any paragraph asks for a title, with a short selected
  default (the first sentence), and Enter confirms.
- A block with a long title can be renamed.

**Effort hypothesis:** M, medium confidence.
**Depends on:** none.

<a id="story-9"></a>

### Move through the book layout with the keyboard

**Identity:** SEED-059#story-9
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** Keyboard users can move between layout blocks and choose one without
changing the layout, and can indent or outdent the same block repeatedly.

**Observed defects**

- **No keyboard movement (defect 13, Medium).** Tab reaches only the first block
  ("*beginning*"); the next Tab is treated as indent, sends a depth change
  (refused: "Block is already at maximum depth relative to predecessor"), and
  focus drops to the page body. Arrow keys do nothing. On any indentable block, a
  Tab meant to move on changes the saved layout.
- **Focus lost after a depth change (defect 14, Low).** After each Tab or
  Shift+Tab, focus goes to the page body, so the block has to be clicked again
  (which also moves the book) before the next key press.

**Key examples**

- Arrow keys move between blocks, and Enter chooses one.
- After Shift+Tab, a second Shift+Tab moves the same block again.

**Effort hypothesis:** M, medium confidence. Open question: which keys move and
which change depth, if Tab no longer does both?
**Depends on:** none.

<a id="story-10"></a>

### Give EPUB readers the reading and reorganizing tools PDF readers have

**Identity:** SEED-059#story-10
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** EPUB readers get the "Now reading / Read from here / Back to selected"
bar and working reorganizing controls, or at least no controls that do nothing.

**Observed defect and gap**

- **Controls that do nothing (defect 11, Medium).** In both EPUBs, *AI
  Reorganize* sends no request and shows nothing; Tab, Shift+Tab and Backspace
  on a block send nothing and say nothing. So a poor EPUB table of contents
  cannot be improved, and the spurious "Contents" block cannot be cancelled.
  Automated scenarios cover reorganizing only for PDF.
- **No current-block bar.** Once an EPUB reader scrolls on, the only way back to
  the selection is clicking the block again, and the panel keeps targeting the
  old selection.

**Key examples**

- In an EPUB, scrolling past the selection shows the same bar as PDF, and *Back
  to selected* returns to it.
- In an EPUB, indent, outdent, cancel, and *AI Reorganize* act as in PDF; any
  action not supported is not shown.

**Effort hypothesis:** L, low confidence. A first slice can hide the controls
that do nothing. Open question: is creating a block from EPUB text also needed?
**Depends on:** none (reliable EPUB positions are in place).

<a id="story-11"></a>

### Go from attaching a book to reading it without searching

**Identity:** SEED-059#story-11
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** A user can find how to attach a PDF or EPUB and reach the reader in one
step after attaching.

**Observed friction**

- The web's *Attach book…* accepts only `.epub`, with no hint that PDFs go
  through the CLI.
- The CLI's top-level `/help` needs two Enters, does not list `/attach`, and
  says "Not supported" in notebook context; it does not say where to get an
  access token (Account → Manage Access Tokens on the web).
- After a CLI attach, nothing says where to read. On the notebook page the book
  appears as a plain file with *Download* and *Delete* but no *Read*; the ways in
  are Settings → *Read* or an unlabelled icon in the notebook list.

**Key examples**

- The web attach card says how to attach a PDF.
- CLI help lists `/attach` and where to get a token.
- After attaching, the CLI prints the reader link, and the notebook page offers
  *Read* where the book is shown.

**Effort hypothesis:** S–M, medium confidence. Open question: should PDFs also be
attachable on the web?
**Depends on:** none.

<a id="story-12"></a>

### See reading progress through the book

**Identity:** SEED-059#story-12
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** A reader can tell which blocks are read, skimmed, or skipped, how far
through each chapter and the book they are, and what is left at the end.

**Observed friction**

- A record shows only as a thin coloured bar at the layout's right edge (green
  read, orange skimmed, black skipped), with no legend or tooltip; the black bar
  cannot be seen in the dark theme.
- A chapter whose sections are all read shows nothing on the chapter itself.
- After the last block was marked, the panel disappeared; nothing said the book
  was finished or that 19 of 27 blocks were still unmarked.

**Key examples**

- Hovering a mark says what it is, and all marks are visible in both themes.
- A chapter shows how many of its sections are marked.
- After the last block, a summary shows unmarked blocks with a way to jump to
  them.

**Effort hypothesis:** M, medium confidence.
**Depends on:** none.

<a id="story-16"></a>

### The current block moves the same way in PDF as in EPUB

**Identity:** SEED-059#story-16
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"planned","plan":"../slice-plans/058-current-block-same-in-pdf-and-epub/PLAN.md"}
```

**Goal:** A reader sees the current block change at the same moment in PDF and
EPUB, a chosen PDF block lands with its start at the top and stays current, and
marking goes on after choosing a block with no text of its own.

**Scope** (resplit from story 15; awaiting story refinement)

- **One current-block rule for both formats.** EPUB uses "the last block whose
  start is at the top of the view (24 px tolerance), and the chosen block wins
  among blocks sharing that start". PDF uses "the first visible block above the
  middle of the view", with no chosen-block preference, and lands a chosen block
  40 PDF points below its start. Use the EPUB rule for both, in one place, and
  land PDF at the start.
- **Marking goes on after choosing a block with no text of its own** (moved
  from story 6 via story 15; UAT defect 15). A PDF without bookmarks keeps
  MinerU's headings, so a "Chapter N" label can come directly before its title
  block. After choosing such a block and scrolling, each following block
  becomes current in order (the title block is not skipped), the empty block is
  marked read when its successor is entered, and the Reading Control Panel is
  offered for the next unmarked block with text. UAT: choosing "Chapter 9",
  "Chapter 10" or "Chapter 12" in Think Python (before the bookmark layout) hid
  the panel for 3–4 pages, the current block went from the label straight to
  x.1, and neither the label nor the title was marked.

**Key examples**

- Scroll a PDF and an EPUB past a heading → the current block changes when the
  heading reaches the top of the view in both.
- Choose a PDF block → its start is at the top and it is current.
- Choose the first of two PDF headings sharing a start → it stays selected and
  current.
- In a PDF without bookmarks whose layout has "Chapter 12" (no text), then
  "Tuples" (introduction text), then "12.1 Tuples are immutable": choose
  "Chapter 12" and scroll down → "Tuples" becomes current before 12.1,
  "Chapter 12" is marked read, and the panel is offered for "Tuples".

**Effort hypothesis:** M–L, low confidence. Provisional slices:
[plan 058](../slice-plans/058-current-block-same-in-pdf-and-epub/PLAN.md).
**Depends on:** story 5 (it removes snap-back from the same PDF pipeline).
"No text of its own" is `hasNoTextOfItsOwn` in
`frontend/src/lib/book-reading/bookBlockDirectContent.ts` (a PDF heading-only
block, or an EPUB block whose only content is its start anchor); auto-mark
already uses it. Snap-back's `hasDirectContent` (`useBookReadingSnapBack.ts`)
still means "more than one locator" and should use it too.

<a id="story-17"></a>

### Reopen an EPUB at the exact paragraph

**Identity:** SEED-059#story-17
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"planned","plan":"../slice-plans/059-reopen-epub-at-exact-paragraph/PLAN.md"}
```

**Goal:** A reader who stops part-way through a long EPUB block reopens the
book at the same paragraph, as PDF readers already do.

**Scope** (resplit from story 15; awaiting story refinement)

- **Exact EPUB resume.** Today EPUB saves the current block's start, so a
  reader part-way through a long block (Origin chapter IV is over 13,000 px)
  reopens at its heading; PDF saves page and offset. Save epub.js's CFI as an
  optional extra field of the EPUB position and resume from it; the block start
  still drives choosing and the current block. A pixel offset is not enough,
  because text reflows at other widths.

**Key examples**

- Read to the middle of Origin chapter IV, leave, and reopen at a different
  window width → the same paragraph is at the top.

**Effort hypothesis:** M, low confidence (CFI precision in continuous scrolled
mode is unverified). Provisional slices:
[plan 059](../slice-plans/059-reopen-epub-at-exact-paragraph/PLAN.md).
**Depends on:** none.

<a id="story-18"></a>

### Anchor the Reading Control Panel after a one-paragraph EPUB block

**Identity:** SEED-059#story-18
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** A reader of an EPUB block with one paragraph gets the Reading Control
Panel at the end of that paragraph, as they do for blocks with more text.

**Scope** (awaiting story refinement)

- **Anchor at the block's last text, whatever its count.**
  `lastDirectContentLocator` (`bookBlockDirectContent.ts`) returns nothing when
  a block has one locator. In EPUB that locator is often the block's only
  paragraph, so `useReadingPanelAnchor` gives no anchor for a heading plus one
  paragraph. It should agree with `hasNoTextOfItsOwn`: only a block with no
  text of its own has no anchor.
- **Check snap-back too:** `useBookReadingSnapBack.ts` also uses
  `lastDirectContentLocator`; story 16 owns snap-back's other rules.

**Key examples**

- In an EPUB, choose "Part One" (a heading and one paragraph) → the Reading
  Control Panel is anchored at the end of "Opening paragraph for part one."
- A PDF block with a heading and paragraphs still anchors as today.

**Effort hypothesis:** S, low confidence.
**Depends on:** none; coordinate with story 16 on snap-back.

<a id="story-19"></a>

### Give the PDF reading view a designed structure, not just a smaller file

**Identity:** SEED-059#story-19
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/060-designed-structure-for-reading-view/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"9969518ae62bcc71bff0bd055be88b65cfa23b7067766df35effc8b13ce3a412","plan":"0da09d6cdfba248eba95cc6016fed2292a0e7012f372d90f3c17fb9112599f16"}}
```

**Goal:** Developers of later book-reading stories (7, 9, 10, 16, 17, 18)
change one reading concept in the one place that owns it, and a concept that
PDF and EPUB share is wired once for both formats. Readers see no change.
Today `BookReadingContent.vue` has 455 lines, over the 250-line limit. It wires
the PDF viewer, the viewport and current block, selection and snap-back, the
Reading Control Panel anchor, reading records, creating a block from content
with its title dialog, AI reorganize, layout changes, and the last-read
position. `BookReadingEpubView.vue` (218 lines) wires the same bar, book
layout, records, selection, and panel again. Every story that touches reading
adds lines to one of them.

**Scope** (owner request, 2026-09-29)

- **Design first, not a mechanical split.** Owner: it needs architectural
  consideration, not simply splitting it. The design is the whole reading view,
  both formats, as set out under **Architecture**. Splitting
  `BookReadingContent.vue` into files without changing who owns what does not
  meet the goal.
- **Behavior unchanged, in both formats.** The existing book-reading E2E
  features and page specs stay green, and no user-visible change is made. The
  current differences between formats stay as they are:
  - PDF only: layout changes (indent, outdent, cancel, drag), AI reorganize,
    the "Now reading" bar, creating a block from content, snap-back, the page
    and zoom control, and moving the selection to a valid block when the
    layout changes.
  - The two current-block rules (story 16 unifies them).
  - Leaving the book: EPUB sends a pending reading position; PDF drops it.
  - The EPUB panel anchor is updated on window resize.
- **Observable structure outcome.** No file of the reading view is over the
  250-line limit, so the waiver recorded in DD-160 ends.
- **Deferred:** giving EPUB the PDF-only tools (story 10), one current-block
  rule (story 16), and changes to `PdfBookViewer.vue` (400 lines) or
  `EpubBookViewer.vue` other than what the new surface boundary needs. The
  viewer is the format's rendering component and is not part of this story.

**Architecture** (confirmed by the owner, 2026-09-29)

- **One format-neutral reading view** owns everything that does not depend on
  how the book is displayed: the global bar with the book name, the book
  layout panel, reading records and the mark control, selection and the
  Reading Control Panel, the current block and its live announcement, saving
  the last-read position, the "Now reading" bar, and layout changes with AI
  reorganize.
- **One reading surface per format** (PDF and EPUB) sits in its main area and
  owns only what depends on the format: its viewer, showing a chosen block,
  turning a view change into a current-block candidate, the locator saved as
  the reading position, the geometry that anchors the panel, and anything that
  only the format has (PDF page and zoom control, creating a block from
  content, snap-back). The existing `BookReaderViewerRef` geometry contract is
  the starting point for this surface contract.
- **Format differences are explicit capabilities of the surface**, not
  duplicated wiring. For example, "the reader can reorganize the layout" is on
  for PDF and off for EPUB. Story 10 turns it on for EPUB instead of wiring
  reorganizing again.
- `BookReader.vue` keeps loading the book and choosing the surface by format.
- The existing composables (`useBookReadingSelection`,
  `useBookReadingCurrentBlock`, `useReadingPanelAnchor`,
  `useBookReadingSnapBack`, `useBookLayoutMutations`,
  `useBookLayoutAiReorganize`) are reused under these owners. Their
  format-switch options (for example `overrideBlockAwaitingConfirmation` and
  `repairSelectionWhenBlocksChange`) are replaced by the surface's
  capabilities where that makes them simpler.

**Key examples**

- Story 16 changes how the current block moves in PDF → it edits the
  current-block owner and the PDF surface's candidate, not the reading view's
  wiring.
- Story 10 gives EPUB readers AI reorganize → it turns on a capability for the
  EPUB surface, and the reading view already wires AI reorganize once.
- Story 17 saves an EPUB CFI → only the EPUB surface's reading-position
  locator changes.
- A reader marks a block Read in a PDF and in an EPUB → the same wiring saves
  the record in both, and each format looks and behaves as it does today.
- In an EPUB, Tab on a block still sends nothing (defect 11 stays for story
  10).

**Effort hypothesis:** M–L, low confidence; the design moves the wiring of
two views into one, not only one file. Slices:
[plan 060](../slice-plans/060-designed-structure-for-reading-view/PLAN.md).
**Depends on:** none. Placed first so later book-reading stories build on it.
**Evidence:** DD-160 (the file-size check was waived for
`BookReadingContent.vue` in SEED-059#story-2), and SEED-059#story-6 (change or
clear a reading mark) added one binding line to each view in two slices while
leaving the PDF view over the limit.

## Ordering and Scope Reduction

- **Highest priority** (reading breaks for common books and devices):
  delivered.
- **Next** (reading works but is jerky, records go wrong, or correcting is
  slow): stories 5 and 7–9.
- **Then** (completing the feature): stories 10–12.

Stories are independent unless stated. The bookmark-based PDF layout (delivered) lowers
the cleanup that stories 4 and 7 are needed for; re-judge the priority of story
7. First to drop: story 12, then story 11.

## Open Decisions

- Which open question in each story needs the owner before refinement.

## When to Surface

When the owner selects book reading work from the product backlog.

## Breadcrumbs

- UAT report: `4f2f230505:.planning/seeds/SEED-054-book-reading-uat.md`,
  section `## UAT Findings` (screenshots were not committed).
- Supported behavior: `e2e_test/features/book_reading/`.
- PDF extraction: `cli/python/mineru_book_outline.py`,
  `backend/src/main/java/com/odde/donut/services/book/MineruContentListLayoutBuilder.java`.
