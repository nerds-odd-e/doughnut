import { CircleForUserView, NotebookViewedByUser } from "@/generated/backend";
import Builder from "./Builder";
import generateId from "./generateId";
import NotebooksBuilder from "./NotebooksBuilder";

class CircleNoteBuilder extends Builder<CircleForUserView> {
  notebooksBuilder: NotebooksBuilder = new NotebooksBuilder();

  notebooks(notebook: NotebookViewedByUser) {
    this.notebooksBuilder.notebooks(notebook);
    return this;
  }

  do(): CircleForUserView {
    return {
      id: generateId(),
      name: "",
      invitationCode: "",
      notebooks: this.notebooksBuilder.do(),
      members: [],
    };
  }
}

export default CircleNoteBuilder;
