import Builder from "./Builder";
import NotebookBuilder from "./NotebookBuilder";

class NotePositionBuilder extends Builder<Generated.NotePositionViewedByUser> {
  notebookBuilder = new NotebookBuilder();

  headNote?: Generated.Note;

  for(note: Generated.Note) {
    this.headNote = note;
    return this;
  }

  inBazaar(): NotePositionBuilder {
    this.notebookBuilder.fromBazzar();
    return this;
  }

  inCircle(value: string): NotePositionBuilder {
    this.notebookBuilder.inCircle(value);
    return this;
  }

  do(): Generated.NotePositionViewedByUser {
    const notebook = this.notebookBuilder.headNote(this.headNote).do();
    return {
      noteId: notebook.headNoteId,
      notebook,
      ancestors: [],
    };
  }
}

export default NotePositionBuilder;
