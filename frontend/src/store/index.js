import { createStore } from "vuex";
import useStore from "./pinia_store";

export default ()=>createStore({
  state: () => ({
    notes: {},
    noteUndoHistories: [],
    piniaStore: useStore(),
  }),

  getters: {
    getCurrentUser: (state) => () => state.piniaStore.currentUser,
    getHighlightNoteId: (state) => () => state.piniaStore.highlightNoteId,
    getViewType: (state) => () => state.piniaStore.viewType,
    getHighlightNote: (state) => () => state.piniaStore.getHighlightNote(),
    getEnvironment: (state) => () => state.piniaStore.environment,
    getFeatureToggle: (state) => () => state.piniaStore.featureToggle,
    getNoteById: (state) => (id) => state.piniaStore.getNoteById(id),
    getChildrenIdsByParentId: (state) => (parentId) => state.piniaStore.getChildrenIdsByParentId(parentId),
    getChildrenOfParentId: (state) => (parentId) => state.piniaStore.getChildrenOfParentId(parentId),
    peekUndo: (state) => () => {
      if(state.noteUndoHistories.length === 0) return null
      return state.noteUndoHistories[state.noteUndoHistories.length - 1]
    },
  },

  mutations: {
    addEditingToUndoHistory(state, {noteId}) {
      state.noteUndoHistories.push({type: 'editing', noteId, textContent: {
        ...state.piniaStore.getNoteById(noteId).textContent}});
    },
    popUndoHistory(state) {
      if (state.noteUndoHistories.length === 0) {
        return
      }
      state.noteUndoHistories.pop();
    },
    loadNotes(state, notes) {
      state.piniaStore.loadNotes(notes)
    },
    deleteNote(state, noteId) {
      state.piniaStore.deleteNote(noteId);
      state.noteUndoHistories.push({type: 'delete note', noteId});
    },
    highlightNoteId(state, noteId) {
      state.piniaStore.setHighlightNoteId(noteId)
    },
    viewType(state, viewType) {
      state.piniaStore.setViewType(viewType)
    },
    currentUser(state, user) {
      state.piniaStore.setCurrentUser(user)
    },
    featureToggle(state, ft) {
      state.environment = "testing"
      state.piniaStore.setFeatureToggle(ft)
    },
  },
});
