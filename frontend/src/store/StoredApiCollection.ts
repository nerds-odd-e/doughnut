import type {
  NoteContentCompletion,
  NoteCreationDto,
  NoteTrashDto,
  NoteRealm,
  NoteUpdateTitleDto,
} from "@generated/donut-backend-api"
import {
  RelationController,
  NoteController,
  TextContentController,
  NotebookController,
} from "@generated/donut-backend-api/sdk.gen"
import {
  toOpenApiError,
  setErrorObjectForFieldErrors,
} from "@/managedApi/openApiError"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { refreshSidebarStructuralListings } from "@/components/notes/sidebarStructuralRefresh"
import { realmLeafFolder } from "@/components/notes/useNoteSidebarTree"
import type { Ref } from "vue"
import type { Router } from "vue-router"
import NoteEditingHistory from "./NoteEditingHistory"
import type NoteStorage from "./NoteStorage"

export type NoteTrashReferenceHandling = NoteTrashDto["referenceHandling"]

export type NoteTrashOptions = {
  referenceHandling: NoteTrashReferenceHandling
  sourcePropertyKey?: string
  sourceNoteId?: number
}

export type TitleRenameReferenceHandling = NonNullable<
  NoteUpdateTitleDto["referenceHandling"]
>

function toErrorMessage(error: unknown, fallback: string): string {
  if (typeof error === "string") return error
  return error ? (toOpenApiError(error).message ?? fallback) : fallback
}

function throwStoredApiError(
  error: unknown,
  response: { status?: number } | undefined,
  fallback: string,
  options?: { attachFieldErrors?: boolean }
): never {
  const apiError = new Error(fallback) as Error & {
    body?: unknown
    status?: number
    [key: string]: unknown
  }
  if (error) {
    apiError.body = error
    if (options?.attachFieldErrors) {
      setErrorObjectForFieldErrors(apiError)
    }
    const errorObj = toOpenApiError(error)
    apiError.message = errorObj.message || fallback
    if (response?.status !== undefined) {
      apiError.status = response.status
    } else if (errorObj.errors) {
      apiError.status = 400
    }
  }
  throw apiError
}

export interface StoredApi {
  getNoteRealmRefAndLoadWhenNeeded(noteId: Donut.ID): Ref<NoteRealm | undefined>

  getNoteRealmRef(noteId: Donut.ID): Ref<NoteRealm | undefined>

  /** Loads a note realm into storage (same as navigating to the note-id route). */
  loadNoteRealm(noteId: Donut.ID): Promise<NoteRealm>

  createRootNoteAtNotebook(
    router: Router,
    notebookId: number,
    data: NoteCreationDto,
    options?: {
      folderId?: number | null
      refreshWikiLinkCacheForNoteIds?: number[]
      skipNavigation?: boolean
    }
  ): Promise<NoteRealm>

  /** Refresh storage, sidebar listings, and navigate to this note (replace route). */
  focusNoteRealm(router: Router, noteRealm: NoteRealm): Promise<NoteRealm>

  updateTextField(
    noteId: Donut.ID,
    field: "edit title" | "edit content",
    value: string,
    options?: {
      titleReferenceHandling?: TitleRenameReferenceHandling
    }
  ): Promise<void>

  /** Persists note content without recording undo (e.g. initial body after create). */
  setNoteContentWithoutUndo(noteId: Donut.ID, content: string): Promise<void>

  completeContent(
    noteId: Donut.ID,
    value?: NoteContentCompletion
  ): Promise<void>

  /** PATCH note content with current stored body so the backend rebuilds the resolved wiki-link index. */
  refreshWikiLinkCacheForNote(noteId: Donut.ID): Promise<void>

  undo(router: Router): Promise<NoteRealm | undefined>

  trashNote(
    router: Router,
    noteId: Donut.ID,
    options: NoteTrashOptions
  ): Promise<NoteRealm | undefined>

  moveNoteToFolder(sourceId: Donut.ID, targetFolderId: Donut.ID): Promise<void>

  moveNoteToNotebookRoot(
    sourceId: Donut.ID,
    targetNotebookId: number
  ): Promise<void>
}

function noteReferenceHandlingBody(options: NoteTrashOptions): NoteTrashDto {
  const body: NoteTrashDto = {
    referenceHandling: options.referenceHandling,
  }
  if (options.sourcePropertyKey !== undefined) {
    body.sourcePropertyKey = options.sourcePropertyKey
  }
  return body
}

export default class StoredApiCollection implements StoredApi {
  noteEditingHistory: NoteEditingHistory

  storage: NoteStorage

