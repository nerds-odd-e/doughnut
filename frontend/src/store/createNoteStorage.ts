import ManagedApi from "@/managedApi/ManagedApi";
import { Ref, ref } from "vue";
import NoteEditingHistory, { HistoryRecord } from "./NoteEditingHistory";
import NoteStorage, { StorageImplementation } from "./NoteStorage";
import StoredApiCollection, { StoredApi } from "./StoredApiCollection";

type CurrentNoteId = {
  id: number | undefined;
};
interface StorageAccessor extends NoteStorage {
  storedApi(): StoredApi;
  peekUndo(): null | HistoryRecord;
  currentNoteIdRef(): Ref<CurrentNoteId>;
}

class AccessorImplementation
  extends StorageImplementation
  implements StorageAccessor
{
  noteEditingHistory: NoteEditingHistory;

  managedApi: ManagedApi;

  currentNoteIdRefValue = ref({ id: undefined });

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

  storedApi(): StoredApi {
    return new StoredApiCollection(
      this.managedApi,
      this.noteEditingHistory,
      this,
    );
  }

  currentNoteIdRef() {
    return this.currentNoteIdRefValue;
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
