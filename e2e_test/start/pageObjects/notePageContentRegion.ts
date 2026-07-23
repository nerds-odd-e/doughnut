import { pageIsNotLoading } from '../pageBase'
import { clickPopupConfirmOk } from '../../support/daisyModalHelpers'

export const noteContentRegion = {
  role: 'region' as const,
  name: 'Note content',
}

export const richNotePropertyRow = (key: string) =>
  `[data-testid="rich-note-property-row"][data-property-key="${key}"]`

export function confirmPropertyMemoryTrackerChange() {
  cy.get('dialog', { timeout: 15000 })
    .filter(':visible')
    .contains('memory tracker')
    .should('be.visible')
  clickPopupConfirmOk()
  pageIsNotLoading()
}

export function findNoteContentRegion() {
  return cy.findByRole(noteContentRegion.role, {
    name: noteContentRegion.name,
  })
}
