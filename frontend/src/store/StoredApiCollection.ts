import type {
  NoteDetailsCompletion,
  NoteMoveDto,
  NoteRealm,
  WikidataAssociationCreation,
} from "@generated/backend"
import type { LinkCreation, NoteCreationDto } from "@generated/backend"
import {
  LinkController,
  NoteController,
  NoteCreationController,
  TextContentController,
} from "@generated/backend/sdk.gen"
import {
  toOpenApiError,
  setErrorObjectForFieldErrors,
} from "@/managedApi/openApiError"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import type { Ref } from "vue"
import type { Router } from "vue-router"
import NoteEditingHistory from "./NoteEditingHistory"
import type NoteStorage from "./NoteStorage"
import { applyPatch } from "diff"

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
    data: NoteCreationDto
  ): Promise<NoteRealm>

  createNoteAfter(
    router: Router,
    referenceId: Doughnut.ID,
    data: NoteCreationDto
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
    data: NoteMoveDto
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
    return await router.replace({
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
      const { data, error } = await apiCallWithLoading(() =>
        TextContentController.updateNoteTitle({
          path: { note: noteId },
          body: {
            newTitle: content,
          },
        })
      )
      if (error || !data) {
        throw new Error(error || "Failed to update note title")
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
      throw new Error(error || "Failed to update note details")
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
      throw new Error(error || "Failed to load note")
    }
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
    data: NoteCreationDto
  ) {
    const { data: nrwp, error } = await apiCallWithLoading(() =>
      NoteCreationController.createNoteUnderParent({
        path: { parentNote: parentId },
        body: data,
      })
    )
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
        if (errorObj.errors) {
          apiError.status = 400
        }
      }
      throw apiError
    }
    const focus = this.storage.refreshNoteRealm(nrwp.created)
    this.storage.refreshNoteRealm(nrwp.parent)
    await this.routerReplaceFocus(router, focus)
    return focus
  }

  async createNoteAfter(
    router: Router,
    referenceId: Doughnut.ID,
    data: NoteCreationDto
  ) {
    const { data: nrwp, error } = await apiCallWithLoading(() =>
      NoteCreationController.createNoteAfter({
        path: { referenceNote: referenceId },
        body: data,
      })
    )
    if (error || !nrwp) {
      const apiError = new Error("Failed to create note after") as Error & {
        body?: unknown
        status?: number
        [key: string]: unknown
      }
      if (error) {
        apiError.body = error
        setErrorObjectForFieldErrors(apiError)
        const errorObj = toOpenApiError(error)
        apiError.message = errorObj.message || "Failed to create note after"
        if (errorObj.errors) {
          apiError.status = 400
        }
      }
      throw apiError
    }
    const focus = this.storage.refreshNoteRealm(nrwp.created)
    this.storage.refreshNoteRealm(nrwp.parent)
    await this.routerReplaceFocus(router, focus)
    return focus
  }

  async createLink(
    sourceId: Doughnut.ID,
    targetId: Doughnut.ID,
    data: LinkCreation
  ) {
    const { data: noteRealms, error } = await apiCallWithLoading(() =>
      LinkController.linkNoteFinalize({
        path: {
          sourceNote: sourceId,
          targetNote: targetId,
        },
        body: data,
      })
    )
    if (error || !noteRealms) {
      throw new Error(error || "Failed to create link")
    }
    this.refreshNoteRealms(noteRealms)
  }

  async updateLink(linkId: Doughnut.ID, data: LinkCreation) {
    const { data: noteRealms, error } = await apiCallWithLoading(() =>
      LinkController.updateLink({
        path: { link: linkId },
        body: data,
      })
    )
    if (error || !noteRealms) {
      throw new Error(error || "Failed to update link")
    }
    this.refreshNoteRealms(noteRealms)
  }

  private refreshNoteRealms(noteRealms: NoteRealm[]) {
    noteRealms.forEach((n) => this.storage.refreshNoteRealm(n))
  }

  async moveAfter(
    noteId: number,
    targetNoteId: number,
    dropMode: "after" | "asFirstChild"
  ): Promise<NoteRealm[]> {
    const { data: updatedNotes, error } = await apiCallWithLoading(() =>
      NoteController.moveAfter({
        path: {
          note: noteId,
          targetNote: targetNoteId,
          asFirstChild: dropMode === "asFirstChild" ? "true" : "false",
        },
      })
    )
    if (error || !updatedNotes) {
      throw new Error(error || "Failed to move note")
    }
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
    if (!value || !value.patch) return

    let currentNote = this.storage.refOfNoteRealm(noteId).value?.note
    if (!currentNote) {
      currentNote = (await this.loadNote(noteId)).note
    }

    const old = currentNote?.details ?? ""
    try {
      const result = applyPatch(old, value.patch)
      if (result === false) {
        throw new Error("Failed to apply patch to note details")
      }
      await this.updateTextField(noteId, "edit details", result)
    } catch (error) {
      throw new Error(
        `Failed to apply patch to note details: ${error instanceof Error ? error.message : String(error)}`
      )
    }
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
    const { data: noteRealm, error } = await apiCallWithLoading(() =>
      NoteController.undoDeleteNote({
        path: { note: undone.noteId },
      })
    )
    if (error || !noteRealm) {
      throw new Error(error || "Failed to undo delete note")
    }
    return noteRealm
  }

  async undo(router: Router) {
    const noteRealm = await this.undoInner()
    await router.push({
      name: "noteShow",
      params: { noteId: noteRealm.id },
    })
    return noteRealm
  }

  async deleteNote(router: Router, noteId: Doughnut.ID) {
    const { data: res, error } = await apiCallWithLoading(() =>
      NoteController.deleteNote({
        path: { note: noteId },
      })
    )
    if (error || !res) {
      throw new Error(error || "Failed to delete note")
    }
    this.noteEditingHistory.deleteNote(noteId)
    if (res.length === 0) {
      await this.routerReplaceFocus(router)
      return undefined
    }
    const noteRealm = this.storage.refreshNoteRealm(res[0]!)
    await this.routerReplaceFocus(router, noteRealm)
    return noteRealm
  }

  async moveNote(
    sourceId: Doughnut.ID,
    targetId: Doughnut.ID,
    data: NoteMoveDto
  ) {
    const { data: noteRealms, error } = await apiCallWithLoading(() =>
      LinkController.moveNote({
        path: {
          sourceNote: sourceId,
          targetNote: targetId,
        },
        body: data,
      })
    )
    if (error || !noteRealms) {
      throw new Error(error || "Failed to move note")
    }
    this.refreshNoteRealms(noteRealms)
  }
}
