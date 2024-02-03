import Builder from "./Builder";
import NoteBuilder from "./NoteBuilder";
import generateId from "./generateId";

class NotePositionBuilder extends Builder<Generated.NotePositionViewedByUser> {
  fromBazaar: boolean = false;

  circle?: Generated.Circle = undefined;

  headNoteBuilder = new NoteBuilder();

  headNote?: Generated.Note;

  for(note: Generated.Note) {
    this.headNote = note;
    return this;
  }

  inBazaar(): NotePositionBuilder {
    this.fromBazaar = true;
    return this;
  }

  inCircle(value: string): NotePositionBuilder {
    this.circle = {
      id: generateId(),
      name: value,
    };
    return this;
  }

  do(): Generated.NotePositionViewedByUser {
    return {
      noteId: generateId(),
      fromBazaar: this.fromBazaar,
      circle: this.circle,
      ancestors: [],
    };
  }
}

export default NotePositionBuilder;
