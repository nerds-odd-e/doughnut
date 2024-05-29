import { Note, Thing } from "@/generated/backend";
import Builder from "./Builder";
import NoteBuilder from "./NoteBuilder";
import generateId from "./generateId";

class LinkBuilder extends Builder<Thing> {
  sourceNoteBuilder = new NoteBuilder();

  targetNoteBuilder = new NoteBuilder();

  internalType = Note.linkType.RELATED_TO;

  from(note: Note): LinkBuilder {
    this.sourceNoteBuilder.data = note;
    return this;
  }

  to(note: Note): LinkBuilder {
    this.targetNoteBuilder.data = note;
    return this;
  }

  type(t: Note.linkType): LinkBuilder {
    this.internalType = t;
    return this;
  }

  do(): Thing {
    return {
      id: generateId(),
      note: new NoteBuilder().linkType(this.internalType).do(),
      sourceNote: this.sourceNoteBuilder.do(),
      targetNote: this.targetNoteBuilder.do(),
    };
  }
}

export default LinkBuilder;
