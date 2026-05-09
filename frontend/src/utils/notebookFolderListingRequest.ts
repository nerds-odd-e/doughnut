import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"

/** Notebook root when `parentFolderId` is null; otherwise notes and child folders under that folder. */
export function requestNotebookFolderListing(
  notebookId: number,
  parentFolderId: number | null
) {
  return NotebookController.listNotebookFolderListing({
    path: { notebook: notebookId },
    query: parentFolderId == null ? undefined : { parent: parentFolderId },
  })
}
