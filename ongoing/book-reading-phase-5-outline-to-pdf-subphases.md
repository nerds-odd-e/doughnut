# Book reading — Phase 5 sub-phases (outline click → PDF position)

**Parent delivery plan:** [`ongoing/book-reading-read-a-range-plan.md`](book-reading-read-a-range-plan.md) — **Phase 5** (*Click a layout range → PDF jumps to the right place*).

**UX/UI guidance:** [`ongoing/book-reading-ux-ui-roadmap-phases-4-7.md`](book-reading-ux-ui-roadmap-phases-4-7.md) — *Phase 5 — Click a range → PDF jumps to the right place*.

**Architecture:** [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md) — anchors (`pdf.mineru_outline_v1`), `BookRange` start/end, layout → PDF sync direction.

**Planning rules:** [`.cursor/rules/planning.mdc`](.cursor/rules/planning.mdc) — one **user-visible** slice per sub-phase where possible, scenario-first ordering, **no dead code** (production code exercised by that phase’s **E2E** for normal paths, or by **unit tests** for deliberate non–happy-path contracts), **at most one intentionally failing test** while driving a given sub-phase, phase-complete cleanup before the next.

**Note on “phase 2”:** If that phrase was meant to refer to **Phase 2 (GCP storage)** in the parent plan, that phase is already **done** and is not subdivided here. This document decomposes **Phase 5** only (the **layout → PDF** half of two-way sync; **PDF → outline** is Phase 6).

**Anchor shape (v1):** `startAnchor.value` / `endAnchor.value` are JSON strings produced by the CLI outline path (e.g. `page_idx`, optional `bbox` — see `cli/python/mineru_book_outline.py`). Phase 5 should treat **start** as the navigation target unless product explicitly chooses otherwise.

**Prerequisites:** Phases 1–4 shipped (`PdfBookViewer`, drawer outline, `GET …/book` + file).

---

## Cross-cutting constraints

- **Tests:** Prefer extending [`e2e_test/features/book_reading/book_reading.feature`](e2e_test/features/book_reading/book_reading.feature) and [`e2e_test/start/pageObjects/bookReadingPage.ts`](e2e_test/start/pageObjects/bookReadingPage.ts) over new parallel specs, unless a scenario needs isolation.
- **Observable assertions:** Continue to prefer **canvas/OCR** or other **user-observable** signals over pdf.js internals, consistent with existing “beginning of PDF” checks — add markers on **additional PDF pages** in the fixture if page-change detection requires it.
- **Cohesion:** Keep “parse anchor → scroll command” in one small, testable module; wire it from `BookReadingPage` + `PdfBookViewer` so later Phase 6 can reuse anchor/page logic without duplicating parsing.
- **Interim behavior:** Allowed only if called out and **removed or replaced** by a later sub-phase in this list (planning.mdc interim rule).

---

## Phase 5.1 — Click a range → PDF shows a **different page** (page index only)

**User outcome:** Choosing an outline entry whose **start** anchor points at **another page** than the current view moves the PDF so that **that page** is brought into view (v1: use **`page_idx`** from parsed JSON; ignore `bbox` until 5.4).

**Main behavior:** Outline rows are proper **interactive controls** (e.g. button semantics), click invokes navigation, `PdfBookViewer` exposes a **narrow** imperative API (e.g. scroll to 0-based page index) backed by pdf.js.

**Tests:**

- **E2E:** Extend fixture / stub outline so at least one row is **not** page 0; after click, assert a **page-specific** observable (second-page OCR marker or equivalent). Reuse attach flow from the existing scenario.
- **Unit (black-box):** Parse representative `pdf.mineru_outline_v1` JSON strings → extracted page index (and explicit behavior when `page_idx` absent — see 5.5 if you defer handling, document the interim).

**Phase-complete:** No unused bbox logic yet; no “selected row” styling required beyond focus/hover defaults unless already shared with app patterns.

---

## Phase 5.2 — **Selected** outline row after navigation

**User outcome:** After using the outline to jump, the user sees **which** section they asked for — **selected** state is visually distinct from the Phase 6 **viewport-current** highlight (two styles or one merged design is an [open UX question](book-reading-ux-ui-roadmap-phases-4-7.md); this sub-phase only guarantees **selection** after click).

**Tests:**

- **E2E:** Click a known row → assert stable **DOM** contract (`aria-current`, `data-` attribute, or focused control) on that row only.
- **Unit:** Only if there is a **pure** style/class helper worth testing in isolation; avoid mirroring DOM structure in tests.

**Phase-complete:** Remove any duplicate or unused state variables; selection state must be **read** by the template (no dead refs).

---

## Phase 5.3 — Jump lands **below chrome** (safe area / header) — **done**

**User outcome:** After a jump, the target page is not **lost under** `GlobalBar` or notches; scrolling accounts for **fixed header height** / `safe-area-inset` in a minimal, product-consistent way.

**Tests:**

- **E2E:** Assert **observable** layout (e.g. first visible canvas/page block’s position relative to a known chrome element, or scroll container metrics) within tolerance — pick **one** approach and document it in the step/page object to avoid flaky pixel tests.
- **Unit:** Optional only for pure “offset math” extracted as **input → number** without DOM.

**Implemented:** [`e2e_test/start/pageObjects/bookReadingPage.ts`](e2e_test/start/pageObjects/bookReadingPage.ts) — canvas top vs `nav.daisy-navbar` bottom inside `[data-testid="book-reading-page"]`, 2px tolerance, no `scrollIntoView` before OCR. [`BookReadingPage.vue`](frontend/src/pages/BookReadingPage.vue) — `padding-top: env(safe-area-inset-top, 0px)` on `.book-reading-page`.

