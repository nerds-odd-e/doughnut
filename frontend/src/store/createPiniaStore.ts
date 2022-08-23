import { defineStore } from "pinia";
import history, { HistoryState } from "./history";

interface State extends HistoryState {
  currentUser: Generated.User | undefined;
}

export default defineStore("main", {
  state: () =>
    ({
      noteUndoHistories: [],
      currentUser: undefined,
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

    setCurrentUser(user: Generated.User) {
      this.currentUser = user;
    },
  },
});
