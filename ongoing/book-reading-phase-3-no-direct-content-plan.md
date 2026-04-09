# Plan: Reading record Phase 3 — auto-read when a block has no direct content

**Parent story:** [`ongoing/book-reading-reading-record-plan.md`](book-reading-reading-record-plan.md) — *Phase 3 — Mark a block with no direct content as read automatically*.

**Architecture direction:** [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md) — `BookBlock`, `BookContentBlock`, direct-content defaults, synthetic `*beginning*` block.

**UX context:** [`ongoing/book-reading-ux-ui-roadmap.md`](book-reading-ux-ui-roadmap.md) — the Reading Control Panel should disappear from this path because the block becomes read automatically.

**Planning rule used here:** `.cursor/rules/planning.mdc` — Phase 3 needs one short **structure** sub-phase first because the current attach/import model does not persist the evidence required for the later user-visible E2E behavior. After that, user-visible auto-read ships in **3C**, followed by a second **structure** slice (**3D**: stop persisting the duplicate heading row), then a **red / green** pair (**3E** / **3F**) for the **type-based** direct-content predicate with **table-driven unit tests** as the predicate’s spec. Orphan-prefix `*beginning*` and cleanup remain **3G** and **3H**.

**This file is a plan only** — do not execute it here.

---

## Problem

Phase 2 can mark a selected `BookBlock` as read only when the user presses the Reading Control Panel action.

Phase 3 needs a different trigger:

- the system must know which imported MinerU content belongs to each `BookBlock`
- a block with **no direct content** must auto-mark as read when the reader enters its immediate successor in reading order
- content that appears before the first heading still needs an owning `BookBlock`

The current attach-book contract only persists the structural `layout.roots` tree, so Phase 3 does not yet have durable import evidence.

---

## Design direction

### 1. Keep `BookBlock` structural; persist imported content separately

Introduce a persisted `BookContentBlock` artifact owned by exactly one `BookBlock`.

- `BookBlock` remains the unit for navigation, selection, current-block logic, and `ReadingRecord`.
- `BookContentBlock` is the imported MinerU **body** items for that `BookBlock` (after **3D**, excluding the heading row that is already on `BookBlock`).
- Persist queryable columns needed for Phase 3 logic: `type`, page index, bbox, optional `textLevel`, stable order within the owning block.
- Also persist the raw MinerU payload so unknown future block types survive without another schema change.

### 2. Put ownership in the attach-book payload

Extend the CLI attach payload so each layout node carries its owned imported content blocks.

- Keep the existing nested `layout.roots` shape.
- Add ordered `contentBlocks` on each layout node rather than building a second parallel tree.
- **After sub-phase 3D:** the heading MinerU item that creates a `BookBlock` is **not** duplicated into that block’s `contentBlocks`. Structural boundary and display stay on `BookBlock` (`title`, `startAnchor`). `contentBlocks` holds only the **body stream** for that section (non-heading items in reading order). **Before 3D (current shipped interim):** the heading row was still persisted as the first `BookContentBlock` and excluded from the direct-content heuristic via the `size > 1` shortcut.

This keeps high cohesion: the CLI already walks MinerU `content_list`, and the backend can validate and persist one coherent import tree atomically.

### 3. Ownership rule while walking MinerU `content_list`

Walk MinerU items in reading order.

- When the item is `type == "text"` and has `text_level`, start a new `BookBlock`.
- **After sub-phase 3D:** do **not** append that heading item to the new block’s `contentBlocks` (only `BookBlock` fields carry the heading). **Before 3D:** the heading was also the first persisted `BookContentBlock`.
- Non-heading items belong to the most recent `BookBlock`.
- If no prior heading exists, Phase 3 still needs a synthetic root block titled `*beginning*`, but that case is intentionally delivered in **sub-phase 3G** instead of the first import slice.

### 4. Synthetic `*beginning*` block anchor

Use the first orphan content block to synthesize the block’s start anchor.

- Source page: same `page_idx` as the orphan content block.
- Source bbox: if the orphan block bbox is `[x0, y0, x1, y1]`, line height is `y1 - y0`.
- Synthetic bbox: `[x0, max(0, y0 - lineHeight), x1, y0]`.

That gives the synthetic block a real start anchor just above the first owned content, keeps the ownership rule total, and gives a concrete unit-testable contract. The design is fixed early, but the implementation lands in its own later sub-phase.

### 5. Default no-direct-content rule

A `BookBlock` has **no direct content** when it owns no **non-structural** imported content block whose `type` is one of:

- `text`
- `table`
- `image`

The following do **not** count by default:

- structural heading text (on `BookBlock` only after sub-phase 3D; before 3D, the duplicate heading row persisted as a `BookContentBlock` must still be excluded from the count)
- `page_number`
- footnotes (`page_footnote` in the committed fixture; confirm naming in MinerU outputs during type research)
- unknown future types

