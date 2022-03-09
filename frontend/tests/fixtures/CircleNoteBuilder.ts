import Builder from "./Builder";
import NotebookBuilder from "./NotebookBuilder";

let idCounter = 1;

const generateId = () => (idCounter++).toString();

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
