import type {
  NoteContentCompletion,
  NoteDeleteDto,
  NoteRealm,
} from "@generated/doughnut-backend-api"
import type { NoteCreationDto } from "@generated/doughnut-backend-api"
import {
  RelationController,
  NoteController,
  TextContentController,
  NotebookController,
} from "@generated/doughnut-backend-api/sdk.gen"
import {
  toOpenApiError,
  setErrorObjectForFieldErrors,
} from "@/managedApi/openApiError"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { refreshSidebarStructuralListings } from "@/components/notes/sidebarStructuralRefresh"
import type { Ref } from "vue"
import type { Router } from "vue-router"
import NoteEditingHistory from "./NoteEditingHistory"
import type NoteStorage from "./NoteStorage"

export type NoteDeleteReferenceHandling = NoteDeleteDto["referenceHandling"]

function toErrorMessage(error: unknown, fallback: string): string {
  if (typeof error === "string") return error
  return error ? (toOpenApiError(error).message ?? fallback) : fallback
}

export interface StoredApi {
  getNoteRealmRefAndReloadPosition(
    noteId: Doughnut.ID
  ): Ref<NoteRealm | undefined>

  getNoteRealmRefAndLoadWhenNeeded(
    noteId: Doughnut.ID
  ): Ref<NoteRealm | undefined>

  getNoteRealmRef(noteId: Doughnut.ID): Ref<NoteRealm | undefined>

  /** Loads a note realm into storage (same as navigating to the note-id route). */
  loadNoteRealm(noteId: Doughnut.ID): Promise<NoteRealm>

  createRootNoteAtNotebook(
    router: Router,
    notebookId: number,
    data: NoteCreationDto,
    options?: {
      folderId?: number | null
      refreshWikiTitleCacheForNoteIds?: number[]
      /** When true, refresh storage but do not navigate (e.g. relationship note creation). */
      skipRouterReplace?: boolean
    }
  ): Promise<NoteRealm>

  /** PATCH undo-delete for a soft-deleted note; refreshes storage and navigates to the note. */
  restoreDeletedNote(router: Router, noteId: Doughnut.ID): Promise<NoteRealm>

  updateTextField(
    noteId: Doughnut.ID,
    field: "edit title" | "edit content",
    value: string
  ): Promise<void>

  /** Persists note content without recording undo (e.g. initial body after create). */
  setNoteContentWithoutUndo(noteId: Doughnut.ID, content: string): Promise<void>

  completeContent(
    noteId: Doughnut.ID,
    value?: NoteContentCompletion
  ): Promise<void>

  /** PATCH note content with current stored body so the backend rebuilds wiki title cache. */
  refreshWikiLinkCacheForNote(noteId: Doughnut.ID): Promise<void>

  undo(router: Router): Promise<NoteRealm | undefined>

  deleteNote(
    router: Router,
    noteId: Doughnut.ID,
    referenceHandling: NoteDeleteReferenceHandling
  ): Promise<NoteRealm | undefined>

  moveNoteToFolder(
    sourceId: Doughnut.ID,
    targetFolderId: Doughnut.ID
  ): Promise<void>

