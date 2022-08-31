export default interface NoteStorage {
  notePosition?: Generated.NotePositionViewedByUser;
  updatedNoteRealm?: Generated.NoteRealm;
  updatedAt?: Date;
  notebookDeleted(): void;
  refreshNoteRealm(
    data: Generated.NoteRealm | Generated.NoteRealmWithPosition
  ): Generated.NoteRealm;
}
