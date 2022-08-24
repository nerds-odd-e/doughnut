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

    async createNote(parentId: Doughnut.ID, data: Generated.NoteCreation) {
      return (await managedApi.restPostMultiplePartForm(
        `notes/${parentId}/create`,
        data
      )) as Generated.NoteRealm;
    },

    async createLink(
      sourceId: Doughnut.ID,
      targetId: Doughnut.ID,
      data: Generated.LinkCreation
    ) {
      return (await managedApi.restPost(
        `links/create/${sourceId}/${targetId}`,
        data
      )) as Generated.NoteRealm;
    },

    async updateLink(linkId: Doughnut.ID, data: Generated.LinkCreation) {
      return (await managedApi.restPost(`links/${linkId}`, data)) as number;
    },

    async deleteLink(linkId: Doughnut.ID) {
      return (await managedApi.restPost(
        `links/${linkId}/delete`,
        {}
      )) as number;
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
      const res = (await managedApi.restPostMultiplePartForm(
        `user`,
        data
      )) as Generated.User;
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
