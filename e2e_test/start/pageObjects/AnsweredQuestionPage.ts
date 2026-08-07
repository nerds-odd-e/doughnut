import { waitUntilAppIsNotBusy } from '../pageBase'
import { form } from '../forms'
import {
  expectAccidentalMatchAlert,
  expectNoMatchedNotesOrAccidentalMatch,
  expectOverlapTryAgainAlert,
  openResolveAndClickMatchedNoteCta,
} from './answeredQuestionAccidentalMatch'
import { answeredQuestionRefineMethods } from './answeredQuestionRefine'
import { assumeMemoryTrackerPage } from './memoryTrackerPage'

const assumeAnsweredQuestionPage = () => {
  cy.get('body').should('be.visible')

  const self = {
    expectMCQAnswerToBeCorrect() {
      cy.get('[data-test="question-section"]').within(() => {
        cy.get('.is-correct.is-selected').should('exist')
      })
    },
    expectSpellingAnswerToBeCorrect() {
      cy.findByText('Correct!').should('exist')
      return self
    },
    expectMCQAnswerToBeIncorrect(answer: string) {
      cy.contains('button', answer).should('have.class', 'is-selected')
      cy.contains('button', answer).should('not.have.class', 'is-correct')
      cy.get('.is-correct').should('exist')
    },
    expectSpellingAnswerToBeIncorrect(answer: string) {
      cy.findByText(`Your answer \`${answer}\` is incorrect.`).should('exist')
    },
    expectAccidentalMatchReveal(
      answer: string,
      reviewedNoteTitle: string,
      matchedNoteTitle: string
    ) {
      expectAccidentalMatchAlert(answer)
      cy.findByText(`Your answer \`${answer}\` is incorrect.`).should(
        'not.exist'
      )
      cy.findByTestId('resolve-accidental-match')
        .scrollIntoView()
        .should('be.visible')
        .and('contain.text', 'Resolve accidental match')
      cy.findByText('Note under question').should('be.visible')
      cy.get('[data-test="note-title"]')
        .filter(`:contains("${reviewedNoteTitle}")`)
        .should('have.length.at.least', 1)
      cy.findByTestId('matched-notes-section').should('not.exist')

      cy.findByTestId('resolve-accidental-match').click()
      waitUntilAppIsNotBusy()
      cy.findByTestId('accidental-match-resolve-dialog')
        .should('be.visible')
        .and('contain.text', matchedNoteTitle)
        .and('contain.text', 'English practice')
        .and('contain.text', 'largely overlaps with the current note')
        .within(() => {
          cy.contains('a', matchedNoteTitle).should('be.visible')
          cy.findByTestId('resolve-overlap-explanation').should('be.visible')
        })
      cy.get('.close-button').filter(':visible').first().click()
      cy.findByTestId('accidental-match-resolve-dialog').should('not.exist')

      expectAccidentalMatchAlert(answer)
      cy.get('[data-test="note-title"]')
        .filter(`:contains("${reviewedNoteTitle}")`)
        .should('have.length.at.least', 1)
      return self
    },
    openLinkToMatchedNote(matchedNoteTitle: string) {
      openResolveAndClickMatchedNoteCta(
        matchedNoteTitle,
        'link-to-matched-note-',
        'Build a link'
      )
      cy.contains('Link to:')
        .should('be.visible')
        .parent()
        .should('contain.text', matchedNoteTitle)
      cy.findByPlaceholderText('Search').should('not.exist')
      cy.findByRole('button', { name: 'Insert as a wiki link' }).should(
        'not.exist'
      )
      return self
    },
    openAddAsOverlappedNote(matchedNoteTitle: string) {
      openResolveAndClickMatchedNoteCta(
        matchedNoteTitle,
        'add-as-overlapped-note-',
        'Add as overlapped note'
      )
      waitUntilAppIsNotBusy()
      return self
    },
    expectNoOverlapTryAgainOnAccidentalMatchResult() {
      cy.findByTestId('overlap-try-again').should('not.exist')
      cy.findByTestId('overlap-try-again-alert').should('not.exist')
      return self
    },
    linkMatchedNoteAsProperty(matchedNoteTitle: string) {
      self.openLinkToMatchedNote(matchedNoteTitle)
      cy.findByRole('button', {
        name: 'Add wiki link as a new property',
      }).click()
      waitUntilAppIsNotBusy()
      return self
    },
    linkMatchedNoteAsRelationship(
      matchedNoteTitle: string,
      relationType: string
    ) {
      self.openLinkToMatchedNote(matchedNoteTitle)
      cy.findByRole('button', {
        name: 'Add a new relationship note',
      }).click()
      form.getField('Relation Type').clickOption(relationType)
      waitUntilAppIsNotBusy()
      return self
    },
    expectStillOnAccidentalMatchResult(
      answer: string,
      matchedNoteTitle: string
    ) {
      cy.url().should('include', '/recall')
      expectAccidentalMatchAlert(answer)
      cy.findByTestId('resolve-accidental-match')
        .scrollIntoView()
        .should('be.visible')
      cy.findByTestId('accidental-match-resolve-dialog')
        .should('be.visible')
        .and('contain.text', matchedNoteTitle)
      return self
    },
    openResolveDialog() {
      cy.findByTestId('resolve-accidental-match')
        .scrollIntoView()
        .should('be.visible')
        .click()
      waitUntilAppIsNotBusy()
      cy.findByTestId('accidental-match-resolve-dialog').should('be.visible')
      return self
    },
    clickMatchedNoteTitle(title: string) {
      cy.findByTestId('accidental-match-resolve-dialog').within(() => {
        cy.contains('a', title).should('be.visible').click()
      })
      waitUntilAppIsNotBusy()
      cy.url({ timeout: 15000 }).should('match', /\/d\/n\/\d+|\/n\/\d+|\/n\d+/)
      return self
    },
    goBackToRecallResult() {
      cy.go('back')
      waitUntilAppIsNotBusy()
      cy.url().should('include', '/recall')
      return self
    },
    expectResolveAvailableAgainWithMatch(
      answer: string,
      matchedNoteTitle: string
    ) {
      expectAccidentalMatchAlert(answer)
      cy.findByTestId('resolve-accidental-match')
        .scrollIntoView()
        .should('be.visible')
      cy.findByTestId('accidental-match-resolve-dialog').should('not.exist')
      self.openResolveDialog()
      cy.findByTestId('accidental-match-resolve-dialog')
        .should('be.visible')
        .and('contain.text', matchedNoteTitle)
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
    confirmReAssimilation() {
      cy.contains('re-assimilate').should('be.visible')
      cy.findByRole('button', { name: 'OK' }).click()
    },
  }
  return Object.assign(self, answeredQuestionRefineMethods(self))
}

export { assumeAnsweredQuestionPage }
