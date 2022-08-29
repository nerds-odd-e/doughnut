/* eslint-disable max-classes-per-file */
import ManagedApi from "../managedApi/ManagedApi";

interface HistoryRecord {
  type: "editing" | "delete note";
  noteId: Doughnut.ID;
  textContent?: Generated.TextContent;
}

interface HistoryState {
  noteUndoHistories: HistoryRecord[];
  api(): StoredApiCollection;
  peekUndo(): null | HistoryRecord;
  popUndoHistory(): void;
  addEditingToUndoHistory(
    noteId: Doughnut.ID,
    textContent: Generated.TextContent
  ): void;
  deleteNote(noteId: Doughnut.ID): void;
}

interface StoredApi {
  createNote(
    parentId: Doughnut.ID,
    data: Generated.NoteCreation
  ): Promise<Generated.NoteRealmWithPosition>;

  createLink(
    sourceId: Doughnut.ID,
    targetId: Doughnut.ID,
    data: Generated.LinkCreation
  ): Promise<Generated.NoteRealm>;

  updateLink(
    linkId: Doughnut.ID,
    data: Generated.LinkCreation
  ): Promise<number>;

  deleteLink(linkId: Doughnut.ID): Promise<number>;

  updateNote(
    noteId: Doughnut.ID,
    noteContentData: Generated.NoteAccessories
  ): Promise<Generated.NoteRealm>;

  updateTextContent(
    noteId: Doughnut.ID,
    noteContentData: Generated.TextContent,
    oldContent: Generated.TextContent
  ): Promise<Generated.NoteRealm>;

  undo(): Promise<Generated.NoteRealm>;

  deleteNote(noteId: Doughnut.ID): Promise<number | undefined>;
}

class StoredApiCollection implements StoredApi {
  undoHistory: HistoryWriter;

  managedApi: ManagedApi;

  constructor(undoHistory: HistoryWriter) {
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
    this.undoHistory.addEditingToUndoHistory(noteId, oldContent);
    return this.updateTextContentWithoutUndo(noteId, noteContentData);
  }

  async undo() {
    const undone = this.undoHistory.peekUndo();
    if (!undone) throw new Error("undo history is empty");
    this.undoHistory.popUndoHistory();
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
    this.undoHistory.deleteNote(noteId);
    if (res.length > 0) {
      return res[0];
    }
    return undefined;
  }
}

export { StoredApiCollection };

class History implements HistoryState {
  noteUndoHistories: HistoryRecord[];

  constructor() {
    this.noteUndoHistories = [];
  }

  api(): StoredApiCollection {
    return new StoredApiCollection(this);
  }

  peekUndo() {
    if (this.noteUndoHistories.length === 0) return null;
    return this.noteUndoHistories[this.noteUndoHistories.length - 1];
  }

  addEditingToUndoHistory(
    noteId: Doughnut.ID,
    textContent: Generated.TextContent
  ) {
    this.noteUndoHistories.push({
      type: "editing",
      noteId,
      textContent: { ...textContent },
    });
  }

  popUndoHistory() {
    if (this.noteUndoHistories.length === 0) {
      return;
    }
    this.noteUndoHistories.pop();
  }

  deleteNote(noteId: Doughnut.ID) {
    this.noteUndoHistories.push({ type: "delete note", noteId });
  }
}

type HistoryWriter = HistoryState;

function createHistory(): HistoryWriter {
  return new History();
}

export default createHistory;
export type { HistoryWriter };
