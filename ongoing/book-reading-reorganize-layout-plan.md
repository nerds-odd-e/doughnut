# Plan: Reorganizing the book layout

**User story:** [`ongoing/book-reading-user-stories.md`](book-reading-user-stories.md) — *Story: Reorganizing the book layout*.

**Architecture:** [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md).

**UX context:** [`ongoing/book-reading-ux-ui-roadmap.md`](book-reading-ux-ui-roadmap.md).

**Planning rule:** `.cursor/rules/planning.mdc` — one user-visible behavior per phase, scenario-first ordering, test-first workflow.

---

## Current state

- Blocks are stored flat in preorder with `layout_sequence` (DB order) and `depth` (nesting level, root = 0).
- Wire format (`GET …/book`): flat `blocks[]` array with `depth` per block; no `layout_sequence` exposed.
- `BookReadingBookLayout.vue` renders a flat `v-for` list with `paddingLeft: depth * 0.75rem` for visual indentation.
- Indent/outdent via Tab/Shift+Tab and horizontal drag are shipped (leaf + subtree).
- Cancel (Backspace) is shipped.
- **Create block from content (Phases 9–10)** was shipped with a **content stream panel** below the PDF — a textual list of imported `contentBlocks` with long-press on a text row. **This approach is being replaced:** long-press should happen on the **content block bbox overlay in the PDF viewer**, not on a separate text panel. The stream panel and the `raw` field on `BookContentBlock` in the API are being removed.
- AI-assisted depth reorganization (Phase 11) is shipped.

---

## Design notes

### Depth constraints (valid tree invariant)

A flat preorder block list is a valid tree when, for every consecutive pair (A, B):

