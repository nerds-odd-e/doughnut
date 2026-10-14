import { waitUntilAppIsNotBusy } from '../pageBase'
import { memoryTrackerRecallHistoryMethods } from './memoryTrackerRecallHistory'

const SKIPPED_MEMORY_TRACKER_MESSAGE =
  'This memory tracker is currently skipped and will not appear in recall sessions.'

const expectMemoryTrackerPage = () => {
  cy.findByRole('heading', { name: 'Memory Tracker' }).should('be.visible')
}

const labeledValue = (label: string) =>
  cy
    .contains('span.font-semibold', label)
    .siblings('span')
    .invoke('text')
    .then((text) => text.trim())

const expectLabeledValue = (label: string, expected: string) => {
  labeledValue(label).then((text) => {
    expect(text).to.equal(expected)
  })
}

const labeledPair = (firstLabel: string, secondLabel: string) =>
  labeledValue(firstLabel).then((first) =>
    labeledValue(secondLabel).then((second) => ({ first, second }))
  )

const assumeMemoryTrackerPage = () => {
  return {
    ...memoryTrackerRecallHistoryMethods(expectMemoryTrackerPage),
    removeFromRecall() {
      expectMemoryTrackerPage()
      cy.findByRole('button', {
        name: /remove this note from recall/i,
      })
        .should('be.visible')
        .click()
      cy.findByRole('button', { name: 'OK' }).click()
      waitUntilAppIsNotBusy()
      return assumeMemoryTrackerPage()
    },
    reviveMemoryTracker() {
      expectMemoryTrackerPage()
      cy.findByRole('button', {
        name: /revive this memory tracker/i,
      })
        .should('be.visible')
        .click()
      waitUntilAppIsNotBusy()
      return assumeMemoryTrackerPage()
    },
    expectAvailableForRecall() {
      expectMemoryTrackerPage()
      cy.findByText(SKIPPED_MEMORY_TRACKER_MESSAGE).should('not.exist')
      cy.findByRole('button', {
        name: /remove this note from recall/i,
      }).should('be.visible')
      return assumeMemoryTrackerPage()
    },
    expectRecallCount(count: number) {
      expectMemoryTrackerPage()
      expectLabeledValue('Recall Count:', String(count))
      return assumeMemoryTrackerPage()
    },
    expectHoursBetweenLastAndNextRecall(hours: number) {
      expectMemoryTrackerPage()
      labeledPair('Last Recall Time:', 'Next Recall Time:').then(
        ({ first: lastRecallTime, second: nextRecallTime }) => {
          const intervalInHours =
            (new Date(nextRecallTime).getTime() -
              new Date(lastRecallTime).getTime()) /
            3_600_000
          expect(
            intervalInHours,
            `Expected ${hours} hours between Last Recall Time (${lastRecallTime}) and Next Recall Time (${nextRecallTime})`
          ).to.equal(hours)
        }
      )
      return assumeMemoryTrackerPage()
    },
    expectStability(stability: number) {
      expectMemoryTrackerPage()
      expectLabeledValue('Stability:', String(stability))
      return assumeMemoryTrackerPage()
    },
    expectDifficulty(difficulty: number) {
      expectMemoryTrackerPage()
      expectLabeledValue('Difficulty:', String(difficulty))
      return assumeMemoryTrackerPage()
    },
  }
}

export { assumeMemoryTrackerPage }
