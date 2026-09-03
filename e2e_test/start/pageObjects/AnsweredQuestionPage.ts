import { waitUntilAppIsNotBusy } from '../pageBase'
import {
  expectAccidentalMatchRevealForNotes,
  expectNoMatchedNotesOrAccidentalMatch,
  expectOverlapTryAgainAlert,
} from './answeredQuestionAccidentalMatch'
import { assumeMemoryTrackerPage } from './memoryTrackerPage'

const assumeAnsweredQuestionPage = () => {
  cy.get('body').should('be.visible')

  const self = {
    expectSpellingAnswerToBeCorrect() {
      cy.findByText('Correct!').should('exist')
      return self
    },
    expectMCQAnswerToBeIncorrect(answer: string) {
      cy.contains('button', answer).should(($btn) => {
        expect(
          $btn.hasClass('is-selected'),
          `Expected MCQ choice "${answer}" to be selected`
        ).to.eq(true)
        expect(
          $btn.hasClass('is-correct'),
          `Expected MCQ choice "${answer}" not to be marked correct`
        ).to.eq(false)
      })
      cy.get('.is-correct').should('exist')
      return self
    },
    expectSpellingAnswerToBeIncorrect(answer: string) {
      cy.findByText(`Your answer \`${answer}\` is incorrect.`).should('exist')
      return self
    },

    expectAccidentalMatchReveal(
      answer: string,
      reviewedNoteTitle: string,
      matchedNoteTitle: string
    ) {
      expectAccidentalMatchRevealForNotes(
        answer,
        reviewedNoteTitle,
        matchedNoteTitle
      )
      return self
    },
    expectOverlapTryAgainForSpelling() {
      expectOverlapTryAgainAlert()
      return self
    },
    expectNoMatchedNotesOrAccidentalMatchOnOverlap() {
      expectNoMatchedNotesOrAccidentalMatch()
      return self
    },
    trySpellingQuestionAgain() {
      cy.findByTestId('overlap-try-again').scrollIntoView().click()
      waitUntilAppIsNotBusy()
      return self
    },
    viewMemoryTracker() {
      waitUntilAppIsNotBusy()
      cy.findByRole('button', { name: 'View Memory Tracker' })
        .should('be.visible')
        .click()
      waitUntilAppIsNotBusy()
      cy.findByRole('heading', { name: 'Memory Tracker' }).should('be.visible')
      return assumeMemoryTrackerPage()
    },
    goToLastAnsweredQuestion: () => {
      waitUntilAppIsNotBusy()
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
      waitUntilAppIsNotBusy()
      cy.findByText('Note under question').should('be.visible')
      return assumeAnsweredQuestionPage()
    },
    expectFrequentFailureWarningForNote(
      wrongCount: number,
      periodDays: number
    ) {
      waitUntilAppIsNotBusy()
      cy.get('dialog')
        .filter(':visible')
        .should(
          'contain.text',
          `You've answered incorrectly ${wrongCount} times within the last ${periodDays} days.`
        )
      return self
    },
  }
  return self
}

export { assumeAnsweredQuestionPage }
