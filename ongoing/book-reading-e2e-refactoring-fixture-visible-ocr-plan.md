# Plan: Book-reading E2E — `refactoring.pdf`, committed MinerU JSON, visible-viewport OCR

**Intent:** Migrate book-reading Cypress E2E from `top-maths.pdf` + inline `_E2E_CONTENT_LIST` to `e2e_test/fixtures/book_reading/refactoring.pdf` + `mineru_output_for_refactoring.json`, add a maintainer script to refresh that JSON from **real** MinerU, then replace synthetic page-marker strings with **OCR of the visible PDF viewport** in **small steps**. **Phase 1** is split into **sp-1.1** (PDF-only swap + fix OCR failures) and **sp-1.2** (same mock content, file-backed JSON). **Phase 2** is split into **sp-2.1** (script + capture to a **different** JSON filename) and **sp-2.2** (promote to canonical fixture JSON + fix E2E). **Do not** design or schedule production positioning fixes until a phase’s E2E assertion fails for the right reason (reproduces the misalignment).

**Grounding:** [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md) (Story 2 sync), [`ongoing/book-reading-user-stories.md`](book-reading-user-stories.md), [`ongoing/book-reading-read-a-range-plan.md`](book-reading-read-a-range-plan.md). **Planning rules:** `.cursor/rules/planning.mdc`.

**Current hooks (for implementers):**

- Feature: [`e2e_test/features/book_reading/book_reading.feature`](../e2e_test/features/book_reading/book_reading.feature) (`@mockMineruLib`, `refactoring.pdf`, outline table).
- Stub: [`e2e_test/python_stubs/mineru_site/mineru/cli/common.py`](../e2e_test/python_stubs/mineru_site/mineru/cli/common.py) — loads committed [`mineru_output_for_refactoring.json`](../e2e_test/fixtures/book_reading/mineru_output_for_refactoring.json) into `_E2E_CONTENT_LIST`; fake `do_parse` writes `{stem}_content_list.json` from that list.
- Page object: [`e2e_test/start/pageObjects/bookReadingPage.ts`](../e2e_test/start/pageObjects/bookReadingPage.ts) — `expectPdfBeginningVisible` OCRs the **first** page canvas and asserts substring **`Code Refactoring`**. **`expectPdfPageMarkerVisible`** OCRs the **full** page canvas. **`expectPdfViewerViewportScreenshotContains(marker, pageNumber)`** waits for that page’s canvas to have ink, then OCRs a Cypress **element screenshot** of `[data-testid="pdf-book-viewer"]` (outline-jump scenario). The beginning step still uses the **full** first-page canvas.
- Steps: [`e2e_test/step_definitions/book_reading.ts`](../e2e_test/step_definitions/book_reading.ts) — CLI expectations include refactoring outline substrings (e.g. `Protecting Intention in Working Software`, `Easier to Change`).

---

## Research snapshot (symptoms already observed; **not** a fix backlog)

These explain why “layout ↔ PDF” can look **a few lines** wrong **without** implying a specific patch until E2E reproduces it:

1. **pdf.js `scrollPageIntoView` + XYZ:** The internal `scrollIntoView` sets `scrollTop` so the destination **spot** is aligned to the **top** of the scroll container, not vertically centered. We pass an XYZ point at the **center** of the MinerU bbox → content **above** that point (including part of the bbox and headings) often sits **above** the visible band.
2. **PDF → layout:** `pdfViewerViewportTopYDown` uses the **vertical midpoint** of the visible slice; `viewportCurrentAnchorIdFromAnchorPage` compares that to each row’s **`y0` (bbox top)**. That is a different vertical convention than (1), so highlight and scroll target can drift relative to each other.
3. **MinerU bbox vs PDF user space, rotation:** Committed JSON assumes MinerU’s bbox coordinates match pdf.js viewport space; extraction jitter and **unhandled page rotation** in bbox navigation can add variable error.

**Plan discipline:** Record positioning fixes **only after** a phase adds/changes an assertion that **fails** on `main` (or on the branch at that point). Each fix should be the **minimum** change to satisfy **that** phase’s single assertion.

---

## Phase 1 — `refactoring.pdf` + file-backed mock (still full-canvas OCR)

**User-visible outcome (end of Phase 1):** Book-reading E2E passes end-to-end with **`refactoring.pdf`** and **`mineru_output_for_refactoring.json`** as the single source of truth for the MinerU **`content_list`** shape the stub emits (content still **logically** the same as today’s inline mock until Phase 2 replaces it with real MinerU output).

Phase 1 stops after **sp-1.2**. Outline table, CLI expectations, scroll helpers, and **`DOUGHNUT_E2E_BOOK_PAGE*`** must match **`refactoring.pdf`** and the mock’s page indices; assertions stay **full-page canvas** (`expectPdfBeginningVisible` / `expectPdfPageMarkerVisible`) — no viewport-only OCR or positioning fixes in Phase 1.

