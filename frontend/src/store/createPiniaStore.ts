import { defineStore } from "pinia";
import history, { HistoryState } from "./history";

interface State extends HistoryState {
  currentUser: Generated.User | null;
  featureToggle: boolean;
  environment: "production" | "testing";
}

export default defineStore("main", {
  state: () =>
    ({
      noteUndoHistories: [],
      currentUser: null,
      featureToggle: false,
      environment: "production",
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
    setFeatureToggle(ft: boolean) {
      this.environment = "testing";
      this.featureToggle = ft;
    },
  },
});
