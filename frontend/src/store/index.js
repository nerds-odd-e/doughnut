import { createStore } from "vuex";
import useStore from "./pinia_store";

export default ()=>createStore({
  state: () => ({
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
    peekUndo: (state) => () => state.piniaStore.peekUndo(),
  },

  mutations: {
    addEditingToUndoHistory(state, {noteId}) {
      state.piniaStore.addEditingToUndoHistory({noteId})
    },
    popUndoHistory(state) {
      state.piniaStore.popUndoHistory()
    },
    loadNotes(state, notes) {
      state.piniaStore.loadNotes(notes)
    },
    deleteNote(state, noteId) {
      state.piniaStore.deleteNote(noteId);
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
      state.piniaStore.setFeatureToggle(ft)
    },
  },
});
