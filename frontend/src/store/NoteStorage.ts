export default interface NoteStorage {
  updatedNoteRealm?: Generated.NoteRealm;
  updatedAt?: Date;
  notebookDeleted(): void;
  refreshNoteRealm(
    data: Generated.NoteRealm | Generated.NoteRealmWithPosition
  ): Generated.NoteRealm;
}
