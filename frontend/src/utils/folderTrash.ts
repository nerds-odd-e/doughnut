import type {
  Folder,
  FolderRealm,
  NoteRealm,
} from "@generated/donut-backend-api"

const TRASH_ROOT_NAME = "_trash"

/**
 * Determines whether a location is in trash by inspecting its notebook-root
 * folder. The root is the first ancestor when present, otherwise the current
 * folder itself (i.e. the folder is the notebook root). Matching is
 * case-insensitive per ADR 0004.
 */
export function isLocationInTrash(
  ancestorFolders: Folder[],
  currentFolder: Folder | undefined
): boolean {
  const root = ancestorFolders[0] ?? currentFolder
  return root?.name.toLowerCase() === TRASH_ROOT_NAME
}

/** A folder is in trash when its own chain starts at the trash root. */
export function isFolderRealmInTrash(
  folderRealm: FolderRealm | undefined
): boolean {
  return isLocationInTrash(
    folderRealm?.ancestorFolders ?? [],
    folderRealm?.folder
  )
}

/** A note is in trash when its own folder chain starts at the trash root. */
export function isNoteRealmInTrash(noteRealm: NoteRealm | undefined): boolean {
  return isLocationInTrash(noteRealm?.ancestorFolders ?? [], undefined)
}
