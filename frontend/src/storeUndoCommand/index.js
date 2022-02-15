const addUndoHistory = (store, note) => {
  store.commit("addUndoHistory", note);
}

const popUndoHistory = (store) => {
  store.commit('popUndoHistory');
}

export default {
  addUndoHistory,
  popUndoHistory
}
