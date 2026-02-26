import { pageIsNotLoading } from '../pageBase'

const assumeQuestionPage = (stem?: string) => {
  if (stem) {
    cy.findByText(stem)
  }
  const question = () => (stem ? cy.findByText(stem).parent().parent() : cy)
  const getQuestionSection = () => cy.get('[data-test="question-section"]')

  return {
    getQuestionSection,
    getStemText() {
      return getQuestionSection()
        .get('[data-test="stem"]')
        .first()
        .invoke('text')
    },
    forNotebook(notebook: string) {
      cy.findByText(notebook, { selector: '.notebook-source *' })
    },
    skipQuestion() {
      pageIsNotLoading()
      getQuestionSection().should('exist')
      cy.get('.daisy-progress-bar').first().click()
      cy.findByRole('button', { name: 'Move to end of list' }).click()
    },
    answerFirstOption() {
      return getQuestionSection().find('button').first().click()
    },
    answer(answer: string) {
      getQuestionSection()
        .should('be.visible')
        .within(() => {
          cy.findByText(answer).should('be.visible').click()
        })
      pageIsNotLoading()
      // Wait for the answered overlay to disappear, indicating it moved to the next stage
      cy.get('[data-test="answered-overlay"]').should('not.exist')
      return this
    },
  }
}

export { assumeQuestionPage }
