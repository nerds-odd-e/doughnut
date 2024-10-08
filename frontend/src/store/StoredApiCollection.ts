import type {
  NoteRealm,
  WikidataAssociationCreation,
} from "@/generated/backend"
import { LinkCreation, NoteCreationDTO } from "@/generated/backend"
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

  createLink(
    sourceId: Doughnut.ID,
    targetId: Doughnut.ID,
    data: LinkCreation
  ): Promise<void>

  updateLink(linkId: Doughnut.ID, data: LinkCreation): Promise<void>

  moveUp(noteId: Doughnut.ID): Promise<NoteRealm | void>

  moveDown(noteId: Doughnut.ID): Promise<NoteRealm | void>

  updateTextField(
    noteId: Doughnut.ID,
    field: "edit topic" | "edit details",
    value: string
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
    field: "edit topic" | "edit details",
    content: string
  ) {
    return this.storage.refreshNoteRealm(
      await this.callUpdateApi(noteId, field, content)
    )
  }

  private async callUpdateApi(
    noteId: Doughnut.ID,
    field: "edit topic" | "edit details",
    content: string
  ) {
    if (field === "edit topic") {
      return this.managedApi.restTextContentController.updateNoteTopicConstructor(
        noteId,
        { topicConstructor: content }
      )
    }
    return this.managedApi.restTextContentController.updateNoteDetails(noteId, {
      details: content,
    })
  }

  async updateWikidataId(
    noteId: Doughnut.ID,
    data: WikidataAssociationCreation
  ): Promise<NoteRealm> {
    return this.storage.refreshNoteRealm(
      await this.managedApi.restNoteController.updateWikidataId(noteId, data)
    )
  }

  private loadNote(noteId: Doughnut.ID) {
    this.managedApi.restNoteController
      .show1(noteId)
      .then((noteRealm) => this.storage.refreshNoteRealm(noteRealm))
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

  async createNote(
    router: Router,
    parentId: Doughnut.ID,
    data: NoteCreationDTO
  ) {
    const nrwp = await this.managedApi.restNoteController.createNote(
      parentId,
      data
    )
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
      await this.managedApi.restLinkController.linkNoteFinalize(
        sourceId,
        targetId,
        data
      )
    )
  }

  async updateLink(linkId: Doughnut.ID, data: LinkCreation) {
    this.refreshNoteRealms(
      await this.managedApi.restLinkController.updateLink(linkId, data)
    )
  }

  private siblingsOf(noteId: Doughnut.ID) {
    const noteRealm = this.storage.refOfNoteRealm(noteId)
    if (!noteRealm.value) return {}
    const { parentId } = noteRealm.value.note
    if (!parentId) return {}
    return {
      parentId,
      siblings: this.storage.refOfNoteRealm(parentId).value?.children,
    }
  }

  private refreshNoteRealms(noteRealms: NoteRealm[]) {
    noteRealms.forEach((n) => this.storage.refreshNoteRealm(n))
  }

  async moveUp(noteId: Doughnut.ID) {
    const { parentId, siblings } = this.siblingsOf(noteId)
    if (!siblings) return
    const currentIndex = siblings.map((n) => n.id).indexOf(noteId)
    this.refreshNoteRealms(
      await this.managedApi.restNoteController.moveAfter(
        noteId,
        currentIndex === 1 ? parentId : siblings[currentIndex - 2]!.id,
        currentIndex === 1 ? "asFirstChild" : "after"
      )
    )
  }

  async moveDown(noteId: Doughnut.ID) {
    const { siblings } = this.siblingsOf(noteId)
    if (!siblings) return
    const currentIndex = siblings.map((n) => n.id).indexOf(noteId)
    this.refreshNoteRealms(
      await this.managedApi.restNoteController.moveAfter(
        noteId,
        siblings[currentIndex + 1]!.id,
        "after"
      )
    )
  }

  async updateTextField(
    noteId: Doughnut.ID,
    field: "edit topic" | "edit details",
    value: string
  ) {
    const currentNote = this.storage.refOfNoteRealm(noteId).value?.note
    if (currentNote) {
      const old =
        field === "edit topic"
          ? currentNote.noteTopic.topicConstructor
          : currentNote.details
      if (old === value) {
        return
      }
      this.noteEditingHistory.addEditingToUndoHistory(noteId, field, old)
    }
    await this.updateTextContentWithoutUndo(noteId, field, value)
  }

  private async undoInner() {
    const undone = this.noteEditingHistory.peekUndo()
    if (!undone) throw new Error("undo history is empty")
    this.noteEditingHistory.popUndoHistory()
    if (
      undone.type === "edit topic" ||
      (undone.type === "edit details" && undone.textContent !== undefined)
    ) {
      return this.updateTextContentWithoutUndo(
        undone.noteId,
        undone.type,
        undone.textContent!
      )
    }
    return this.managedApi.restNoteController.undoDeleteNote(undone.noteId)
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
    const res = await this.managedApi.restNoteController.deleteNote(noteId)
    this.noteEditingHistory.deleteNote(noteId)
    if (res.length === 0) {
      this.routerReplaceFocus(router)
      return undefined
    }
    const noteRealm = this.storage.refreshNoteRealm(res[0]!)
    this.routerReplaceFocus(router, noteRealm)
    return noteRealm
  }
}
