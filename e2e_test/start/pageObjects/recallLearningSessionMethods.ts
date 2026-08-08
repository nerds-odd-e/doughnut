import { commonSenseSplit } from 'support/string_util'
import { waitUntilAppIsNotBusy } from '../pageBase'

type LearningSessionActionLabel = 'Commission' | 'Record report'

function learningSessionActionEntryLabel(
  notebookTitle: string,
  actionLabel: LearningSessionActionLabel
) {
  return `${notebookTitle} — ${actionLabel}`
}

function clickSessionActionFromPicker(
  notebookTitle: string,
  actionLabel: LearningSessionActionLabel
) {
  cy.get('body').then(($body) => {
    if ($body.find('[data-test="learning-session-actions-picker"]').length) {
      cy.contains(
        '[data-test="learning-session-action-entry"]',
        learningSessionActionEntryLabel(notebookTitle, actionLabel)
      ).click()
    }
    waitUntilAppIsNotBusy()
  })
}

export const recallLearningSessionMethods = () => ({
  openLearningSessionAction(
    notebookTitle: string,
    actionLabel: LearningSessionActionLabel
  ) {
    cy.get('[data-test="learning-session-actions"]').click()
    clickSessionActionFromPicker(notebookTitle, actionLabel)
    return this
  },
  commissionLearningSession(notebookTitle: string) {
    cy.get('body').then(($body) => {
      if (
        $body.find('[data-test="commission-learning-session-submit"]').length >
        0
      ) {
        cy.get('[data-test="commission-learning-session-submit"]').click()
        waitUntilAppIsNotBusy()
        return
      }
      this.openLearningSessionAction(notebookTitle, 'Commission')
      cy.get('[data-test="commission-learning-session-submit"]').click()
      waitUntilAppIsNotBusy()
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
  expectLearningSessionRequestIncludesContent(content: string) {
    this.learningSessionRequestText().should(
      'contain',
      `Expected learning content: ${content}`
    )
    return this
  },
  expectLearningSessionRequestIncludesRubric() {
    this.learningSessionRequestText().should(
      'contain',
      'score from 0 to 5 per item'
    )
    return this
  },
  expectLearningSessionAwaitingReport() {
    cy.get('[data-test="learning-session-awaiting-report"]').should(
      'be.visible'
    )
    return this
  },
  recordLearningSessionReport(
    reportMarkdown: string,
    options?: { notebookTitle?: string }
  ) {
    const fillAndSubmit = () => {
      cy.get('[data-test="commission-learning-session-dialog"]')
        .find('[data-test="learning-session-report"]')
        .clear()
        .invoke('val', reportMarkdown)
        .trigger('input')
      cy.get('[data-test="record-learning-session-report-submit"]').click()
      waitUntilAppIsNotBusy()
    }

    if (!options?.notebookTitle) {
      fillAndSubmit()
      return this
    }

    waitUntilAppIsNotBusy()
    cy.get(
      '[data-test="recorded-learning-session"], [data-test="learning-session-actions"], [data-test="record-learning-session-report-submit"]'
    ).should('exist')

    cy.get('body').then(($body) => {
      if (
        $body.find('[data-test="record-learning-session-report-submit"]')
          .length > 0
      ) {
        fillAndSubmit()
        return
      }
      if ($body.find('[data-test="recorded-learning-session"]').length > 0) {
        cy.contains(
          '[data-test="recorded-learning-session"]',
          options.notebookTitle!
        )
          .find('[data-test="amend-learning-session-report"]')
          .click()
        waitUntilAppIsNotBusy()
        fillAndSubmit()
        return
      }
      this.openLearningSessionAction(options.notebookTitle!, 'Record report')
      fillAndSubmit()
    })
    return this
  },
  expectLearningSessionRecorded() {
    cy.get('[data-test="learning-session-recorded"]').should('be.visible')
    cy.get('[data-test="learning-session-awaiting-report"]').should('not.exist')
    return this
  },
  openAmendLearningSessionReport(notebookTitle: string) {
    cy.contains('[data-test="recorded-learning-session"]', notebookTitle)
      .find('[data-test="amend-learning-session-report"]')
      .click()
    waitUntilAppIsNotBusy()
    cy.get('[data-test="commission-learning-session-dialog"]').should(
      'be.visible'
    )
    cy.get('[data-test="learning-session-recorded"]').should('be.visible')
    return this
  },
  expectPotentialLearningSession(count: number, notebookTitle: string) {
    if (count === 0) {
      cy.get('body').then(($body) => {
        if ($body.find('[data-test="learning-session-actions"]').length > 0) {
          cy.get('[data-test="learning-session-actions"]').click()
          cy.get('[data-test="learning-session-action-entry"]').each(
            ($entry) => {
              expect($entry.text()).not.to.contain(
                learningSessionActionEntryLabel(notebookTitle, 'Commission')
              )
            }
          )
          cy.get('[data-test="learning-session-actions"]').click()
        } else {
          cy.get('[data-test="learning-session-actions"]').should('not.exist')
        }
      })
      return this
    }
    cy.get('[data-test="learning-session-actions"]').should('be.visible')
    cy.get('[data-test="learning-session-actions"]').click()
    cy.get('body').then(($body) => {
      if ($body.find('[data-test="learning-session-actions-picker"]').length) {
        const matchingEntries = $body
          .find('[data-test="learning-session-action-entry"]')
          .filter((_, el) =>
            el.textContent?.includes(
              learningSessionActionEntryLabel(notebookTitle, 'Commission')
            )
          )
        expect(
          matchingEntries.length,
          `commission entries for ${notebookTitle}`
        ).to.eq(count)
        cy.get('[data-test="learning-session-actions"]').click()
      } else {
        cy.get('[data-test="commission-learning-session-dialog"]').should(
          'be.visible'
        )
        cy.contains(`Notebook: "${notebookTitle}"`).should('be.visible')
        cy.get('.close-button').first().click()
      }
    })
    return this
  },
})
