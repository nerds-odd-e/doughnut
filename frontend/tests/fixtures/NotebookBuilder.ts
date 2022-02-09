import Builder from "./Builder";

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
      ownership: {},
      headNote: {
        id: generateId(),
        shortDescription: '',
        parentId: null,
        orBuildTranslationTextContent: {
          title: ''
        },
        title: ''
      },
      skipReviewEntirely: false
    };
  }

  do(): any {
    return this.data
  }
}

export default NotebookBuilder;
