import { noteIdFromUrl } from '../../noteIdFromUrl'
import { waitUntilAppIsNotBusy } from '../../pageBase'
import router from '../../router'
import testability from '../../testability'
import { assumeMemoryTrackerPage } from '../memoryTrackerPage'
import {
  clickPropertyTrackerStatusLink,
  noteLevelTrackerStatusElement,
  type NoteLevelTrackerKind,
} from './shared'

function openMemoryTrackerPage() {
  cy.url().should('include', '/memory-trackers/')
  waitUntilAppIsNotBusy()
  return assumeMemoryTrackerPage()
}

/**
 * Reads a tracker's recall count on its own tracker page, then returns to
 * the note the caller came from (by note id, not `cy.go('back')` — the
 * click into the tracker page is a same-origin router-link, but navigating
 * back deterministically by named route is more robust for subsequent
 * steps that read state from the current note's URL). Shared by note-level
 * and property-level recall-count assertions, which differ only in how
 * they get from the note onto the tracker page.
 */
function expectRecallCountAndReturnToNote(
  openTrackerPage: () => ReturnType<typeof assumeMemoryTrackerPage>,
  count: number
) {
  cy.url().then((url) => {
    const noteId = noteIdFromUrl(url)
    openTrackerPage().expectRecallCount(count)
    router().push('noteShow', { noteId })
    waitUntilAppIsNotBusy()
  })
}

export function assimilationPropertyMemoryTrackerExpectations() {
  return {
    /**
     * Clicking the status link doubles as the "tracker exists" assertion
     * (Cypress fails clearly if the link isn't there); the recall count
     * read and return-to-note then follow
     * `expectRecallCountAndReturnToNote`, the same path note-level trackers
     * use.
     */
    expectPropertyMemoryTracker(propertyKey: string, recallCount = 0) {
      expectRecallCountAndReturnToNote(() => {
        clickPropertyTrackerStatusLink(propertyKey)
        return openMemoryTrackerPage()
      }, recallCount)
      return this
    },
    /**
     * Note-level counterpart of `expectPropertyMemoryTracker`: reads recall
     * count on the tracker page reached via `openNoteLevelMemoryTracker`,
     * then returns to the note.
     */
    expectNoteLevelMemoryTrackerRecallCount(
      kind: 'understanding' | 'spelling',
      count: number
    ) {
      expectRecallCountAndReturnToNote(
        () => this.openNoteLevelMemoryTracker(kind),
        count
      )
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
        })
      clickPropertyTrackerStatusLink(propertyKey)
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
