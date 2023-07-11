import { Ref } from "vue";
import { Router } from "vue-router";
import ManagedApi from "@/managedApi/ManagedApi";
import NoteEditingHistory, { HistoryRecord } from "./NoteEditingHistory";
import NoteStorage, { StorageImplementation } from "./NoteStorage";
import StoredApiCollection, { StoredApi } from "./StoredApiCollection";

interface StorageAccessor extends NoteStorage {
  api(router: Router): StoredApi;
  peekUndo(): null | HistoryRecord;
  refOfNoteRealm(noteId: Doughnut.ID): Ref<Generated.NoteRealm | undefined>;
}

class AccessorImplementation
  extends StorageImplementation
  implements StorageAccessor
{
  noteEditingHistory: NoteEditingHistory;

  managedApi: ManagedApi;

  constructor(managedApi: ManagedApi, noteEditingHistory?: NoteEditingHistory) {
    super();
    this.managedApi = managedApi;
    if (noteEditingHistory) {
      this.noteEditingHistory = noteEditingHistory;
    } else {
      this.noteEditingHistory = new NoteEditingHistory();
    }
  }

  peekUndo(): HistoryRecord | null {
    return this.noteEditingHistory.peekUndo() as HistoryRecord;
  }

  api(router: Router): StoredApi {
    return new StoredApiCollection(
      this.managedApi,
      this.noteEditingHistory,
      router,
      this,
    );
  }
}

function createNoteStorage(
  managedApi: ManagedApi,
  noteEditingHistory?: NoteEditingHistory,
): StorageAccessor {
  return new AccessorImplementation(managedApi, noteEditingHistory);
}

export default createNoteStorage;
export type { StorageAccessor };
export { NoteEditingHistory };
