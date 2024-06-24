import { commonSenseSplit } from "support/string_util"

const addToMyLearning = "Add to my learning"

export const notebookList = () => {
  return {
    expectNotebooks: (notebooks: string) => {
      cy.pageIsNotLoading()
      cy.get("h5 .topic-text").then(($els) => {
        const cardTitles = Array.from($els, (el) => el.innerText)
        expect(cardTitles).to.deep.eq(commonSenseSplit(notebooks, ","))
      })
    },
    findNotebookCardButton: (notebook: string, name: string) => {
      const finder = () =>
        cy
          .findCardTitle(notebook)
          .parent()
          .parent()
          .parent()
          .parent()
          .parent()
          .findByRole("button", { name: name })

      return {
        click() {
          finder().click()
        },
        shouldNotExist() {
          finder().should("not.exist")
        },
      }
    },
    notebookAssistant(notebook: string) {
      this.findNotebookCardButton(notebook, "Notebook Assistant").click()
      return {
        create() {
          cy.findByRole("button", { name: "Create Assistant For Notebook"}).click()
        },
      }
    },
  }
}

export const bazaarOrCircle = () => {
  return {
    ...notebookList(),
    selfAssessmentOnNotebook(notebook: string) {
      this.findNotebookCardButton(notebook, "Start Assessment").click()
    },
    expectNoAddToMyLearningButton(noteTopic: string) {
      this.findNotebookCardButton(noteTopic, addToMyLearning).shouldNotExist()
    },
    subscribe(notebook: string, dailyLearningCount: string) {
      this.findNotebookCardButton(notebook, addToMyLearning).click()
      cy.get("#subscription-dailyTargetOfNewNotes")
        .clear()
        .type(dailyLearningCount)
      cy.findByRole("button", { name: "Submit" }).click()
    },
  }
}
