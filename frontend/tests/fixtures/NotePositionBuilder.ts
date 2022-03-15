import Builder from "./Builder";
import NotebookBuilder from "./NotebookBuilder";


class NotePositionBuilder extends Builder<Generated.NotePositionViewedByUser> {
  notebookBuilder = new NotebookBuilder();

  inBazaar(): NotePositionBuilder {
    this.notebookBuilder.fromBazzar()
    return this
  }

  shortDescription(value: string): NotePositionBuilder {
    this.notebookBuilder.shortDescription(value)
    return this;
  }


  inCircle(value: string): NotePositionBuilder {
    this.notebookBuilder.inCircle(value);
    return this;
  }


  do(): Generated.NotePositionViewedByUser {
    return {
      noteId: 0,
      notebook: this.notebookBuilder.do(),
      ancestors:[]

    }
  }
}

export default NotePositionBuilder;
