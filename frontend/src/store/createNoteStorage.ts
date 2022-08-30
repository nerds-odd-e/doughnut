import NoteEditingHistory, { HistoryRecord } from "./NoteEditingHistory";
import StoredApiCollection, { StoredApi } from "./StoredApiCollection";

interface StorageAccessor {
  api(): StoredApi;
  peekUndo(): null | HistoryRecord;
  updatedNoteRealm?: Generated.NoteRealm;
  updatedAt?: Date;
}

class NoteStorage implements StorageAccessor {
  noteEditingHistory: NoteEditingHistory;

  updatedNoteRealm?: Generated.NoteRealm;

  updatedAt?: Date;

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

function createNoteStorage(
  noteEditingHistory?: NoteEditingHistory
): StorageAccessor {
  return new NoteStorage(noteEditingHistory);
}

export default createNoteStorage;
export type { StorageAccessor };
export { NoteEditingHistory };
