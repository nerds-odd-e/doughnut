import { createStore } from "vuex";
import useStore from "./pinia_store";

export default ()=>createStore({
  state: () => ({
    piniaStore: useStore(),
  }),

  getters: {
    ps: (state) => () => state.piniaStore,
  },
});
