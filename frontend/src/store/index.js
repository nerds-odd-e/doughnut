import { createStore } from "vuex";
import useStore from "./pinia_store";

export default ()=>createStore({
  state: () => ({
    piniaStore: useStore(),
  }),

  getters: {
    ps: (state) => () => state.piniaStore,
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
