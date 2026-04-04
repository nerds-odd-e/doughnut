# Meta-plan: Phase 3 — PDF in main content (pdf.js)

**Parent intent:** [book-reading-read-a-range-plan.md](book-reading-read-a-range-plan.md) — **Phase 3** (*Book reading page shows the PDF with pdf.js (main content)*).

**Note on naming:** [book-reading-pdf-storage-subphases.md](book-reading-pdf-storage-subphases.md) already decomposed **Phase 1 + Phase 2** (attach, download, storage). **Phase 2 (GCP)** in the parent plan is **done**. This document **only** splits **Phase 3** into fine-grained sub-phases. It does **not** re-slice storage work.

**Architecture:** [doughnut-book-reading-architecture-roadmap.md](doughnut-book-reading-architecture-roadmap.md) — Story 2 (*pdf.js in main pane*; drawer and two-way sync are later phases).

**Planning discipline:** [.cursor/rules/planning.mdc](../.cursor/rules/planning.mdc) — one **user-visible** slice per sub-phase where possible; **integrator-visible** slices only when they unblock the next sub-phase without leaving unused production code; **all tests green** at the end of each sub-phase; **no dead code**; **no historical comments**; prefer **observable** tests (HTTP, Cypress, Vitest driving mounted UI); **at most one intentionally failing test** while driving a given sub-phase.

**Prerequisites:** Parent Phase 1–2 shipped (`GET …/book/file`, multipart attach, `BookReadingPage` outline + download). E2E: [e2e_test/features/book_reading/book_reading.feature](../e2e_test/features/book_reading/book_reading.feature).

---

## E2E scope (Phase 3)

- **No new scenario.** Extend the existing scenario **Attach PDF and see structure in the browser** with:

  `And I should see the beginning of the PDF book "top-maths.pdf"`

- **Assertion style:** Use **stable, human-readable text** that appears on **page 1** of the fixture (e.g. `cy.contains` on the pdf.js **text layer** or equivalent DOM that reflects extracted text). Avoid asserting on canvas pixels unless necessary.

---

## Fixture requirement (shared across sub-phases)

Committed [e2e_test/fixtures/book_reading/top-maths.pdf](../e2e_test/fixtures/book_reading/top-maths.pdf) (SP-3.1): valid small multi-page PDF for pdf.js; **page 1** text `DOUGHNUT_E2E_BOOK_PAGE1`, **page 2** `DOUGHNUT_E2E_BOOK_PAGE2` (SP-3.6 should assert page 1).

**Compatibility:** Download E2E compares bytes to that fixture; **outline** in the feature file stays aligned with **mocked Mineru** (PDF bytes and outline JSON are independent).

**Step glue:** [e2e_test/step_definitions/book_reading.ts](../e2e_test/step_definitions/book_reading.ts) derives the attached book stem from the `.pdf` filename for CLI and browser steps.

---

## Constants for all sub-phases below

- **Single file URL:** Reuse **same-origin** authenticated **`GET /api/notebooks/{notebook}/book/file`** (session cookie) for the viewer — no new download API.
- **Scope cap:** **Main content** PDF render only; **no** drawer move (Phase 4), **no** outline → PDF scroll sync (Phase 5), **no** PDF → outline highlight (Phase 6).
- **CI profile:** Same as today — **non-`prod`** / DB `BookStorage` for book-reading E2E.

**Explicitly not in this meta-plan:** GCS-specific viewer behavior, new attach formats, deleting blobs (Phase 8).

---

## Sub-phase ordering (maximal split, merge gates)

Each row is a **merge gate**: targeted `pnpm backend:verify` / `pnpm frontend:test` / `pnpm cli:test` as touched, and **book_reading** Cypress **spec** when E2E or fixture changes. **Deploy gate** per planning.mdc when the team’s cadence requires it between sub-phases.

### SP-3.1 — Replace `top-maths.pdf` with a real multi-page fixture — **done**

- **Outcome (test / integrator-visible):** New committed **`top-maths.pdf`** meets the **fixture requirement** above. **Download** byte comparison and **CLI attach** still use the same path and filename.
- **Tests:** Existing [book_reading.feature](../e2e_test/features/book_reading/book_reading.feature) **without** the new PDF step yet — structure table + download step **green**.
- **Deliverable cleanliness:** Remove reliance on the old invalid bytes; **no** pdf.js code until SP-3.2 (avoids unused dependencies).

### SP-3.2 — Viewer presentational core (buffer → page 1 canvas)

