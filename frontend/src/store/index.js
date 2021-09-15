import { createStore } from 'vuex'
import note from './modules/note'

export default createStore({
  modules: {
    note,
  },
})