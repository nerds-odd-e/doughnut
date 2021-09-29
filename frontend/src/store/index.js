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
        .map(nvb=>nvb ? nvb.note : null)
        .filter(n=>n)
    }
  }
}

export default createStore({
  state: () => ({
    notes: {},
  }),

  getters: {
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
  },
});
