# Plan: Story 1 — Add a PDF book to a notebook and browse its structure

**Scope:** Only [Story: Add a PDF book to a notebook and browse its structure](book-reading-user-stories.md) (PDF, CLI attach, browser structure). Out of scope: reading ranges, reading record, navigation modes, EPUB, AI summaries, `ReadingRecord`, `SourceSpan` on notes.

**Architecture direction:** Align persisted shapes with [doughnut-book-reading-architecture-roadmap.md](doughnut-book-reading-architecture-roadmap.md) (`Book`, `BookRange`, `BookAnchor`). Do not duplicate that document here; update the roadmap when concrete storage/API choices land.

**Parsing boundary:** **CLI runs MinerU (or equivalent) in a subprocess** via [`cli/python/mineru_book_outline.py`](../cli/python/mineru_book_outline.py) and **`runMineruOutlineSubprocess`**, then sends **extracted outline** to the server. Historical spike notes: [spike-mineru-read-layout.md](spike-mineru-read-layout.md). **PDF bytes are not sent on `attach-book`** (separate upload/bind flow—see Phase 3 notes). The server stores the range tree from the nested layout payload; it does **not** need to run Python/MinerU in v1.

**Test discipline:** Follow [.cursor/rules/planning.mdc](../.cursor/rules/planning.mdc): one main user-visible outcome per phase, observable assertions (CLI stdout/transcript, HTTP, Cypress DOM), avoid layer-only phases without a visible slice.

**Deploy gate:** After each phase, commit/push and let CD deploy before starting the next, unless the team agrees otherwise (same as planning checklist).

---

## Phase 1 — `/use <notebook>`: active notebook in the CLI — **done**

**User outcome:** In interactive CLI, the user can run `/use Top Maths` (or similar) and get a clear assistant confirmation that **this notebook is now the target** for subsequent book commands run **from the notebook stage**. Errors when the notebook does not exist or the user lacks access are user-visible and consistent with existing CLI error patterns.

**Shipped CLI details (interactive):** `/use` accepts an **optional** title; **without** a title it opens a **notebook picker** (↑/↓, Enter, Esc). The picker supports **type-to-filter** on titles (**case-insensitive substring**). With a title, resolution is **exact case-sensitive** match against `myNotebooks`. The resolved notebook is **only in memory for the notebook stage**—there is **no** file-backed or cross-session persistence; leaving the stage (`/exit`) clears that context. **`/attach`** (Phase 3) is a **notebook-stage sub-command**, not a top-level slash command.

**Tests:** CLI coverage via **`runInteractive`** / Vitest: happy path, picker, filter, not-found / unauthorized, and related paths. No Cypress for this phase (web app untouched).

**Phase complete:** Delivered; `pnpm cli:test` green.

---

## Phase 2 — Domain + API: book on notebook, outline as `Book` / `BookRange` / `BookAnchor`

**User outcome:** A **logged-in integrator** (tests use the same auth path as the rest of the app) can create a **`format: pdf` book record** on a **notebook** via **`attach-book`** (name + **nested** layout only) and fetch a **JSON representation of the outline** suitable for the browser. **PDF file bytes are not part of Phase 2’s endpoint.** This phase is **API-visible**; the browser may not be updated yet.

**Phase 2 completion gate (decided):** Ship **`POST /api/notebooks/{notebook}/attach-book`** as the primary create path for **book metadata + outline**. It accepts **`application/json`**: **book display name** and a **nested** **layout** (outline). It **does not** accept or store the PDF file; blob upload and `sourceFileRef` binding are **out of scope for this endpoint** (handled elsewhere when the full “PDF on notebook” story is closed—see Phase 3). Naming follows existing notebook controllers (`/api/notebooks/...`); the path segment is **`attach-book`** (kebab-case), not a top-level `/attach-book` root.

### `attach-book` HTTP contract

| Decision | Choice |
|----------|--------|
| Method / route | `POST /api/notebooks/{notebook}/attach-book` |
| AuthZ | Same as other notebook mutations: caller must be allowed to modify the target notebook (reuse `Notebook` path variable + `authorizationService.assertAuthorization(notebook)` pattern). |
| Content type | **`application/json`** — request body is the DTO below (**no** `multipart`, **no** file part). |
| Body | Single JSON object: `bookName`, `format`, `layout` (nested tree—see below). |
| Response | **`201 Created`** with the persisted **`Book`** JSON (same shape as **`GET .../book`**): metadata plus **`ranges`** (flat list with `parentRangeId`, `siblingOrder`, `title`, anchors). At most **one book per notebook**; a second **`attach-book`** for the same notebook should fail (**`409 Conflict`** or equivalent). Errors: **`400`** validation (invalid layout, wrong content type), **`403`** / **`404`** consistent with notebook APIs. |

