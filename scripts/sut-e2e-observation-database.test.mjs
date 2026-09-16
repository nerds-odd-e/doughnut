import assert from 'node:assert/strict'
import { test } from 'node:test'
import { e2eObservationDatabase } from './sut-isolated-target.mjs'

test('unconfigured checkout observes doughnut_e2e_test', () => {
  assert.equal(
    e2eObservationDatabase({ isolated: false, target: {} }),
    'doughnut_e2e_test'
  )
})

test('unconfigured checkout does not observe doughnut_test', () => {
  assert.equal(
    e2eObservationDatabase({
      isolated: false,
      target: { database: 'doughnut_test' },
    }),
    'doughnut_e2e_test'
  )
})

test('isolated checkout observes the allocated database', () => {
  assert.equal(
    e2eObservationDatabase({
      isolated: true,
      target: { database: 'doughnut_e2e_wt_a7c2' },
    }),
    'doughnut_e2e_wt_a7c2'
  )
})

test('isolated checkout refuses the shared E2E database', () => {
  assert.throws(
    () =>
      e2eObservationDatabase({
        isolated: true,
        target: { database: 'doughnut_e2e_test' },
      }),
    /must not observe the shared E2E database/
  )
})