Unknown types are still persisted. They are simply ignored by the Phase 3 predicate until product logic opts them in later.

### 6. Where the auto-read decision lives

Keep the final auto-read decision on the server side, backed by persisted content ownership.

- The CLI/import path is responsible for grouping and uploading content blocks.
- The backend is responsible for persisting them and using them for the Phase 3 “no direct content” predicate.
- The reader continues to react to server-backed `ReadingRecord` state the same way it already does in Phase 2.

---

## E2E scenario to drive Phase 3

Use the committed `refactoring.pdf` / MinerU fixture and replace the placeholder story text with a real consecutive pair that has no direct content.

Suggested pair from the current fixture:

- selected block: `2. The Usual Defi nition Is Not Enough`
- next current block: `2.1 Easier to Change—and Harder to Misuse`

Reason:

- they are consecutive in reading order
- the earlier block has **no** body stream before the next heading starts (interim: only a persisted heading row; after sub-phase **3D**: empty `contentBlocks` for that block)
- this keeps the first E2E small and deterministic

---

## Ordered sub-phases

### Sub-phase 3A — Persist grouped imported content blocks

**Why this phase exists:** planning.mdc allows a short structure phase when a later user-visible step needs a large structural change first. Phase 3 cannot be implemented cleanly without durable ownership of imported content.

**User-visible outcome:** none yet; this is the minimum structural slice that makes the later E2E meaningful.

**Changes to plan for this sub-phase:**

- CLI MinerU extraction emits layout nodes with ordered `contentBlocks`.
- Backend `attach-book` validates and persists `BookContentBlock` rows under the owning `BookBlock`.
- For this first slice, keep the importer focused on books where a heading block appears before later content; the orphan-prefix `*beginning*` case is handled in **sub-phase 3G**.

**Tests for this sub-phase:**

- CLI/TypeScript contract test that accepts the richer stdout JSON shape.
- Backend controller/persistence test proving attach-book round-trips persisted content ownership.

**Done when:**

- importing a book persists both the structural block tree and grouped imported content blocks
- no reader behavior changes yet

### Sub-phase 3B — E2E red for the smallest no-direct-content scenario

**Goal:** add the smallest real browser scenario before implementing the behavior.

**Red step:**

- Extend `e2e_test/features/book_reading/reading_record.feature` with the concrete “no direct content” scenario using the fixture pair above.
- Keep only the prefix through the first failing assertion enabled.
- Confirm exactly one failure: the earlier block is not yet auto-marked as read.

**Why this phase matters:**

- it proves the user-visible behavior is missing for the right reason
- it locks the smallest intended contract before server logic changes

### Sub-phase 3C — Green auto-read for heading-only predecessor blocks

**User-visible outcome:** when the reader enters block `B`, block `A` is automatically shown as read if `A` owns only its structural heading content block and no direct content.

**Implementation target:**

- Use persisted `BookContentBlock` ownership to answer the predicate.
- Exclude the structural heading content block from the direct-content count.
- Reuse existing `ReadingRecord` persistence and book-layout styling from Phase 2.

**Tests for this sub-phase:**

- The new Cypress scenario passes.
- Focused backend regression tests prove a heading-only predecessor block auto-marks.

### Sub-phase 3D — Omit structural heading from persisted `BookContentBlock` rows

**Why this phase exists:** the heading MinerU item is already represented on `BookBlock` (`title`, `startAnchor`). Persisting it again as the first `BookContentBlock` duplicates structure and forced interim heuristics such as `contentBlocks.size() > 1`. Removing it tightens the model: `contentBlocks` is **only** the imported body stream for that section.

**User-visible outcome:** none by itself if the type-based predicate (3F) still matches prior behavior; users should see the same auto-read and panel behavior for existing fixture paths.

**Implementation target:**

- **CLI / Python:** in `layout_roots_with_content_blocks` (or equivalent), stop placing the heading dict into each node's `contentBlocks`; still use that item to create the node, `title`, and `startAnchor`.
- **Backend:** accept attach payloads where `contentBlocks` contains **no** heading row; keep validating ordered body items. Existing DB rows from older imports may still have a leading heading row until re-import (document; optional one-off migration out of scope unless product requires it).
- **Predicate interim:** until 3F ships, adjust `hasDirectContent` so heading-only blocks stay `false` when `contentBlocks` is empty (after 3D) — do not regress 3C E2E.

**Tests for this sub-phase:**

- CLI / contract test: layout nodes have `contentBlocks` without the heading item.
- Backend attach round-trip: persisted `BookContentBlock` list for a heading-only section is empty (or matches new contract).
- Existing Cypress reading-record scenarios still pass after 3D + interim predicate.

### Sub-phase 3E — Research MinerU `type` values; red tests for the type-based direct-content predicate

**Goal:** replace the interim `size > 1` (and any heading-skip logic) with the **agreed type table** from design §5, with **black-box unit tests** as the primary specification of the predicate.

