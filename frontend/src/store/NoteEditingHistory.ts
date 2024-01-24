export interface HistoryRecord {
  type: "edit topic" | "edit details" | "delete note";
  noteId: Doughnut.ID;
  textContent?: string;
}

export default class NoteEditingHistory {
  noteUndoHistories: HistoryRecord[];

  constructor() {
    this.noteUndoHistories = [];
  }

  peekUndo() {
    if (this.noteUndoHistories.length === 0) return null;
    return this.noteUndoHistories[this.noteUndoHistories.length - 1];
  }

  addEditingToUndoHistory(
    noteId: Doughnut.ID,
    field: "edit topic" | "edit details",
    textContent: string,
  ) {
    this.noteUndoHistories.push({
      type: field,
      noteId,
      textContent,
    });
  }

  popUndoHistory() {
    if (this.noteUndoHistories.length === 0) {
      return;
    }
    this.noteUndoHistories.pop();
  }

  deleteNote(noteId: Doughnut.ID) {
    this.noteUndoHistories.push({ type: "delete note", noteId });
  }
}
