import { pageIsNotLoading } from '../pageBase'
import { assumeMemoryTrackerPage } from './memoryTrackerPage'
import { assumeNotePage } from './notePage'

const assumeAnsweredQuestionPage = () => {
  // Note under question might not always be present immediately
  // Wait for the page to load and check if it exists
  cy.get('body').should('be.visible')

  return {
    expectMCQAnswerToBeCorrect() {
      // Multiple choice question - check that the selected answer is correct
      cy.get('[data-test="question-section"]').within(() => {
        cy.get('.is-correct.is-selected').should('exist')
      })
    },
    expectSpellingAnswerToBeCorrect() {
      // Spelling question - check for the success message
      cy.findByText('Correct!').should('exist')
    },
    expectMCQAnswerToBeIncorrect(answer: string) {
      // Multiple choice question - check visual indicators
      cy.contains('button', answer).should('have.class', 'is-selected')
      cy.contains('button', answer).should('not.have.class', 'is-correct')
      // Check that the correct answer is visually marked (green background)
      cy.get('.is-correct').should('exist')
    },
    expectSpellingAnswerToBeIncorrect(answer: string) {
      // Spelling question - check for the text message
      cy.findByText(`Your answer \`${answer}\` is incorrect.`).should('exist')
    },
    showMemoryTracker(noteTopology: string) {
      pageIsNotLoading()
      cy.findByText('Note under question').should('be.visible')
      cy.findByText('Note under question')
        .closest('.note-under-question')
        .find('a')
        .last()
        .click()
      cy.url({ timeout: 15000 }).should('match', /\/n\d+|\/n\/\d+/)
      pageIsNotLoading()
      return assumeNotePage(noteTopology).openAssimilationSettings()
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
