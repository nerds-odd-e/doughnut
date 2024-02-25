import { Note, NotebookViewedByUser } from "@/generated/backend";
import Builder from "./Builder";
import generateId from "./generateId";
import NoteBuilder from "./NoteBuilder";

class NotebookBuilder extends Builder<NotebookViewedByUser> {
  data: NotebookViewedByUser;

  notebuilder = new NoteBuilder();

  constructor() {
    super();
    this.data = {
      id: generateId(),
      headNote: this.notebuilder.data,
      headNoteId: this.notebuilder.data.id,
      skipReviewEntirely: false,
    };
  }

  headNote(headNote: Note | undefined) {
    this.notebuilder.for(headNote);
    return this;
  }

  do(): NotebookViewedByUser {
    this.data.headNote = this.notebuilder.do();
    this.data.headNoteId = this.data.headNote.id;
    return this.data;
  }
}

export default NotebookBuilder;
