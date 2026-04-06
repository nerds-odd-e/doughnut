# Book reading in Doughnut — architecture roadmap

This document is **not** a delivery plan. It does not define phased user-visible work (for that, see `.cursor/rules/planning.mdc` and a future `ongoing/<short-name>.md` plan when one exists). It **is** the guideline for **architecture direction**: how we want concepts and boundaries to line up so implementation can stay coherent as features land.

**Companion:** market and product research stays in `ongoing/book-reading-research-report.md`. That report is now cross-walked to the vocabulary below.

**Living document:** When a book-reading plan is written and executed, **update this roadmap** so it stays the single place for “what we believe the shape of the system should be.” Do not duplicate long architecture prose inside the plan; link here instead. **Delivery plan for “Read a range of a book”:** [`ongoing/book-reading-read-a-range-plan.md`](book-reading-read-a-range-plan.md).

---

## Intent

Separate concerns so one object does not have to mean everything at once:

| Concern | Question it answers |
|--------|---------------------|
| **Where** | A single precise point in a book file |
| **Which region** | A navigable or hierarchical chunk (section, “current reading unit”) |
| **Evidence** | The exact span a note is about |
| **Knowledge** | The user’s PKM note |
| **Progress** | Where the user is in the book, at chunk granularity |

---

## Core model (directional)

The diagram encodes **relationships we want to preserve** across PDF, EPUB, and future formats. Concrete storage types and APIs will evolve; the splits below should not collapse without an explicit decision.

```mermaid
classDiagram
    class Notebook {
    }

    class Book {
      +format
      +sourceFileRef
    }

    class Note {
    }

    class BookRange {
      +structuralTitle : text
    }

    class BookAnchor {
      +anchorFormat
      +value
    }

    class SourceSpan {
      +kind
    }

    class User {
    }

    class ReadingRecord {
      +status
      +startedAt
      +lastReadAt
      +completedAt
    }

    Notebook "1" --> "0..*" Note : contains
    Notebook "1" --> "0..*" Book : contains

    Book "1" --> "0..*" BookRange : has
    BookRange "1" --> "1" BookAnchor : startAnchor
    BookRange "1" --> "1" BookAnchor : endAnchor

    BookRange "0..1" --> "0..*" BookRange : child ranges

    Note "0..*" --> "0..1" SourceSpan : cites
    SourceSpan "1" --> "1" BookAnchor : startAnchor
    SourceSpan "1" --> "1" BookAnchor : endAnchor
    SourceSpan "0..*" --> "1" BookRange : within

    User "1" --> "0..*" ReadingRecord : has
    ReadingRecord "0..*" --> "1" BookRange : for
```

### BookAnchor

The most precise locator: one place in the book. Examples over time: PDF coordinates, EPUB CFI, paragraph offsets, etc.

Keep the abstraction **open** early: `anchorFormat` + `value` (opaque until format-specific design is justified).

### BookRange

A region: `startAnchor` + `endAnchor`. Primary unit for **navigation**, **hierarchical decomposition**, and **progress**. Optional **`structuralTitle`** is the human-readable label for that node in the outline (e.g. `Chapter 3`, `2.4.1`). A breadcrumb-style path can be **derived** by walking parent ranges; we do not use a separate persisted “structural address” field.

### SourceSpan

Optional evidence on a **Note**: also start/end anchors, but purpose is **citation**, not navigation tree. May sit **within** a `BookRange` so a small quote still relates to the larger reading chunk.

### Note

Belongs to a `Notebook`. At most **one** `SourceSpan` for the first version—enough for anchored extraction without multi-evidence complexity until needed.

### ReadingRecord

Per `User`, refers to a `BookRange`. Progress attaches to **meaningful chunks**, not citation-sized spans.

---

## Architectural rules (default)

1. Every `BookRange` has exactly one `startAnchor` and one `endAnchor`.
2. Every `SourceSpan` has exactly one `startAnchor` and one `endAnchor`.
3. `ReadingRecord` points at a `BookRange`, not a `SourceSpan`.
4. `SourceSpan` is optional on `Note`.
5. Prefer `SourceSpan` to be smaller than or equal to the `BookRange` it sits within.

These are **defaults** for consistency; revisiting them is a roadmap-level change, not a silent refactor.

---

## Current directional choices

- **One span per note (initially):** Keeps PKM extraction simple; multi-span and cross-book evidence are explicit future extensions.
- **`structuralTitle` on `BookRange`:** Human-readable title for the range in the book’s structure tree; parent chain + title is enough to reconstruct display paths when needed.
- **No `StructuralBookRange` subtype yet:** Structural vs user-carved ranges may be distinguished later if the product requires it (e.g. import vs override).

---

## Story 1 (shipped)

**Book** metadata plus **BookRange** tree on a **Notebook**: **`POST /api/notebooks/{notebook}/attach-book`** (JSON outline only) and **`GET /api/notebooks/{notebook}/book`**, at most one book per notebook. As shipped, **`sourceFileRef` is not used** and there is **no server-side PDF storage**; the PDF stayed on the client. Outline anchors use **`pdf.mineru_outline_v1`** on the wire (backend `BookReadingWireConstants`).