**Rationale:** Outline-only attach keeps a clear boundary: **structure** is declared in one JSON payload; **bytes** follow a separate API or flow so this handler stays simple and testable.

### Request JSON (wire DTO)

This is the **interchange structure** the CLI (Phase 3) builds from MinerU / outline scripts **without** the server re-running Python. There are **no client-supplied `BookRange` ids**—the nested tree is the **canonical** on-the-wire shape; the server **validates** and **walks** the tree in **array order** (sibling order = `children[]` order) and **assigns** surrogate keys and parent links on insert. Do **not** require a flatten-and-“normalize” step keyed by temporary ids; persistence derives parent/child **only** from nesting.

| Field | Type | Required | Notes |
|-------|------|----------|--------|
| `bookName` | string | yes | Display title for the `Book` (trimmed; max length TBD in implementation, e.g. 512). |
| `format` | string | yes | For this story: **`"pdf"`** only. Leaves room for later `"epub"` without a new route. |
| `layout` | object | yes | Root container for the outline **tree** (see below). |

**`layout` object**

| Field | Type | Required | Notes |
|-------|------|----------|--------|
| `roots` | array | yes | Top-level sections in **display order**. Non-empty after validation (unless product explicitly allows an empty outline—decide in implementation). |

**`layout.roots[]` — recursive `LayoutNode` (same shape for every depth)**

| Field | Type | Required | Notes |
|-------|------|----------|--------|
| `title` | string | yes | Maps to `BookRange.structuralTitle` / tree label (non-empty after trim). |
| `startAnchor` | anchor object | yes | See below. |
| `children` | array | no | Same as `roots` element shape; **omit or use `[]`** for leaves. Sibling order = array order. |

**Anchor object (mirrors roadmap `BookAnchor` on the wire)**

| Field | Type | Required | Notes |
|-------|------|----------|--------|
| `anchorFormat` | string | yes | For PDF v1 from MinerU-style output, use a single format literal agreed in implementation, e.g. **`pdf.mineru_outline_v1`**. |
| `value` | string | yes | **Opaque to generic clients**; JSON-serialized blob recommended. Example content: page index and optional bbox from `content_list` / middle.json (align with Phase 3’s subprocess). Server stores as `BookAnchor.value`. |

**Server mapping (directional, not implementation detail):**

- One `Book` per successful `attach-book`: `format` and `bookName` from payload; **`sourceFileRef` unset or null** until a later upload/bind step.
- Recursive descent of `layout.roots` / `children` → one **`BookRange`** per node, parent/child from **nesting**, `structuralTitle` from `title`, `startAnchor` → **`BookAnchor`** row.
- Reject: invalid anchors, or structural violations (e.g. excessively deep tree—limit TBD if needed).

### Read path for Phase 2

**`GET /api/notebooks/{notebook}/book`** returns the **only** **`Book`** for that notebook (metadata + flat **`ranges`** with server-assigned ids, `title`, anchors, `parentRangeId`, `siblingOrder`). **No `{book}` path segment**—one notebook has at most one book. **`404`** when the notebook has no book yet. There is **no** list-books endpoint. **Create via `attach-book`**, **read via `GET .../book`**. OpenAPI documents the `Book` schema.

**Data shape (directional, persistence):**

- One `Book` per logical attach on a notebook: `format` from payload; **`sourceFileRef`** populated only after the **separate** file pipeline lands.
- Outline nodes map to **`BookRange`** with parent/child links from the stored tree, `structuralTitle` text, and one **`BookAnchor`** per range (`anchorFormat` + `value` per roadmap).

**Tests:** Prefer **controller-level** tests with real DB (`@Transactional`) and **`makeMe`** factories. Assert HTTP response bodies and persistence (e.g. reload from repository), not internal service private methods. Use **`application/json`** request bodies (nested `layout.roots` / `children`).