---

### sp-1.1 — Replace the PDF only

**Scope:** Switch the feature to [`e2e_test/fixtures/book_reading/refactoring.pdf`](../e2e_test/fixtures/book_reading/refactoring.pdf) (`attach` + any step that names the file). **No** change to `_E2E_CONTENT_LIST`, stub wiring, or JSON on disk — mocked data and **how** it is mocked stay the same.

**Expectation:** At least one scenario in [`book_reading.feature`](../e2e_test/features/book_reading/book_reading.feature) should **fail**, because OCR reads **`refactoring.pdf`** while expectations may still assume **`top-maths.pdf`** text.

**Fix:** Update only what is needed for green: e.g. **`DOUGHNUT_E2E_BOOK_PAGE1` / `PAGE2`**, Gherkin outline examples if they pin PDF-visible strings, CLI substring expectations in `book_reading.ts`, **`readBook` stem** (`refactoring` from filename), and page-object scroll helpers (offsets/deltas, same-page row titles) so they match **`refactoring.pdf`** and the **unchanged** mock outline. Drop **`top-maths.pdf`** from this feature’s contract (remove or keep the file if another suite still needs it — book-reading E2E + some **frontend** tests may still reference it until updated).

**Tests:** Relevant Cypress spec(s) for `book_reading.feature` only.

---

### sp-1.2 — Move mocked data to JSON (content unchanged)

**Scope:** Add **`e2e_test/fixtures/book_reading/mineru_output_for_refactoring.json`** with the MinerU **`content_list`** array — **byte-for-byte / semantically identical** to what is currently embedded in `_E2E_CONTENT_LIST` (no real MinerU capture yet). Change the E2E stub’s `do_parse` to **read that file** from a path resolved relative to the repo (single source of truth: the committed JSON).

**Expectation:** E2E **still passes** after the extraction; behavior should be unchanged aside from where the mock payload lives.

**Tests:** Same as sp-1.1.

**Collateral updates (if touched in Phase 1):** [`ongoing/book-reading-user-stories.md`](book-reading-user-stories.md) example filename; [`ongoing/book-reading-read-a-range-plan.md`](book-reading-read-a-range-plan.md) “shipped” references if they name `top-maths.pdf`; frontend/CLI tests that import `top-maths.pdf` only for book-reading parity.

---

## Phase 2 — Maintainer script: real MinerU → canonical fixture JSON

**User-visible outcome:** None (developer workflow + refreshed fixture content).

---

### sp-2.1 — Script + capture to a differently named JSON

**Scope:** Add a script under **`e2e_test/fixtures/book_reading/`** (e.g. `regenerate_mineru_output_for_refactoring.sh` or a small Python wrapper) that:

- Documents activation: `source .venv-mineru/bin/activate` (or equivalent) before invoking MinerU.
- Runs the **real** MinerU pipeline against `refactoring.pdf` into a temp output dir.
- Writes the normalized **`content_list`** array to a **new** committed JSON file whose name is **not** `mineru_output_for_refactoring.json` (e.g. `mineru_output_for_refactoring.captured.json` or `mineru_output_for_refactoring.from_mineru.json` — pick one naming convention and stick to it).

Short comment in the script or adjacent README snippet: **when to re-run** (PDF changed, MinerU version bump, outline drift).

**Tests:** Optional smoke: script exits 0 where venv + MinerU exist (may be manual / CI optional). E2E continues to use **`mineru_output_for_refactoring.json`** (unchanged content) until sp-2.2.

---

### sp-2.2 — Promote captured JSON to the fixture + fix E2E

**Scope:** Replace **`mineru_output_for_refactoring.json`** with the real MinerU output (rename/copy from the sp-2.1 artifact so the canonical filename is **`mineru_output_for_refactoring.json`**). Update Gherkin, CLI expectations, scroll helpers, and page markers so E2E matches **real** outline geometry and text for `refactoring.pdf`.

**Tests:** `book_reading.feature` (and any collateral tests touched by expectation changes).

---

## Phase 3 — First assertion: beginning of book via OCR (initial load) — **shipped**

**User-visible outcome (met):** The first scenario’s “beginning of PDF” step proves the reader shows the **opening** of the book using OCR (no DOM text layer), via a **fixture substring** that only appears at the start of `refactoring.pdf`.

**Implemented:**

- [`expectPdfBeginningVisible`](../e2e_test/start/pageObjects/bookReadingPage.ts) OCRs the **first** page’s canvas and requires **`Code Refactoring`** (see scenario [`book_reading.feature`](../e2e_test/features/book_reading/book_reading.feature) → `I should see the beginning of the PDF book "refactoring.pdf"`).

