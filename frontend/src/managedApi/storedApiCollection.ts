import ManagedApi from "./ManagedApi";
import { HistoryState, HistoryWriter } from "../store/history";

const storedApiCollection = (
  undoHistory: HistoryWriter | undefined,
  managedApi: ManagedApi
) => {
  function writeHistory(w: (h: HistoryState) => void) {
    if (undoHistory) {
      undoHistory((h) => {
        w(h);
      });
    }
  }

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
    async createNote(parentId: Doughnut.ID, data: Generated.NoteCreation) {
      return (await managedApi.restPostMultiplePartForm(
        `notes/${parentId}/create`,
        data
      )) as Generated.NoteRealmWithPosition;
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
      writeHistory((h: HistoryState) =>
        h.addEditingToUndoHistory(noteId, oldContent)
      );
      return updateTextContentWithoutUndo(noteId, noteContentData);
    },

    async undo() {
      let undone;
      writeHistory((h) => {
        undone = h.peekUndo();
        if (!undone) throw new Error("undo history is empty");
        h.popUndoHistory();
      });
      if (undone.type === "editing" && undone.textContent) {
        return updateTextContentWithoutUndo(undone.noteId, undone.textContent);
      }
      return (await managedApi.restPatch(
        `notes/${undone.noteId}/undo-delete`,
        {}
      )) as Generated.NoteRealm;
    },

    async deleteNote(noteId: Doughnut.ID) {
      const res = (await managedApi.restPost(
        `notes/${noteId}/delete`,
        {}
      )) as number[];
      writeHistory((h) => h.deleteNote(noteId));
      if (res.length > 0) {
        return res[0];
      }
      return undefined;
    },
  };
};

export default storedApiCollection;
