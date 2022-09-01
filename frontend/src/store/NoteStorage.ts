export default interface NoteStorage {
  notePosition?: Generated.NotePositionViewedByUser;
  updatedNoteRealm?: Generated.NoteRealm;
  updatedAt?: Date;
  focusOnNotebooks(): void;
  setPosition(notePosition?: Generated.NotePositionViewedByUser): void;
  refreshNoteRealm(
    data: Generated.NoteRealm | Generated.NoteRealmWithPosition
  ): Generated.NoteRealm;
}

export class StorageImplementation implements NoteStorage {
  notePosition?: Generated.NotePositionViewedByUser;

  updatedNoteRealm?: Generated.NoteRealm;

  updatedAt?: Date;

  focusOnNotebooks(): void {
    this.notePosition = undefined;
    this.updatedNoteRealm = undefined;
    this.updatedAt = new Date();
  }

  refreshNoteRealm(
    data: Generated.NoteRealm | Generated.NoteRealmWithPosition
  ): Generated.NoteRealm {
    let noteRealm: Generated.NoteRealm;
    if (data && "noteRealm" in data) {
      noteRealm = data.noteRealm;
      this.notePosition = data.notePosition;
    } else {
      noteRealm = data;
    }
    this.updatedNoteRealm = noteRealm;
    this.updatedAt = new Date();
    return noteRealm;
  }

  setPosition(notePosition?: Generated.NotePositionViewedByUser) {
    this.notePosition = notePosition;
  }
}
