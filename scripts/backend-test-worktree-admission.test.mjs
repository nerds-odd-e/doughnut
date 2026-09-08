import assert from 'node:assert/strict'
import { existsSync, mkdirSync } from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import {
  assertRefusedBeforeGradle,
  outputOf,
  runLauncher,
} from './backend-test-worktree-launcher-fixtures.mjs'
import { makeCheckout } from './backend-test-worktree-stand-in-fixtures.mjs'
import {
  RETIREMENT_ADMISSION_GATE_DIR_NAME,
  RETIREMENT_MARKER_NAME,
  retirementAdmissionGatePath,
  writeRetirementMarker,
} from './worktree-retirement-admission.mjs'

const configuredId = 'wt_a7c2'

function configuredCheckout(t) {
  return makeCheckout(t, {
    config: JSON.stringify({ id: configuredId }),
  })
}

test('held retirement admission gate refuses backend launch before gradle or mysql', (t) => {
  const checkout = configuredCheckout(t)
  mkdirSync(retirementAdmissionGatePath(checkout.root))
  const result = runLauncher(checkout)
  assertRefusedBeforeGradle(checkout, result)
  assert.equal(existsSync(checkout.mysqlInvocation), false)
  assert.match(outputOf(result), new RegExp(RETIREMENT_ADMISSION_GATE_DIR_NAME))
  assert.equal(
    existsSync(path.join(checkout.root, '.worktree.local.lock')),
    false
  )
})

test('seeded retirement marker refuses backend launch before provisioning', (t) => {
  const checkout = configuredCheckout(t)
  writeRetirementMarker(checkout.root)
  const result = runLauncher(checkout, { env: { FAKE_SCHEMA_MISSING: '1' } })
  assertRefusedBeforeGradle(checkout, result)
  assert.equal(existsSync(checkout.mysqlInvocation), false)
  assert.match(outputOf(result), new RegExp(RETIREMENT_MARKER_NAME))
  assert.doesNotMatch(outputOf(result), /Provisioning database administration/)
  assert.equal(
    existsSync(path.join(checkout.root, '.worktree.local.lock')),
    false
  )
})
