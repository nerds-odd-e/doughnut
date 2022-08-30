export default interface NoteStorage {
  updatedNoteRealm?: Generated.NoteRealm;
  updatedAt?: Date;
  refreshNoteRealm(noteRealm: Generated.NoteRealm): void;
}
