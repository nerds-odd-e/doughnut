import { CircleForUserView, Notebook } from "@/generated/backend";
import Builder from "./Builder";
import generateId from "./generateId";
import NotebooksBuilder from "./BazaarNotebooksBuilder";

class CircleNoteBuilder extends Builder<CircleForUserView> {
  notebooksBuilder: NotebooksBuilder = new NotebooksBuilder();

  notebooks(notebook: Notebook) {
    this.notebooksBuilder.notebooks(notebook);
    return this;
  }

  do(): CircleForUserView {
    return {
      id: generateId(),
      name: "",
      invitationCode: "",
      notebooks: {
        notebooks: this.notebooksBuilder
          .do()
          .map((bazaarNotebook) => bazaarNotebook.notebook),
      },
      members: [],
    };
  }
}

export default CircleNoteBuilder;
