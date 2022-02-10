const initUndoHistory = (store, notes) => {
    store.commit("initUndoHistory", notes);
}

const addUndoHistory = (store, params) => {
    store.commit("addUndoHistory", params);
}

export default {
    initUndoHistory,
    addUndoHistory,
}