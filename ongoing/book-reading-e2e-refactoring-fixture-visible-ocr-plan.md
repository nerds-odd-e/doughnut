# Plan: Book-reading E2E — `refactoring.pdf`, committed MinerU JSON, visible-viewport OCR

**Intent:** Migrate book-reading Cypress E2E from `top-maths.pdf` + inline `_E2E_CONTENT_LIST` to `e2e_test/fixtures/book_reading/refactoring.pdf` + `mineru_output_for_refactoring.json`, add a maintainer script to refresh that JSON from **real** MinerU, then replace synthetic page-marker strings with **OCR of the visible PDF viewport** in **small steps**. **Do not** design or schedule production positioning fixes until a phase’s E2E assertion fails for the right reason (reproduces the misalignment).

**Grounding:** [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md) (Story 2 sync), [`ongoing/book-reading-user-stories.md`](book-reading-user-stories.md), [`ongoing/book-reading-read-a-range-plan.md`](book-reading-read-a-range-plan.md). **Planning rules:** `.cursor/rules/planning.mdc`.

**Current hooks (for implementers):**

- Feature: [`e2e_test/features/book_reading/book_reading.feature`](../e2e_test/features/book_reading/book_reading.feature) (`@mockMineruLib`, `top-maths.pdf`, outline table, `DOUGHNUT_E2E_BOOK_PAGE*`).
- Stub: [`e2e_test/python_stubs/mineru_site/mineru/cli/common.py`](../e2e_test/python_stubs/mineru_site/mineru/cli/common.py) — `_E2E_CONTENT_LIST` written as `{stem}_content_list.json` by fake `do_parse`.
- Page object: [`e2e_test/start/pageObjects/bookReadingPage.ts`](../e2e_test/start/pageObjects/bookReadingPage.ts) — `expectPdfBeginningVisible` OCRs **first** page canvas; `expectPdfPageMarkerVisible` OCRs **full** canvas of `[data-page-number="N"]` plus `assertJumpedPageCanvas` (GlobalBar vs canvas top).
- Steps: [`e2e_test/step_definitions/book_reading.ts`](../e2e_test/step_definitions/book_reading.ts) — CLI expectations hard-code `Main Topic 1` / `Subtopic 1.1`.

---

## Research snapshot (symptoms already observed; **not** a fix backlog)

These explain why “layout ↔ PDF” can look **a few lines** wrong **without** implying a specific patch until E2E reproduces it:

1. **pdf.js `scrollPageIntoView` + XYZ:** The internal `scrollIntoView` sets `scrollTop` so the destination **spot** is aligned to the **top** of the scroll container, not vertically centered. We pass an XYZ point at the **center** of the MinerU bbox → content **above** that point (including part of the bbox and headings) often sits **above** the visible band.
2. **PDF → layout:** `pdfViewerViewportTopYDown` uses the **vertical midpoint** of the visible slice; `viewportCurrentAnchorIdFromAnchorPage` compares that to each row’s **`y0` (bbox top)**. That is a different vertical convention than (1), so highlight and scroll target can drift relative to each other.
3. **MinerU bbox vs PDF user space, rotation:** Committed JSON assumes MinerU’s bbox coordinates match pdf.js viewport space; extraction jitter and **unhandled page rotation** in bbox navigation can add variable error.

**Plan discipline:** Record positioning fixes **only after** a phase adds/changes an assertion that **fails** on `main` (or on the branch at that point). Each fix should be the **minimum** change to satisfy **that** phase’s single assertion.

---

## Phase 1 — Fixture swap + outline/CLI expectations driven by JSON (still full-canvas OCR)

**User-visible outcome:** Book-reading E2E still passes end-to-end, but uses **`refactoring.pdf`** and a **file-backed** MinerU output shape identical to what the CLI attach path expects today.

**Deliverables:**

