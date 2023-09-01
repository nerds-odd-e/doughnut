export default interface CurrentPosition {
  selectedNote?: Generated.Note;
  selectPosition(note?: Generated.Note): void;
}

export class CurrentPositionImplementation implements CurrentPosition {
  selectedNote?: Generated.Note;

  circle?: Generated.Circle;

  selectPosition(note?: Generated.Note): void {
    this.selectedNote = note;
  }
}
