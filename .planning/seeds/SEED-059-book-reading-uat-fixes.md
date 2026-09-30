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

**Design note:** reorganizing is the reading surface's `reorganize` capability
(`useBookReadingSession`); turning it on for EPUB wires the book layout's
reorganize listeners and the AI preview dialog. The "Now reading" bar sits only
in `BookReadingShell`'s PDF pane layout, so showing it in EPUB means giving EPUB
that pane layout (a DOM change), which also lets the shell declare the Reading
Control Panel once.

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
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/058-current-block-same-in-pdf-and-epub/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"b498a207d1d1ab66ce330471ccea01d5aa033e659b382c2c6cbcf98a3b2c96e1","plan":"6d5ff634f7248441c78b9da7ece5f8b1754094a55cbf59a5680ee70acd0e6e1e"}}
```

**Goal:** A reader sees the current block change at the same moment in PDF and
EPUB, a chosen PDF block lands with its start at the top and stays current, and
marking goes on after choosing a block with no text of its own.

**Scope**

- **One current-block rule for both formats.** EPUB uses "the last block whose
  start is at the top of the view (24 px tolerance), and the chosen block wins
  among blocks sharing that start". PDF uses "the first visible block above the
  middle of the view", with no chosen-block preference, and lands a chosen block
  40 PDF points below its start. Use the EPUB rule for both, in one place, and
  land PDF at the start.
  - Where the view cannot bring the start to the top (the first or last page),
    the chosen block still wins, as it does in EPUB today.
  - The old PDF rule is removed, not kept beside the new one.
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
  - "No text of its own" is `hasNoTextOfItsOwn`
    (`frontend/src/lib/book-reading/bookBlockDirectContent.ts`; a PDF
    heading-only block, or an EPUB block whose only content is its start
    anchor). Auto-mark already uses it; the reading panel target's
    `hasDirectContent` (`useReadingPanelTarget.ts`, today "more than one
    locator") should use it too.
- **Boundary with story 18:** this story owns which block the panel is offered
  for; story 18 owns where the panel is anchored for a one-paragraph EPUB
  block. Neither changes the other's rule.
- **Deferred:** no change to the reading position saved on scroll, the page
  indicator, or EPUB behavior beyond sharing the rule; no new UI.

**Key examples**

- Scroll a PDF and an EPUB past a heading → the current block changes when the
  heading reaches the top of the view in both (a heading lower on the page is
  not yet current, so the block above stays current).
- Choose a PDF block → its start is at the top and it is current.
- Choose the first of two PDF headings sharing a start (a parent and child
  bookmark at one destination) → it stays selected and current.
- Choose a PDF block on the first page, whose start cannot reach the top → it
  is still current.
- In a PDF without bookmarks whose layout has "Chapter 12" (no text), then
  "Tuples" (introduction text), then "12.1 Tuples are immutable": choose
  "Chapter 12" and scroll down → "Tuples" becomes current before 12.1,
  "Chapter 12" is marked read, and the panel is offered for "Tuples".

**Effort hypothesis:** M–L, low confidence. Provisional slices:
[plan 058](../slice-plans/058-current-block-same-in-pdf-and-epub/PLAN.md).
**Depends on:** none (story 5 removed snap-back and the hold-window gate).

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
- **Check the panel target too:** `useReadingPanelTarget.ts` also uses
  `lastDirectContentLocator`; story 16 owns its other rules.

**Key examples**

- In an EPUB, choose "Part One" (a heading and one paragraph) → the Reading
  Control Panel is anchored at the end of "Opening paragraph for part one."
- A PDF block with a heading and paragraphs still anchors as today.

**Effort hypothesis:** S, low confidence.
**Depends on:** none; coordinate with story 16 on the reading panel target.

<a id="story-21"></a>

### Book reading E2E scenarios stay stable at any window size

**Identity:** SEED-059#story-21
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** A Donut developer can trust the book reading E2E scenarios to pass or
fail because of reading behavior, not because of the browser window's size.

**Observed friction**

- The step "the book reader PDF viewport should be on page N" reads the page
  indicator, which shows the page under the middle of the reader. After a PDF
  block lands at its start, the middle can sit within pixels of a page boundary.
  In `phone_reading.feature` (390 × 844) block 2.2's start lands 1.5 px from
  the page 2/3 boundary; CI (run 36647846005) failed at a slightly different
  window, and the repair reproduced the failure at 390 × 900. It was repaired
  there by asserting the start position instead.
- `book_browsing.feature` (lines about 22, 38, 45, 73, 76) and
  `reading_record.feature` (line about 42) use the same step after choosing a
  block. They pass at their desktop window size. Whether they, or other book
  reading steps that read the middle of the reader, would fail at other sizes
  is unverified.

**Scope** (awaiting story refinement)

- **Analysis first.** Confirm which scenarios and steps depend on window size or
  on reader-middle geometry: run the book reading features at several window
  sizes (phone, laptop, tall and short desktop), and list every step that reads
  layout geometry (page indicator, scroll offsets, pixel tolerances such as the
  15 px start-at-top check). Report which fail, at which sizes, and why. If none
  can fail, stop with that finding and change no tests.
- **Careful test design.** Decide, before changing tests, how book reading E2E
  steps should express position: what a scenario asserts (which block is
  current, where a block's start is, which page holds it), which step owns each
  kind of check, and how tolerances are chosen and documented. Fit the
  e2e-authoring conventions and the test guidance on whole-suite coverage and
  cost.
- **Then repair** only the steps and scenarios the analysis confirms, keeping
  each scenario's journey and its integration proof.
- **Deferred:** no product change, and no change to the page indicator itself.

**Key examples**

- A scenario that chooses a PDF block asserts where the block's start is, and
  passes at 390 × 844 and at 390 × 900.
- A scenario that scrolls past a heading asserts the current block, and does not
  depend on which page holds the middle of the reader.
- The analysis states the sizes tried and which step failed at which size.

**Open questions:** which window sizes count as supported for E2E; whether the
page indicator step keeps a role for page-1 and page-boundary-free cases.

**Effort hypothesis:** M, low confidence (the analysis decides).
**Depends on:** none.

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
