# Book-reading architecture cleanup — phased plan

**Goal:** Remove eight architecture smells that accumulated during the PDF → PDF+EPUB unification, in small, self-contained commits. No new user-facing features except a single fragment-level EPUB resume win that falls out for free.

**Companion docs:** [book-reading-epub-support-plan.md](book-reading-epub-support-plan.md), [doughnut-book-reading-architecture-roadmap.md](doughnut-book-reading-architecture-roadmap.md).

## Delivery principles

- **One commit per phase.** Every phase must leave `main` green (targeted E2E + relevant unit suites) and be pushed before the next phase starts.
- **Stop-safe.** If delivery stops after any phase, the shipped state is strictly better than the start, never worse.
- **Behavior invariance for structure phases.** External behavior and existing test assertions do not change; only internal shape and (occasionally) generated TypeScript shapes change.
- **No speculative prep.** Every structure phase is justified by the very next phase or by a concrete code-deletion outcome within this plan.
- **Targeted tests only.** Run the specs that cover the touched files; do not run the full Cypress suite unless a phase explicitly requires it.

## Known decisions

These apply across all phases. They are fixed going into execution.

1. **Collapsed locator schema.** `ContentLocator` gets one canonical wire shape per format, with the `_Full`-suffixed `type` tag (`'EpubLocator_Full'`, `'PdfLocator_Full'`). The short `'epub'` / `'pdf'` variants leave the OpenAPI schema. `mergeBookMutationIntoFull` disappears.
2. **EPUB fragment canonical form = bare id (no leading `#`).** The `#` is an EPUB URL separator, not part of the id. Normalization happens at the backend read boundary so existing stored data keeps working.
3. **Reading-position wire format unifies on `ContentLocator`.** The `BookLastReadPositionRequest` becomes `{ locator: ContentLocator, selectedBookBlockId? }`. Persisted in one new `reading_position_locator_json` column; legacy `pageIndex` / `normalizedY` / `epubLocator` columns migrate out.
4. **Backend format dispatch lives in one `BookFormat` enum** with abstract methods (`validateAttachRequest`, `persistNewBook`, `streamBookFile`, `assembleContentLocators`). No interface hierarchy — Java enum with per-constant overrides.
5. **Reading panel anchor defaults live inside the composable.** `READING_PANEL_OBSTRUCTION_PX = 80` and `MIN_READING_PANEL_RESERVE_PX = 88` become composable defaults; call sites may override but currently do not.
6. **Viewer contract shrinks.** `BookReadingPdfViewerRef` keeps only primitives needed by external callers. Snap-back, hold, and content-fit logic move into `useBookReadingSnapBack` internals. `zoomIn` / `zoomOut` stay (used by `PdfControl`). `highlightBlockSelection`, `scrollToBookNavigationTarget`, and `scrollToStoredReadingPosition` stay (real external callers).
7. **Bootstrap composable returns a discriminated union** `{ kind: 'pdf' | 'epub', book, bytes, initial }`. Page becomes a switch on `kind`.
8. **CLI format detection is out of scope.** CLI maps extension → Content-Type; backend remains the sole authority for format acceptance. No change needed.
9. **Dual-write migration for reading position.** We dual-write during the cut-over, add a Flyway backfill, then drop legacy columns. Never a single hard-switch migration.
10. **Behavior invariance checkpoints.** The Cypress features that exercise book reading are `e2e_test/features/book_reading/epub_book.feature`, `book_browsing.feature`, `reading_record.feature`, `reorganize_layout.feature`, `ai_reorganize_layout.feature`. Phases that change reading code run the relevant subset listed in the phase.

## Track P — Product win (standalone)

### Phase P1 — EPUB resumes at the actual scroll fragment (behavior)

**Status: done**

**Why now:** Small, isolated, ships actual user value before any refactor. Currently `BookReadingEpubView.proposeEpubPositionForBlockId` stores the *block-start* href, not the user's live scroll location, so resume is always block-aligned. The relocation event already carries the real href.

**Change:** Swap `blockStartEpubDisplayHref(block)` for the live `payload.href` that `onEpubRelocated` already receives. Propose the live href (plus current `selectedBlockId`) into `lastReadPositionPatchDebouncer.proposeEpubLocator`.

**Files:** `frontend/src/components/book-reading/BookReadingEpubView.vue`.

