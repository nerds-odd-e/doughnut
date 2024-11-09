import type { Note, NoteRealm } from "@/generated/backend"
import type { Ref } from "vue"
import { ref } from "vue"

export default interface NoteStorage {
  refreshNoteRealm(data: NoteRealm): NoteRealm
  refOfNoteRealm(noteId: Doughnut.ID): Ref<NoteRealm | undefined>
  refOfNoteRealmWithFallback(note: Note): Ref<NoteRealm | undefined>
}

export class StorageImplementation implements NoteStorage {
  cache: Map<Doughnut.ID, Ref<NoteRealm | undefined>> = new Map()

  refreshNoteRealm(noteRealm: NoteRealm): NoteRealm {
    this.refOfNoteRealm(noteRealm?.id).value = noteRealm
    return noteRealm
  }

  refOfNoteRealm(noteId: Doughnut.ID): Ref<NoteRealm | undefined> {
    if (!this.cache.has(noteId)) {
      this.cache.set(noteId, ref(undefined))
    }
    return this.cache.get(noteId) as Ref<NoteRealm | undefined>
  }

  refOfNoteRealmWithFallback(note: Note): Ref<NoteRealm | undefined> {
    const ref = this.refOfNoteRealm(note.id)
    if (ref.value) {
      ref.value = {
        id: note.id,
        note: note,
      }
    }
    return ref
  }
}
