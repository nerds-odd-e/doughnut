import { commonSenseSplit } from 'support/string_util'
import { waitUntilAppIsNotBusy } from '../pageBase'
import { RecallsController } from '@generated/doughnut-backend-api/sdk.gen'
import type { DueMemoryTrackers } from '@generated/doughnut-backend-api'

function learningSessionRequestEntryLabel(notebookTitle: string) {
  return `${notebookTitle} — Request`
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

function clickRequestFromList(notebookTitle: string) {
  cy.contains(
    '[data-test="learning-session-action-entry"]',
    learningSessionRequestEntryLabel(notebookTitle)
  ).click()
  cy.get('[data-test="learning-session-list-dialog"]').should('not.exist')
  waitUntilAppIsNotBusy()
}

export const recallLearningSessionMethods = () => ({
  openLearningSessionRequest(notebookTitle: string) {
    openLearningSessionList()
    clickRequestFromList(notebookTitle)
    cy.get('[data-test="learning-session-request"]').should('be.visible')
    cy.get('[data-test="commission-learning-session-submit"]').should(
      'not.exist'
    )
    return this
  },
  expectNoLearningSessionForNotebook(notebookTitle: string) {
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
      const dueCommissioned = dueMemoryTrackers.dueCommissioned ?? []
      expect(
        dueCommissioned.some(
          (tracker) => tracker.notebookName === notebookTitle
        ),
        `expected due commissioned trackers (potential session) still present for ${notebookTitle}`
      ).to.equal(true)
    })
    openLearningSessionList()
    cy.get('[data-test="learning-session-action-entry"]').each(($entry) => {
      expect($entry.text()).not.to.contain('Record report')
      expect($entry.text()).not.to.contain('Amend report')
    })
    cy.contains(
      '[data-test="learning-session-action-entry"]',
      learningSessionRequestEntryLabel(notebookTitle)
    ).should('be.visible')
    cy.get('.close-button').click()
    cy.get('[data-test="learning-session-list-dialog"]').should('not.exist')
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
  expectLearningSessionRequestIncludesTutoringStatus(noteTitle: string) {
    this.learningSessionRequestText().should((text) => {
      expect(text).to.contain(`### ${noteTitle}`)
      expect(text).to.match(/not yet tutored|Tutoring status:/)
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
    cy.get('body').then(($body) => {
      const entries = $body.find('[data-test="learning-session-action-entry"]')
      entries.each((_, el) => {
        const text = Cypress.$(el).text()
        expect(text).not.to.contain('Record report')
        expect(text).not.to.contain('Amend report')
      })
      const matches = entries
        .filter(`:contains("${notebookTitle}")`)
        .filter(':contains("Request")')
      expect(matches).to.have.length(count)
    })
    cy.get('.close-button').click()
    cy.get('[data-test="learning-session-list-dialog"]').should('not.exist')
    return this
  },
})