  constructor(undoHistory: NoteEditingHistory, storage: NoteStorage) {
    this.noteEditingHistory = undoHistory
    this.storage = storage
  }

  // eslint-disable-next-line class-methods-use-this
  private async routerReplaceFocus(router: Router, focusOnNote?: NoteRealm) {
    if (!focusOnNote) {
      return await router.replace({ name: "notebooks" })
    }
    return await router.replace(noteShowLocation(focusOnNote.id))
  }

  private async updateTextContentWithoutUndo(
    noteId: Donut.ID,
    field: "edit title" | "edit content",
    content: string,
    titleReferenceHandling?: TitleRenameReferenceHandling
  ) {
    const realm = this.storage.refreshNoteRealm(
      await this.callUpdateApi(noteId, field, content, titleReferenceHandling)
    )
    if (field === "edit title") {
      refreshSidebarStructuralListings()
    }
    return realm
  }

  private async callUpdateApi(
    noteId: Donut.ID,
    field: "edit title" | "edit content",
    content: string,
    titleReferenceHandling?: TitleRenameReferenceHandling
  ) {
    if (field === "edit title") {
      const body: NoteUpdateTitleDto = {
        newTitle: content,
        ...(titleReferenceHandling != null
          ? { referenceHandling: titleReferenceHandling }
          : {}),
      }
      const { data, error } = await apiCallWithLoading(() =>
        TextContentController.updateNoteTitle({
          path: { note: noteId },
          body,
        })
      )
      if (error || !data) {
        const fieldErrors = toOpenApiError(error).errors
        if (fieldErrors?.newTitle) {
          throw Object.assign(
            new Error(toErrorMessage(error, "Failed to update note title")),
            { title: fieldErrors.newTitle }
          )
        }
        throw new Error(toErrorMessage(error, "Failed to update note title"))
      }
      return data
    }
    const { data, error } = await apiCallWithLoading(() =>
      TextContentController.updateNoteContent({
        path: { note: noteId },
        body: {
          content,
        },
      })
    )
    if (error || !data) {
      throw new Error(toErrorMessage(error, "Failed to update note content"))
    }
    return data
  }

  private async loadNote(noteId: Donut.ID) {
    const { data: noteRealm, error } = await apiCallWithLoading(() =>
      NoteController.showNote({
        path: { note: noteId },
      })
    )
    if (error || !noteRealm) {
      throw new Error(toErrorMessage(error, "Failed to load note"))
    }
    return this.storage.refreshNoteRealm(noteRealm)
  }

  async loadNoteRealm(noteId: Donut.ID): Promise<NoteRealm> {
    return this.loadNote(noteId)
  }

  getNoteRealmRefAndLoadWhenNeeded(noteId: Donut.ID) {
    const result = this.storage.refOfNoteRealm(noteId)
    if (!result.value) this.loadNote(noteId)
    return result
  }

  getNoteRealmRef(noteId: Donut.ID) {
    return this.storage.refOfNoteRealm(noteId)
  }

  private async navigateToFocusedNote(router: Router, focus: NoteRealm) {
    refreshSidebarStructuralListings()
    await this.routerReplaceFocus(router, focus)
    return focus
  }

  async focusNoteRealm(router: Router, noteRealm: NoteRealm) {
    const focus = this.storage.refreshNoteRealm(noteRealm)
    return this.navigateToFocusedNote(router, focus)
  }

  async createRootNoteAtNotebook(
    router: Router,
    notebookId: number,
    data: NoteCreationDto,
    options?: {
      folderId?: number | null
      refreshWikiLinkCacheForNoteIds?: number[]
      skipNavigation?: boolean
    }
  ) {
    const folderId = options?.folderId
    const refreshWikiLinkCacheForNoteIds =
      options?.refreshWikiLinkCacheForNoteIds
    const body: NoteCreationDto =
      folderId != null ? { ...data, folderId } : { ...data }
    const result = await apiCallWithLoading(() =>
      NotebookController.createNoteAtNotebookRoot({
        path: { notebook: notebookId },
        body,
      })
    )
    const { data: nrwp, error, response } = result
    if (error || !nrwp) {
      throwStoredApiError(error, response, "Failed to create note", {
        attachFieldErrors: true,
      })
    }
    const focus = this.storage.refreshNoteRealm(nrwp)
    this.noteEditingHistory.createNote(focus.id)
    if (refreshWikiLinkCacheForNoteIds) {
      for (const id of refreshWikiLinkCacheForNoteIds) {
        await this.refreshWikiLinkCacheForNote(id)
      }
    }
    if (options?.skipNavigation) {
      refreshSidebarStructuralListings()
      return focus
    }
    return this.navigateToFocusedNote(router, focus)
  }

