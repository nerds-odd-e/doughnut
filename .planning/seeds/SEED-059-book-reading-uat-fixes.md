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
  - attach discoverability (web attach card PDF hint, CLI `/help` and access
    token guidance, a reader link after a CLI attach): wording and pointers
    only, with no defect and no technical debt behind them;
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
- **Then** (completing the feature): stories 10 and 12.

Stories are independent unless stated. First to drop: story 12.

## Open Decisions

- Which open question in each story needs the owner before refinement.
- Not queued: place the Reading Control Panel at the real end of an EPUB block's
  text. Today it sits at the bottom of the element its last locator names, else
  the whole spine document, so a last paragraph without an id puts it under the
  block's heading or at the end of a spine file that holds several blocks. Queue
  it if a reader reports a misplaced panel.

## When to Surface

When the owner selects book reading work from the product backlog.

## Breadcrumbs

- UAT report: `4f2f230505:.planning/seeds/SEED-054-book-reading-uat.md`,
  section `## UAT Findings` (screenshots were not committed).
- Supported behavior: `e2e_test/features/book_reading/`.
- PDF extraction: `cli/python/mineru_book_outline.py`,
  `backend/src/main/java/com/odde/donut/services/book/MineruContentListLayoutBuilder.java`.
