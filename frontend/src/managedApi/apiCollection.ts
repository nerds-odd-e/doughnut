import ManagedApi from "./ManagedApi";

const apiCollection = (managedApi: ManagedApi) => ({
  userMethods: {
    logout() {
      return managedApi.restPostWithHtmlResponse(`/logout`, {});
    },

    currentUser() {
      return managedApi.restGet(`user`);
    },

    async getCurrentUserInfo() {
      const res = (await managedApi.restGet(
        `user/current-user-info`
      )) as Generated.CurrentUserInfo;
      return res;
    },

    async updateUser(userId: Doughnut.ID, data: Generated.User) {
      const res = (await managedApi.restPatchMultiplePartForm(
        `user/${userId}`,
        data
      )) as Generated.User;
      return res;
    },

    async createUser(data: Generated.User) {
      return (await managedApi.restPostMultiplePartForm(
        `user`,
        data
      )) as Generated.User;
    },
  },
  reviewMethods: {
    async removeFromReview(reviewPointId: Doughnut.ID) {
      return (await managedApi.restPost(
        `review-points/${reviewPointId}/remove`,
        {}
      )) as Generated.ReviewPoint;
    },

    async overview() {
      return (await managedApi.restGet(
        `reviews/overview`
      )) as Generated.ReviewStatus;
    },

    updateReviewSetting(
      noteId: Doughnut.ID,
      data: Omit<Generated.ReviewSetting, "id">
    ) {
      return managedApi.restPost(`notes/${noteId}/review-setting`, data);
    },

    async getReviewPoint(reviewPointId: Doughnut.ID) {
      return (await managedApi.restGet(
        `review-points/${reviewPointId}`
      )) as Generated.ReviewPoint;
    },

    async initialReview() {
      return (await managedApi.restGet(
        `reviews/initial`
      )) as Generated.ReviewPoint[];
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
        `review-points/${reviewPointId}/self-evaluate`,
        data
      )) as Generated.ReviewPoint;
      return res;
    },

    async getDueReviewPoints(dueInDays?: number) {
      const res = (await managedApi.restGet(
        `reviews/repeat?dueindays=${dueInDays ?? ""}`
      )) as Generated.DueReviewPoints;
      return res;
    },

    async getRandomQuestionForReviewPoint(reviewPointId: Doughnut.ID) {
      const res = (await managedApi.restGet(
        `review-points/${reviewPointId}/random-question`
      )) as Generated.QuizQuestionViewedByUser;
      return res;
    },
  },
  circleMethods: {
    async getCircle(circleId: Doughnut.ID) {
      return (await managedApi.restGet(
        `circles/${circleId}`
      )) as Generated.CircleForUserView;
    },
    async createCircle(data: Generated.Circle) {
      return (await managedApi.restPostMultiplePartForm(
        "circles",
        data
      )) as Generated.Circle;
    },
    joinCircle(data: Generated.CircleJoiningByInvitation) {
      return managedApi.restPostMultiplePartForm(`circles/join`, data);
    },
    async getCirclesOfCurrentUser() {
      return (await managedApi.restGet("circles")) as Generated.Circle[];
    },
  },

  async relativeSearch(searchTerm: Generated.SearchTerm) {
    return (await managedApi.restPost(
      `notes/search`,
      searchTerm
    )) as Generated.Note[];
  },

  async getBazaar() {
    return (await managedApi.restGet(
      "bazaar"
    )) as Generated.NotebooksViewedByUser;
  },
  shareToBazaar(notebookId: Doughnut.ID) {
    return managedApi.restPost(`notebooks/${notebookId}/share`, {});
  },

  getFailureReports() {
    return managedApi.restGet("failure-reports");
  },
  getFailureReport(failureReportId: Doughnut.ID) {
    return managedApi.restGet(`failure-reports/${failureReportId}`);
  },
  subscriptionMethods: {
    subscribe(notebookId: Doughnut.ID, data: Generated.Subscription) {
      return managedApi.restPostMultiplePartForm(
        `subscriptions/notebooks/${notebookId}/subscribe`,
        data
      );
    },
    updateSubscription(
      subscriptionId: Doughnut.ID,
      data: Generated.Subscription
    ) {
      return managedApi.restPostMultiplePartForm(
        `subscriptions/${subscriptionId}`,
        data
      );
    },
    deleteSubscription(subscriptionId: Doughnut.ID) {
      return managedApi.restPost(`subscriptions/${subscriptionId}/delete`, {});
    },
  },
  async getNoteInfo(noteId: Doughnut.ID) {
    return (await managedApi.restGet(
      `notes/${noteId}/note-info`
    )) as Generated.NoteInfo;
  },

  notebookMethods: {
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

    async getNotebooks() {
      return (await managedApi.restGet(
        `notebooks`
      )) as Generated.NotebooksViewedByUser;
    },

    updateNotebookSettings(notebookId: Doughnut.ID, data: Generated.Notebook) {
      return managedApi.restPostMultiplePartForm(
        `notebooks/${notebookId}`,
        data
      );
    },
  },

  noteMethods: {
    async getNoteWithDescendents(noteId: Doughnut.ID) {
      return (await managedApi.restGet(
        `notes/${noteId}/overview`
      )) as Generated.NoteRealmWithAllDescendants;
    },

    async createNote(parentId: Doughnut.ID, data: Generated.NoteCreation) {
      return (await managedApi.restPostMultiplePartForm(
        `notes/${parentId}/create`,
        data
      )) as Generated.NoteRealmWithPosition;
    },
    async getNoteRealmWithPosition(noteId: Doughnut.ID) {
      return (await managedApi.restGet(
        `notes/${noteId}`
      )) as Generated.NoteRealmWithPosition;
    },

    async getNotePosition(noteId: Doughnut.ID) {
      return (await managedApi.restGet(
        `notes/${noteId}/position`
      )) as Generated.NotePositionViewedByUser;
    },
  },
  wikidata: {
    async updateWikidataId(
      noteId: Doughnut.ID,
      data: Generated.WikidataAssociationCreation
    ) {
      return (await managedApi.restPost(
        `notes/${noteId}/updateWikidataId`,
        data
      )) as Generated.NoteRealm;
    },

    async getWikidataEntityById(wikidataId: string) {
      return (await managedApi.restGet(
        `wikidata/entity-data/${wikidataId}`
      )) as Generated.WikidataEntityData;
    },

    async getWikidatas(keyword: string) {
      return (await managedApi.restGet(
        `wikidata/search/${keyword}`
      )) as Generated.WikidataSearchEntity[];
    },
  },
  ai: {
    async keepAskingAISuggestionUntilStop(
      prompt: string,
      noteId: Doughnut.ID,
      prev?: string,
      interimResultShouldContinue?: (suggestion: string) => boolean
    ): Promise<string> {
      const res = await this.askAiSuggestions(
        {
          prompt,
          incompleteAssistantMessage: prev ?? "",
        },
        noteId
      );
      if (interimResultShouldContinue) {
        if (!interimResultShouldContinue(res.suggestion)) return res.suggestion;
      }
      if (res.finishReason === "length") {
        return this.keepAskingAISuggestionUntilStop(
          prompt,
          noteId,
          res.suggestion,
          interimResultShouldContinue
        );
      }
      return res.suggestion;
    },

    async askAiSuggestions(
      request: Generated.AiSuggestionRequest,
      noteId: Doughnut.ID
    ) {
      return (await managedApi.restPost(
        `ai/${noteId}/ask-suggestions`,
        request
      )) as Generated.AiSuggestion;
    },

    async keepAskingAIToGenerateQuestionUntilStop(
      prompt: string,
      noteId: Doughnut.ID,
      prev?: string,
      interimResultShouldContinue?: (suggestion: string) => boolean
    ): Promise<string> {
      const res = await this.askAiToGenerateQuestion(
        {
          prompt,
          incompleteAssistantMessage: prev ?? "",
        },
        noteId
      );
      if (interimResultShouldContinue) {
        if (!interimResultShouldContinue(res.suggestion)) return res.suggestion;
      }
      if (res.finishReason === "length") {
        return this.keepAskingAIToGenerateQuestionUntilStop(
          prompt,
          noteId,
          res.suggestion,
          interimResultShouldContinue
        );
      }
      return res.suggestion;
    },

    async askAiToGenerateQuestion(
      request: Generated.AiSuggestionRequest,
      noteId: Doughnut.ID
    ) {
      return (await managedApi.restPost(
        `ai/generate-question?note=${noteId}`,
        request
      )) as Generated.AiSuggestion;
    },

    async askAiEngagingStories(prompt: string) {
      const request: Generated.AiSuggestionRequest = {
        prompt,
        incompleteAssistantMessage: "",
      };
      return (await managedApi.restPost(
        `ai/ask-engaging-stories`,
        request
      )) as Generated.AiEngagingStory;
    },
  },
  testability: {
    getEnvironment() {
      return window.location.href.includes("odd-e.com")
        ? "production"
        : "testing";
    },
    async getFeatureToggle() {
      return (
        !window.location.href.includes("odd-e.com") &&
        ((await managedApi.restGet(`testability/feature_toggle`)) as boolean)
      );
    },

    async setFeatureToggle(data: boolean) {
      const res = await managedApi.restPost(`testability/feature_toggle`, {
        enabled: data,
      });
      return res;
    },

    async setRandomizer(data: string) {
      const res = await managedApi.restPost(`testability/randomizer`, {
        choose: data,
      });
      return res;
    },
  },
});

export default apiCollection;