1. Use [`e2e_test/fixtures/book_reading/refactoring.pdf`](../e2e_test/fixtures/book_reading/refactoring.pdf) in the feature Background (`attach` + any step that names the file). Drop **`top-maths.pdf`** from this feature’s contract (remove or retain the file only if another suite still needs it; today only book-reading E2E + a couple of **frontend** tests import it — update or split fixtures as needed).
2. Add **`e2e_test/fixtures/book_reading/mineru_output_for_refactoring.json`** containing the MinerU **`content_list`** array (same JSON shape currently embedded in `_E2E_CONTENT_LIST`).
3. Change the E2E stub’s `do_parse` to **read that JSON** from a path resolved relative to the repo (or copy embedded into the temp output layout the CLI expects — keep **one** source of truth: the committed file).
4. Update **Gherkin** outline table, **CLI** substring expectations in `book_reading.ts`, **`readBook` stem** (`refactoring` from filename), and **scroll helpers** (`scrollPdfBookReaderToBringPage2IntoPrimaryView` offset, `scrollPdfBookReaderDownWithinSamePageForNextBbox` delta, same-page row titles) so they match the **committed** JSON and real page indices for `refactoring.pdf`.
5. Replace **`DOUGHNUT_E2E_BOOK_PAGE1` / `PAGE2`** in the feature with **short, stable substrings** that already appear in `refactoring.pdf` and survive OCR (discover by running OCR once on rendered pages or by inspection of PDF text). **Still assert on full-page canvas** as today (`expectPdfBeginningVisible` / `expectPdfPageMarkerVisible`) so this phase does **not** depend on positioning fixes.

**Tests:** Relevant Cypress spec(s) for [`book_reading.feature`](../e2e_test/features/book_reading/book_reading.feature) only.

**Collateral updates (same phase if touched):** [`ongoing/book-reading-user-stories.md`](book-reading-user-stories.md) example filename; [`ongoing/book-reading-read-a-range-plan.md`](book-reading-read-a-range-plan.md) “shipped” references if they name `top-maths.pdf`; frontend/CLI tests that import `top-maths.pdf` only for book-reading parity.

---

## Phase 2 — Maintainer script: real MinerU → refresh `mineru_output_for_refactoring.json`

**User-visible outcome:** None (developer workflow only).

**Deliverables:**

1. Script under **`e2e_test/fixtures/book_reading/`** (e.g. `regenerate_mineru_output_for_refactoring.sh` or small Python wrapper) that:
   - Documents activation: `source .venv-mineru/bin/activate` (or equivalent) before invoking MinerU.
   - Runs the **real** MinerU pipeline against `refactoring.pdf` into a temp output dir.
   - Copies or normalizes the resulting **`_content_list.json`** (or equivalent) into **`mineru_output_for_refactoring.json`** in the **array** form the stub already emits.
2. Short comment at top of the JSON or in the script: **when to re-run** (PDF changed, MinerU version bump, outline drift).

**Tests:** Optional smoke: script exits 0 in an environment with venv + MinerU (may be manual / CI optional).

---

## Phase 3 — First assertion: OCR **visible viewport** only (initial load / “beginning”)

**User-visible outcome:** “PDF is open and readable” is asserted against **what appears in the scrollable viewer**, not the entire first page bitmap.

**Deliverables:**

1. Extend [`bookReadingPage.ts`](../e2e_test/start/pageObjects/bookReadingPage.ts) (or a small helper) to derive a **crop** or sampling region from the intersection of **`[data-testid="pdf-book-viewer"]`** scrollport with the target page’s canvas (or equivalent: OCR only the **on-screen** portion of the rendered page).
2. Change **one** step first: e.g. `I should see the beginning of the PDF book …` to require a **known substring** from `refactoring.pdf` in that **visible** OCR text.
3. **Run E2E.** If it fails because text is **above** or **below** the visible band, implement the **smallest** production fix so **only** this assertion passes. **Do not** “fix forward” for later steps here.

**Tests:** Cypress scenario that contains the updated step; no new marker scenarios yet.

---

## Phase 4 — Visible-viewport OCR: outline jump → page 2 (or first cross-page marker)

**Deliverables:** Replace **one** `Then I should see PDF page … marker …` (or successor phrasing) with visible-viewport OCR + substring chosen from real content on the **target** page after jump. **Run → fail expected if mispositioned → minimal production fix → green.**

**Tests:** Single scenario path.

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

For each phase: failing test (when introducing new behavior) → pass → refactor; run the **book_reading** Cypress spec(s); update this plan’s snapshot or archive when done.

## Open points (decide during implementation)

- **Notebook / Gherkin names:** Keep “Top Maths” as fiction or rename to match `refactoring` / book title — product-neutral either way; pick one and update user-story examples consistently.
- **Whether `assertJumpedPageCanvas` (GlobalBar vs canvas top) stays** when switching to viewport OCR: it encodes a **chrome** expectation; keep, relax, or replace with a visible-region check — decide when Phase 3 fails or passes.
- **OCR flakiness:** Tolerance for whitespace/punctuation (normalize task output or use flexible `include` substrings); timeout already high for `ocrCanvasImage`.

---

## Document maintenance

When phases ship, trim completed checklist noise here; point [`ongoing/book-reading-read-a-range-plan.md`](book-reading-read-a-range-plan.md) at this doc for E2E fixture/OCR strategy if the main plan should stay short.
