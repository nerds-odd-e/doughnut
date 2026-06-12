import { pageIsNotLoading } from '../../pageBase'
import { assumeMemoryTrackerPage } from '../memoryTrackerPage'
import { propertyMemoryTrackerRowLabel } from './shared'

export function assimilationPropertyMemoryTrackerExpectations() {
  return {
    expectPropertyMemoryTracker(propertyKey: string, recallCount = 0) {
      this.expectMemoryTrackerInfo([
        {
          type: propertyMemoryTrackerRowLabel(propertyKey),
          'Recall Count': String(recallCount),
        },
      ])
      return this
    },
    expectPropertyMemoryTrackerAbsent(propertyKey: string) {
      cy.contains('tr', propertyMemoryTrackerRowLabel(propertyKey)).should(
        'not.exist'
      )
      return this
    },
    openPropertyMemoryTracker(propertyKey: string) {
      cy.contains('tr', propertyMemoryTrackerRowLabel(propertyKey)).click()
      cy.url().should('include', '/memory-trackers/')
      pageIsNotLoading()
      return assumeMemoryTrackerPage()
    },
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
      return this
    },
    removeMemoryTrackerFromRecall(type: 'normal' | 'spelling') {
      cy.contains('tr', type).click()
      cy.url().should('include', '/memory-trackers/')
      pageIsNotLoading()
      return assumeMemoryTrackerPage().removeFromRecall()
    },
  }
}
