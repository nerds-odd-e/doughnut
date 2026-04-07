# Reading record — Phase 1 sub-phases: last read position

**Parent:** [`ongoing/book-reading-reading-record-plan.md`](book-reading-reading-record-plan.md) — *Phase 1 — Remember book last read position*.

**Planning rules:** `.cursor/rules/planning.mdc` — one **user-visible** (or **product-observable**) slice per sub-phase, scenario-first ordering, tests in the same sub-phase, **no dead production code** (every line exercised by that phase’s tests or by the user path that phase completes), cleanup before closing.

**Architecture:** [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md) — fine-grained position vs `ReadingRecord` remains an open question; this phase persists a **viewport snapshot** only, not range-level reading semantics.

**UX / viewer context:** [`ongoing/book-reading-ux-ui-roadmap-phases-4-7.md`](book-reading-ux-ui-roadmap-phases-4-7.md); viewport-current and MinerU-normalized space are already implemented in the reader.

**Non-goals (whole Phase 1):** Per-range read/skim/skip, prompts, outline badges (`book-reading-reading-record-plan.md`).

**Cross-cutting requirements (stated by product):**

- **Backend:** Persist **current reading position** per **authenticated user** and **book** (notebook has at most one book — key naturally `user + book` or `user + notebook` consistently with existing `NotebookBooksController` / `BookService`).
- **Frontend:** **Debounce** writes while the user scrolls or changes page/zoom so position recording does not hammer the API (align debounce feel with viewport-current discipline; values may differ — document chosen delays in the sub-phase that implements save).

**Testing stance (inherits parent Phase 1):** No Cypress for these sub-phases. Use **Spring MVC / controller tests** for HTTP contracts and persistence side effects; **mounted Vue / Vitest** for debounce, restore, and lifecycle flush; **small pure tests** only where the function is an intentional public contract (e.g. validation mapping).

**API shape (decide in 1.1, do not defer without cause):** Prefer a dedicated resource under the existing notebook book surface (e.g. `GET` + `PATCH` under `/api/notebooks/{notebook}/book/...`) so `GET /book` stays outline-focused; alternatively embed in `GET …/book` only if it measurably reduces complexity — record the choice in 1.1 and keep OpenAPI + `pnpm generateTypeScript` in sync in the same sub-phase that exposes the operation.

**Persisted fields (minimum):** At least **0-based page index** and **within-page vertical position** in the **same normalized convention** as viewport-current (**0–1000** MinerU-style per page). Optionally **pdf.js scale** in a later sub-phase if restore is wrong without it; do not add columns until that sub-phase.

---

## Sub-phase ordering (finest practical split)

Each row is **shippable alone**: tests green, no unused endpoints/UI hooks, no temporary feature flags left behind. Order is **by user value** as much as dependencies allow: **persist → reliably save → restore → harden**.

| # | Short name | User / observable outcome |
|---|------------|---------------------------|
| 1.1 | **Persisted snapshot (write)** | The server **accepts and stores** a last-position payload for the current user’s book; unauthorized / missing-book cases behave correctly. |
| 1.2 | **Read snapshot back** | The server **returns** the stored snapshot (or a clear “none” contract) so a client can restore; same authz rules as read book. |
| 1.3 | **Debounced save while reading** | While reading, **scrolling (and any agreed zoom/page driver)** causes **debounced** PATCH updates — no save on every scroll tick. |
| 1.4 | **Restore on open** | Opening the book reading page **positions** the PDF to the last stored place when a snapshot exists. |
| 1.5 | **Flush pending save on leave** | If the user navigates away or hides the tab **before** the debounce fires, the **latest** position still **reaches the server** (flush on `visibilitychange` / `pagehide` or equivalent; avoid redundant double-PATCH when debounce already ran). |
| 1.6 | **Validation and bad data** | Invalid payloads (out-of-range page, `y` outside 0–1000, etc.) get **stable 4xx** responses; client does not persist garbage. |
| 1.7 | **Graceful client degradation** | If **GET** fails or no snapshot exists, the reader **starts in a defined safe state** (e.g. top of document); failed PATCH does not break scrolling (behavior defined + tested). |
| 1.8 | **Scale in snapshot (only if needed)** | If product testing shows **restore drift** without scale, add persisted **scale** (or agreed viewer state) **and** tests; if not needed, **skip** this sub-phase and note “not required” in the plan. |

