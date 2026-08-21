import { commonSenseSplit } from 'support/string_util'
import { waitUntilAppIsNotBusy } from '../pageBase'

function learningSessionRequestEntryLabel(notebookTitle: string) {
  return `${notebookTitle} — Request`
}

function doughnutNoteBodiesIn(markdown: string): string[] {
  return [...markdown.matchAll(/```doughnut-note-md\n([\s\S]*?)\n```/g)].map(
    (match) => match[1]
  )
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
  expectLearningSessionRequestIncludesFocusNoteBody(content: string) {
    this.learningSessionRequestText().should((text) => {
      expect(text).not.to.contain('Expected learning content:')
      expect(text).to.contain('<focus_note>')
      const focusSections = [
        ...text.matchAll(/<focus_note>[\s\S]*?<\/focus_note>/g),
      ].map((match) => match[0])
      expect(focusSections.length, 'focus_note blocks').to.be.greaterThan(0)
      expect(
        doughnutNoteBodiesIn(focusSections.join('\n')).some((body) =>
          body.includes(content)
        )
      ).to.equal(true)
    })
    return this
  },
  expectLearningSessionRequestIncludesRelatedNoteBody(content: string) {
    this.learningSessionRequestText().should((text) => {
      expect(text).to.contain('<related_notes>')
      expect(text).to.contain('<retrieved_note>')
      const relatedSection = text.match(
        /<related_notes>[\s\S]*?<\/related_notes>/
      )?.[0]
      expect(relatedSection, 'related_notes block').to.exist
      expect(
        doughnutNoteBodiesIn(relatedSection!).some((body) =>
          body.includes(content)
        )
      ).to.equal(true)
    })
    return this
  },
  expectLearningSessionRequestIncludesRubric() {
    this.learningSessionRequestText().should((text) => {
      expect(text).to.contain('Grade from 1 to 4 per item')
      expect(text).to.contain('<session_item_grades>')
      expect(text).to.contain('Hola: 4')
    })
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
