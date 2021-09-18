import { merge } from "lodash";
import { createStore } from "vuex";

export default createStore({
  state: () => ({
    notes: {},
    childrens: {},
  }),

  getters: {
    getNoteById: (state) => (id) => state.notes[id],
    getChildrenOfParentId: (state) => (parentId) =>
      !state.childrens[parentId]
        ? []
        : state.childrens[parentId].map((c) => state.notes[c]),
  },

  mutations: {
    loadNotes(state, notes) {
      notes.forEach((note) => {
        state.notes[note.id] = note;
      });
    },

    loadParentChildren(state, parentChildren) {
      merge(state.childrens, parentChildren);
    },
  },
});
