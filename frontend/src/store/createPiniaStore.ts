import { defineStore } from "pinia";
import history, { HistoryState } from "./history";

type State = HistoryState;

export default defineStore("main", {
  state: () =>
    ({
      noteUndoHistories: [],
    } as State),

  getters: {
    peekUndo: (state) => () => history(state).peekUndo(),
  },

  actions: {
    addEditingToUndoHistory(
      noteId: Doughnut.ID,
      oldContent: Generated.TextContent
    ) {
      history(this).addEditingToUndoHistory(noteId, oldContent);
    },

    popUndoHistory() {
      history(this).popUndoHistory();
    },

    deleteNote(noteId: Doughnut.ID) {
      history(this).deleteNote(noteId);
    },
  },
});