**Tests:**
- Add a Cypress scenario (or extend an existing one in `epub_book.feature`) asserting that after scrolling within a block and leaving and re-entering the reading view, the viewer resumes at the fragment — not at the block start.
- The scenario starts `@wip`; when the implementation lands the tag is removed.

**Commit message:** `feat(epub): resume reading at the live fragment href`

---

## Track A — Collapse the `_Full` / non-`_Full` locator duality

**Unlocked deletions:** `frontend/src/lib/book-reading/mergeBookMutationIntoFull.ts`, its spec, the non-`_Full` `EpubLocator` / `PdfLocator` schema entries and their generated TS types.

### Phase A1 — Make `mergeBookMutationIntoFull` idempotent (structure)

**Status: done**

**Change:** Teach the mapper to pass through locators whose `type` is already `EpubLocator_Full` / `PdfLocator_Full`. Add a unit case to `mergeBookMutationIntoFull.spec.ts` for the pass-through path.

**Why:** Lets the backend emit `_Full` tags without breaking the frontend in the next phase.

**Files:** `frontend/src/lib/book-reading/mergeBookMutationIntoFull.ts`, `frontend/tests/lib/book-reading/mergeBookMutationIntoFull.spec.ts`.

**Tests:** `pnpm frontend:test tests/lib/book-reading/mergeBookMutationIntoFull.spec.ts`.

**Commit message:** `refactor(book-reading): accept Full locators in mergeBookMutationIntoFull`

### Phase A2 — Backend emits `_Full` tags on mutation responses (structure)

**Status: done**

**Change:** Add the `contentLocators` field on `BookMutationResponse` (or whichever DTO carries updated blocks) to the `BookViews.Full.class` JSON view so Jackson emits `EpubLocator_Full` / `PdfLocator_Full`. Regenerate TypeScript (`pnpm generateTypeScript`).

**Why:** The frontend normalizer from A1 now receives already-Full locators and passes them through.

**Files:** `backend/src/main/java/com/odde/doughnut/controllers/dto/…` (the mutation response DTO), OpenAPI schema (regenerated), `packages/generated/doughnut-backend-api/types.gen.ts` (regenerated).

**Tests:**
- Backend: existing mutation tests (`BookReorganizeController…Test`) pass unchanged.
- Frontend: `pnpm frontend:test tests/lib/book-reading/mergeBookMutationIntoFull.spec.ts` and `tests/components/book-reading`.
- Cypress (targeted): `reorganize_layout.feature`.

**Commit message:** `refactor(api): emit Full locator tags on BookMutationResponse`

### Phase A3 — Delete `mergeBookMutationIntoFull` (structure)

**Status: done**

**Change:** Replace the two call sites (currently in `BookReadingContent.vue`; verify for any other) with a direct `{ ...prev, blocks: updatedBlocks }` merge since updated blocks are now wire-compatible with `BookFull.blocks`. Remove the helper and its spec. Remove the `EpubLocator` / `PdfLocator` (non-Full) type imports wherever they become dead.

**Files:** `frontend/src/lib/book-reading/mergeBookMutationIntoFull.ts` (delete), `frontend/tests/lib/book-reading/mergeBookMutationIntoFull.spec.ts` (delete), call sites.

**Tests:** Frontend component tests for `BookReadingContent`; targeted Cypress `reorganize_layout.feature`.

**Commit message:** `refactor(book-reading): inline mutation merge, delete normalizer`

### Phase A4 — Remove non-Full locator schemas (structure)

**Status: done**

**Change:** Drop the non-Full `EpubLocator` / `PdfLocator` schemas from the backend (they are no longer referenced by any DTO). Regenerate TypeScript. Verify `types.gen.ts` no longer contains the `type: 'epub' | 'pdf'` variants.

**Files:** backend DTOs referencing the old schemas (none should remain), regenerated TS.

**Tests:** `pnpm backend:test_only` on affected packages; `pnpm frontend:test tests/lib/book-reading`.

**Commit message:** `refactor(api): drop legacy non-Full locator schemas`

---

## Track B — EPUB fragment canonicalization

**Unlocked simplification:** `epubDisplayHref` stops tolerating the legacy `#fragment` form; storage and display converge.

### Phase B1 — Backend strips `#` at the read boundary (structure)

**Status: done**

