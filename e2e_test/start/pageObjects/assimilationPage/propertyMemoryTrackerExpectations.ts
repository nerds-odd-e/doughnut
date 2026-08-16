import { waitUntilAppIsNotBusy } from '../../pageBase'
import { assumeMemoryTrackerPage } from '../memoryTrackerPage'
import {
  noteLevelTrackerRowLabel,
  propertyMemoryTrackerRowLabel,
  type NoteLevelTrackerKind,
} from './shared'

function openTrackerRow(rowText: string) {
  cy.contains('tr', rowText).click()
  cy.url().should('include', '/memory-trackers/')
  waitUntilAppIsNotBusy()
  return assumeMemoryTrackerPage()
}

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
      return openTrackerRow(propertyMemoryTrackerRowLabel(propertyKey))
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
    removeMemoryTrackerFromRecall(kind: 'understanding' | 'spelling') {
      return this.openNoteLevelMemoryTracker(kind).removeFromRecall()
    },
    openNoteLevelMemoryTracker(kind: NoteLevelTrackerKind) {
      return openTrackerRow(noteLevelTrackerRowLabel(kind))
    },
  }
}