  moveNoteToNotebookRoot(
    sourceId: Doughnut.ID,
    targetNotebookId: number
  ): Promise<void>
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
    noteId: Doughnut.ID,
    field: "edit title" | "edit content",
    content: string
  ) {
    const realm = this.storage.refreshNoteRealm(
      await this.callUpdateApi(noteId, field, content)
    )
    if (field === "edit title") {
      refreshSidebarStructuralListings()
    }
    return realm
  }

  private async callUpdateApi(
    noteId: Doughnut.ID,
    field: "edit title" | "edit content",
    content: string
  ) {
    if (field === "edit title") {
      const { data, error } = await apiCallWithLoading(() =>
        TextContentController.updateNoteTitle({
          path: { note: noteId },
          body: {
            newTitle: content,
          },
        })
      )
      if (error || !data) {
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

  private async loadNote(noteId: Doughnut.ID) {
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

  async loadNoteRealm(noteId: Doughnut.ID): Promise<NoteRealm> {
    return this.loadNote(noteId)
  }

  getNoteRealmRefAndReloadPosition(noteId: Doughnut.ID) {
    this.loadNote(noteId)
    return this.storage.refOfNoteRealm(noteId)
  }

  getNoteRealmRefAndLoadWhenNeeded(noteId: Doughnut.ID) {
    const result = this.storage.refOfNoteRealm(noteId)
    if (!result.value) this.loadNote(noteId)
    return result
  }

  getNoteRealmRef(noteId: Doughnut.ID) {
    return this.storage.refOfNoteRealm(noteId)
  }

  async createRootNoteAtNotebook(
    router: Router,
    notebookId: number,
    data: NoteCreationDto,
    options?: {
      folderId?: number | null
      refreshWikiTitleCacheForNoteIds?: number[]
      skipRouterReplace?: boolean
    }
  ) {
    const folderId = options?.folderId
    const refreshWikiTitleCacheForNoteIds =
      options?.refreshWikiTitleCacheForNoteIds
    const skipRouterReplace = options?.skipRouterReplace
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
      const apiError = new Error("Failed to create note") as Error & {
        body?: unknown
        status?: number
        [key: string]: unknown
      }
      if (error) {
        apiError.body = error
        setErrorObjectForFieldErrors(apiError)
        const errorObj = toOpenApiError(error)
        apiError.message = errorObj.message || "Failed to create note"
        if (response?.status !== undefined) {
          apiError.status = response.status
        } else if (errorObj.errors) {
          apiError.status = 400
        }
      }
      throw apiError
    }
    const focus = this.storage.refreshNoteRealm(nrwp)
    refreshSidebarStructuralListings()
    this.noteEditingHistory.createNote(focus.id)
    if (refreshWikiTitleCacheForNoteIds) {
      for (const id of refreshWikiTitleCacheForNoteIds) {
        await this.refreshWikiLinkCacheForNote(id)
      }
    }
    if (!skipRouterReplace) {
      await this.routerReplaceFocus(router, focus)
    }
    return focus
  }

  async restoreDeletedNote(router: Router, noteId: Doughnut.ID) {
    const { data: noteRealm, error } = await apiCallWithLoading(() =>
      NoteController.undoDeleteNote({
        path: { note: noteId },
      })
    )
    if (error || !noteRealm) {
      throw new Error(toErrorMessage(error, "Failed to restore note"))
    }
    const focus = this.storage.refreshNoteRealm(noteRealm)
    refreshSidebarStructuralListings()
    await this.routerReplaceFocus(router, focus)
    return focus
  }

  private refreshNoteRealms(noteRealms: NoteRealm[]) {
    noteRealms.forEach((n) => this.storage.refreshNoteRealm(n))
    refreshSidebarStructuralListings()
  }

  private placementUndoForNote(sourceId: Doughnut.ID): {
    folderId: number | null
    notebookId: number
  } | null {
    const realm = this.storage.refOfNoteRealm(sourceId).value
    if (!realm?.note) return null
    const notebookId = realm.notebookView.notebook.id
    if (notebookId == null) return null
    const folderId = realm.note.noteTopology.folderId ?? null
    return { folderId, notebookId }
  }

  async updateTextField(
    noteId: Doughnut.ID,
    field: "edit title" | "edit content",
    value: string
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
    await this.updateTextContentWithoutUndo(noteId, field, value)
  }

  async setNoteContentWithoutUndo(noteId: Doughnut.ID, content: string) {
    await this.updateTextContentWithoutUndo(noteId, "edit content", content)
  }

  async completeContent(noteId: Doughnut.ID, value?: NoteContentCompletion) {
    if (!value || !value.content) return

    let currentNote = this.storage.refOfNoteRealm(noteId).value?.note
    if (!currentNote) {
      currentNote = (await this.loadNote(noteId)).note
    }

    await this.updateTextField(noteId, "edit content", value.content)
  }

  async refreshWikiLinkCacheForNote(noteId: Doughnut.ID): Promise<void> {
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
    const { data: noteRealm, error } = await apiCallWithLoading(() =>
      NoteController.undoDeleteNote({
        path: { note: undone.noteId },
      })
    )
    if (error || !noteRealm) {
      throw new Error(toErrorMessage(error, "Failed to undo delete note"))
    }
    return { noteRealm }
  }

  private async undoMoveNote(
    noteId: Doughnut.ID,
    originalFolderId: Doughnut.ID | null,
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

  private async undoCreateNote(noteId: Doughnut.ID): Promise<{
    noteRealm: NoteRealm | undefined
    notebookFallbackId?: number
  }> {
    const cached = this.storage.refOfNoteRealm(noteId).value
    const notebookFallbackId = cached?.notebookView.notebook.id
    const { data: res, error } = await apiCallWithLoading(() =>
      NoteController.deleteNote({
        path: { note: noteId },
        body: { referenceHandling: "LEAVE_DEAD_LINKS" },
      })
    )
    if (error || !res) {
      throw new Error(toErrorMessage(error, "Failed to undo create note"))
    }
    this.storage.removeNoteRealm(noteId)
    if (res.length === 0) {
      return {
        noteRealm: undefined,
        ...(notebookFallbackId !== undefined ? { notebookFallbackId } : {}),
      }
    }
    return { noteRealm: this.storage.refreshNoteRealm(res[0]!) }
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

  async deleteNote(
    router: Router,
    noteId: Doughnut.ID,
    referenceHandling: NoteDeleteReferenceHandling
  ) {
    const cachedRealm = this.storage.refOfNoteRealm(noteId).value
    const { data: res, error } = await apiCallWithLoading(() =>
      NoteController.deleteNote({
        path: { note: noteId },
        body: { referenceHandling },
      })
    )
    if (error || !res) {
      throw new Error(toErrorMessage(error, "Failed to delete note"))
    }
    this.noteEditingHistory.deleteNote(noteId)
    this.storage.removeNoteRealm(noteId)
    let notebookId = cachedRealm?.notebookView.notebook.id
    let focusRealm: NoteRealm | undefined
    if (res.length > 0) {
      focusRealm = this.storage.refreshNoteRealm(res[0]!)
      notebookId = notebookId ?? focusRealm.notebookView.notebook.id
    }
    if (notebookId !== undefined) {
      await router.replace({
        name: "notebookPage",
        params: { notebookId },
      })
      return focusRealm
    }
    await this.routerReplaceFocus(router)
    return focusRealm
  }

  async moveNoteToFolder(sourceId: Doughnut.ID, targetFolderId: Doughnut.ID) {
    const undoPlacement = this.placementUndoForNote(sourceId)

    const { data: noteRealms, error } = await apiCallWithLoading(() =>
      RelationController.moveNoteToFolder({
        path: {
          sourceNote: sourceId,
          targetFolder: targetFolderId,
        },
      })
    )
    if (error || !noteRealms) {
      throw new Error(toErrorMessage(error, "Failed to move note"))
    }
    this.refreshNoteRealms(noteRealms)

    if (undoPlacement) {
      this.noteEditingHistory.moveNote(sourceId, undoPlacement)
    }
  }

  async moveNoteToNotebookRoot(
    sourceId: Doughnut.ID,
    targetNotebookId: number
  ) {
    const undoPlacement = this.placementUndoForNote(sourceId)

    const { data: noteRealms, error } = await apiCallWithLoading(() =>
      RelationController.moveNoteToNotebookRootInNotebook({
        path: {
          sourceNote: sourceId,
          targetNotebook: targetNotebookId,
        },
      })
    )
    if (error || !noteRealms) {
      throw new Error(toErrorMessage(error, "Failed to move note"))
    }
    this.refreshNoteRealms(noteRealms)

    if (undoPlacement) {
      this.noteEditingHistory.moveNote(sourceId, undoPlacement)
    }
  }
}
