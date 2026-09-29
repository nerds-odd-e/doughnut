---
id: SEED-054
status: dormant
planted: 2026-09-29
planted_during: owner request for a budgeted book reading UAT, following the sidebar UX UAT (SEED-042)
trigger_when: selecting book reading fixes and improvements from observed user experience
scope: small
---

# SEED-054: Find what prevents a complete, smooth, stable book reading experience

## Why This Matters

Donut users attach a book (PDF or EPUB) to a notebook and read it in Donut's book
reader: a book layout beside the content, a current block that follows reading,
reading records (read / skimmed), and layout reorganization by hand or with AI.
The owner considers the book reading feature incomplete. Before adding more, the
owner wants to know how well the already-supported behavior works for a real
reader, and where it breaks or feels unfinished.

## Alternatives and Decision

Extending the feature directly would build on behavior whose real-use quality is
unknown. Fixing isolated reported issues would miss the end-to-end reading
journey. Following the approach used for the sidebar (SEED-042), the owner chose
a two-hour manual UAT of what is already supported, to gather concrete findings
before selecting fixes or new capabilities.

## Story Decomposition

Effort bands: S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery.
The manual UAT has an explicit two-hour budget; setup and findings write-up should
be reported separately rather than silently consuming or extending that budget.
This seed captures the assessment story and authorizes no UAT run or fixes now.

<a id="story-1"></a>

### Identify book reading fixes and improvements through a two-hour manual UAT

