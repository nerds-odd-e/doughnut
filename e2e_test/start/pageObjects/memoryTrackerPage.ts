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

const recordLabeledValueAs = (label: string, alias: string) => {
  labeledValue(label).then((text) => {
    cy.wrap(text).as(alias)
  })
}

const expectLabeledValue = (label: string, expected: string) => {
  labeledValue(label).then((text) => {
    expect(text).to.equal(expected)
  })
}

const expectLabeledValueUnchanged = (
  label: string,
  alias: string,
  message: (recorded: string) => string
) => {
  labeledValue(label).then((actual) => {
    cy.get<string>(`@${alias}`).then((recorded) => {
      expect(actual, message(recorded)).to.equal(recorded)
    })
  })
}

const labeledPair = (firstLabel: string, secondLabel: string) =>
  labeledValue(firstLabel).then((first) =>
    labeledValue(secondLabel).then((second) => ({ first, second }))
  )

const recallLogs = () => cy.get('[data-testid="recall-log"]')

const assumeMemoryTrackerPage = () => {
  return {
    removeFromRecall() {
      expectMemoryTrackerPage()
      cy.findByRole('button', {
        name: /remove this note from recall/i,
      })
        .should('be.visible')
        .click()
      cy.findByRole('button', { name: 'OK' }).click()
      cy.findByText(SKIPPED_MEMORY_TRACKER_MESSAGE)
      return assumeMemoryTrackerPage()
    },
    reviveMemoryTracker() {
      expectMemoryTrackerPage()
      cy.findByRole('button', {
        name: /revive this memory tracker/i,
      })
        .should('be.visible')
        .click()
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
    expectDifficulty(difficulty: number | string) {
      expectMemoryTrackerPage()
      expectLabeledValue('Difficulty:', String(difficulty))
      return assumeMemoryTrackerPage()
    },
    expectLastRecallTime(value: string) {
      expectMemoryTrackerPage()
      expectLabeledValue('Last Recall Time:', value)
      return assumeMemoryTrackerPage()
    },
    expectNextRecallTimeEqualsAssimilatedTime() {
      expectMemoryTrackerPage()
      labeledPair('Assimilated Time:', 'Next Recall Time:').then(
        ({ first: assimilatedTime, second: nextRecallTime }) => {
          expect(
            nextRecallTime,
            `Next Recall Time (${nextRecallTime}) should equal Assimilated Time (${assimilatedTime})`
          ).to.equal(assimilatedTime)
        }
      )
      return assumeMemoryTrackerPage()
    },
    expectTrackerType(type: string) {
      expectMemoryTrackerPage()
      expectLabeledValue('Type:', type)
      return assumeMemoryTrackerPage()
    },
    expectNoteTitle(noteTitle: string) {
      expectMemoryTrackerPage()
      cy.findByText('Note under question').should('be.visible')
      cy.contains('.note-under-question', noteTitle).should('be.visible')
      return assumeMemoryTrackerPage()
    },
    expectFocusedProperty(propertyKey: string) {
      expectMemoryTrackerPage()
      cy.findByTestId('focused-property-indicator')
        .should('be.visible')
        .and('contain.text', `Focused property: ${propertyKey}`)
      return assumeMemoryTrackerPage()
    },
    captureSchedule() {
      expectMemoryTrackerPage()
      recordLabeledValueAs('Last Recall Time:', 'recordedLastRecallTime')
      recordLabeledValueAs('Next Recall Time:', 'recordedNextRecallTime')
      recordLabeledValueAs('Recall Count:', 'recordedRecallCount')
      return assumeMemoryTrackerPage()
    },
    expectBroughtForwardWithoutRecallCredit() {
      expectMemoryTrackerPage()
      expectLabeledValueUnchanged(
        'Last Recall Time:',
        'recordedLastRecallTime',
        (recorded) =>
          `Last Recall Time should stay ${recorded} without recall credit`
      )
      expectLabeledValueUnchanged(
        'Recall Count:',
        'recordedRecallCount',
        (recorded) =>
          `Recall Count should stay ${recorded} without recall credit`
      )
      labeledValue('Next Recall Time:').then((nextRecallTime) => {
        cy.get<string>('@recordedNextRecallTime').then((recorded) => {
          const next = new Date(nextRecallTime).getTime()
          const before = new Date(recorded).getTime()
          expect(
            next,
            `Next Recall Time (${nextRecallTime}) should be earlier than recorded ${recorded}`
          ).to.be.lessThan(before)
        })
      })
      return assumeMemoryTrackerPage()
    },
    expectGoodRecallLogWithoutAnswer() {
      expectMemoryTrackerPage()
      recallLogs().should('have.length', 1)
      cy.get('[data-testid="recall-log-product-outcome"]').should(
        'have.text',
        'GOOD'
      )
      recallLogs().should('contain.text', 'Recorded:')
      recallLogs().should('contain.text', 'Elapsed hours:')
      cy.get('[data-testid="recall-log-answer-id"]').should('not.exist')
      return assumeMemoryTrackerPage()
    },
    expectAgainRecallLog() {
      expectMemoryTrackerPage()
      recallLogs().should('have.length', 2)
      recallLogs().contains(
        '[data-testid="recall-log-product-outcome"]',
        'AGAIN'
      )
      return assumeMemoryTrackerPage()
    },
  }
}

export { assumeMemoryTrackerPage }
