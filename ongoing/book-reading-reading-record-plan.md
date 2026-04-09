# Plan: Reading record (Story: Reading record)

**User story:** [`ongoing/book-reading-user-stories.md`](book-reading-user-stories.md) — *Story: Reading record* (scenarios below map **one scenario → one phase**).

**Architecture (ReadingRecord, BookBlock, direct content vocabulary):** [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md).

**UX context (drawer, book layout, current block, Reading Control Panel):** [`ongoing/book-reading-ux-ui-roadmap.md`](book-reading-ux-ui-roadmap.md) and shipped Story 2 work in [`ongoing/book-reading-read-a-block-plan.md`](book-reading-read-a-block-plan.md).

**Planning rules:** `.cursor/rules/planning.mdc` — one **user-visible** behavior per phase, scenario-first ordering, test-first workflow when adding behavior, at most one intentionally failing test while driving a phase.

**Testing for this story:** **Phase 2** is covered by **E2E** ([`reading_record.feature`](../e2e_test/features/book_reading/reading_record.feature)) plus controller and mounted tests. **Phases 1, 3, and 4** do **not** add E2E in this plan; they rely on **unit-style tests** per `.cursor/rules/planning.mdc` — **observable** surfaces (HTTP from **controllers**, **mounted** Vue via Vitest), **black-box** I/O, **few** focused tests, **direct** tests only for small deliberate contracts (pure predicates, mapping, validation messages).

**This document is a delivery plan only** — not executed here.

**Status:** **Phase 2** (*Mark a book block as read*) is **shipped** (E2E: [`e2e_test/features/book_reading/reading_record.feature`](../e2e_test/features/book_reading/reading_record.feature)). **Phase 3** and **Phase 4** below are **not** started.

---

## Principles for this work

- **Progress on chunks:** [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md) — `ReadingRecord` refers to a **`BookBlock`**, not a `SourceSpan`. Per-block **read / skim / skip** states belong on that model (or equivalent rows), not on arbitrary PDF coordinates.
- **Fine-grained “where on the page”** (exact scroll restore) is an **open architecture question** in the same roadmap; Phase 1 may persist a **viewport-aligned** snapshot (see below) without pretending it is a substitute for long-term `ReadingRecord` semantics.
- **Direct content** between two **book blocks** is **conceptual** today (no required DB column for the gap). Phases 3–4 rely on a **documented heuristic** (e.g. anchor proximity in **MinerU-normalized** space, same-page `y0` ordering, or “next block start immediately follows previous start”) — pick one rule per implementation and test it; revisiting the heuristic is allowed if product feedback demands it.
- **Consecutive blocks in reading order** (for Phase 2’s “successor” check, Phase 3’s A→B gap, and **current block**) are **not** required to sit at the same tree depth. **B** is always the **immediate successor** of **A** in the linear walk. Typical shapes include: **siblings** (A then next sibling), **parent then first child** (A then its first `children[]` entry), and **last descendant then next block after leaving a subtree** (e.g. A is the last block in a subtree; B is the next sibling of an ancestor — “uncle” relative to A). Same rule as **direct content** boundaries in [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md).
- **Book layout reading order** for the reading-control **successor boundary**, Phase 3 auto-marking, and **current block** must match the **same linear order** (depth-first preorder over the `BookBlock` tree unless product explicitly chooses another walk).
- **Observable tests (this plan):** **Phase 2** — Cypress for the full reading-record UX. **Phases 1, 3, 4** — Spring **controller** tests (status, body, persistence side effects via follow-up reads) and/or **mounted** frontend tests where the behavior is UI-shaped; **pure** black-box tests for predicates and formatting. Avoid tests that only pin private helpers when a controller or mounted component already proves the path.

---

## Phase 1 — Remember book last read position

**User story scenario:** *Remember book last read position* — scroll to a position, leave, return, land on the **same** place.