**Research (deliverable in this plan, not a separate doc):** update the **MinerU content types (working notes)** subsection below after reviewing:

- committed fixture [`e2e_test/fixtures/book_reading/mineru_output_for_refactoring.json`](e2e_test/fixtures/book_reading/mineru_output_for_refactoring.json) — types observed there include at least `text`, `table`, `image`, `page_number`, `page_footnote`
- MinerU / PDF-Extract-Kit documentation and sample `content_list` outputs for **other** `type` strings (e.g. equations, code blocks, discarded regions) and whether product should opt them into "direct content" later

**Default after research (unless product overrides in the same delivery):** count toward direct content only persisted blocks whose `type` is **`text`** (body text: `type == "text"` **and** not a heading — e.g. no `text_level` in 1–3, matching the walker's heading rule), **`table`**, or **`image`**. Do **not** count `page_number`, footnotes, or unknown types.

**Red step (planning.mdc):**

- Add a **small pure unit-test suite** (inputs → `hasDirectContent` or equivalent boolean) covering the full matrix: body `text`, `table`, `image` → true; `page_number`, `page_footnote`, unknown `type`, empty list → false; heading-like `text` rows should not appear in persisted lists after 3D — if legacy rows exist, define one explicit test case for "ignore heading-shaped first row" or document migration-only.
- Add or extend **controller / persistence** tests if needed so the observable `GET …/book` flag stays aligned with the same predicate.

**Why this phase matters:**

- locks the predicate to **types**, not row count
- makes MinerU drift visible in one table-driven test file

### Sub-phase 3F — Green the type-based direct-content predicate

**User-visible outcome:** `hasDirectContent` on `GET …/book` matches design §5 for known MinerU kinds; auto-read and Reading Control Panel behavior stay correct for the heading-only E2E and the manual mark-read E2E.

**Implementation target:**

- Implement the predicate in one place (e.g. service or small dedicated type) used by `BookBlock` serialization or `BookService` enrichment — **avoid** scattering `if (type…)` across layers.
- Remove interim `contentBlocks.size() > 1` logic once 3D + this phase are green.

**Tests for this sub-phase:**

- All **unit tests** added in 3E pass.
- Heading-only and mark-read Cypress scenarios pass.
- Optional: one focused controller assertion per "surprising" type (`table` / `image` / footnote) if not already covered by the unit suite.

### Sub-phase 3G — Handle content before the first book block

**Goal:** cover the orphan-prefix import case as a separate phase after the normal auto-read rule is already working.

**User-visible outcome:** when MinerU emits content before the first heading block, the importer creates a synthetic `*beginning*` block so every content block still belongs to a `BookBlock`.

**Implementation target:**

- Create the synthetic root-level `*beginning*` block only when the first imported items appear before any heading block.
- Use the one-line-above synthetic bbox rule already defined in the design section.

**Tests for this sub-phase:**

- Python unit test for the MinerU walker that creates `*beginning*` and uses the one-line-above bbox rule.
- Backend/controller regression proving orphan prefix content persists under that synthetic block.

### Sub-phase 3H — Cleanup and plan fold-back

**Goal:** close the phase cleanly once the orphan-prefix phase is green.

**Checklist:**

- remove obsolete heuristic wording from the parent plan if it no longer matches the shipped rule
- update the parent reading-record plan to mark Phase 3 shipped
- trim this document down or archive it once the parent plan and architecture roadmap fully reflect reality
- refresh [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md) if `BookContentBlock` no longer duplicates the heading row

---

## MinerU content types (working notes)

**Purpose:** single place to record findings during sub-phase **3E** (update this subsection when that phase runs; do not rely on chat-only notes).

**Observed in [`e2e_test/fixtures/book_reading/mineru_output_for_refactoring.json`](e2e_test/fixtures/book_reading/mineru_output_for_refactoring.json):** `text`, `table`, `image`, `page_number`, `page_footnote`.

**To research and list here:** any additional `type` values from MinerU/PDF-Extract-Kit docs or real `content_list` dumps (e.g. code blocks, equations, figures, headers/footers). For each, note: **default counts as direct content?** (yes / no / product decision pending) and **rationale**.

**Default product stance until overridden:** only `text` (non-heading body), `table`, and `image` count; everything else in the research table defaults to **no** unless explicitly promoted later.

---

## Risks to watch while implementing later

- After sub-phase **3D**, treating an **empty** `contentBlocks` list as "unknown" instead of "no direct content" would break heading-only auto-read; align interim `hasDirectContent` with the attach contract in the same delivery.
- Counting a duplicate heading row as direct content (legacy DB or pre-3D imports) would make every block look non-empty and break Phase 3.
- Letting unknown MinerU block types count by default would make the rule unstable as MinerU evolves.
- Building a second tree for imported content would duplicate the structural hierarchy and lower cohesion.
- Making the synthetic `*beginning*` block a frontend-only concept would break the “every content block belongs to a book block” invariant at persistence time.
