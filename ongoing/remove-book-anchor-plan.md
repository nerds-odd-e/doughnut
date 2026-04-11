# Remove BookAnchor concept

**Goal:** Eliminate `startAnchor` / `BookAnchor` from the codebase entirely. The information `startAnchor` carried is already available via `allBboxes[0]` (same underlying source: first content block's `rawData`), and identity (`startAnchor.id`) is always equal to `block.id`.

**Related:** [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md), [`ongoing/book-reading-ux-ui-roadmap.md`](book-reading-ux-ui-roadmap.md)

---

## Current state

- **DB:** `book_anchor` table and `start_anchor_value` column are already dropped (V300000139, V300000140).
- **Backend:** `BookBlock.getStartAnchor()` is `@Transient` — synthesizes `BookAnchorFullWire(getId(), contentBlocks.getFirst().getRawData())`. `allBboxes` is built from the same content blocks. Both are on the wire via `@JsonView(BookViews.Full.class)`.
- **Frontend:** Uses `startAnchor.id` for block identity (current-block highlight, selection matching, dwell-select, live announcements, snap-back) and `startAnchor.value` for navigation (`parsePdfOutlineV1Anchor`). `allBboxes[0]` already comes from the same source as `startAnchor.value`.
- **CLI:** Python layout builder does not emit `startAnchor` on nodes. CLI TS test fixtures still include `startAnchor` in attach-book shaped payloads.
- **Docs:** Architecture roadmap, UX roadmap, research report, and reading-record plan reference `BookAnchor`.

### Key equivalences

| `startAnchor` usage | Replacement |
|---|---|
| `startAnchor.id` | `block.id` (always the same value) |
| `startAnchor.value` (JSON string) | `allBboxes[0]` as `PageBboxFull` (already structured; same underlying data) |
| `parsePdfOutlineV1Anchor(anchor)` | first element of `wireItemsToNavigationTargets(block.allBboxes)`, or direct use of `allBboxes[0]` fields |

### Test data concern

Current frontend unit tests may have `startAnchor` values that are inconsistent with `allBboxes` (or `allBboxes` may lack a proper first element). When switching navigation from `startAnchor.value` → `allBboxes[0]`, test fixtures need updating so that `allBboxes[0]` carries the same page/bbox info that `startAnchor.value` used to provide.

---

## Phase 1 — Frontend: replace `startAnchor.id` with `block.id` for identity

**User-visible change:** None (values are identical). Decouples block identity from the anchor concept.

**Scope:**

- `BookReadingBookLayout.vue`: `block.startAnchor.id === currentBlockAnchorId` → `block.id === currentBlockId`; `aria-current` check likewise.
- `BookReadingContent.vue`: all `r.startAnchor.id` comparisons → `r.id`; rename `currentBlockAnchorId` ref → `currentBlockId` throughout.
- `useBookReadingBlockSelection.ts`: `r.startAnchor.id === still` → `r.id === still`; remove `startAnchor` from `BookBlockRowForSelection` type (only `id` and `allBboxes` needed).
- `currentBlockLiveAnnouncement.ts`: `r.startAnchor.id === anchorId` → `r.id === blockId`; simplify `BookBlockRowForLiveAnnouncement` type (drop `startAnchor`, keep `id` + `title`).
- Rename `currentBlockAnchorId` prop on `BookReadingBookLayout.vue` → `currentBlockId`.
- Update unit tests: `currentBlockLiveAnnouncement.spec.ts`, `BookReadingPage.spec.ts` (identity-related assertions).
- After changes, run affected frontend unit tests + E2E `book_browsing.feature` and `reading_record.feature` to confirm no regression.

---

## Phase 2 — Frontend: replace `startAnchor.value` with `allBboxes[0]` for navigation and viewport detection

**User-visible change:** None (same underlying data). Frontend no longer references `startAnchor` at all.

There are four production `startAnchor` usage sites in the frontend, each addressed by a sub-phase. The sub-phases follow E2E-led decomposition: each is independently deployable, has no dead code at its boundary, and is verified by E2E.

### Sub-phase 2a (RED) — Rewrite `currentBlockAnchorFromAnchorPage` unit tests for structured input

Rewrite `currentBlockAnchorFromAnchorPage.spec.ts` to pass structured `{ id, firstBbox }` objects (matching `PageBboxFull` shape) instead of `BookAnchorFull`. The spec currently uses `makeMe.aBookAnchor` and `makeMe.bookReadingTopMathsLikeAnchors()` — replace with inline `{ id, firstBbox: { pageIndex, bbox } }` arrays.

- Tests **fail** (function still expects `BookAnchorFull[]`).
- E2E: existing `book_browsing.feature` still passes (no production change).
- No dead code.

### Sub-phase 2b (GREEN) — Rewrite `currentBlockAnchorIdFromAnchorPage` implementation and caller

- Change function signature from `(orderedPreorderStartAnchors: BookAnchorFull[], ...)` to take `{ id: number; firstBbox: { pageIndex: number; bbox: ReadonlyArray<number> } | undefined }[]` (or equivalent). Remove internal `parsePdfOutlineV1Anchor` call; use `firstBbox.pageIndex` and `firstBbox.bbox` directly.
- Update caller in `BookReadingContent.vue` (`flatBookBlocks.value.map((r) => r.startAnchor)` → `flatBookBlocks.value.map((r) => ({ id: r.id, firstBbox: r.allBboxes[0] }))`).
- Remove `BookAnchorFull` import from `currentBlockAnchorFromAnchorPage.ts`.
- Unit tests from 2a now **pass**.
- E2E: `book_browsing.feature` (viewport-driven current-block highlight).
- No dead code: `parsePdfOutlineV1Anchor` is still called by navigation (line 426) and snap-back (line 227).

### Sub-phase 2c — Replace `startAnchor` in block-selection navigation with `allBboxes[0]`

- In `applyBookBlockSelection`: replace `parsePdfOutlineV1Anchor(block.startAnchor)` with `wireItemsToNavigationTargets(block.allBboxes)[0] ?? null`.
- E2E: `book_browsing.feature` (clicking a book block scrolls PDF to the right place).
- No dead code: `parsePdfOutlineV1Anchor` is still called by snap-back (line 227).

### Sub-phase 2d — Replace `startAnchor` in snap-back with `allBboxes[0]`; remove dead anchor-parsing code

- In `performSnapBack`: replace `parsePdfOutlineV1Anchor(sel.startAnchor)` with `wireItemsToNavigationTargets(sel.allBboxes)[0] ?? null`.
- `parsePdfOutlineV1Anchor` now has **zero** production callers → remove it, `parsePdfOutlineV1StartAnchor`, and `extractPageIndexZeroBased` from `pdfOutlineV1Anchor.ts`. Remove `BookAnchorFull` import from that file.
- Remove corresponding unit tests from `pdfOutlineV1Anchor.spec.ts` (the `parsePdfOutlineV1Anchor`, `parsePdfOutlineV1StartAnchor`, and `extractPageIndexZeroBased` describe blocks). Keep `wireItemsToNavigationTargets`, `outlineV1BboxToPdfJsXyzDestArray`, and geometry helper tests.
- E2E: `reading_record.feature` (snap-back behavior) + `book_browsing.feature`.
- No dead code.

### Sub-phase 2e — Remove `startAnchor` from row type and test fixtures

- Remove `startAnchor` field from `BookReadingBookLayoutBlockRow` type in `BookReadingBookLayout.vue`.
- Remove `startAnchor: child.startAnchor` from row construction in `BookReadingContent.vue` (line 164).
- Update `BookReadingPage.spec.ts`: the `.map((startAnchor, i) => ({ ... startAnchor, ...}))` pattern no longer needs `startAnchor` on the built blocks. Ensure `allBboxes[0]` in test data carries the same page/bbox info. Systematic replacement of ad-hoc block construction with **`makeMe` builders** across page specs is **Phase 4**.
- Remove `BookAnchorFullBuilder`, `makeMe.aBookAnchor`, and `makeMe.bookReadingTopMathsLikeAnchors()` from `doughnut-test-fixtures` **if no consumers remain** (check CLI tests; if CLI still references them, defer removal to Phase 5).
- E2E: `book_browsing.feature` + `reading_record.feature`.
- No dead code.

---

## Phase 3 — Backend API: remove `startAnchor` from the wire

**User-visible change:** `GET .../book` response no longer includes `startAnchor` on each `BookBlock`. External consumers (if any) would break — acceptable because frontend is already decoupled in Phase 2.

**Scope:**

- **`BookBlock.java`** — remove `getStartAnchor()` method, remove `startAnchor` from `@JsonPropertyOrder`, remove `BookAnchorFullWire` import.
- **`BookAnchorFullWire.java`** — delete.
- **`open_api_docs.yaml`** — remove `BookAnchor_Full` schema and `startAnchor` from `BookBlock_Full` required/properties (or regenerate from code).
- **Regenerate TypeScript** — `pnpm generateTypeScript`; `BookAnchorFull` type and `BookBlockFull.startAnchor` field disappear from generated types.
- **`BookAnchorFullBuilder.ts`** and `makeMe.aBookAnchor` — delete if not already removed in Phase 2.
- **Backend tests** — verify `NotebookBooksControllerTest` allBboxes tests still pass (they don't assert `startAnchor` directly). Check for any compile errors from removed type.
- After changes, run `pnpm backend:verify` + `pnpm frontend:test` + E2E `book_browsing.feature` and `reading_record.feature`.

---

## Phase 4 — Frontend tests: makeMe builders for API-shaped book data (no ad-hoc `BookBlockFull`)

**User-visible change:** None. Improves test maintainability and alignment with generated API types.

**Context:** The frontend consumes `BookBlockFull`, `PageBboxFull`, and related types from `@generated/doughnut-backend-api` without a separate translation layer. After Phase 2b, current-block logic and navigation depend on `allBboxes[0]` being coherent with that shape. Ad-hoc object literals in page specs (e.g. `NotebookPage.spec.ts`, `BookReadingPage.spec.ts`, and other specs that stub `getBook` or build flat block lists) tend to drift: empty `allBboxes`, `as unknown as PageBboxFull` to fake optional `bbox`, or hand-built rows that disagree with OpenAPI. That forces brittle fixes whenever wire shape or semantics change.

**Scope:**

- **Extend `doughnut-test-fixtures` / `makeMe`** — Add or extend builders for book-reading and notebook flows so tests can request **API-shaped** `BookBlockFull[]` (and nested `PageBboxFull`) with named options, e.g. “first block page-only anchor (no bbox)”, “first block with degenerate bbox for auto-mark scenarios”, “block with direct content (multiple `allBboxes` entries)”, cross-page last bbox, preorder helpers aligned with existing `bookReadingTopMathsLike`-style data. Prefer one coherent representation per scenario over duplicated inline arrays in specs.
- **Migrate high-churn specs first** — Replace hand-constructed blocks in `BookReadingPage.spec.ts` (including `PREORDER_FIRST_BBOXES`-style literals and casts) and similar patterns in `NotebookPage.spec.ts` (and any other frontend tests that build `BookBlockFull` or book payloads inline for the SDK). After migration, tests should import from **`doughnut-test-fixtures/makeMe`** only (per project convention), not re-encode the same geometry in each file.
- **Type fidelity** — Where the runtime allows `bbox` to be absent (e.g. page index only) but generated `PageBboxFull` still requires `bbox`, keep that edge in the **builder** (single place) rather than scattered `as unknown` in every spec; document the intended wire/backend behavior in the builder’s option names, not in test comments.
- **Verification** — `pnpm frontend:test` for affected specs; no production code change required for this phase unless a gap in builders forces a tiny shared helper in test-only code (avoid if makeMe can own it).

**Dependency:** Sensible to run after Phase 3 (generated types no longer expose `startAnchor`), so builders and specs target the final `BookBlockFull` shape. If Phase 3 is delayed, builders can still omit `startAnchor` to match the post–Phase 2 frontend assumption.

---

## Phase 5 — CLI and documentation: remove all remaining BookAnchor references

**User-visible change:** Codebase and docs are free of the BookAnchor concept.

**Scope:**

- **CLI TS test fixtures** — `InteractiveCliApp.useNotebook.test.tsx` and `mineruOutlineSubprocess.test.ts`: remove `startAnchor` from attach-book shaped test payloads (the actual CLI layout builder already does not emit it; tests should match).
- **Python comments** — `mineru_book_outline.py`: update comments that say "serves as startAnchor" → describe the `contentBlocks[0]` role directly without referencing the removed concept.
- **Python test** — `test_mineru_book_outline.py`: `test_heading_node_has_no_startAnchor_field` — either keep as a regression guard (asserting the field is absent) or remove if the concept no longer needs guarding.
- **Architecture roadmap** (`doughnut-book-reading-architecture-roadmap.md`):
  - Remove `BookAnchor` from class diagram (the `BookBlock "1" --> "1" BookAnchor : startAnchor` edge, `SourceSpan` anchor edges, the `BookAnchor` class itself).
  - Update "Architectural rules" (rule 1: "Every BookBlock has exactly one startAnchor" → remove or restate in terms of `allBboxes[0]` / first content block).
  - Update "BookBlock" section (remove "startAnchor locates the section").
  - Update "Current directional choices" (`allBboxes` paragraph: remove "The first entry matches startAnchor").
  - Update "Open architecture questions" (remove "Whether BookAnchor.value should gain a typed JSON schema").
  - Update "Anti-patterns" (remove/rephrase "Anchors that only mean page number" if it referenced BookAnchor).
- **UX roadmap** (`book-reading-ux-ui-roadmap.md`): update "Related" paragraph (remove "`BookAnchor` is a single `value` string" clause).
- **Research report** (`book-reading-research-report.md`): update or remove `BookAnchor` / `startAnchor` references (8 occurrences).
- **Reading record plan** (`book-reading-reading-record-plan.md`): update `startAnchor.pageIndex` reference.
- **Note on SourceSpan:** The architecture roadmap shows `SourceSpan` referencing `BookAnchor` for start/end anchors. Since `SourceSpan` is not yet implemented, update the diagram to use a future-appropriate representation (e.g. inline page+bbox on `SourceSpan` directly, or a note that the locator format is TBD). Do not introduce a new entity just to preserve the old shape.