  private refreshNoteRealms(noteRealms: NoteRealm[]) {
    noteRealms.forEach((n) => this.storage.refreshNoteRealm(n))
    refreshSidebarStructuralListings()
  }

  private placementUndoForNote(sourceId: Donut.ID): {
    folderId: number | null
    notebookId: number
  } | null {
    const realm = this.storage.refOfNoteRealm(sourceId).value
    if (!realm?.note) return null
    const notebookId = realm.notebookRealm.notebook.id
    if (notebookId == null) return null
    const folderId = realmLeafFolder(realm)?.id ?? null
    return { folderId, notebookId }
  }

  async updateTextField(
    noteId: Donut.ID,
    field: "edit title" | "edit content",
    value: string,
    options?: { titleReferenceHandling?: TitleRenameReferenceHandling }
  ) {
    const currentNote = this.storage.refOfNoteRealm(noteId).value?.note
    if (currentNote) {
      const old =
        field === "edit title"
          ? currentNote.noteTopology.title
          : (currentNote.content ?? "")
      if (old === value) {
        return
      }
      this.noteEditingHistory.addEditingToUndoHistory(noteId, field, old)
    }
    await this.updateTextContentWithoutUndo(
      noteId,
      field,
      value,
      field === "edit title" ? options?.titleReferenceHandling : undefined
    )
  }

  async setNoteContentWithoutUndo(noteId: Donut.ID, content: string) {
    await this.updateTextContentWithoutUndo(noteId, "edit content", content)
  }

  async completeContent(noteId: Donut.ID, value?: NoteContentCompletion) {
    if (!value || !value.content) return

    let currentNote = this.storage.refOfNoteRealm(noteId).value?.note
    if (!currentNote) {
      currentNote = (await this.loadNote(noteId)).note
    }

    await this.updateTextField(noteId, "edit content", value.content)
  }

  async refreshWikiLinkCacheForNote(noteId: Donut.ID): Promise<void> {
    let realm = this.storage.refOfNoteRealm(noteId).value
    if (!realm?.note) {
      realm = await this.loadNote(noteId)
    }
    const content = realm.note.content ?? ""
    await this.updateTextContentWithoutUndo(noteId, "edit content", content)
  }

  private async undoInner(): Promise<{
    noteRealm: NoteRealm | undefined
    notebookFallbackId?: number
  }> {
    const undone = this.noteEditingHistory.peekUndo()
    if (!undone) throw new Error("undo history is empty")
    if (undone.type === "trash note") {
      const { data: noteRealm, error } = await apiCallWithLoading(() =>
        NoteController.undoTrashNote({
          path: { note: undone.noteId },
          body: {
            priorTitle: undone.originalTitle,
            ...(undone.originalFolderId == null
              ? {}
              : { priorFolderId: undone.originalFolderId }),
          },
        })
      )
      if (error || !noteRealm) {
        throw new Error(toErrorMessage(error, "Failed to undo trash note"))
      }
      this.noteEditingHistory.popUndoHistory()
      refreshSidebarStructuralListings()
      return { noteRealm: this.storage.refreshNoteRealm(noteRealm) }
    }
    this.noteEditingHistory.popUndoHistory()
    if (undone.type === "edit title" || undone.type === "edit content") {
      const noteRealm = await this.updateTextContentWithoutUndo(
        undone.noteId,
        undone.type,
        undone.textContent ?? ""
      )
      return { noteRealm }
    }
    if (undone.type === "create note") {
      return this.undoCreateNote(undone.noteId)
    }
    if (undone.type === "move note") {
      const noteRealm = await this.undoMoveNote(
        undone.noteId,
        undone.originalFolderId ?? null,
        undone.originalNotebookId
      )
      return { noteRealm }
    }
    return { noteRealm: undefined }
  }

  private async undoMoveNote(
    noteId: Donut.ID,
    originalFolderId: Donut.ID | null,
    originalNotebookId?: number
  ): Promise<NoteRealm> {
    if (originalFolderId != null) {
      const { data: noteRealms, error } = await apiCallWithLoading(() =>
        RelationController.moveNoteToFolder({
          path: { sourceNote: noteId, targetFolder: originalFolderId },
        })
      )
      if (error || !noteRealms) {
        throw new Error(toErrorMessage(error, "Failed to move note"))
      }
      this.refreshNoteRealms(noteRealms)
      return noteRealms[0]!
    }
    if (originalNotebookId != null) {
      const { data: noteRealms, error } = await apiCallWithLoading(() =>
        RelationController.moveNoteToNotebookRootInNotebook({
          path: { sourceNote: noteId, targetNotebook: originalNotebookId },
        })
      )
      if (error || !noteRealms) {
        throw new Error(toErrorMessage(error, "Failed to move note"))
      }
      this.refreshNoteRealms(noteRealms)
      return noteRealms[0]!
    }
    const { data: noteRealms, error } = await apiCallWithLoading(() =>
      RelationController.moveNoteToNotebookRoot({
        path: { sourceNote: noteId },
      })
    )
    if (error || !noteRealms) {
      throw new Error(toErrorMessage(error, "Failed to move note"))
    }
    this.refreshNoteRealms(noteRealms)
    return noteRealms[0]!
  }

