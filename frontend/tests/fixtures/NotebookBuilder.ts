import { Note, Notebook } from "@/generated/backend";
import Builder from "./Builder";
import generateId from "./generateId";
import NoteBuilder from "./NoteBuilder";

class NotebookBuilder extends Builder<Notebook> {
  data: Notebook;

  notebuilder = new NoteBuilder();

  constructor() {
    super();
    this.data = {
      id: generateId(),
      headNote: this.notebuilder.data,
      notebookSettings: {
        skipReviewEntirely: false,
      },
    };
  }

  headNote(headNote: Note | undefined) {
    this.notebuilder.for(headNote);
    return this;
  }

  do(): Notebook {
    this.data.headNote = this.notebuilder.do();
    return this.data;
  }
}

export default NotebookBuilder;
