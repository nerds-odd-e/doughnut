import Builder from "./Builder";
import generateId from "./generateId";
import NotebooksBuilder from "./NotebooksBuilder";

class CircleNoteBuilder extends Builder<Generated.CircleForUserView> {
  notebooksBuilder: NotebooksBuilder = new NotebooksBuilder()

  notebooks(notebook: Generated.NotebookViewedByUser) {
    this.notebooksBuilder.notebooks(notebook)
    return this;
  }

  do(): Generated.CircleForUserView {
    return {
      id: generateId(),
      name: '',
      invitationCode: '',
      notebooks: this.notebooksBuilder.do(),
      members: []
    };

  }
}

export default CircleNoteBuilder;
