import type {
  NoteContentCompletion,
  NoteCreationDto,
  NoteTrashDto,
  NoteRealm,
  NoteUpdateTitleDto,
} from "@generated/donut-backend-api"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { refreshSidebarStructuralListings } from "@/components/notes/sidebarStructuralRefresh"
import type { Router } from "vue-router"
import type NoteUndo from "./noteUndo"
import type NoteStorage from "./NoteStorage"
import {
  updateTextContentRequest,
  loadNoteRequest,
  createNoteRequest,
  placeNoteRequest,
  trashNoteRequest,
  permanentlyDeleteNoteRequest,
  reduceRelationNoteToSourcePropertyRequest,
  uploadNoteImageRequest,
} from "./noteRequests"

export type NoteTrashReferenceHandling = NoteTrashDto["referenceHandling"]

export type TitleRenameReferenceHandling = NonNullable<
  NoteUpdateTitleDto["referenceHandling"]
>

/** The note's folder id, or null at the notebook root. */
function containingFolderId(realm: NoteRealm) {
  return realm.ancestorFolders?.at(-1)?.id ?? null
}

export default class StoredApiCollection {
  constructor(
    private noteUndo: NoteUndo,
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

  /** Loads a note realm into storage (same as navigating to the note-id route). */
  async loadNoteRealm(noteId: Donut.ID): Promise<NoteRealm> {
    const noteRealm = await loadNoteRequest(noteId)
    return this.storage.refreshNoteRealm(noteRealm)
  }

  getNoteRealmRefAndLoadWhenNeeded(noteId: Donut.ID) {
    const result = this.storage.refOfNoteRealm(noteId)
    if (!result.value && !this.storage.isNotePermanentlyRemoved(noteId)) {
      this.loadNoteRealm(noteId)
    }
    return result
  }

  private async navigateToFocusedNote(router: Router, focus: NoteRealm) {
    refreshSidebarStructuralListings()
    await router.replace(noteShowLocation(focus.id))
  }

  /** Refresh storage, sidebar listings, and navigate to this note (replace route). */
  async focusNoteRealm(router: Router, noteRealm: NoteRealm) {
    const focus = this.storage.refreshNoteRealm(noteRealm)
    await this.navigateToFocusedNote(router, focus)
  }

  async createRootNoteAtNotebook(
    router: Router,
    notebookId: number,
    data: NoteCreationDto,
    options?: {
      folderId?: number | null
      skipNavigation?: boolean
    }
  ) {
    const folderId = options?.folderId
    const body: NoteCreationDto =
      folderId != null ? { ...data, folderId } : { ...data }
    const nrwp = await createNoteRequest(notebookId, body)
    const focus = this.storage.refreshNoteRealm(nrwp)
    this.noteUndo.createNote(focus.id)
    if (options?.skipNavigation) {
      refreshSidebarStructuralListings()
    } else {
      await this.navigateToFocusedNote(router, focus)
    }
  }

  private refreshNoteRealms(noteRealms: NoteRealm[]) {
    noteRealms.forEach((n) => this.storage.refreshNoteRealm(n))
    refreshSidebarStructuralListings()
  }

  /** This note no longer exists: drop its cached realm and forget its undo entries. */
  private noteNoLongerExists(noteId: Donut.ID) {
    this.storage.permanentlyRemoveNoteRealm(noteId)
    this.noteUndo.forgetNote(noteId)
  }

  private placementUndoForNote(sourceId: Donut.ID): {
    folderId: number | null
    notebookId: number
  } | null {
    const realm = this.storage.refOfNoteRealm(sourceId).value
    if (!realm) return null
    const notebookId = realm.notebookRealm.notebook.id
    const folderId = containingFolderId(realm)
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
      this.noteUndo.addEditingToUndoHistory(noteId, field, old)
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

    if (!this.storage.refOfNoteRealm(noteId).value) {
      await this.loadNoteRealm(noteId)
    }

    await this.updateTextField(noteId, "edit content", value.content)
  }

  /** Uploads a picture as a file beside the note; the returned realm carries the note's new `image:`. */
  async uploadNoteImage(noteId: Donut.ID, file: File) {
    const noteRealm = await uploadNoteImageRequest(noteId, file)
    this.storage.refreshNoteRealm(noteRealm)
    refreshSidebarStructuralListings()
  }

  async undo(router: Router) {
    await router.push(await this.noteUndo.undoLast())
  }

  async trashNote(noteId: Donut.ID, options: NoteTrashDto) {
    const cachedRealm = this.storage.refOfNoteRealm(noteId).value
    if (!cachedRealm) throw new Error("Cannot trash a note that is not loaded")
    const trashedRealm = await trashNoteRequest(noteId, options)

    const originalFolderId = containingFolderId(cachedRealm)
    this.noteUndo.trashNote(
      noteId,
      cachedRealm.note.noteTopology.title,
      originalFolderId
    )
    return trashedRealm
  }

  /**
   * Permanently deletes a note that is in trash, with everything it owns.
   * Records no undo and drops the note from cache.
   */
  async permanentlyDeleteNote(noteId: Donut.ID) {
    const cachedRealm = this.storage.refOfNoteRealm(noteId).value
    if (!cachedRealm) throw new Error("Cannot delete a note that is not loaded")
    await permanentlyDeleteNoteRequest(noteId)

    this.noteNoLongerExists(noteId)
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

    this.noteNoLongerExists(relationNoteId)
    await router.replace(noteShowLocation(sourceRealm.id))
    this.storage.refreshNoteRealm(sourceRealm)
    refreshSidebarStructuralListings()
  }

  async moveNote(
    sourceId: Donut.ID,
    target: { folderId: Donut.ID } | { notebookId: number }
  ) {
    const undoPlacement = this.placementUndoForNote(sourceId)
    await this.placeNoteAt(sourceId, target)

    if (undoPlacement) {
      this.noteUndo.moveNote(sourceId, undoPlacement)
    }
  }
}
