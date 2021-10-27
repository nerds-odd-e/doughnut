import { restGet, restPost, restPatchMultiplePartForm, restPostMultiplePartForm } from "../restful/restful";

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

const storedApiCreateNote = async (store, parentId, data) => {
    const res = await restPostMultiplePartForm(
        `/api/notes/${parentId}/create`,
        data,
        () => null
    )
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
        () => null
      )
  store.commit("currentUser", res)
  return res
}

const storedApiCreateUser = async (store, data) => {
  const res = await restPostMultiplePartForm(
        `/api/user`,
        data,
        () => null
      )
  store.commit("currentUser", res)
  return res
}

const storedApiGetFeatureToggle = (store) =>
 restGet(`/api/testability/feature_toggle`).then((res) => store.commit("featureToggle", res))

const apiSelfEvaluate = async (reviewPointId, data) => {
  const res = await restPost(
        `/api/reviews/${reviewPointId}/self-evaluate`,
        data,
        () => null
      )
  return res
}

const apiProcessAnswer = async (reviewPointId, data) => {
  const res = await restPost(
        `/api/reviews/${reviewPointId}/answer`,
        data,
        () => null
      )
  return res
}

const apiGetNextReviewItem = async () => await restGet(`/api/reviews/repeat`)

export {
    apiLogout,
    apiGetNextReviewItem,
    apiSelfEvaluate,
    apiProcessAnswer,
    storedApiGetNoteWithDescendents,
    storedApiGetNoteAndItsChildren,
    storedApiCreateNote,
    storedApiSplitNote,
    storedApiGetCurrentUserInfo,
    storedApiUpdateUser,
    storedApiCreateUser,
    storedApiGetFeatureToggle,
}
