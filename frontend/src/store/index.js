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
    },

    deleteNote(id) {
      this.getChildrenIdsByParentId(id).forEach(cid=>
        this.deleteNote(cid))
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

export default createStore({
  state: () => ({
    notes: {},
    currentUser: null,
    featureToggle: false,
    environment: 'production',
  }),

  getters: {
    getCurrentUser: (state) => () => state.currentUser,
    getEnvironment: (state) => () => state.environment,
    getFeatureToggle: (state) => () => state.featureToggle,
    getNoteById: (state) => (id) => withState(state).getNoteById(id),
    getChildrenIdsByParentId: (state) => (parentId) => withState(state).getChildrenIdsByParentId(parentId),
    getChildrenOfParentId: (state) => (parentId) => withState(state).getChildrenOfParentId(parentId),
    getCurrentLanguage: (state) => () => state.currentLanguage,
  },

  mutations: {
    loadNotes(state, notes) {
      notes.forEach((note) => {
        state.notes[note.id] = note;
      });
    },
    deleteNote(state, noteId) {
      withState(state).deleteNoteFromParentChildrenList(noteId)
      withState(state).deleteNote(noteId)
    },
    currentUser(state, user) {
      state.currentUser = user
    },
    featureToggle(state, ft) {
      state.environment = "testing"
      state.featureToggle = ft
    },
    changeNotesLanguage(state, language) {
      state.currentLanguage = language;
    }
  },
});
