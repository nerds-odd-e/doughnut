import { waitUntilAppIsNotBusy } from '../pageBase'
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
  waitUntilAppIsNotBusy()
}

const SCOPED_README_BODY_TEST_IDS = [
  'folder-readme-body',
  'notebook-readme-body',
] as const

/**
 * Note body (`role=region` "Note content") or notebook/folder scoped readme
 * editor (`*-readme-body`), whichever is on the current page.
 */
export function findNoteContentRegion() {
  return cy.get('body').then(($body) => {
    for (const testId of SCOPED_README_BODY_TEST_IDS) {
      const scoped = $body.find(`[data-testid="${testId}"]`)
      if (scoped.length > 0) {
        return cy.wrap(scoped.first())
      }
    }
    return cy.findByRole(noteContentRegion.role, {
      name: noteContentRegion.name,
    })
  })
}