  private async undoCreateNote(noteId: Donut.ID): Promise<{
    noteRealm: NoteRealm | undefined
    notebookFallbackId?: number
  }> {
    const cached = this.storage.refOfNoteRealm(noteId).value
    const notebookFallbackId = cached?.notebookRealm.notebook.id
    const { data: trashedRealm, error } = await apiCallWithLoading(() =>
      NoteController.trashNote({
        path: { note: noteId },
        body: { referenceHandling: "LEAVE_DEAD_LINKS" },
      })
    )
    if (error || !trashedRealm) {
      throw new Error(toErrorMessage(error, "Failed to undo create note"))
    }
    this.storage.removeNoteRealm(noteId)
    return {
      noteRealm: undefined,
      ...(notebookFallbackId !== undefined ? { notebookFallbackId } : {}),
    }
  }

  async undo(router: Router) {
    const { noteRealm, notebookFallbackId } = await this.undoInner()
    if (!noteRealm) {
      if (notebookFallbackId !== undefined) {
        await router.push({
          name: "notebookPage",
          params: { notebookId: notebookFallbackId },
        })
      } else {
        await router.push({ name: "notebooks" })
      }
      return
    }
    await router.push(noteShowLocation(noteRealm.id))
    return noteRealm
  }

  async trashNote(router: Router, noteId: Donut.ID, options: NoteTrashOptions) {
    const { referenceHandling, sourceNoteId } = options
    const cachedRealm = this.storage.refOfNoteRealm(noteId).value
    if (!cachedRealm) throw new Error("Cannot trash a note that is not loaded")
    const body = noteReferenceHandlingBody(options)
    const { data: trashedRealm, error } = await apiCallWithLoading(() =>
      NoteController.trashNote({
        path: { note: noteId },
        body,
      })
    )
    if (error || !trashedRealm) return

    const notebookId = cachedRealm.notebookRealm.notebook.id
    const originalFolderId = realmLeafFolder(cachedRealm)?.id ?? null
    this.noteEditingHistory.trashNote(
      noteId,
      cachedRealm.note.noteTopology.title,
      originalFolderId
    )
    const destination =
      referenceHandling === "REDUCE_TO_SOURCE_PROPERTY" &&
      sourceNoteId !== undefined
        ? noteShowLocation(sourceNoteId)
        : originalFolderId != null
          ? {
              name: "folderPage",
              params: { notebookId, folderId: originalFolderId },
            }
          : { name: "notebookPage", params: { notebookId } }
    await router.replace(destination)
    this.storage.refreshNoteRealm(trashedRealm)
    refreshSidebarStructuralListings()
    return trashedRealm
  }

  async moveNoteToFolder(sourceId: Donut.ID, targetFolderId: Donut.ID) {
    const undoPlacement = this.placementUndoForNote(sourceId)

    const {
      data: noteRealms,
      error,
      response,
    } = await apiCallWithLoading(() =>
      RelationController.moveNoteToFolder({
        path: {
          sourceNote: sourceId,
          targetFolder: targetFolderId,
        },
      })
    )
    if (error || !noteRealms) {
      throwStoredApiError(error, response, "Failed to move note")
    }
    this.refreshNoteRealms(noteRealms)

    if (undoPlacement) {
      this.noteEditingHistory.moveNote(sourceId, undoPlacement)
    }
  }

  async moveNoteToNotebookRoot(sourceId: Donut.ID, targetNotebookId: number) {
    const undoPlacement = this.placementUndoForNote(sourceId)

    const {
      data: noteRealms,
      error,
      response,
    } = await apiCallWithLoading(() =>
      RelationController.moveNoteToNotebookRootInNotebook({
        path: {
          sourceNote: sourceId,
          targetNotebook: targetNotebookId,
        },
      })
    )
    if (error || !noteRealms) {
      throwStoredApiError(error, response, "Failed to move note")
    }
    this.refreshNoteRealms(noteRealms)

    if (undoPlacement) {
      this.noteEditingHistory.moveNote(sourceId, undoPlacement)
    }
  }
}
