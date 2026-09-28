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
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
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