**User outcome:** When the user opens the book reading page again (same user, same notebook book), the PDF **restores** to the **last reading position** they had in a prior session (or prior navigation away), within normal browser refresh constraints.

**Suggested shape (implementation detail, not fixed in this plan):**

- Persist a **viewport snapshot** keyed by **user + book** (notebook has at most one book): e.g. **page index** + **within-page vertical position** in the **same normalized space** already used for the **current block** (`0–1000` MinerU-style), or an agreed scalar stored server-side; avoid a second ad-hoc coordinate system if possible.
- **Save triggers:** debounced updates while scrolling (reuse debounce discipline similar to **current block** updates) and/or **save on visibility unload** — choose the smallest set that **passes tests** and feels reliable on mobile.
- **API:** e.g. read with book payload or dedicated `GET`/`PATCH` under the notebook book resource; follow existing controller and OpenAPI patterns, then `pnpm generateTypeScript`.

**Tests (no E2E for this phase):** Prove behavior through **observable** layers per `.cursor/rules/planning.mdc`:

- **Backend:** **`@WebMvcTest` / controller tests** (or full MVC slice if that is the project habit) — e.g. `PATCH`/`GET` returns expected JSON and **persists** position; **error** bodies for wrong notebook, unauthorized user, missing book.
- **Frontend:** **Mounted** `BookReadingPage` / composable tests (Vitest) — debounced **save** sends the API payload that matches the **viewport descriptor** already produced for **current block** logic (mock `fetch`/generated SDK); **restore on load** applies stored page/Y into the viewer contract **without** importing pdf.js internals.
- **Pure helpers** (if any): minimal **inputs → outputs** tests only where that API is the intentional contract.

**Out of scope for this phase:** Per-block read/skim/skip, prompts, read badges on **book blocks** in the book layout.

---

## Phase 2 — Mark a book block as read — **shipped**

**User story scenario:** [`ongoing/book-reading-user-stories.md`](book-reading-user-stories.md) — *mark a book block as read* (same steps as [`e2e_test/features/book_reading/reading_record.feature`](../e2e_test/features/book_reading/reading_record.feature)): user **selects** block **2.1**, scrolls until **2.2** is **current block**, marks **2.1** read from the panel; **2.1** shows read styling and **2.2** stays **selected**.

**UX (Reading Control Panel):** **Bottom-anchored** in the **PDF main pane** ([`ongoing/book-reading-ux-ui-roadmap.md`](book-reading-ux-ui-roadmap.md)). The panel appears when **current block** (viewport) is the **immediate successor** of the **selected block** in reading order — equivalent to treating the **selected** block’s direct content as “in question” while the reader sits on the next heading. **Mark as read** persists disposition for the **selected** `BookBlock` (not the current-block row). Expanded/minimized bar; panel does **not** own document scroll.

**User outcome (as implemented):**

- Server: table **`book_block_reading_record`**, entity **`BookBlockReadingRecord`**, **`READ`** status, **`completed_at`** on mark; uniqueness **(user_id, book_block_id)**.
- **`GET …/book`** remains **layout-only**. Client merges **`GET …/book/reading-records`** (`bookBlockId`, `status`, `completedAt`) with layout for read borders (`frontend/src/lib/book-reading/readBlockIdsFromRecords.ts`, `useNotebookBookReadingRecords`, `BookReadingPage.vue`).
- **`PUT …/book/blocks/{bookBlock}/reading-record`** → **200** + JSON array of **`BookBlockReadingRecordListItem`** (full list after write; same item shape as **`GET …/book/reading-records`** — avoids an extra list fetch after mark).

**E2E:** `reading_record.feature` (CLI attach `refactoring.pdf`, OCR-backed scroll steps).

**Other tests:** `NotebookBooksControllerTest` (list + put + auth/idempotency paths); mounted **`BookReadingPage.spec.ts`**; **`readBlockIdsFromRecords.spec.ts`**.

