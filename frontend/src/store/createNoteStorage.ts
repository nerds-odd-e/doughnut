import type { HistoryRecord } from "./NoteEditingHistory"
import NoteEditingHistory from "./NoteEditingHistory"
import type NoteStorage from "./NoteStorage"
import { StorageImplementation } from "./NoteStorage"
import type { StoredApi } from "./StoredApiCollection"
import StoredApiCollection from "./StoredApiCollection"

interface StorageAccessor extends NoteStorage {
  storedApi(): StoredApi
  peekUndo(): null | HistoryRecord
}

class AccessorImplementation
  extends StorageImplementation
  implements StorageAccessor
{
  noteEditingHistory: NoteEditingHistory

  constructor(noteEditingHistory?: NoteEditingHistory) {
    super()
    if (noteEditingHistory) {
      this.noteEditingHistory = noteEditingHistory
    } else {
      this.noteEditingHistory = new NoteEditingHistory()
    }
  }

  peekUndo(): HistoryRecord | null {
    return this.noteEditingHistory.peekUndo() as HistoryRecord
  }

  storedApi(): StoredApi {
    return new StoredApiCollection(this.noteEditingHistory, this)
  }
}

function createNoteStorage(
  noteEditingHistory?: NoteEditingHistory
): StorageAccessor {
  return new AccessorImplementation(noteEditingHistory)
}

export default createNoteStorage
export type { StorageAccessor }
export { NoteEditingHistory }
