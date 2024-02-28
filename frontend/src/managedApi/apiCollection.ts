import {
  AiCompletionAnswerClarifyingQuestionParams,
  AiCompletionParams,
  AiCompletionResponse,
  AiGeneratedImage,
  ChatRequest,
  ChatResponse,
  Circle,
  GlobalAiModelSettings,
  NoteCreationDTO,
  NotePositionViewedByUser,
  NoteRealm,
  Notebook,
  NotebooksViewedByUser,
  RedirectToNoteResponse,
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

  notebookMethods: {
    async createNotebook(circle: Circle | undefined, data: NoteCreationDTO) {
      const url = (() => {
        if (circle) {
          return `circles/${circle.id}/notebooks`;
        }
        return `notebooks/create`;
      })();

      return (await managedApi.restPostMultiplePartForm(
        url,
        data,
      )) as RedirectToNoteResponse;
    },

    async getNotebooks() {
      return (await managedApi.restGet(`notebooks`)) as NotebooksViewedByUser;
    },

    updateNotebookSettings(notebookId: Doughnut.ID, data: Notebook) {
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
