import Builder from "./Builder";
import generateId from "./generateId";
import NotebookBuilder from "./NotebookBuilder";

class CircleNoteBuilder extends Builder {
  data: any;

  constructor(parentBuilder?: Builder) {
    super(parentBuilder);
    this.data = {
      id: generateId(),
      name: '',
      invitationCode: '',
      notebooks: [],
      members: []
    };
  }

  notebooks(notebook: NotebookBuilder) {
    this.data.notebooks = [ ...this.data.notebooks, notebook];
    return this;
  }

  do(): any {
    return this.data
  }
}

export default CircleNoteBuilder;
