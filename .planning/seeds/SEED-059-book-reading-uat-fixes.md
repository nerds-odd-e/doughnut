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

<a id="story-1"></a>

### Land on and track the chosen place in an EPUB

**Identity:** SEED-059#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/049-epub-land-and-track-chosen-place/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"d61c0b22c3c154ada0410336b7a2d57bde30f3111cfab6dac5ac91214b82906f","plan":"22a042d813b2336cb680bf9a81ed07d7854d545a54de6f6b81c0211aa71ab6c1"}}
```

**Goal:** An EPUB reader lands where they chose, and the content shown, the
current block, and the selection agree, from the first open onwards. This lets
readers trust EPUB navigation and reading records, which reading a book end to
end needs.

**Scope**

- **Choosing a block lands on it.** Choosing a chapter or section in the layout,
  or following a link inside the book, puts that place's start at the top of the
  view at any viewport size.
- **Every block has a place to go** (owner decision, 2026-09-29). A block with no
  content of its own goes to the target its entry has in the book's own table of
  contents. The chosen block stays selected and current, even when another block
  starts at the same spot (Origin: the book title and the detailed contents).
- **First open makes no record.** A newly attached EPUB opens with the current
  block matching what is shown, and no block is marked.
- **Reopening shows the current block.** Reopening a book resumes at the last
  place, with the current block visible in the layout, as PDF already does.
- **Deferred:** blocks with no anchor in books attached before this fix keep
  today's behavior (owner decision, 2026-09-29); re-attaching the book gives the
  fix. Chapter landing, first open, and reopening are promised for every book.
  Changing or clearing a wrong record already made (story 6) and the EPUB
  current-block bar (story 10) are separate stories.
- **Must keep working:** the current block follows scrolling, Read/Skim/Skip save
  and persist, resume works, and PDF behavior is unchanged.

**Key examples**

- In *Origin of Species* at 1440×900, choose "CHAPTER IV. NATURAL SELECTION."
  (today 6,416 px too far) → the heading is at the top of the view. The same at
  1280×560, and for Alice chapters III and XII.
- In Origin's contents page, follow the "CHAPTER 2" link → chapter II's heading
  is at the top (today it lands near the end of chapter II).
- Choose "ON THE ORIGIN OF SPECIES." → the book shows the title, and the title
  block stays selected and current (today "DETEAILED CONTENTS…" is selected).
- In Alice, choose "THE FULL PROJECT GUTENBERG™ LICENSE" → the book shows the
  licence's start (today the content does not move).
- Attach Alice on the web and open it → the cover is shown, the current block is
  the one holding the cover, and no block is marked (today "Contents" is current
  and marked read).
- Read part of Origin, then reopen the book at 1280×560 → the book resumes at
  the last place, and the layout shows the current block (today it stays at the
  layout's top).

**UAT evidence:** defects 1, 7, 8, and 16 in the report linked under
*Why This Matters*. Unverified hypothesis for defect 1: the offset grows with
the previous chapter's length, so the scroll target may be computed before
epub.js inserts the previous section above the chosen one. Defect 8 happens when
an entry gets no content at attach time (two entries starting at the same spot,
or its text counted under another entry), so it has no stored start.

**Effort hypothesis:** M–L, medium confidence.
**Depends on:** none.

<a id="story-2"></a>

### Read a book on a phone

**Identity:** SEED-059#story-2
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** A reader on a phone-width screen can read a PDF or EPUB and open the
book layout as a drawer.

**Observed defect (defect 2, High)**

- At 390×844 the collapsed layout panel is moved off screen (x = −288) but still
  takes its 288 px, so the content gets a 102 px column. PDF pages render as
  thumbnails, EPUB shows one or two words per line, and the "Now reading" bar and
  the Read/Skim/Skip panel are cut off.
- The *Book layout* toggle (x 88–130) sits under the main menu's *Toggle menu*
  button, which takes the click, so the layout cannot be opened. At 768×1024 the
  toggle is also covered. At 1024×768 and above it works.
- Reproduction: open a book's reader (`/notebooks/<id>/book`) at 390×844.

**Key examples**

- At 390×844 the content uses the screen width and the reading panel is fully
  visible.
- The layout toggle opens the layout as a drawer at 390 and 768 px wide.

**Effort hypothesis:** M, medium confidence. Open question: should the drawer
behave like the notebook sidebar drawer?
**Depends on:** none.

<a id="story-3"></a>

### Get a PDF book layout that matches the book's headings and nesting

**Identity:** SEED-059#story-3
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** A PDF attached through the CLI gets one block per real heading, nested
as in the book, so readers do not have to clean up the layout before reading.

**Observed defects**

- **Duplicates from running page headers (defect 5, High).** Think Python's 361
  blocks contain all 218 numbered sections plus 75 duplicates from page headers
  (for example "14.6. Databases" next to "14.6 Databases"), 3 "Contents" and 6
  "Index" blocks, and 16 separate "Chapter N"/"Appendix A" label blocks. Choosing
  a duplicate lands on the next page's header; the current block and "Now
  reading" pass through duplicates while scrolling, and they are auto-marked read
  because they have no text of their own.
- **Flat or wrong nesting (defect 6, Medium).** Attention: every block from
  "Abstract" to "References", including 3.2.1, sits at one depth, so "3 Model
  Architecture" does not contain its sections. Think Python: only two depths; 6
  of 21 chapter titles (Strings, Lists, Tuples, Inheritance, The Goodies,
  Debugging) and 5 label blocks sit under the previous chapter; subsections such
  as A.2.1 sit beside A.2. Hypothesis (not verified): running the MinerU outline
  script alone gave a nested outline for the arXiv paper, so nesting may be lost
  in Donut after extraction.
- Reproduction: `/attach thinkpython2.pdf` in the CLI, open the reader, and
  compare with the book's contents pages.

**Key examples**

- Think Python's layout has one block per contents entry, with chapters at the
  top and sections and subsections nested under them; no page-header duplicates,
  no repeated "Contents"/"Index".
- The Attention paper arrives nested: 3 → 3.2 → 3.2.1.

**Effort hypothesis:** L (M if nesting is lost in Donut after MinerU), low
confidence. Open question: should the PDF's own bookmarks be used when present?
**Depends on:** none.

<a id="story-4"></a>

### Reorganize a full-size book with AI

**Identity:** SEED-059#story-4
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** *AI Reorganize* gives a preview for a book of real size, and when it
cannot, the reader gets a plain explanation and keeps their place.

**Observed defect (defect 10, High)**

- On Think Python (361 blocks), 3 of 3 runs failed with a server error after
  7.5–8.3 s. A red message filled a third of the screen with raw text ("Error
  parsing JSON: {"blocks":[{"depth":0,"id":44}, …") that stops mid-entry at
  block id 130, then disappeared. No preview. In 2 runs the layout also scrolled
  back to its top, leaving the current block out of view.
- The 27-block paper works (preview in 3–6 s).
- Hypothesis (not verified): the model's answer is cut off by an output size
  limit, so books above roughly 90 blocks would fail. No book between 27 and 361
  blocks was tried.

**Key examples**

- *AI Reorganize* on Think Python shows a preview that can be confirmed.
- If the request still fails, the message says so in plain words, and the
  layout keeps the current block in view.

**Effort hypothesis:** M–L, low confidence. Open question: split the book into
parts, or ask the AI only for the changes?
**Depends on:** none; story 3 reduces how much the AI has to fix.

<a id="story-5"></a>

### Scroll a PDF smoothly right after choosing a block

**Identity:** SEED-059#story-5
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** Scrolling a PDF moves in the reader's direction from the first wheel
step, wherever the pointer rests.

**Observed defects**

- **Backward jumps (defect 3, Medium).** In the first second after choosing a
  block, 400 px wheel steps at 120 ms gave `+400, +400, −156, +400, −800, +400…`
  (Think Python 8.1, three of three runs), and similar on 14.6 and the Attention
  paper. Reproduction: in Think Python choose "8.1 A string is a sequence" and
  wheel down steadily.
- **The Reading Control Panel stops wheel scrolling (defect 4, Medium).** The PDF
  panel (Read/Skim/Skip) appears mid-view (y 437–485 at 1440×900); when it comes
  to rest under the pointer, every wheel step is swallowed (0 px for 12 steps,
  still 0 after 3 s). The EPUB panel, docked at the bottom, passes wheel events
  through.

**Key examples**

- Choosing a block, then wheeling down steadily, moves down on every step.
- With the pointer at the centre of the PDF, wheeling keeps scrolling when the
  panel passes under it.

**Effort hypothesis:** M, medium confidence.
**Depends on:** none.

<a id="story-6"></a>

### Keep reading records right and let readers correct them

**Identity:** SEED-059#story-6
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** Marking continues through chapter labels, and a reader can change or
clear a mark that is wrong.

**Observed defect and gap**

- **Heading-only block selected (defect 15, Medium).** After choosing "Chapter
  9", "Chapter 10" or "Chapter 12" in Think Python, the Reading Control Panel
  stays hidden for 3–4 pages; the current block goes from the label straight to
  x.1, skipping the chapter title block ("Case study: word play", "Lists"), which
  has introduction text. Neither is marked. Choosing the title block instead
  marks the label read at once, as promised.
- **Marks cannot be changed or cleared.** Choosing a marked block makes the
  panel offer the next block, and no other control exists. A mistaken Skim, the
  spurious "Contents" record from story 1's first-open defect, and auto-marked
  header duplicates from story 3 stay for good. Cancelling the block clears its
  record, but only in PDF.

**Key examples**

- Choose "Chapter 12" in Think Python and scroll → the panel is offered, and the
  chapter title block becomes current and can be marked.
- Choose a block marked Skimmed → the reader can change it to Read or clear it.

**Effort hypothesis:** M, medium confidence.
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
**Depends on:** story 1 (reliable EPUB positions).

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

## Ordering and Scope Reduction

- **Highest priority** (reading breaks for common books and devices): stories
  1–4.
- **Next** (reading works but is jerky, records go wrong, or correcting is
  slow): stories 5–9.
- **Then** (completing the feature): stories 10–12.

Stories are independent unless stated. Story 3 lowers the cleanup that stories 4
and 7 are needed for; if story 3 is delivered first, re-judge the priority of
story 7. First to drop: story 12, then story 11.

## Open Decisions

- Which open question in each story needs the owner before refinement: for
  example, the drawer behaviour in story 2.

## When to Surface

When the owner selects book reading work from the product backlog.

## Breadcrumbs

- UAT report: `4f2f230505:.planning/seeds/SEED-054-book-reading-uat.md`,
  section `## UAT Findings` (screenshots were not committed).
- Supported behavior: `e2e_test/features/book_reading/`.
- PDF extraction: `cli/python/mineru_book_outline.py`,
  `backend/src/main/java/com/odde/donut/services/book/MineruContentListLayoutBuilder.java`.
