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

<a id="story-9"></a>

### Move through the book layout with the keyboard

**Identity:** SEED-059#story-9
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/062-book-layout-keyboard-navigation/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"7c8bc055cbc6000a861731a40df768adb4f9d66c3b0ac0ffaea48080ed964680","plan":"8b357dd037961318aff9ba15e9462d3f291b7c6bacb6fe14eaff974d1937f8f2"}}
```

**Goal:** A reader who uses the keyboard can move through the book layout,
choose a block, and leave the layout with Tab, without changing the saved layout
by accident, and can still indent or outdent the same block repeatedly.

**Why it matters:** Today Tab and Shift+Tab on every layout row are taken for
indent and outdent, so the keyboard cannot leave the layout: each attempt to
move on changes the saved layout or is refused. That is a keyboard trap, which
is the main reason for this story. Hand-editing the layout is now rare, so the
story stays narrow.

**Observed defects** (UAT defects 13, Medium, and 14, Low)

- **No keyboard movement.** Tab reaches only the first block ("*beginning*"); the
  next Tab is treated as indent, sends a depth change (refused: "Block is
  already at maximum depth relative to predecessor"), and focus drops to the
  page body. Arrow keys do nothing. On any indentable block, a Tab meant to move
  on changes the saved layout.
- **Focus lost after a depth change.** After each Tab or Shift+Tab, focus goes
  to the page body, so the block has to be clicked again (which also moves the
  book) before the next key press. Probable cause, not yet observed: the
  blocking "Updating book layout…" modal takes focus and it is not restored.

**Scope**

- **Tab leaves the layout.** Tab and Shift+Tab move focus natively and never
  change the layout. The layout is one tab stop, so Tab from a row leaves it and
  Shift+Tab from outside comes back to the row last focused (else the selected
  block, else the current block, else the first).
- **Arrows move, Enter chooses.** ArrowUp and ArrowDown move focus to the
  previous or next row, stopping at the first and last. They do not choose, move
  the book, or change the panel. Enter or Space chooses the focused block, as a
  click does today.
- **New depth keys.** Alt+Shift+ArrowRight indents and Alt+Shift+ArrowLeft
  outdents the focused block, replacing Tab and Shift+Tab, with the same effect
  and refusals as today. Alt+ArrowLeft and Cmd+[ were avoided because they are
  browser Back on some systems (not verified).
- **Focus stays on the block after a depth change.** The block that was indented
  or outdented keeps keyboard focus, so the same key can be pressed again.
- **Formats:** PDF and EPUB share the layout, so movement and choosing work in
  both. The depth keys act only where reorganizing exists today (PDF); in EPUB
  they do nothing, as Tab does today, until story 10.
- **Deferred (not committed here):**
  - Backspace or Delete still cancels the focused block at once and cannot be
    undone (story 7 defers undoing Cancel). With arrows, a reader is one key
    from it; left unchanged, a known risk.
  - Home, End, PageUp, PageDown and type-ahead.
  - Tree semantics (`role=tree`, `aria-level`, expand and collapse).
  - Ctrl/Cmd+Z (story 7), drag, phones, the mark control.
  - Focus after Cancel or AI reorganize.

**Key examples**

- Focus a block and press Tab → focus leaves the layout and the layout is
  unchanged. Shift+Tab from outside → focus returns to the same row.
- ArrowDown on "3.1" → "3.2" is focused; the book has not moved. Enter → "3.2"
  is chosen and the book moves.
- Alt+Shift+ArrowLeft on "Chapter A" (depth 1), then Alt+Shift+ArrowLeft again
  → the same block is at depth 0, with no click between.
- Alt+Shift+ArrowRight on the first block → refused as today.

**Effort hypothesis:** S–M, medium confidence. Three Behavior slices; the plan is
[plan 062](../slice-plans/062-book-layout-keyboard-navigation/PLAN.md).
**Depends on:** none (story 7's Undo and Ctrl/Cmd+Z are on trunk).

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

<a id="story-18"></a>

### Anchor the Reading Control Panel after a one-paragraph EPUB block

**Identity:** SEED-059#story-18
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/060-panel-after-one-paragraph-epub-block/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"51f8bee2c64fccd3a70733ded508ffd0e9606f70fccafdeb958d1888f2e195fc","plan":"6dad6b064ec82775bfb26f28ac75163c60498af7c833582d34d316a54d97f0db"}}
```

**Goal:** A reader of an EPUB block with one paragraph gets the Reading Control
Panel, placed after that paragraph, as they do for blocks with more text.

**Why it matters:** Story 16's delivery makes the panel target treat a
one-paragraph EPUB block as having text, so the panel is offered only once the
block's last text is on screen. That position is measured from
`lastDirectContentLocator`, which returns nothing for a block with one locator.
The panel therefore never appears for such a block. Before story 16 it appeared
when the next block became current.

**Scope**

- **One rule for "the block's last text".** `lastDirectContentLocator`
  (`bookBlockDirectContent.ts`) returns nothing only for a block with no text of
  its own (`hasNoTextOfItsOwn`), and otherwise the block's last locator, so a
  block with one locator that is its only paragraph has one. Panel target and
  panel anchor both use it, so both work with no separate change. PDF blocks
  behave as today.
- **Deferred: precise placement.** The panel is placed at the bottom of what
  that locator resolves to: its element when it has an id, otherwise the whole
  spine document. For an EPUB block whose last paragraph has no id, the panel
  may sit under the block's heading, or at the end of the spine file when the
  file holds several blocks. This is accepted for now, and it holds for
  multi-paragraph blocks today too. Placing the panel at the real end of a
  block's text is a separate story, not yet queued.

**Key examples**

- In an EPUB, choose "Part One" (a heading and one paragraph, in its own file)
  → the Reading Control Panel is shown, anchored beneath "Opening paragraph for
  part one."
- A block with a heading and several paragraphs, in either format, is offered
  and anchored as today.
- A PDF or EPUB block with no text of its own still gets no anchor.

**Evidence (2026-09-30):** In *Alice's Adventures in Wonderland* and *On the
Origin of Species* (Project Gutenberg EPUBs), no block has one paragraph; the
few short blocks are front matter with none. No paragraph in either has an id,
and most blocks are one per spine file. One-paragraph blocks are therefore not
seen in these two books; the story is kept at the owner's decision, for books
with short sections.

**Effort hypothesis:** S, high confidence (one function and one E2E scenario).
**Depends on:** none (story 16's target rule is on trunk).

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
  slow): stories 5 and 9.
- **Then** (completing the feature): stories 10–12.

Stories are independent unless stated. First to drop: story 12, then story 11.

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
