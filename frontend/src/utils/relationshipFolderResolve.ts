import type { StoredApi } from "@/store/StoredApiCollection"

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

async function findOrCreateChildFolder(
  api: StoredApi,
  notebookId: number,
  parentFolderId: number | null,
  childName: string
): Promise<number> {
  const listing =
    parentFolderId == null
      ? await api.loadNotebookRootNotes(notebookId)
      : await api.loadFolderListing(notebookId, parentFolderId)
  const match = listing.folders?.find((f) => f.name === childName)
  if (match?.id != null) {
    return match.id
  }
  const body =
    parentFolderId == null
      ? { name: childName }
      : { name: childName, underFolderId: parentFolderId }
  const created = await api.createFolder(notebookId, body)
  if (created.id == null) {
    throw new Error("createFolder did not return folder id")
  }
  return created.id
}

export type ResolveRelationshipNoteFolderParams = {
  api: StoredApi
  notebookId: number
  sourceFolderId: number | undefined
  sourceTitle: string | undefined
  placement: RelationshipNotePlacement
}

/** Resolves target folder id for a new relationship note (matches former `NoteChildContainerFolderService` behavior). */
export async function resolveRelationshipNoteFolderId({
  api,
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
        api,
        notebookId,
        sourceFolderId ?? null,
        "relations"
      )
    case "named_after_source_note":
      return findOrCreateChildFolder(
        api,
        notebookId,
        sourceFolderId ?? null,
        folderNameForSourceNote(sourceTitle)
      )
    default:
      return sourceFolderId
  }
}