- **Outcome (user-visible in app once wired):** A focused component or module that, given a **loaded** PDF **ArrayBuffer** (or `TypedArray`), uses **pdf.js** to render **page 1** to a **canvas** (or documented equivalent) in the DOM. **Vite** worker URL / bundling for `pdfjs-dist` is **resolved in this sub-phase** so CI and `pnpm sut` both work.
- **Tests:** **Vitest** (mounted component or thin wrapper) supplying fixture bytes **read from the repo fixture** in the test — **no** Cypress yet. Covers happy parse + render path.
- **Deliverable cleanliness:** No orphan `pdfjs-dist` config without a **used** import path from production code; **no** duplicate worker setup left for “later.”

### SP-3.3 — Book reading page loads the PDF from the API into the viewer

- **Outcome (user-visible):** On [BookReadingPage](../frontend/src/pages/BookReadingPage.vue) (or a single co-located child used only there), when **`hasSourceFile`** is true, the app **fetches** the book file with **credentials**, passes the result into the SP-3.2 viewer, and the user **sees page 1** in the **main reading area** (layout may still show the outline list on the same page until Phase 4 refines layout — pick one clear arrangement and keep it for this sub-phase only).
- **Tests:** Extend **BookReadingPage** tests (mock SDK + **mock `fetch`** for `/book/file`) so the **wired** path is covered; manual smoke acceptable only if tests already prove the wiring.
- **Deliverable cleanliness:** **No** second fetch path for the same bytes for “preview” vs download unless justified; prefer **one** code path or shared helper.

### SP-3.4 — Loading state for PDF fetch and document open

- **Outcome (user-visible):** While bytes are loading or the document is opening, the user sees a **clear loading indicator** (spinner, skeleton, or existing app pattern) in the PDF pane; it **disappears** when page 1 is ready.
- **Tests:** Vitest asserting loading → rendered transition with mocked slow `fetch` or deferred pdf.js promise (minimal assertions, **observable** DOM).
- **Deliverable cleanliness:** Remove any **permanent** placeholder that could be mistaken for final UI once loading exists.

### SP-3.5 — Error state for missing / failed PDF load

- **Outcome (user-visible):** If the file request fails (**4xx/5xx**) or the bytes are not a valid PDF, show a **short, readable error** in the PDF pane (reuse existing error styling patterns if present).
- **Tests:** Vitest for failed fetch or reject from document load — **one** focused case is enough unless two failure modes differ in user-visible text.
- **Deliverable cleanliness:** **No** silent empty pane on error.

### SP-3.6 — E2E step: beginning of the PDF visible in the browser

- **Outcome (user-visible E2E):** The scenario **Attach PDF and see structure in the browser** includes  
  `And I should see the beginning of the PDF book "top-maths.pdf"`  
  implemented via **thin** step definition + **page object** method (reuse [bookReadingPage](../e2e_test/start/pageObjects/bookReadingPage.ts) patterns). Assert **known page-1 text** from the SP-3.1 fixture.
- **Tests:** Single feature [book_reading.feature](../e2e_test/features/book_reading/book_reading.feature); run with `--spec` for that file per [e2e_test.mdc](../.cursor/rules/e2e_test.mdc).
- **Deliverable cleanliness:** Step does not embed PDF parsing logic — **browser DOM / user-observable** assertions only.

---

## Optional consolidation (if the team prefers fewer gates)

These pairs are **safe to merge** without violating “no dead code” if merge gates feel heavy:

- **SP-3.4 + SP-3.5** → one sub-phase “loading and error states.”
- **SP-3.2 + SP-3.3** → one sub-phase “pdf.js + page wiring” (slightly larger diff).

The list above is the **maximal** split that still gives each sub-phase a **clear, testable outcome** and avoids leaving **unused** pdf.js or viewer code between merges.

---

## Sub-phase checklist (mechanical)

After each SP-3.*:

1. **Tests:** As touched — `pnpm frontend:test` (scoped if possible), `pnpm backend:verify` / `pnpm cli:test` only if those layers changed, Cypress **book_reading** spec if E2E or fixture changed.
2. **Dead code:** No unused pdf.js imports, no duplicate fetch helpers, no abandoned loading/error branches.
3. **Codegen:** Only if backend/OpenAPI changed (`pnpm generateTypeScript`).
4. **Plan:** Tick or adjust this doc; update [book-reading-read-a-range-plan.md](book-reading-read-a-range-plan.md) Phase 3 when execution starts or completes, per team habit.

---

## After Phase 3 execution

- **Phase 4** (drawer for outline) may **relayout** the same viewer component — keep the viewer **cohesive** so Phase 4 moves containers, not PDF logic.
- **Parent doc** completion hint: *“Depends on file access from Phases 1–2; keep viewer concerns in the main pane only.”* This meta-plan implements that scope until Phase 4.
