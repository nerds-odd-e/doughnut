import { reactive } from "vue"
import type { RouteLocationRaw } from "vue-router"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { refreshSidebarStructuralListings } from "@/components/notes/sidebarStructuralRefresh"
import type NoteStorage from "./NoteStorage"
import {
  placeNoteRequest,
  trashNoteRequest,
  undoTrashNoteRequest,
  updateTextContentRequest,
} from "./noteRequests"

export type HistoryRecord =
  | {
      type: "edit title" | "edit content"
      noteId: Donut.ID
      textContent?: string
    }
  | {
      type: "create note"
      noteId: Donut.ID
    }
  | {
      type: "move note"
      noteId: Donut.ID
      /** Previous folder before the move; null means notebook root. */
      originalFolderId: number | null
      /** Notebook the note was in before the move (needed to undo root placement across notebooks). */
      originalNotebookId: number
    }
  | {
      type: "trash note"
      noteId: Donut.ID
      originalTitle: string
      /** Previous folder before trash; null means notebook root. */
      originalFolderId: number | null
    }

export default class NoteUndo {
  noteUndoHistories = reactive<HistoryRecord[]>([])

  constructor(private storage: NoteStorage) {}

  peekUndo() {
    if (this.noteUndoHistories.length === 0) return null
    return this.noteUndoHistories[this.noteUndoHistories.length - 1]
  }

  addEditingToUndoHistory(
    noteId: Donut.ID,
    field: "edit title" | "edit content",
    textContent?: string
  ) {
    const lastEntry = this.peekUndo()
    // Accumulate continuous edits to the same note's title or content into one undo entry
    if (lastEntry && lastEntry.type === field && lastEntry.noteId === noteId) {
      // Don't add a new entry - keep the original old value
      return
    }
    this.noteUndoHistories.push({
      type: field,
      noteId,
      textContent,
    })
  }

  discardUndo() {
    if (this.noteUndoHistories.length === 0) {
      return
    }
    this.noteUndoHistories.pop()
  }

  /** Drops every undo entry belonging to a note that no longer exists. */
  forgetNote(noteId: Donut.ID) {
    const remaining = this.noteUndoHistories.filter(
      (entry) => entry.noteId !== noteId
    )
    this.noteUndoHistories.splice(
      0,
      this.noteUndoHistories.length,
      ...remaining
    )
  }

  trashNote(
    noteId: Donut.ID,
    originalTitle: string,
    originalFolderId: number | null
  ) {
    this.noteUndoHistories.push({
      type: "trash note",
      noteId,
      originalTitle,
      originalFolderId,
    })
  }

  createNote(noteId: Donut.ID) {
    this.noteUndoHistories.push({ type: "create note", noteId })
  }

  moveNote(
    noteId: Donut.ID,
    undoPlacement: { folderId: number | null; notebookId: number }
  ) {
    this.noteUndoHistories.push({
      type: "move note",
      noteId,
      originalFolderId: undoPlacement.folderId,
      originalNotebookId: undoPlacement.notebookId,
    })
  }

  async undoLast(): Promise<RouteLocationRaw> {
    const undone = this.peekUndo()
    if (!undone) throw new Error("undo history is empty")
    if (undone.type !== "trash note") this.discardUndo()
    switch (undone.type) {
      case "trash note": {
        const realm = await undoTrashNoteRequest(undone.noteId, {
          priorTitle: undone.originalTitle,
          ...(undone.originalFolderId == null
            ? {}
            : { priorFolderId: undone.originalFolderId }),
        })
        this.discardUndo()
        refreshSidebarStructuralListings()
        return noteShowLocation(this.storage.refreshNoteRealm(realm).id)
      }
      case "edit title":
      case "edit content": {
        const realm = this.storage.refreshNoteRealm(
          await updateTextContentRequest(
            undone.noteId,
            undone.type,
            undone.textContent ?? ""
          )
        )
        if (undone.type === "edit title") refreshSidebarStructuralListings()
        return noteShowLocation(realm.id)
      }
      case "create note": {
        const notebookId = this.storage.refOfNoteRealm(undone.noteId).value
          ?.notebookRealm.notebook.id
        await trashNoteRequest(undone.noteId, {
          referenceHandling: "LEAVE_DEAD_LINKS",
        })
        this.storage.removeNoteRealm(undone.noteId)
        return notebookId === undefined
          ? { name: "notebooks" }
          : { name: "notebookPage", params: { notebookId } }
      }
      case "move note": {
        const realms = await placeNoteRequest(
          undone.noteId,
          undone.originalFolderId != null
            ? { folderId: undone.originalFolderId }
            : { notebookId: undone.originalNotebookId }
        )
        realms.forEach((realm) => this.storage.refreshNoteRealm(realm))
        refreshSidebarStructuralListings()
        return noteShowLocation(realms[0]!.id)
      }
    }
  }
}
