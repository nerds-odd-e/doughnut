import { waitUntilAppIsNotBusy } from '../pageBase'
import { form } from '../forms'
import {
  expectAccidentalMatchAlert,
  expectAccidentalMatchRevealForNotes,
  expectNoMatchedNotesOrAccidentalMatch,
  expectOverlapTryAgainAlert,
  openResolveAndClickMatchedNoteCta,
} from './answeredQuestionAccidentalMatch'
import { assumeMemoryTrackerPage } from './memoryTrackerPage'

const addWikiLinkOrRelationshipLabel = 'Add wiki link or relationship'

const waitUntilOnNoteShowPage = () => {
  waitUntilAppIsNotBusy()
  cy.url({ timeout: 15000 }).should('match', /\/d\/n\/\d+|\/n\/\d+|\/n\d+/)
}

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
      ...matchedNoteTitles: string[]
    ) {
      expectAccidentalMatchRevealForNotes(
        answer,
        reviewedNoteTitle,
        matchedNoteTitles
      )
      return self
    },
    openLinkToMatchedNote(matchedNoteTitle: string) {
      openResolveAndClickMatchedNoteCta(
        matchedNoteTitle,
        'wiki-link-or-relationship-to-matched-note-',
        addWikiLinkOrRelationshipLabel
      )
      cy.contains('Target:')
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
      waitUntilOnNoteShowPage()
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
