/* eslint-disable max-classes-per-file */
import ManagedApi from "../managedApi/ManagedApi";

interface HistoryRecord {
  type: "editing" | "delete note";
  noteId: Doughnut.ID;
  textContent?: Generated.TextContent;
}

interface HistoryState {
  api(): StoredApi;
  peekUndo(): null | HistoryRecord;
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
  noteEditingHistory: NoteEditingHistory;

  managedApi: ManagedApi;

  constructor(undoHistory: NoteEditingHistory) {
    this.managedApi = new ManagedApi(undefined);
    this.noteEditingHistory = undoHistory;
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
    this.noteEditingHistory.addEditingToUndoHistory(noteId, oldContent);
    return this.updateTextContentWithoutUndo(noteId, noteContentData);
  }

  async undo() {
    const undone = this.noteEditingHistory.peekUndo();
    if (!undone) throw new Error("undo history is empty");
    this.noteEditingHistory.popUndoHistory();
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
    this.noteEditingHistory.deleteNote(noteId);
    if (res.length > 0) {
      return res[0];
    }
    return undefined;
  }
}

class NoteStorage implements HistoryState {
  noteEditingHistory: NoteEditingHistory;

  constructor(noteEditingHistory?: NoteEditingHistory) {
    if (noteEditingHistory) {
      this.noteEditingHistory = noteEditingHistory;
    } else {
      this.noteEditingHistory = new NoteEditingHistory();
    }
  }

  peekUndo(): HistoryRecord | null {
    return this.noteEditingHistory.peekUndo();
  }

  api(): StoredApi {
    return new StoredApiCollection(this.noteEditingHistory);
  }
}

class NoteEditingHistory implements NoteEditingHistory {
  noteUndoHistories: HistoryRecord[];

  constructor() {
    this.noteUndoHistories = [];
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

type StorageAccessor = HistoryState;

function createNoteStorage(
  noteEditingHistory?: NoteEditingHistory
): StorageAccessor {
  return new NoteStorage(noteEditingHistory);
}

export default createNoteStorage;
export type { StorageAccessor };
export { NoteEditingHistory };
