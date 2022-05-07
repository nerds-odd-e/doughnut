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
    getNoteRealmById: (state) => (id: Doughnut.ID) =>
      noteCache(state).getNoteRealmById(id),
    getNotePosition: (state) => (id: Doughnut.ID) =>
      noteCache(state).getNotePosition(id),
    peekUndo: (state) => () => history(state).peekUndo(),
  },

  actions: {
    loadNotebooks(notebooks: Generated.NotebookViewedByUser[]) {
      noteCache(this).loadNotebooks(notebooks);
    },

    addEditingToUndoHistory(noteId : Doughnut.ID, textContent: Generated.TextContent) {
      history(this).addEditingToUndoHistory(noteId, textContent);
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
