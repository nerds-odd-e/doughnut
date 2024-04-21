import { Router } from "vue-router";
import {
  AudioUploadDTO,
  LinkCreation,
  NoteAccessoriesDTO,
  NoteCreationDTO,
  NoteRealm,
  WikidataAssociationCreation,
} from "@/generated/backend";
import ManagedApi from "@/managedApi/ManagedApi";
import NoteEditingHistory from "./NoteEditingHistory";
import NoteStorage from "./NoteStorage";

export interface StoredApi {
  getNoteRealmAndReloadPosition(noteId: Doughnut.ID): Promise<NoteRealm>;

  createNote(
    router: Router,
    parentId: Doughnut.ID,
    data: NoteCreationDTO,
  ): Promise<NoteRealm>;

  createLink(
    sourceId: Doughnut.ID,
    targetId: Doughnut.ID,
    data: LinkCreation,
  ): Promise<NoteRealm>;

  updateLink(linkId: Doughnut.ID, data: LinkCreation): Promise<NoteRealm>;

  deleteLink(
    linkId: Doughnut.ID,
    fromTargetPerspective: boolean,
  ): Promise<NoteRealm>;

  updateNoteAccessories(
    noteId: Doughnut.ID,
    noteAccessories: NoteAccessoriesDTO,
  ): Promise<NoteRealm>;

  updateTextField(
    noteId: Doughnut.ID,
    field: "edit topic" | "edit details",
    value: string,
  ): Promise<void>;

  updateWikidataId(
    noteId: Doughnut.ID,
    data: WikidataAssociationCreation,
  ): Promise<NoteRealm>;

  undo(router: Router): Promise<NoteRealm>;

  deleteNote(
    router: Router,
    noteId: Doughnut.ID,
  ): Promise<NoteRealm | undefined>;

  uploadAudio(
    noteId: Doughnut.ID,
    formData: AudioUploadDTO,
    convert: boolean,
  ): Promise<NoteRealm>;
  convertAudio(formData: AudioUploadDTO);
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
  private routerReplaceFocus(router: Router, focusOnNote?: NoteRealm) {
    if (!focusOnNote) {
      return router.replace({ name: "notebooks" });
    }
    return router.replace({
      name: "noteShow",
      params: { noteId: focusOnNote.id },
    });
  }

  private async updateTextContentWithoutUndo(
    noteId: Doughnut.ID,
    field: "edit topic" | "edit details",
    content: string,
  ) {
    return this.storage.refreshNoteRealm(
      await this.callUpdateApi(noteId, field, content),
    );
  }

  private async callUpdateApi(
    noteId: Doughnut.ID,
    field: "edit topic" | "edit details",
    content: string,
  ) {
    if (field === "edit topic") {
      return this.managedApi.restTextContentController.updateNoteTopicConstructor(
        noteId,
        { topicConstructor: content },
      );
    }
    return this.managedApi.restTextContentController.updateNoteDetails(noteId, {
      details: content,
    });
  }

  async updateWikidataId(
    noteId: Doughnut.ID,
    data: WikidataAssociationCreation,
  ): Promise<NoteRealm> {
    return this.storage.refreshNoteRealm(
      await this.managedApi.restNoteController.updateWikidataId(noteId, data),
    );
  }

  async getNoteRealmAndReloadPosition(noteId: Doughnut.ID) {
    const nrwp = await this.managedApi.restNoteController.show1(noteId);
    return this.storage.refreshNoteRealm(nrwp);
  }

  async createNote(
    router: Router,
    parentId: Doughnut.ID,
    data: NoteCreationDTO,
  ) {
    const nrwp = await this.managedApi.restNoteController.createNote(
      parentId,
      data,
    );
    const focus = this.storage.refreshNoteRealm(nrwp);
    this.routerReplaceFocus(router, focus);
    return focus;
  }

  async createLink(
    sourceId: Doughnut.ID,
    targetId: Doughnut.ID,
    data: LinkCreation,
  ) {
    return this.storage.refreshNoteRealm(
      await this.managedApi.restLinkController.linkNoteFinalize(
        sourceId,
        targetId,
        data,
      ),
    );
  }

  async updateLink(linkId: Doughnut.ID, data: LinkCreation) {
    return this.storage.refreshNoteRealm(
      await this.managedApi.restLinkController.updateLink(linkId, data),
    );
  }

  async deleteLink(linkId: Doughnut.ID, fromTargetPerspective: boolean) {
    return this.storage.refreshNoteRealm(
      await this.managedApi.restLinkController.deleteLink(
        linkId,
        fromTargetPerspective ? "tview" : "sview",
      ),
    );
  }

  async updateNoteAccessories(
    noteId: Doughnut.ID,
    noteContentData: NoteAccessoriesDTO,
  ) {
    return this.storage.refreshNoteRealm(
      await this.managedApi.restNoteController.updateNoteAccessories(
        noteId,
        noteContentData,
      ),
    );
  }

  async updateTextField(
    noteId: Doughnut.ID,
    field: "edit topic" | "edit details",
    value: string,
  ) {
    const currentNote = this.storage.refOfNoteRealm(noteId).value?.note;
    if (currentNote) {
      const old =
        field === "edit topic"
          ? currentNote.topicConstructor
          : currentNote.details;
      if (old === value) {
        return;
      }
      this.noteEditingHistory.addEditingToUndoHistory(noteId, field, old);
    }
    await this.updateTextContentWithoutUndo(noteId, field, value);
  }

  private async undoInner() {
    const undone = this.noteEditingHistory.peekUndo();
    if (!undone) throw new Error("undo history is empty");
    this.noteEditingHistory.popUndoHistory();
    if (
      undone.type === "edit topic" ||
      (undone.type === "edit details" && undone.textContent !== undefined)
    ) {
      return this.updateTextContentWithoutUndo(
        undone.noteId,
        undone.type,
        undone.textContent!,
      );
    }
    return this.managedApi.restNoteController.undoDeleteNote(undone.noteId);
  }

  async undo(router: Router) {
    const noteRealm = await this.undoInner();
    router.push({
      name: "noteShow",
      params: { noteId: noteRealm.id },
    });
    return noteRealm;
  }

  async deleteNote(router: Router, noteId: Doughnut.ID) {
    const res = await this.managedApi.restNoteController.deleteNote(noteId);
    this.noteEditingHistory.deleteNote(noteId);
    if (res.length === 0) {
      this.routerReplaceFocus(router);
      return undefined;
    }
    const noteRealm = this.storage.refreshNoteRealm(res[0]!);
    this.routerReplaceFocus(router, noteRealm);
    return noteRealm;
  }

  async uploadAudio(
    noteId: number,
    formData?: AudioUploadDTO,
    convert?: boolean,
  ) {
    return this.storage.refreshNoteRealm(
      await this.managedApi.restNoteController.uploadAudio(
        noteId,
        convert,
        formData,
      ),
    );
  }

  async convertAudio(formData: AudioUploadDTO) {
    const res = await this.managedApi.restNoteController.convertSrt(formData);
    return res;
  }
}
