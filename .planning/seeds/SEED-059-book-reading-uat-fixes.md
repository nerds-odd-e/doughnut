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

### Book reading E2E position checks do not sit on a page boundary

**Identity:** SEED-059#story-21
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/002-book-reading-e2e-position-checks/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"131736b7183a651a49c38f4184727bc1a590cb9d70481a50904c27a8ff601e67","plan":"bfef90d66d174e8bff6142c079dfbc7264cee3a20d4aaeab767433e3c0d02215"}}
```

**Goal:** A Donut developer can trust the book reading E2E scenarios to pass or
fail because of reading behavior, not because a page-position assertion sits
within pixels of a page boundary at the scenario's window size. Every scenario
runs at one fixed window size, so the risk is boundary proximity, not a range of
sizes.

**Observed friction**

- The step "the book reader PDF viewport should be on page N" reads the page
  indicator, which shows the page under the middle of the reader. After a PDF
  block lands at its start, the middle can sit within pixels of a page boundary.
  In `phone_reading.feature` (390 × 844) block 2.2's start landed 1.5 px from
  the page 2/3 boundary; CI (run 36647846005) failed at a slightly different
  window, and the repair reproduced the failure at 390 × 900. It was repaired
  there (`5206b08370`) by asserting the start position instead.
- `book_browsing.feature` (lines about 22, 38, 45, 52, 73, 76),
  `reading_record.feature` (line about 42) and `phone_reading.feature` (line
  about 55) use the same step. They pass at their fixed window sizes. Whether any
  sits close to a boundary is unverified.

**Scope**

- **Analysis by arithmetic first.** For every scenario that uses a step reading
  layout geometry (page indicator, scroll offsets, pixel tolerances such as the
  15 px start-at-top check, the 37.8% same-page scroll), take its fixed window
  size and compute the margin between the reader's middle (or other read
  position) and the nearest page boundary. The margin is computed from the
  measured reader height and page height, and the phone case is confirmed once
  locally at 390 × 844 and 390 × 900 to check the method. No multi-size run of
  the suite.
- **Stop when nothing is at risk.** If no scenario other than the already
  repaired one has a small margin, stop with that finding and change no tests.
- **Then repair only what the analysis confirms**, following the pattern already
  used: assert where the block's start is ("the top of the PDF book reader should
  be at Y of 1000 down page N") after landing, and assert the current block after
  scrolling, instead of the page indicator. Keep each scenario's journey and its
  integration proof.
- **Excluded:** a supported-window-size list for E2E; running features at several
  sizes; designing new position-step conventions beyond the existing start
  position step; changing `START_AT_TOP_TOLERANCE_PX`; removing page-indicator
  checks that carry no risk (they are redundant with start-position assertions in
  places, but that is a different outcome); any product change, including the page
  indicator itself.

**Key examples**

- A scenario that chooses a PDF block asserts where the block's start is, and
  passes at 390 × 844 and at 390 × 900.
- A scenario that scrolls past a heading asserts the current block, and does not
  depend on which page holds the middle of the reader.
- The analysis states each scenario's window size and margin, and which are at
  risk.

**Effort hypothesis:** S, moderate confidence (the analysis decides whether any
repair is needed).
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
