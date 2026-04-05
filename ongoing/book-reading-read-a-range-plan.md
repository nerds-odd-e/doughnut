# Plan: Read a range of a book (Story 2)

**User story:** [`ongoing/book-reading-user-stories.md`](book-reading-user-stories.md) — *Story: Read a range of a book*.

**Architecture (boundaries, storage, pdf.js, drawer, two-way sync):** [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md) — *Story 2 — Read a range (direction)*.

**Prerequisites:** *Story: Add a PDF book to a notebook and browse its structure* is done (CLI `/attach`, `GET .../book` outline in browser).

**Planning rules:** `.cursor/rules/planning.mdc` — one **user-visible** behavior per phase, scenario-first ordering, **E2E** (or equivalent observable surface) per phase where the story is UI- or integration-shaped, test-first workflow when adding behavior.

**Not executed in this document** — phases are intent and order only.

---

## Principles for this work

- **Same attach surface:** Extend **`POST /api/notebooks/{notebook}/attach-book`** (or keep one logical “attach book” operation) so the **CLI sends the PDF and outline together**; avoid a parallel “upload-only” product API unless a hard technical constraint appears.
- **Observable tests:** Prefer Cypress + CLI/subprocess or HTTP assertions over tests that pin internal PDF or storage helpers, except for pure mapping (anchor ↔ scroll) if extracted as a small black-box unit.
- **Storage rollout:** **Phase 1** and **Phase 2** are **done** (multipart **`POST …/attach-book`**, **`GET …/book/file`** for the in-app viewer, **`BookStorage`** with **`DbBookStorage`** for non-`prod` and **`GcsBookStorage`** for **`prod`**, E2E on DB storage in CI, prod bucket and ops in [`docs/gcp/prod_env.md`](../docs/gcp/prod_env.md) §7). Book PDF attach limits and error bodies: **`spring.servlet.multipart`** (100MB) and **`ApiError`**; GCS orphan cleanup when a book row is removed is **not** implemented yet (documented in that section).
- **At most one intentionally failing test** while driving a given phase (see planning rule).

---

## Phase 1 — CLI attach uploads the PDF; frontend can obtain the file (dev / test storage) — **done**

**User outcome:** After **`/attach`** from the CLI, the **PDF bytes** are stored server-side (local or test bucket), linked on the **Book** (`sourceFileRef` or agreed field), and the **browser** can **load** the file for the book reading page via **`GET …/book/file`**. **E2E:** book-reading scenario covers attach → outline + rendered PDF (DB storage in CI; no real GCP required).

**Shipped:** Multipart attach, **`DbBookStorage`**, **`GET …/book/file`**, Cypress book-reading scenario (DB storage in CI).

---

## Phase 2 — Production object storage on GCP — **done**

**User outcome:** In **production configuration**, uploaded PDFs are stored in a **GCP bucket**; the **book reading page** still **loads** the PDF the same way as Phase 1. **E2E:** **same spec** as Phase 1 where feasible (e.g. CI uses emulator, fake GCS, or a dedicated test bucket—document the chosen approach).

**Shipped:** **`GcsBookStorage`** under **`prod`** only, bucket + IAM in **`docs/gcp/prod_env.md`**, wiring test for prod **`BookStorage`** bean; CI book E2E stays on DB profile (no real GCS in CI).

---

## Phase 3 — Book reading page shows the PDF with pdf.js (main content) — **done**

**User outcome:** On the book reading route, the user **sees the attached PDF rendered** in the **main content area** using **pdf.js**. **E2E:** attach via CLI → book page shows page 1; scenario asserts fixture page-1 marker text is recoverable from the rendered canvas (see sub-phases doc for the OCR-based check).

**Shipped:** `PdfBookViewer` (pdf.js `PDFViewer`, all pages → scrollable canvas column), `BookReadingPage` fetch from **`GET …/book/file`** with loading and error UI, scenario step `And I should see the beginning of the PDF book "top-maths.pdf"`.

**Completion hint:** Outline still lives in the main column next to the viewer until Phase 4 (drawer).

---

## Phase 4 — Book layout in a drawer; PDF remains main focus — **done**

**User outcome:** The **outline / ranges** from **`GET .../book`** appear in a **drawer sidebar**; the **PDF viewer stays in the main** region. **E2E:** layout tree visible alongside rendered PDF (drawer open by default or clearly openable—pick one consistent default and assert it).

