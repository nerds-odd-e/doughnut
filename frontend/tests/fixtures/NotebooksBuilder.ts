import Builder from "./Builder";

class NotebooksBuilder extends Builder<Generated.NotebooksViewedByUser> {
  data: Generated.NotebooksViewedByUser;

  constructor() {
    super();
    this.data = {
      notebooks: [],
      subscriptions: [],
    };
  }

  notebooks(notebook: Generated.NotebookViewedByUser) {
    this.data.notebooks = [...this.data.notebooks, notebook];
    return this;
  }

  do(): Generated.NotebooksViewedByUser {
    return this.data;
  }
}

export default NotebooksBuilder;
