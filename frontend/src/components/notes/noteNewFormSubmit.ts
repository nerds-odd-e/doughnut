import type { Router } from "vue-router"
import type { NoteCreationDto, NoteRealm } from "@generated/donut-backend-api"
import {
  applyParentRelationshipToCreateContent,
  type NoteCreationParentRelationship,
} from "@/utils/noteCreationParentRelationship"

type NoteCreateApi = {
  createRootNoteAtNotebook: (
    router: Router,
    notebookId: number,
    body: NoteCreationDto,
    options?: {
      folderId?: number
      refreshWikiLinkCacheForNoteIds?: number[]
    }
  ) => Promise<NoteRealm>
}

type ConfirmPopups = {
  confirm: (message: string) => Promise<boolean>
}

export function contentWithWikidataFrontmatter(
  wikidataId: string
): string | undefined {
  const t = wikidataId.trim()
  return t ? `---\nwikidata_id: ${t}\n---\n` : undefined
}

export function contentForNewNote(input: {
  noteContentMarkdown: string | undefined
  wikidataId: string
  parentRelationship: NoteCreationParentRelationship
  contextNote: { title: string; content?: string } | undefined
}): string | undefined {
  const baseContent =
    input.noteContentMarkdown !== undefined
      ? input.noteContentMarkdown
      : contentWithWikidataFrontmatter(input.wikidataId)
  return applyParentRelationshipToCreateContent(
    baseContent,
    input.parentRelationship,
    input.contextNote
  )
}

export function parseCreateNoteFailure(e: unknown): {
  fieldErrors: { newTitle?: string; wikidataId?: string }
} {
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
  refreshWikiLinkCacheForNoteIds?: number[]
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
        refreshWikiLinkCacheForNoteIds: input.refreshWikiLinkCacheForNoteIds,
      }
    )
    input.onSuccess()
  } catch (e: unknown) {
    const { fieldErrors } = parseCreateNoteFailure(e)
    input.onFieldErrors({
      newTitle: fieldErrors.newTitle,
      wikidataId: fieldErrors.wikidataId,
    })
  }
}
