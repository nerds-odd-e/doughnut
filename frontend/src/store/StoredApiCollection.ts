import type {
  FolderCreationRequest,
  FolderListing,
  FolderMoveRequest,
  Folder,
  NoteDetailsCompletion,
  NoteRealm,
  WikidataAssociationCreation,
} from "@generated/doughnut-backend-api"
import type {
  RelationshipCreation,
  NoteCreationDto,
} from "@generated/doughnut-backend-api"
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

  loadNotebookRootNotes(notebookId: number): Promise<FolderListing>

  loadFolderListing(
    notebookId: number,
    folderId: number
  ): Promise<FolderListing>

  createFolder(notebookId: number, body: FolderCreationRequest): Promise<Folder>

  moveFolder(
    notebookId: number,
    folderId: number,
    newParentFolderId: number | null
  ): Promise<Folder>

  dissolveFolder(notebookId: number, folderId: number): Promise<void>

  createRootNoteAtNotebook(
    router: Router,
    notebookId: number,
    data: NoteCreationDto,
    options?: {
      folderId?: number | null
      refreshWikiTitleCacheForNoteIds?: number[]
    }
  ): Promise<NoteRealm>

  /** PATCH undo-delete for a soft-deleted note; refreshes storage and navigates to the note. */
  restoreDeletedNote(router: Router, noteId: Doughnut.ID): Promise<NoteRealm>

  createRelationship(
    sourceId: Doughnut.ID,
    targetId: Doughnut.ID,
    data: RelationshipCreation
  ): Promise<void>

  updateTextField(
    noteId: Doughnut.ID,
    field: "edit title" | "edit details",
    value: string
  ): Promise<void>

  completeDetails(
    noteId: Doughnut.ID,
    value?: NoteDetailsCompletion
  ): Promise<void>

  /** PATCH note details with current stored body so the backend rebuilds wiki title cache. */
  refreshWikiLinkCacheForNote(noteId: Doughnut.ID): Promise<void>

  updateWikidataId(
    noteId: Doughnut.ID,
    data: WikidataAssociationCreation
  ): Promise<NoteRealm>

  undo(router: Router): Promise<NoteRealm | undefined>

  deleteNote(
    router: Router,
    noteId: Doughnut.ID
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
    field: "edit title" | "edit details",
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
    field: "edit title" | "edit details",
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
      TextContentController.updateNoteDetails({
        path: { note: noteId },
        body: {
          details: content,
        },
      })
    )
    if (error || !data) {
      throw new Error(toErrorMessage(error, "Failed to update note details"))
    }
    return data
  }

  async updateWikidataId(
    noteId: Doughnut.ID,
    data: WikidataAssociationCreation
  ): Promise<NoteRealm> {
    const { data: noteRealm, error } = await apiCallWithLoading(() =>
      NoteController.updateWikidataId({
        path: { note: noteId },
        body: data,
      })
    )
    if (error || !noteRealm) {
      const apiError = new Error("Failed to update Wikidata ID") as Error & {
        body?: unknown
        status?: number
        [key: string]: unknown
      }
      if (error) {
        apiError.body = error
        setErrorObjectForFieldErrors(apiError)
        const errorObj = toOpenApiError(error)
        apiError.message = errorObj.message || "Failed to update Wikidata ID"
        if (errorObj.errors) {
          apiError.status = 400
        }
      }
      throw apiError
    }
    return this.storage.refreshNoteRealm(noteRealm)
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

  async loadNotebookRootNotes(notebookId: number): Promise<FolderListing> {
    const { data, error } = await apiCallWithLoading(() =>
      NotebookController.listNotebookRootNotes({
        path: { notebook: notebookId },
      })
    )
    if (error || !data) {
      throw new Error(
        toErrorMessage(error, "Failed to load notebook root notes")
      )
    }
    return data
  }

  async loadFolderListing(
    notebookId: number,
    folderId: number
  ): Promise<FolderListing> {
    const { data, error } = await apiCallWithLoading(() =>
      NotebookController.listFolderListing({
        path: { notebook: notebookId, folder: folderId },
      })
    )
    if (error || !data) {
      throw new Error(toErrorMessage(error, "Failed to load folder listing"))
    }
    return data
  }

  async createFolder(notebookId: number, body: FolderCreationRequest) {
    const payload: FolderCreationRequest =
      body.underFolderId != null
        ? { name: body.name, underFolderId: body.underFolderId }
        : body.underNoteId != null
          ? { name: body.name, underNoteId: body.underNoteId }
          : { name: body.name }
    const { data, error } = await apiCallWithLoading(() =>
      NotebookController.createFolder({
        path: { notebook: notebookId },
        body: payload,
      })
    )
    if (error || !data) {
      throw new Error(toErrorMessage(error, "Failed to create folder"))
    }
    refreshSidebarStructuralListings()
    return data
  }

  async moveFolder(
    notebookId: number,
    folderId: number,
    newParentFolderId: number | null
  ): Promise<Folder> {
    const body: FolderMoveRequest =
      newParentFolderId == null ? {} : { newParentFolderId }
    const { data, error } = await apiCallWithLoading(() =>
      NotebookController.moveFolder({
        path: { notebook: notebookId, folder: folderId },
        body,
      })
    )
    if (error || !data) {
      throw new Error(toErrorMessage(error, "Failed to move folder"))
    }
    refreshSidebarStructuralListings()
    return data
  }

  async dissolveFolder(notebookId: number, folderId: number): Promise<void> {
    const { error } = await apiCallWithLoading(() =>
      NotebookController.dissolveFolder({
        path: { notebook: notebookId, folder: folderId },
      })
    )
    if (error) {
      throw new Error(toErrorMessage(error, "Failed to dissolve folder"))
    }
    refreshSidebarStructuralListings()
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
    }
  ) {
    const folderId = options?.folderId
    const refreshWikiTitleCacheForNoteIds =
      options?.refreshWikiTitleCacheForNoteIds
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
    await this.routerReplaceFocus(router, focus)
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

  async createRelationship(
    sourceId: Doughnut.ID,
    targetId: Doughnut.ID,
    data: RelationshipCreation
  ) {
    const { data: noteRealms, error } = await apiCallWithLoading(() =>
      RelationController.addRelationshipFinalize({
        path: {
          sourceNote: sourceId,
          targetNote: targetId,
        },
        body: data,
      })
    )
    if (error || !noteRealms) {
      throw new Error(toErrorMessage(error, "Failed to create relationship"))
    }
    this.refreshNoteRealms(noteRealms)
    refreshSidebarStructuralListings()
    const relationNote = noteRealms[0]
    if (relationNote) {
      this.noteEditingHistory.createNote(relationNote.id)
    }
  }

  private refreshNoteRealms(noteRealms: NoteRealm[]) {
    noteRealms.forEach((n) => this.storage.refreshNoteRealm(n))
  }

  private placementUndoForNote(sourceId: Doughnut.ID): {
    folderId: number | null
    notebookId: number
  } | null {
    const realm = this.storage.refOfNoteRealm(sourceId).value
    if (!realm?.note) return null
    const notebookId = realm.notebookView?.notebook?.id
    if (notebookId == null) return null
    const folderId = realm.note.noteTopology.folderId ?? null
    return { folderId, notebookId }
  }

  async updateTextField(
    noteId: Doughnut.ID,
    field: "edit title" | "edit details",
    value: string
  ) {
    const currentNote = this.storage.refOfNoteRealm(noteId).value?.note
    if (currentNote) {
      const old =
        field === "edit title"
          ? currentNote.noteTopology.title
          : (currentNote.details ?? "")
      if (old === value) {
        return
      }
      this.noteEditingHistory.addEditingToUndoHistory(noteId, field, old)
    }
    await this.updateTextContentWithoutUndo(noteId, field, value)
  }

  async completeDetails(noteId: Doughnut.ID, value?: NoteDetailsCompletion) {
    if (!value || !value.details) return

    let currentNote = this.storage.refOfNoteRealm(noteId).value?.note
    if (!currentNote) {
      currentNote = (await this.loadNote(noteId)).note
    }

    await this.updateTextField(noteId, "edit details", value.details)
  }

  async refreshWikiLinkCacheForNote(noteId: Doughnut.ID): Promise<void> {
    let realm = this.storage.refOfNoteRealm(noteId).value
    if (!realm?.note) {
      realm = await this.loadNote(noteId)
    }
    const details = realm.note.details ?? ""
    await this.updateTextContentWithoutUndo(noteId, "edit details", details)
  }

  private async undoInner(): Promise<{
    noteRealm: NoteRealm | undefined
    notebookFallbackId?: number
  }> {
    const undone = this.noteEditingHistory.peekUndo()
    if (!undone) throw new Error("undo history is empty")
    this.noteEditingHistory.popUndoHistory()
    if (undone.type === "edit title" || undone.type === "edit details") {
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
    const notebookFallbackId = cached?.notebookView?.notebook?.id
    const { data: res, error } = await apiCallWithLoading(() =>
      NoteController.deleteNote({
        path: { note: noteId },
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

  async deleteNote(router: Router, noteId: Doughnut.ID) {
    const cachedRealm = this.storage.refOfNoteRealm(noteId).value
    const { data: res, error } = await apiCallWithLoading(() =>
      NoteController.deleteNote({
        path: { note: noteId },
      })
    )
    if (error || !res) {
      throw new Error(toErrorMessage(error, "Failed to delete note"))
    }
    this.noteEditingHistory.deleteNote(noteId)
    this.storage.removeNoteRealm(noteId)
    let notebookId = cachedRealm?.notebookView?.notebook?.id
    let focusRealm: NoteRealm | undefined
    if (res.length > 0) {
      focusRealm = this.storage.refreshNoteRealm(res[0]!)
      notebookId = notebookId ?? focusRealm.notebookView?.notebook?.id
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
