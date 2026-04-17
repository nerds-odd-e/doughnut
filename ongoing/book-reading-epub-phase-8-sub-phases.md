# EPUB Phase 8 — sub-phases plan

**Parent plan:** [book-reading-epub-support-plan.md](book-reading-epub-support-plan.md) — Phase 8 "EPUB direct-content boundaries and no-direct-content automation".

**Goal of this plan:** break Phase 8 into small, each-a-complete-git-commit slices (~5 minutes of focused work each). Each slice either delivers externally observable EPUB behavior or restructures the codebase specifically to enable the immediate next behavior slice — no speculative prep. While doing this, take the opportunity to remove duplication between the EPUB and PDF reading paths so the shared domain stays cohesive.

## Guiding principles for these sub-phases

- **Stop-safe:** after any sub-phase the main branch is shippable. PDF behavior never regresses; EPUB behavior either stays the same or improves.
- **One behavior per behavior-slice; one enabling refactor per structure-slice.** Structure slices must be justified by the next behavior slice (not by slices further out).
- **Cohesion via a single domain concept:** where PDF and EPUB express the same domain concept (block start address, direct-content locators, has-direct-content, panel-anchor geometry), prefer a single generalized model over parallel format-specific fields.
- **Observable tests first:** add or extend unit/E2E tests at the same slice as the behavior they prove, driven from the component/page level the user actually uses.
- **`@wip` for scenarios that outrun the code.** Keep the `@wip` budget under 5.

---

## The central refactor: virtualize `PageBbox` into `ContentLocator`

Today the reading domain has **four** format-specific concepts that all express variants of "where is this block's content":

| Concept | Today | What it really means |
|---|---|---|
| `BookBlock.allBboxes` | PDF-only list of `PageBbox` (pageIndex + [x0,y0,x1,y1]) | ordered locators: index 0 = anchor (heading), index 1+ = direct content |
| `PageBbox` | PDF-shaped struct | a locator whose medium happens to be a PDF page |
| `BookBlock.epubStartHref` | EPUB-only string `spine#fragment` | the block's start locator, medium = EPUB spine |
| Proposed `hasDirectContent` / `epubEndLocator` | one-off derived fields | countable locators / last direct-content locator |

All four collapse into **one** generalized concept:

- `BookBlock.contentLocators: List<ContentLocator>` — ordered list with the same semantics `allBboxes` already has (index 0 anchor; index 1+ direct content, filtered at extraction time via `BookBlockDirectContentPredicate`).
- `ContentLocator` is a discriminated union:
  - PDF variant: `{ pageIndex, bbox: [x0,y0,x1,y1] }`
  - EPUB variant: `{ href, fragment? }`

Derived rules become format-agnostic:

- `hasDirectContent(block) := block.contentLocators.length > 1`
- Block-start locator := `block.contentLocators[0]` (replaces `epubStartHref`; PDF viewer reads its PDF variant for jump-to-page)
- Panel-anchor target (last direct-content locator) := `block.contentLocators[last]`

The reader viewer contract goes format-agnostic too:

```
interface BookReaderViewerRef {
  displayLocator(loc: ContentLocator): Promise<void>
  resolveLocatorRect(loc: ContentLocator): Rect | null
  isLocatorBottomVisible(loc: ContentLocator, obstructionPx: number): boolean
  readingPanelAnchorTopPx(loc: ContentLocator, obstructionPx: number): number | null
}
```

PDF implements each from the PDF variant; EPUB from the EPUB variant. Everything above the viewer (`BookReadingContent.vue`, `BookReadingEpubView.vue`, `useBookReadingSnapBack`, `useAutoMarkNoDirectContentPredecessor`) operates on `ContentLocator` without knowing the format.

This refactor is bigger than an "add one API field" change, but it pays Phase 8 back: once `contentLocators` exists, the EPUB auto-mark rule and the EPUB panel-anchor behavior each drop in with almost no new view-specific code.

---

## Architecture opportunities beyond Phase 8 (not scheduled)

Noted here so future work can pick them up; Phase 8 behaviors don't need them:

- Shared scroll→currentBlock + scroll→last-read-patch pipeline (duplicated between `BookReadingContent.vue` and `BookReadingEpubView.vue`).
- Shared `mark-and-advance(status)` helper (duplicated between the two views).
- Shared mount-time layout-breakpoint + `bookReading.syncFromServer()` wiring.

---

## Sub-phases

Numbering is **8.N** and is plan-only bookkeeping — commit messages, test names, and any permanent artifact should be named by capability, not by sub-phase number.

### 8.1 — Structure: introduce `ContentLocator` union on the backend, populated for PDF