**Change:** In `BookBlockEpubContentLocators.epubLocatorFromRaw`, strip any leading `#` from the fragment before constructing `EpubLocator`. In `EpubStructureExtractor.fragmentFor`, return the bare id (drop the `"#" + id` concatenation). Update its callers that currently expect `"#id"` (there is one place that prepends when writing the beginning-anchor payload; verify it does the same strip/normalize).

**Why:** Canonical bare form downstream; legacy `"#id"` rows still normalize correctly on read, no data migration needed.

**Files:** `backend/src/main/java/com/odde/doughnut/services/book/BookBlockEpubContentLocators.java`, `backend/src/main/java/com/odde/doughnut/services/book/EpubStructureExtractor.java`.

**Tests:**
- Backend unit: existing EPUB structure tests should pass; add one case in the EPUB locator test for a raw payload with `"#sec"` and one with `"sec"` both producing bare form.
- Cypress (targeted): `epub_book.feature` (structure + navigation scenarios).

**Commit message:** `refactor(epub): normalize fragment to bare id at the read boundary`

### Phase B2 — Frontend joins with a single `#` (structure)

**Status: done**

**Change:** Simplify `epubDisplayHref` to `frag.length === 0 ? href : `${href}#${frag}`` (drop the tolerance branch we added earlier). Update test fixtures in `currentBlockIdFromEpubLocation.spec.ts` and any other EPUB spec that still builds fragments with `"#"` prefix. Verify `useAutoMarkNoDirectContentPredecessor.spec.ts` does not depend on the fragment form.

**Files:** `frontend/src/lib/book-reading/asEpubLocator.ts`, relevant specs.

**Tests:** `pnpm frontend:test tests/lib/book-reading`; `pnpm frontend:test tests/composables`.

**Commit message:** `refactor(epub): epubDisplayHref joins canonical bare fragment`

---

## Track C — Extract the shared reading-panel anchor composable

### Phase C1 — `useReadingPanelAnchor` composable (structure)

**Status: done**

**Change:** Create `frontend/src/composables/useReadingPanelAnchor.ts` exporting a composable that takes `{ viewerRef, blockRef, mainPaneRef, obstructionPx?, minReservePx? }` and returns `Ref<number | null>`. The composable encapsulates: resolve `lastDirectContentLocator(block)`, ask the viewer for `readingPanelAnchorTopPx`, clamp against `mainPane` height minus reserve. Default constants live inside the composable.

Replace the duplicated `updateReadingPanelAnchor` functions in `BookReadingContent.vue` and `BookReadingEpubView.vue` with consumption of this composable. Both views stop declaring `READING_PANEL_OBSTRUCTION_PX` and `MIN_READING_PANEL_RESERVE_PX`.

**Files:** `frontend/src/composables/useReadingPanelAnchor.ts` (new), `BookReadingContent.vue`, `BookReadingEpubView.vue`.

**Tests:**
- Add a small unit test for the composable's clamp rule (block at/near the bottom → `null`, block well above → a numeric value).
- Cypress (targeted): `epub_book.feature` (reading control panel anchored scenario) and `reading_record.feature`.

**Commit message:** `refactor(book-reading): extract useReadingPanelAnchor composable`

---

## Track D — Extract shared reading-session composables

**Goal:** Shrink `BookReadingContent.vue` and `BookReadingEpubView.vue` so the only format-specific code left in each is viewer wiring and the format-specific current-block resolver / position proposer.

### Phase D1 — `useBookReadingSelection` composable (structure)

**Status: done**

**Change:** Create `frontend/src/composables/useBookReadingSelection.ts` owning `selectedBlockId`, `blockAwaitingConfirmation`, `markSelectedBlockDisposition`, and the "mark and advance to next block" rule. Input: `{ bookBlocks, bookReading, onAdvance(block) }` where `onAdvance` is a format-specific callback (PDF scrolls, EPUB calls `viewerRef.displayLocator(block.contentLocators[0])`). Keeps `useAutoMarkNoDirectContentPredecessor` invocation.

Both views consume it and drop their local `blockAwaitingConfirmation` / `markSelectedBlockDisposition` / `applyBookBlockSelection` declarations.

**Files:** new composable, `BookReadingContent.vue`, `BookReadingEpubView.vue`.

**Tests:**
- Move the existing behavioral tests from `useAutoMarkNoDirectContentPredecessor.spec.ts` style to a mounted harness asserting selection + auto-advance at the composable level where practical.
- Cypress (targeted): `reading_record.feature`, `epub_book.feature` (skimmed/read + mark-and-advance scenarios).

