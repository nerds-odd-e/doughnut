# Plan: Notebook Group

**Planning rules:** `.cursor/rules/planning.mdc` — one **user-visible** behavior per phase, scenario-first ordering, observable tests (controllers, mounted UI, pure I/O), at most one intentionally failing test while driving a multi-step E2E slice.

**This document is a delivery plan only** — not executed here.

---

## Product intent

- A user **organizes owned notebooks** into **notebook groups**.
- **One level only** — groups do not nest; a notebook belongs to **none or one** group.
- Users can **move a notebook into a group** and **move it out** (back to “ungrouped”).
- Each group has a **name** and appears on the **notebooks page** in a **single merged list** with **ungrouped** notebooks, ordered by **creation time** (see **Sort key** below).
- Group rows use a **visually distinct** treatment from notebook cards.
- At the notebooks page, the user gets a **hint** of which notebooks are inside each group (e.g. short list of titles, ellipsis, and/or count — exact copy and limits are an implementation detail).

**Scope note:** Start with **personal** `NotebookController.myNotebooks` ownership (see [`NotebookController.java`](../backend/src/main/java/com/odde/doughnut/controllers/NotebookController.java), [`NotebooksPage.vue`](../frontend/src/pages/NotebooksPage.vue)). **Circle** notebooks use a parallel path ([`CircleForUserView`](../backend/src/main/java/com/odde/doughnut/controllers/dto/CircleForUserView.java)); treat **circle parity** as a **later phase** unless product requires it on day one.

---

## Sort key (creation time)

The `notebook` table today has **`updated_at`** but no **`created_at`** ([`V10000063__baseline.sql`](../backend/src/main/resources/db/migration/V10000063__baseline.sql)). Before implementing merged ordering, **pick one server-visible rule**, for example:

- Add **`notebook.created_at`**, backfill from the **head note’s** first-known timestamp (or `updated_at` as a one-time fallback), and use **`notebook_group.created_at`** for groups; **or**
- Define “created” as **head note creation** only for notebooks and persist **`notebook_group.created_at`** for groups, without a new notebook column.

Document the chosen rule in code (single place) so API and UI stay consistent.

---

## Testing strategy (per user direction)

| Layer | Role |
|--------|------|
| **E2E (Cypress)** | **One** feature (or one focused scenario) that exercises the **main happy path** end-to-end: e.g. create a group → assign a notebook → see list + **hint** → remove notebook from group. |
| **Unit / integration-style** | Everything else: **Spring controller** tests (status, body, auth, validation, persistence via follow-up `GET`), **mounted** [`NotebooksPageView`](../frontend/src/pages/NotebooksPageView.vue) / child components with **mocked SDK** ([`frontend.mdc`](../.cursor/rules/frontend.mdc)), and **minimal pure** tests only for intentional small contracts (e.g. sort merge, hint formatting). Prefer **observable surfaces** over tests that mirror private helpers ([`planning.mdc`](../.cursor/rules/planning.mdc)). |

---

## Phase 1 — Persist notebook groups and ownership scope

**User outcome:** The system can **store** notebook groups tied to an **ownership** (same boundary as [`Ownership.notebooks`](../backend/src/main/java/com/odde/doughnut/entities/Ownership.java)), and **optionally** associate a notebook with at most one group via a nullable FK.

**Suggested shape (implementation detail):**

- Flyway: `notebook_group` (`id`, `ownership_id`, `name`, `created_at`, …), `notebook.notebook_group_id` nullable FK → `notebook_group`, enforce **no nesting** by schema only (no parent on group).
- JPA entities + repository; authorization: only owners of that ownership may mutate/read groups for that list.
- No user-facing UI required to **close** this phase if tests prove persistence via **controller** (or test slice) APIs introduced in Phase 2 — *or* include a minimal `POST` here if the team prefers persistence + create in one phase.

**Tests:** Controller (or repository integration) tests — **create group**, **invalid ownership**, **assign notebook** to group, **reject** second group assignment (same notebook), **clear** `notebook_group_id`.

---

## Phase 2 — APIs: create group, list catalog for notebooks page

