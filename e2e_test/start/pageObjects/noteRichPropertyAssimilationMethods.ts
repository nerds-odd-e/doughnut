import { waitUntilAppIsNotBusy } from '../pageBase'
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

const withPropertyPanel = (key: string, fn: () => void) => {
  findNoteContentRegion().within(() => {
    cy.get(richNotePropertyRow(key)).within(() => {
      openPropertyPanelIfClosed()
      fn()
    })
  })
}

const clickPropertyPanelAction = (key: string, testId: string) => {
  withPropertyPanel(key, () => {
    cy.get(`[data-test="${testId}"]`).click()
  })
}

export const noteRichPropertyAssimilationMethods = () => ({
  assimilateRichNotePropertyFromPanel(key: string) {
    this.switchToRichContent()
    clickPropertyPanelAction(key, 'assimilate')
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
  reviveRichNotePropertyFromPanel(key: string) {
    this.switchToRichContent()
    clickPropertyPanelAction(key, 'revive')
    waitUntilAppIsNotBusy()
    return this
  },
  returnRichNotePropertyToSequenceFromPanel(key: string) {
    this.switchToRichContent()
    clickPropertyPanelAction(key, 'return-to-sequence')
    waitUntilAppIsNotBusy()
    return this
  },
  removeRichNotePropertyFromRecallFromPanel(key: string) {
    this.switchToRichContent()
    clickPropertyPanelAction(key, 'remove-from-recall')
    cy.findByRole('button', { name: 'OK' }).click()
    waitUntilAppIsNotBusy()
    return this
  },
  expectRichNotePropertyAssimilateDisabled(key: string) {
    this.switchToRichContent()
    withPropertyPanel(key, () => {
      cy.get('[data-test="assimilate"]').should('be.disabled')
    })
    return this
  },
  expectRichNotePropertyAssimilateEnabled(key: string) {
    this.switchToRichContent()
    withPropertyPanel(key, () => {
      cy.get('[data-test="assimilate"]').should('not.be.disabled')
    })
    return this
  },
  expectRichNotePropertyPanelAction(
    key: string,
    action: 'skip' | 'revive' | 'return-to-sequence' | 'remove-from-recall'
  ) {
    this.switchToRichContent()
    withPropertyPanel(key, () => {
      cy.get(`[data-test="${action}"]`).should('exist')
    })
    return this
  },
  expectAssimilationSettingsAbsent() {
    cy.get('[data-testid="assimilation-settings"]').should('not.exist')
    return this
  },
})
