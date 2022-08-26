interface HistoryRecord {
  type: "editing" | "delete note";
  noteId: Doughnut.ID;
  textContent?: Generated.TextContent;
}

interface HistoryState {
  noteUndoHistories: HistoryRecord[];
}

class History implements HistoryState {
  noteUndoHistories: HistoryRecord[];

  constructor(state: HistoryState) {
    this.noteUndoHistories = state.noteUndoHistories;
  }

  peekUndo() {
    if (this.noteUndoHistories.length === 0) return null;
    return this.noteUndoHistories[this.noteUndoHistories.length - 1];
  }

  addEditingToUndoHistory(
    noteId: Doughnut.ID,
    textContent: Generated.TextContent
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

function history(state: HistoryState) {
  return new History(state);
}

export default history;
export type { HistoryState, History };