**Commit message:** `refactor(book-reading): extract useBookReadingSelection composable`

### Phase D2 — `useBookReadingCurrentBlock` composable (structure)

**Status: done**

**Change:** Create `frontend/src/composables/useBookReadingCurrentBlock.ts` owning `createCurrentBlockIdDebouncer(…)` and `createLastReadPositionPatchDebouncer(…)`, plus the wiring that proposes a last-read-position whenever `currentBlockId` changes. Input: `{ notebookId, resolveCurrentBlock, proposeReadingPosition }` — the resolver and proposer are format-specific strategies the views supply.

PDF view supplies: resolver based on `currentBlockIdFromVisiblePage`, proposer using `pageIndex + normalizedY`.
EPUB view supplies: resolver based on `currentBlockIdFromEpubLocation`, proposer using the live `payload.href` (now baseline thanks to Phase P1).

Both views drop their local debouncer creation and the `proposeEpubPositionForBlockId` / equivalent helpers.

**Files:** new composable, `BookReadingContent.vue`, `BookReadingEpubView.vue`.

**Tests:**
- Unit: extend `tests/lib/book-reading/debounceLastReadPositionPatch.spec.ts` or add `tests/composables/useBookReadingCurrentBlock.spec.ts` covering the "currentBlockId change → proposer called" wiring.
- Cypress (targeted): `reading_record.feature`, `epub_book.feature` (resume scenarios).

**Commit message:** `refactor(book-reading): extract useBookReadingCurrentBlock composable`

---

## Track E — Backend `BookFormat` strategy

**Goal:** Replace the `if (BOOK_FORMAT_EPUB.equals(format))` chains in `BookService` with a `BookFormat` enum whose constants carry the format-specific implementation.

### Phase E1 — Extract `BookBlockContentLocatorAssembler` (structure)

**Status: done**

**Change:** Introduce `BookBlockContentLocatorAssembler` with `List<ContentLocator> assemble(String format, List<BookContentBlock> contentBlocks)`. Move the format branch currently inside `BookBlock.getContentLocators()` into it. `BookBlock.getContentLocators()` keeps its `@JsonProperty`/`@JsonView`/`@Schema` annotations and delegates with a single line.

**Why:** Entity no longer makes format decisions; prep for E2.

**Files:** `backend/src/main/java/com/odde/doughnut/services/book/BookBlockContentLocatorAssembler.java` (new), `backend/src/main/java/com/odde/doughnut/entities/BookBlock.java`.

**Tests:** Existing entity and controller tests that cover `contentLocators` serialization (PDF + EPUB).

**Commit message:** `refactor(book-block): extract BookBlockContentLocatorAssembler`

### Phase E2a — `BookFormat` enum scaffold with `assembleContentLocators` (structure)

**Status: done**

**Change:** Create `BookFormat` enum with `PDF` and `EPUB` constants, a static `fromString(String format)` factory, and one abstract method: `List<ContentLocator> assembleContentLocators(List<BookContentBlock> contentBlocks)`. Wire `BookBlockContentLocatorAssembler` through the enum. Remove `BookBlock`'s reference to the raw string format constants and replace with `BookFormat.fromString(book.getFormat())`.

**Files:** `backend/src/main/java/com/odde/doughnut/services/book/BookFormat.java` (new), updates to `BookBlockContentLocatorAssembler` and `BookBlock`.

**Tests:** Existing book serialization tests.

**Commit message:** `refactor(book-format): introduce BookFormat enum with assembleContentLocators`

### Phase E2b — Move `validateAttachRequest` branching into `BookFormat` (structure)

**Status: done**

**Change:** Add `void validateAttachRequest(AttachBookRequest request)` to the enum. PDF constant enforces the existing MinerU-required `layout` + `contentList` rule; EPUB constant enforces "no layout or contentList". `BookService.validateAttachRequest` shrinks to `BookFormat.fromString(request.getFormat()).validateAttachRequest(request)` plus the pre-format string check.

**Files:** `BookFormat.java`, `BookService.java`.

**Tests:** Existing attach validation tests for both formats.

**Commit message:** `refactor(book-format): move attach validation into BookFormat`

### Phase E2c — Move `persistNewBook` branching into `BookFormat` (structure)

**Status: done**