**User outcome:** Authenticated user can **create a named group**. `GET` notebooks catalog (extend [`NotebooksViewedByUser`](../backend/src/main/java/com/odde/doughnut/controllers/dto/NotebooksViewedByUser.java) or replace with a richer DTO) returns enough to render the **merged list**: **ungrouped notebooks** + **groups** (with member notebook ids or embedded summaries for hints), **sorted** by the agreed **creation** rule.

**Suggested shape:**

- `POST /api/notebook-groups` (or under `/api/notebooks/...` — follow existing routing style) with **name**; returns created group.
- Extend **`myNotebooks`** response so the client does not re-implement authorization-sensitive merging. Options: discriminated **`catalogItems[]`**, or `notebookGroups[]` + `notebooks[]` plus explicit **sort keys** — prefer **one ordered list** if it keeps the UI dumb and matches “listed together.”
- OpenAPI update → `pnpm generateTypeScript` ([`generated-backend-api`](../.cursor/rules/generated-backend-api-code-for-frontend.mdc)).

**Tests:** `NotebookController` / dedicated controller tests — create group, list includes group, sort order assertions, ungrouped notebooks omitted from “top-level” duplicate if the product rule is **grouped notebooks only under their group row** (state this explicitly in implementation).

---

## Phase 3 — Assign and remove notebook from group

**User outcome:** User can set **which group** a notebook belongs to, or **none** (ungrouped). **No** notebook in two groups; **no** assigning to another user’s group.

**Suggested shape:**

- `PATCH` on existing notebook update endpoint or a narrow `PATCH /api/notebooks/{id}/notebook-group` with nullable body.
- Server validates: group **same ownership** as notebook, notebook **not** subscribed-only foreign state if that matters for your rules.

**Tests:** Controller tests for **happy path**, **wrong ownership**, **nonexistent group**, **clear group** (null).

---

## Phase 4 — Notebooks page: merged list, distinct group styling, content hint

**User outcome:** On [`NotebooksPageView`](../frontend/src/pages/NotebooksPageView.vue) (and supporting components), the user sees **groups and ungrouped notebooks** in **one list**, **ordered by creation time**, **groups visually distinct** from notebook cards, and a **hint** (titles / count) for **members**.

**Suggested shape:**

- Replace or augment [`NotebookCardsWithButtons`](../frontend/src/pages/NotebooksPageView.vue) usage so the **iteratee** can be **notebook vs group** (wrapper component or two card types in one list).
- Reuse existing **notebook actions** for member notebooks; add **group actions** (rename/delete optional — see Phase 5).
- Hint text: keep **accessible** (not only color): e.g. `aria-label` or visible subtitle.

**Tests:** **Mounted** tests with **mocked** `myNotebooks` / mutations — snapshot of ordering, presence of hint, distinct classes/roles for group vs notebook. **No new E2E** until Phase 5 if the team wants a single E2E after UI stabilizes.

---

## Phase 5 — One end-to-end scenario

**User outcome:** Confidence that the **full path** works in the browser.

**Suggested shape:**

- One Gherkin feature (or one scenario in an existing notebooks feature) under `e2e_test/features/…`: create group → assign notebook → assert **hint** visible → remove from group → assert notebook **ungrouped** appearance.
- Run with `--spec` for that file only during development ([`e2e_test.mdc`](../.cursor/rules/e2e_test.mdc)).

**Tests:** That single E2E; fix step definitions/page objects so **one failure at a time** when extending ([`planning.mdc`](../.cursor/rules/planning.mdc) E2E-led decomposition).

---

## Optional follow-ups (separate phases if needed)

- **Rename / delete** empty or non-empty group (product rules for members on delete).
- **Circle** notebook lists — same concept on circle ownership catalog.
- **Bazaar / subscription** notebooks — explicitly **out of scope** unless product says otherwise (subscribed notebooks are already a separate section on the page).

---

## Phase checklist (before closing each phase)

1. **Clean up** — no dead endpoints or unused DTO fields.
2. **Deploy gate** — commit, push, CD as per team habit ([`planning.mdc`](../.cursor/rules/planning.mdc)).
3. **Update this plan** — mark shipped phases, drop obsolete notes.
