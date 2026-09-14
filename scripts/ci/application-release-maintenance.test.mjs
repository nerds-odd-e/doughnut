import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  makeMaintenanceEntry,
  OLD_TEMPLATE_NAME,
  COMPATIBLE_TEMPLATE_NAME,
} from './application-release-maintenance-fixtures.mjs'

test('maintenance entry sets the compatible instance template, stops, waits and verifies quiescence in order before permitting migration', (t) => {
  const { enter, traceCalls, describeCurrentTemplate } = makeMaintenanceEntry(
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
    `gcloud compute instance-groups managed set-instance-template doughnut-app-group --template=${COMPATIBLE_TEMPLATE_NAME} --zone=us-east1-b`
  )
  assert.equal(
    calls[1],
    'gcloud compute instance-groups managed stop-instances doughnut-app-group --zone=us-east1-b --all-instances'
  )
  // Quiescence verification polls list-instances after the stop call, in
  // order, until it observes no active instance statuses.
  const listInstancesCalls = calls.slice(2)
  assert.ok(listInstancesCalls.length >= 2, 'expected more than one poll')
  for (const call of listInstancesCalls) {
    assert.equal(
      call,
      'gcloud compute instance-groups managed list-instances doughnut-app-group --zone=us-east1-b --format=value(instanceStatus)'
    )
  }

  // The instance-template assignment is durable MIG-owned state, not
  // something this script's own process holds: a later, independent
  // gcloud invocation (standing in for autohealing, the update policy's
  // proactive convergence, or an operator retry) still observes the
  // compatible template, never the old one.
  assert.equal(describeCurrentTemplate(), COMPATIBLE_TEMPLATE_NAME)
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
    `gcloud compute instance-groups managed set-instance-template doughnut-app-group --template=${COMPATIBLE_TEMPLATE_NAME} --zone=us-east1-b`
  )
  assert.equal(
    calls[1],
    'gcloud compute instance-groups managed stop-instances doughnut-app-group --zone=us-east1-b --all-instances'
  )
  assert.equal(
    calls[2],
    'gcloud compute instance-groups managed list-instances doughnut-app-group --zone=us-east1-b --format=value(instanceStatus)'
  )
})

test('a process/connection loss between the template assignment and stop-instances leaves the compatible template durably set, migration not permitted', (t) => {
  const { enter, traceCalls, describeCurrentTemplate } = makeMaintenanceEntry(
    t,
    'interrupted-after-template-set'
  )

  const result = enter()

  assert.notEqual(result.status, 0)
  assert.doesNotMatch(result.stdout, /Migration is permitted/)

  const calls = traceCalls()
  assert.equal(
    calls[0],
    `gcloud compute instance-groups managed set-instance-template doughnut-app-group --template=${COMPATIBLE_TEMPLATE_NAME} --zone=us-east1-b`
  )
  assert.equal(
    calls[1],
    'gcloud compute instance-groups managed stop-instances doughnut-app-group --zone=us-east1-b --all-instances'
  )
  // No quiescence polling was reached; the script died at the stop call.
  assert.equal(calls.length, 2)

  // Precondition proof: even though this run never reached quiescence
  // (and therefore never permitted migration), the instance-template
  // assignment already committed to GCP's own MIG state before the
  // interruption. Any subsequent instance start for this MIG -- whether
  // GCP autohealing recreating a since-stopped instance, the PROACTIVE
  // update policy converging remaining instances, or an operator's manual
  // retry/recreate-instances -- necessarily boots this same compatible
  // template, never the old one.
  assert.equal(describeCurrentTemplate(), COMPATIBLE_TEMPLATE_NAME)
})

test('a missing compatible-template input fails before any gcloud call, leaving the prior template untouched', (t) => {
  const { enter, traceCalls, describeCurrentTemplate } = makeMaintenanceEntry(
    t,
    'quiesces-after-polling'
  )

  const result = enter({ MAINTENANCE_INSTANCE_TEMPLATE: '' })

  assert.notEqual(result.status, 0)
  assert.match(result.stderr, /MAINTENANCE_INSTANCE_TEMPLATE/)
  assert.doesNotMatch(result.stdout, /Migration is permitted/)
  assert.equal(traceCalls().length, 0, 'no gcloud call should be made')
  assert.equal(describeCurrentTemplate(), OLD_TEMPLATE_NAME)
})
