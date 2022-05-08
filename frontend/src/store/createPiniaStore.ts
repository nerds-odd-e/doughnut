import { defineStore } from "pinia";
import history, { HistoryState } from "./history";
import noteCache, { NoteCacheState } from "./noteCache";

interface State extends NoteCacheState, HistoryState {
  currentUser: Generated.User | null;
  featureToggle: boolean;
  environment: "production" | "testing";
}

export default defineStore("main", {
  state: () =>
    ({
      notebooks: [],
      notebooksMapByHeadNoteId: {},
      noteRealms: {},
      noteUndoHistories: [],
      popupInfo: undefined,
      currentUser: null,
      featureToggle: false,
      environment: "production",
    } as State),

  getters: {
    peekUndo: (state) => () => history(state).peekUndo(),
  },

  actions: {
    loadNotebooks(notebooks: Generated.NotebookViewedByUser[]) {
      noteCache(this).loadNotebooks(notebooks);
    },

    addEditingToUndoHistory(
      noteId: Doughnut.ID,
      oldContent: Generated.TextContent
    ) {
      history(this).addEditingToUndoHistory(noteId, oldContent);
    },

    popUndoHistory() {
      history(this).popUndoHistory();
    },

    loadNoteRealms(noteRealms: Generated.NoteRealm[]) {
      noteCache(this).loadNoteRealms(noteRealms);
    },

    loadNotePosition(notePosition: Generated.NotePositionViewedByUser) {
      noteCache(this).loadNotePosition(notePosition);
    },

    loadNotesBulk(noteBulk: Generated.NotesBulk) {
      this.loadNoteRealms(noteBulk.notes);
      this.loadNotePosition(noteBulk.notePosition);
    },

    loadNoteWithPosition(noteWithPosition: Generated.NoteWithPosition) {
      this.loadNoteRealms([noteWithPosition.note]);
      this.loadNotePosition(noteWithPosition.notePosition);
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
