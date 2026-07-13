import type { Router } from "vue-router"
import type {
  NoteCreationDto,
  NoteRealm,
} from "@generated/doughnut-backend-api"
import { parseSoftDeletedTitleConflict } from "@/managedApi/softDeletedTitleConflict"

type NoteCreateApi = {
  createRootNoteAtNotebook: (
    router: Router,
    notebookId: number,
    body: NoteCreationDto,
    options?: {
      folderId?: number
      refreshWikiTitleCacheForNoteIds?: number[]
    }
  ) => Promise<NoteRealm>
  restoreDeletedNote: (router: Router, noteId: number) => Promise<NoteRealm>
}

type ConfirmPopups = {
  confirm: (message: string) => Promise<boolean>
}

export function parseCreateNoteFailure(e: unknown): {
  fieldErrors: { newTitle?: string; wikidataId?: string }
  softDeletedNoteId?: number
} {
  const conflict = parseSoftDeletedTitleConflict(e)
  if (conflict?.deletedNoteId != null) {
    return { fieldErrors: {}, softDeletedNoteId: conflict.deletedNoteId }
  }
  return {
    fieldErrors: {
      newTitle: undefined,
      wikidataId: undefined,
      ...(typeof e === "object" && e !== null ? (e as object) : {}),
    },
  }
}

export async function createNoteFromForm(input: {
  api: NoteCreateApi
  router: Router
  popups: ConfirmPopups
  notebookId: number
  body: NoteCreationDto
  folderId?: number
  refreshWikiTitleCacheForNoteIds?: number[]
  onFieldErrors: (errors: { newTitle?: string; wikidataId?: string }) => void
  onSuccess: () => void
}): Promise<void> {
  try {
    await input.api.createRootNoteAtNotebook(
      input.router,
      input.notebookId,
      input.body,
      {
        folderId: input.folderId,
        refreshWikiTitleCacheForNoteIds: input.refreshWikiTitleCacheForNoteIds,
      }
    )
    input.onSuccess()
  } catch (e: unknown) {
    const { fieldErrors, softDeletedNoteId } = parseCreateNoteFailure(e)
    if (softDeletedNoteId != null) {
      const confirmed = await input.popups.confirm(
        "A note with this title was deleted. OK restores that note instead of creating a new one."
      )
      if (confirmed) {
        try {
          await input.api.restoreDeletedNote(input.router, softDeletedNoteId)
          input.onSuccess()
        } catch (res: unknown) {
          input.onFieldErrors({
            newTitle: undefined,
            wikidataId: undefined,
            ...(res as object),
          })
        }
      }
      return
    }
    input.onFieldErrors({
      newTitle: fieldErrors.newTitle,
      wikidataId: fieldErrors.wikidataId,
    })
  }
}
