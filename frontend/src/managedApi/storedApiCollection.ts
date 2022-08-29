import ManagedApi from "./ManagedApi";
import { HistoryWriter } from "../store/history";

const storedApiCollection = (undoHistory: HistoryWriter | undefined) => {
  return new StoredApiCollection(undoHistory);
};

class StoredApiCollection {
  undoHistory?: HistoryWriter;

  managedApi: ManagedApi;

  constructor(undoHistory: HistoryWriter | undefined) {
    this.managedApi = new ManagedApi(undefined);
    this.undoHistory = undoHistory;
  }

  private async updateTextContentWithoutUndo(
    noteId: Doughnut.ID,
    noteContentData: Generated.TextContent
  ) {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { updatedAt, ...data } = noteContentData;
    return (await this.managedApi.restPatchMultiplePartForm(
      `text_content/${noteId}`,
      data
    )) as Generated.NoteRealm;
  }

  async createNote(parentId: Doughnut.ID, data: Generated.NoteCreation) {
    return (await this.managedApi.restPostMultiplePartForm(
      `notes/${parentId}/create`,
      data
    )) as Generated.NoteRealmWithPosition;
  }

  async createLink(
    sourceId: Doughnut.ID,
    targetId: Doughnut.ID,
    data: Generated.LinkCreation
  ) {
    return (await this.managedApi.restPost(
      `links/create/${sourceId}/${targetId}`,
      data
    )) as Generated.NoteRealm;
  }

  async updateLink(linkId: Doughnut.ID, data: Generated.LinkCreation) {
    return (await this.managedApi.restPost(`links/${linkId}`, data)) as number;
  }

  async deleteLink(linkId: Doughnut.ID) {
    return (await this.managedApi.restPost(
      `links/${linkId}/delete`,
      {}
    )) as number;
  }

  async updateNote(
    noteId: Doughnut.ID,
    noteContentData: Generated.NoteAccessories
  ) {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { updatedAt, ...data } = noteContentData;
    return (await this.managedApi.restPatchMultiplePartForm(
      `notes/${noteId}`,
      data
    )) as Generated.NoteRealm;
  }

  async updateTextContent(
    noteId: Doughnut.ID,
    noteContentData: Generated.TextContent,
    oldContent: Generated.TextContent
  ) {
    this.undoHistory?.addEditingToUndoHistory(noteId, oldContent);
    return this.updateTextContentWithoutUndo(noteId, noteContentData);
  }

  async undo() {
    const undone = this.undoHistory?.peekUndo();
    if (!undone) throw new Error("undo history is empty");
    this.undoHistory?.popUndoHistory();
    if (undone.type === "editing" && undone.textContent) {
      return this.updateTextContentWithoutUndo(
        undone.noteId,
        undone.textContent
      );
    }
    return (await this.managedApi.restPatch(
      `notes/${undone.noteId}/undo-delete`,
      {}
    )) as Generated.NoteRealm;
  }

  async deleteNote(noteId: Doughnut.ID) {
    const res = (await this.managedApi.restPost(
      `notes/${noteId}/delete`,
      {}
    )) as number[];
    this.undoHistory?.deleteNote(noteId);
    if (res.length > 0) {
      return res[0];
    }
    return undefined;
  }
}

export default storedApiCollection;
