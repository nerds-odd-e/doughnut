import { Circle, Note, NotePositionViewedByUser } from "@/generated/backend";
import Builder from "./Builder";
import NoteBuilder from "./NoteBuilder";
import generateId from "./generateId";

class NotePositionBuilder extends Builder<NotePositionViewedByUser> {
  fromBazaar: boolean = false;

  circle?: Circle = undefined;

  headNoteBuilder = new NoteBuilder();

  headNote?: Note;

  for(note: Note) {
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

  do(): NotePositionViewedByUser {
    return {
      noteId: generateId(),
      fromBazaar: this.fromBazaar,
      circle: this.circle,
      ancestors: [],
    };
  }
}

export default NotePositionBuilder;
