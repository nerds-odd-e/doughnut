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
- **Storage rollout:** **Phase 1** uses **dev/test-local** object storage for attach/download E2E; **Phase 2** wires **GCP** (bucket) for production with the **same** attach/download contract (reuse the same E2E scenarios where feasible—emulator, fake GCS, or test bucket in CI). Backend abstraction: **`BookStorage`** with **`DbBookStorage`** (non-`prod`) and **`GcsBookStorage`** (`prod`); PDF is the first file format—execution detail in [book-reading-pdf-storage-subphases.md](book-reading-pdf-storage-subphases.md).
- **At most one intentionally failing test** while driving a given phase (see planning rule).

---

## Phase 1 — CLI attach uploads the PDF; frontend can obtain the file (dev / test storage)

**User outcome:** After **`/attach`** from the CLI, the **PDF bytes** are stored server-side (local or test bucket), linked on the **Book** (`sourceFileRef` or agreed field), and the **browser** can **download or open** the file through a **documented API** (authenticated fetch, redirect, or signed URL — choose one approach in implementation). **E2E:** scenario aligned with *“attach in cli and download book from frontend”* for **dev/test** (no real GCP required).

**Completion hint:** Extend attach contract (e.g. multipart) and persistence; add read/download path; wire frontend or minimal test-only UI to prove download; one focused E2E.

---

## Phase 2 — Production object storage on GCP

**User outcome:** In **production configuration**, uploaded PDFs are stored in a **GCP bucket**; **download/view** still works from the frontend **with the same user-visible flow** as Phase 1. **E2E:** **same spec** as Phase 1 where feasible (e.g. CI uses emulator, fake GCS, or a dedicated test bucket—document the chosen approach).

**Completion hint:** Introduce storage abstraction in Phase 1 if not already present; Phase 2 swaps or extends the backend to GCS—no second attach API.

---

## Phase 3 — Book reading page shows the PDF with pdf.js (main content)

**User outcome:** On the book reading route, the user **sees the attached PDF rendered** in the **main content area** using **pdf.js**. **E2E:** open notebook with attached book → PDF is visible (e.g. first page or canvas present).

**Completion hint:** Depends on file access from Phases 1–2; keep viewer concerns in the main pane only if that reduces scope—drawer can follow in Phase 4.

---

## Phase 4 — Book layout in a drawer; PDF remains main focus

**User outcome:** The **outline / ranges** from **`GET .../book`** appear in a **drawer sidebar**; the **PDF viewer stays in the main** region. **E2E:** layout tree visible alongside rendered PDF (drawer open by default or clearly openable—pick one consistent default and assert it).

**Completion hint:** Reuse existing book JSON shape; DaisyUI/drawer patterns consistent with the rest of the app.

---

## Phase 5 — Click a layout range → PDF jumps to the right place

**User outcome:** **Selecting a node** in the layout navigates **pdf.js** to the range’s **start (or agreed) anchor** (page/region per **`pdf.mineru_outline_v1`**). **E2E:** click known range → observable scroll/page change or focus.

**Completion hint:** One-way sync from **layout → PDF**; define minimal mapping rules for v1 anchors before polishing edge cases.

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

**User outcome:** User removes the book from the notebook in the **browser**; **book metadata** is removed and the **blob** is **deleted** from the active backend (local store or GCS). **E2E:** delete → subsequent download returns **404** (or book missing), and no orphan object in the configured store (assert via API or test hook as appropriate).

**Completion hint:** Align with existing notebook/book permissions patterns.

---

## After this story

- **Reading record** and **next range** stories build on **BookRange** + **current range** from Phases 5–6.
- Roadmap **open questions** (opaque anchors, finer progress) may narrow once PDF anchor mapping is exercised end-to-end.

---

## Phase checklist (when executing)

For each phase: failing test → pass → refactor; run the **relevant** Cypress spec(s); update this plan’s checkboxes or remove done phases when the story is finished; refresh the architecture roadmap if decisions diverge from the table there.
