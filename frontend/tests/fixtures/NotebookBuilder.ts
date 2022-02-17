import Builder from "./Builder";
import NoteBuilder from "./NoteBuilder";

let idCounter = 1;

const generateId = () => {
  return (idCounter++).toString();
};

class NotebookBuilder extends Builder {
  data: any;

  constructor(parentBuilder?: Builder) {
    super(parentBuilder);

  }

  do(): any {
    return {
      id: generateId(),
      ownership: {},
      headNote: new NoteBuilder().do(),
      skipReviewEntirely: false
    };

  }
}

export default NotebookBuilder;
