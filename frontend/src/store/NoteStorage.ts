export default interface NoteStorage {
  selectedNote?: Generated.Note;
  notePosition?: Generated.NotePositionViewedByUser;
  circle?: Generated.Circle;
  updatedNoteRealm?: Generated.NoteRealm;
  updatedAt?: Date;
  focusOnNotebooks(): void;
  selectPosition(
    note?: Generated.Note,
    notePosition?: Generated.NotePositionViewedByUser,
    circle?: Generated.Circle
  ): void;
  refreshNoteRealm(
    data: Generated.NoteRealm | Generated.NoteRealmWithPosition
  ): Generated.NoteRealm;
}

export class StorageImplementation implements NoteStorage {
  selectedNote?: Generated.Note;

  notePosition?: Generated.NotePositionViewedByUser;

  circle?: Generated.Circle;

  updatedNoteRealm?: Generated.NoteRealm;

  updatedAt?: Date;

  focusOnNotebooks(): void {
    this.selectPosition();
    this.updatedNoteRealm = undefined;
    this.updatedAt = new Date();
  }

  refreshNoteRealm(
    data: Generated.NoteRealm | Generated.NoteRealmWithPosition
  ): Generated.NoteRealm {
    let noteRealm: Generated.NoteRealm;
    if (data && "noteRealm" in data) {
      noteRealm = data.noteRealm;
      this.selectPosition(noteRealm.note, data.notePosition);
    } else {
      noteRealm = data;
    }
    this.updatedNoteRealm = noteRealm;
    this.updatedAt = new Date();
    return noteRealm;
  }

  selectPosition(
    note?: Generated.Note,
    notePosition?: Generated.NotePositionViewedByUser,
    circle?: Generated.Circle
  ): void {
    this.selectedNote = note;
    this.notePosition = notePosition;
    this.circle = circle;
  }
}
