import { commonSenseSplit } from "support/string_util"

export const bazaar = () => {
  cy.visit("/bazaar")

  return {
    sharedNotebooks: (notebooks: string) => {
      cy.pageIsNotLoading()
      cy.get("h5 .topic-text").then(($els) => {
        const cardTitles = Array.from($els, (el) => el.innerText)
        expect(cardTitles).to.deep.eq(commonSenseSplit(notebooks, ","))
      })
    },
    selfAssessmentOnNotebook: (notebook: string) => {
      cy.contains(notebook).parents(".card").findByTitle("Start Assessment").click()
    },
    generateAssessmentQuestions: (notebook: string) => {
      cy.findNoteCardButton(notebook, "Generate assessment questions").click()
    },
    expectNoAddToMyLearningButton: (noteTopic: string) => {
      cy.findNoteCardButton(noteTopic, "Add to my learning").should("not.exist")
    },
  }
}
