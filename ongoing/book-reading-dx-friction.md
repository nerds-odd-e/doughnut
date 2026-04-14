# Book-Reading DX Friction — Improvement Plan

Source: codebase retrospective after fixing the `book_browsing.feature` scroll/current-block bug
(scroll `y0=89/1000` midpoint issue + OCR screenshot masking failure).

---

## Context: what caused the friction

Three concrete pain points emerged during the fix:

1. **OCR screenshot silently overwritten** — `overwrite: true` in `expectVisibleOCRContains`
   causes every OCR check to write to the same file (`book-reading-pdf-viewer-ocr.png`).
   When scenario 3 fails and scenario 4 (same feature, later) passes, the screenshot shows
   the *passing* scenario's viewport, not the *failing* one. The Cypress failure screenshot
   and the error message then describe two different assertions, burning time disambiguating.

2. **Scroll → current-block pipeline spans 5 files with no single entry point** — To trace
   why block 2.2 was not becoming "current", the path was:
   `bookReadingPage.ts` → `BookReadingContent.vue` (`onViewportAnchorPage`) →
   `pdfViewerViewportTopYDown.ts` (anchor-page math) → `currentBlockIdFromVisiblePage.ts`
   (midpoint rule) → `debounceCurrentBlockId.ts` (timing). No file in the chain mentions
   the next. A cross-reference comment at each major hand-off would collapse this into
   a one-hop search.

3. **`ReadingControlPanel` has two independent show-triggers that look like one** —
   Inside `useBookReadingSnapBack.ts`, `blockAwaitingConfirmation` shows the panel either
   because (a) snap-back wants to hold the user at the selected block's content, or (b)
   `geometryEverVisibleForSelection` is true and the current block has passed the immediate
   successor. These two paths have completely different meanings but share one `if/else`
   chain. When (b) fired unexpectedly in the test, it looked like snap-back misfiring.
   Named helpers or computed properties for each trigger would make the distinction immediate.

---

## Phases

### Phase 1 — OCR screenshot names are scenario-specific  *(shipped)*

`expectVisibleOCRContains` now defaults to `book-reading-pdf-viewer-ocr-p${pageNumber}`;
callers can pass an explicit `screenshotKey` when needed. `overwrite: true` is kept.

---

### Phase 2 — Scroll → current-block hand-offs are discoverable in one hop  *(planned)*

**Developer value:** A developer reading `onViewportAnchorPage` in `BookReadingContent.vue`
immediately knows *where* the current block is calculated from the scroll position, without
reading all five files. Same for the reverse direction: someone reading
`currentBlockIdFromVisiblePage.ts` can find where it is called.

**What changes:**

- Add a JSDoc/inline cross-reference comment to `onViewportAnchorPage` in
  `BookReadingContent.vue` that names `currentBlockIdFromVisiblePage` as the function that
  maps the anchor page + viewport Y-range to a block ID, and describes the midpoint rule in
  one sentence.

- Add a `@see` annotation to `pdfViewerViewportTopYDown` in `PdfBookViewer.vue` pointing to
  `currentBlockIdFromVisiblePage` as its consumer.

- Optionally annotate `currentBlockIdFromVisiblePage.ts` with a note that its input
  `viewport` comes from `pdfViewerViewportTopYDown` and that `y0 > mid` returns the
  *preceding* block (the key invariant that caused the bug).

**Important design knowledge (preserve in the comment):**

> The `currentBlockIdFromVisiblePage` midpoint rule: if the first visible block's `y0` is
> **above** the viewport midpoint, the algorithm returns the **previous** block. This means
> scrolling page N to the container top is not sufficient to make a block at `y0 > 0` the
> current block in a short viewport — the scroll must reach at least `y0` pixels into the
> page so `viewport.mid ≥ y0`.
>
> In `refactoring.pdf`, block 2.2 starts at `y0 = 89/1000` on page 2
> (fixture: `e2e_test/fixtures/book_reading/mineru_output_for_refactoring.json` item #13).
> `scrollPdfBookReaderToBringPage2IntoPrimaryView` therefore adds
> `Math.ceil(89/1000 × pageHeight)` extra pixels after `scrollIntoView({ block: 'start' })`.

**Test:** No behavioral change — run `book_browsing.feature` to confirm nothing regresses.

**Files touched:**
- `frontend/src/components/book-reading/BookReadingContent.vue`
- `frontend/src/components/book-reading/PdfBookViewer.vue`
- `frontend/src/lib/book-reading/currentBlockIdFromVisiblePage.ts`

---

### Phase 3 — ReadingControlPanel's two show-triggers are named separately  *(planned)*

**Developer value:** When debugging why the `ReadingControlPanel` appears (or doesn't),
a developer can immediately see which of the two independent conditions fired, without
tracing the entangled `if/else` inside `blockAwaitingConfirmation`.

**Background:** `blockAwaitingConfirmation` in `useBookReadingSnapBack.ts` has two distinct
reasons to show the panel:

- **Trigger A — geometry-visible path:** `geometryEverVisibleForSelection === true` AND the
  current block has moved past the immediate successor of the selected block. This shows the
  panel as a *reminder* even though snap-back did not fire (snap-back checks the intermediate
  blocks for direct-content and may short-circuit). This is the trigger that confused the
  investigation: it appeared without snap-back.

- **Trigger B — content-bottom-visible path:** the last direct-content bbox of the
  confirmation target is currently visible in the viewport (`lastContentBottomVisible`).
  This is the "user has seen the content; confirm to advance" trigger.

**What changes:**

- In `useBookReadingSnapBack.ts`, extract the two conditions from the `blockAwaitingConfirmation`
  `if/else` into named helpers or `computed` refs, e.g.:
  - `panelShownBecauseScrolledPastContent` — the `geometryEverVisibleForSelection` + successor
    check
  - `panelShownBecauseContentBottomVisible` — the `lastContentBottomVisible` check
- Combine them in `blockAwaitingConfirmation` with a clear logical OR and label.

**Constraint:** This is a pure internal rename/extract — no change in observable behavior.
All existing E2E scenarios must still pass.

**Test:** Run `e2e_test/features/book_reading/book_browsing.feature` and any other
book-reading feature that exercises the ReadingControlPanel
(`e2e_test/features/book_reading/reading_record.feature` or equivalent) to confirm no
regression.

**Files touched:**
- `frontend/src/composables/useBookReadingSnapBack.ts`

---

## Order rationale

1. Phase 1 first — it directly improves the feedback loop for future debugging. Any bug in
   the book-reading E2E suite now produces a correctly named screenshot immediately.
2. Phase 2 second — pure documentation; zero risk, high discoverability gain.
3. Phase 3 last — involves internal restructuring of a composable; lowest urgency and
   highest surface area for unintended change.

Each phase is independently deployable and leaves tests green.
