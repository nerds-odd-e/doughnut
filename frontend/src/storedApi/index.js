import { restGet, restPost, restPatchMultiplePartForm } from "../restful/restful";

const apiLogout = async () => {
    await restPost(`/logout`, {})
}


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

const storedApiGetCurrentUserInfo = async (store) => {
  const res = await restGet(`/api/user/current-user-info`)
  store.commit("currentUser", res.user);
  return res
}

const storedApiUpdateUser = async (store, userId, data) => {
  const res = await restPatchMultiplePartForm(
        `/api/user/${userId}`,
        data,
        () => {}
      )
  store.commit("currentUser", res)
  return res
}

const apiGetFeatureToggle = () => restGet(`/api/testability/feature_toggle`)

export {
    apiLogout,
    storedApiGetNoteWithDescendents,
    storedApiGetNoteAndItsChildren,
    storedApiSplitNote,
    storedApiGetCurrentUserInfo,
    storedApiUpdateUser,
    apiGetFeatureToggle,
}
