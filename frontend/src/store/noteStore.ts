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
import NoteUndo from "./noteUndo"
import NoteTextEditing from "./noteTextEditing"
import { StorageImplementation } from "./NoteStorage"
import {
  loadNoteRequest,
  createNoteRequest,
  placeNoteRequest,
  trashNoteRequest,
  permanentlyDeleteNoteRequest,
  reduceRelationNoteToSourcePropertyRequest,
  reifyPropertyRequest,
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

class NoteStore extends StorageImplementation {
  noteUndo = new NoteUndo(this)
  private textEditing = new NoteTextEditing(this)

  peekUndo() {
    return this.noteUndo.peekUndo() ?? null
  }
  discardUndo() {
    this.noteUndo.discardUndo()
  }

  async loadNoteRealm(noteId: Donut.ID): Promise<NoteRealm> {
    const noteRealm = await loadNoteRequest(noteId)
    return this.refreshNoteRealm(noteRealm)
  }

  async getOrLoadNoteRealm(noteId: Donut.ID): Promise<NoteRealm> {
    return this.refOfNoteRealm(noteId).value ?? this.loadNoteRealm(noteId)
  }

  getNoteRealmRefAndLoadWhenNeeded(noteId: Donut.ID) {
    const result = this.refOfNoteRealm(noteId)
    if (!result.value && !this.isNotePermanentlyRemoved(noteId)) {
      this.loadNoteRealm(noteId)
    }
    return result
  }

  private async navigateToFocusedNote(router: Router, focus: NoteRealm) {
    refreshSidebarStructuralListings()
    await router.replace(noteShowLocation(focus.id))
  }

  async focusNoteRealm(router: Router, noteRealm: NoteRealm) {
    const focus = this.refreshNoteRealm(noteRealm)
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
    const focus = this.refreshNoteRealm(nrwp)
    this.noteUndo.createNote(focus.id)
    if (options?.skipNavigation) {
      refreshSidebarStructuralListings()
    } else {
      await this.navigateToFocusedNote(router, focus)
    }
  }

  private noteNoLongerExists(noteId: Donut.ID) {
    this.permanentlyRemoveNoteRealm(noteId)
    this.noteUndo.forgetNote(noteId)
  }

  private placementUndoForNote(sourceId: Donut.ID): {
    folderId: number | null
    notebookId: number
  } | null {
    const realm = this.refOfNoteRealm(sourceId).value
    if (!realm) return null
    const notebookId = realm.notebookRealm.notebook.id
    const folderId = containingFolderId(realm)
    return { folderId, notebookId }
  }

  private async placeNoteAt(
    sourceId: Donut.ID,
    target: { folderId: Donut.ID } | { notebookId: number }
  ): Promise<NoteRealm> {
    const noteRealms = await placeNoteRequest(sourceId, target)
    noteRealms.forEach((realm) => this.refreshNoteRealm(realm))
    refreshSidebarStructuralListings()
    return noteRealms[0]!
  }

  updateTextField(
    noteId: Donut.ID,
    field: "edit title" | "edit content",
    value: string,
    options?: { titleReferenceHandling?: TitleRenameReferenceHandling }
  ) {
    return this.textEditing.updateTextField(noteId, field, value, options)
  }

  setNoteContentWithoutUndo(noteId: Donut.ID, content: string) {
    return this.textEditing.setNoteContentWithoutUndo(noteId, content)
  }

  completeContent(noteId: Donut.ID, value?: NoteContentCompletion) {
    return this.textEditing.completeContent(noteId, value)
  }

  addDictatedText(noteId: Donut.ID, segmentTexts: string[]) {
    return this.textEditing.addDictatedText(noteId, segmentTexts)
  }

  async uploadNoteImage(noteId: Donut.ID, file: File) {
    const noteRealm = await uploadNoteImageRequest(noteId, file)
    this.refreshNoteRealm(noteRealm)
    refreshSidebarStructuralListings()
  }

  async undo(router: Router) {
    await router.push(await this.noteUndo.undoLast())
  }

  async trashNote(noteId: Donut.ID, options: NoteTrashDto) {
    const cachedRealm = this.refOfNoteRealm(noteId).value
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

  async permanentlyDeleteNote(noteId: Donut.ID) {
    const cachedRealm = this.refOfNoteRealm(noteId).value
    if (!cachedRealm) throw new Error("Cannot delete a note that is not loaded")
    await permanentlyDeleteNoteRequest(noteId)

    this.noteNoLongerExists(noteId)
  }

  async reduceRelationNoteToSourceProperty(
    router: Router,
    relationNoteId: Donut.ID
  ) {
    const sourceRealm =
      await reduceRelationNoteToSourcePropertyRequest(relationNoteId)

    this.noteNoLongerExists(relationNoteId)
    await router.replace(noteShowLocation(sourceRealm.id))
    this.refreshNoteRealm(sourceRealm)
    refreshSidebarStructuralListings()
  }

  async reifyProperty(router: Router, noteId: Donut.ID, propertyKey: string) {
    const relationshipRealm = await reifyPropertyRequest(noteId, propertyKey)
    await this.focusNoteRealm(router, relationshipRealm)
    await this.loadNoteRealm(noteId)
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

const noteStore = new NoteStore()

export function useNoteStore() {
  return noteStore
}

export function resetNoteStore() {
  Object.assign(noteStore, new StorageImplementation())
  noteStore.noteUndo = new NoteUndo(noteStore)
  return noteStore
}

export type { NoteStore }
