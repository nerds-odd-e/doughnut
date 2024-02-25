import {
  NotebookViewedByUser,
  NotebooksViewedByUser,
} from "@/generated/backend";
import Builder from "./Builder";

class NotebooksBuilder extends Builder<NotebooksViewedByUser> {
  data: NotebooksViewedByUser;

  constructor() {
    super();
    this.data = {
      notebooks: [],
      subscriptions: [],
    };
  }

  notebooks(notebook: NotebookViewedByUser) {
    this.data.notebooks = [...this.data.notebooks, notebook];
    return this;
  }

  do(): NotebooksViewedByUser {
    return this.data;
  }
}

export default NotebooksBuilder;
