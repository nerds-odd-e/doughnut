import type {
  NoteContentCompletion,
  NoteCreationDto,
  NoteTrashDto,
  NoteRealm,
  NoteUpdateTitleDto,
} from "@generated/donut-backend-api"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { containingLocationOf } from "@/routes/containingLocation"
import { refreshSidebarStructuralListings } from "@/components/notes/sidebarStructuralRefresh"
import { realmLeafFolder } from "@/components/notes/useNoteSidebarTree"
import type { Router } from "vue-router"
import NoteEditingHistory from "./NoteEditingHistory"
import type NoteStorage from "./NoteStorage"
import {
  updateTextContentRequest,
  loadNoteRequest,
  createNoteRequest,
  placeNoteRequest,
  trashNoteRequest,
  undoTrashNoteRequest,
  permanentlyDeleteNoteRequest,
  reduceRelationNoteToSourcePropertyRequest,
  uploadNoteImageRequest,
} from "./noteRequests"

export type NoteTrashReferenceHandling = NoteTrashDto["referenceHandling"]

export type NoteTrashOptions = {
  referenceHandling: NoteTrashReferenceHandling
}

export type TitleRenameReferenceHandling = NonNullable<
  NoteUpdateTitleDto["referenceHandling"]
>

function noteReferenceHandlingBody(options: NoteTrashOptions): NoteTrashDto {
  return { referenceHandling: options.referenceHandling }
}

export default class StoredApiCollection {
  constructor(
    private noteEditingHistory: NoteEditingHistory,
    private storage: NoteStorage
  ) {}

  private async updateTextContentWithoutUndo(
    noteId: Donut.ID,
    field: "edit title" | "edit content",
    content: string,
    titleReferenceHandling?: TitleRenameReferenceHandling
  ) {
    const realm = this.storage.refreshNoteRealm(
      await updateTextContentRequest(
        noteId,
        field,
        content,
        titleReferenceHandling
      )
    )
    if (field === "edit title") {
      refreshSidebarStructuralListings()
    }
    return realm
  }

  private async loadNote(noteId: Donut.ID) {
    const noteRealm = await loadNoteRequest(noteId)
    return this.storage.refreshNoteRealm(noteRealm)
  }

  /** Loads a note realm into storage (same as navigating to the note-id route). */
  async loadNoteRealm(noteId: Donut.ID): Promise<NoteRealm> {
    return this.loadNote(noteId)
  }

  getNoteRealmRefAndLoadWhenNeeded(noteId: Donut.ID) {
    const result = this.storage.refOfNoteRealm(noteId)
    if (!result.value && !this.storage.isNotePermanentlyRemoved(noteId)) {
      this.loadNote(noteId)
    }
    return result
  }

  getNoteRealmRef(noteId: Donut.ID) {
    return this.storage.refOfNoteRealm(noteId)
  }

  private async navigateToFocusedNote(router: Router, focus: NoteRealm) {
    refreshSidebarStructuralListings()
    await router.replace(noteShowLocation(focus.id))
    return focus
  }

  /** Refresh storage, sidebar listings, and navigate to this note (replace route). */
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
    const nrwp = await createNoteRequest(notebookId, body)
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

