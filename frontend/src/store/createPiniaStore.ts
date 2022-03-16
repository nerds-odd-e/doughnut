import { defineStore } from "pinia";
import noteCache, { NoteCacheState } from "./noteCache";


interface State extends NoteCacheState {
  noteUndoHistories: any[]
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
        currentUser: null,
        featureToggle: false,
        environment: 'production',
    } as State),

    getters: {
        getNoteSphereById: (state)        => (id: Doughnut.ID) => noteCache(state).getNoteSphereById(id),
        getNotePosition: (state)        => (id: Doughnut.ID) => noteCache(state).getNotePosition(id),
        peekUndo: (state)           => () => {
          if(state.noteUndoHistories.length === 0) return null
          return state.noteUndoHistories[state.noteUndoHistories.length - 1]
        },
    },

    actions: {
        loadNotebooks(notebooks: Generated.NotebookViewedByUser[]) {
          noteCache(this).loadNotebooks(notebooks)
        },

        addEditingToUndoHistory({noteId}: {noteId: Doughnut.ID}) {
          this.noteUndoHistories.push({type: 'editing', noteId, textContent: {...noteCache(this).getNoteSphereById(noteId)?.note.textContent}});
        },

        popUndoHistory() {
          if (this.noteUndoHistories.length === 0) {
            return
          }
          this.noteUndoHistories.pop();
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
          this.noteUndoHistories.push({type: 'delete note', noteId});
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
