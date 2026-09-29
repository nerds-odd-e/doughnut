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
terminal). Setup took about 15 minutes outside the budget. Attach and browse
exploration took 27 minutes of its 50-minute share (07:33–08:00). Breadth
across the planned areas was complete by then, and the remaining time was not
needed to confirm the defects below. MinerU ran while other books were being
explored, so its time is recorded per book and was not spent waiting.
Screenshots (`s2-…`) and the raw measurements stay outside the repository; the
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
   - *Hypothesis (not verified):* the slice-1 probe of the MinerU script alone
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

### What worked

- PDF navigation is precise. Every deep block tried landed on the right page
  with its heading at the top:
  - Think Python: 3.9 → p. 45, 14.6 → p. 163, 16.1 → p. 177, B.4 → p. 228,
    matching the contents page numbers plus the 22-page front matter;
  - Attention: 3.2.1 → p. 4, 6.2 → p. 9.
- The current block follows scrolling in both formats. In PDF, the "Now
  reading … / Read from here / Back to selected" bar appears when the current
  block differs from the selection, and *Back to selected* returns to it.
- At 1280×560, the layout kept the current block visible on every one of 30
  scroll steps in both a PDF (Think Python, 361 blocks) and an EPUB (Origin).
  Zooming in twice and resizing from 1440×900 to 1280×560 kept the position.
- Rapidly choosing 5–6 blocks ended in the right place for the last choice.
  Scrolling had no long-running browser tasks, and the worst frame gap was
  33–83 ms. No console or page errors appeared apart from development-only
  items (a feature-toggle 404, and the yellow "T" testability button on
  screenshots).
- Reopening a reader resumed the last position (PDF 3.9; EPUB chapter VIII).
  Slice 3 examines resume in depth.
- The DRM refusal is clear by both routes. EPUB upload is quick on the web and
  in the CLI, and the CLI prints the resulting layout.

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
  "Attaching book…" for the whole MinerU run (about 3 minutes for 244 pages). Showing the
  stage and page progress would tell the reader it is working.
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
- **Small visual items:**
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

### Coverage gaps (attach and browse)

- Only headless Chromium with synthetic wheel events was used. Trackpad
  momentum, touch scrolling, other browsers, and perceived flicker were not
  observed; only frame timing was measured.
- PDF attach failures (a corrupt PDF, a MinerU error or timeout) and PDF
  upload through the web (not offered) were not exercised.
- The dark theme was not checked. No image-heavy EPUB was used: this Alice
  edition has only a cover.
- Mobile browsing could not go beyond the first screen because of defect 2.
- Keyboard-only navigation of the layout was not tried.
- Deleting a book through the sidebar file view was not tried, to keep the
  data for slice 3.

### Data left for reading records and reorganizing

- *UAT Attention* is the cleanest start for reading records and AI
  reorganizing: 26 blocks, no records, and a flat hierarchy that AI should
  nest.
- *UAT Think Python* suits manual reorganizing: running-header duplicates to
  cancel and misplaced chapters to outdent. It already has one READ record,
  from heading-only auto-marking during browsing.
- *UAT Origin of Species* and *UAT Alice* each carry the spurious "Contents"
  READ record from defect 7, and each has a saved reading position.

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
