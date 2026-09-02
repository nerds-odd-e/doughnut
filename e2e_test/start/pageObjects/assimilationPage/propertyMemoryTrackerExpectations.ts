import { waitUntilAppIsNotBusy } from '../../pageBase'
import router from '../../router'
import testability from '../../testability'
import { assumeMemoryTrackerPage } from '../memoryTrackerPage'
import {
  noteLevelTrackerStatusElement,
  type NoteLevelTrackerKind,
} from './shared'

function openMemoryTrackerPage() {
  cy.url().should('include', '/memory-trackers/')
  waitUntilAppIsNotBusy()
  return assumeMemoryTrackerPage()
}

export function assimilationPropertyMemoryTrackerExpectations() {
  return {
    expectPropertyMemoryTracker(propertyKey: string, recallCount = 0) {
      testability()
        .propertyMemoryTrackerForCurrentNote(propertyKey)
        .then((tracker) => {
          expect(
            Boolean(tracker && tracker.removedFromTracking !== true),
            `expected an active property memory tracker for "${propertyKey}"`
          ).to.equal(true)
          expect(
            String(tracker?.recallCount ?? 0),
            `expected recall count ${recallCount} for property memory tracker "${propertyKey}"`
          ).to.equal(String(recallCount))
        })
      return this
    },
    expectPropertyMemoryTrackerAbsent(propertyKey: string) {
      testability()
        .propertyMemoryTrackerForCurrentNote(propertyKey)
        .then((tracker) => {
          expect(
            !tracker || tracker.removedFromTracking === true,
            `expected no active property memory tracker for "${propertyKey}"`
          ).to.equal(true)
        })
      return this
    },
    openPropertyMemoryTracker(propertyKey: string) {
      testability()
        .propertyMemoryTrackerForCurrentNote(propertyKey)
        .then((tracker) => {
          expect(
            tracker,
            `expected a property memory tracker for "${propertyKey}"`
          ).to.exist
          router().push('memoryTrackerShow', {
            memoryTrackerId: tracker!.id,
          })
        })
      return openMemoryTrackerPage()
    },
    openNoteLevelMemoryTracker(kind: NoteLevelTrackerKind) {
      noteLevelTrackerStatusElement(kind).click()
      return openMemoryTrackerPage()
    },
    removeMemoryTrackerFromRecall(kind: 'understanding' | 'spelling') {
      return this.openNoteLevelMemoryTracker(kind).removeFromRecall()
    },
  }
}