---

## Phase 3 — Mark a block with no direct content as read automatically

**User story scenario:** *mark a book block with no direct content as read automatically* — no meaningful gap between title “xxx” and “ooo”; scrolling through **xxx** then **ooo** results in **xxx** shown as read **without** an explicit answer.

**User outcome:** When the system classifies **direct content between block A and the immediate successor B** in **book layout reading order** as **empty / not meaningful** (per the agreed heuristic in Principles), **entering B** (or passing the boundary—define one rule) **automatically** creates or updates **`ReadingRecord`** for **A** as read (or equivalent “no gap” disposition), and the **book layout** updates like Phase 2. **A** and **B** may be siblings, parent/first-child, last-child-in-subtree/next-after-subtree, or any other **consecutive preorder** pair — not only same-level headings.

**Depends on:** Phase 2 (persistence and **book layout** display of read state).

**Tests (no E2E for this phase):**

- **Pure predicate** (if extracted): **no-direct-content** between two anchors / blocks — table-style **inputs → outputs**; no callers from tests that bypass the real rule.
- **Service or controller slice:** when the “next block” transition runs, assert **`ReadingRecord`** (or API response) shows **A** marked read **without** a user answer — use **real DB** `@Transactional` tests if that is the project pattern for this feature, or controller tests that observe **HTTP + repository** outcomes, **not** private method names.

Keep **one behavior’s assertions together** (one focused test class or `describe`) where practical.

---

## Phase 4 — Mark a book block as skimmed or skipped

**User story scenario:** *mark a book block as skimmed/skipped* (Gherkin to be completed in `book-reading-user-stories.md` when this phase starts).

**User outcome:** The same flow family as Phase 2 supports **skimmed** and **skipped** from the **Reading Control Panel** (expanded actions), aligned with **Direct content disposition** names in the architecture doc. The **book layout** distinguishes states **at least** as much as product requires (could be icon, label, or shared “touched” vs “completed” — decide in implementation; roadmap allows enum-style status).

**Depends on:** Phase 2 (and reuses Phase 3 heuristic only if skim/skip also applies to “no prompt” cases—optional; do not expand scope unless a single cohesive UX falls out naturally).

**Tests (no E2E for this phase):**

- **Controller / API:** `POST`/`PATCH` (or whatever surface answers the prompt) accepts **skimmed** and **skipped**; responses and **403/404** paths; persisted status round-trips on **GET** book or records endpoint.
- **Frontend:** **Mounted** tests — choosing **skim** vs **skip** in the prompt UI calls the SDK with the right enum/body and updates **book layout** presentation props or DOM hooks you expose for testing (prefer **user-visible** strings/roles over implementation-only `data-testid` unless the project already standardizes them).
- **Validation / enum:** illegal status rejected with stable **error text** or code if that is part of the public contract.

**API / schema:** Extend `ReadingRecord.status` (or equivalent) with **skimmed** and **skipped**; migrate forward-only per `.cursor/rules/db-migration.mdc`.

---

## Phase discipline (checklist)

After each phase:

1. **Clean up** dead code and temporary flags.
2. **Deploy gate** — commit/push and let CD deploy before the next phase unless the team agrees otherwise.
3. **Update this plan** — mark the phase done, drop obsolete notes, link to merged PR or components if helpful.
4. **Architecture doc** — if a **default** in [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md) changes (e.g. how fine-grained position relates to `ReadingRecord`), update *Current directional choices* or *Open architecture questions* there in the same delivery stream.

---

## Document maintenance

When phases ship, trim duplication here; keep the architecture roadmap as the single place for long-lived conceptual rules. **Phase 2** Cypress path: [`e2e_test/features/book_reading/reading_record.feature`](../e2e_test/features/book_reading/reading_record.feature). Phases 1, 3, and 4 rely on unit/controller/mounted tests only (no extra E2E required by this plan except Phase 2, done).
