import ManagedApi from "./ManagedApi";

const timezoneParam = () => {
  const { timeZone } = Intl.DateTimeFormat().resolvedOptions();
  return `timezone=${encodeURIComponent(timeZone)}`;
};

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
        `user/current-user-info`,
      )) as Generated.CurrentUserInfo;
      return res;
    },

    async updateUser(userId: Doughnut.ID, data: Generated.User) {
      const res = (await managedApi.restPatchMultiplePartForm(
        `user/${userId}`,
        data,
      )) as Generated.User;
      return res;
    },

    async createUser(data: Generated.User) {
      return (await managedApi.restPostMultiplePartForm(
        `user`,
        data,
      )) as Generated.User;
    },
  },

  quizQuestions: {
    async generateQuestion(
      noteId: Doughnut.ID,
    ): Promise<Generated.QuizQuestion> {
      const url = `quiz-questions/generate-question?note=${noteId}`;
      return (await managedApi.restPost(url, {})) as Generated.QuizQuestion;
    },

    async regenerateQuestion(
      quizQuestionId: number,
    ): Promise<Generated.QuizQuestion> {
      return managedApi.restPost(
        `quiz-questions/${quizQuestionId}/regenerate`,
        {},
      ) as Promise<Generated.QuizQuestion>;
    },

    async contest(
      quizQuestionId: number,
    ): Promise<Generated.QuizQuestionContestResult> {
      return managedApi.restPost(
        `quiz-questions/${quizQuestionId}/contest`,
        {},
      ) as Promise<Generated.QuizQuestionContestResult>;
    },

    async suggestQuestionForFineTuning(
      quizQuestionId: number,
      suggestedQuestion: Generated.QuestionSuggestionCreationParams,
    ): Promise<string> {
      return managedApi.restPost(
        `quiz-questions/${quizQuestionId}/suggest-fine-tuning`,
        suggestedQuestion,
      ) as Promise<string>;
    },

    async processAnswer(quizQuestionId: Doughnut.ID, data: Generated.Answer) {
      const res = (await managedApi.restPost(
        `quiz-questions/${quizQuestionId}/answer`,
        data,
      )) as Generated.AnsweredQuestion;
      return res;
    },
  },

  reviewMethods: {
    async markAsRepeated(reviewPointId: Doughnut.ID, successful: boolean) {
      return (await managedApi.restPost(
        `review-points/${reviewPointId}/mark-as-repeated?successful=${successful}`,
        {},
      )) as Generated.ReviewPoint;
    },

    async removeFromReview(reviewPointId: Doughnut.ID) {
      return (await managedApi.restPost(
        `review-points/${reviewPointId}/remove`,
        {},
      )) as Generated.ReviewPoint;
    },

    async overview() {
      return (await managedApi.restGet(
        `reviews/overview?${timezoneParam()}`,
      )) as Generated.ReviewStatus;
    },
    updateReviewSetting(
      noteId: Doughnut.ID,
      data: Omit<Generated.ReviewSetting, "id">,
    ) {
      return managedApi.restPost(`notes/${noteId}/review-setting`, data);
    },

    async getReviewPoint(reviewPointId: Doughnut.ID) {
      return (await managedApi.restGet(
        `review-points/${reviewPointId}`,
      )) as Generated.ReviewPoint;
    },

    async initialReview() {
      return (await managedApi.restGet(
        `reviews/initial?${timezoneParam()}`,
      )) as Generated.ReviewPoint[];
    },

    async doInitialReview(data: Generated.InitialInfo) {
      return (await managedApi.restPost(
        `reviews`,
        data,
      )) as Generated.ReviewPoint;
    },

    async getAnswer(answerId: Doughnut.ID) {
      return (await managedApi.restGet(
        `reviews/answers/${answerId}`,
      )) as Generated.AnsweredQuestion;
    },

    async selfEvaluate(
      reviewPointId: Doughnut.ID,
      data: Generated.SelfEvaluation,
    ) {
      const res = (await managedApi.restPost(
        `review-points/${reviewPointId}/self-evaluate`,
        data,
      )) as Generated.ReviewPoint;
      return res;
    },

    async getDueReviewPoints(dueInDays?: number) {
      const res = (await managedApi.restGet(
        `reviews/repeat?${timezoneParam()}&dueindays=${dueInDays ?? ""}`,
      )) as Generated.DueReviewPoints;
      return res;
    },

    async getRandomQuestionForReviewPoint(reviewPointId: Doughnut.ID) {
      const res = (await managedApi.restGet(
        `review-points/${reviewPointId}/random-question`,
      )) as Generated.QuizQuestion;
      return res;
    },
  },
  fineTuning: {
    async getSuggestedQuestionsForFineTuning() {
      return (await managedApi.restGet(
        "fine-tuning/all-suggested-questions-for-fine-tuning",
      )) as Generated.SuggestedQuestionForFineTuning[];
    },
    async postUploadFineTuningExamples() {
      await managedApi.restGet("fine-tuning/upload-fine-tuning-examples");
    },
    async postUploadAndTriggerFineTuning() {
      await managedApi.restPost(
        "fine-tuning/upload-and-trigger-fine-tuning",
        {},
      );
    },
    async suggestedQuestionForFineTuningUpdate(
      suggestedId: Doughnut.ID,
      suggestedQuestion: Generated.QuestionSuggestionParams,
    ): Promise<string> {
      return managedApi.restPatch(
        `fine-tuning/${suggestedId}/update-suggested-question-for-fine-tuning`,
        suggestedQuestion,
      ) as Promise<string>;
    },
    async duplicateSuggestedQuestionForFineTuning(id: Doughnut.ID) {
      return (await managedApi.restPost(
        `fine-tuning/${id}/duplicate`,
        {},
      )) as Generated.SuggestedQuestionForFineTuning;
    },
    async deleteSuggestedQuestionForFineTuning(id: Doughnut.ID) {
      return (await managedApi.restPost(
        `fine-tuning/${id}/delete`,
        {},
      )) as Generated.SuggestedQuestionForFineTuning;
    },
  },
  circleMethods: {
    async getCircle(circleId: Doughnut.ID) {
      return (await managedApi.restGet(
        `circles/${circleId}`,
      )) as Generated.CircleForUserView;
    },
    async createCircle(data: Generated.Circle) {
      return (await managedApi.restPostMultiplePartForm(
        "circles",
        data,
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
      searchTerm,
    )) as Generated.Note[];
  },

  async getBazaar() {
    return (await managedApi.restGet(
      "bazaar",
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
        data,
      );
    },
    updateSubscription(
      subscriptionId: Doughnut.ID,
      data: Generated.Subscription,
    ) {
      return managedApi.restPostMultiplePartForm(
        `subscriptions/${subscriptionId}`,
        data,
      );
    },
    deleteSubscription(subscriptionId: Doughnut.ID) {
      return managedApi.restPost(`subscriptions/${subscriptionId}/delete`, {});
    },
  },
  async getNoteInfo(noteId: Doughnut.ID) {
    return (await managedApi.restGet(
      `notes/${noteId}/note-info`,
    )) as Generated.NoteInfo;
  },

  notebookMethods: {
    async createNotebook(
      circle: Generated.Circle | undefined,
      data: Generated.Notebook,
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
        `notebooks`,
      )) as Generated.NotebooksViewedByUser;
    },

    updateNotebookSettings(notebookId: Doughnut.ID, data: Generated.Notebook) {
      return managedApi.restPostMultiplePartForm(
        `notebooks/${notebookId}`,
        data,
      );
    },
  },

  noteMethods: {
    async createNote(parentId: Doughnut.ID, data: Generated.NoteCreation) {
      return (await managedApi.restPostMultiplePartForm(
        `notes/${parentId}/create`,
        data,
      )) as Generated.NoteRealm;
    },
    async getNoteRealm(noteId: Doughnut.ID) {
      return (await managedApi.restGet(
        `notes/${noteId}`,
      )) as Generated.NoteRealm;
    },

    async getNotePosition(noteId: Doughnut.ID) {
      return (await managedApi.restGet(
        `notes/${noteId}/position`,
      )) as Generated.NotePositionViewedByUser;
    },
  },
  wikidata: {
    async updateWikidataId(
      noteId: Doughnut.ID,
      data: Generated.WikidataAssociationCreation,
    ) {
      return (await managedApi.restPost(
        `notes/${noteId}/updateWikidataId`,
        data,
      )) as Generated.NoteRealm;
    },

    async getWikidataEntityById(wikidataId: string) {
      return (await managedApi.restGet(
        `wikidata/entity-data/${wikidataId}`,
      )) as Generated.WikidataEntityData;
    },

    async getWikidatas(keyword: string) {
      return (await managedApi.restGet(
        `wikidata/search/${keyword}`,
      )) as Generated.WikidataSearchEntity[];
    },
  },
  ai: {
    async chat(noteId: Doughnut.ID, userMessage: string): Promise<string> {
      const request: Generated.ChatRequest = { userMessage };
      const res = (await managedApi.restPost(
        `ai/chat?note=${noteId}`,
        request,
      )) as Generated.ChatResponse;
      return res.assistantMessage;
    },
    async getManageModel() {
      return (await managedApi.restGet(
        `ai/model-versions`,
      )) as Generated.ModelVersionOption[];
    },
    async getManageModelSelected() {
      return (await managedApi.restGet(
        `ai/current-model-version`,
      )) as Generated.CurrentModelVersionResponse;
    },
    async keepAskingAICompletionUntilStop(
      prompt: string,
      noteId: Doughnut.ID,
      prev?: string,
      interimResultShouldContinue?: (moreCompleteContent: string) => boolean,
    ): Promise<string> {
      const res = await this.askAiCompletion(
        {
          prompt,
          incompleteContent: prev ?? "",
        },
        noteId,
      );
      if (interimResultShouldContinue) {
        if (!interimResultShouldContinue(res.moreCompleteContent))
          return res.moreCompleteContent;
      }
      if (res.finishReason === "length") {
        return this.keepAskingAICompletionUntilStop(
          prompt,
          noteId,
          res.moreCompleteContent,
          interimResultShouldContinue,
        );
      }
      return res.moreCompleteContent;
    },

    async askAiCompletion(
      request: Generated.AiCompletionParams,
      noteId: Doughnut.ID,
    ) {
      return (await managedApi.restPost(
        `ai/${noteId}/completion`,
        request,
      )) as Generated.AiCompletion;
    },

    async generateImage(prompt: string) {
      const request: Generated.AiCompletionParams = {
        prompt,
        incompleteContent: "",
      };
      return (await managedApi.restPost(
        `ai/generate-image`,
        request,
      )) as Generated.AiGeneratedImage;
    },

    async getTrainingFiles() {
      return (await managedApi.restGet(
        `ai/training-files`,
      )) as Generated.AiTrainingFile[];
    },

    async triggerFineTuning(fileId: string) {
      return (await managedApi.restPost(
        `ai/trigger-finetuning/${fileId}`,
        {},
      )) as Generated.ApiResponse;
    },
    async evaluateQuestionModel() {
      return 66;
      // return (await managedApi.restPost(
      //   `ai/evaluate/question-model/${modelId}`,
      //   {},
      // )) as Generated.ApiResponse
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
