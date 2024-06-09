import { commonSenseSplit } from "support/string_util"

const addToMyLearning = "Add to my learning"

export const bazaarOrCircle = () => {
  return {
    expectNotebooks: (notebooks: string) => {
      cy.pageIsNotLoading()
      cy.get("h5 .topic-text").then(($els) => {
        const cardTitles = Array.from($els, (el) => el.innerText)
        expect(cardTitles).to.deep.eq(commonSenseSplit(notebooks, ","))
      })
    },
    selfAssessmentOnNotebook: (notebook: string) => {
      cy.findNoteCardButton(notebook, "Start Assessment").click()
    },
    generateAssessmentQuestions: (notebook: string) => {
      cy.findNoteCardButton(notebook, "Generate assessment questions").click()
    },
    expectNoAddToMyLearningButton: (noteTopic: string) => {
      cy.findNoteCardButton(noteTopic, addToMyLearning).should("not.exist")
    },
    subscribe: (notebook: string, dailyLearningCount: string) => {
      cy.findNoteCardButton(notebook, addToMyLearning).click()
      cy.get("#subscription-dailyTargetOfNewNotes").clear().type(dailyLearningCount)
      cy.findByRole("button", { name: "Submit" }).click()
    },
  }
}
