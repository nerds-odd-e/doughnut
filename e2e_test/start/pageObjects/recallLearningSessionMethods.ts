import { commonSenseSplit } from 'support/string_util'
import { waitUntilAppIsNotBusy } from '../pageBase'
import testability from '../testability'

const LEARNING_SESSION_REPORT_RECORDED_AT_ALIAS =
  'learningSessionReportRecordedAt'

function learningSessionRequestEntryLabel(notebookTitle: string) {
  return `${notebookTitle} — Request`
}

function donutNoteBodiesIn(markdown: string): string[] {
  return [...markdown.matchAll(/```donut-note-md\n([\s\S]*?)\n```/g)].map(
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
        donutNoteBodiesIn(focusSections.join('\n')).some((body) =>
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
        donutNoteBodiesIn(relatedSection!).some((body) =>
          body.includes(content)
        )
      ).to.equal(true)
    })
    return this
  },
  expectLearningSessionRequestInstructsDescriptiveFeedback() {
    this.learningSessionRequestText().should((text) => {
      expect(
        text,
        'how_to_report should ask for Grade plus descriptive text'
      ).to.contain('Grade from 1 to 4 and descriptive text per item')
      const howToReport = text.match(
        /<how_to_report>[\s\S]*?<\/how_to_report>/
      )?.[0]
      expect(howToReport, 'how_to_report block').to.exist
      expect(
        howToReport,
        'how_to_report example should wrap session_item_feedback in a fenced code block'
      ).to.match(/```(?:markdown)?\n[\s\S]*<session_item_feedback>[\s\S]*```/)
    })
    return this
  },
  recordLearningSessionReport(reportMarkdown: string) {
    cy.intercept('POST', '**/api/learning-sessions/record**').as(
      'recordLearningSessionReport'
    )
    cy.get('[data-test="commission-learning-session-dialog"]')
      .find('[data-test="learning-session-report"]')
      .clear()
      .invoke('val', reportMarkdown)
      .trigger('input')
    cy.get('[data-test="record-learning-session-report-submit"]').click()
    cy.wait('@recordLearningSessionReport').then((interception) => {
      const recordedAt = interception.response?.body?.recordedAt
      expect(recordedAt, 'learning session report response recordedAt')
        .to.be.a('string')
        .and.not.equal('')
      cy.wrap(recordedAt as string).as(
        LEARNING_SESSION_REPORT_RECORDED_AT_ALIAS
      )
      cy.log(`learning session report recordedAt=${recordedAt}`)
    })
    waitUntilAppIsNotBusy()
    return this
  },
  expectCommissionedTrackersScheduledAfterReport(noteTitles: string) {
    cy.get<string>(`@${LEARNING_SESSION_REPORT_RECORDED_AT_ALIAS}`).then(
      (recordedAt) => {
        const reportTime = new Date(recordedAt).getTime()
        expect(
          Number.isFinite(reportTime),
          `report recordedAt (${recordedAt}) should parse as a time`
        ).to.equal(true)
        commonSenseSplit(noteTitles, ',').forEach((noteTitle) => {
          testability()
            .memoryTrackerForNote(noteTitle, 'COMMISSIONED')
            .then((tracker) => {
              const nextRecallAt = tracker.nextRecallAt
              expect(
                nextRecallAt,
                `commissioned tracker for "${noteTitle}" (id=${tracker.id}) should have Next Recall Time`
              )
                .to.be.a('string')
                .and.not.equal('')
              const nextTime = new Date(nextRecallAt).getTime()
              cy.log(
                `commissioned "${noteTitle}" id=${tracker.id} nextRecallAt=${nextRecallAt} report recordedAt=${recordedAt}`
              )
              expect(
                nextTime,
                `commissioned tracker for "${noteTitle}" (id=${tracker.id}) nextRecallAt (${nextRecallAt}) should be after report recordedAt (${recordedAt})`
              ).to.be.greaterThan(reportTime)
            })
        })
      }
    )
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
