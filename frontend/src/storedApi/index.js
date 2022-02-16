import { restGet, restPatchMultiplePartForm, restPost, restPatch, restPostMultiplePartForm } from "../restful/restful";

const storedApi = (store) => {
    function loadReviewPointViewedByUser(data) {
        if(!data) return
        const { noteWithPosition, linkViewedbyUser } = data
        if (noteWithPosition) {
          store.commit("loadNotes", [noteWithPosition.note]);
        }
        if(linkViewedbyUser){
            loadReviewPointViewedByUser({noteWithPosition: linkViewedbyUser.sourceNoteWithPosition})
            loadReviewPointViewedByUser({noteWithPosition: linkViewedbyUser.targetNoteWithPosition})
        }
    }

    async function undoDeleteNote(deletedNoteId) {
        const res = await restPatch(
            `/api/notes/${deletedNoteId}/undo-delete`,
            {},
        )
        store.commit("loadNotes", res.notes)
        if(res.notes[0].parentId === null) {
            this.getNotebooks(store)
        }
        return res;
    }

    async function updateTextContentWithoutUndo(noteId, noteContentData)  {
        const { updatedAt, ...data } = noteContentData
        const res = await restPatchMultiplePartForm(
            `/api/text_content/${noteId}`,
            data,
            () => null
        )
        store.commit("loadNotes", [res]);
        return res;
    }

    return {
        reviewMethods: {
            async getOneInitialReview()  {
                const res = await restGet(`/api/reviews/initial`);
                loadReviewPointViewedByUser(res)
                return res;
            },

            async doInitialReview(data)  {
                const res = await restPost(
                    `/api/reviews`,
                    data,
                    () => null
                )
                loadReviewPointViewedByUser(res)
                return res;
            },

            async selfEvaluate(reviewPointId, data)  {
                const res = await restPost(
                        `/api/reviews/${reviewPointId}/self-evaluate`,
                        data,
                        () => null
                    )
                loadReviewPointViewedByUser(res.reviewPointViewedByUser)
                return res
            },

            async getNextReviewItem()  {
                const res = await restGet(`/api/reviews/repeat`)
                loadReviewPointViewedByUser(res.reviewPointViewedByUser)
                return res
            },
        },

        async getNoteWithDescendents(noteId) {
            const res = await restGet(
                `/api/notes/${noteId}/overview`);
            store.commit("loadNotes", res.notes);
            return res;
        },

        async getNoteAndItsChildren(noteId)  {
            const res = await restGet(
                `/api/notes/${noteId}`);
            store.commit("loadNotes", res.notes);
            return res;
        },

        async getNotebooks()  {
        const res = await restGet(`/api/notebooks`)
        store.commit("notebooks", res.notebooks)
        return res
        },

        async createNotebook(circle, data)  {
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
        },

        async createNote(parentId, data)  {
            const res = await restPostMultiplePartForm(
                `/api/notes/${parentId}/create`,
                data,
                () => null
            )
            store.commit("loadNotes", res.notes);
            return res;
        },

        async updateNote(noteId, noteContentData)  {
            const { updatedAt, ...data } = noteContentData
            const res = await restPatchMultiplePartForm(
                `/api/notes/${noteId}`,
                data,
                () => null
            )
            store.commit("loadNotes", [res]);
            return res;
        },

        async updateTextContent(noteId, noteContentData)  {
            store.commit('addEditingToUndoHistory',  { noteId });
            return updateTextContentWithoutUndo(noteId, noteContentData)
        },

        async undo()  {
            const history = store.getters.peekUndo()
            store.commit('popUndoHistory')
            if(history.type==='editing') {
                return updateTextContentWithoutUndo(history.noteId, history.textContent)
            }
            return undoDeleteNote(history.noteId)
        },

        async deleteNote(noteId)  {
            const res = await restPost(
                `/api/notes/${noteId}/delete`,
                {},
                () => null
            )
            store.commit("deleteNote", noteId)
            return res;
        },

        async splitNote(noteId)  {
            const res = await restPost(`/api/notes/${noteId}/split`, {})
            store.commit("loadNotes", res.notes);
            return res;
        },

        async getCurrentUserInfo()  {
        const res = await restGet(`/api/user/current-user-info`)
        store.commit("currentUser", res.user);
        return res
        },

        async updateUser(userId, data)  {
        const res = await restPatchMultiplePartForm(
                `/api/user/${userId}`,
                data,
                () => null
            )
        store.commit("currentUser", res)
        return res
        },

        async createUser(data)  {
        const res = await restPostMultiplePartForm(
                `/api/user`,
                data,
                () => null
            )
        store.commit("currentUser", res)
        return res
        },

        getFeatureToggle() {
            return restGet(`/api/testability/feature_toggle`).then((res) => store.commit("featureToggle", res))
        },

        async setFeatureToggle(data)  {
            const res = await restPost(
                    `/api/testability/feature_toggle`,
                    {enabled: data},
                    () => null
                )
            this.getFeatureToggle(store)
            return res
        },

    }
}

const apiLogout = async () => {
    await restPost(`/logout`, {})
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

export {
    apiLogout,
    apiProcessAnswer,
    apiRemoveFromReview,

    storedApi,
}