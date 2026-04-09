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
- **Later (Phases 6–8):** **Subscribed (Bazaar) notebooks** appear in the **same** merged **`catalogItems`** list as owned notebooks and groups, and can be placed in the **subscriber’s** personal groups **without** changing the shared [`Notebook`](../backend/src/main/java/com/odde/doughnut/entities/Notebook.java) row’s `notebook_group_id` (see Phase 7).

**Ownership scope:** Notebook groups are tied to an [`Ownership`](../backend/src/main/java/com/odde/doughnut/entities/Ownership.java) — the same row backs **personal** notebooks (`NotebookController.myNotebooks`, [`NotebooksPage.vue`](../frontend/src/pages/NotebooksPage.vue)) and **circle** notebooks (`CircleForUserView` via [`CircleController.showCircle`](../backend/src/main/java/com/odde/doughnut/controllers/CircleController.java)). Groups for a circle **belong to that circle’s ownership** (`notebook_group.ownership_id` = the circle’s ownership). Backend catalog merging applies to both paths; **circle-specific behavior does not need its own E2E test** — cover it with **unit / controller** tests (see Phase 2 tests). **Circle UI** on the notebooks list (when built) should mirror the personal page pattern for catalog + hints.

**Catalog “category” (discriminator):** The notebooks-page catalog is a discriminated union in [`NotebookCatalogItem`](../backend/src/main/java/com/odde/doughnut/controllers/dto/NotebookCatalogItem.java): JSON/OpenAPI **`type`** distinguishes rows (today `notebook` vs `notebookGroup`). Tooling and generated clients often surface this as a **category** or variant. Phases 6–8 extend this contract so **subscribed** rows are first-class in **one** ordered list while still allowing **different actions** (e.g. unsubscribe vs edit owned notebook) where needed.

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

**Tests:** `NotebookController` / dedicated controller tests — create group, list includes group, sort order assertions, ungrouped notebooks omitted from “top-level” duplicate if the product rule is **grouped notebooks only under their group row** (state this explicitly in implementation). **`CircleController.showCircle`:** assert nested `NotebooksViewedByUser.catalogItems` includes circle-owned groups and members the same way (no separate Cypress scenario for circle groups).

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

## Phase 6 — Unified catalog: subscribed notebooks in `catalogItems`

**User outcome:** On `myNotebooks`, **subscribed (Bazaar) notebooks** appear in the **same ordered `catalogItems` list** as **ungrouped owned** notebooks and **notebook group** rows—no separate “Subscribed notebooks” **slice** in the API contract for ordering/rendering the main list (the UI may still show a small explainer line if product wants one).

**Suggested shape:**

- Extend [`NotebookCatalogService`](../backend/src/main/java/com/odde/doughnut/services/NotebookCatalogService.java) / DTOs so **subscriptions** participate in **`catalogItems`**. Options (pick one and keep OpenAPI accurate):
  - **New discriminant** value under [`NotebookCatalogItem`](../backend/src/main/java/com/odde/doughnut/controllers/dto/NotebookCatalogItem.java) (e.g. `subscribedNotebook`) carrying **notebook + subscription** identifiers and any fields the list needs; **or**
  - Keep `type: "notebook"` and add an explicit **origin** field (`owned` vs `subscribed`) if that stays clear for clients and generators.
- **Sort key** for subscribed top-level rows: document one rule (e.g. **head note `created_at`** like owned ungrouped rows, or **subscription** timestamp—may require a small migration if you add `subscription.created_at` / similar). **Tie-break** with stable ids so order is deterministic.
- **`subscriptions` on [`NotebooksViewedByUser`](../backend/src/main/java/com/odde/doughnut/controllers/dto/NotebooksViewedByUser.java):** Either **derive** from `catalogItems` for subscribers or **deprecate** in favor of catalog-only once the client migrates—avoid long-lived duplicate sources of truth.
- OpenAPI update → `pnpm generateTypeScript`.

**Tests:** `NotebookController` tests—merged `catalogItems` includes a subscribed row **between** owned rows/groups per sort rule; discriminator/origin field assertions. **Circle path:** only if circle notebooks list ever exposes subscriptions the same way; otherwise **omit** and note “personal `myNotebooks` only” in code.

**Note:** If Phase 7 is not done yet, treat **all** subscribed rows as **top-level** catalog entries (not nested under groups). Nesting comes in Phase 7.

---

## Phase 7 — Subscriber-local group: add subscribed notebook to **my** group

**User outcome:** A user can assign a **subscribed** notebook to **one of their personal notebook groups**, or **none** (ungrouped in **their** catalog). **No** change to [`Notebook.notebookGroup`](../backend/src/main/java/com/odde/doughnut/entities/Notebook.java) for this—only the **subscriber’s** view/organization changes.

**Suggested shape:**

- Flyway: nullable **`subscription.notebook_group_id`** → `notebook_group`, with integrity rules such as: subscription’s `user_id` is the subscriber; group’s `ownership_id` is that user’s **personal** ownership (same boundary as personal groups in Phase 1); notebook is the subscription’s `notebook_id`.
- **Catalog builder:** Subscribed notebooks with a non-null **subscriber group** appear **only** under that **group row** (as members), not as duplicate top-level rows—mirror the rule for **owned** grouped notebooks.
- API: extend an existing subscription update DTO/endpoint or add a narrow `PATCH` (nullable body = ungrouped). Reject wrong ownership, non-subscribed notebook, or assigning to someone else’s group.
- Owned notebooks continue to use **`notebook.notebook_group_id`**; subscribed use **`subscription.notebook_group_id`**. Document the split in one place (service or catalog builder).

**Tests:** Controller tests—happy path, wrong group ownership, not subscribed, clear group; catalog lists members under the group row. Prefer **observable** responses over repository-only tests ([`planning.mdc`](../.cursor/rules/planning.mdc)).

---

## Phase 8 — Notebooks page: one list + subscribed grouping in the UI

**User outcome:** [`NotebooksPageView`](../frontend/src/pages/NotebooksPageView.vue) renders **one** main list from **`catalogItems`** (groups, owned notebooks, subscribed rows), with **subscription** actions where needed. The old **separate** subscribed section is removed or reduced to non-duplicating chrome only.

**Suggested shape:**

- Consume **`catalogItems`** (discriminant / origin) instead of a parallel `subscriptions` list for layout/order.
- Group rows: **member** rendering supports **owned** and **subscribed** members (hints, counts, actions).
- **Mounted** tests with mocked `myNotebooks`—ordering, discriminant handling, group member row for subscribed.

**Tests:** Extend the Phase 5 Gherkin **or** add a focused scenario: subscribed notebook appears in the **merged** list → user moves it **into** a group → hint/membership visible → moves **out** to ungrouped. Use E2E-led decomposition if the scenario is long: **one newly enabled step failing at a time** ([`planning.mdc`](../.cursor/rules/planning.mdc)).

---

## Optional follow-ups (separate phases if needed)

- **Rename / delete** empty or non-empty group (product rules for members on delete).
- **Circle + subscriptions:** if the product later mixes circle notebooks with personal subscriptions in one UI, reconcile ownership rules for group placement before reusing Phase 6–7 patterns.

---

## Phase checklist (before closing each phase)

1. **Clean up** — no dead endpoints or unused DTO fields.
2. **Deploy gate** — commit, push, CD as per team habit ([`planning.mdc`](../.cursor/rules/planning.mdc)).
3. **Update this plan** — mark shipped phases, drop obsolete notes.