**Phase-complete:** No duplicate scroll calls; remove interim “always scroll top” hacks from 5.1 if this replaces them.

---

## Phase 5.4 — **`bbox`** refinement on the same page — **done**

**User outcome:** When the start anchor includes **`bbox`**, the PDF scrolls so the **region** is in view, not only the page top (still v1: document supported bbox coordinate space vs pdf.js).

**Tests:**

- **E2E:** Two ranges on the **same** `page_idx` with different bboxes produce **discernible** scroll or visible-region change (OCR on distinct markers placed in fixture, or agreed observable).
- **Unit:** Bbox → scroll destination math for fixed examples.

**Phase-complete:** If bbox is optional in data, ensure **no** unused branches — page-only path still covered by 5.1 E2E.

**Implemented:** [`parseMineruOutlineV1StartAnchor`](frontend/src/lib/book-reading/mineruOutlineV1PageIndex.ts), [`mineruOutlineV1BboxToXyzDestArray`](frontend/src/lib/book-reading/mineruOutlineV1PageIndex.ts) (XYZ at bbox center, null zoom — keeps page-width; avoids FitR rescaling that removed scroll delta in E2E), [`PdfBookViewer`](frontend/src/components/book-reading/PdfBookViewer.vue) `scrollToMineruOutlineV1Target`, E2E stub bboxes in [`e2e_test/python_stubs/mineru_site/mineru/cli/common.py`](e2e_test/python_stubs/mineru_site/mineru/cli/common.py), scenario *Outline rows on the same page scroll the PDF to different places* in [`e2e_test/features/book_reading/book_reading.feature`](e2e_test/features/book_reading/book_reading.feature).

---

## Phase 5.5 — **Graceful** handling of bad or minimal anchors — **done**

**User outcome:** A row whose anchor JSON **misses** `page_idx`, is **invalid JSON**, or cannot be mapped does **not** break the viewer; behavior is **predictable** — **no-op** (no scroll, outline selection unchanged) via early return in [`BookReadingPage.vue`](frontend/src/pages/BookReadingPage.vue) when [`parseMineruOutlineV1StartAnchor`](frontend/src/lib/book-reading/mineruOutlineV1PageIndex.ts) returns `null`.

**Tests:**

- **Unit:** [`frontend/tests/lib/book-reading/mineruOutlineV1PageIndex.spec.ts`](frontend/tests/lib/book-reading/mineruOutlineV1PageIndex.spec.ts) — invalid JSON, missing/wrong `page_idx`, non-object JSON roots; `parseMineruOutlineV1StartAnchor` **never throws** (contract documented on the function and module header in `mineruOutlineV1PageIndex.ts`).
- **E2E:** **Unit-only** for this sub-phase — seeding a book with a deliberately bad outline through attach would be heavy; production outline clicks still call `parseMineruOutlineV1StartAnchor`, so the parser is not dead code.

**Phase-complete:** [`PdfBookViewer.vue`](frontend/src/components/book-reading/PdfBookViewer.vue) `flushPendingNavigation` documents why rejected outline jumps from pdf.js are ignored (`void` + commented `.catch`).

---

## Phase 5.6 — **Async** navigation feedback (if needed) — **deferred**

**Deferral:** Not required for Phase 5 closure for now; rationale and when to reopen — [`ongoing/book-reading-read-a-range-plan.md`](book-reading-read-a-range-plan.md) Phase 5 completion hint.

**User outcome:** If scrolling/page render requires **awaiting** pdf.js work, the UI avoids **jarring** blank flashes: **subtle** pending state on the row or a **minimal** main-area indicator consistent with `apiCallWithLoading`-style patterns **only where** async is genuinely user-visible.

**Tests:**

- **E2E:** Include this sub-phase **only** if a reliable observable (e.g. `aria-busy`, disabled state) can be asserted; otherwise **merge** into 5.1–5.4 and **omit** this sub-phase to avoid dead UI flags.

**Phase-complete:** Remove unused loading state if measurement shows navigation is synchronous in practice.

---

## Phase 5.7 — **Keyboard** activation matches click

**User outcome:** Focus an outline control and activate with **Enter** / **Space** (platform-appropriate) → same navigation and **selected** state as click (mobile: N/A but must not regress touch).

**Tests:**

- **E2E:** One scenario step: keyboard path to same postcondition as 5.1 (and 5.2 if selection exists).

**Phase-complete:** No duplicate handlers; single code path for “activate row” preferred.

---

## Phase 5.8 — **Nested** outline interaction pattern (only if expand/collapse exists)

**User outcome:** If/when the outline is **not** purely flat — e.g. chevron toggles expand/collapse — **go-to** does not fire accidentally when toggling (UX: chevron vs row body). **Skip** this sub-phase entirely while the UI remains a **flat** list with no expand control.

**Tests:**

- **E2E:** Expand vs navigate as separate actions with distinct assertions.

**Phase-complete:** Remove dead click handlers on non-interactive wrappers.

---

## Execution checklist (per sub-phase)

1. Add or extend the **failing** test(s) for **this** sub-phase only; confirm wrong reason.
2. Implement the **smallest** change; keep **one** intentionally failing test at a time across the repo where possible.
3. Run **relevant** Cypress spec(s) (`e2e_test.mdc` — single feature).
4. Run **frontend** unit tests touched (`PdfBookViewer.spec.ts` / parser spec as applicable).
5. **Clean up** dead code and redundant assertions; update this file and the parent plan’s Phase 5 line when the story advances.
6. **Deploy gate** per team practice ([`planning.mdc`](.cursor/rules/planning.mdc) phase discipline).

---

## After Phase 5

- Update [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md) if wire format or navigation rules diverge from the current Story 2 table.
- Phase 6 adds **viewport → current range**; reuse parsing and page/bbox helpers rather than forking.