**Phase complete when:** Migrations applied (including **one book per notebook** in storage); **`POST .../attach-book`** persists book + outline under auth (**no** file on this route); **`GET .../book`** returns **`Book`** JSON with range ids; OpenAPI updated for any surface consumed by the frontend / `doughnut-api` generation.

---

## Phase 3 — `/attach <pdf>`: parse locally, upload to Doughnut — **done**

**User outcome:** From the **notebook stage** (after `/use` has chosen the notebook), `/attach path/to/book.pdf` runs the **local PDF outline pipeline** (`runMineruOutlineSubprocess` + [`cli/python/mineru_book_outline.py`](../cli/python/mineru_book_outline.py)), builds a **nested** layout matching Phase 2’s `layout.roots` / `children` shape, then calls **`POST /api/notebooks/{notebook}/attach-book`** with **`application/json`** (book name + layout). **PDF bytes are not sent on that endpoint**—Phase 3 also introduces or uses a **separate** upload/bind API (or storage flow) so the file is stored and linked to the created `Book`; exact route and ordering (create book first vs upload first) **TBD at implementation** and should be documented here once chosen. Assistant message confirms success (and surfaces actionable failures: missing file, invalid outline JSON from a running subprocess, network/auth errors, and **Python/MinerU environment** failures such as missing interpreter, missing MinerU, or MinerU runtime errors—see [book-reading-phase-3-detailed-plan.md](book-reading-phase-3-detailed-plan.md)).

**Implementation notes:** Outline extraction lives in **`cli/src/commands/mineruOutline/`**; the Python script is embedded in the CLI bundle and also resolved from checkout at **`cli/python/mineru_book_outline.py`**. PDF page cap: **`DOUGHNUT_MINERU_PDF_END_PAGE`**. The interactive **`/read`** spike command was removed in Phase 3.8.

**Tests:** CLI tests through **`runInteractive`** with **HTTP mocked** via the established `doughnut-api` spy pattern (see `.cursor/rules/cli.mdc`) **or** a thin test server **only** if the scenario is transport-level—prefer the project’s default mock approach for happy path + one failure class.

**Phase complete:** Attach path, E2E gate with **`@mockMineruLib`**, bundle embed, exceptional cases, **`pnpm cli:test`** green; no **`/read`** entry point.

---

## Phase 4 — Browser: see the book structure for the notebook

**User outcome:** The user opens the **relevant notebook experience in the web app** and **sees the attached PDF’s structure** (hierarchical outline: expand/collapse or equivalent—match Doughnut’s existing navigation patterns). This satisfies the story’s **Then** clause: *see the book structure of the notebook “Top Maths” in the browser*.

**UX placement:** Prefer the smallest change that fits existing IA (notebook detail sidebar, a “Books” tab, or a dedicated sub-route—decide during implementation; document the chosen entry point briefly here when known).

**Tests:** **Cypress + Cucumber** scenario matching the story (Given notebook exists, When CLI attach via steps or preconditions, Then structure visible in browser). Run the **specific feature/spec** for this phase, not the full suite. Extend page objects/step definitions in the usual thin style.

**Follow-up:** Run **`pnpm generateTypeScript`** after OpenAPI changes.

**Phase complete when:** E2E passes for the story-shaped scenario; the browser shows correct hierarchical structure for multiple levels when the outline has children (using **`GET .../book`** or the **`attach-book`** response—no list-books API).

---

## Phase discipline checklist (story closure)

1. **Clean up** — Remove interim flags or duplicate outline code paths once attach + UI are stable.
2. **Deploy gate** — Ship each phase via normal CD before the next.
3. **Update this plan** — Strike or shorten phases that are done; note any scope change.
4. **Update** [doughnut-book-reading-architecture-roadmap.md](doughnut-book-reading-architecture-roadmap.md) if implemented types diverged from the directional diagram (e.g. anchor payload format, `structuralTitle`, table names).

---

## Explicit non-goals (later stories)

- Reading inside the app (range reader), splitting ranges, progress, notes with `SourceSpan`, citations, EPUB.

## Risks / product notes (from research)

- PDFs without reliable headings produce weak outlines; user-visible copy should say when structure is partial or empty ([book-reading-research-report.md](book-reading-research-report.md)).
- MinerU/heavy PDFs: timeouts and machine prerequisites remain a **CLI environment** concern; keep failure messages honest.
