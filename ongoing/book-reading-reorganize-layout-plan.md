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

- **Single-block indent/outdent:** changes only the target block's depth by ±1. The operation is rejected if it would violate the tree invariant with either the predecessor or successor.
- **Subtree indent/outdent:** changes the target block **and all its descendants** (contiguous run of blocks with depth > target's current depth). All move by ±1 together, preserving internal relative depths. Rejected if the result violates invariant at the boundary (predecessor of the target, or successor of the last descendant).

### Backend API direction

A single endpoint for depth change:

```
PUT /api/notebooks/{notebook}/book/blocks/{bookBlock}/depth
Body: { "direction": "INDENT" | "OUTDENT", "withDescendants": false }
```

Returns the updated `Book` (same shape as `GET …/book`) so the client can replace the layout in one round-trip. `withDescendants` added in Phase 4.

### AI reorganization (Phase 6)

Sends the current flat layout (titles + depths) to the AI, receives a suggested depth array, previews diff to the user, applies on confirm. Exact API shape deferred to Phase 6.

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

### Phase 2 — Indent a single block via keyboard (tab)

**User value:** User can increase the nesting depth of a single block by pressing Tab while it is selected, correcting flat structures one block at a time.

**Scenario (E2E):**

```gherkin
Scenario: Indent a single block in the book layout
  # (uses existing background with attached refactoring.pdf)
  Given the book layout shows block "2. The Usual Defi nition Is Not Enough" at depth 0
  When I select the book block "2. The Usual Defi nition Is Not Enough" in the book layout
  And I press Tab on the book layout
  Then the book block "2. The Usual Defi nition Is Not Enough" should be at depth 1 in the book layout
```

**What changes:**

- **Backend:** New endpoint `PUT /api/notebooks/{notebook}/book/blocks/{bookBlock}/depth` accepting `{ "direction": "INDENT" }`. Validates depth constraint against predecessor and successor. Updates `depth` column. Returns updated `Book`.
- **Frontend:** On `keydown.tab` (with `preventDefault`) while a block is focused/selected in the layout, call the indent API and refresh the book data.
- **Validation:** Reject if the block is already at predecessor's depth + 1 (can't nest deeper), or if it is the first block (no predecessor to be child of).

**Sub-phases (E2E-led):**

1. Red: add the E2E scenario (indent step fails — no API). Green: implement backend endpoint + frontend tab handler for indent.

---

### Phase 3 — Outdent a single block via keyboard (shift-tab)

**User value:** User can decrease the nesting depth of a single block by pressing Shift+Tab, pulling it up in the hierarchy.

**Scenario (E2E):**

```gherkin
Scenario: Outdent a single block in the book layout
  Given the book layout shows block "3.1 Can You Refactor Without Tests?" at depth 1
  When I select the book block "3.1 Can You Refactor Without Tests?" in the book layout
  And I press Shift+Tab on the book layout
  Then the book block "3.1 Can You Refactor Without Tests?" should be at depth 0 in the book layout
```

**What changes:**

- **Backend:** Same endpoint, `{ "direction": "OUTDENT" }`. Validates: depth > 0, and successor's depth <= new depth + 1 (or reject).
- **Frontend:** `keydown.tab` with `shiftKey` calls outdent.

**Sub-phases (E2E-led):**

1. Red: add outdent scenario (fails — outdent not implemented). Green: extend endpoint + handler for outdent direction.

---

### Phase 4 — Drag a block left/right to change depth

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

- **Frontend:** Add horizontal drag gesture detection on each book block row (pointer/touch events with a horizontal threshold). On drag-end past threshold, call the same indent/outdent API from Phases 2–3.
- No backend changes — reuses the existing depth endpoint.

---

### Phase 5 — Indent/outdent a block with its descendants (subtree move)

**User value:** When a block has children, indenting or outdenting moves the entire section together instead of requiring one-by-one adjustment.

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

- **Backend:** Extend the depth endpoint to accept `{ "direction": "INDENT", "withDescendants": true }`. Identifies descendants as the contiguous run of blocks after the target whose depth > target's current depth. Shifts all by ±1. Validates boundary invariant at predecessor of target and successor of last descendant.
- **Frontend:** Default behavior for tab/shift-tab and drag becomes "with descendants." Single-block mode could be an option (e.g. holding Alt) or could be dropped if subtree is always preferable — decide during implementation.

---

### Phase 6 — AI-assisted depth reorganization

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
