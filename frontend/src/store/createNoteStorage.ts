import ManagedApi from "@/managedApi/ManagedApi"
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

  managedApi: ManagedApi

  constructor(managedApi: ManagedApi, noteEditingHistory?: NoteEditingHistory) {
    super()
    this.managedApi = managedApi
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
    return new StoredApiCollection(
      this.managedApi,
      this.noteEditingHistory,
      this
    )
  }
}

function createNoteStorage(
  managedApi: ManagedApi,
  noteEditingHistory?: NoteEditingHistory
): StorageAccessor {
  return new AccessorImplementation(managedApi, noteEditingHistory)
}

export default createNoteStorage
export type { StorageAccessor }
export { NoteEditingHistory }
