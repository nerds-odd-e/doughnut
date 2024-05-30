import { Circle, NotePositionViewedByUser } from "@/generated/backend";
import Builder from "./Builder";
import generateId from "./generateId";

class NotePositionBuilder extends Builder<NotePositionViewedByUser> {
  fromBazaar: boolean = false;

  circle?: Circle = undefined;

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
    };
  }
}

export default NotePositionBuilder;
