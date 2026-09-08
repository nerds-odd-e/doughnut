import assert from 'node:assert/strict'
import fs, { existsSync } from 'node:fs'
import { syncBuiltinESMExports } from 'node:module'
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

test('retirement that finishes after the clear observation refuses and drops the gate', (t) => {
  const checkout = makePrimaryCheckout(t)
  const markerPath = retirementMarkerPath(checkout.root)
  const originalExistsSync = fs.existsSync
  let interleaved = false
  fs.existsSync = (filePath) => {
    const present = originalExistsSync(filePath)
    if (filePath === markerPath && !present && !interleaved) {
      writeRetirementMarker(checkout.root)
      interleaved = true
    }
    return present
  }
  syncBuiltinESMExports()
  try {
    assert.throws(
      () => acquireRetirementAdmission(checkout.root),
      new RegExp(RETIREMENT_MARKER_NAME)
    )
    assert.equal(interleaved, true)
    assert.equal(existsSync(markerPath), true)
    assert.equal(existsSync(retirementAdmissionGatePath(checkout.root)), false)
  } finally {
    fs.existsSync = originalExistsSync
    syncBuiltinESMExports()
  }
})

test('mutation admission may acquire the gate when a retirement marker already exists', (t) => {
  const checkout = makePrimaryCheckout(t)
  writeRetirementMarker(checkout.root)
  acquireRetirementAdmission(checkout.root, { allowRetired: true })
  assert.equal(existsSync(retirementAdmissionGatePath(checkout.root)), true)
  releaseRetirementAdmission(checkout.root)
  assert.equal(existsSync(retirementMarkerPath(checkout.root)), true)
})

test('release does not clear a retirement marker', (t) => {
  const checkout = makePrimaryCheckout(t)
  writeRetirementMarker(checkout.root)
  releaseRetirementAdmission(checkout.root)
  assert.equal(existsSync(retirementMarkerPath(checkout.root)), true)
})
