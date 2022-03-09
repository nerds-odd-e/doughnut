const storedApiCollection = (managedApi, piniaStore) => {
  function loadReviewPointViewedByUser(data) {
    if (!data) return;
    const { noteWithPosition, linkViewedbyUser } = data;
    if (noteWithPosition) {
      piniaStore.loadNotes([noteWithPosition.note]);
    }
    if (linkViewedbyUser) {
      loadReviewPointViewedByUser({
        noteWithPosition: linkViewedbyUser.sourceNoteWithPosition,
      });
      loadReviewPointViewedByUser({
        noteWithPosition: linkViewedbyUser.targetNoteWithPosition,
      });
    }
  }

  async function updateTextContentWithoutUndo(noteId, noteContentData) {
    const { updatedAt, ...data } = noteContentData;
    const res = await managedApi.restPatchMultiplePartForm(
      `text_content/${noteId}`,
      data
    );
    piniaStore.loadNotes([res]);
    return res;
  }

  return {
    reviewMethods: {
      async getOneInitialReview() {
        const res = await managedApi.restGet(`reviews/initial`);
        loadReviewPointViewedByUser(res);
        return res;
      },

      async doInitialReview(data) {
        const res = await managedApi.restPost(`reviews`, data);
        loadReviewPointViewedByUser(res);
        return res;
      },

      async selfEvaluate(reviewPointId, data) {
        const res = await managedApi.restPost(
          `reviews/${reviewPointId}/self-evaluate`,
          data
        );
        loadReviewPointViewedByUser(res.reviewPointViewedByUser);
        return res;
      },

      async getNextReviewItem() {
        const res = await managedApi.restGet(`reviews/repeat`);
        loadReviewPointViewedByUser(res.reviewPointViewedByUser);
        return res;
      },
    },

    async getNoteWithDescendents(noteId) {
      const res = await managedApi.restGet(`notes/${noteId}/overview`);
      piniaStore.loadNotes(res.notes);
      return res;
    },

    async getNoteAndItsChildren(noteId) {
      const res = await managedApi.restGet(`notes/${noteId}`);
      piniaStore.loadNotes(res.notes);
      return res;
    },

    async getNotebooks() {
      const res = await managedApi.restGet(`notebooks`);
      piniaStore.setNotebooks(res.notebooks);
      return res;
    },

    async createNotebook(circle, data) {
      const url = (() => {
        if (circle) {
          return `circles/${circle.id}/notebooks`;
        }
        return `notebooks/create`;
      })();

      const res = await managedApi.restPostMultiplePartForm(url, data);
      return res;
    },

    async createNote(parentId, data) {
      const res = await managedApi.restPostMultiplePartForm(
        `notes/${parentId}/create`,
        data
      );
      piniaStore.loadNotes(res.notes);
      return res;
    },

    async createLink(sourceId, targetId, data) {
      const res = await managedApi.restPost(
        `links/create/${sourceId}/${targetId}`,
        data
      );
      piniaStore.loadNotes(res.notes);
      return res;
    },

    async updateLink(linkId, data) {
      const res = await managedApi.restPost(`links/${linkId}`, data);
      piniaStore.loadNotes(res.notes);
      return res;
    },

    async deleteLink(linkId) {
      const res = await managedApi.restPost(`links/${linkId}/delete`, {});
      piniaStore.loadNotes(res.notes);
      return res;
    },

    async updateNote(noteId, noteContentData) {
      const { updatedAt, ...data } = noteContentData;
      const res = await managedApi.restPatchMultiplePartForm(
        `notes/${noteId}`,
        data
      );
      piniaStore.loadNotes([res]);
      return res;
    },

    async updateTextContent(noteId, noteContentData) {
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
      );
      piniaStore.loadNotes(res.notes);
      if (res.notes[0].parentId === null) {
        this.getNotebooks();
      }
      return res;
    },

    async deleteNote(noteId) {
      const res = await managedApi.restPost(
        `notes/${noteId}/delete`,
        {},
        () => null
      );
      piniaStore.deleteNote(noteId);
      return res;
    },

    async getCurrentUserInfo() {
      const res = await managedApi.restGet(`user/current-user-info`);
      piniaStore.setCurrentUser(res.user);
      return res;
    },

    async updateUser(userId, data) {
      const res = await managedApi.restPatchMultiplePartForm(
        `user/${userId}`,
        data
      );
      piniaStore.setCurrentUser(res);
      return res;
    },

    async createUser(data) {
      const res = await managedApi.restPostMultiplePartForm(`user`, data);
      piniaStore.setCurrentUser(res);
      return res;
    },

    getFeatureToggle() {
      return (
        !window.location.href.includes('odd-e.com') &&
        managedApi
          .restGet(`testability/feature_toggle`)
          .then((res) => piniaStore.setFeatureToggle(res))
      );
    },

    async setFeatureToggle(data) {
      const res = await managedApi.restPost(`testability/feature_toggle`, {
        enabled: data,
      });
      this.getFeatureToggle();
      return res;
    },

    getCircle(circleId) {
      return managedApi.restGet(`circles/${circleId}`);
    },
  };
};

export default storedApiCollection;
