import { commonSenseSplit } from 'support/string_util'
import { waitUntilAppIsNotBusy } from '../pageBase'

function learningSessionRequestEntryLabel(notebookTitle: string) {
  return `${notebookTitle} — Request`
}

function donutNoteBodiesIn(markdown: string): string[] {
  return [...markdown.matchAll(/```donut-note-md\n([\s\S]*?)\n```/g)].map(
    (match) => match[1]
  )
}

function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function sessionItemSection(markdown: string, noteTitle: string) {
  const sessionItems = markdown.match(
    /<session_items>[\s\S]*?<\/session_items>/
  )?.[0]
  expect(sessionItems, 'session_items block').to.exist
  const heading = `### ${noteTitle}`
  const headingAt = sessionItems!.indexOf(heading)
  expect(headingAt, `Session Item heading ${noteTitle}`).to.be.greaterThan(-1)
  const nextHeading = sessionItems!.indexOf('### ', headingAt + heading.length)
  return nextHeading === -1
    ? sessionItems!.slice(headingAt)
    : sessionItems!.slice(headingAt, nextHeading)
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
  expectLearningSessionRequestIncludesDatedFeedbacks(
    noteTitle: string,
    feedbacks: Array<{ Grade?: string; Text?: string }>
  ) {
    this.learningSessionRequestText().should((text) => {
      const item = sessionItemSection(text, noteTitle)
      expect(
        item,
        `tutoring status for ${noteTitle} should remain beside dated Feedbacks`
      ).to.match(
        /Tutoring status: \d+ previous sessions?, last on \d{4}-\d{2}-\d{2}/
      )
      let remaining = item
      for (const row of feedbacks) {
        const pattern = new RegExp(
          `- \\d{4}-\\d{2}-\\d{2} — Grade: ${row.Grade}\\n  ${escapeRegExp(row.Text ?? '')}`
        )
        const match = remaining.match(pattern)
        expect(
          match,
          `dated Feedback Grade ${row.Grade} with text ${JSON.stringify(row.Text)} for ${noteTitle}. Actual Session Item:\n${item}`
        ).to.exist
        remaining = remaining.slice((match!.index ?? 0) + match![0].length)
      }
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