**Why now:** First step in the unification. Nothing reads `contentLocators` yet; PDF extraction emits it alongside the existing `allBboxes` getter so callers can migrate in later slices without a breaking flag day.

**Scope:**
- Backend: add `ContentLocator` as a sealed/tagged type with variants `PdfLocator { pageIndex, bbox }` and `EpubLocator { href, fragment? }`.
- Add `BookBlock.getContentLocators()` that for PDF books delegates to the same walk as `BookBlockContentBboxes.allBboxes` but emits PDF-variant locators. For EPUB books this slice emits an empty list (EPUB population is the next slice).
- Annotate for `BookViews.Full`, mark required in the schema; OpenAPI + TS regenerate.
- Leave `allBboxes` and `epubStartHref` untouched — no callers change.

**Tests:**
- One controller test per format asserting `contentLocators` matches expectations on the existing fixtures (PDF: anchor + direct entries match `allBboxes`; EPUB: empty for now).

### 8.2 — Structure: populate `contentLocators` for EPUB at extraction time

**Why now:** Needed by every downstream slice that reads EPUB locators (starting with 8.5's shared auto-mark rule).

**Scope:**
- `EpubStructureExtractor` / `BookBlock.getContentLocators()`: for EPUB books, emit `EpubLocator` entries — index 0 = the same locator `epubStartHref` returns today (anchor); index 1+ = one entry per `BookContentBlock` that `contributesDirectContent`, carrying `href` (and `fragment` when present) read from `rawData`.
- Still no frontend consumer; `epubStartHref` remains as the canonical EPUB start address for now.

**Tests:**
- Controller test on the existing `epub_valid_minimal.epub` fixture: pick one block that should have direct content and assert `contentLocators.length > 1` with the expected `href`/`fragment` shape on the last entry; pick a structural-only block and assert `contentLocators.length == 1`.

### 8.3 — Structure: migrate PDF frontend callers from `allBboxes` → `contentLocators`

**Why now:** Needed by 8.5 so the shared composable only depends on the unified field. PDF behavior unchanged.

**Scope:**
- Introduce a small `asPdfLocator(loc: ContentLocator): PdfLocator | null` helper in `frontend/src/lib/book-reading/` so call sites stay readable.
- Switch PDF call sites (`useBookReadingSnapBack.ts`, `BookReadingContent.vue`, `pdfOutlineV1Anchor.ts` / `wireItemsToNavigationTargets`, `currentBlockIdFromVisiblePage.ts`, the PDF viewer's `BookNavigationTarget` conversion) from reading `block.allBboxes` / `PageBbox` to reading `block.contentLocators` + the helper.
- Delete the `allBboxes` API surface and `PageBbox` type once no caller remains (this may land in a follow-up slice 8.3b if too big for one commit).

**Tests:**
- Existing PDF unit + E2E tests must all stay green, unchanged.

### 8.4 — Structure: migrate the block-start locator from `epubStartHref` → `contentLocators[0]`

**Why now:** Needed by 8.7's EPUB viewer contract, which takes `ContentLocator`, not strings. PDF behavior and EPUB navigation behavior unchanged.

**Scope:**
- Frontend EPUB call sites (`BookReadingEpubView.vue`, `EpubBookViewer.vue`, `currentBlockIdFromEpubLocation.ts`, the layout click path) switch from `block.epubStartHref` to `block.contentLocators[0]` of EPUB variant.
- `EpubBookViewer.displayEpubTarget(href)` becomes `displayLocator(loc: ContentLocator)` internally, still calling `rendition.display(href#fragment)` on the EPUB variant.
- Remove `epubStartHref` from the backend getter and API once unused; regenerate TS.

**Tests:**
- Existing EPUB E2E scenarios (precise-nav, scroll-sync, resume, mark-as-read) must all stay green, unchanged.
- Update `currentBlockIdFromEpubLocation.spec.ts` input shape if needed — behavior assertions stay the same.

### 8.5 — Structure: extract `useAutoMarkNoDirectContentPredecessor` with the unified rule

**Why now:** Needed by 8.6 so EPUB gets the behavior for free.

**Scope:**
- New composable `frontend/src/composables/useAutoMarkNoDirectContentPredecessor.ts`. Inputs: `bookBlocks`, `currentBlockId`, `hasRecordedDisposition`, `submitReadingDisposition`. Body is the current PDF watcher logic, but uses the unified rule `predecessor.contentLocators.length > 1` for direct-content, and skips blocks with empty `contentLocators` (synthetic `*beginning*` on both formats).
- `BookReadingContent.vue`: install via the composable, remove the inline watcher.

**Tests:**
- One focused unit test file for the composable covering: predecessor with direct content (no call), predecessor with no direct content but already recorded (no call), predecessor with no direct content and no recording (calls `submitReadingDisposition(predId, "READ")`), predecessor with empty locators (no call).
- Existing PDF E2E stays green.

### 8.6 — Behavior: EPUB auto-marks a predecessor block that has no direct content

**Why now:** First user-visible Phase 8 behavior. Rides entirely on 8.2 + 8.5.

**Scope:**
- `BookReadingEpubView.vue`: install `useAutoMarkNoDirectContentPredecessor` with the same arguments shape PDF uses.
- Ensure the EPUB fixture has at least one block with no direct content whose successor a scroll can reach. If `epub_valid_minimal.epub` already contains a structural-only nav entry (e.g. "Part One" wrapping "Chapter Alpha"), use it; otherwise extend the fixture in this slice.

**Tests:**
- New scenario in `e2e_test/features/book_reading/epub_book.feature`: user scrolls past a no-direct-content EPUB block → that block shows as marked read in the layout. Start `@wip` if fixture extension is large enough to warrant splitting (see note below); un-`@wip` once both halves land.

**Notes:**
- If the fixture change is more than trivial (adds new XHTML / manifest entries), split into **8.6a** (fixture extension + backend re-extraction test that pins the expected structure) and **8.6b** (frontend wiring + E2E scenario un-`@wip`'d).

### 8.7 — Structure: unified viewer contract `BookReaderViewerRef`

**Why now:** Needed by 8.8 (PDF side) and 8.9 (EPUB side). Behavior unchanged.

**Scope:**
- Define `BookReaderViewerRef` in `frontend/src/composables/` (or `frontend/src/lib/book-reading/`) with `displayLocator`, `resolveLocatorRect`, `isLocatorBottomVisible`, `readingPanelAnchorTopPx`. Keep PDF-only methods (`zoomIn`, `zoomOut`, `scrollToStoredReadingPosition`, `snapToContentBottomAndHold`, `suppressScrollInput`, `contentFitsFromBlockTop`, `highlightBlockSelection`) on a PDF sub-interface that extends the base.
- `PdfBookViewer.vue` implements the unified methods from the PDF locator variant (delegating to the existing page+bbox implementations).
- `useBookReadingSnapBack.ts` takes the generic `BookReaderViewerRef` for the unified methods and the PDF sub-interface for the PDF-only calls. The `readingPanelAnchorTopPx` call site stops threading `pageIndex` + `bbox` separately and passes the locator.

**Tests:**
- PDF unit + E2E green unchanged.

### 8.8 — Structure: `useBookReadingSnapBack` panel-anchor path reads the unified locator

**Why now:** Needed by 8.9 so EPUB reuses the same final prop wiring into `ReadingControlPanel`, and by 8.10 so no PDF-only prop overrides remain.

**Scope:**
- `updateReadingPanelAnchor` in `BookReadingContent.vue` (and related pieces inside `useBookReadingSnapBack`) switch from `block.allBboxes[last]` reads to `block.contentLocators[last]` via the viewer's unified `readingPanelAnchorTopPx(locator, obstructionPx)`.
- Any remaining PDF-shape assumptions in snap-back geometry (e.g. `contentFitsFromBlockTop`, `snapToContentBottomAndHold`) stay PDF-specific behind the PDF sub-interface — snap-back remains a PDF-only behavior.

**Tests:**
- PDF reading-record and snap-back E2E scenarios green unchanged.

### 8.9 — Structure: `EpubBookViewer` implements the unified viewer contract

**Why now:** Needed by 8.10.

**Scope:**
- `EpubBookViewer.vue` adds `resolveLocatorRect(loc)`, `isLocatorBottomVisible(loc, obstructionPx)`, `readingPanelAnchorTopPx(loc, obstructionPx)`. Implementation: pick the rendered section whose spine href matches the EPUB locator's path; inside that section's iframe document, find the element by `#fragment` id when present, fall back to the section `<body>`. Return rects in the rendition host's coordinate space (same system PDF's implementation uses so the panel-anchor consumer code is identical).
- `displayLocator(loc)` wraps the existing `rendition.display(href + fragment)` call.

**Tests:**
- Consider a component-mount smoke test asserting `resolveLocatorRect` returns non-null for a known fixture locator and null for an unknown locator, if cheap; otherwise coverage falls out of the 8.10 E2E scenario.

### 8.10 — Behavior: EPUB Reading Control Panel anchors below the last content locator

**Why now:** The primary user-visible Phase 8 behavior, riding on 8.7–8.9.

**Scope:**
- `BookReadingEpubView.vue`: compute the panel anchor via the unified viewer API using `block.contentLocators[last]`; pass the resulting number (or null) to `ReadingControlPanel :anchor-top-px`. Fall back to null (bottom-dock) when the locator resolves to no rect (e.g. section not yet rendered).
- Remove the Phase 7 `:anchor-top-px="null"` override.

**Tests:**
- New scenario in `epub_book.feature`: select a block with direct content → `ReadingControlPanel` appears with `data-panel-placement="anchored"` rather than fixed. Tag `@wip` if first wiring is flaky; un-tag once stable.
- Existing "Mark an EPUB block as read advances the selection" scenario must stay green — panel placement changes, mark-and-advance behavior does not.

### 8.11 — Behavior: EPUB supports Skim / Skip dispositions

**Why now:** The user-stories doc lists this as pending on content-aware anchoring; once the panel is content-anchored in 8.10, surfacing Skim/Skip is a small, coherent addition that removes the last EPUB-specific prop override on `ReadingControlPanel`.

**Scope:**
- `BookReadingEpubView.vue`:
  - Remove `:show-skim-and-skip="false"` so the default (true) applies.
  - Wire `@markAsSkimmed` / `@markAsSkipped` through the existing `submitReadingDisposition` + `nextBookBlockAfter` + `applyBookBlockSelection` flow. Consider extracting a tiny local `markBookBlockAndAdvance(status)` helper to reuse from all three mark paths; defer extracting a shared PDF+EPUB composable to a future slice unless it falls out obvious here.

**Tests:**
- New scenario in `epub_book.feature`: mark an EPUB block as Skimmed → layout shows it as skimmed, next block selected. One scenario for Skim is enough; Skip shares the same code path.

### 8.12 — Cleanup, plan refresh, and interim-behavior removal

**Why now:** Phase-discipline gate for closing Phase 8.

**Scope:**
- Delete any leftovers from the generalization: stray `PageBbox` references, dead helpers in `useBookReadingSnapBack`'s return value, unused imports, EPUB-specific prop overrides on `ReadingControlPanel` that 8.10/8.11 made redundant.
- Confirm `allBboxes` and `epubStartHref` are fully removed from the backend, frontend, and generated TS.
- Update `ongoing/book-reading-epub-support-plan.md`: mark Phase 8 as done, summarize what shipped under "Scope (shipped)" the same way Phases 4–7 document it, update the phase summary table.
- Update `ongoing/book-reading-user-stories.md`: flip the two pending bullets to shipped sub-stories.

**Tests:** no new tests; all prior PDF and EPUB scenarios and units must be green.

---

## Mapping back to parent Phase 8 scope

| Parent Phase 8 bullet | Sub-phase(s) |
|---|---|
| Resolve EPUB direct-content boundaries in the live DOM | 8.2, 8.9 |
| Panel uses EPUB-aware content boundaries for anchoring | 8.7, 8.8, 8.9, 8.10 |
| No-direct-content auto-marking using the shared reading-order rule | 8.1, 8.2, 8.5, 8.6 |
| Remove Phase 7 bottom-docked interim behavior | 8.10, 8.12 |
| (Implied by user-stories) Skim/Skip for EPUB | 8.11 |

## Stop-safety check per sub-phase

| After… | Main branch state |
|---|---|
| 8.1 | PDF unchanged; EPUB unchanged; `contentLocators` exposed but EPUB list empty |
| 8.2 | PDF unchanged; EPUB unchanged externally; `contentLocators` populated both sides, still unused |
| 8.3 | PDF unchanged; `allBboxes` deleted |
| 8.4 | PDF unchanged; EPUB unchanged; `epubStartHref` deleted |
| 8.5 | PDF unchanged (auto-mark now via composable); EPUB unchanged |
| 8.6 | PDF unchanged; **EPUB auto-marks no-direct-content predecessor** |
| 8.7 | PDF unchanged (viewer contract generalized) |
| 8.8 | PDF unchanged (snap-back reads unified locator) |
| 8.9 | PDF unchanged; EPUB viewer exposes unified API, still unwired upstream |
| 8.10 | PDF unchanged; **EPUB panel anchors below last content locator** |
| 8.11 | PDF unchanged; **EPUB supports Skim / Skip** |
| 8.12 | Cleanup only |

## Commit checklist per sub-phase

1. Tests written or extended alongside the slice, failing for the right reason before the code change (for behavior slices; structure slices must keep existing tests green).
2. `pnpm frontend:test` for any frontend change; backend tests for any backend change; the single targeted `cypress run --spec` for touched E2E features.
3. Lint / format: `pnpm lint:all` (scope to touched files is usually enough).
4. Commit with a capability-focused message; no "Phase 8.x" in the message body.
5. Push; let CD deploy before starting the next sub-phase.
