interface HistoryRecord {
  type: "editing" | "delete note";
  noteId: Doughnut.ID;
  textContent?: Generated.TextContent;
}

interface HistoryState {
  noteUndoHistories: HistoryRecord[];
}

class History {
  state;

  constructor(state: HistoryState) {
    this.state = state;
  }

  peekUndo() {
    if (this.state.noteUndoHistories.length === 0) return null;
    return this.state.noteUndoHistories[
      this.state.noteUndoHistories.length - 1
    ];
  }

  addEditingToUndoHistory(
    noteId: Doughnut.ID,
    textContent: Generated.TextContent
  ) {
    this.state.noteUndoHistories.push({
      type: "editing",
      noteId,
      textContent: { ...textContent },
    });
  }

  popUndoHistory() {
    if (this.state.noteUndoHistories.length === 0) {
      return;
    }
    this.state.noteUndoHistories.pop();
  }

  deleteNote(noteId: Doughnut.ID) {
    this.state.noteUndoHistories.push({ type: "delete note", noteId });
  }
}

function history(state: HistoryState) {
  return new History(state);
}

export default history;
export { HistoryState };
