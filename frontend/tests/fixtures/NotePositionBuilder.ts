import Builder from "./Builder";
import NotebookBuilder from "./NotebookBuilder";


class NotePositionBuilder extends Builder {
  data: any;

  notebookBuilder = new NotebookBuilder();

  constructor(parentBuilder?: Builder) {
    super(parentBuilder);
    this.data = {
      owns: true,
      ancestors: [],
    }
  }

  inBazaar(): NotePositionBuilder {
    this.data.owns = false
    this.data.headNote = {}
    return this
  }

  shortDescription(value: string): NotePositionBuilder {
    this.data.headNote.shortDescription = value;
    return this;
  }


  inCircle(value: string): NotePositionBuilder {
    this.data.owns = true;
    this.notebookBuilder.inCircle(value);
    return this;
  }


  do(): any {
    this.data.notebook = this.notebookBuilder.do();
    return this.data
  }
}

export default NotePositionBuilder;
