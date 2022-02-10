const initUndoHistory = (store, notes) => {
  store.commit("initUndoHistory", notes);
}

const addUndoHistory = (store, params) => {
  store.commit("addUndoHistory", params);
}

const popUndoHistory = (store, id) => {
  store.commit('popUndoHistory', id);
}

export default {
  initUndoHistory,
  addUndoHistory,
  popUndoHistory
}
