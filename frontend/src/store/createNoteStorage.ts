import { Router } from "vue-router";
import NoteEditingHistory, { HistoryRecord } from "./NoteEditingHistory";
import NoteStorage, { StorageImplementation } from "./NoteStorage";
import StoredApiCollection, { StoredApi } from "./StoredApiCollection";

interface StorageAccessor extends NoteStorage {
  api(router: Router): StoredApi;
  peekUndo(): null | HistoryRecord;
}

class AccessorImplementation
  extends StorageImplementation
  implements StorageAccessor
{
  noteEditingHistory: NoteEditingHistory;

  constructor(noteEditingHistory?: NoteEditingHistory) {
    super();
    if (noteEditingHistory) {
      this.noteEditingHistory = noteEditingHistory;
    } else {
      this.noteEditingHistory = new NoteEditingHistory();
    }
  }

  peekUndo(): HistoryRecord | null {
    return this.noteEditingHistory.peekUndo();
  }

  api(router: Router): StoredApi {
    return new StoredApiCollection(this.noteEditingHistory, router, this);
  }
}

function createNoteStorage(
  noteEditingHistory?: NoteEditingHistory
): StorageAccessor {
  return new AccessorImplementation(noteEditingHistory);
}

export default createNoteStorage;
export type { StorageAccessor };
export { NoteEditingHistory };
