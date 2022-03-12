import Builder from "./Builder";
import NoteSphereBuilder from "./NoteSphereBuilder";

let idCounter = 1;

const generateId = () => {
	idCounter += 1;
	return idCounter.toString();
}

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
