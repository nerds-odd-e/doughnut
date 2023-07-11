export interface HistoryRecord {
  type: "editing" | "delete note";
  noteId: Doughnut.ID;
  textContent?: Generated.TextContent;
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
    textContent: Generated.TextContent,
  ) {
    this.noteUndoHistories.push({
      type: "editing",
      noteId,
      textContent: { ...textContent },
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