**Change:** Add `Book persistNewBook(BookService.PersistContext ctx)` (or similar) to the enum, where `PersistContext` carries the notebook, request, source ref, file bytes, and the persistence collaborators (repositories, entity persister). PDF and EPUB constants implement their respective persist flow (`persistNewPdfBook` / `persistNewEpubBook`). `BookService.attachBook` becomes a small dispatcher.

**Files:** `BookFormat.java`, `BookService.java`.

**Tests:** Existing attach tests for both formats; Cypress `book_browsing.feature` (targeted).

**Commit message:** `refactor(book-format): move persist-new-book into BookFormat`

### Phase E2d — Move `streamBookFile` branching into `BookFormat` (structure)

**Status: done**

**Change:** Add `ResponseEntity<Resource> streamFile(byte[] bytes, String baseName, String etag)` to the enum. Each constant supplies its `MediaType` and filename extension. `BookService.streamBookFile` shrinks to a dispatcher.

**Files:** `BookFormat.java`, `BookService.java`.

**Tests:** Existing file-streaming controller tests for PDF and EPUB.

**Commit message:** `refactor(book-format): move book file streaming into BookFormat`

### Phase E3 — Move `applyReadingPositionFields` dispatch through `BookFormat` (structure)

**Status: done**

**Change:** Add `void writeLegacyColumns(BookUserLastReadPosition row, BookLastReadPositionRequest req)` to `BookFormat`. Each constant writes only its format's columns and clears the others. `BookService.applyReadingPositionFields` becomes two lines: resolve the format from the payload shape, delegate.

**Why:** This is the final prep for the Track F unification.

**Files:** `BookFormat.java`, `BookService.java`.

**Tests:** Existing reading-position controller tests.

**Commit message:** `refactor(book-format): route legacy reading-position column writes through BookFormat`

---

## Track F — Unify reading-position wire format on `ContentLocator`

**Requires:** Track E (so the new field dispatch is clean on arrival).

### Phase F1 — Backend accepts `locator` alongside legacy fields (structure)

**Status: done**

**Change:** Add `ContentLocator locator` as an optional field on `BookLastReadPositionRequest`. If present, it wins over the legacy fields. `BookFormat.writeLegacyColumns` gains a sibling `writeLegacyColumnsFromLocator(row, locator)` that derives the existing column values from the locator shape. Validation: at least one of (`locator`, legacy fields) must be present.

**Files:** `BookLastReadPositionRequest.java`, `BookService.java`, `BookFormat.java`, regenerated TS.

**Tests:** Add a controller test for the new shape (both EPUB and PDF locator bodies). Existing legacy-shape tests still pass.

**Commit message:** `feat(api): accept ContentLocator on reading-position patch`

### Phase F2 — Frontend debouncer collapses to `propose(locator, sel?)` (structure)

**Status: done**

**Change:** Replace `propose(pageIndex, normalizedY, sel?)` and `proposeEpubLocator(epubLocator, sel?)` with a single `propose(locator: ContentLocatorFull, sel?: number)`. Both reader views construct the locator (PDF: `{ type: 'PdfLocator_Full', pageIndex, bbox, contentBlockId: null, derivedTitle: null }` or a dedicated reading-position shape, see F3; EPUB: `{ type: 'EpubLocator_Full', href, fragment }` from the live relocation). Update `useBookReadingCurrentBlock` to accept a `locator` proposer. Delete the discriminated-union body types.

**Files:** `frontend/src/lib/book-reading/debounceLastReadPositionPatch.ts`, `frontend/tests/lib/book-reading/debounceLastReadPositionPatch.spec.ts`, `useBookReadingCurrentBlock`, both views.

**Tests:** `pnpm frontend:test tests/lib/book-reading/debounceLastReadPositionPatch.spec.ts`; Cypress (targeted): `reading_record.feature`, `epub_book.feature`.

**Commit message:** `refactor(book-reading): unify reading-position proposer on ContentLocator`

### Phase F3 — Add `reading_position_locator_json` column and dual-write (structure)

**Status: done**

**Change:**
- Flyway migration `backend/src/main/resources/db/migration/V…__book_user_last_read_position_locator_json.sql` adding a `reading_position_locator_json TEXT NULL` column to `book_user_last_read_position`.
- `BookUserLastReadPosition` entity gains the new field.
- `BookFormat` strategy writes both the new JSON column **and** the legacy columns (dual-write).
- Reads continue to go through legacy columns for now.

