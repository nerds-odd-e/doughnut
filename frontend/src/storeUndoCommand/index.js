const addEditingToUndoHistory = (store, note) => {
  store.commit("addEditingToUndoHistory", note);
}

export default {
  addEditingToUndoHistory,
}
