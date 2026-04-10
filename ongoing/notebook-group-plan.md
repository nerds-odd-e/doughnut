# Notebook catalog UX — groups & ordering

**Planning rules:** `.cursor/rules/planning.mdc`

**Goal:** Organize notebooks from the **catalog** (row/card), not from the notebook settings page. **Subscriptions** are grouped by updating the **subscription** (`subscription.notebook_group_id`), not the owner’s notebook.

**Domain (unchanged, implementers must respect):**

- **Owned notebook:** `PATCH /api/notebooks/{id}/notebook-group` → `notebook.notebook_group_id`.
- **Subscribed notebook:** subscription group PATCH on [`SubscriptionController`](../backend/src/main/java/com/odde/doughnut/controllers/SubscriptionController.java) (same pattern as [`NotebookGroupService.assignSubscriptionToGroup`](../backend/src/main/java/com/odde/doughnut/services/NotebookGroupService.java)).
- **Catalog data:** [`NotebookCatalogItem`](../backend/src/main/java/com/odde/doughnut/controllers/dto/NotebookCatalogItem.java), ordering in [`NotebookCatalogService`](../backend/src/main/java/com/odde/doughnut/services/NotebookCatalogService.java). **My notebooks** UI: [`NotebooksPageView`](../frontend/src/pages/NotebooksPageView.vue), [`NotebookCatalogSection`](../frontend/src/components/notebook/NotebookCatalogSection.vue). Current row actions: [`NotebookButtons`](../frontend/src/components/notebook/NotebookButtons.vue), [`SubscriptionNoteButtons`](../frontend/src/components/subscriptions/SubscriptionNoteButtons.vue).

---

## Phase 1 — Overflow menu on each catalog row/card

**Outcome:** Every notebook and subscribed-notebook entry has a **`...`** control that opens a **dropdown**. **Edit notebook settings** and **Edit subscription** (today: pencil actions) live **inside** that menu. **Unsubscribe** stays **outside** the menu (visible on the row/card) so subscriptions remain obviously distinct.

**Scope:** List and grid layouts; top-level rows and notebooks inside group panels ([`NotebookCatalogSection`](../frontend/src/components/notebook/NotebookCatalogSection.vue), [`NotebookCatalogGroupPanel`](../frontend/src/components/notebook/NotebookCatalogGroupPanel.vue), cards path). Preserve existing bazaar/circle affordances on owned circle notebooks ([`NotebookButtons`](../frontend/src/components/notebook/NotebookButtons.vue))—either keep them next to `...` or fold only what matches “edit settings” semantics; do not hide subscription identity.

**Tests:** E2E or Vitest at **observable** level (DOM: `...` opens menu, unsubscribe still visible). Extend or replace steps that target the old pencil-only pattern ([`e2e_test/features/notebooks/notebook_group.feature`](../e2e_test/features/notebooks/notebook_group.feature), page objects under [`e2e_test/start/pageObjects/`](../e2e_test/start/pageObjects/)).

---

## Phase 2 — “Move to group…” dialog (owned + subscribed)

**Outcome:** From the **overflow menu**, **Move to group…** opens a **dialog** to pick a target: **existing groups**, **create new group** (name field + confirm), and **No group** / **Ungrouped** when the item **is already in a group** (hidden or disabled when already ungrouped—product: only show “remove from group” when it applies). Applying the choice updates the catalog without requiring a trip to the notebook page.

**Owned path:** `NotebookController.updateNotebookGroup` (existing client).

**Subscribed path:** Same UX, but API must update **subscription** group id, **not** the notebook—owner’s data stays untouched.

**Tests:** E2E covers at least one **owned** and one **subscribed** move; optional sub-phases (Gherkin prefix commented) if two failures at once is hard to debug. Assert catalog reflects new grouping.

---

## Phase 3 — Remove standalone “New notebook group” from My notebooks

**Outcome:** The **New notebook group** button is removed from [`NotebooksPageView`](../frontend/src/pages/NotebooksPageView.vue). **Creating** a group happens only via **Move to group… → New group** (Phase 2). Update E2E that currently clicks “New notebook group” ([`myNotebooksPage.ts`](../e2e_test/start/pageObjects/myNotebooksPage.ts)) to use the new flow.

---

## Phase 4 — Remove group controls from the notebook settings page

**Outcome:** Remove [`NotebookGroupAssignmentControl`](../frontend/src/components/notebook/NotebookGroupAssignmentControl.vue) (and any copy) from [`NotebookPageView`](../frontend/src/pages/NotebookPageView.vue). Grouping is **only** from the catalog dialog. Update E2E that assigns via notebook page ([`notebookPage.ts`](../e2e_test/start/pageObjects/notebookPage.ts), [`notebook_group.ts`](../e2e_test/step_definitions/notebook_group.ts)) to drive the catalog flow instead. Delete or shrink the component if nothing else imports it.

---

## Phase 5 — Circle page: same catalog affordances as My notebooks

**Outcome:** [`CircleShowPage`](../frontend/src/pages/CircleShowPage.vue) uses **`circle.notebooks.catalogItems`** with [`NotebookCatalogSection`](../frontend/src/components/notebook/NotebookCatalogSection.vue) (list/grid, sort, filter, overflow, move-to-group). `GET /api/circles/{id}` already returned merged `catalogItems` via [`NotebookCatalogService.buildView`](../backend/src/main/java/com/odde/doughnut/services/NotebookCatalogService.java). **Create group on circle:** optional `circleId` on [`CreateNotebookGroupRequest`](../backend/src/main/java/com/odde/doughnut/controllers/dto/CreateNotebookGroupRequest.java) + [`NotebookGroupController`](../backend/src/main/java/com/odde/doughnut/controllers/NotebookGroupController.java); circle page **provide**s catalog group choices for [`NotebookCatalogMoveToGroupDialog`](../frontend/src/components/notebook/NotebookCatalogMoveToGroupDialog.vue).

**Tests:** [`e2e_test/features/circles/notebooks_in_circles.feature`](../e2e_test/features/circles/notebooks_in_circles.feature) scenario “Circle catalog shows notebook groups and layout controls”; Vitest [`frontend/tests/pages/CircleShowPage.spec.ts`](../frontend/tests/pages/CircleShowPage.spec.ts).

---

## Phase 6 — Research, then “Last updated” sort

**Research (document findings in this file before implementation):**

- Which **timestamp** should define “last updated” for **owned notebook**, **subscribed** row, and **group** header (e.g. max of member updates, group `updated_at`, head note, latest note activity—pick one rule and apply in **one place**: [`NotebookCatalogService`](../backend/src/main/java/com/odde/doughnut/services/NotebookCatalogService.java)).
- Whether **client-only** sort is enough (if API already returns enough fields) or **server-ordered** `catalogItems` is required.

**Outcome:** New sort option alongside existing created / alphabetical ([`NotebooksPageView`](../frontend/src/pages/NotebooksPageView.vue), [`sortNotebookCatalogAlphabetically`](../frontend/src/components/notebook/sortNotebookCatalogAlphabetically.ts) or successor). **Circle** should offer the same option once Phase 5 lands (or same phase if trivial).

**Tests:** Prefer **one** observable test (E2E or black-box unit) that proves ordering changes when update timestamps differ; avoid mirroring sort implementation.

---

## Phase discipline (checklist)

Before closing each phase: behavior covered by tests in that phase, dead UI removed, this doc updated (drop notes that no longer apply).