  /** This note no longer exists: drop its cached realm and forget its undo entries. */
  private noteNoLongerExists(noteId: Donut.ID) {
    this.storage.permanentlyRemoveNoteRealm(noteId)
    this.noteEditingHistory.forgetNote(noteId)
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

  /** Sends the one request that places a note at a folder or a notebook root. */
  private async placeNoteAt(
    sourceId: Donut.ID,
    target: { folderId: Donut.ID } | { notebookId: number }
  ): Promise<NoteRealm> {
    const noteRealms = await placeNoteRequest(sourceId, target)
    this.refreshNoteRealms(noteRealms)
    return noteRealms[0]!
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

  /** Persists note content without recording undo (e.g. initial body after create). */
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

  /** Uploads a picture as a file beside the note; the returned realm carries the note's new `image:`. */
  async uploadNoteImage(noteId: Donut.ID, file: File) {
    const noteRealm = await uploadNoteImageRequest(noteId, file)
    if (!noteRealm) return
    this.storage.refreshNoteRealm(noteRealm)
    refreshSidebarStructuralListings()
  }

  /** PATCH note content with current stored body so the backend rebuilds the resolved wiki-link index. */
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
      const noteRealm = await undoTrashNoteRequest(undone.noteId, {
        priorTitle: undone.originalTitle,
        ...(undone.originalFolderId == null
          ? {}
          : { priorFolderId: undone.originalFolderId }),
      })
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
    originalNotebookId: number
  ): Promise<NoteRealm> {
    if (originalFolderId != null) {
      return this.placeNoteAt(noteId, { folderId: originalFolderId })
    }
    return this.placeNoteAt(noteId, { notebookId: originalNotebookId })
  }

  private async undoCreateNote(noteId: Donut.ID): Promise<{
    noteRealm: NoteRealm | undefined
    notebookFallbackId?: number
  }> {
    const cached = this.storage.refOfNoteRealm(noteId).value
    const notebookFallbackId = cached?.notebookRealm.notebook.id
    const trashedRealm = await trashNoteRequest(noteId, {
      referenceHandling: "LEAVE_DEAD_LINKS",
    })
    if (!trashedRealm) throw new Error("Failed to undo create note")
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
    const cachedRealm = this.storage.refOfNoteRealm(noteId).value
    if (!cachedRealm) throw new Error("Cannot trash a note that is not loaded")
    const body = noteReferenceHandlingBody(options)
    const trashedRealm = await trashNoteRequest(noteId, body)
    if (!trashedRealm) return

    const originalFolderId = realmLeafFolder(cachedRealm)?.id ?? null
    this.noteEditingHistory.trashNote(
      noteId,
      cachedRealm.note.noteTopology.title,
      originalFolderId
    )
    await router.replace(containingLocationOf(cachedRealm))
    this.storage.refreshNoteRealm(trashedRealm)
    refreshSidebarStructuralListings()
    return trashedRealm
  }

  /**
   * Permanently deletes a note that is in trash, with everything it owns.
   * Records no undo and drops the note from cache.
   */
  async permanentlyDeleteNote(router: Router, noteId: Donut.ID) {
    const cachedRealm = this.storage.refOfNoteRealm(noteId).value
    if (!cachedRealm) throw new Error("Cannot delete a note that is not loaded")
    const ok = await permanentlyDeleteNoteRequest(noteId)
    if (!ok) return

    this.noteNoLongerExists(noteId)
    await router.replace(containingLocationOf(cachedRealm))
    refreshSidebarStructuralListings()
  }

  /**
   * Permanently reduces a relationship note into a property of its source.
   * Records no undo and drops the relationship note from cache.
   */
  async reduceRelationNoteToSourceProperty(
    router: Router,
    relationNoteId: Donut.ID
  ) {
    const sourceRealm =
      await reduceRelationNoteToSourcePropertyRequest(relationNoteId)
    if (!sourceRealm) return

    this.noteNoLongerExists(relationNoteId)
    await router.replace(noteShowLocation(sourceRealm.id))
    this.storage.refreshNoteRealm(sourceRealm)
    refreshSidebarStructuralListings()
    return sourceRealm
  }

  async moveNoteToFolder(sourceId: Donut.ID, targetFolderId: Donut.ID) {
    const undoPlacement = this.placementUndoForNote(sourceId)
    await this.placeNoteAt(sourceId, { folderId: targetFolderId })

    if (undoPlacement) {
      this.noteEditingHistory.moveNote(sourceId, undoPlacement)
    }
  }

  async moveNoteToNotebookRoot(sourceId: Donut.ID, targetNotebookId: number) {
    const undoPlacement = this.placementUndoForNote(sourceId)
    await this.placeNoteAt(sourceId, { notebookId: targetNotebookId })

    if (undoPlacement) {
      this.noteEditingHistory.moveNote(sourceId, undoPlacement)
    }
  }
}
