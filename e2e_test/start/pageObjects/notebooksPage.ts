import { assumeNotePage } from "./notePage"
import NotePath from "../../support/NotePath"
import noteCreationForm from "./noteForms/noteCreationForm"

export const routerToNotebooksPage = () => {
  cy.routerPush("/notebooks", "notebooks", {})
  return {
    navigateToPath(notePath: NotePath) {
      return notePath.path.reduce(
        (page, noteTopic) => page.navigateToChild(noteTopic),
        assumeNotePage(),
      )
    },
    creatingNotebook(notebookTopic: string) {
      cy.findByText("Add New Notebook").click()
      return noteCreationForm.createNote(notebookTopic, undefined, undefined);
    },
  }
}
