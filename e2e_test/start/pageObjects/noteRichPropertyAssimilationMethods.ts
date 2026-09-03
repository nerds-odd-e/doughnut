import { waitUntilAppIsNotBusy } from '../pageBase'
import { switchToRichContentIfNeeded } from './noteContentEditingMethods'
import {
  findNoteContentRegion,
  richNotePropertyPanelToggleTestId,
  richNotePropertyRow,
} from './notePageContentRegion'

const openPropertyPanelIfClosed = () => {
  cy.findByTestId(richNotePropertyPanelToggleTestId).then(($toggle) => {
    if ($toggle.attr('aria-expanded') !== 'true') {
      cy.wrap($toggle).click()
    }
  })
}

// The property row markup (and its status link) only exists in rich content
// view, so callers that reach a property panel through this helper no
// longer need to remember `switchToRichContent()` themselves — including
// `clickPropertyTrackerStatusLink`, whose test-only callers scrape the
// panel from wherever the previous step left the note (e.g. the markdown
// editor).
const withPropertyPanel = (key: string, fn: () => void) => {
  switchToRichContentIfNeeded()
  findNoteContentRegion().within(() => {
    cy.get(richNotePropertyRow(key)).within(() => {
      openPropertyPanelIfClosed()
      fn()
    })
  })
}

export const clickPropertyPanelAction = (key: string, testId: string) => {
  withPropertyPanel(key, () => {
    cy.get(`[data-test="${testId}"]`).click()
  })
}

export const noteRichPropertyAssimilationMethods = () => ({
  assimilateRichNotePropertyFromPanel(key: string) {
    this.switchToRichContent()
    clickPropertyPanelAction(key, 'assimilate-UNDERSTANDING')
    waitUntilAppIsNotBusy()
    return this
  },
  skipRichNotePropertyFromPanel(key: string) {
    this.switchToRichContent()
    clickPropertyPanelAction(key, 'skip')
    cy.findByRole('button', { name: 'OK' }).click()
    waitUntilAppIsNotBusy()
    return this
  },
  returnRichNotePropertyToSequenceFromPanel(key: string) {
    this.switchToRichContent()
    clickPropertyPanelAction(key, 'return-to-sequence')
    waitUntilAppIsNotBusy()
    return this
  },
  expectRichNotePropertyAssimilateDisabled(key: string) {
    this.switchToRichContent()
    withPropertyPanel(key, () => {
      cy.get('[data-test="assimilate-UNDERSTANDING"]').should('not.exist')
      cy.get('[data-test="assimilation-status-UNDERSTANDING"]').should('exist')
    })
    return this
  },
  expectRichNotePropertyAssimilateEnabled(key: string) {
    this.switchToRichContent()
    withPropertyPanel(key, () => {
      cy.get('[data-test="assimilate-UNDERSTANDING"]').should('not.be.disabled')
    })
    return this
  },
  expectRichNotePropertyPanelAction(
    key: string,
    action: 'skip' | 'return-to-sequence'
  ) {
    this.switchToRichContent()
    withPropertyPanel(key, () => {
      cy.get(`[data-test="${action}"]`).should('exist')
    })
    return this
  },
  expectAssimilationSettingsAbsent() {
    cy.get('[data-testid="note-assimilation-modes"]').should('not.exist')
    return this
  },
})
