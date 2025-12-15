import { assumeMemoryTrackerPage } from './memoryTrackerPage'
import { toolbarButton } from './toolbarButton'

export const makeSureNoteMoreOptionsDialogIsOpen = () => {
  cy.findByRole('button', { name: 'more options' }).then(($button) => {
    if (!$button.hasClass('daisy-btn-active')) {
      cy.wrap($button).click()
    }
  })
  return {
    toolbarButton,
    expectMemoryTrackerInfo(expected: { [key: string]: string }[]) {
      for (const k in expected) {
        cy.contains('tr', expected[k]?.type ?? '').within(() => {
          for (const attr in expected[k]) {
            if (expected[k][attr] !== undefined) {
              cy.contains('td', expected[k][attr])
            }
          }
        })
      }
    },
    removeMemoryTrackerFromReview(type: 'normal' | 'spelling') {
      cy.contains('tr', type).click()
      cy.url().should('include', '/d/memory-trackers/')
      cy.pageIsNotLoading()
      return assumeMemoryTrackerPage().removeFromReview()
    },
  }
}