---

## Story 2 — Read a range (direction)

**Goal:** After CLI (or future UI) attach, the **same book the user reads in the browser** is the **file stored server-side**, with a reading UI that ties **outline navigation** to **PDF position**.

| Decision | Direction |
|----------|-----------|
| **CLI + server** | **`/attach` in the CLI** uploads the PDF to the backend **via the same `attach-book` surface** as the rest of the product (extend the route to accept outline + file in one logical operation—e.g. multipart—or an equivalent single-user-visible “attach” that does not fork a second attach API). |
| **Blob storage** | **Production:** PDF bytes live in a **GCP bucket** (object key or URL recorded so `sourceFileRef` or equivalent can resolve the object). **Dev / automated tests:** a **local or test-local object store** (filesystem, emulator, or test-only bucket) so the **same E2E scenarios** run without requiring real GCP. |
| **Frontend PDF** | Render the book with **pdf.js** in the **main content** area of the book reading page. |
| **Chrome layout** | **Book layout** (outline / ranges tree) lives in a **drawer sidebar** on the book reading page; the **PDF viewer occupies the main pane**. |
| **Sync** | **Two-way:** (1) **Selecting / activating a range** in the layout drives **pdf.js** to the corresponding anchors (page / region). (2) **Scrolling (and relevant zoom / page changes) in the PDF** updates which range is **highlighted** as current in the layout. |

**Deletion:** Removing a book from the notebook (frontend flow) must **delete the persisted book record** and **remove the object** from the configured storage backend (GCS in prod, local/test store in dev).

**Plan:** Phased delivery is spelled out in [`ongoing/book-reading-read-a-range-plan.md`](book-reading-read-a-range-plan.md).

**Implemented so far (Story 2):** Phases **1–6** of that plan are shipped: multipart attach, **`GET …/book/file`**, **pdf.js** full scrollable viewer (`PdfBookViewer` using `PDFViewer` from `pdfjs-dist/web/pdf_viewer.mjs`), the **outline** from **`GET …/book`** in a **left** responsive drawer/panel (PDF in **`main`**; **768px** breakpoint: open by default on large, overlay + backdrop on small; **Outline** control in **GlobalBar**), **layout → PDF** navigation from **`pdf.mineru_outline_v1`** anchors (page index, optional bbox, chrome/safe-area offset, bad-anchor no-op), and **PDF → outline** sync (viewport drives **viewport-current** row highlight, debounced, with accessible live region for title changes). The book-reading E2E uses **OCR on rendered canvases** (Tesseract.js, committed language data under `e2e_test/tesseract/`) for page markers — **not** a product DOM text layer on top of the canvas.

**E2E fixture / MinerU mock migration (`refactoring.pdf`, committed JSON, real MinerU refresh):** Sub-phased delivery in [`ongoing/book-reading-e2e-refactoring-fixture-visible-ocr-plan.md`](book-reading-e2e-refactoring-fixture-visible-ocr-plan.md) — **Phase 1** = **sp-1.1** (PDF-only swap + fix OCR-driven failures) then **sp-1.2** (same mock content in JSON); **Phase 2** = **sp-2.1** (script, capture to a non-canonical JSON name) then **sp-2.2** (promote to `mineru_output_for_refactoring.json` + fix E2E). **Phase 3** (beginning-of-book OCR using substring **`Code Refactoring`**, same viewer **element screenshot** pipeline as Phase 4 with page **1** canvas ink wait) is **shipped**. **Phase 4** (outline-jump scenario: same screenshot OCR with page **2**; MinerU XYZ scroll target offset above bbox top) is **shipped**. **Phase 5** (scroll PDF → viewport-current: same viewport OCR on page **2**) is **shipped**. **Phase 6** (short viewport + outline aside: same viewport OCR on page **2**) is **shipped**. **Phase 7** covers same-page scroll visible-viewport OCR and any positioning fixes driven by failing tests.

---

## Open architecture questions

Revisit when implementation or product constraints clarify:

- Whether `BookAnchor.value` stays opaque text or becomes structured payload (e.g. JSON) per `anchorFormat`.
- Whether `ReadingRecord` needs finer-grained progress inside a range (percentage, character offset, etc.).
- Whether `BookRange` should distinguish imported outline ranges from user-created ranges.
- Whether `SourceSpan.kind` should classify text, image, figure, table, or mixed content for rendering and export.

---

## Anti-patterns (what this roadmap pushes against)

- **Single “range” type** for TOC node, reading cursor, highlight, and AI chunk—leads to muddy APIs and broken exports.
- **Progress on arbitrary citations**—makes re-entry and queue semantics harder than progress on `BookRange`.
- **Anchors that only mean “page number”**—insufficient for structure-first reading and EPUB; `BookAnchor` is the extension point.
