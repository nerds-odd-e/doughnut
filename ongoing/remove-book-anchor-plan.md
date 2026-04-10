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

**Scope:**

- **`currentBlockAnchorIdFromAnchorPage`** — rewrite signature from `(orderedPreorderStartAnchors: BookAnchorFull[], ...)` to take `{ id: number; firstBbox: PageBboxFull | undefined }[]` (or equivalent). Internally, use `firstBbox.pageIndex` and `firstBbox.bbox` instead of `parsePdfOutlineV1Anchor(anchor).pageIndex` / `.bbox`. Update callers in `BookReadingContent.vue` (`flatBookBlocks.value.map(r => r.startAnchor)` → map to new shape from `r.id` + `r.allBboxes[0]`).
- **`BookReadingContent.vue` navigation** — `parsePdfOutlineV1Anchor(block.startAnchor)` in `applyBookBlockSelection` → derive from `block.allBboxes[0]` (use `wireItemsToNavigationTargets([block.allBboxes[0]])` or direct field access). Same for snap-back logic (`parsePdfOutlineV1Anchor(sel.startAnchor)` → `allBboxes[0]`).
- **`pdfOutlineV1Anchor.ts`** — remove `parsePdfOutlineV1Anchor(anchor: BookAnchorFull)` and `parsePdfOutlineV1StartAnchor(value: string)` once no callers remain; remove `BookAnchorFull` import. Keep `wireItemsToNavigationTargets`, `outlineV1BboxToPdfJsXyzDestArray`, and geometry helpers (they operate on `PageBboxFull` / `PdfOutlineV1NavigationTarget`, not on anchor strings). If `extractPageIndexZeroBased(value: string)` has no callers, remove it too.
- **Remove `startAnchor` from `BookReadingBookLayoutBlockRow`** type — it is no longer read.
- **Test fixture updates** — ensure `allBboxes[0]` in test data matches what `startAnchor.value` used to provide (page index, bbox coordinates). If `BookAnchorFullBuilder` is no longer imported anywhere in frontend tests, remove its usage. May need to build `PageBboxFull` values in tests where only `startAnchor` was provided before.
- **Remove `BookAnchorFullBuilder`** and `aBookAnchor` from `doughnut-test-fixtures` if no consumers remain after this phase (check CLI tests — they may still reference it; if so, defer removal to Phase 4).
- Update unit tests: `currentBlockAnchorFromAnchorPage.spec.ts` (new input shape), `pdfOutlineV1Anchor.spec.ts` (remove anchor-string tests), `BookReadingPage.spec.ts` (navigation-related assertions).
- After changes, run frontend unit tests + E2E `book_browsing.feature` and `reading_record.feature`.

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

## Phase 4 — CLI and documentation: remove all remaining BookAnchor references

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
