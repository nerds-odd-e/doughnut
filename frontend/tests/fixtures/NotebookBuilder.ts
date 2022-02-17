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
    this.data = {
      id: generateId(),
      ownership: {
        isFromCircle: false,
      },
      headNote: new NoteBuilder().do(),
      skipReviewEntirely: false
    }
  }

  inCircle(value: string): NotebookBuilder {
    this.data.ownership = {
      isFromCircle: true,
      circle: {
        name: value,
      },
    };
    return this;
  }

  do(): any {
    return this.data;
  }
}

export default NotebookBuilder;
