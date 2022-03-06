import { createStore } from "vuex";
import useStore from "./pinia_store";

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

export default ()=>createStore({
  state: () => ({
    notes: {},
    highlightNoteId: null,
    viewType: null,
    noteUndoHistories: [],
    currentUser: null,
    featureToggle: false,
    environment: 'production',
    piniaStore: useStore(),
  }),

  getters: {
    getCurrentUser: (state) => () => state.piniaStore.currentUser,
    getHighlightNoteId: (state) => () => state.highlightNoteId,
    getViewType: (state) => () => state.viewType,
    getHighlightNote: (state) => () => withState(state).getNoteById(state.highlightNoteId),
    getEnvironment: (state) => () => state.environment,
    getFeatureToggle: (state) => () => state.featureToggle,
    getNoteById: (state) => (id) => withState(state).getNoteById(id),
    getChildrenIdsByParentId: (state) => (parentId) => withState(state).getChildrenIdsByParentId(parentId),
    getChildrenOfParentId: (state) => (parentId) => withState(state).getChildrenOfParentId(parentId),
    peekUndo: (state) => () => {
      if(state.noteUndoHistories.length === 0) return null
      return state.noteUndoHistories[state.noteUndoHistories.length - 1]
    },
  },

  mutations: {
    addEditingToUndoHistory(state, {noteId}) {
      state.noteUndoHistories.push({type: 'editing', noteId, textContent: {...withState(state).getNoteById(noteId).textContent}});
    },
    popUndoHistory(state) {
      if (state.noteUndoHistories.length === 0) {
        return
      }
      state.noteUndoHistories.pop();
    },
    loadNotes(state, notes) {
      notes.forEach((note) => {
       state.notes[note.id] = note;
      });
    },
    deleteNote(state, noteId) {
      withState(state).deleteNoteFromParentChildrenList(noteId)
      withState(state).deleteNote(noteId)
      state.noteUndoHistories.push({type: 'delete note', noteId});
    },
    highlightNoteId(state, noteId) {
      state.highlightNoteId = noteId
    },
    viewType(state, viewType) {
      state.viewType = viewType
    },
    currentUser(state, user) {
      state.piniaStore.setCurrentUser(user)
    },
    featureToggle(state, ft) {
      state.environment = "testing"
      state.featureToggle = ft
    },
  },
});