---

## 1.1 — Persisted snapshot (write)

**Scenario slice:** *When the client sends a position, the server remembers it for that user and book.*

**Scope**

- Flyway migration: table keyed by **user + book** (or **user + notebook** if book is always 1:1 with notebook and FKs are simpler — pick one and document).
- JPA entity + repository + small service method on the **book / notebook** boundary (match `BookService` cohesion).
- **`PATCH`** (or `PUT`) only — request body = viewport snapshot DTO; **idempotent upsert**.
- OpenAPI annotation + **`pnpm generateTypeScript`** if the DTO is new on the wire (generated client may be unused until 1.3 — **acceptable** if 1.1’s **controller tests** are the sole caller until then; avoid adding frontend imports until 1.3).

**Tests**

- Controller tests: success persists row; **403/404** for wrong notebook or no book; unauthenticated if that’s the project pattern.
- Assert persistence via **repository**, `TestEntityManager`, or JDBC — **without** requiring the GET endpoint from 1.2.

**Cleanup**

- No dead controllers or DTOs; migration applied.

**Deploy gate:** Per parent checklist — commit/push/CD before 1.2 if that is team practice.

---

## 1.2 — Read snapshot back

**Scenario slice:** *The client can read back what was stored.*

**Scope**

- **`GET`** same resource: returns **200 + body** or **204 / 404** — choose one **documented** contract and stick to it in frontend 1.4.
- Reuse authz as **`GET …/book`**.

**Tests**

- Controller tests: after PATCH (via API or test setup), GET returns the same values; missing row matches empty contract; same error paths as 1.1.

**Cleanup**

- OpenAPI + regenerate TS; **still** no frontend wiring required — HTTP tests exercise GET.

---

## 1.3 — Debounced save while reading

**Scenario slice:** *As I move through the book, my position is saved without flooding the network.*

**Scope**

- Integrate with **`PdfBookViewer`** (or a **composable** colocated with book reading): derive **page index + normalized y** using the **same pipeline** as viewport-current (single source of truth for “where am I” numerically — avoid a second coordinate derivation).
- Call generated SDK **`PATCH`** with **debounce** (timer; consider **max wait** / trailing-only vs leading+trailing — pick one, document).
- **Do not** implement full restore here if that would leave “save” unused — restore comes in 1.4; saves must be **observable** in tests (mock SDK / `fetch`).

**Tests**

- Mounted Vitest: rapid scroll-like events produce **at most one** PATCH in the debounce window; payload matches expected shape; **fake timers** acceptable.

**Cleanup**

- No unused composable exports; debouncer cancelled on unmount if applicable.

**Choices (shipped):**

- **Debounce:** Trailing-only, **400ms** (`LAST_READ_POSITION_PATCH_DEBOUNCE_MS` on [`frontend/src/pages/BookReadingPage.vue`](../frontend/src/pages/BookReadingPage.vue)); separate from outline viewport-current debounce (120ms).
- **Payload:** `pageIndex` = `anchorPageIndexZeroBased` from `viewportAnchorPage`; `normalizedY` = `Math.round(viewport.mid)` (MinerU 0–1000). **No PATCH** when `viewport === null` (within-page position unknown).
- **Dedupe:** After the debounce delay, skip PATCH if the payload matches `lastSent` (updated only after a successful PATCH; failed PATCH leaves `lastSent` unchanged so a later event can retry). Timing uses **`debounce` from `es-toolkit`** (aligned with [`TextContentWrapper.vue`](../frontend/src/components/notes/core/TextContentWrapper.vue)).
- **Implementation:** [`frontend/src/lib/book-reading/debounceLastReadPositionPatch.ts`](../frontend/src/lib/book-reading/debounceLastReadPositionPatch.ts) + wiring in `BookReadingPage`; viewport-current debounce uses the same library in [`debounceViewportCurrentAnchorId.ts`](../frontend/src/lib/book-reading/debounceViewportCurrentAnchorId.ts); tests in [`frontend/tests/pages/BookReadingPage.spec.ts`](../frontend/tests/pages/BookReadingPage.spec.ts).

