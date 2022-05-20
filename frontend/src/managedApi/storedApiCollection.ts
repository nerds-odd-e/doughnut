import ManagedApi from "./ManagedApi";
import createPiniaStore from "../store/createPiniaStore";

const storedApiCollection = (
  managedApi: ManagedApi,
  piniaStore: ReturnType<typeof createPiniaStore>
) => {
  async function updateTextContentWithoutUndo(
    noteId: Doughnut.ID,
    noteContentData: Generated.TextContent
  ) {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { updatedAt, ...data } = noteContentData;
    return (await managedApi.restPatchMultiplePartForm(
      `text_content/${noteId}`,
      data
    )) as Generated.NoteRealm;
  }

  return {
    reviewMethods: {
      async getReviewPoint(reviewPointId: Doughnut.ID) {
        return (await managedApi.restGet(
          `review-points/${reviewPointId}`
        )) as Generated.ReviewPoint;
      },

      async initialReview() {
        return (await managedApi.restGet(
          `reviews/initial`
        )) as Generated.ReviewPointWithReviewSetting[];
      },

      async doInitialReview(data: Generated.InitialInfo) {
        return (await managedApi.restPost(
          `reviews`,
          data
        )) as Generated.ReviewPoint;
      },

      async processAnswer(data: Generated.Answer) {
        const res = (await managedApi.restPost(
          `reviews/answer`,
          data
        )) as Generated.AnswerResult;
        return res;
      },

      async getAnswer(answerId: Doughnut.ID) {
        return (await managedApi.restGet(
          `reviews/answers/${answerId}`
        )) as Generated.AnswerViewedByUser;
      },

      async selfEvaluate(
        reviewPointId: Doughnut.ID,
        data: Generated.SelfEvaluation
      ) {
        const res = (await managedApi.restPost(
          `reviews/${reviewPointId}/self-evaluate`,
          data
        )) as Generated.ReviewPoint;
        return res;
      },

      async getNextReviewItem() {
        const res = (await managedApi.restGet(
          `reviews/repeat`
        )) as Generated.RepetitionForUser;
        return res;
      },
    },

    testability: {
      getFeatureToggle() {
        return (
          !window.location.href.includes("odd-e.com") &&
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

      async setRandomizer(data: string) {
        const res = await managedApi.restPost(`testability/randomizer`, {
          choose: data,
        });
        return res;
      },
    },

    async getNoteWithDescendents(noteId: Doughnut.ID) {
      return (await managedApi.restGet(
        `notes/${noteId}/overview`
      )) as Generated.NoteRealmWithAllDescendants;
    },

    async getNoteRealmWithPosition(noteId: Doughnut.ID) {
      return (await managedApi.restGet(
        `notes/${noteId}`
      )) as Generated.NotesBulk;
    },

    async getNotePosition(noteId: Doughnut.ID) {
      return (await managedApi.restGet(
        `notes/${noteId}/position`
      )) as Generated.NotePositionViewedByUser;
    },

    async createNotebook(
      circle: Generated.Circle | undefined,
      data: Generated.Notebook
    ) {
      const url = (() => {
        if (circle) {
          return `circles/${circle.id}/notebooks`;
        }
        return `notebooks/create`;
      })();

      const res = await managedApi.restPostMultiplePartForm(url, data);
      return res;
    },

    async createNote(parentId: Doughnut.ID, data: Generated.NoteCreation) {
      return (await managedApi.restPostMultiplePartForm(
        `notes/${parentId}/create`,
        data
      )) as Generated.NotesBulk;
    },

    async createLink(
      sourceId: Doughnut.ID,
      targetId: Doughnut.ID,
      data: Generated.LinkRequest
    ) {
      return (await managedApi.restPost(
        `links/create/${sourceId}/${targetId}`,
        data
      )) as Generated.NotesBulk;
    },

    async updateLink(linkId: Doughnut.ID, data: Generated.LinkRequest) {
      return (await managedApi.restPost(
        `links/${linkId}`,
        data
      )) as Generated.NotesBulk;
    },

    async deleteLink(linkId: Doughnut.ID) {
      return (await managedApi.restPost(
        `links/${linkId}/delete`,
        {}
      )) as Generated.NotesBulk;
    },

    async updateNote(
      noteId: Doughnut.ID,
      noteContentData: Generated.NoteAccessories
    ) {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const { updatedAt, ...data } = noteContentData;
      return (await managedApi.restPatchMultiplePartForm(
        `notes/${noteId}`,
        data
      )) as Generated.NoteRealm;
    },

    async updateTextContent(
      noteId: Doughnut.ID,
      noteContentData: Generated.TextContent,
      oldContent: Generated.TextContent
    ) {
      piniaStore.addEditingToUndoHistory(noteId, oldContent);
      return updateTextContentWithoutUndo(noteId, noteContentData);
    },

    async undo() {
      const history = piniaStore.peekUndo();
      if (!history) throw new Error("undo history is empty");
      piniaStore.popUndoHistory();
      if (history.type === "editing" && history.textContent) {
        return updateTextContentWithoutUndo(
          history.noteId,
          history.textContent
        );
      }
      return (await managedApi.restPatch(
        `notes/${history.noteId}/undo-delete`,
        {}
      )) as Generated.NoteRealm;
    },

    async deleteNote(noteId: Doughnut.ID) {
      const res = await managedApi.restPost(`notes/${noteId}/delete`, {});
      piniaStore.deleteNote(noteId);
      return res;
    },

    async getCurrentUserInfo() {
      const res = (await managedApi.restGet(
        `user/current-user-info`
      )) as Generated.CurrentUserInfo;
      piniaStore.setCurrentUser(res.user);
      return res;
    },

    async updateUser(userId: Doughnut.ID, data: Generated.User) {
      const res = (await managedApi.restPatchMultiplePartForm(
        `user/${userId}`,
        data
      )) as Generated.User;
      piniaStore.setCurrentUser(res);
      return res;
    },

    async createUser(data: Generated.User) {
      const res = (await managedApi.restPostMultiplePartForm(
        `user`,
        data
      )) as Generated.User;
      piniaStore.setCurrentUser(res);
      return res;
    },

    async getCircle(circleId: Doughnut.ID) {
      return (await managedApi.restGet(
        `circles/${circleId}`
      )) as Generated.CircleForUserView;
    },
  };
};

export default storedApiCollection;
