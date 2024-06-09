import { assumeNotePage } from "./notePage"
import NotePath from "../../support/NotePath"
import noteCreationForm from "./noteForms/noteCreationForm"
import { notebookList } from "./NotebookList"

export const routerToNotebooksPage = () => {
  cy.pageIsNotLoading()
  cy.routerPush("/notebooks", "notebooks", {})
  cy.findByText("Notebooks")
  return {
    ...notebookList(),
    navigateToPath(notePath: NotePath) {
      return notePath.path.reduce(
        (page, noteTopic) => page.navigateToChild(noteTopic),
        assumeNotePage(),
      )
    },
    creatingNotebook(notebookTopic: string) {
      cy.findByText("Add New Notebook").click()
      return noteCreationForm.createNote(notebookTopic, undefined, undefined)
    },
    shareNotebookToBazaar(notebook: string) {
      this.findNotebookCardButton(notebook, "Share notebook to bazaar").click()
      cy.findByRole("button", { name: "OK" }).click()
    },
  }
}
