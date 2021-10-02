import { restGet } from "../restful/restful";

const storedApiGetNoteWithDescendents = async (store, noteId) => {
    const res = await restGet(
        `/api/notes/${noteId}/overview`);
    store.commit("loadNotes", res.notes);
    return res;
}

const storedApiGetNoteAndItsChildren = async (store, noteId) => {
    const res = await restGet(
        `/api/notes/${noteId}`);
    store.commit("loadNotes", res.notes);
    return res;
}

const apiGetCurrentUserInfo = async () => {
    return restGet(`/api/current-user-info`);
}

export {
    storedApiGetNoteWithDescendents,
    storedApiGetNoteAndItsChildren,
    apiGetCurrentUserInfo,
}