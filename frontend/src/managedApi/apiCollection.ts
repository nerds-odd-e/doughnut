import { User } from "@/generated/backend/models/User";
import {
  AiCompletionAnswerClarifyingQuestionParams,
  AiCompletionParams,
  ChatResponse,
  DueReviewPoints,
  GlobalAiModelSettings,
  Note,
  NoteCreationDTO,
  NotePositionViewedByUser,
  NoteRealm,
  SuggestedQuestionForFineTuning,
  WikidataEntityData,
  WikidataSearchEntity,
} from "@/generated/backend";
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

    async updateUser(userId: Doughnut.ID, data: User) {
      const res = (await managedApi.restPatchMultiplePartForm(
        `user/${userId}`,
        data,
      )) as User;
      return res;
    },

    async createUser(data: User) {
      return (await managedApi.restPostMultiplePartForm(`user`, data)) as User;
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

    async processAnswer(
      quizQuestionId: Doughnut.ID,
      data: Partial<Generated.Answer>,
    ) {
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
      )) as Generated.Thing[];
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
      )) as DueReviewPoints;
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
      )) as SuggestedQuestionForFineTuning[];
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
      )) as SuggestedQuestionForFineTuning;
    },
    async deleteSuggestedQuestionForFineTuning(id: Doughnut.ID) {
      return (await managedApi.restPost(
        `fine-tuning/${id}/delete`,
        {},
      )) as SuggestedQuestionForFineTuning;
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

  async relativeSearch(
    noteId: undefined | Doughnut.ID,
    searchTerm: Generated.SearchTerm,
  ) {
    if (noteId) {
      return (await managedApi.restPost(
        `notes/${noteId}/search`,
        searchTerm,
      )) as Note[];
    }
    return (await managedApi.restPost(`notes/search`, searchTerm)) as Note[];
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
      data: NoteCreationDTO,
    ) {
      const url = (() => {
        if (circle) {
          return `circles/${circle.id}/notebooks`;
        }
        return `notebooks/create`;
      })();

      return (await managedApi.restPostMultiplePartForm(
        url,
        data,
      )) as Generated.RedirectToNoteResponse;
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
    async createNote(parentId: Doughnut.ID, data: NoteCreationDTO) {
      return (await managedApi.restPostMultiplePartForm(
        `notes/${parentId}/create`,
        data,
      )) as NoteRealm;
    },
    async getNoteRealm(noteId: Doughnut.ID) {
      return (await managedApi.restGet(`notes/${noteId}`)) as NoteRealm;
    },

    async getNotePosition(noteId: Doughnut.ID) {
      return (await managedApi.restGet(
        `notes/${noteId}/position`,
      )) as NotePositionViewedByUser;
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
      )) as NoteRealm;
    },

    async getWikidataEntityById(wikidataId: string) {
      return (await managedApi.restGet(
        `wikidata/entity-data/${wikidataId}`,
      )) as WikidataEntityData;
    },

    async getWikidatas(keyword: string) {
      return (await managedApi.restGet(
        `wikidata/search/${keyword}`,
      )) as WikidataSearchEntity[];
    },
  },
  settings: {
    async getManageModelSelected() {
      return (await managedApi.restGet(
        `settings/current-model-version`,
      )) as GlobalAiModelSettings;
    },
    async setManageModelSelected(settings: GlobalAiModelSettings) {
      return (await managedApi.restPost(
        `settings/current-model-version`,
        settings,
      )) as GlobalAiModelSettings;
    },
  },
  ai: {
    async recreateAllAssistants() {
      return (await managedApi.restPost(
        `ai/recreate-all-assistants`,
        {},
      )) as Record<string, string>;
    },
    async chat(
      noteId: Doughnut.ID,
      userMessage: string,
    ): Promise<string | undefined> {
      const request: Generated.ChatRequest = { userMessage };
      const res = (await managedApi.restPost(
        `ai/chat?note=${noteId}`,
        request,
      )) as ChatResponse;
      return res.assistantMessage;
    },

    async getAvailableGptModels() {
      return (await managedApi.restGet(`ai/available-gpt-models`)) as string[];
    },

    async askAiCompletion(noteId: Doughnut.ID, request: AiCompletionParams) {
      return (await managedApi.restPost(
        `ai/${noteId}/completion`,
        request,
      )) as Generated.AiCompletionResponse;
    },

    async answerCompletionClarifyingQuestion(
      request: AiCompletionAnswerClarifyingQuestionParams,
    ) {
      return (await managedApi.restPost(
        `ai/answer-clarifying-question`,
        request,
      )) as Generated.AiCompletionResponse;
    },

    async generateImage(prompt: string) {
      return (await managedApi.restPost(
        `ai/generate-image`,
        prompt,
      )) as Generated.AiGeneratedImage;
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
