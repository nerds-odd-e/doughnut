# Plan: Story 1 — Add a PDF book to a notebook and browse its structure

**Scope:** Only [Story: Add a PDF book to a notebook and browse its structure](book-reading-user-stories.md) (PDF, CLI attach, browser structure). Out of scope: reading ranges, reading record, navigation modes, EPUB, AI summaries, `ReadingRecord`, `SourceSpan` on notes.

**Architecture direction:** Align persisted shapes with [doughnut-book-reading-architecture-roadmap.md](doughnut-book-reading-architecture-roadmap.md) (`Book`, `BookRange`, `BookAnchor`). Do not duplicate that document here; update the roadmap when concrete storage/API choices land.

**Parsing boundary:** Reuse the same operational approach as the `/read` spike ([spike-mineru-read-layout.md](spike-mineru-read-layout.md), `cli/src/commands/read/`): **CLI runs MinerU (or equivalent) in a subprocess**, then sends **extracted outline + file** to the server. The server stores artifacts and the range tree; it does **not** need to run Python/MinerU in v1.

**Test discipline:** Follow [.cursor/rules/planning.mdc](../.cursor/rules/planning.mdc): one main user-visible outcome per phase, observable assertions (CLI stdout/transcript, HTTP, Cypress DOM), avoid layer-only phases without a visible slice.

**Deploy gate:** After each phase, commit/push and let CD deploy before starting the next, unless the team agrees otherwise (same as planning checklist).

---

## Phase 1 — `/use <notebook>`: active notebook in the CLI

**User outcome:** In interactive CLI, the user can run `/use Top Maths` (or similar) and get a clear assistant confirmation that **this notebook is now the target** for subsequent book commands. Errors when the notebook does not exist or the user lacks access are user-visible and consistent with existing CLI error patterns.

**Shipped CLI details (interactive):** `/use` accepts an **optional** title; **without** a title it opens a **notebook picker** (↑/↓, Enter, Esc). The picker supports **type-to-filter** on titles (**case-insensitive substring**). With a title, resolution is **exact case-sensitive** match against `myNotebooks`. Sub-phase checklist: [book-reading-phase-1-subphases.md](book-reading-phase-1-subphases.md).

**Implementation notes (non-prescriptive):** Persist the selection for the CLI session and/or the same persistence mechanism other CLI context uses (e.g. file-backed state), so `/attach` does not require repeating the notebook name every time. Resolve notebook by **title or stable id**—pick one rule and document it in command help.

**Tests:** Extend CLI coverage through **`runInteractive`** (or equivalent entry point): happy path + not-found / unauthorized. No need for Cypress in this phase if the web app is untouched.

**Phase complete when:** `/use` works end-to-end in the CLI with tests green; no dead code.

---

## Phase 2 — Domain + API: book on notebook, outline as `Book` / `BookRange` / `BookAnchor`

**User outcome:** A **logged-in integrator** (tests use the same auth path as the rest of the app) can create a **PDF book** on a **notebook** and fetch a **JSON representation of the outline** suitable for the browser. This phase is **API-visible**; the browser may not be updated yet.

**Data shape (directional):**

- One `Book` per attached PDF on a notebook: `format` = PDF, `sourceFileRef` (or equivalent) pointing at stored bytes or object key.
- Outline nodes map to **`BookRange`** with parent/child links, optional `structuralAddress` text, and **`BookAnchor`** start/end per range (`anchorFormat` + opaque `value` for PDF until a narrower contract is justified—per roadmap).

**API sketch (adjust to project conventions):** e.g. multipart **upload PDF** + **outline payload** (tree or ordered list with parent ids) **or** a two-step upload + finalize; plus **GET** outline for `(notebookId, bookId)` or “books for notebook”. Authorization: same user/notebook ownership rules as existing notebook APIs.

**Tests:** Prefer **controller-level** tests with real DB (`@Transactional`) and **`makeMe`** factories. Assert HTTP response bodies and persistence (e.g. reload from repository), not internal service private methods.

**Phase complete when:** Migrations applied; create + read outline works under auth; OpenAPI updated if controllers are part of the public API surface consumed by the frontend.

---

## Phase 3 — `/attach <pdf>`: parse locally, upload to Doughnut

**User outcome:** After `/use` has set the active notebook, `/attach path/to/book.pdf` runs the **existing local PDF outline pipeline** (same family as `/read` / `runMineruOutlineSubprocess`), then **uploads** the file + structured outline to the backend so the book is **stored on that notebook**. Assistant message confirms success (and surfaces actionable failures: missing file, MinerU/Python errors, network/auth errors).

**Implementation notes:** Factor shared “outline extraction” so `/read` and `/attach` do not fork incompatible logic. Respect env knobs already used for PDF caps (e.g. `DOUGHNUT_READ_PDF_END_PAGE`) or introduce attach-specific documented limits if product needs them.

**Tests:** CLI tests through **`runInteractive`** with **HTTP mocked** via the established `doughnut-api` spy pattern (see `.cursor/rules/cli.mdc`) **or** a thin test server **only** if the scenario is transport-level—prefer the project’s default mock approach for happy path + one failure class.

**Phase complete when:** Attach works against a running backend in dev; tests cover observable CLI + client/API contract; no unused upload code paths.

---

## Phase 4 — Browser: see the book structure for the notebook

**User outcome:** The user opens the **relevant notebook experience in the web app** and **sees the attached PDF’s structure** (hierarchical outline: expand/collapse or equivalent—match Doughnut’s existing navigation patterns). This satisfies the story’s **Then** clause: *see the book structure of the notebook “Top Maths” in the browser*.

**UX placement:** Prefer the smallest change that fits existing IA (notebook detail sidebar, a “Books” tab, or a dedicated sub-route—decide during implementation; document the chosen entry point briefly here when known).

**Tests:** **Cypress + Cucumber** scenario matching the story (Given notebook exists, When CLI attach via steps or preconditions, Then structure visible in browser). Run the **specific feature/spec** for this phase, not the full suite. Extend page objects/step definitions in the usual thin style.

**Follow-up:** Run **`pnpm generateTypeScript`** after OpenAPI changes.

**Phase complete when:** E2E passes for the story-shaped scenario; frontend lists show correct data for multiple levels when the outline has children.

---

## Phase discipline checklist (story closure)

1. **Clean up** — Remove interim flags or duplicate outline code paths once attach + UI are stable.
2. **Deploy gate** — Ship each phase via normal CD before the next.
3. **Update this plan** — Strike or shorten phases that are done; note any scope change.
4. **Update** [doughnut-book-reading-architecture-roadmap.md](doughnut-book-reading-architecture-roadmap.md) if implemented types diverged from the directional diagram (e.g. anchor payload format, table names).

---

## Explicit non-goals (later stories)

- Reading inside the app (range reader), splitting ranges, progress, notes with `SourceSpan`, citations, EPUB.

## Risks / product notes (from research)

- PDFs without reliable headings produce weak outlines; user-visible copy should say when structure is partial or empty ([book-reading-research-report.md](book-reading-research-report.md)).
- MinerU/heavy PDFs: timeouts and machine prerequisites remain a **CLI environment** concern; keep failure messages honest.
