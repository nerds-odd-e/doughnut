import { createStore } from "vuex";

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
    }
  }
}

export default createStore({
  state: () => ({
    highlightNoteId: null,
    notes: {},
    currentUser: null,
    featureToggle: false,
  }),

  getters: {
    getCurrentUser: (state) => () => state.currentUser,
    getFeatureToggle: (state) => () => state.featureToggle,
    getHighlightNoteId: (state) => () => state.highlightNoteId,
    getHighlightNote: (state) => () => withState(state).getNoteById(state.highlightNoteId),
    getNoteById: (state) => (id) => withState(state).getNoteById(id),
    getChildrenIdsByParentId: (state) => (parentId) => withState(state).getChildrenIdsByParentId(parentId),
    getChildrenOfParentId: (state) => (parentId) => withState(state).getChildrenOfParentId(parentId),
  },

  mutations: {
    loadNotes(state, notes) {
      notes.forEach((note) => {
        state.notes[note.id] = note;
      });
    },
    currentUser(state, user) {
      state.currentUser = user
    },
    featureToggle(state, ft) {
      state.featureToggle = ft
    },
    highlightNoteId(state, value) {
      state.highlightNoteId = value
    }
  },
});
