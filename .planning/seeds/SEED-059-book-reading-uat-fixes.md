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

<a id="story-6"></a>

### Let readers change or clear a reading mark

**Identity:** SEED-059#story-6
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/056-change-or-clear-reading-mark/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"7890add75c01c9427e347cdffbd0fa46f990fcef261e47862fd96096523152c3","plan":"46254747e103fd019fc6a7510694643d2e5529f9df094e1e53182af68cd0b837"}}
```

**Goal:** A reader can change or clear a reading mark that is wrong. Today a
mistaken Skim, a spurious record, or an auto-marked header copy stays for good,
so the reading record, the point of reading a book in Donut, cannot be
trusted.

**Scope**

- **A mark can be changed or cleared on the block in the layout** (owner
  decision, 2026-09-29). Clicking the mark of the chosen, marked block offers
  Read, Skimmed, Skipped, and Clear mark. The choice saves at once and
  persists. A cleared block is unmarked, so the panel treats it as any unmarked
  block. It works the same in PDF and EPUB, because both share the layout; this
  also clears the spurious "Contents" record in EPUBs attached before the
  first-open fix, and auto-marked header duplicates in old layouts.
- **Moved to story 15** (owner decision, 2026-09-29): marking going on after
  choosing a block with no text of its own (UAT defect 15). Story 15 changes
  the current-block rule and what counts as "no text of its own", which that
  fix depends on.
- **Must keep working:** Read/Skim/Skip on the panel, the panel moving on to
  the next block once the selected one is marked, and offering the next block
  when a marked block is chosen; resume; cancelling a PDF block clears its
  record.
- **Deferred:** a legend, tooltips, dark-theme colours, and progress on
  chapters (story 12); marking or clearing several blocks at once; undoing a
  change of mark (choosing again corrects it).

**Key examples**

- Choose a block marked Skimmed, click its mark, choose Read → the layout shows
  it read, and it stays read after reopening the book.
- In an EPUB attached before the first-open fix, choose "Contents", click its
  mark, choose Clear mark → it is unmarked after reopening.
- Choose a read block without touching its mark → the panel still offers the
  next unmarked block, as today.

**UAT evidence:** the improvement "A reading record cannot be changed or
removed" in the report linked under *Why This Matters*.

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

<a id="story-15"></a>

### Resume exactly and track the current block and records the same way in EPUB and PDF

**Identity:** SEED-059#story-15
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/057-exact-resume-one-current-block-rule/PLAN.md","assessment":"not-ready","reasons":["Four independent outcomes in one story (A auto-mark, B one current-block rule, C marking after an empty block, D exact EPUB resume); nine slices with different prerequisites","B and C depend on story 5 (snap-back removal) and D on story 14 (resume-test cleanup) being delivered first","B3 is over the slice target (about 15 min)","Unverified: CFI precision in continuous scrolled mode (D1 probe) and a no-bookmark fixture for defect 15 (C1)"],"basis":{"document":"0d4b36c2c9f00bf9c3ccddfdb18c942b7ba2303d9aaf5d65311fc09e1a38ff5f","plan":"e0304f4e6eaf8c0632add25bd0678d97c13b5ce553022f1744891b579f804c54"}}
```

**Goal:** A reader reopens a book exactly where they stopped, sees the current
block move the same way in EPUB and PDF, and gets a record only for blocks they
read or chose to mark, so reading positions and records can be trusted.

**Scope** (owner decision, 2026-09-29, from the SEED-059#story-1 retrospective)

- **Exact EPUB resume.** Today EPUB saves the current block's start, so a
  reader part-way through a long block (Origin chapter IV is over 13,000 px)
  reopens at its heading; PDF saves page and offset. Save epub.js's CFI as an
  optional extra field of the EPUB position and resume from it; the block start
  still drives choosing and the current block. A pixel offset is not enough,
  because text reflows at other widths.
- **One current-block rule for both formats.** EPUB uses "the last block whose
  start is at the top of the view (24 px tolerance), and the chosen block wins
  among blocks sharing that start". PDF uses "the first visible block above the
  middle of the view", with no chosen-block preference. Use the EPUB rule for
  both, in one place: both formats now land a chosen block at the top, and PDFs
  can have two headings on one page.
- **Marking goes on after choosing a block with no text of its own** (moved
  from story 6, owner decision 2026-09-29; UAT defect 15). A PDF without
  bookmarks keeps MinerU's headings, so a "Chapter N" label can come directly
  before its title block. After choosing such a block and scrolling, each
  following block becomes current in order (the title block is not skipped),
  the empty block is marked read when its successor is entered, and the Reading
  Control Panel is offered for the next unmarked block with text. UAT: choosing
  "Chapter 9", "Chapter 10" or "Chapter 12" in Think Python (before the bookmark
  layout) hid the panel for 3–4 pages, the current block went from the label
  straight to x.1, and neither the label nor the title was marked.
- **Auto-mark only blocks with no text of their own.**
  `useAutoMarkNoDirectContentPredecessor` treats "exactly one locator" as "no
  direct content", so leaving a one-paragraph block unread marks it read. Test
  instead whether the block's only payload is a start anchor (EPUB) or it holds
  only a heading (PDF).

**Key examples**

- Read to the middle of Origin chapter IV, leave, and reopen at a different
  window width → the same paragraph is at the top.
- Scroll a PDF and an EPUB past a heading → the current block changes when the
  heading reaches the top of the view in both.
- Choose the first of two PDF headings on one page → it stays selected and
  current.
- Move from a one-paragraph block to the next without reading it → it is not
  marked read; a heading-only block still is.
- In a PDF without bookmarks whose layout has "Chapter 12" (no text), then
  "Tuples" (introduction text), then "12.1 Tuples are immutable": choose
  "Chapter 12" and scroll down → "Tuples" becomes current before 12.1,
  "Chapter 12" is marked read, and the panel is offered for "Tuples".

**Effort hypothesis:** L, low confidence. The parts are independent and
may be split into separate stories at refinement.
**Depends on:** none. Holds the marking-after-an-empty-block part moved from
story 6.

## Ordering and Scope Reduction

- **Highest priority** (reading breaks for common books and devices):
  delivered.
- **Next** (reading works but is jerky, records go wrong, or correcting is
  slow): stories 5–9.
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
