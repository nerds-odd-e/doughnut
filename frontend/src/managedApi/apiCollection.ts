import {
  AiCompletionAnswerClarifyingQuestionParams,
  AiCompletionParams,
  AiCompletionResponse,
  AiGeneratedImage,
  ChatRequest,
  ChatResponse,
  GlobalAiModelSettings,
  NoteRealm,
  WikidataAssociationCreation,
  WikidataEntityData,
  WikidataSearchEntity,
} from "@/generated/backend";
import ManagedApi from "./ManagedApi";

export const timezoneParam = () => {
  const { timeZone } = Intl.DateTimeFormat().resolvedOptions();
  return timeZone;
};

const apiCollection = (managedApi: ManagedApi) => ({
  userMethods: {
    logout() {
      return managedApi.restPostWithHtmlResponse(`/logout`, {});
    },
  },

  wikidata: {
    async updateWikidataId(
      noteId: Doughnut.ID,
      data: WikidataAssociationCreation,
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
      const request: ChatRequest = { userMessage };
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
      )) as AiCompletionResponse;
    },

    async answerCompletionClarifyingQuestion(
      request: AiCompletionAnswerClarifyingQuestionParams,
    ) {
      return (await managedApi.restPost(
        `ai/answer-clarifying-question`,
        request,
      )) as AiCompletionResponse;
    },

    async generateImage(prompt: string) {
      return (await managedApi.restPost(
        `ai/generate-image`,
        prompt,
      )) as AiGeneratedImage;
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
