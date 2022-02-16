import { restGet, restPatchMultiplePartForm, restPost, restPatch, restPostMultiplePartForm } from "../restful/restful";

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

function loadReviewPointViewedByUser(store, data) {
    if(!data) return
    const { noteWithPosition, linkViewedbyUser } = data
    if (noteWithPosition) {
       store.commit("loadNotes", [noteWithPosition.note]);
    }
    if(linkViewedbyUser){
      loadReviewPointViewedByUser(store, {noteWithPosition: linkViewedbyUser.sourceNoteWithPosition})
      loadReviewPointViewedByUser(store, {noteWithPosition: linkViewedbyUser.targetNoteWithPosition})
    }
}

const storedApiGetOneInitialReview = async (store) => {
    const res = await restGet(
        `/api/reviews/initial`);
    loadReviewPointViewedByUser(store, res)

    return res;
}

const storedApiDoInitialReview = async (store, data) => {
    const res = await restPost(
        `/api/reviews`,
        data,
        () => null
    )
    loadReviewPointViewedByUser(store, res)
    return res;
}

const storedApiGetNotebooks = async (store) => {
  const res = await restGet(`/api/notebooks`)
  store.commit("notebooks", res.notebooks)
  return res
}

const storedApiCreateNotebook = async (store, circle, data) => {
    const url = (() =>{
        if (circle) {
            return `/api/circles/${circle.id}/notebooks`;
        }
        return `/api/notebooks/create`;
        })();

    const res = await restPostMultiplePartForm(
        url,
        data,
        () => null
    )
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

const storedApiUpdateNote = async (store, noteId, noteContentData) => {
    const { updatedAt, ...data } = noteContentData
    const res = await restPatchMultiplePartForm(
        `/api/notes/${noteId}`,
        data,
        () => null
    )
    store.commit("loadNotes", [res]);
    return res;
}

const storedApiUpdateTextContent = async (store, noteId, noteContentData) => {
    const { updatedAt, ...data } = noteContentData
    const res = await restPatchMultiplePartForm(
        `/api/text_content/${noteId}`,
        data,
        () => null
    )
    store.commit("loadNotes", [res]);
    return res;
}

const storedApiDeleteNote = async (store, noteId) => {
    const res = await restPost(
        `/api/notes/${noteId}/delete`,
        {},
        () => null
    )
    store.commit("deleteNote", noteId)
    return res;
}

const storedApiUndoDeleteNote = async (store) => {
    const deletedNoteId = store.getters.getLastDeletedNoteId()
    store.commit('popUndoHistory1')
    const res = await restPatch(
        `/api/notes/${deletedNoteId}/undo-delete`,
        {},
    )
    store.commit("loadNotes", res.notes)
    if(res.notes[0].parentId === null) {
        storedApiGetNotebooks(store)
    }
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

const storedApiSetFeatureToggle = async (store, data) => {
  const res = await restPost(
        `/api/testability/feature_toggle`,
        {enabled: data},
        () => null
      )
  storedApiGetFeatureToggle(store)
  return res
}

const storedApiSelfEvaluate = async (store, reviewPointId, data) => {
  const res = await restPost(
        `/api/reviews/${reviewPointId}/self-evaluate`,
        data,
        () => null
      )
  loadReviewPointViewedByUser(store, res.reviewPointViewedByUser)
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

const apiRemoveFromReview = async (reviewPointId) => {
  const res = await restPost(
        `/api/review-points/${reviewPointId}/remove`,
        {},
        () => null
      )
  return res
}

const storedApiGetNextReviewItem = async (store) => {
    const res = await restGet(`/api/reviews/repeat`)
    loadReviewPointViewedByUser(store, res.reviewPointViewedByUser)
    return res
}

export {
    apiLogout,
    apiProcessAnswer,
    apiRemoveFromReview,

    storedApiGetNextReviewItem,
    storedApiSelfEvaluate,
    storedApiGetNotebooks,
    storedApiCreateNotebook,
    storedApiGetNoteWithDescendents,
    storedApiGetNoteAndItsChildren,
    storedApiCreateNote,
    storedApiUpdateNote,
    storedApiUpdateTextContent,
    storedApiDeleteNote,
    storedApiUndoDeleteNote,
    storedApiSplitNote,
    storedApiGetCurrentUserInfo,
    storedApiUpdateUser,
    storedApiCreateUser,
    storedApiGetOneInitialReview,
    storedApiDoInitialReview,
    storedApiGetFeatureToggle,
    storedApiSetFeatureToggle,
}