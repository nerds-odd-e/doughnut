# Notebook groups — shipped

**Planning rules:** `.cursor/rules/planning.mdc`

This feature is **implemented**. The sections below record what exists and what is optional next.

---

## Product behavior (current)

- Owned notebooks are organized into **one-level** groups (`notebook_group` per `ownership`, `notebook.notebook_group_id`).
- **My notebooks** uses a single ordered **`catalogItems`** list: `notebook`, `notebookGroup`, and `subscribedNotebook` rows ([`NotebookCatalogItem`](../backend/src/main/java/com/odde/doughnut/controllers/dto/NotebookCatalogItem.java)).
- **Sort keys** (single place): [`NotebookCatalogService`](../backend/src/main/java/com/odde/doughnut/services/NotebookCatalogService.java) — notebook/subscribed rows by head note `created_at`, groups by `notebook_group.created_at`.
- **Subscriber-local grouping:** `subscription.notebook_group_id` ([`NotebookGroupService.assignSubscriptionToGroup`](../backend/src/main/java/com/odde/doughnut/services/NotebookGroupService.java)); owned notebooks still use `notebook.notebook_group_id`.
- **APIs:** `POST /api/notebook-groups`, `PATCH /api/notebooks/{id}/notebook-group`, subscription group PATCH on [`SubscriptionController`](../backend/src/main/java/com/odde/doughnut/controllers/SubscriptionController.java).
- **UI:** [`NotebooksPageView`](../frontend/src/pages/NotebooksPageView.vue) + [`NotebookCatalogSection`](../frontend/src/components/notebook/NotebookCatalogSection.vue); client-side **filter** on the merged list (search by notebook or group name).
- **E2E:** [`e2e_test/features/notebooks/notebook_group.feature`](../e2e_test/features/notebooks/notebook_group.feature).

---

## API notes

- **`GET /api/notebooks` (`myNotebooks`)** still returns `notebooks` (flat owned list) for compatibility and for **circle** / other clients; the **personal** page uses **`catalogItems`** (+ **`subscriptions`** for subscription actions keyed by `subscriptionId` on catalog rows).
- **Circle** notebook list still renders the flat **`notebooks`** field ([`CircleShowPage.vue`](../frontend/src/pages/CircleShowPage.vue)); **`catalogItems`** is populated on the same DTO for API parity with tests.

---

## Optional follow-ups

- **Rename / delete** group (product rules for members on delete).
- **Circle UI:** render **`catalogItems`** like the personal page (groups + hints) when product wants parity.
- **Thin API:** drop duplicate **`subscriptions`** once each `subscribedNotebook` row carries enough **`Subscription`** payload for the client (OpenAPI + `pnpm generateTypeScript`).

---

## Cleanup done (this pass)

- Removed unused **`Ownership.jsonNotebooksViewedByUser`**.
- **My notebooks** page no longer mirrors **`notebooks`** in component state; **`catalogItems`** is the source of truth for the list.
