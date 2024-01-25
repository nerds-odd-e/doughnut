import noteCreationForm from "./noteForms/noteCreationForm"

export const navigateToCircle = (circleName: string) => {
  cy.routerToRoot()
  cy.pageIsNotLoading()
  cy.openSidebar()
  cy.findByText(circleName, { selector: ".modal-body a" }).click()

  return {
    creatingNotebook(notebookTopic: string) {
      cy.findByText("Add New Notebook In This Circle").click()
      return noteCreationForm.createNote(notebookTopic, undefined, undefined)
    },
    haveMembers(count: number) {
      cy.get("body").find(".circle-member").should("have.length", count)
    },
  }
}