**Files:** Flyway SQL (new), `BookUserLastReadPosition.java`, `BookFormat.java`, `BookService.java`.

**Tests:** Controller tests that write a position then read it back (both formats). Migration smoke via `pnpm backend:verify`.

**Commit message:** `feat(book-reading): dual-write reading position locator JSON`

### Phase F4 — Backfill existing rows into the JSON column (migration)

**Status: done**

**Change:** Flyway migration that populates `reading_position_locator_json` for every existing row where it is still `NULL`, by constructing either an EPUB locator (`{type: 'EpubLocator_Full', href, fragment}` — split on `#` if needed) or a PDF locator from legacy columns. Use SQL JSON functions (MySQL `JSON_OBJECT`) or a one-shot Java migration if JSON building in SQL is awkward.

**Files:** Flyway SQL (new).

**Tests:** Migration dry-run on the existing test database; verify a selection of rows convert correctly.

**Commit message:** `chore(db): backfill reading_position_locator_json from legacy columns`

### Phase F5 — Backend reads from JSON column (structure)

**Status: done**

**Change:** `getLastReadPosition` and `patchNotebookBookReadingPosition` read from `reading_position_locator_json` first. `BookFormat` stops writing legacy columns. Legacy columns become read-only rollback insurance.

**Files:** `BookService.java`, `BookFormat.java`, the response DTOs (GetNotebookBookReadingPosition response now exposes `locator`).

**Tests:** Controller tests reading back the new shape. Frontend: `BookReadingPage.vue` now consumes `pos.locator` directly instead of switching on `pos.epubLocator` / `pos.pageIndex`.

**Commit message:** `refactor(book-reading): read reading-position from locator JSON`

### Phase F6 — Drop legacy columns and API fields (cleanup)

**Status: done**

**Change:**
- Flyway migration dropping `page_index`, `normalized_y`, `epub_locator` columns from `book_user_last_read_position`.
- `BookLastReadPositionRequest` keeps only `locator` and `selectedBookBlockId`.
- Regenerate TS.
- Frontend removes the last references to `pos.pageIndex` / `pos.epubLocator` / `pos.normalizedY` in `BookReadingPage.vue` (replaced by `pos.locator`).

**Files:** Flyway SQL (new), request/response DTOs, regenerated TS, `BookReadingPage.vue`.

**Tests:** Full backend verify (`pnpm backend:verify` — this phase warrants it because of the schema drop). Frontend component tests for `BookReadingPage`. Cypress (targeted): `reading_record.feature`, `epub_book.feature` (resume scenarios), `book_browsing.feature`.

**Commit message:** `refactor(book-reading): drop legacy reading-position columns and fields`

---

## Track G — Shrink `BookReadingPdfViewerRef`

**Scoping rule:** only move methods that are truly internal to snap-back. Keep `scrollToBookNavigationTarget`, `scrollToStoredReadingPosition`, `highlightBlockSelection`, `zoomIn`, `zoomOut` — they have real external callers.

### Phase G1 — Move `contentFitsFromBlockTop` into snap-back internals (structure)

**Status: done**

**Change:** Expose the geometry primitive the snap-back logic actually needs from the viewer (e.g. `getPageRect(pageIndex): { height } | null` or similar), and compute `contentFitsFromBlockTop` inside `useBookReadingSnapBack`. Remove the method from `BookReadingPdfViewerRef`.

**Files:** `frontend/src/composables/bookReaderViewerRef.ts`, `PdfBookViewer.vue`, `useBookReadingSnapBack.ts`.

**Tests:** Snap-back unit tests; Cypress `reading_record.feature`.

**Commit message:** `refactor(book-reading): move contentFitsFromBlockTop into snap-back`

### Phase G2 — Move `snapToContentBottomAndHold` + `suppressScrollInput` into snap-back (structure)

**Status: done**

**Change:** Replace the pair with a single viewer primitive `scrollToNormalizedPoint(pageIndex, normalizedY)` (or similar) plus an external hold/suppress mechanism owned by `useBookReadingSnapBack`. The composable manages the hold timer, the "don't re-emit viewport updates during the hold" gate, and the highlight handoff.

**Files:** `bookReaderViewerRef.ts`, `PdfBookViewer.vue`, `useBookReadingSnapBack.ts`.

