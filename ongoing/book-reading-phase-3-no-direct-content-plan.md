# Plan: Reading record Phase 3 — auto-read when a block has no direct content

**Parent story:** [`ongoing/book-reading-reading-record-plan.md`](book-reading-reading-record-plan.md) — *Phase 3 — Mark a block with no direct content as read automatically*.

**Architecture direction:** [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md) — `BookBlock`, `BookContentBlock`, direct-content defaults, synthetic `*beginning*` block.

**UX context:** [`ongoing/book-reading-ux-ui-roadmap.md`](book-reading-ux-ui-roadmap.md) — the Reading Control Panel should disappear from this path because the block becomes read automatically.

**Planning rule used here:** `.cursor/rules/planning.mdc` — Phase 3 needs one short **structure** sub-phase first because the current attach/import model does not persist the evidence required for the later user-visible E2E behavior. After that, the user-visible auto-read behavior is split into smaller red/green cycles instead of one large swing.

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
- `BookContentBlock` is the imported MinerU `content_list` stream attached to a `BookBlock`.
- Persist queryable columns needed for Phase 3 logic: `type`, page index, bbox, optional `textLevel`, stable order within the owning block.
- Also persist the raw MinerU payload so unknown future block types survive without another schema change.

### 2. Put ownership in the attach-book payload

Extend the CLI attach payload so each layout node carries its owned imported content blocks.

- Keep the existing nested `layout.roots` shape.
- Add ordered `contentBlocks` on each layout node rather than building a second parallel tree.
- The heading item that creates a `BookBlock` stays in that block’s `contentBlocks`; it is persisted but treated as the block’s structural boundary, not as direct body content.

This keeps high cohesion: the CLI already walks MinerU `content_list`, and the backend can validate and persist one coherent import tree atomically.

### 3. Ownership rule while walking MinerU `content_list`

Walk MinerU items in reading order.

- When the item is `type == "text"` and has `text_level`, start a new `BookBlock`.
- Persist that heading item inside the new block’s `contentBlocks`.
- Non-heading items belong to the most recent `BookBlock`.
- If no prior heading exists, Phase 3 still needs a synthetic root block titled `*beginning*`, but that case is intentionally delivered in a later dedicated sub-phase instead of the first import slice.

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

- the heading content block that created the `BookBlock`
- `page_number`
- footnotes
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
- the earlier block owns only its heading content block before the next heading starts
- this keeps the first E2E small and deterministic

---

## Ordered sub-phases

### Sub-phase 3A — Persist grouped imported content blocks

**Why this phase exists:** planning.mdc allows a short structure phase when a later user-visible step needs a large structural change first. Phase 3 cannot be implemented cleanly without durable ownership of imported content.

**User-visible outcome:** none yet; this is the minimum structural slice that makes the later E2E meaningful.

**Changes to plan for this sub-phase:**

- CLI MinerU extraction emits layout nodes with ordered `contentBlocks`.
- Backend `attach-book` validates and persists `BookContentBlock` rows under the owning `BookBlock`.
- For this first slice, keep the importer focused on books where a heading block appears before later content; the orphan-prefix `*beginning*` case is handled in a later dedicated sub-phase.

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

### Sub-phase 3D — Red for “real body text prevents auto-read”

**Goal:** tighten the predicate after the smallest happy path is green.

**Red step:**

- Add focused backend/controller regression coverage showing that a predecessor block with a persisted non-structural `text` content block must **not** auto-mark when the reader reaches the next block.
- Keep the new failure isolated to this one content kind first.

**Why this phase matters:**

- it prevents the initial heading-only implementation from over-marking
- it adds the first blocking content kind with one clear reason for failure

### Sub-phase 3E — Green the `text` blocking rule

**User-visible outcome:** a predecessor block with body text no longer auto-marks just because the reader reached the next block.

**Implementation target:**

- Count non-structural persisted `text` content blocks as direct content.
- Keep the existing heading-only scenario green.

**Tests for this sub-phase:**

- The red regression from 3D passes.
- The earlier heading-only E2E still passes.

### Sub-phase 3F — Red for remaining content-type classification

**Goal:** add the rest of the predicate in one small observable step after `text` works.

**Red step:**

- Add focused regression coverage showing that `table` prevents auto-read.
- Add focused regression coverage showing that `image` prevents auto-read.
- Add focused regression coverage showing that `page_number`, footnotes, and unknown types do **not** prevent auto-read.

**Why this phase matters:**

- it finishes the predicate table without expanding the first green step too far
- it makes future MinerU type drift explicit

### Sub-phase 3G — Green the remaining classification rules

**User-visible outcome:** the no-direct-content rule matches the agreed content-type table for the currently known MinerU block kinds.

**Implementation target:**

- Count `table` and `image` as direct content.
- Ignore `page_number`, footnotes, and unknown types by default.

**Tests for this sub-phase:**

- The red regressions from 3F pass.
- The earlier heading-only E2E still passes.

### Sub-phase 3H — Handle content before the first book block

**Goal:** cover the orphan-prefix import case as a separate phase after the normal auto-read rule is already working.

**User-visible outcome:** when MinerU emits content before the first heading block, the importer creates a synthetic `*beginning*` block so every content block still belongs to a `BookBlock`.

**Implementation target:**

- Create the synthetic root-level `*beginning*` block only when the first imported items appear before any heading block.
- Use the one-line-above synthetic bbox rule already defined in the design section.

**Tests for this sub-phase:**

- Python unit test for the MinerU walker that creates `*beginning*` and uses the one-line-above bbox rule.
- Backend/controller regression proving orphan prefix content persists under that synthetic block.

### Sub-phase 3I — Cleanup and plan fold-back

**Goal:** close the phase cleanly once the orphan-prefix phase is green.

**Checklist:**

- remove obsolete heuristic wording from the parent plan if it no longer matches the shipped rule
- update the parent reading-record plan to mark Phase 3 shipped
- trim this document down or archive it once the parent plan and architecture roadmap fully reflect reality

---

## Risks to watch while implementing later

- Counting the heading content block itself as direct content would make every block look non-empty and break Phase 3.
- Letting unknown MinerU block types count by default would make the rule unstable as MinerU evolves.
- Building a second tree for imported content would duplicate the structural hierarchy and lower cohesion.
- Making the synthetic `*beginning*` block a frontend-only concept would break the “every content block belongs to a book block” invariant at persistence time.