---

## 1.4 — Restore on open

**Scenario slice:** *Given I scroll to a certain position … when I read the book again … I start from the same position* (core Gherkin from [`ongoing/book-reading-user-stories.md`](book-reading-user-stories.md)).

**Scope**

- On book load (after PDF document ready — order matters): **`GET`** snapshot; if present, **scroll viewer** to page + y using the **public** viewer API already used for outline navigation (no direct pdf.js internals in tests).
- Coordinate **race**: restore after metadata load; avoid fighting user input — minimal first slice: restore **once** on initial load.

**Tests**

- Mounted Vitest: mock GET returning a snapshot → assert viewer receives **go-to** with expected page/y (observable props, emitted events, or DOM hooks already used elsewhere — follow `frontend.mdc`).

**Cleanup**

- Remove any interim `console` or one-off flags.

---

## 1.5 — Flush pending save on leave

**Scenario slice:** *I scroll and immediately close the tab; next visit still matches.*

**Scope**

- On **`visibilitychange` (hidden)** and/or **`pagehide`**, **flush** pending debounced value (immediate PATCH or cancel timer + send once).
- Dedupe with 1.3 so two identical PATCHes are not sent back-to-back without reason.

**Tests**

- Mounted Vitest: simulate hidden + pending debounce → assert **one** PATCH with latest payload.

**Cleanup**

- Remove duplicate listeners; ensure teardown on unmount.

---

## 1.6 — Validation and bad data

**Scenario slice:** *Broken clients cannot corrupt stored state.*

**Scope**

- Server-side validation on PATCH: page **≥ 0**, **y** in **[0, 1000]** (or agreed bounds), optional max page vs PDF page count **if** cheaply available server-side; otherwise document “client-trusted page index” and validate only numeric range.
- Stable error body or code consistent with existing API errors.

**Tests**

- Controller tests: malformed / out-of-range bodies → **400** (or project standard).

**Cleanup**

- No duplicate validation in UI unless it improves UX without diverging rules.

---

## 1.7 — Graceful client degradation

**Scenario slice:** *If persistence is unavailable, I can still read.*

**Scope**

- GET errors → fall back to default position; PATCH errors → **no** infinite retry loop; optional user-visible toast only if consistent with `apiCallWithLoading` patterns (minimal — avoid new UX surface unless needed).

**Tests**

- Mounted Vitest: failing GET / PATCH does not throw; reader remains usable.

**Cleanup**

- No dead error branches.

---

## 1.8 — Scale in snapshot (conditional)

**Scenario slice:** *After zooming, restore still lands on the same visual place.*

**Scope**

- Only if 1.4–1.5 fail acceptance in manual or automated checks: extend migration + DTO + client to persist **`currentScale`** (or equivalent), debounced with position, restored with scroll.

**Tests**

- Controller round-trip + mounted restore with non-default scale.

**If skipped:** Note in [`ongoing/book-reading-reading-record-plan.md`](book-reading-reading-record-plan.md) that scale was not required.

---

## Phase discipline (per sub-phase)

1. **Tests first or alongside** the behavior; at most **one** intentionally failing test while driving the change.
2. **Clean up** dead code, debug UI, and one-off flags before marking the sub-phase done.
3. **Update** this file and the parent plan’s Phase 1 bullet if scope or decisions change.
4. **Deploy gate** between sub-phases if the team follows CD discipline from `planning.mdc`.

---

## Document maintenance

When Phase 1 is complete, fold any lasting API or coordinate decisions into [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md) (*Open architecture questions* / *Current directional choices*) and trim duplication here.
