import assert from 'node:assert/strict'
import { test } from 'node:test'
import { makeMaintenanceEntry } from './application-release-maintenance-fixtures.mjs'

const describe =
  'gcloud compute instance-groups managed describe doughnut-app-group --zone=us-east1-b --format=value(targetSize)'
const update =
  'gcloud compute instance-groups managed update doughnut-app-group --update-policy-type=OPPORTUNISTIC --zone=us-east1-b'
const resize =
  'gcloud compute instance-groups managed resize doughnut-app-group --size=0 --zone=us-east1-b'
const list =
  'gcloud compute instance-groups managed list-instances doughnut-app-group --zone=us-east1-b --format=value(instance)'

test('maintenance entry retains size, switches policy, resizes to zero, then verifies no instance remains', (t) => {
  const fixture = makeMaintenanceEntry(t)
  const result = fixture.enter()

  assert.equal(result.status, 0, result.stderr)
  assert.match(
    result.stdout,
    /Retained original MIG target size for recovery: 2/
  )
  assert.match(result.stdout, /Verified closed/)
  assert.deepEqual(fixture.traceCalls(), [describe, update, resize, list])
  assert.deepEqual(fixture.state(), {
    targetSize: '0',
    updatePolicy: 'OPPORTUNISTIC',
  })
})

test('failed empty-instance verification leaves the MIG durably zero-sized', (t) => {
  const fixture = makeMaintenanceEntry(t, 'never-closes')
  const result = fixture.enter()

  assert.notEqual(result.status, 0)
  assert.match(
    result.stderr,
    /Template assignment and migration are NOT permitted/
  )
  assert.deepEqual(fixture.state(), {
    targetSize: '0',
    updatePolicy: 'OPPORTUNISTIC',
  })
})

for (const command of ['describe', 'update', 'resize', 'list-instances']) {
  test(`process loss after ${command} cannot begin a mixed-version rollout`, (t) => {
    const fixture = makeMaintenanceEntry(t, `fails-after-${command}`)
    const result = fixture.enter()

    assert.notEqual(result.status, 0)
    assert.ok(
      fixture
        .traceCalls()
        .every((call) => !call.includes('set-instance-template'))
    )
    const state = fixture.state()
    assert.equal(
      state.targetSize,
      ['resize', 'list-instances'].includes(command) ? '0' : '2'
    )
    assert.equal(
      state.updatePolicy,
      command === 'describe' ? 'PROACTIVE' : 'OPPORTUNISTIC'
    )
  })
}
