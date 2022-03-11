import Builder from "./Builder";
import NotebookBuilder from "./NotebookBuilder";

const idCounter = 1;

class BazaarNotebooksBuilder extends Builder {
  data: any;

  constructor(parentBuilder?: Builder) {
    super(parentBuilder);
    this.data = {
      notebooks: [],
      subscriptions: []
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

export default BazaarNotebooksBuilder;
