import type {
  NoteDetailsCompletion,
  NoteMoveDTO,
  NoteRealm,
  WikidataAssociationCreation,
} from "@generated/backend"
import type { LinkCreation, NoteCreationDTO } from "@generated/backend"
import ManagedApi from "@/managedApi/ManagedApi"
import type { Ref } from "vue"
import type { Router } from "vue-router"
import NoteEditingHistory from "./NoteEditingHistory"
import type NoteStorage from "./NoteStorage"

export interface StoredApi {
  getNoteRealmRefAndReloadPosition(
    noteId: Doughnut.ID
  ): Ref<NoteRealm | undefined>

  getNoteRealmRefAndLoadWhenNeeded(
    noteId: Doughnut.ID
  ): Ref<NoteRealm | undefined>

  getNoteRealmRef(noteId: Doughnut.ID): Ref<NoteRealm | undefined>

  createNote(
    router: Router,
    parentId: Doughnut.ID,
    data: NoteCreationDTO
  ): Promise<NoteRealm>

  createNoteAfter(
    router: Router,
    referenceId: Doughnut.ID,
    data: NoteCreationDTO
  ): Promise<NoteRealm>

  createLink(
    sourceId: Doughnut.ID,
    targetId: Doughnut.ID,
    data: LinkCreation
  ): Promise<void>

  updateLink(linkId: Doughnut.ID, data: LinkCreation): Promise<void>

  moveAfter(
    noteId: number,
    targetNoteId: number,
    dropMode: "after" | "asFirstChild"
  ): Promise<NoteRealm[]>

  updateTextField(
    noteId: Doughnut.ID,
    field: "edit title" | "edit details",
    value: string
  ): Promise<void>

  completeDetails(
    noteId: Doughnut.ID,
    value?: NoteDetailsCompletion
  ): Promise<void>

  updateWikidataId(
    noteId: Doughnut.ID,
    data: WikidataAssociationCreation
  ): Promise<NoteRealm>

  undo(router: Router): Promise<NoteRealm>

  deleteNote(
    router: Router,
    noteId: Doughnut.ID
  ): Promise<NoteRealm | undefined>

  moveNote(
    sourceId: Doughnut.ID,
    targetId: Doughnut.ID,
    data: NoteMoveDTO
  ): Promise<void>
}
export default class StoredApiCollection implements StoredApi {
  noteEditingHistory: NoteEditingHistory

  managedApi: ManagedApi

  storage: NoteStorage

  constructor(
    managedApi: ManagedApi,
    undoHistory: NoteEditingHistory,
    storage: NoteStorage
  ) {
    this.managedApi = managedApi
    this.noteEditingHistory = undoHistory
    this.storage = storage
  }

  // eslint-disable-next-line class-methods-use-this
  private routerReplaceFocus(router: Router, focusOnNote?: NoteRealm) {
    if (!focusOnNote) {
      return router.replace({ name: "notebooks" })
    }
    return router.replace({
      name: "noteShow",
      params: { noteId: focusOnNote.id },
    })
  }

  private async updateTextContentWithoutUndo(
    noteId: Doughnut.ID,
    field: "edit title" | "edit details",
    content: string
  ) {
    return this.storage.refreshNoteRealm(
      await this.callUpdateApi(noteId, field, content)
    )
  }

  private async callUpdateApi(
    noteId: Doughnut.ID,
    field: "edit title" | "edit details",
    content: string
  ) {
    if (field === "edit title") {
      return this.managedApi.services.updateNoteTitle({
        note: noteId,
        requestBody: {
          newTitle: content,
        },
      })
    }
    return this.managedApi.services.updateNoteDetails({
      note: noteId,
      requestBody: {
        details: content,
      },
    })
  }

  async updateWikidataId(
    noteId: Doughnut.ID,
    data: WikidataAssociationCreation
  ): Promise<NoteRealm> {
    return this.storage.refreshNoteRealm(
      await this.managedApi.services.updateWikidataId({
        note: noteId,
        requestBody: data,
      })
    )
  }

  private async loadNote(noteId: Doughnut.ID) {
    const noteRealm = await this.managedApi.services.show({ note: noteId })
    return this.storage.refreshNoteRealm(noteRealm)
  }

  getNoteRealmRefAndReloadPosition(noteId: Doughnut.ID) {
    this.loadNote(noteId)
    return this.storage.refOfNoteRealm(noteId)
  }

  getNoteRealmRefAndLoadWhenNeeded(noteId: Doughnut.ID) {
    const result = this.storage.refOfNoteRealm(noteId)
    // if children are undefined instead of empty array, we need to load the note
    if (!result.value || result.value.children === undefined)
      this.loadNote(noteId)
    return result
  }