**Tests:** Snap-back unit tests; Cypress `reading_record.feature`.

**Commit message:** `refactor(book-reading): move snap-back hold and suppression out of viewer`

---

## Track H — `useBookReadingBootstrap`

### Phase H1 — Extract bootstrap composable (structure)

**Change:** Move `BookReadingPage.vue`'s `onMounted` block (fetch book, fetch file bytes, fetch reading position, derive initial state) into `frontend/src/composables/useBookReadingBootstrap.ts`. The composable returns `Ref<BookReadingBootstrap | null>` where

```ts
type BookReadingBootstrap =
  | { kind: 'pdf'; book: BookFull; bytes: ArrayBuffer; initialLastRead: {...} | null; initialSelectedBlockId: number | null }
  | { kind: 'epub'; book: BookFull; bytes: ArrayBuffer; initialLocator: ContentLocatorFull | null; initialSelectedBlockId: number | null }
```

(Note the EPUB `initialLocator` is now a `ContentLocatorFull`, not a string — this lands cleanly because Track F already moved the wire shape.)

`BookReadingPage.vue` becomes a switch on `bootstrap.value.kind` that picks the component and spreads props.

**Files:** `frontend/src/composables/useBookReadingBootstrap.ts` (new), `BookReadingPage.vue`, minor prop type updates in `BookReadingEpubView.vue` (accept `ContentLocatorFull | null` instead of `string | null`).

**Tests:** Unit tests for the composable (happy path, missing position, file load failure). Cypress (targeted): `book_browsing.feature`, `reading_record.feature`, `epub_book.feature`.

**Commit message:** `refactor(book-reading): extract useBookReadingBootstrap composable`

---

## Phase checklist (per phase)

Every phase follows the discipline from `.cursor/rules/planning.mdc`:

1. **Write or adjust the test first** (unit or Cypress scenario for behavior phases; locked existing tests for structure phases).
2. **Implement the smallest change** that makes the test pass.
3. **Run the targeted test subset** listed in the phase.
4. **Remove any dead code** the change revealed.
5. **Commit with the exact message listed.**
6. **Push** and wait for CD before starting the next phase (unless two phases are explicitly bundled).

## Sequencing summary

```
P1   → EPUB fragment-level resume                (behavior)
A1–4 → Collapse _Full duality                    (structure, deletes mergeBookMutationIntoFull)
B1–2 → Canonicalize EPUB fragment form           (structure, removes tolerance branch)
C1   → Reading panel anchor composable           (structure, dedupes anchor logic)
D1–2 → Reading session + current-block composables (structure, thins both views)
E1   → BookBlockContentLocatorAssembler           (structure, prep)
E2a–d→ BookFormat enum with strategy methods      (structure)
E3   → Reading-position dispatch via BookFormat   (structure, final prep for F)
F1   → Accept locator field on reading-position   (structure, new API shape)
F2   → Frontend debouncer on ContentLocator       (structure, single proposer)
F3   → Dual-write locator JSON column             (structure, safe rollout)
F4   → Backfill legacy → locator JSON             (migration)
F5   → Read from locator JSON                     (structure)
F6   → Drop legacy columns & fields               (cleanup, final simplification)
G1–2 → Shrink PdfViewerRef contract               (structure)
H1   → useBookReadingBootstrap                    (structure)
```

Total: 20 phases. Any phase may be skipped at the end of a track without blocking later tracks, with two exceptions:
- **F5 requires F4.** You may not read from the new column before it is backfilled.
- **F6 requires F5.** You may not drop legacy columns while anything still reads them.
- **H1 assumes F5 has landed** (so `BookReadingPage.vue` can consume `pos.locator` directly); if stopped mid-F, H1 still works but carries a small pre-F switch on `book.format`.

## Out of scope (and why)

- **CLI / backend format detection parity.** CLI maps `.pdf`/`.epub` → Content-Type; backend validates format via `BookFormat`. Two distinct responsibilities, not duplication. No change.
- **Replacing pdf.js or epub.js.** Reader library choice is a separate architectural decision, not a cleanup.
- **Cross-format search, annotations, offline caching.** Out of scope; covered (or to be covered) by the reading-feature roadmap.
- **Backfilling a canonical bare-form fragment into existing `BookContentBlock.rawData` rows.** Normalization at read time (B1) is sufficient; a storage migration adds risk without user value.
