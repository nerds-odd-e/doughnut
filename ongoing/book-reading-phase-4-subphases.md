# Phase 4 sub-plan — Browser: notebook book entry + BookReading layout

**Parent:** [book-reading-story-1-plan.md](book-reading-story-1-plan.md) — Phase 4.

**Discipline:** [.cursor/rules/planning.mdc](../.cursor/rules/planning.mdc) — one user-visible outcome per sub-phase, observable tests, TDD order, at most **one** intentionally failing Cypress scenario while driving each slice.

**API:** Book data via existing **`GET /api/notebooks/{notebook}/book`** (404 when none). No new HTTP surface required unless implementation discovers a gap.

**E2E scope:** Run **`CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/book_reading/book_reading.feature`** when touching this feature (not the full suite).

**Deploy gate:** Same as the parent plan — commit/push and CD between sub-phases if the team keeps phase boundaries on `main`.

---

## Sub-phase 4.1 — E2E fails for the right reason: book title + Read on notebook page

**User-visible outcome (asserted):** After the CLI has successfully attached a book, opening that notebook in the web app shows the **book display title** and a **Read** control **grouped in one section** on the notebook experience (exact copy and markup to be chosen in 4.2; the test pins observable text/roles).

**Test-first:**

- Replace the commented **Then** in [`e2e_test/features/book_reading/book_reading.feature`](../e2e_test/features/book_reading/book_reading.feature) with a scenario that:
  1. Keeps the existing **When** attach step.
  2. **Navigates to the notebook in the browser** after attach (reuse patterns such as `navigateToNotebooksPage` → `subscribedNotebooks().openNotebook(...)` from existing step/page objects, or a thin book-reading page object — avoid duplicating selectors).
  3. Asserts the **book title** (match what the CLI/fixture attach uses, e.g. derived name for `top-maths.pdf`) and a **Read** button (or link) in the **same** containing section.

**Synchronization with CLI:** The attach **When** must complete its **success** checks before navigation/assertions run. Today [`e2e_test/step_definitions/book_reading.ts`](../e2e_test/step_definitions/book_reading.ts) chains `useNotebook` → `attachPdfBook` → transcript assertions. Ensure the new **Then** (or an intermediate **When**) is implemented as **one Cypress command chain** that only starts **after** that chain resolves (e.g. continue `.then(() => …)` from the same wrapped context, or return `cy.wrap` from the final assertion step). Goal: no flake from visiting the notebook before `attach-book` has finished.

**Complete when:** Cypress fails **only** because the notebook UI does not yet show the title + Read in one section (clear assertion messages).

---

## Sub-phase 4.2 — Notebook page: show attached book + Read (inert)

**User-visible outcome:** On the notebook screen (`NotebookPage.vue` / [`NotebookPageView.vue`](../frontend/src/pages/NotebookPageView.vue) — place UI where the rest of the notebook layout lives), when the notebook has a book, show a **single section** with the **book title** and a **Read** control. **Read does not navigate yet** (no route, or button without handler — whichever keeps the build honest); sub-phase 4.4 will require the click behavior.

**Implementation notes:**

- Load book with the generated client for **`GET .../book`** (handle 404 as “no book” only if 4.3 is done in the same PR; if 4.3 is separate, minimal handling is acceptable as long as the happy path works for 4.1’s scenario).
- Match existing DaisyUI / layout conventions on the notebook page.

**Complete when:** Sub-phase 4.1 scenario **passes**; Read is visible but non-navigating.

---

## Sub-phase 4.3 — No book: copy + no Read (frontend unit test)

**User-visible outcome:** When there is **no** book on the notebook, the notebook page shows **no book attached** (or equivalent product copy) and **does not** show Read.

**Tests:** **Vitest** (mounted component or page-level test per [`.cursor/rules/frontend.mdc`](../.cursor/rules/frontend.mdc)) asserting the empty state and absence of Read. Prefer driving the same component/route entry used in production, with API mocked to 404 or empty book.

**Complete when:** Unit test passes; E2E for attach scenario still green.

---

## Sub-phase 4.4 — E2E fails for the right reason: Read → BookReading + layout

**User-visible outcome (asserted):** From the notebook, clicking **Read** opens a **router-backed** **BookReading** experience and the user sees the **hierarchical book layout** (the story table: main topics and indented subtopics — align with fixture/mock MinerU output used in E2E).

**Test-first:**

- Extend [`e2e_test/features/book_reading/book_reading.feature`](../e2e_test/features/book_reading/book_reading.feature) (same scenario or a focused second scenario — avoid two failing scenarios at once).
- Steps: attach (existing) → open notebook → assert title + Read → **click Read** → assert **URL/route** and **visible outline** matching expected rows (DataTable or explicit `contains` with clear failure messages).

**Complete when:** Cypress fails **only** because BookReading route/page/layout is missing or wrong — not because of the earlier steps.

---

## Sub-phase 4.5 — BookReading route, page, and layout

**User-visible outcome:** **Vue Router** registers a **BookReading** route (path TBD; e.g. under notebook id). The page loads book + ranges from **`GET .../book`** and renders the **hierarchical structure** (expand/collapse optional if the test only needs visible hierarchy; follow existing Doughnut navigation patterns).

**Implementation notes:**

- Wire **Read** on the notebook page to **`router.push`** (or equivalent) to the new route.
- Run **`pnpm generateTypeScript`** only if OpenAPI / client types change.

**Complete when:** Sub-phase 4.4 scenario **passes**; notebook empty state (4.3) still covered by unit test.

---

## After Phase 4

- Shorten or link Phase 4 in [book-reading-story-1-plan.md](book-reading-story-1-plan.md) to this file once sub-phases are done.
- If IA stabilizes, note the chosen route path and section labels in the parent plan or roadmap if useful for Story 2+.