  getNoteRealmRef(noteId: Doughnut.ID) {
    return this.storage.refOfNoteRealm(noteId)
  }

  async createNote(
    router: Router,
    parentId: Doughnut.ID,
    data: NoteCreationDTO
  ) {
    const nrwp = await this.managedApi.services.createNote({
      parentNote: parentId,
      requestBody: data,
    })
    const focus = this.storage.refreshNoteRealm(nrwp.created)
    this.storage.refreshNoteRealm(nrwp.parent)
    this.routerReplaceFocus(router, focus)
    return focus
  }

  async createNoteAfter(
    router: Router,
    referenceId: Doughnut.ID,
    data: NoteCreationDTO
  ) {
    const nrwp =
      await this.managedApi.services.createNoteAfter({
        referenceNote: referenceId,
        requestBody: data,
      })
    const focus = this.storage.refreshNoteRealm(nrwp.created)
    this.storage.refreshNoteRealm(nrwp.parent)
    this.routerReplaceFocus(router, focus)
    return focus
  }

  async createLink(
    sourceId: Doughnut.ID,
    targetId: Doughnut.ID,
    data: LinkCreation
  ) {
    this.refreshNoteRealms(
      await this.managedApi.services.linkNoteFinalize({
        sourceNote: sourceId,
        targetNote: targetId,
        requestBody: data,
      })
    )
  }

  async updateLink(linkId: Doughnut.ID, data: LinkCreation) {
    this.refreshNoteRealms(
      await this.managedApi.services.updateLink({
        link: linkId,
        requestBody: data,
      })
    )
  }

  private refreshNoteRealms(noteRealms: NoteRealm[]) {
    noteRealms.forEach((n) => this.storage.refreshNoteRealm(n))
  }

  async moveAfter(
    noteId: number,
    targetNoteId: number,
    dropMode: "after" | "asFirstChild"
  ): Promise<NoteRealm[]> {
    const updatedNotes = await this.managedApi.services.moveAfter({
      note: noteId,
      targetNote: targetNoteId,
      asFirstChild: dropMode === "asFirstChild" ? "true" : "false",
    })
    this.refreshNoteRealms(updatedNotes)
    return updatedNotes
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
          ? currentNote.noteTopology.titleOrPredicate
          : currentNote.details
      if (old === value) {
        return
      }
      this.noteEditingHistory.addEditingToUndoHistory(noteId, field, old)
    }
    await this.updateTextContentWithoutUndo(noteId, field, value)
  }

  async completeDetails(noteId: Doughnut.ID, value?: NoteDetailsCompletion) {
    if (!value) return

    let currentNote = this.storage.refOfNoteRealm(noteId).value?.note
    if (!currentNote) {
      currentNote = (await this.loadNote(noteId)).note
    }

    const old = currentNote?.details ?? ""
    const deleteCount = Math.min(value.deleteFromEnd ?? 0, old.length)
    const newContent =
      old.slice(0, old.length - deleteCount) + (value.completion ?? "")
    await this.updateTextField(noteId, "edit details", newContent)
  }

  private async undoInner() {
    const undone = this.noteEditingHistory.peekUndo()
    if (!undone) throw new Error("undo history is empty")
    this.noteEditingHistory.popUndoHistory()
    if (
      undone.type === "edit title" ||
      (undone.type === "edit details" && undone.textContent !== undefined)
    ) {
      return this.updateTextContentWithoutUndo(
        undone.noteId,
        undone.type,
        undone.textContent!
      )
    }
    return this.managedApi.services.undoDeleteNote({ note: undone.noteId })
  }

  async undo(router: Router) {
    const noteRealm = await this.undoInner()
    router.push({
      name: "noteShow",
      params: { noteId: noteRealm.id },
    })
    return noteRealm
  }

  async deleteNote(router: Router, noteId: Doughnut.ID) {
    const res = await this.managedApi.services.deleteNote({ note: noteId })
    this.noteEditingHistory.deleteNote(noteId)
    if (res.length === 0) {
      this.routerReplaceFocus(router)
      return undefined
    }
    const noteRealm = this.storage.refreshNoteRealm(res[0]!)
    this.routerReplaceFocus(router, noteRealm)
    return noteRealm
  }

  async moveNote(
    sourceId: Doughnut.ID,
    targetId: Doughnut.ID,
    data: NoteMoveDTO
  ) {
    this.refreshNoteRealms(
      await this.managedApi.services.moveNote({
        sourceNote: sourceId,
        targetNote: targetId,
        requestBody: data,
      })
    )
  }
}
