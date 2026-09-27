import type { FolderListing } from "@generated/donut-backend-api"
import { NotebookFolderController } from "@generated/donut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"

/** Notebook root when `parentFolderId` is null; otherwise notes and child folders under that folder. */
export function requestNotebookFolderListing(
  notebookId: number,
  parentFolderId: number | null
) {
  return NotebookFolderController.listNotebookFolderListing({
    path: { notebook: notebookId },
    query: parentFolderId == null ? undefined : { parent: parentFolderId },
  })
}

/** A listing the person is waiting for: shows the app as busy and fails loudly. */
export async function loadFolderListing(
  notebookId: number,
  parentFolderId: number | null
): Promise<FolderListing> {
  const { data, error } = await apiCallWithLoading(() =>
    requestNotebookFolderListing(notebookId, parentFolderId)
  )
  if (error || !data) throw new Error("Failed to load folder listing")
  return data
}
