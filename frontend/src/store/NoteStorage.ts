export default interface NoteStorage {
  updatedNoteRealm?: Generated.NoteRealm;
  updatedAt?: Date;
  refreshNoteRealm(
    data: Generated.NoteRealm | Generated.NoteRealmWithPosition
  ): Generated.NoteRealm;
}