**Identity:** SEED-054#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/011-book-reading-uat/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"1ab4b82536f69590d4c26d8481a5230751e9cb711ab3c8a0fcbbd05e5af4da71","plan":"5fe6bafaf69a2fc654ada9803c8e400c8271a2912422547f808d8cecfe6c58b6"}}
```

**Goal**

The product owner receives an evidence-backed list of defects to fix and UX
improvements to consider in the currently supported book reading feature, so
subsequent work can make reading a book in Donut complete, smooth, and stable.

**Scope**

- Start the assessment with a two-hour budgeted manual UAT of the supported book
  reading behavior only; do not evaluate capabilities that do not exist yet,
  except to note where their absence blocks an otherwise supported journey.
- Attaching a book: a real PDF through the CLI's notebook attach command (book
  layout extraction included) and a real EPUB, including the refusal of an
  unsupported (for example DRM-protected) EPUB. Use books of varied size and
  structure, not only the minimal fixtures used by automated tests.
- Browsing: the book layout beside the PDF and EPUB content, choosing a book
  block to jump to its position, and scrolling to see the current block follow.
- Current block and selection: the difference between the current block and the
  explicitly selected block, the navigation affordance back to the selection,
  and keeping the book layout scrolled to the current block in short viewports.
- Reading records: marking blocks read or skimmed, automatic marking of
  heading-only blocks, the reading control panel's target, the last block, and
  resuming at the last reading position after leaving and returning.
- Reorganizing the layout: indent and outdent (with descendants), cancelling a
  block, creating a block from a content bbox (with a typed title), and the AI
  reorganization preview and confirmation.
- Assess smoothness, visual presentation, and stability during reading, scrolling,
  navigation, and reorganization, across viewport sizes where practical. These
  scenarios are a starting set, not an exhaustive limit on exploration within
  the budget.
- Deliver findings covering both defects and improvement opportunities, with
  suggested priority and candidates for later product backlog items, including
  which missing pieces most limit the feature's completeness.

**Key examples**

- Attach a real multi-chapter PDF with the CLI → open it in the book reader →
  observe whether the extracted book layout matches the book's structure and
  whether choosing a deep block lands on the right page and position.
- Read an EPUB for several blocks, mark some read and some skimmed, leave the
  reading view and return → observe whether reading resumes where it stopped and
  the records are shown as marked.
- Reorganize a PDF layout (indent a parent, cancel a block, create a block from
  a bbox) → observe whether the layout, current block, and reading records stay
  consistent and understandable.

**Output and evaluation**

Produce one findings report containing:

- Scenarios exercised, book formats and characteristics, how each book was
  attached, and actual UAT time; identify important scenarios not reached within
  the two hours.
- For each defect: expected versus observed behavior, reproduction steps,
  relevant book and viewport conditions, user impact, and evidence where practical.
- For each improvement: the observed friction, intended user benefit, and a
  proportionate recommendation. Distinguish observations from hypotheses.
- Suggested priorities and proposed follow-up story outcomes, separating fixes
  to supported behavior from new capabilities that would complete the feature.

The owner can evaluate success by reviewing reproducible findings and actionable
recommendations. An unreproduced suspected problem must be recorded honestly with
the conditions tried; it must not become a confirmed defect by assumption.

- **For / why:** Users benefit from subsequent improvements selected using real
  book reading behavior; the owner can judge which fixes and missing pieces
  deserve priority.
- **Value / learning:** Learn where attaching, browsing, tracking, recording, and
  reorganizing a book cause friction or fail with real books.
- **Effort hypothesis:** L, medium confidence: two hours of UAT plus bounded setup
  (real books, CLI attach) and synthesis. The two hours limit exploration, not a
  promise of exhaustive coverage.
- **Depends on:** A usable application, the CLI with book layout extraction, and
  representative real PDF and EPUB books; no dependency on other queued stories
  is established.
- **Safe stopping point:** The findings report is useful independently of later
  fixes. Record coverage gaps and inconclusive observations at the budget boundary.
- **Execution handoff:** Use
  [dough-manual-testing](../../.agents/skills/dough-manual-testing/SKILL.md)
  when this assessment is selected for execution.

**Refinement decisions (owner, 2026-09-29)**

- The agent runs the whole UAT itself, then plans and executes it; the owner does
  not drive the browser.
- Books are freely licensed and downloaded from the internet, not committed:
  *Think Python 2e* (244-page PDF with chapters, sections, code, and figures),
  *Attention Is All You Need* (15-page arXiv PDF with numbered sections, tables,
  and formulas), and Project Gutenberg EPUBs of *Alice's Adventures in
  Wonderland* (short, flat chapters with images) and *On the Origin of Species*
  (long chapters). A real DRM-protected EPUB cannot be obtained legitimately, so
  the refusal case uses a copy of a Gutenberg EPUB with a
  `META-INF/encryption.xml` added; report it as derived, not real DRM.
- The owner expects the local MinerU install to work. PDFs attach through the
  CLI's `/attach` with real MinerU; EPUBs attach through the web notebook
  settings, as supported today.
- AI reorganization is judged against the real OpenAI service, so the UAT uses
  the Development stack (profile `dev`) from the default checkout, whose product
  code matches the story's base commit. The E2E stack replaces OpenAI with a mock.
  UAT data goes into the development database under notebooks named `UAT …`.
- MinerU extraction time for a real book is part of the attach experience and is
  recorded, but waiting for it does not use up the two-hour exploration budget.

## UAT Findings

UAT on 2026-09-29 against commit `1f9bdae1bc` (Development stack from the
default checkout, real MinerU and real OpenAI, account `manual`, headless
Chromium driven by Playwright, the interactive CLI driven through a pseudo
terminal). Viewports are 1440×900 unless stated.

Time, against the two-hour exploration budget:

- Setup took about 15 minutes, outside the budget.
- Attaching and browsing took 27 minutes of its 50-minute share (07:33–08:00).
  Breadth across the planned areas was complete by then, and the remaining
  time was not needed to confirm defects 1–9. MinerU ran while other books
  were being explored, so its time is recorded per book and was not spent
  waiting.
- Reading records and reorganizing took 29 minutes of its 55-minute share
  (08:07–08:36). Defects 10–17 come from this part.
- None of the 15-minute reserve was used. In total, exploration used 56 of the
  120 budget minutes (27 + 29).
- Why the budget was not used up: every starting scenario was covered early,
  and each defect was reproduced before it was written down. The scenarios
  still open (see "Coverage gaps") need other browsers, touch devices, or books
  of other sizes, not more time with the same setup.
- Writing up took about 10 minutes for each of the two parts, plus about 10
  minutes for the priorities and follow-up stories, all outside the budget.

Screenshots (`s2-…` from attaching and browsing, `s3-…` from reading records
and reorganizing) and the raw measurements stay outside the repository; the
figures quoted below are the retained evidence.

### Setup and books

| Notebook (id) | Book (book id) | Format and structure | Attached by | Extraction | Result |
| --- | --- | --- | --- | --- | --- |
| UAT Think Python (17) | *Think Python 2e* (5) | PDF, 244 pages; 19 chapters, appendices A and B, 218 numbered sections, code, figures, running page headers | CLI `/attach` | MinerU 167 s (07:32–07:35) | 361 blocks; see defects 5 and 6 |
| UAT Attention (21) | *Attention Is All You Need* (6) | PDF, 15 pages; numbered sections to three levels, tables, formulas | CLI `/attach` | MinerU 28 s | 26 blocks, every heading found, but the hierarchy is flat (defect 6) |
| UAT Alice (18) | *Alice's Adventures in Wonderland*, Gutenberg (3) | EPUB; 12 short chapters, cover image | Web notebook Settings → *Attach book…* | 0.8 s upload | 17 blocks, the same as the EPUB table of contents |
| UAT Origin of Species (20) | *On the Origin of Species*, Gutenberg (4) | EPUB; 14 long chapters, detailed contents | CLI `/attach` (raw EPUB upload) | under 7 s | 24 blocks, the same as the EPUB table of contents |
| UAT DRM Alice (19) | Gutenberg Alice plus an added `META-INF/encryption.xml` (a derived DRM case, not real DRM) | EPUB | Web and CLI | — | Refused by both routes: "EPUB is encrypted or DRM-protected; only non-DRM EPUB is supported". The web shows it as a short error message and the notebook stays without a book |

Setup observation, about the owner's environment rather than the product: the
MinerU venv has a broken interpreter link: `bin/python3` points to a missing Nix
store path. The CLI worked only with
`DONUT_MINERU_PYTHON=/opt/homebrew/bin/python3.12` plus the venv's
`site-packages` on `PYTHONPATH`.

### Defects

1. **Choosing an EPUB chapter lands far inside the chapter, not at its start
   (High).**
   - *Expected:* choosing a book block shows the start of that chapter, with its
     heading at the top.
   - *Observed:* the chapter heading ends up far above the visible area. Offsets
     measured at 1440×900: Alice chapter I and chapter VII, several paragraphs
     in; chapter III 855 px; chapter XII 1,077 px. Origin: Introduction 358 px;
     chapter XIV 3,805 px; chapter IV 6,416 px, which is the middle of the
     chapter. At 1280×560, chapter IV was 7,702 px. The offsets are
     deterministic and repeat on every choice. A link inside the book (Origin's
     detailed contents → "CHAPTER 2") also lands near the end of chapter II.
   - *Reproduction:* open the reader for *UAT Origin of Species*, choose
     "CHAPTER IV. NATURAL SELECTION.", and look for the chapter heading.
   - *Impact:* layout navigation, the core EPUB browsing action, cannot be
     trusted. The reader starts mid-chapter, and the current block and reading
     position then describe a place the reader never chose.
   - *Evidence:* `s2-06-alice-land-*`, `s2-17-origin-land-1`,
     `s2-18-origin-repeat`, `s2-28-origin-inbook-link`,
     `s2-32-origin-land-short-*`.
   - *Hypothesis (not verified):* the offset grows with the length of the
     preceding chapter. This suggests the scroll target is computed before
     epub.js inserts the previous section above the chosen one.
2. **At phone width the reader is unusable (High).**
   - *Expected:* at 390×844 the content uses the screen width, and the book
     layout can be opened as a drawer.
   - *Observed:* the collapsed book layout panel is moved off screen (x = −288) but
     still takes its 288 px of width. The PDF or EPUB content is squeezed into
     a 102 px column on the right: PDF pages render as thumbnails, EPUB text
     shows one or two words per line, and the "Now reading" bar and the
     Read/Skim/Skip panel are cut off. The *Book layout* toggle (x 88–130) sits
     under the main menu's *Toggle menu* button, which intercepts the click, so
     the layout cannot be opened. At 768×1024 the toggle is also covered,
     although the layout panel is shown there. At 1024×768 and above, it works.
   - *Reproduction:* open `/notebooks/17/book` or `/notebooks/18/book` at
     390×844.
   - *Impact:* no reading on phones.
   - *Evidence:* `s2-11-tp-mobile-open`, `s2-12-alice-mobile-open`,
     `s2-13-*` (bounding boxes), `s2-14-tp-390-toggle-forced`.
3. **Right after choosing a PDF block, the first scrolling jumps backwards
   (Medium).**
   - *Expected:* each downward wheel step moves the PDF down.
   - *Observed:* in the first second of scrolling after choosing a block, the
     view jumps back. Per-step scroll deltas for 400 px wheel steps at 120 ms:
     `+400, +400, −156, +400, −800, +400 …` (Think Python 8.1, three of three
     runs), `+400, +400, −265, +400, −800 …` (14.6), and `+400, 0, +400, −800 …`
     (Attention 3.1). At a 700 ms pace it was one −156 jump and one ignored
     step. It happens wherever the pointer is.
   - *Reproduction:* open *UAT Think Python*, choose "8.1 A string is a
     sequence", and wheel down steadily.
   - *Impact:* the page jerks up by up to a screen while the reader is moving
     down, a visible jerk at the start of every reading run.
   - *Evidence:* the measured scroll deltas above.
4. **The Reading Control Panel stops wheel scrolling when it is under the
   pointer (PDF, Medium).**
   - *Expected:* the book keeps scrolling while the pointer rests over the
     reading area.
   - *Observed:* the content-anchored panel (Read/Skim/Skip) appears in the
     middle of the PDF view, at y 437–485 at 1440×900. When it arrives under a
     resting pointer, every further wheel step is swallowed: scroll moved 0 px
     for 12 steps, and still 0 px after waiting 3 s. `elementFromPoint`
     returned `book-reading-reading-control-panel`. This occurred on the first
     run after opening, in 2 of 2 sessions. The EPUB panel, docked at the
     bottom, passes wheel events through.
   - *Reproduction:* as in defect 3, with the pointer at the centre of the PDF.
   - *Impact:* the reader appears frozen until the pointer is moved.
   - *Evidence:* `s2-20-pdf-stuck-0`.
5. **PDF extraction turns running page headers into duplicate blocks
   (High).**
   - *Expected:* one block per heading in the book.
   - *Observed:* Think Python's 361 blocks contain every one of the 218
     numbered sections in its table of contents, plus:
     - 75 duplicates taken from running page headers, for example "14.6.
       Databases" next to "14.6 Databases";
     - 3 "Contents" blocks and 6 "Index" blocks;
     - 16 separate "Chapter N" or "Appendix A" label blocks next to the
       chapter titles.
     Choosing a duplicate lands on the next page's header. While scrolling, the
     current block and the "Now reading" bar pass through the duplicates, for
     example "14.9. Writing modules" before "14.9 Writing modules".
   - *Reproduction:* attach `thinkpython2.pdf` with the CLI, then open the
     reader.
   - *Impact:* about a fifth of the layout is not real headings. Reading
     records, the current block, and "last block" logic operate on these extra
     blocks, and the reader has to clean the layout up by hand before it is
     useful.
   - *Evidence:* `s2-07-tp-open`, `s2-08-tp-land-3`, `s2-09-tp-scroll-*`, and
     the Think Python layout dump `s2-tp-layout.txt`.
6. **The extracted PDF hierarchy is flat or wrong (Medium).**
   - *Expected:* the layout's depth follows the book: chapter → section →
     subsection.
   - *Observed:*
     - Attention: every block from "Abstract" to "References", including 3.2.1,
       sits at depth 1 under the title, so "3 Model Architecture" does not
       contain its sections.
     - Think Python: only depths 0 and 1 exist.
       - 6 of 21 chapter or appendix titles (Strings, Lists, Tuples,
         Inheritance, The Goodies, Debugging) and 5 of the label blocks are
         nested under the previous chapter.
       - Subsections such as A.2.1 sit at the same depth as A.2.
   - *Reproduction:* open either PDF's reader and compare with the book's
     contents pages (`pdftotext -layout -f 13 -l 21 thinkpython2.pdf -`).
   - *Impact:* the layout cannot be collapsed or understood by chapter, and
     heading-only auto-marking and descendant-aware reorganizing have wrong
     parents to work with.
   - *Evidence:* the Think Python layout dump `s2-tp-layout.txt`.
   - *Hypothesis (not verified):* the setup probe of the MinerU script alone
     produced a nested outline for the arXiv paper, so nesting may be lost
     after extraction.
7. **On first open, the EPUB current block is not what is shown, and
   "Contents" is marked read (Medium).**
   - *Expected:* a newly attached book opens with the current block matching
     the visible start of the book, and nothing is marked read.
   - *Observed:* Origin's first open shows the cover and the Gutenberg header,
     but the current block is "DETEAILED CONTENTS…", and "Contents" already
     carries the read mark. The API shows a READ record created at that moment.
     Alice behaved the same way: current block "Contents" over the cover, then
     a read mark on "Contents".
   - *Reproduction:* attach an EPUB to a new notebook and open the reader once.
   - *Impact:* a reading record the reader never made, and a wrong starting
     point for the current block.
   - *Evidence:* `s2-03-alice-open`, `s2-15-origin-open`.
8. **Choosing an EPUB block without an anchor does not go to it (Medium).**
   - *Expected:* choosing any block shows its content, or the block is not
     offered as a navigation target.
   - *Observed:*
     - Alice: choosing "Alice's Adventures in Wonderland" or "THE FULL PROJECT
       GUTENBERG™ LICENSE" selects it, and the panel offers "Read" for it, but
       the content stays in chapter VIII.
     - Origin: choosing "ON THE ORIGIN OF SPECIES." selects a different block
       ("DETEAILED CONTENTS…").
     These are the layout blocks shown without an EPUB start href.
   - *Reproduction:* open the reader for *UAT Alice* and choose "THE FULL
     PROJECT GUTENBERG™ LICENSE".
   - *Impact:* the selection and the reading panel disagree with the visible
     text, so a click on Read records the wrong block.
   - *Evidence:* `s2-29-alice-nohref-2`, `s2-17-origin-land-3`.
9. **The CLI runs the full PDF extraction before saying the notebook already has
   a book (Low).**
   - *Expected:* an immediate refusal.
   - *Observed:* `/attach arxiv-attention.pdf` in *UAT Alice* spent 30.6 s in
     MinerU before printing "This notebook already has a book attached". For
     Think Python this would be about 3 minutes. For an EPUB the refusal is
     immediate.
   - *Reproduction:* in the CLI, `/use` *UAT Alice* and run
     `/attach arxiv-attention.pdf`.
   - *Impact:* wasted wait with no result.
   - *Evidence:* the CLI session log (`cli-raw.log`).
10. **AI reorganization fails on a full-size book (High).**
    - *Expected:* *AI Reorganize* on *UAT Think Python* (361 blocks) shows the
      preview of a proposed structure, as it does for the 27-block paper.
    - *Observed:* in 3 of 3 runs, the request failed with a server error after
      7.5–8.3 s. A red message filled a third of the screen with raw text:
      "Error parsing JSON: {"blocks":[{"depth":0,"id":44}, …". The text stops
      in the middle of an entry at block id 130, about 87 of the 361 blocks.
      The message disappeared after a few seconds, with no preview and no
      plain explanation. After the failure, the layout had scrolled back to its
      top. The current block, 9.1, moved from y 860 to y 6,367, out of view
      (seen in 2 runs).
    - *Reproduction:* open the reader for *UAT Think Python* and click *AI
      Reorganize*.
    - *Impact:* AI help is unavailable for exactly the kind of book that needs
      it most. Think Python has defects 5 and 6 to clean up, and doing that by
      hand is slow (see the improvement "Fixing a layout by hand").
    - *Evidence:* `s3-33-ai-err-17`, `s3-34-ai-err-later-17`, `s3-ai-tp.txt`.
    - *Hypothesis (not verified):* the model's answer is cut off by an output
      size limit. That would make every book above roughly 90 blocks fail.
11. **In EPUB, the reorganizing controls are shown but do nothing (Medium).**
    - *Expected:* *AI Reorganize*, Tab, Shift+Tab and Backspace on a chosen
      block act as they do in PDF, or are not offered.
    - *Observed:* in both *UAT Alice* and *UAT Origin of Species*:
      - clicking *AI Reorganize* sends no request and shows nothing, no
        progress, message or preview;
      - Tab on "CHAPTER II" (the block before it is at the same depth) sends
        no request, and so do Shift+Tab and Backspace. The depth stays the same,
        and nothing says the action is unavailable.
      The spurious "Contents" block from defect 7 therefore cannot be cancelled
      either. Clicking the EPUB text offers no "New block".
    - *Reproduction:* open the reader for *UAT Alice*, click *AI Reorganize*;
      then click "CHAPTER II. The Pool of Tears" and press Tab.
    - *Impact:* an EPUB whose table of contents is poor cannot be improved, and
      a visible button that silently does nothing looks broken. The automated
      scenarios cover reorganizing only for PDF, so keyboard reorganizing of
      EPUB may be a missing capability. The visible button doing nothing is a
      defect either way.
    - *Evidence:* `s3-35-ai-epub-click`, `s3-36-ai-epub-after`,
      `s3-37-epub-reorg`, `s3-37-epub-reorg-origin`.
12. **A block made from an ordinary paragraph takes the whole paragraph as its
    title, with no chance to type one (Medium).**
    - *Expected:* creating a block from a long content box asks for a title,
      with the content as the default.
    - *Observed:* the title prompt appears only when the paragraph text reaches
      512 characters. Paragraphs of 200 characters (Attention 3.2) and 289
      characters (Think Python 8.3) became block titles directly. In the
      layout, the 200-character title fills seven lines. No control to rename a
      block was found. A 512-character paragraph (Think Python 9.1) did show the
      "Name the new block" prompt, and the typed title was used.
    - *Reproduction:* in *UAT Attention*, choose "3.2 Attention", click the
      first paragraph under the heading, then click *New block*.
    - *Impact:* the layout fills with paragraph-length titles that cannot be
      corrected afterwards.
    - *Evidence:* `s3-23-attn-callout`, `s3-25-attn-after-new-block`,
      `s3-26-title-dialog`.
13. **Keyboard users cannot move through the book layout (Medium).**
    - *Expected:* keyboard users can move from block to block and choose one.
    - *Observed:* pressing Tab from the top of the page reaches the zoom and
      search buttons, *AI Reorganize*, and then the first block, "*beginning*".
      The next Tab is treated as "indent": it sends a depth change, which is
      refused with "Block is already at maximum depth relative to predecessor",
      and focus drops to the page body. The arrow keys do nothing. No other
      block can be reached by keyboard. On any block that can be indented, a
      Tab meant to move on would change the saved layout.
    - *Reproduction:* open *UAT Attention*, click the page header, and press
      Tab seven times.
    - *Impact:* the layout, the main way to move around a book, cannot be used
      without a mouse.
    - *Evidence:* `s3-38-kbd-focus-block` and the refused depth request.
14. **Keyboard focus leaves the block after each indent or outdent (Low).**
    - *Expected:* after Tab or Shift+Tab, the block keeps focus, so a second
      press moves it again.
    - *Observed:* after each successful Tab or Shift+Tab, focus went to the page
      body (2 of 2). A second key press then moved focus through the page
      instead, and in one run reached a close button. To press the key again,
      the block has to be clicked again, which also moves the book to it.
    - *Reproduction:* in *UAT Think Python*, click "Case study: word play" and
      press Shift+Tab twice.
    - *Impact:* moving a block by two levels, or fixing several blocks, takes a
      click per step (see the improvement "Fixing a layout by hand").
    - *Evidence:* `s3-19-tp-focus-after-outdent`.
15. **With a heading-only block selected, there is no Read/Skim/Skip, and the
    heading is never marked (Medium).**
    - *Expected:* after choosing a block with no text of its own, such as a
      "Chapter 9" label, the reader can go on reading and marking. The
      heading-only block is marked read when its successor is entered.
    - *Observed:* choosing "Chapter 9", "Chapter 10" or "Chapter 12" in *UAT
      Think Python* hid the Reading Control Panel. It stayed hidden while
      scrolling through 3–4 pages of the chapter. The current block went from
      the label straight to x.1. It skipped the chapter title block ("Case
      study: word play", "Lists"), which starts on the same screen and has
      introduction text. Neither the label nor the title was marked. The "Now
      reading" bar was shown, so *Read from here* lets the reader continue.
      When "Case study: word play" was chosen instead, "Chapter 9" was marked
      read straight away, as promised. Separately, running-header duplicates
      from defect 5 (for example "9.3. Search") have no text of their own, so
      they are auto-marked read while scrolling.
    - *Reproduction:* open *UAT Think Python*, choose "Chapter 12", and scroll
      down four pages.
    - *Impact:* at every chapter start of a book like this, marking stops until
      the reader notices and uses *Read from here*. The chapter title block is
      never reached, so it keeps no record.
    - *Evidence:* `s3-12-tp-chapter-label`, `s3-13-tp-after-scroll`.
16. **Reopening an EPUB does not scroll the layout to the current block
    (Low).**
    - *Expected:* on reopening, the layout shows the current and selected
      block, as it does for PDF.
    - *Observed:* at 1280×560, *UAT Origin of Species* reopened with the layout
      at its top. The current and selected block was out of view: "CHAPTER
      III" at y 689, and after choosing "CHAPTER XIV", leaving and returning,
      at y 1,288, with the layout panel ending at y 560. At 1440×900, CHAPTER
      III was just visible only because it was near the top of the list. Think
      Python (PDF) reopened with its current block scrolled into the layout.
    - *Reproduction:* at 1280×560 in *UAT Origin of Species*, choose "CHAPTER
      XIV", go to the notebook page, and open the reader again.
    - *Impact:* the reader cannot see where they are in the book until they
      scroll the layout by hand.
    - *Evidence:* `s3-41-open-layoutpos-20-560`, `s3-42-origin-reopen-xiv-560`.
17. **AI reorganization leaves one level of the paper unnested (Low).**
    - *Expected:* the proposed structure follows the paper's numbering:
      3 → 3.2 → 3.2.1.
    - *Observed:* in two runs (6.1 s and 3.1 s) the same proposal nested every
      x.y under x and kept Abstract … References under the title. However, it
      left 3.2.1, 3.2.2 and 3.2.3 at the same depth as 3.2 instead of under it.
      Everything else was correct. The preview cannot be adjusted row by row,
      so these three blocks must be fixed by hand after confirming.
    - *Reproduction:* attach `arxiv-attention.pdf` to a new notebook with the
      CLI. It arrives flat, as described in defect 6 (*UAT Attention* is now
      nested). Then click *AI Reorganize*.
    - *Impact:* small here, but it shows that the proposal needs checking. The
      preview makes that possible only by reading every row.
    - *Evidence:* `s3-31-ai-preview-a`, `s3-31-ai-preview-b`.

### What worked

- Attaching and browsing:
  - PDF navigation is precise. Every deep block tried landed on the right page
    with its heading at the top:
    - Think Python: 3.9 → p. 45, 14.6 → p. 163, 16.1 → p. 177,
      B.4 → p. 228, matching the contents page numbers plus the 22-page front matter;
    - Attention: 3.2.1 → p. 4, 6.2 → p. 9.
  - The current block follows scrolling in both formats. In PDF, the "Now
    reading … / Read from here / Back to selected" bar appears when the
    current block differs from the selection, and *Back to selected* returns
    to it.
  - At 1280×560, the layout kept the current block visible on every one of 30
    scroll steps in both a PDF (Think Python, 361 blocks) and an EPUB
    (Origin). Zooming in twice and resizing from 1440×900 to 1280×560 kept the
    position.
  - Rapidly choosing 5–6 blocks ended in the right place for the last choice.
    Scrolling had no long-running browser tasks, and the worst frame gap was
    33–83 ms. No console or page errors appeared apart from development-only
    items (a feature-toggle 404, and the yellow "T" testability button on
    screenshots).
  - The DRM refusal is clear by both routes. EPUB upload is quick on the web
    and in the CLI, and the CLI prints the resulting layout.
- Resuming: reopening a reader resumed the last position in both formats.
  - While browsing: PDF at 3.9; EPUB at chapter VIII.
  - While recording: Attention reopened at page 3 and at page 15; Alice
    reopened at the scrolled place in chapter V with chapter IV still
    selected.
- Reading records and reorganizing:
  - Read, Skim and Skip each save at once (checked through the API), show as
    a mark in the layout, and move the selection to the next block. This
    worked in PDF (Attention) and EPUB (Alice), and the marks were still there
    after leaving and returning.
  - As promised, choosing an already marked block makes the panel offer the
    next block: "5.1" marked → panel for "5.2". The last block ("References")
    could be marked once the end of the paper was reached.
  - PDF indent and outdent (with descendants) and cancel (children move up a
    level) worked and were still there after reloading. Each took about 1.5 s
    to show. An impossible indent shows a clear message. Dragging a block left
    or right with the mouse also changes its depth.
  - Records, the current block and the selection stayed consistent through
    indent, cancel, block creation and AI confirmation. Cancelling a marked
    block also removed its record.
  - AI reorganization on the paper: preview in 3.1–6.1 s with changed rows
    highlighted. *Cancel* left the layout untouched, and *Confirm* applied the
    proposal in 0.8 s and kept all records.

### Improvements

- **EPUB has no current-block bar.** The "Now reading / Read from here / Back
  to selected" bar exists only for PDF. In EPUB, once the reader scrolls on,
  the only way back to the selection is clicking the block again, and the panel
  keeps targeting the old selection (`s2-04-alice-scrolled`). Offer the same
  bar for EPUB, so both formats behave alike.
- **Attaching is hard to discover.**
  - The web's *Attach book…* accepts only `.epub`, with no hint that PDFs go
    through the CLI.
  - The CLI's top-level `/help` needs two Enters (accept the suggestion, then
    run). It does not list `/attach`, and in notebook context it says "Not
    supported".
  - The CLI help does not say where to get the access token it needs, although
    getting one on the web was easy (Account → Manage Access Tokens).
  - Recommendation: one sentence in the web card about the CLI route for PDF,
    and `/attach` and where to get a token in the CLI help.
- **There is no feedback during extraction.** The CLI shows only a spinner and
  "Attaching book…" for the whole MinerU run (about 3 minutes for 244
  pages). Showing the stage and page progress would tell the reader it is
  working.
- **It is hard to get from attaching to reading.**
  - After a CLI attach, nothing says where to read, although a reader URL could
    be printed.
  - On the notebook page, the book appears in the sidebar as a plain file with
    *Download* and *Delete* but no *Read* (`s2-25-sidebar-book-file`). What
    *Delete* does to the book and its records was not tried.
  - The ways in are Settings → *Read*, or the unlabelled book icon in the
    notebook list (`s2-24-notebooks-uat`). Adding *Read* where the book file is
    shown would help.
- **Books are named after the file:** "thinkpython2", "arxiv-attention",
  "alice", "origin-of-species". The EPUB metadata title, or a title the
  reader can edit, would read better in the reader header and the settings.
- **Overlays cover the text.** The content-anchored panel and the "Now
  reading" bar are semi-transparent over the book text. For example, the
  "3.2.2 Multi-Head Attention" heading is under the panel in
  `s2-19-attn-land-1`. The bars could reserve space instead, or avoid the next
  heading.
- **A reading record cannot be changed or removed.** Choosing a marked block
  makes the panel offer the next block, as designed, and no other control
  exists. A mistaken Skim, the spurious "Contents" record (defect 7), and
  auto-marked running-header duplicates (defect 15) stay for good. Cancelling
  the block clears the record, but only in PDF (defect 11). Recommendation:
  when a marked block is chosen, offer to change or clear its mark.
- **Record marks are hard to read** (`s3-03-attn-after-skip`,
  `s3-39-dark-21`). A record shows only as a thin coloured bar at the layout's
  right edge: green read, orange skimmed, black skipped. There is no legend or
  tooltip; the words exist only for screen readers. In the dark theme, the
  black "skipped" bar cannot be seen. A chapter whose sections are all read
  shows nothing on the chapter itself. Recommendation: a tooltip or legend,
  colours that work in both themes, and a simple progress sign on parents.
- **Nothing marks the end of the book.** After the last block was marked, the
  panel simply disappeared (`s3-10-attn-after-last-read`). Nothing said the
  book was finished, or that 19 of 27 blocks were still unmarked. A short
  summary with a way to jump to unmarked blocks would tell the reader what is
  left.
- **Fixing a layout by hand takes one click and one key per block.**
  - Outdenting a block makes the blocks after it at its old level its
    children, because the order of blocks is fixed. One wrong Shift+Tab on
    "9.1" put the rest of chapter 9 under it. Restoring it took 8 separate
    operations.
  - Cancelling the "Chapter 12" label moved "Tuples" and all 14 sections to
    the top level. Nesting them again under "Tuples" took 14 click-and-Tab
    steps.
  - Every click to choose a block also moves the book there, and after
    cancelling, the selection and the book go back to the previous block.
  - Together with defects 5, 6 and 10, cleaning up Think Python by hand would
    take hundreds of such steps.
  - Recommendation: an undo for the last change, and a way to indent or
    outdent a range of blocks at once, for example "make the following
    blocks children of this one".
- **The "New block" action is hidden.** The coloured content boxes that show
  where a click creates a block fade out about 2 s after a block is chosen
  (`s3-22-attn-bboxes-400ms`). Nothing tells the reader that clicking a
  paragraph offers *New block*, and the small callout then covers the
  paragraph's own text (`s3-23-attn-callout`). A visible hint or a menu entry
  would help.
- **The title prompt is awkward** (`s3-26-title-dialog`). The default is the
  full 512 characters, cut in the middle of a word ("…113809of.fic; y"). It is
  not selected, so typing adds to it, and Enter does not confirm. A short
  default (the first sentence), selected, with Enter to confirm, would make
  typing a title quick.
- **The AI preview is hard to check.**
  - Changed rows are highlighted, but there is no count of changes. The list
    sits in a small scrolling box, about 11 rows at 900 px high and 8 at
    560 px, so checking a long book means scrolling through every row.
  - When the proposal matches the current layout, nothing says so and
    *Confirm* is still offered (`s3-31-ai-preview-small`).
  - Rows cannot be accepted or rejected one by one (see defect 17).
  - The waiting screen ("Analyzing book layout…") has no cancel. That is fine
    at 3–6 s, but not for a long wait.
- **Small items:**
  - In short viewports, the current block is kept at the layout panel's
    bottom edge (520–560 of 560 px), so the reader cannot see what comes next;
    a margin would help.
  - The page indicator can disagree with the heading in view ("11 / 15" while
    "References" at the top is on page 10).
  - The content-block bbox overlays show as small unexplained red, green, and
    cyan boxes around citations.
  - The reader title sits higher than the "Notebook" back link.
  - *Read from here* scrolls back to the block's start (about 280 px at
    1280×560) instead of keeping the reader's place.
  - The new-notebook name field has only a placeholder, no visible label.
  - Right after creating a block, the "Now reading" bar appears, because the
    parent is current and the new child is selected, although the reader has
    not moved (`s3-25-attn-after-new-block`).
  - In the dark theme, EPUB pages stay bright white next to the dark layout
    (`s3-39-dark-18`).

### Coverage gaps

- Only headless Chromium with synthetic wheel events was used. Trackpad
  momentum, touch scrolling, other browsers, and perceived flicker were not
  observed; only frame timing was measured.
- Phone width could not be explored beyond the first screen because of
  defect 2.
- PDF attach failures (a corrupt PDF, a MinerU error or timeout) and PDF
  upload through the web (not offered) were not exercised.
- No image-heavy EPUB was used: this Alice edition has only a cover.
- The dark theme was checked only through the browser's colour-scheme setting,
  on one screen per format. Keyboard use was checked only for the layout
  (defects 13 and 14), not for the reading panel or dialogs.
- Heading-only auto-marking in EPUB was not exercised; no suitable block was
  found in these two EPUBs within the time.
- AI reorganization was judged on one small book only. The book size at which
  it starts to fail (defect 10) is unknown, because no book between 27 and 361
  blocks was tried. AI reorganization of EPUB could not be judged (defect 11).
- Block creation from a content box exists only for PDF; EPUB offers nothing
  to click, so it was not evaluated.
- Reading on two devices or in two tabs at once was not tried.
- Deleting a book through the sidebar file view, and what that does to its
  records, was not tried, to keep the UAT data.

### UAT data left in the development database

- *UAT Attention* arrived flat with 26 blocks and no records. It is now nested
  by AI and has records on 7 blocks. It has a new block made from the first
  paragraph of 3.2.
- *UAT Think Python* had one READ record after browsing, from heading-only
  auto-marking. It has lost the duplicate "9.3. Search" and the "Chapter 12"
  label. It has three new blocks: one under 8.3 titled with a whole paragraph
  ("A lot of computations involve processing a string…"), "Moby word list
  (UAT)", and "Anagram exercises (UAT)". Chapter 9's depths were restored by
  hand.
- *UAT Origin of Species* and *UAT Alice* each carry the spurious "Contents"
  READ record from defect 7, and each has a saved reading position. *UAT
  Alice* also has records on chapters II and III.

### Suggested priorities

The ranking asks one question: how much does the finding stop a reader from
reading a whole book completely, smoothly, and stably?

**P1: the reader cannot rely on the book or the layout**

- Defect 1, EPUB chapter landing: choosing a chapter, the main way to move
  around an EPUB, starts the reader in the middle of the chapter.
- Defect 5, duplicate blocks from PDF page headers: about a fifth of a real
  book's layout is not real headings, and records and the current block pass
  through them.
- Defect 6, flat or wrong PDF nesting: the layout cannot be read or folded by
  chapter, and the parent-based features work on wrong parents.
- Defect 10, AI reorganization fails on a real-size book: the one tool that
  could clean up defects 5 and 6 fails on exactly the books that need it, and
  shows raw JSON.
- Defect 2, phone width: no reading at all on a phone.

**P2: reading works, but breaks the flow or leaves wrong data**

- Defects 3 and 4, PDF scrolling jumps back or stops: a visible jerk or a
  frozen view at the start of every reading run.
- Defect 15, chapter labels stop marking: at each chapter start in a book like
  Think Python, marking stops until the reader notices.
- Defects 7 and 8, EPUB first open and blocks without an anchor: records and
  the selection describe text the reader never saw.
- Defect 11, EPUB reorganizing controls that do nothing: a visible button that
  silently does nothing looks broken.
- Defect 12, paragraph-length block titles: they clutter the layout and cannot
  be corrected.
- Defect 13, Tab in the layout: keyboard users cannot move around, and a Tab
  can change the saved layout by accident.
- Improvement "A reading record cannot be changed or removed": wrong records
  from defects 7 and 15, or from a mistaken click, stay for good.
- Improvement "Fixing a layout by hand": without undo or range moves, one wrong
  key costs many steps, and imperfect extraction makes such fixes common.
- Improvement "EPUB has no current-block bar": EPUB readers lose the way back
  to the selection that PDF readers have.

**P3: friction or polish**

- Defect 9 (the CLI extracts before refusing), defect 14 (focus leaves the
  block), defect 16 (EPUB reopening does not scroll the layout), and defect 17
  (the AI leaves one level unnested): each costs a little time and has an easy
  way around it.
- Improvements on attaching and finding the reader: discovering how to attach,
  extraction progress, a *Read* entry point, and the book title.
- Improvements on reading comfort and progress: overlays over the text, marks
  that are hard to read, the end of the book, the hidden *New block* action, the
  title prompt, checking the AI preview, and the small items.

### Proposed follow-up stories

Sizes use the seed's effort bands (S = 30–60 minutes, M = 1–2 hours,
L = 2–4 hours). These are candidates for the owner's decision; none is queued.

**Fixes to supported behavior**

| Story outcome | Covers | Size | Open question |
| --- | --- | --- | --- |
| Choosing an EPUB block shows the start of that block, and reopening shows it in the layout | Defects 1, 8, 16 | M | For a block without an anchor: go to the nearest place in the book, or stop offering it as a place to go? |
| A reader on a phone can read a book and open the layout as a drawer | Defect 2 | M | Should the drawer behave like the notebook sidebar drawer? |
| A PDF attached through the CLI gets one block per real heading, nested as in the book | Defects 5, 6 (and the header duplicates auto-marked in defect 15) | L | Is nesting lost in Donut after MinerU (the hypothesis in defect 6)? If so, the story may be M. Should the PDF's own bookmarks be used when present? |
| AI reorganization gives a preview for a real-size book, and a failure is explained in plain words without moving the layout | Defect 10; defect 17 could be included | M–L | Where is the limit (no book between 27 and 361 blocks was tried)? Split the book into parts, or ask the AI only for the changes? |
| Scrolling a PDF moves smoothly in one direction from the first wheel step, wherever the pointer rests | Defects 3, 4 | M | None |
| A newly attached EPUB opens at its start, with no records the reader did not make | Defect 7 | S | None |
| Reading and marking continue through a chapter label into the chapter | Defect 15 | S–M | Should label blocks such as "Chapter 9" disappear once extraction is fixed? |
| Keyboard users can move between layout blocks without changing the layout, and can indent or outdent the same block again | Defects 13, 14 | M | Which keys move and which keys change depth, if Tab no longer does both? |
| EPUB readers see only reorganizing controls that work | Defect 11 (hiding them); making them work is a capability below | S | Not needed if EPUB reorganizing is built first |
| A new block gets a short title the reader can type, whatever the paragraph length | Defect 12, improvement "The title prompt is awkward" | S | None |
| The CLI refuses a PDF for a notebook that already has a book before extracting it | Defect 9 | S | None |

**Missing capabilities that would complete the feature**

| Story outcome | Covers | Size | Open question |
| --- | --- | --- | --- |
| A reader can undo the last layout change and indent or outdent several blocks at once | Improvement "Fixing a layout by hand" | M | Does undo need to cover more than the last change? |
| A reader can change or clear the mark on a block | Improvement "A reading record cannot be changed or removed"; the wrong records left by defects 7 and 15 | S–M | None |
| A reader can rename a block | Defect 12 (existing titles) | S | None |
| A reader can reorganize an EPUB layout as for PDF (indent, outdent, cancel, AI) | Defect 11 | L | Is creating a block from EPUB text also needed, or only the other actions? |
| EPUB readers get the same "Now reading / Read from here / Back to selected" bar as PDF readers | Improvement "EPUB has no current-block bar" | M | None |
| A reader sees how far through the book they are: readable marks, progress on chapters, and a summary at the end with a way to the unmarked blocks | Improvements "Record marks are hard to read" and "Nothing marks the end of the book" | M | None |
| A reader goes from an attached book to reading it in one step | Improvement "It is hard to get from attaching to reading" (a *Read* entry point where the book file is shown, a reader link after a CLI attach); improvement "Attaching is hard to discover" | S–M | Should PDFs also be attachable on the web, or is a hint about the CLI enough? |
| A book shows its real title | Improvement "Books are named after the file" | S | EPUB metadata only, or also a title the reader can edit? |
| The CLI shows extraction progress while a PDF is attached | Improvement "There is no feedback during extraction" | S–M | Does MinerU report page progress that the CLI can show? |
| A reader can check an AI proposal quickly and take only the rows they want | Improvement "The AI preview is hard to check"; defect 17 | M | None |

**Recommendations on the Open Decisions** (the owner decides):

- *Which findings, in what order:* the P1 fixes first, starting with EPUB
  landing and PDF extraction, because they decide whether a layout can be
  trusted at all. The phone-width fix can follow if phone reading matters now.
- *Which missing pieces most limit completeness:* the ability to correct
  things. Extraction is imperfect and AI help fails on real books, yet the
  reader cannot undo a layout change, move several blocks at once, rename a
  block, reorganize an EPUB, or correct a reading record. After that comes giving
  EPUB what PDF already has (the current-block bar and reorganizing).
- *Missing capabilities before fixes?* No, with one exception. Fixes to
  supported behavior should come first, because the missing pieces matter less
  while navigation and extraction are unreliable. The exception is changing or
  clearing a record: it is small, and it lets readers recover from the wrong
  records that the defects already produce. Undo and range moves should be
  judged again after the PDF extraction and AI reorganization fixes, which
  decide how much fixing by hand is still needed.

## Ordering and Scope Reduction

The owner added this as the last item in the current product backlog.
At the two-hour boundary, stop exploration and synthesize what was observed.
Preserve untested scenarios as coverage gaps rather than extending the UAT or
claiming complete coverage. Implementation fixes, new book reading capabilities,
and automatic creation of follow-up backlog entries are deferred; the output
supplies candidates for a later product decision.

## Open Decisions

- Which findings warrant fixes or improvements, and in what order? Decide from
  the UAT report rather than selecting them in advance.
- Which missing capabilities most limit the feature's completeness, and should
  they come before fixes to supported behavior?
- The UAT's recommendations on both questions are at the end of
  `### Proposed follow-up stories`; the owner decides.

## When to Surface

Select after the preceding queued stories, or when the owner reprioritizes book
reading assessment.

## Breadcrumbs

- Owner request on 2026-09-29: the book reading feature is incomplete; run a
  two-hour manual UAT of what is already supported, with the same target and
  process as the sidebar UX UAT (SEED-042#story-1).
- SEED-042's refinement ran the UAT against the local app from the story's own
  execution checkout with a seeded account and a real browser (screenshots as
  evidence), reported setup time separately, delivered the report as a
  `## UAT Findings` section in the seed, and executed as one planless slice with
  no product code, automated test, or tooling change. Its findings were queued as
  a separate defect seed (SEED-043). Recoverable from commits c01f1942fb,
  dbe4a996b4, and adce721eea.
- Supported behavior today is described by the E2E features under
  `e2e_test/features/book_reading/`; real PDF attachment goes through the CLI
  notebook attach command with MinerU book layout extraction.
