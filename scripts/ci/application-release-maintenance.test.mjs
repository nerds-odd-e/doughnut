import assert from 'node:assert/strict'
import { test } from 'node:test'
import { makeMaintenanceEntry } from './application-release-maintenance-fixtures.mjs'

test('maintenance entry stops, waits and verifies quiescence in order before permitting migration', (t) => {
  const { enter, traceCalls } = makeMaintenanceEntry(
    t,
    'quiesces-after-polling'
  )

  const result = enter()

  assert.equal(result.status, 0, result.stderr)
  assert.match(result.stdout, /Verified quiescent/)
  assert.match(result.stdout, /Migration is permitted/)

  const calls = traceCalls()
  assert.equal(
    calls[0],
    'gcloud compute instance-groups managed stop-instances doughnut-app-group --zone=us-east1-b --all-instances'
  )
  // Quiescence verification polls list-instances after the stop call, in
  // order, until it observes no active instance statuses.
  const listInstancesCalls = calls.slice(1)
  assert.ok(listInstancesCalls.length >= 2, 'expected more than one poll')
  for (const call of listInstancesCalls) {
    assert.equal(
      call,
      'gcloud compute instance-groups managed list-instances doughnut-app-group --zone=us-east1-b --format=value(instanceStatus)'
    )
  }
})

test('failed quiescence verification prevents migration', (t) => {
  const { enter, traceCalls } = makeMaintenanceEntry(
    t,
    'quiescence-never-verified'
  )

  const result = enter()

  assert.notEqual(result.status, 0)
  assert.match(result.stderr, /Quiescence verification failed/)
  assert.match(result.stderr, /Migration is NOT permitted/)
  assert.doesNotMatch(result.stdout, /Migration is permitted/)

  const calls = traceCalls()
  assert.equal(
    calls[0],
    'gcloud compute instance-groups managed stop-instances doughnut-app-group --zone=us-east1-b --all-instances'
  )
  assert.equal(
    calls[1],
    'gcloud compute instance-groups managed list-instances doughnut-app-group --zone=us-east1-b --format=value(instanceStatus)'
  )
})
