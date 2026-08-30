import { waitUntilAppIsNotBusy } from '../pageBase'
import {
  findNoteContentRegion,
  richNotePropertyRow,
} from './notePageContentRegion'

const propertyToggleOptionsToggleTestId =
  'rich-note-property-row-options-toggle'

const openPropertyToggleOptionsIfClosed = () => {
  cy.findByTestId(propertyToggleOptionsToggleTestId).then(($toggle) => {
    if ($toggle.attr('aria-expanded') !== 'true') {
      cy.wrap($toggle).click()
    }
  })
}

const withPropertyToggleOptions = (key: string, fn: () => void) => {
  findNoteContentRegion().within(() => {
    cy.get(richNotePropertyRow(key)).within(() => {
      openPropertyToggleOptionsIfClosed()
      fn()
    })
  })
}

const clickPropertyToggleAction = (key: string, testId: string) => {
  withPropertyToggleOptions(key, () => {
    cy.get(`[data-test="${testId}"]`).click()
  })
}

export const noteRichPropertyAssimilationMethods = () => ({
  assimilateRichNotePropertyFromToggle(key: string) {
    this.switchToRichContent()
    clickPropertyToggleAction(key, 'assimilate')
    waitUntilAppIsNotBusy()
    return this
  },
  skipRichNotePropertyFromToggle(key: string) {
    this.switchToRichContent()
    clickPropertyToggleAction(key, 'skip')
    cy.findByRole('button', { name: 'OK' }).click()
    waitUntilAppIsNotBusy()
    return this
  },
  reviveRichNotePropertyFromToggle(key: string) {
    this.switchToRichContent()
    clickPropertyToggleAction(key, 'revive')
    waitUntilAppIsNotBusy()
    return this
  },
  returnRichNotePropertyToSequenceFromToggle(key: string) {
    this.switchToRichContent()
    clickPropertyToggleAction(key, 'return-to-sequence')
    waitUntilAppIsNotBusy()
    return this
  },
  removeRichNotePropertyFromRecallFromToggle(key: string) {
    this.switchToRichContent()
    clickPropertyToggleAction(key, 'remove-from-recall')
    cy.findByRole('button', { name: 'OK' }).click()
    waitUntilAppIsNotBusy()
    return this
  },
  expectRichNotePropertyAssimilateDisabled(key: string) {
    this.switchToRichContent()
    withPropertyToggleOptions(key, () => {
      cy.get('[data-test="assimilate"]').should('be.disabled')
    })
    return this
  },
  expectRichNotePropertyAssimilateEnabled(key: string) {
    this.switchToRichContent()
    withPropertyToggleOptions(key, () => {
      cy.get('[data-test="assimilate"]').should('not.be.disabled')
    })
    return this
  },
  expectRichNotePropertyToggleAction(
    key: string,
    action: 'skip' | 'revive' | 'return-to-sequence' | 'remove-from-recall'
  ) {
    this.switchToRichContent()
    withPropertyToggleOptions(key, () => {
      cy.get(`[data-test="${action}"]`).should('exist')
    })
    return this
  },
  expectAssimilationSettingsAbsent() {
    cy.get('[data-testid="assimilation-settings"]').should('not.exist')
    return this
  },
})
