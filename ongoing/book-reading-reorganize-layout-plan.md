# Plan: Reorganizing the book layout

**User story:** [`ongoing/book-reading-user-stories.md`](book-reading-user-stories.md) — *Story: Reorganizing the book layout*.

**Architecture:** [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md).

**UX context:** [`ongoing/book-reading-ux-ui-roadmap.md`](book-reading-ux-ui-roadmap.md).

**Planning rule:** `.cursor/rules/planning.mdc` — one user-visible behavior per phase, scenario-first ordering, test-first workflow.

---

## Current state

- Blocks are stored flat in preorder with `layout_sequence` (DB order) and `depth` (nesting level, root = 0).
- Wire format (`GET …/book`): flat `blocks[]` array with `depth` per block; no `layout_sequence` exposed.
- `BookReadingBookLayout.vue` renders a flat `v-for` list with `paddingLeft: depth * 0.75rem` for visual indentation. No tree lines, connectors, or other hierarchy cues beyond left padding.
- **No API exists** to update block depth or order after attach. Layout is immutable once the book is attached.
- No drag-and-drop or keyboard reorder support.

---

## Design notes

### Depth constraints (valid tree invariant)

A flat preorder block list is a valid tree when, for every consecutive pair (A, B):

- `B.depth <= A.depth + 1` (B can be a child of A, a sibling, or an ancestor's sibling, but not deeper than one level below A).
- `depth >= 0` for all blocks.

Every depth mutation must preserve this invariant.

### Single-block vs subtree operations

- **Leaf block (no children):** Indent/outdent changes only the target block's depth by ±1. Rejected if it would violate the tree invariant with either the predecessor or successor. Phases 3–4 use this simpler case.
- **Block with children (general case):** Indent/outdent **always** moves the target block **and all its descendants** (contiguous run of blocks with depth > target's current depth) by ±1 together, preserving internal relative depths. Rejected if the result violates invariant at the boundary. Phase 7 generalizes to this behavior — there is no separate "single-block" vs "subtree" toggle; descendants always follow the head.

### Backend API direction

A single endpoint for depth change:

```
PUT /api/notebooks/{notebook}/book/blocks/{bookBlock}/depth
Body: { "direction": "INDENT" | "OUTDENT" }
```

Returns the updated `Book` (same shape as `GET …/book`) so the client can replace the layout in one round-trip. The backend always moves descendants with the head when they exist — no `withDescendants` flag needed.

### AI reorganization (Phase 11)

Sends the current flat layout (titles + depths) to the AI, receives a suggested depth array, previews diff to the user, applies on confirm. Exact API shape deferred to Phase 11.

---

## Phases

### Phase 1 — Improve book layout nesting visuals

**User value:** Every user sees clearer hierarchy in the book layout sidebar without any editing capability.

**Scenario (E2E):**

```gherkin
# Extend existing "See book layout" scenario in book_browsing.feature
# Verify visual nesting cues are present (e.g. tree-line indicator or distinct indentation styling)
Then each book block in the book layout should show a visual nesting indicator matching its depth
```

**What changes:**

- `BookReadingBookLayout.vue`: add tree-line or connector styling to make parent/child relationships visually obvious (e.g. a thin vertical rule per depth level, or a left-border step pattern). The current `paddingLeft` alone is hard to parse for deep or flat-then-deep transitions.
- CSS only — no backend or API changes.

**E2E:** Extend `book_browsing.feature` to assert depth-related visual cues (e.g. `data-book-block-depth` values already exist; verify a tree-line element or distinct visual pattern appears for nested blocks).

---

### Phase 2 — Focus the selected block in the book layout

**User value:** User can focus a block in the book layout (e.g. by clicking it), and the focus persists while interacting with the block (keyboard shortcuts). Focus blurs only when it should — clicking outside the book layout, navigating away, etc. — not prematurely (e.g. not on every scroll or PDF interaction while the user is still editing layout).

**Scenario (E2E):**

```gherkin
Scenario: Focus a book block in the layout
  When I select the book block "2. The Usual Defi nition Is Not Enough" in the book layout
  Then the book block "2. The Usual Defi nition Is Not Enough" should be focused in the book layout
```

**What changes:**

- **Frontend:** Ensure book block items in the layout are focusable (`tabindex` or native button/link). Clicking a block focuses it (DOM focus, not just "selected" highlight). Focus is retained while the user presses keyboard shortcuts (Tab, Shift+Tab) on the layout — it should not blur until the user explicitly moves focus elsewhere (click outside, tab out of the layout list, etc.).
- No backend changes.

**Why a separate phase:** Tab/Shift+Tab (Phases 3–4) rely on a focused block to know which block to indent/outdent. Getting focus management right (stable focus that survives API round-trips and re-renders) is its own testable behavior.

---

### Phase 3 — Indent a leaf block via keyboard (tab)

**Precondition:** The block has **no children**. This phase keeps the scenario simple; Phase 7 generalizes to always move descendants with the head.

**User value:** User can increase the nesting depth of a single leaf block by pressing Tab while it is focused, correcting flat structures one block at a time.

**Scenario (E2E):**

```gherkin
Scenario: Indent a leaf block in the book layout
  # (uses existing background with attached refactoring.pdf)
  # The block used here has no children in the test fixture
  Given the book layout shows block "2. The Usual Defi nition Is Not Enough" at depth 0
  When I select the book block "2. The Usual Defi nition Is Not Enough" in the book layout
  And I press Tab on the book layout
  Then the book block "2. The Usual Defi nition Is Not Enough" should be at depth 1 in the book layout
```

**What changes:**

- **Backend:** New endpoint `PUT /api/notebooks/{notebook}/book/blocks/{bookBlock}/depth` accepting `{ "direction": "INDENT" }`. Validates depth constraint against predecessor and successor. Updates `depth` column. Returns updated `Book`.
- **Frontend:** On `keydown.tab` (with `preventDefault`) while a block is focused in the layout, call the indent API and refresh the book data. Focus must remain on the same block after re-render.
- **Validation:** Reject if the block is already at predecessor's depth + 1 (can't nest deeper), or if it is the first block (no predecessor to be child of).

**Sub-phases (E2E-led):**

1. Red: add the E2E scenario (indent step fails — no API). Green: implement backend endpoint + frontend tab handler for indent.

---

### Phase 4 — Outdent a leaf block via keyboard (shift-tab)

**Precondition:** The block has **no children**. This phase keeps the scenario simple; Phase 7 generalizes to always move descendants with the head.

**User value:** User can decrease the nesting depth of a single leaf block by pressing Shift+Tab while it is focused, pulling it up in the hierarchy.

**Scenario (E2E):**

```gherkin
Scenario: Outdent a leaf block in the book layout
  # The block used here has no children in the test fixture
  Given the book layout shows block "3.1 Can You Refactor Without Tests?" at depth 1
  When I select the book block "3.1 Can You Refactor Without Tests?" in the book layout
  And I press Shift+Tab on the book layout
  Then the book block "3.1 Can You Refactor Without Tests?" should be at depth 0 in the book layout
```

**What changes:**

- **Backend:** Same endpoint, `{ "direction": "OUTDENT" }`. Validates: depth > 0, and successor's depth <= new depth + 1 (or reject).
- **Frontend:** `keydown.tab` with `shiftKey` calls outdent. Focus must remain on the same block after re-render.

**Sub-phases (E2E-led):**

1. Red: add outdent scenario (fails — outdent not implemented). Green: extend endpoint + handler for outdent direction.

---

### Phase 5 — Drag a block left/right to change depth

**User value:** More discoverable and touch-friendly alternative to keyboard — user drags a block horizontally to indent (right) or outdent (left).

**Scenario (E2E):**

```gherkin
Scenario: Drag a book block right to indent it
  Given the book layout shows block "2. The Usual Defi nition Is Not Enough" at depth 0
  When I drag the book block "2. The Usual Defi nition Is Not Enough" to the right in the book layout
  Then the book block "2. The Usual Defi nition Is Not Enough" should be at depth 1 in the book layout

Scenario: Drag a book block left to outdent it
  Given the book layout shows block "3.1 Can You Refactor Without Tests?" at depth 1
  When I drag the book block "3.1 Can You Refactor Without Tests?" to the left in the book layout
  Then the book block "3.1 Can You Refactor Without Tests?" should be at depth 0 in the book layout
```

**What changes:**

- **Frontend:** Add horizontal drag gesture detection on each book block row (pointer/touch events with a horizontal threshold). On drag-end past threshold, call the same indent/outdent API from Phases 3–4.
- No backend changes — reuses the existing depth endpoint.

---

### Phase 6 — Drag a block with its descendants

**User value:** When a block has children, dragging it moves the entire section together.

**What changes:**

- Drag applies the same subtree logic as Phase 7 (keyboard). Backend `withDescendants` support is shared.

---

### Phase 7 — Indent/outdent a block with its descendants (subtree move)

**User value:** When a block has children, indenting or outdenting via Tab/Shift+Tab always moves the entire section together instead of requiring one-by-one adjustment. This is the general behavior — Phases 3–4 were the simple leaf-only introduction.

**Scenario (E2E):**

```gherkin
Scenario: Indent a block and its children together
  # Block "3. Refactoring Is Not Only About Changing Production Code" at depth 0
  # has children "3.1 ..." and "3.2 ..." at depth 1
  When I select the book block "3. Refactoring Is Not Only About Changing Production Code" in the book layout
  And I press Tab on the book layout
  Then the book block "3. Refactoring Is Not Only About Changing Production Code" should be at depth 1 in the book layout
  And the book block "3.1 Can You Refactor Without Tests?" should be at depth 2 in the book layout
  And the book block "3.2 Can You Refactor Without Changing the Code?" should be at depth 2 in the book layout
```

**What changes:**

- **Backend:** Extend the depth endpoint: when the target block has descendants (contiguous run of blocks after the target whose depth > target's current depth), always shift all by ±1 together. Validates boundary invariant at predecessor of target and successor of last descendant.
- **Frontend:** Tab/Shift+Tab and drag now always move descendants with the head. No separate "single-block" vs "subtree" toggle needed — descendants always follow.

---

### Phase 8 — Cancel a book block (merge content to previous)

**User value:** User can remove a book block boundary, merging its content into the previous block — useful when the PDF parser created too many structural splits.

**Scenario (E2E):**

```gherkin
Scenario: Cancel a book block and merge to previous
  Given the book layout has blocks "A" at depth 0 and "B" at depth 0
  When I select the book block "B" in the book layout
  And I cancel the book block "B"
  Then the book block "B" should no longer appear in the book layout
  And the content that belonged to "B" should now belong to the previous block "A"
```

**What changes:**

- **Backend:** New endpoint (e.g. `DELETE /api/notebooks/{notebook}/book/blocks/{bookBlock}` or `POST .../merge-to-previous`). Removes the target block, reassigns its `BookContentBlock` rows to the predecessor block, recalculates `allBboxes` for the predecessor. If the cancelled block had children, they become children of the predecessor (or are promoted — decide during implementation). Returns updated `Book`.
- **Frontend:** UI action (context menu, button, or keyboard shortcut) on a focused/selected block to cancel it. Disabled on the first block (no predecessor to merge into).

---

### Phase 9 — Create a book block

**User value:** User can insert a new book block boundary — useful when the PDF parser missed a structural break or the user wants finer granularity.

**Scenario (E2E):**

```gherkin
Scenario: Create a new book block
  When I select a position in the book layout to insert a new block
  And I create a new book block
  Then a new book block should appear at the chosen position in the book layout
```

**What changes:**

- **Backend:** New endpoint (e.g. `POST /api/notebooks/{notebook}/book/blocks`) accepting position info (e.g. after which block, and which content blocks to split off). Creates a new `BookBlock`, reassigns ownership of content blocks from the split point onward. Returns updated `Book`.
- **Frontend:** UI affordance to insert a block (e.g. an "add" action between blocks, or a "split here" on a content boundary). The new block's title is derived from the first text content block it receives.

---

### Phase 10 — Create a book block from a long or untitled content region

**User value:** When the selected content region is too long or has no text suitable as a book block title, the user can still create a block — the system handles the edge case (e.g. prompts for a title, uses a placeholder, or picks the first N characters).

**Scenario (E2E):**

```gherkin
Scenario: Create a book block when content has no heading text
  Given a content region with no text suitable as a title
  When I create a new book block from that region
  Then a new book block should appear with a placeholder or user-supplied title
```

**What changes:**

- **Backend/Frontend:** Extend the create-block flow from Phase 9 to handle: (1) content that is too long to use as a title — truncate or prompt; (2) content with no text at all (e.g. images only) — use a placeholder title or let the user type one. Exact UX to be decided during implementation.

---

### Phase 11 — AI-assisted depth reorganization

**User value:** User can ask AI to automatically fix/suggest the nesting structure for the entire book layout, saving manual block-by-block work.

**Scenario (E2E):**

```gherkin
@usingMockedOpenAiService
Scenario: AI reorganizes the book layout depth
  When I request AI reorganization of the book layout
  Then I should see a preview of the suggested depth changes
  When I confirm the AI suggestion
  Then the book layout should reflect the AI-suggested depths
```

**What changes:**

- **Backend:** New endpoint (e.g. `POST /api/notebooks/{notebook}/book/reorganize-layout`) that sends block titles + current depths to the configured AI service, receives a suggested depth array, and returns it as a preview. A separate confirm endpoint (or the same with a flag) applies the changes.
- **Frontend:** UI control (button in the layout sidebar or a menu option) to trigger AI reorg. Preview shows a diff of current vs suggested depths. Confirm applies; cancel discards.
- Depends on the existing OpenAI service integration pattern used elsewhere in the codebase.

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
