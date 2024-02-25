import { NoteRealm } from "@/generated/backend";
import { Ref, ref } from "vue";

export default interface NoteStorage {
  refreshNoteRealm(data: NoteRealm): NoteRealm;
  refOfNoteRealm(noteId: Doughnut.ID): Ref<NoteRealm | undefined>;
}

export class StorageImplementation implements NoteStorage {
  cache: Map<Doughnut.ID, Ref<NoteRealm | undefined>> = new Map();

  refreshNoteRealm(noteRealm: NoteRealm): NoteRealm {
    this.refOfNoteRealm(noteRealm?.id).value = noteRealm;
    return noteRealm;
  }

  refOfNoteRealm(noteId: Doughnut.ID): Ref<NoteRealm | undefined> {
    if (!this.cache.has(noteId)) {
      this.cache.set(noteId, ref(undefined));
    }
    return this.cache.get(noteId) as Ref<NoteRealm | undefined>;
  }
}
