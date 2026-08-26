import type { NoteRealm } from "@generated/donut-backend-api"
import type { Ref } from "vue"
import { ref } from "vue"

export default interface NoteStorage {
  refreshNoteRealm(data: NoteRealm): NoteRealm
  removeNoteRealm(noteId: Donut.ID): void
  refOfNoteRealm(noteId: Donut.ID): Ref<NoteRealm | undefined>
}

export class StorageImplementation implements NoteStorage {
  cache: Map<Donut.ID, Ref<NoteRealm | undefined>> = new Map()

  refreshNoteRealm(noteRealm: NoteRealm): NoteRealm {
    this.refOfNoteRealm(noteRealm?.id).value = noteRealm
    return noteRealm
  }

  removeNoteRealm(noteId: Donut.ID): void {
    this.cache.delete(noteId)
  }

  refOfNoteRealm(noteId: Donut.ID): Ref<NoteRealm | undefined> {
    if (!this.cache.has(noteId)) {
      this.cache.set(noteId, ref(undefined))
    }
    return this.cache.get(noteId) as Ref<NoteRealm | undefined>
  }
}
