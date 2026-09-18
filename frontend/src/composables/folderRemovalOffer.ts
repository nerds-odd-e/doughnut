import type { FolderRealm } from "@generated/donut-backend-api"
import { NotebookController } from "@generated/donut-backend-api/sdk.gen"
import { isFolderRealmInTrash } from "@/utils/folderTrash"

export type FolderPath = { notebook: number; folder: number }

export type FolderRemovalOffer = {
  testId: string
  buttonLabel: string
  description: string
  confirmation: string
  failureMessage: string
  request: (path: FolderPath) => Promise<{
    error?: unknown
    response?: { status?: number }
  }>
}

const permanentDeleteSubject = (name: string) =>
  `Permanently delete folder "${name}" and everything inside it`

const permanentDeleteConsequence =
  "All its notes are deleted with their learning history, questions, conversations and images, and this cannot be undone. Earlier Git history of this notebook still contains the text."

const trashDescription = (name: string) =>
  `Trash "${name}" with its complete subtree. Its contents leave active use. References remain authored, but may no longer resolve until the folder is recovered with Move.`

const trashConfirmation = (name: string) =>
  `Trash folder "${name}"? Its complete subtree will leave active use. References remain authored but may no longer resolve until you recover the folder with Move.`

/**
 * The single removal a folder's Settings tab offers: Trash while the folder is
 * active, permanent deletion once it is in trash. ADR 0001 keeps the two
 * distinct, so a folder never offers both.
 */
export function folderRemovalOffer(
  folderRealm: FolderRealm
): FolderRemovalOffer {
  const name = folderRealm.folder.name
  if (isFolderRealmInTrash(folderRealm)) {
    return {
      testId: "folder-permanent-delete-button",
      buttonLabel: "Permanently delete folder",
      description: `${permanentDeleteSubject(name)}. ${permanentDeleteConsequence}`,
      confirmation: `${permanentDeleteSubject(name)}? ${permanentDeleteConsequence}`,
      failureMessage: "Failed to permanently delete folder",
      request: (path) => NotebookController.permanentlyDeleteFolder({ path }),
    }
  }
  return {
    testId: "folder-trash-button",
    buttonLabel: "Trash folder",
    description: trashDescription(name),
    confirmation: trashConfirmation(name),
    failureMessage: "Failed to trash folder",
    request: (path) => NotebookController.trashFolder({ path }),
  }
}
