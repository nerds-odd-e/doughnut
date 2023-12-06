import { Router } from "vue-router";
import { debounce } from "lodash";
import ManagedApi from "../managedApi/ManagedApi";
import apiCollection from "../managedApi/apiCollection";
import NoteEditingHistory from "./NoteEditingHistory";
import NoteStorage from "./NoteStorage";
import NoteTextContentChanger from "./NoteTextContentChanger";

export interface StoredApi {
  getNoteRealmAndReloadPosition(
    noteId: Doughnut.ID,
  ): Promise<Generated.NoteRealm>;

  createNote(
    router: Router,
    parentId: Doughnut.ID,
    data: Generated.NoteCreation,
  ): Promise<Generated.NoteRealm>;

  createLink(
    sourceId: Doughnut.ID,
    targetId: Doughnut.ID,
    data: Generated.LinkCreation,
  ): Promise<Generated.NoteRealm>;

  updateLink(
    linkId: Doughnut.ID,
    data: Generated.LinkCreation,
  ): Promise<Generated.NoteRealm>;

  deleteLink(
    linkId: Doughnut.ID,
    fromTargetPerspective: boolean,
  ): Promise<Generated.NoteRealm>;

  updateNoteAccessories(
    noteId: Doughnut.ID,
    noteAccessories: Generated.NoteAccessories,
  ): Promise<Generated.NoteRealm>;

  noteTextContentChanger(): NoteTextContentChanger;

  updateTextContent(
    noteId: Doughnut.ID,
    noteContentData: Omit<Generated.TextContent, "updatedAt">,
    oldContent: Generated.TextContent,
    errorHandler: (err: unknown) => void,
  ): Promise<void>;

  updateWikidataId(
    noteId: Doughnut.ID,
    data: Generated.WikidataAssociationCreation,
  ): Promise<Generated.NoteRealm>;

  undo(router: Router): Promise<Generated.NoteRealm>;

  deleteNote(
    router: Router,
    noteId: Doughnut.ID,
  ): Promise<Generated.NoteRealm | undefined>;
}
export default class StoredApiCollection implements StoredApi {
  noteEditingHistory: NoteEditingHistory;

  managedApi: ManagedApi;

  storage: NoteStorage;

  constructor(
    managedApi: ManagedApi,
    undoHistory: NoteEditingHistory,
    storage: NoteStorage,
  ) {
    this.managedApi = managedApi;
    this.noteEditingHistory = undoHistory;
    this.storage = storage;
  }

  // eslint-disable-next-line class-methods-use-this
  private routerReplaceFocus(
    router: Router,
    focusOnNote?: Generated.NoteRealm,
  ) {
    if (!focusOnNote) {
      return router.replace({ name: "notebooks" });
    }
    return router.replace({
      name: "noteShow",
      params: { noteId: focusOnNote.id },
    });
  }

  private get statelessApi(): ReturnType<typeof apiCollection> {
    return apiCollection(this.managedApi);
  }

  private async updateTextContentWithoutUndo(
    noteId: Doughnut.ID,
    noteContentData: Omit<Generated.TextContent, "updatedAt">,
  ) {
    function excludeProperty<T, K extends string>(
      obj: T,
      property: K,
    ): Omit<T, K> {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const { [property]: _, ...rest } = obj as T & Record<K, unknown>;
      return rest;
    }
    return (await this.managedApi.restPatchMultiplePartForm(
      `text_content/${noteId}`,
      excludeProperty(noteContentData, "updatedAt"),
    )) as Generated.NoteRealm;
  }

  async updateWikidataId(
    noteId: Doughnut.ID,
    data: Generated.WikidataAssociationCreation,
  ): Promise<Generated.NoteRealm> {
    return this.storage.refreshNoteRealm(
      await this.statelessApi.wikidata.updateWikidataId(noteId, data),
    );
  }

  async getNoteRealmAndReloadPosition(noteId: Doughnut.ID) {
    const nrwp = await this.statelessApi.noteMethods.getNoteRealm(noteId);
    return this.storage.refreshNoteRealm(nrwp);
  }

