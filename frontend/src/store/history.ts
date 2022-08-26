interface HistoryRecord {
  type: "editing" | "delete note";
  noteId: Doughnut.ID;
  textContent?: Generated.TextContent;
}

interface HistoryState {
  noteUndoHistories: HistoryRecord[];
  peekUndo(): null | HistoryRecord;
  popUndoHistory(): void;
  addEditingToUndoHistory(
    noteId: Doughnut.ID,
    textContent: Generated.TextContent
  ): void;
  deleteNote(noteId: Doughnut.ID): void;
}

class History implements HistoryState {
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

function history(): HistoryState {
  return new History();
}

type HistoryWriter = (writer: (h: HistoryState) => void) => void;

export default history;
export type { HistoryState, HistoryWriter };
