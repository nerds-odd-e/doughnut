import { commonSenseSplit } from 'support/string_util'
import { waitUntilAppIsNotBusy } from '../pageBase'
import { RecallsController } from '@generated/doughnut-backend-api/sdk.gen'
import type { DueMemoryTrackers } from '@generated/doughnut-backend-api'

type LearningSessionActionLabel = 'Commission'

function learningSessionActionEntryLabel(
  notebookTitle: string,
  actionLabel: LearningSessionActionLabel
) {
  return `${notebookTitle} — ${actionLabel}`
}

function closeLearningSessionDetailIfOpen() {
  cy.get('body').then(($body) => {
    if (
      $body.find('[data-test="commission-learning-session-dialog"]').length > 0
    ) {
      cy.get('[data-test="commission-learning-session-dialog"]')
        .closest('dialog')
        .find('.close-button')
        .click()
      waitUntilAppIsNotBusy()
    }
  })
}

function openLearningSessionList() {
  closeLearningSessionDetailIfOpen()
  cy.get('[data-test="learning-session-actions"]').click()
  cy.get('[data-test="learning-session-list-dialog"]').should('be.visible')
}

function clickSessionActionFromList(
  notebookTitle: string,
  actionLabel: LearningSessionActionLabel
) {
  cy.contains(
    '[data-test="learning-session-action-entry"]',
    learningSessionActionEntryLabel(notebookTitle, actionLabel)
  ).click()
  cy.get('[data-test="learning-session-list-dialog"]').should('not.exist')
  waitUntilAppIsNotBusy()
}

export const recallLearningSessionMethods = () => ({
  openLearningSessionAction(
    notebookTitle: string,
    actionLabel: LearningSessionActionLabel
  ) {
    openLearningSessionList()
    clickSessionActionFromList(notebookTitle, actionLabel)
    return this
  },
  openLearningSessionRequest(notebookTitle: string) {
    this.openLearningSessionAction(notebookTitle, 'Commission')
    cy.get('[data-test="learning-session-request"]').should('be.visible')
    cy.get('[data-test="commission-learning-session-submit"]').should(
      'not.exist'
    )
    return this
  },
  expectNoLearningSessionForNotebook(notebookTitle: string) {
    cy.get('[data-test="learning-session-awaiting-report"]').should('not.exist')
    cy.get('[data-test="commission-learning-session-dialog"]')
      .closest('dialog')
      .find('.close-button')
      .click()
    cy.get('[data-test="commission-learning-session-dialog"]').should(
      'not.exist'
    )
    waitUntilAppIsNotBusy()
    const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone
    cy.wrap(
      RecallsController.recalling({ query: { timezone, dueindays: 0 } }),
      { log: false }
    ).then((dueMemoryTrackers: DueMemoryTrackers) => {
      const awaiting = dueMemoryTrackers.awaitingReportSessions ?? []
      expect(
        awaiting.filter((session) => session.notebookName === notebookTitle),
        `expected no awaiting learning session for ${notebookTitle}`
      ).to.have.length(0)
    })
    return this
  },
  learningSessionRequestText() {
    return cy
      .get('[data-test="learning-session-request"]')
      .invoke('val')
      .then((value) => String(value ?? ''))
  },
  expectLearningSessionRequestListsNotes(noteTitles: string) {
    commonSenseSplit(noteTitles, ',').forEach((title) => {
      this.learningSessionRequestText().should('contain', `### ${title}`)
    })
    return this
  },
  expectLearningSessionRequestListsOnlyNotes(noteTitles: string) {
    const expected = commonSenseSplit(noteTitles, ',')
    expected.forEach((title) => {
      this.learningSessionRequestText().should('contain', `### ${title}`)
    })
    this.learningSessionRequestText().then((text) => {
      const sessionItemsSection =
        text.match(/<session_items>([\s\S]*?)<\/session_items>/)?.[1] ?? ''
      const itemHeaders = sessionItemsSection.match(/^### .+$/gm) ?? []
      expect(itemHeaders).to.have.length(expected.length)
      for (const header of itemHeaders) {
        const title = header.replace('### ', '')
        expect(expected).to.include(title)
      }
    })
    return this
  },
  expectLearningSessionRequestIncludesLearningStatus(noteTitle: string) {
    this.learningSessionRequestText().should((text) => {
      expect(text).to.contain(`### ${noteTitle}`)
      expect(text).to.match(/not yet tutored|Learning status:/)
    })
    return this
  },
  expectLearningSessionRequestIncludesFocusContextNoteBody(content: string) {
    this.learningSessionRequestText().should((text) => {
      expect(text).not.to.contain('Expected learning content:')
      expect(text).to.contain('<focus_context>')
      expect(text).to.contain('```doughnut-note-md')
      const noteBodies = [
        ...text.matchAll(/```doughnut-note-md\n([\s\S]*?)\n```/g),
      ].map((match) => match[1])
      expect(noteBodies.some((body) => body.includes(content))).to.equal(true)
    })
    return this
  },
  expectLearningSessionRequestIncludesRubric() {
    this.learningSessionRequestText().should(
      'contain',
      'score from 0 to 5 per item'
    )
    return this
  },
  recordLearningSessionReport(reportMarkdown: string) {
    cy.get('[data-test="commission-learning-session-dialog"]')
      .find('[data-test="learning-session-report"]')
      .clear()
      .invoke('val', reportMarkdown)
      .trigger('input')
    cy.get('[data-test="record-learning-session-report-submit"]').click()
    waitUntilAppIsNotBusy()
    return this
  },
  expectLearningSessionReportRecorded() {
    cy.get('[data-test="learning-session-recorded-items"]').should('be.visible')
    return this
  },
  expectPotentialLearningSession(count: number, notebookTitle: string) {
    cy.get('[data-test="learning-session-actions"]').should('be.visible')
    openLearningSessionList()
    if (count === 0) {
      cy.get('[data-test="learning-session-action-entry"]').each(($entry) => {
        expect($entry.text()).not.to.contain(
          learningSessionActionEntryLabel(notebookTitle, 'Commission')
        )
      })
    } else {
      cy.get('[data-test="learning-session-action-entry"]')
        .filter(`:contains("${notebookTitle}")`)
        .filter(':contains("Commission")')
        .should('have.length', count)
    }
    cy.get('.close-button').click()
    cy.get('[data-test="learning-session-list-dialog"]').should('not.exist')
    return this
  },
})
