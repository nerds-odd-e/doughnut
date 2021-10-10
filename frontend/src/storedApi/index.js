import { restGet, restPost } from "../restful/restful";

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

const storedApiSplitNote = async (store, noteId) => {
    const res = await restPost(`/api/notes/${noteId}/split`, {})
    store.commit("loadNotes", res.notes);
    return res;
}


const apiGetCurrentUserInfo = () => restGet(`/api/user/current-user-info`)

const apiGetFeatureToggle = () => restGet(`/api/testability/feature_toggle`)

export {
    storedApiGetNoteWithDescendents,
    storedApiGetNoteAndItsChildren,
    storedApiSplitNote,
    apiGetCurrentUserInfo,
    apiGetFeatureToggle,
}
