# Context: Add notebook to group

## Intent

Users create notebooks into an existing group from the catalog group card (and group page), with the create form naming the target group.

## Key files

- Backend: `NotebookCreationRequest`, `NotebookController.createNotebook`, `CircleController.createNotebookInCircle`, `NotebookGroupService`
- Frontend: `NotebookCatalogGroupPanel`, `NotebookNewForm`, `NotebookNewButton`
- E2E: `notebook_group.feature`, notebook creation form page object
