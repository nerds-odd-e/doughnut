import { defineStore } from "pinia";
import history, { HistoryState } from "./history";
import noteCache, { NoteCacheState } from "./noteCache";

interface PopupInfo {
  type: 'alert' | 'confirm' | 'dialog'
  message?: string
  doneResolve: (value: unknown)=>void
  component?: string
  attrs?: unknown
}

interface State extends NoteCacheState, HistoryState {
  popupInfo?: PopupInfo,
  currentUser: Generated.User | null
  featureToggle: boolean
  environment: 'production' | 'testing'
}

export default defineStore('main', {
  state: () => ({
    notebooks: [],
    notebooksMapByHeadNoteId: {},
    noteSpheres: {},
    noteUndoHistories: [],
    popupInfo: undefined,
    currentUser: null,
    featureToggle: false,
    environment: 'production',
  } as State),

  getters: {
    getNoteSphereById: (state) => (id: Doughnut.ID) => noteCache(state).getNoteSphereById(id),
    getNotePosition: (state) => (id: Doughnut.ID) => noteCache(state).getNotePosition(id),
    peekUndo: (state) => () => history(state).peekUndo()
  },

  actions: {
    loadNotebooks(notebooks: Generated.NotebookViewedByUser[]) {
      noteCache(this).loadNotebooks(notebooks)
    },

    addEditingToUndoHistory({ noteId }: { noteId: Doughnut.ID }) {
      const noteSphere = noteCache(this).getNoteSphereById(noteId)
      if(!noteSphere) throw new Error('Note not in cache')
      history(this).addEditingToUndoHistory(noteId, noteSphere.note.textContent);
    },

    popUndoHistory() {
      history(this).popUndoHistory()
    },

    loadNoteSpheres(noteSpheres: Generated.NoteSphere[]) {
      noteCache(this).loadNoteSpheres(noteSpheres)
    },

    loadNotePosition(notePosition: Generated.NotePositionViewedByUser) {
      noteCache(this).loadNotePosition(notePosition)
    },

    loadNotesBulk(noteBulk: Generated.NotesBulk) {
      this.loadNoteSpheres(noteBulk.notes);
      this.loadNotePosition(noteBulk.notePosition);
    },

    loadNoteWithPosition(noteWithPosition: Generated.NoteWithPosition) {
      this.loadNoteSpheres([noteWithPosition.note]);
      this.loadNotePosition(noteWithPosition.notePosition);
    },

    deleteNote(noteId: Doughnut.ID) {
      noteCache(this).deleteNoteAndDescendents(noteId)
      history(this).deleteNote(noteId)
    },
    setCurrentUser(user: Generated.User) {
      this.currentUser = user
    },
    setFeatureToggle(ft: boolean) {
      this.environment = "testing"
      this.featureToggle = ft
    },
  },
});