- `B.depth <= A.depth + 1` (B can be a child of A, a sibling, or an ancestor's sibling, but not deeper than one level below A).
- `depth >= 0` for all blocks.

Every depth mutation must preserve this invariant.

### Backend API direction

A single endpoint for depth change:

```
PUT /api/notebooks/{notebook}/book/blocks/{bookBlock}/depth
Body: { "direction": "INDENT" | "OUTDENT" }
```

Returns the updated `Book` (same shape as `GET …/book`) so the client can replace the layout in one round-trip.

### New approach for creating a book block from content

**Trigger:** Long-press (click-and-hold) on a **content block bbox** rendered as an overlay in the **PDF viewer** (the semi-transparent rectangles that appear when a book block is selected). Not a separate text panel below the PDF.

**Why:** The bboxes already show the user exactly where each imported content block sits on the page. Long-pressing on the actual content in context is more natural than finding a row in a separate list below the PDF.

**Content block text for title:** When the user confirms "New block" and the source content is long enough to need a title prompt, the frontend queries the content block's text from the backend on demand (e.g. a lightweight endpoint or by including minimal text in the `allBboxes` / content block wire shape). The full `raw` JSON string does **not** need to be shipped to every client on `GET …/book`.

---

## Shipped phases (summary)

| Phase | Description |
|-------|-------------|
| 1 | Improve book layout nesting visuals |
| 2 | Focus the selected block in the book layout |
| 3 | Indent a leaf block via keyboard (Tab) |
| 4 | Outdent a leaf block via keyboard (Shift+Tab) |
| 5 | Drag a block left/right to change depth |
| 6 | Indent/outdent a block with its descendants (subtree move) |
| 7 | Drag a block with its descendants |
| 8 | Cancel a book block (merge content to previous) |
| 9 (old) | Create a book block from content stream panel (long-press on text row) — **being replaced** |
| 10 (old) | Title entry when source content is long (in stream panel) — **being replaced** |
| 11 | AI-assisted depth reorganization |

---

## Phases

### Phase 12 — Remove content stream panel and `raw` from the API

**User value:** Cleaner API response (no bulky `raw` JSON string per content block shipped to every client), simpler reader layout (no extra panel below the PDF).

**What to remove:**

Frontend:
- `BookReadingContentStreamPanel.vue` — the entire component
- `contentBlockRawPreview.ts` — preview text helper (only used by stream panel)
- `contentBlockStructuralTitleSource.ts` — title source helper (only used by stream panel)
- `BookReadingContentStreamPanel.spec.ts` — unit tests for the panel
- `contentBlockStructuralTitleSource.spec.ts` — unit tests for the title helper
- All references in `BookReadingContent.vue`: the import, the `<BookReadingContentStreamPanel>` template usage, and the `onCreateBlockFromContent` handler
- E2E scenarios in `reorganize_layout.feature` under "Rule: Create a new book block from a content block via long-press" (both scenarios)
- E2E step definitions in `book_reading.ts` for long-press / callout / title dialog
- E2E page object methods in `bookReadingPage.ts`: `pressAndHoldThirdContentStreamBlock`, `expectNewBlockCalloutVisible`, `clickContentStreamNewBlockConfirm`, `expectNewBlockTitleDialogVisible`, `enterNewBlockTitleAndConfirm`

Backend:
- Remove `raw` from the `BookContentBlock` JSON serialization (drop `@JsonProperty("raw")` / `@JsonView` on `rawData`, or exclude it from the `Full` view). Keep the DB column and server-side usage of `rawData` (bbox derivation, direct-content predicate, split title derivation all need it).
- Update OpenAPI spec and regenerate TypeScript (`pnpm generateTypeScript`).

Backend tests:
- Update any controller test assertions that check for `contentBlocks[].raw` in the wire JSON.

**Verification:** Existing E2E tests for depth changes, cancel, and AI reorg still pass. The two removed long-press scenarios no longer exist.

---

### Phase 13 — Make content block bboxes persistent overlays on selection

**User value:** When a book block is selected, the user sees **persistent** bbox overlays on the PDF for each content block owned by that block — not just a brief highlight that fades out. These overlays are the interaction surface for the upcoming long-press.

**Current state:** `attachBookBlockSelectionBboxHighlight` creates overlays that **fade out** after 2 seconds. For long-press, the overlays need to remain visible as long as the block is selected.

**What changes:**

- **Frontend (`PdfBookViewer.vue` / `bookBlockSelectionBboxHighlight.ts`):** When showing selection bbox highlights, keep them visible (no fade-out) until the selection changes or clears. Each overlay gets a `data-book-content-block-id` attribute so it can be identified for long-press events. The overlay `pointerEvents` changes from `none` to `auto` so pointer events can be captured.
- **Wire shape:** Each entry in `allBboxes` needs to carry the `BookContentBlock.id` so the frontend can map an overlay to a content block id for the split API. Add `contentBlockId` to `PageBbox` on the wire (or a parallel field). The first bbox (structural heading) may have no content block id.

**Scenario (E2E):**

```gherkin
Scenario: Content block bboxes are visible while a block is selected
  When I choose the book block "1. Refactoring: Protecting Intention in Working Software"
  Then I should see content block bbox overlays on the PDF
```

---

### Phase 14 — Long-press on a content block bbox in the PDF to create a new block

**User value:** User can click-and-hold on a content block bbox overlay in the PDF viewer, see a callout with "New block", confirm, and see the layout update — all without leaving the PDF reading surface.

**What changes:**

- **Frontend (`PdfBookViewer.vue`):** Pointer hold (same duration threshold / movement tolerance pattern) on a bbox overlay element. On hold completion, show a callout (popover / floating card) near the held overlay with "New block" + "Cancel". On confirm, call `NotebookBooksController.createBookBlockFromContent` with `{ fromBookContentBlockId }` from the overlay's `data-book-content-block-id`. Refresh the book and select the new block.
- **Title prompt:** After "New block" confirm, the backend can respond with a flag or the frontend can query the content block's text to decide whether a title prompt is needed. Alternatively, the create-block API returns the new block's derived title and the frontend only prompts when the backend signals truncation. Details deferred to implementation.

**Scenario (E2E):**

```gherkin
Scenario: Create a new book block from a content block bbox via long-press
  When I choose the book block "1. Refactoring: Protecting Intention in Working Software"
  And I press and hold on a content block bbox overlay in the PDF
  Then I should see the "New block" callout near the held overlay
  When I confirm creating a new block
  Then a new book block should appear as a child of the block that owned that content
```

---

### Phase 15 — Title entry when source content is long (PDF bbox flow)

**User value:** Same as old Phase 10 but triggered from the PDF bbox long-press flow. When the content block text exceeds the title length threshold, the user is prompted to name the new block.

**What changes:**

- **Frontend:** After "New block" confirm on a long content block, show a title dialog (same UX as before — modal with pre-filled truncated text). The frontend gets the text either from a lightweight API call or from the create-block API response.
- **Backend:** The create-block API already accepts optional `structuralTitle`. May need a small endpoint or response field to communicate the derived title / "needs override" signal when the source text is too long.

**Scenario (E2E):**

```gherkin
Scenario: Create a book block from long content bbox with a typed title
  When I choose the book block "1. Refactoring: Protecting Intention in Working Software"
  And I press and hold on a long-text content block bbox overlay in the PDF
  Then I should see the "New block" callout
  When I confirm creating a new block
  Then I should be prompted to enter a title defaulting to truncated content
  When I confirm the title
  Then a new book block should appear with the supplied title as a child of the owning block
```

---

## Phase discipline

After each phase:

1. Add or extend the E2E test first; confirm the failure is for the right reason.
2. Implement the smallest change that makes the phase green.
3. Remove dead code and temporary scaffolding.
4. Update this plan and, when relevant, the architecture or UX roadmap.

---

## Document maintenance

Keep this file forward-looking. Shipped implementation detail lives in code, tests, and the architecture/UX roadmaps.
