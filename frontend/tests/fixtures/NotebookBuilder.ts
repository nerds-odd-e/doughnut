import Builder from "./Builder";
import generateId from "./generateId";
import NoteSphereBuilder from "./NoteSphereBuilder";

class NotebookBuilder extends Builder {
  data: any;

  constructor(parentBuilder?: Builder) {
    super(parentBuilder);
    this.data = {
      id: generateId(),
      ownership: {
        isFromCircle: false,
      },
      headNote: new NoteSphereBuilder().do(),
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
