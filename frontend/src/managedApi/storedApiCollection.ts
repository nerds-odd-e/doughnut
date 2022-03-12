import ManagedApi from "./ManagedApi";
import createPiniaStore from "../store/createPiniaStore";

const storedApiCollection = (managedApi: ManagedApi, piniaStore: ReturnType<typeof createPiniaStore>) => {
  function loadReviewPointViewedByUser(data: Generated.ReviewPointViewedByUser) {
    if (!data) return;
    const { noteWithPosition, linkViewedByUser } = data;
    if (noteWithPosition) {
      piniaStore.loadNoteSpheres([noteWithPosition.note]);
    }
    if (linkViewedByUser) {
      piniaStore.loadNoteSpheres([linkViewedByUser.sourceNoteWithPosition.note])
      piniaStore.loadNoteSpheres([linkViewedByUser.targetNoteWithPosition.note])
    }
  }

  async function updateTextContentWithoutUndo(noteId: number, noteContentData: any) {
    const { updatedAt, ...data } = noteContentData;
    const res = await managedApi.restPatchMultiplePartForm(
      `text_content/${noteId}`,
      data
    ) as Generated.NoteSphere;
    piniaStore.loadNoteSpheres([res]);
    return res;
  }

  return {
    reviewMethods: {
      async getOneInitialReview() {
        const res = await managedApi.restGet(`reviews/initial`) as Generated.ReviewPointViewedByUser;
        loadReviewPointViewedByUser(res);
        return res;
      },

      async doInitialReview(data: any) {
        const res = await managedApi.restPost(`reviews`, data) as Generated.ReviewPointViewedByUser;
        loadReviewPointViewedByUser(res);
        return res;
      },

      async selfEvaluate(reviewPointId: number, data: any) {
        const res = await managedApi.restPost(
          `reviews/${reviewPointId}/self-evaluate`,
          data
        ) as Generated.RepetitionForUser;
        loadReviewPointViewedByUser(res.reviewPointViewedByUser);
        return res;
      },

      async getNextReviewItem() {
        const res = await managedApi.restGet(`reviews/repeat`) as Generated.RepetitionForUser
        loadReviewPointViewedByUser(res.reviewPointViewedByUser);
        return res;
      },
    },

    async getNoteWithDescendents(noteId: number) {
      const res = await managedApi.restGet(`notes/${noteId}/overview`) as Generated.NotesBulk;
      piniaStore.loadNoteSpheres(res.notes);
      return res;
    },

    async getNoteAndItsChildren(noteId: number) {
      const res = await managedApi.restGet(`notes/${noteId}`) as Generated.NotesBulk;
      piniaStore.loadNoteSpheres(res.notes);
      return res;
    },

    async getNotebooks() {
      const res = await managedApi.restGet(`notebooks`) as Generated.NotebooksViewedByUser
      piniaStore.setNotebooks(res.notebooks);
      return res;
    },

    async createNotebook(circle: any, data: any) {
      const url = (() => {
        if (circle) {
          return `circles/${circle.id}/notebooks`;
        }
        return `notebooks/create`;
      })();

      const res = await managedApi.restPostMultiplePartForm(url, data);
      return res;
    },

    async createNote(parentId: number, data: any) {
      const res = await managedApi.restPostMultiplePartForm(
        `notes/${parentId}/create`,
        data
      ) as Generated.NotesBulk;
      piniaStore.loadNoteSpheres(res.notes);
      return res;
    },

    async createLink(sourceId: number, targetId: number, data: any) {
      const res = await managedApi.restPost(
        `links/create/${sourceId}/${targetId}`,
        data
      ) as Generated.NotesBulk;
      piniaStore.loadNoteSpheres(res.notes);
      return res;
    },

    async updateLink(linkId: number, data: any) {
      const res = await managedApi.restPost(`links/${linkId}`, data) as Generated.NotesBulk;
      piniaStore.loadNoteSpheres(res.notes);
      return res;
    },

    async deleteLink(linkId: number) {
      const res = await managedApi.restPost(`links/${linkId}/delete`, {}) as Generated.NotesBulk;
      piniaStore.loadNoteSpheres(res.notes);
      return res;
    },

    async updateNote(noteId: number, noteContentData: any) {
      const { updatedAt, ...data } = noteContentData;
      const res = await managedApi.restPatchMultiplePartForm(
        `notes/${noteId}`,
        data
      ) as Generated.NoteSphere;
      piniaStore.loadNoteSpheres([res]);
      return res;
    },

    async updateTextContent(noteId: number, noteContentData: Generated.TextContent) {
      piniaStore.addEditingToUndoHistory({ noteId });
      return updateTextContentWithoutUndo(noteId, noteContentData);
    },

    async undo() {
      const history = piniaStore.peekUndo();
      piniaStore.popUndoHistory();
      if (history.type === 'editing') {
        return updateTextContentWithoutUndo(
          history.noteId,
          history.textContent
        );
      }
      const res = await managedApi.restPatch(
        `notes/${history.noteId}/undo-delete`,
        {}
      ) as Generated.NotesBulk;
      piniaStore.loadNoteSpheres(res.notes);
      if (res.notes[0].note.parentId === null) {
        this.getNotebooks();
      }
      return res;
    },

    async deleteNote(noteId: number) {
      const res = await managedApi.restPost(
        `notes/${noteId}/delete`,
        {},
      );
      piniaStore.deleteNote(noteId);
      return res;
    },

    async getCurrentUserInfo() {
      const res = await managedApi.restGet(`user/current-user-info`) as Generated.CurrentUserInfo;
      piniaStore.setCurrentUser(res.user);
      return res;
    },

    async updateUser(userId: number, data: any) {
      const res = await managedApi.restPatchMultiplePartForm(
        `user/${userId}`,
        data
      ) as Generated.User;
      piniaStore.setCurrentUser(res);
      return res;
    },

    async createUser(data: any) {
      const res = await managedApi.restPostMultiplePartForm(`user`, data) as Generated.User;
      piniaStore.setCurrentUser(res);
      return res;
    },

    getFeatureToggle() {
      return (
        !window.location.href.includes('odd-e.com') &&
        managedApi
          .restGet(`testability/feature_toggle`)
          .then((res) => piniaStore.setFeatureToggle(res as boolean))
      );
    },

    async setFeatureToggle(data: boolean) {
      const res = await managedApi.restPost(`testability/feature_toggle`, {
        enabled: data,
      });
      this.getFeatureToggle();
      return res;
    },

    getCircle(circleId: number) {
      return managedApi.restGet(`circles/${circleId}`);
    },
  };
};

export default storedApiCollection;
