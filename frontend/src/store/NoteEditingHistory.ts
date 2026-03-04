export interface HistoryRecord {
  type:
    | "edit title"
    | "edit details"
    | "delete note"
    | "create note"
    | "move note"
  noteId: Doughnut.ID
  textContent?: string
  originalParentId?: Doughnut.ID
  previousSiblingId?: Doughnut.ID | null
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
    noteId: Doughnut.ID,
    field: "edit title" | "edit details",
    textContent?: string
  ) {
    const lastEntry = this.peekUndo()
    // Accumulate continuous edits to the same note's title or details into one undo entry
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

  deleteNote(noteId: Doughnut.ID) {
    this.noteUndoHistories.push({ type: "delete note", noteId })
  }

  createNote(noteId: Doughnut.ID) {
    this.noteUndoHistories.push({ type: "create note", noteId })
  }

  moveNote(
    noteId: Doughnut.ID,
    originalParentId: Doughnut.ID | null,
    previousSiblingId: Doughnut.ID | null
  ) {
    this.noteUndoHistories.push({
      type: "move note",
      noteId,
      originalParentId: originalParentId ?? undefined,
      previousSiblingId,
    })
  }
}
