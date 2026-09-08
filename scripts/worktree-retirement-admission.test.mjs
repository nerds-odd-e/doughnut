import assert from 'node:assert/strict'
import { existsSync } from 'node:fs'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  RETIREMENT_ADMISSION_GATE_DIR_NAME,
  RETIREMENT_MARKER_NAME,
  acquireRetirementAdmission,
  releaseRetirementAdmission,
  retirementAdmissionGatePath,
  retirementMarkerPath,
  writeRetirementMarker,
} from './worktree-retirement-admission.mjs'

test('runner admission and a second gate acquire cannot overlap', (t) => {
  const checkout = makePrimaryCheckout(t)
  acquireRetirementAdmission(checkout.root)
  assert.equal(existsSync(retirementAdmissionGatePath(checkout.root)), true)
  assert.throws(
    () => acquireRetirementAdmission(checkout.root),
    new RegExp(RETIREMENT_ADMISSION_GATE_DIR_NAME)
  )
  releaseRetirementAdmission(checkout.root)
  assert.equal(existsSync(retirementAdmissionGatePath(checkout.root)), false)
  acquireRetirementAdmission(checkout.root)
  releaseRetirementAdmission(checkout.root)
})

test('seeded retirement marker refuses admission before any gate is created', (t) => {
  const checkout = makePrimaryCheckout(t)
  writeRetirementMarker(checkout.root)
  assert.equal(existsSync(retirementMarkerPath(checkout.root)), true)
  assert.throws(
    () => acquireRetirementAdmission(checkout.root),
    new RegExp(RETIREMENT_MARKER_NAME)
  )
  assert.equal(existsSync(retirementAdmissionGatePath(checkout.root)), false)
})

test('release does not clear a retirement marker', (t) => {
  const checkout = makePrimaryCheckout(t)
  writeRetirementMarker(checkout.root)
  releaseRetirementAdmission(checkout.root)
  assert.equal(existsSync(retirementMarkerPath(checkout.root)), true)
})
