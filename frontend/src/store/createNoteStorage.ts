import type { HistoryRecord } from "./noteUndo"
import NoteUndo from "./noteUndo"
import type NoteStorage from "./NoteStorage"
import { StorageImplementation } from "./NoteStorage"
import StoredApiCollection from "./StoredApiCollection"

interface StorageAccessor extends NoteStorage {
  noteUndo: NoteUndo
  storedApi(): StoredApiCollection
  peekUndo(): null | HistoryRecord
  discardUndo(): void
}

class AccessorImplementation
  extends StorageImplementation
  implements StorageAccessor
{
  noteUndo = new NoteUndo(this)

  peekUndo() {
    return this.noteUndo.peekUndo() ?? null
  }

  storedApi() {
    return new StoredApiCollection(this.noteUndo, this)
  }

  discardUndo() {
    this.noteUndo.discardUndo()
  }
}

function createNoteStorage(): StorageAccessor {
  return new AccessorImplementation()
}

export default createNoteStorage
export type { StorageAccessor }
