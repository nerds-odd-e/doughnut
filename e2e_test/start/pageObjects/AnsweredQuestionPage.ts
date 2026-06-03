import { pageIsNotLoading } from '../pageBase'
import { assumeMemoryTrackerPage } from './memoryTrackerPage'

const assumeAnsweredQuestionPage = () => {
  cy.get('body').should('be.visible')

  return {
    expectMCQAnswerToBeCorrect() {
      cy.get('[data-test="question-section"]').within(() => {
        cy.get('.is-correct.is-selected').should('exist')
      })
    },
    expectSpellingAnswerToBeCorrect() {
      cy.findByText('Correct!').should('exist')
    },
    expectMCQAnswerToBeIncorrect(answer: string) {
      cy.contains('button', answer).should('have.class', 'is-selected')
      cy.contains('button', answer).should('not.have.class', 'is-correct')
      cy.get('.is-correct').should('exist')
    },
    expectSpellingAnswerToBeIncorrect(answer: string) {
      cy.findByText(`Your answer \`${answer}\` is incorrect.`).should('exist')
    },
    viewMemoryTracker() {
      pageIsNotLoading()
      cy.findByRole('button', { name: 'View Memory Tracker' })
        .should('be.visible')
        .click()
      pageIsNotLoading()
      cy.findByRole('heading', { name: 'Memory Tracker' }).should('be.visible')
      return assumeMemoryTrackerPage()
    },
    goToLastAnsweredQuestion: () => {
      pageIsNotLoading()
      cy.get('body').then(($body) => {
        if ($body.text().includes('Note under question')) {
          return
        }
        cy.get(
          'button[title="view last answered question"], button[title="view previous answered question"]'
        )
          .filter(':visible')
          .first()
          .should('not.be.disabled')
          .click()
      })
      pageIsNotLoading()
      cy.findByText('Note under question').should('be.visible')
      return assumeAnsweredQuestionPage()
    },
    confirmReAssimilation() {
      cy.contains('re-assimilate').should('be.visible')
      cy.findByRole('button', { name: 'OK' }).click()
    },
  }
}

export { assumeAnsweredQuestionPage }
