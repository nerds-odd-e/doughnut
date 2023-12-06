import { Ref, ref } from "vue";

export default interface NoteStorage {
  refreshNoteRealm(data: Generated.NoteRealm): Generated.NoteRealm;
  refOfNoteRealm(noteId: Doughnut.ID): Ref<Generated.NoteRealm | undefined>;
}

export class StorageImplementation implements NoteStorage {
  cache: Map<Doughnut.ID, Ref<Generated.NoteRealm | undefined>> = new Map();

  refreshNoteRealm(noteRealm: Generated.NoteRealm): Generated.NoteRealm {
    this.refOfNoteRealm(noteRealm?.id).value = noteRealm;
    return noteRealm;
  }

  refOfNoteRealm(noteId: Doughnut.ID): Ref<Generated.NoteRealm | undefined> {
    if (!this.cache.has(noteId)) {
      this.cache.set(noteId, ref(undefined));
    }
    return this.cache.get(noteId) as Ref<Generated.NoteRealm | undefined>;
  }
}
