import { createStore } from 'vuex'

export default createStore({
  state: () => ({
    all: {}
  }),

  getters: {
    getNoteById: (state) => (id) => state.all[id]
  },
  
  mutations: {
    addNote (state, note) {
      state.all[note.note.id] = note
    },
  },
})