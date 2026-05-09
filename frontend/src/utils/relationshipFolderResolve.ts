import type { FolderListing } from "@generated/doughnut-backend-api"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { refreshSidebarStructuralListings } from "@/components/notes/sidebarStructuralRefresh"

export type RelationshipNotePlacement =
  | "relations_subfolder"
  | "same_level_as_source"
  | "named_after_source_note"

function folderNameForSourceNote(title: string | undefined | null): string {
  if (title == null || title.trim() === "") {
    return " "
  }
  return title
}

async function loadFolderListing(
  notebookId: number,
  parentFolderId: number | null
): Promise<FolderListing> {
  const { data, error } = await apiCallWithLoading(() =>
    parentFolderId == null
      ? NotebookController.listNotebookRootNotes({
          path: { notebook: notebookId },
        })
      : NotebookController.listFolderListing({
          path: { notebook: notebookId, folder: parentFolderId },
        })
  )
  if (error || !data) throw new Error("Failed to load folder listing")
  return data
}

async function findOrCreateChildFolder(
  notebookId: number,
  parentFolderId: number | null,
  childName: string
): Promise<number> {
  const listing = await loadFolderListing(notebookId, parentFolderId)
  const match = listing.folders?.find((f) => f.name === childName)
  if (match?.id != null) {
    return match.id
  }
  const body =
    parentFolderId == null
      ? { name: childName }
      : { name: childName, underFolderId: parentFolderId }
  const { data: created, error } = await apiCallWithLoading(() =>
    NotebookController.createFolder({
      path: { notebook: notebookId },
      body,
    })
  )
  if (error || !created) throw new Error("Failed to create folder")
  if (created.id == null) {
    throw new Error("createFolder did not return folder id")
  }
  refreshSidebarStructuralListings()
  return created.id
}

export type ResolveRelationshipNoteFolderParams = {
  notebookId: number
  sourceFolderId: number | undefined
  sourceTitle: string | undefined
  placement: RelationshipNotePlacement
}

/** Resolves target folder id for a new relationship note (matches former `NoteChildContainerFolderService` behavior). */
export async function resolveRelationshipNoteFolderId({
  notebookId,
  sourceFolderId,
  sourceTitle,
  placement,
}: ResolveRelationshipNoteFolderParams): Promise<number | undefined> {
  switch (placement) {
    case "same_level_as_source":
      return sourceFolderId
    case "relations_subfolder":
      return findOrCreateChildFolder(
        notebookId,
        sourceFolderId ?? null,
        "relations"
      )
    case "named_after_source_note":
      return findOrCreateChildFolder(
        notebookId,
        sourceFolderId ?? null,
        folderNameForSourceNote(sourceTitle)
      )
    default:
      return sourceFolderId
  }
}
