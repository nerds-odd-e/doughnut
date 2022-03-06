import { defineStore } from "pinia";

function withState(state) {
  return {
    getNoteById(id) { return state.notes[id] },

    getChildrenIdsByParentId(parentId) {
      return !state.notes[parentId]
        ? []
        : state.notes[parentId].childrenIds
    },

    getChildrenOfParentId(parentId) {
      return this.getChildrenIdsByParentId(parentId)
        .map(id=>this.getNoteById(id))
        .filter(n=>n)
    },
   
    deleteNote(id) {
      this.getChildrenIdsByParentId(id)?.forEach(cid=>this.deleteNote(cid))
      delete state.notes[id]
    },

    deleteNoteFromParentChildrenList(id) {
      const children = this.getNoteById(
        this.getNoteById(id)?.parentId
      )?.childrenIds
      if (children) {
        const index = children.indexOf(id)
        if (index > -1) {
          children.splice(index, 1);
        }
      }
    },

  }
}

export default defineStore('main', {
    state: () => ({
        notebooks: [],
        notes: {},
        highlightNoteId: null,
        viewType: null,
        noteUndoHistories: [],
        currentUser: null,
        featureToggle: false,
        environment: 'production',
    }),

    getters: {
        getCurrentUser: (state)     => state.currentUser,
        getHighlightNoteId: (state) => () => state.highlightNoteId,
        getViewType: (state) => ()  => state.viewType,
        getHighlightNote: (state)   => () => withState(state).getNoteById(state.highlightNoteId),
        getEnvironment: (state)     => () => state.environment,
        getFeatureToggle: (state)   => () => state.featureToggle,
        getNoteById: (state)        => (id) => withState(state).getNoteById(id),
        peekUndo: (state)           => () => {
          if(state.noteUndoHistories.length === 0) return null
          return state.noteUndoHistories[state.noteUndoHistories.length - 1]
        },
        getChildrenIdsByParentId: (state) => (parentId) => withState(state).getChildrenIdsByParentId(parentId),
        getChildrenOfParentId: (state)    => (parentId) => withState(state).getChildrenOfParentId(parentId),
    },

    actions: {
        setNotebooks(notebooks) {
          this.notebooks = notebooks
        },
        addEditingToUndoHistory({noteId}) {
          console.log(`in store.addEditingToUndoHistory ${noteId}`)
          this.noteUndoHistories.push({type: 'editing', noteId, textContent: {...withState(this).getNoteById(noteId).textContent}});
        },
        popUndoHistory() {
          if (this.noteUndoHistories.length === 0) {
            return
          }
          this.noteUndoHistories.pop();
        },
        loadNotes(notes) {
          notes.forEach((note) => {
            this.notes[note.id] = note;
          });
        },
        deleteNote(noteId) {
          withState(this).deleteNoteFromParentChildrenList(noteId)
          withState(this).deleteNote(noteId)
          this.noteUndoHistories.push({type: 'delete note', noteId});
        },
        highlightNoteId(noteId) {
          this.highlightNoteId = noteId
        },
        viewType(viewType) {
          this.viewType = viewType
        },
        currentUser(user) {
          this.currentUser = user
        },
        featureToggle(ft) {
          this.environment = "testing"
          this.featureToggle = ft
        },
      },
    });