  async createNote(
    router: Router,
    parentId: Doughnut.ID,
    data: Generated.NoteCreation,
  ) {
    const nrwp = await this.statelessApi.noteMethods.createNote(parentId, data);
    const focus = this.storage.refreshNoteRealm(nrwp);
    this.routerReplaceFocus(router, focus);
    return focus;
  }

  async createLink(
    sourceId: Doughnut.ID,
    targetId: Doughnut.ID,
    data: Generated.LinkCreation,
  ) {
    return this.storage.refreshNoteRealm(
      (await this.managedApi.restPost(
        `links/create/${sourceId}/${targetId}`,
        data,
      )) as Generated.NoteRealm,
    );
  }

  async updateLink(linkId: Doughnut.ID, data: Generated.LinkCreation) {
    return this.storage.refreshNoteRealm(
      (await this.managedApi.restPost(
        `links/${linkId}`,
        data,
      )) as Generated.NoteRealm,
    );
  }

  async deleteLink(linkId: Doughnut.ID, fromTargetPerspective: boolean) {
    return this.storage.refreshNoteRealm(
      (await this.managedApi.restPost(
        `links/${linkId}/${fromTargetPerspective ? "tview" : "sview"}/delete`,
        {},
      )) as Generated.NoteRealm,
    );
  }

  async updateNoteAccessories(
    noteId: Doughnut.ID,
    noteContentData: Generated.NoteAccessories,
  ) {
    return this.storage.refreshNoteRealm(
      (await this.managedApi.restPatchMultiplePartForm(
        `notes/${noteId}`,
        noteContentData,
      )) as Generated.NoteRealm,
    );
  }

  noteTextContentChanger() {
    const changer = (
      noteId: number,
      newValue: Generated.TextContent,
      oldValue: Generated.TextContent,
      errorHander: (errs: unknown) => void,
    ) => {
      if (
        newValue.topic === oldValue.topic &&
        newValue.details === oldValue.details
      ) {
        return;
      }
      this.updateTextContent(noteId, newValue, oldValue, errorHander);
    };

    return new NoteTextContentChanger(debounce(changer, 1000));
  }

  async updateTextContent(
    noteId: Doughnut.ID,
    noteContentData: Omit<Generated.TextContent, "updatedAt">,
    oldContent: Generated.TextContent,
    errorHander: (err: unknown) => void,
  ) {
    this.noteEditingHistory.addEditingToUndoHistory(noteId, oldContent);
    try {
      this.storage.refreshNoteRealm(
        await this.updateTextContentWithoutUndo(noteId, noteContentData),
      );
    } catch (err) {
      errorHander(err);
    }
  }

  private async undoInner() {
    const undone = this.noteEditingHistory.peekUndo();
    if (!undone) throw new Error("undo history is empty");
    this.noteEditingHistory.popUndoHistory();
    if (undone.type === "editing" && undone.textContent) {
      return this.updateTextContentWithoutUndo(
        undone.noteId,
        undone.textContent,
      );
    }
    return (await this.managedApi.restPatch(
      `notes/${undone.noteId}/undo-delete`,
      {},
    )) as Generated.NoteRealm;
  }

  async undo(router: Router) {
    const noteRealm = this.storage.refreshNoteRealm(await this.undoInner());
    router.push({
      name: "noteShow",
      params: { noteId: noteRealm.id },
    });
    return noteRealm;
  }

  async deleteNote(router: Router, noteId: Doughnut.ID) {
    const res = (await this.managedApi.restPost(
      `notes/${noteId}/delete`,
      {},
    )) as Generated.NoteRealm[];
    this.noteEditingHistory.deleteNote(noteId);
    if (res.length === 0) {
      this.routerReplaceFocus(router);
      return undefined;
    }
    const noteRealm = this.storage.refreshNoteRealm(
      res[0] as Generated.NoteRealm,
    );
    this.routerReplaceFocus(router, noteRealm);
    return noteRealm;
  }
}
