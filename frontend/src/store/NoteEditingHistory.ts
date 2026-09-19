export type HistoryRecord =
  | {
      type: "edit title" | "edit content" | "create note"
      noteId: Donut.ID
      textContent?: string
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

export default class NoteEditingHistory {
  noteUndoHistories: HistoryRecord[]

  constructor() {
    this.noteUndoHistories = []
  }

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

  popUndoHistory() {
    if (this.noteUndoHistories.length === 0) {
      return
    }
    this.noteUndoHistories.pop()
  }

  /** Drops every undo entry belonging to a note that no longer exists. */
  forgetNote(noteId: Donut.ID) {
    this.noteUndoHistories = this.noteUndoHistories.filter(
      (entry) => entry.noteId !== noteId
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
}