**Original plan nuance (not done, optional follow-up):** The first version of this phase called for OCR on **only** the intersection of **`[data-testid="pdf-book-viewer"]`** with the on-screen part of the page (not the full first-page bitmap). That **viewport crop** is **not** implemented; the test still reads the **entire** first-page canvas. The **`Code Refactoring`** marker is strong enough that the scenario would fail if the viewer were scrolled so far that the title band were off-canvas, but it does not strictly assert “visible viewport band” geometry. Add a crop helper later if a failing scenario needs it.

**Tests:** `See book structure and beginning of PDF in the browser` (beginning step only for this phase).

---

## Phase 4 — Visible-viewport OCR: outline jump → page 2 (or first cross-page marker) — **shipped**

**Deliverables:** Replace **one** `Then I should see PDF page … marker …` (or successor phrasing) with visible-viewport OCR + substring chosen from real content on the **target** page after jump. **Run → fail expected if mispositioned → minimal production fix → green.**

**Implemented:**

- Feature [`book_reading.feature`](../e2e_test/features/book_reading/book_reading.feature): scenario **Outline row jumps the PDF to the anchored page** uses `Then I should see in the book reader visible PDF viewport on page 2 text including "Strengthening the Code"`.
- Page object: `expectPdfViewerViewportScreenshotContains` — wait for `[data-page-number]` canvas ink, then `cy.get('[data-testid="pdf-book-viewer"]').screenshot` → `readFile` base64 → `ocrCanvasImage`; step `I should see in the book reader visible PDF viewport on page {int} text including {string}`.
- **Production:** [`mineruOutlineV1BboxToXyzDestArray`](../frontend/src/lib/book-reading/mineruOutlineV1PageIndex.ts) targets a point **above** the MinerU bbox top by a fixed margin so pdf.js top-biased `scrollPageIntoView` still leaves the section title in the visible band (viewport OCR failed until this).

**Tests:** That scenario path only for the new step; other scenarios still use full-canvas `expectPdfPageMarkerVisible` until Phases 5–7.

---

## Phase 5 — Visible-viewport OCR: scroll PDF → viewport-current (page 2 in view)

**Deliverables:** Same pattern: **one** assertion migration, then fix only what that assertion proves.

**Tests:** The scroll-to-page-2 / viewport-current scenario that still relied on full-canvas or marker wording.

---

## Phase 6 — Visible-viewport OCR: short viewport + aside visibility

**Deliverables:** Migrate the **short viewport** scenario’s PDF text assertion to visible-viewport OCR; fix production if the failure is positioning-related.

**Tests:** That scenario only for the PDF OCR part.

---

## Phase 7 — Visible-viewport OCR: same-page bbox scroll + viewport-current vs selected

**Deliverables:** Replace remaining PDF-side assertion(s) with visible-viewport OCR; tune `deltaPx` (and any bbox-dependent scroll) against **real** `mineru_output_for_refactoring.json` geometry. **One assertion migration per sub-step** if two failures appear; avoid bundling multiple production fixes without a failing test each time.

**Tests:** Same-page scroll scenario(s).

---

## Phase checklist (per `.cursor/rules/planning.mdc`)

For each phase and sub-phase: failing test (when introducing new behavior) → pass → refactor; run the **book_reading** Cypress spec(s); update this plan’s snapshot or archive when done. **sp-1.1** explicitly expects at least one failure after the PDF swap, then fixes it; **sp-1.2** should stay green throughout.

| Phase | Status |
|-------|--------|
| 1 (sp-1.1, sp-1.2) | Done — `refactoring.pdf` + `mineru_output_for_refactoring.json` |
| 2 (sp-2.1, sp-2.2) | Done — regenerate script + real MinerU fixture |
| 3 | **Done** — beginning step OCR + **`Code Refactoring`**; viewport-only crop deferred (see Phase 3 section) |
| 4 | **Done** — outline-jump scenario: PDF viewer element screenshot OCR + MinerU scroll target margin above bbox top |
| 5–7 | Not done — migrate remaining page-marker / scroll scenarios to visible-viewport OCR + minimal fixes per phase |

## Open points (decide during implementation)

- **Notebook / Gherkin names:** Keep “Top Maths” as fiction or rename to match `refactoring` / book title — product-neutral either way; pick one and update user-story examples consistently.
- **Whether `assertJumpedPageCanvas` (GlobalBar vs canvas top) stays** when switching marker steps to viewport OCR: it encodes a **chrome** expectation; keep, relax, or replace with a visible-region check — decide when Phase 4+ touches those assertions.
- **OCR flakiness:** Tolerance for whitespace/punctuation (normalize task output or use flexible `include` substrings); timeout already high for `ocrCanvasImage`.

---

## Document maintenance

When phases ship, trim completed checklist noise here; point [`ongoing/book-reading-read-a-range-plan.md`](book-reading-read-a-range-plan.md) at this doc for E2E fixture/OCR strategy if the main plan should stay short.
