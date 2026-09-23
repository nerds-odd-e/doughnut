import type { NoteRealm } from "@generated/donut-backend-api"
import type { Ref } from "vue"
import { ref } from "vue"

export default interface NoteStorage {
  refreshNoteRealm(data: NoteRealm): NoteRealm
  removeNoteRealm(noteId: Donut.ID): void
  permanentlyRemoveNoteRealm(noteId: Donut.ID): void
  isNotePermanentlyRemoved(noteId: Donut.ID): boolean
  refOfNoteRealm(noteId: Donut.ID): Ref<NoteRealm | undefined>
}

export class StorageImplementation implements NoteStorage {
  cache: Map<Donut.ID, Ref<NoteRealm | undefined>> = new Map()
  private permanentlyRemovedNoteIds = new Set<Donut.ID>()

  refreshNoteRealm(noteRealm: NoteRealm): NoteRealm {
    if (!this.isNotePermanentlyRemoved(noteRealm.id)) {
      this.refOfNoteRealm(noteRealm.id).value = noteRealm
    }
    return noteRealm
  }

  removeNoteRealm(noteId: Donut.ID): void {
    this.cache.delete(noteId)
  }

  permanentlyRemoveNoteRealm(noteId: Donut.ID): void {
    this.permanentlyRemovedNoteIds.add(noteId)
    const existingRef = this.cache.get(noteId)
    if (existingRef) existingRef.value = undefined
    this.removeNoteRealm(noteId)
  }

  isNotePermanentlyRemoved(noteId: Donut.ID): boolean {
    return this.permanentlyRemovedNoteIds.has(noteId)
  }

  refOfNoteRealm(noteId: Donut.ID): Ref<NoteRealm | undefined> {
    if (!this.cache.has(noteId)) {
      this.cache.set(noteId, ref(undefined))
    }
    return this.cache.get(noteId) as Ref<NoteRealm | undefined>
  }
}
