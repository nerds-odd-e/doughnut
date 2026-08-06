# Add notebook to group

## Goal

From a notebook group on `/notebooks` (or the group page that reuses the same panel), open **Add notebook…**, see which group the notebook will join, submit, and get a notebook that is already in that group.

## Design decisions

1. **Create-with-group on the API** — optional `notebookGroupId` on `NotebookCreationRequest`; assign in the same transaction via `NotebookGroupService.assignNotebookToGroup`.
2. **Group overflow menu** on `NotebookCatalogGroupPanel` (covers catalog + group page).
3. **Form clarity** — helper text `Creates in group "…"`.
4. **Circle catalogs** — use `createNotebookInCircle` when circle context is injected.
5. **After success** — navigate to the new notebook page (existing behavior).

## Phases

### Phase 1 — Structure: create API accepts optional group

**Status:** done
**Type:** Structure

- Add optional `notebookGroupId` to `NotebookCreationRequest`.
- Assign after create in `NotebookController.createNotebook` and `CircleController.createNotebookInCircle`.
- Controller tests; regenerate TS client.

**Done when:** unit tests pass; client types include `notebookGroupId`.

### Phase 2 — Behavior: group menu → form shows group → notebook in group

**Status:** done
**Type:** Behavior

- E2E: add notebook from group overflow; form shows group; notebook listed under group.
- UI: group overflow + `NotebookNewForm` group prop/hint/`notebookGroupId`.

**Done when:** E2E passes without `@wip`.

## Out of scope

- Creating a new group from this menu.
- Changing ungrouped GlobalBar “Add New Notebook”.
