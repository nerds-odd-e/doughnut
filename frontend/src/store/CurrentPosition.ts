export default interface CurrentPosition {
  selectedNote?: Generated.Note;
  notePosition?: Generated.NotePositionViewedByUser;
  circle?: Generated.Circle;
  selectPosition(
    note?: Generated.Note,
    notePosition?: Generated.NotePositionViewedByUser,
    circle?: Generated.Circle,
  ): void;
}

export class CurrentPositionImplementation implements CurrentPosition {
  selectedNote?: Generated.Note;

  notePosition?: Generated.NotePositionViewedByUser;

  circle?: Generated.Circle;

  selectPosition(
    note?: Generated.Note,
    notePosition?: Generated.NotePositionViewedByUser,
    circle?: Generated.Circle,
  ): void {
    this.selectedNote = note;
    this.notePosition = notePosition;
    this.circle = circle;
  }
}
