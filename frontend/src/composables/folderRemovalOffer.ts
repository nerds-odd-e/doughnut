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

const trashSubject = (name: string) =>
  `Trash folder "${name}" with its complete subtree`

const trashConsequence =
  "Its contents leave active use. References remain authored, but may no longer resolve until the folder is recovered with Move."

const removalMessages = (subject: string, consequence: string) => ({
  description: `${subject}. ${consequence}`,
  confirmation: `${subject}? ${consequence}`,
})

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
      ...removalMessages(
        permanentDeleteSubject(name),
        permanentDeleteConsequence
      ),
      failureMessage: "Failed to permanently delete folder",
      request: (path) => NotebookController.permanentlyDeleteFolder({ path }),
    }
  }
  return {
    testId: "folder-trash-button",
    buttonLabel: "Trash folder",
    ...removalMessages(trashSubject(name), trashConsequence),
    failureMessage: "Failed to trash folder",
    request: (path) => NotebookController.trashFolder({ path }),
  }
}
