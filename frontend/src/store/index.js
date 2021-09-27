import { merge } from "lodash";
import { createStore } from "vuex";

export default createStore({
  state: () => ({
    notes: {},
  }),

  getters: {
    getNoteById: (state) => (id) => state.notes[id],
    getChildrenIdsByParentId: (state) => (parentId) =>
      !state.notes[parentId]
        ? []
        : state.notes[parentId].childrenIds
  },

  mutations: {
    loadNotes(state, notes) {
      notes.forEach((note) => {
        state.notes[note.id] = note;
      });
    },
  },
});
