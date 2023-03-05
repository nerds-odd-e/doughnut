import Builder from "./Builder";
import generateId from "./generateId";
import NoteBuilder from "./NoteBuilder";

class NotebookBuilder extends Builder<Generated.NotebookViewedByUser> {
  data: Generated.NotebookViewedByUser;

  notebuilder = new NoteBuilder();

  constructor() {
    super();
    this.data = {
      id: generateId(),
      ownership: {
        id: generateId(),
      },
      headNote: this.notebuilder.data,
      headNoteId: this.notebuilder.data.id,
      fromBazaar: false,
      skipReviewEntirely: false,
    };
  }

  headNote(headNote: Generated.Note | undefined) {
    this.notebuilder.for(headNote);
    return this;
  }

  fromBazzar(): NotebookBuilder {
    this.data.fromBazaar = true;
    return this;
  }

  inCircle(value: string): NotebookBuilder {
    this.data.ownership.circle = {
      id: generateId(),
      name: value,
    };
    return this;
  }

  do(): Generated.NotebookViewedByUser {
    this.data.headNote = this.notebuilder.do();
    this.data.headNoteId = this.data.headNote.id;
    return this.data;
  }
}

export default NotebookBuilder;