**Shipped:** `BookReadingPage` uses the same responsive sidebar pattern as `NoteShowPage` (768px): outline in a **left** panel with **internal scroll**, PDF in **`main`** with **overflow-y-auto**; **large** viewports open the outline by default, **small** hidden with **backdrop** and tap-outside to close. **GlobalBar:** compact **Notebook** link, book title, and **Outline** control (`aria-label`) so small viewports can open the panel. **E2E** unchanged (1200px viewport → outline open).

**Completion hint:** Reuse existing book JSON shape; DaisyUI/drawer patterns consistent with the rest of the app.

---

## Phase 5 — Click a layout range → PDF jumps to the right place — **done**

**User outcome:** **Selecting a node** in the layout navigates **pdf.js** to the range’s **start (or agreed) anchor** (page/region per **`pdf.mineru_outline_v1`**). **E2E:** click known range → observable scroll/page change or focus.

**Shipped:** `mineruOutlineV1PageIndex` (`parseMineruOutlineV1StartAnchor`, bbox → scroll destination), `PdfBookViewer` `scrollToMineruOutlineV1Target`, `BookReadingPage` outline row activation and chrome/safe-area handling; Cypress scenarios *Outline row jumps the PDF to the anchored page* and *Outline rows on the same page scroll the PDF to different places* in [`e2e_test/features/book_reading/book_reading.feature`](../e2e_test/features/book_reading/book_reading.feature).

**Completion hint:** **Sub-phases:** [`ongoing/book-reading-phase-5-outline-to-pdf-subphases.md`](book-reading-phase-5-outline-to-pdf-subphases.md). **Phase 5.6 (async navigation feedback)** remains **deferred** — **revisit 5.6** only if we observe noticeable delay or blank states on slow devices or very large PDFs (or if Phase 6 viewport sync makes latency more visible). **Phases 5.7 and 5.8** (keyboard activation; nested outline expand vs navigate) are **deferred** to **Phase 9** and **Phase 10** after Phase 8—see below.

---

## Phase 6 — Scroll / navigate the PDF → active range highlight in the layout

**User outcome:** As the user **scrolls or changes page** in the PDF, the **layout** shows which **BookRange** is **current** (highlight / active row). **E2E:** scroll to a region tied to a different range → highlight moves (or equivalent DOM assertion).

**Completion hint:** Two-way sync completes here; keep “current range” logic cohesive (single place to compute active range from viewport).

---

## Phase 7 — Show / hide the layout drawer

**User outcome:** User can **toggle** the drawer for **more reading space** without losing session state (current range / scroll should remain sensible when reopening). **E2E:** toggle control works; PDF still usable.

**Completion hint:** Small UX phase; can be folded into Phase 4 if the team prefers fewer phases—only combine if Phase 4 E2E already covers toggle.

---

## Phase 8 — Delete book from frontend (record + stored file)

**User outcome:** User removes the book from the notebook in the **browser**; **book metadata** is removed and the **blob** is **deleted** from the active backend (local store or GCS). **E2E:** delete → **`GET …/book/file`** returns **404** (or book missing), and no orphan object in the configured store (assert via API or test hook as appropriate).

**Completion hint:** Align with existing notebook/book permissions patterns.

---

## Phase 9 — Outline keyboard activation matches click (deferred from Phase 5.7)

**User outcome:** Focus an outline control and activate with **Enter** / **Space** (platform-appropriate) → same PDF navigation and **selected** state as pointer click; touch must not regress.

**E2E:** One scenario path: keyboard activation reaches the same postcondition as outline click → page/scroll and selection contract as in Phase 5.

**Completion hint:** Single “activate row” code path preferred; no duplicate handlers.

---

## Phase 10 — Nested outline: expand/collapse vs go-to (deferred from Phase 5.8)

**User outcome:** If the outline gains **hierarchical** rows with expand/collapse (e.g. chevron), **go-to** must not fire when toggling expansion (chevron vs row body or equivalent pattern). **Skip** this phase while the outline stays a **flat** list with no expand control.

**E2E:** Expand vs navigate as separate actions with distinct assertions when the UI exists.

**Completion hint:** Remove dead handlers on non-interactive wrappers when implementing.

---

## After this story

- **Reading record** and **next range** stories build on **BookRange** + **current range** from Phases 5–6.
- **Phases 9–10** (keyboard outline activation; nested outline interaction) are optional polish after Phase 8 when the team schedules them.
- Roadmap **open questions** (opaque anchors, finer progress) may narrow once PDF anchor mapping is exercised end-to-end.

---

## Phase checklist (when executing)

For each phase: failing test → pass → refactor; run the **relevant** Cypress spec(s); update this plan’s checkboxes or remove done phases when the story is finished; refresh the architecture roadmap if decisions diverge from the table there.
