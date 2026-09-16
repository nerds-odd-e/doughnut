import assert from 'node:assert/strict'
import { mkdtempSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { test } from 'node:test'
import {
  completeIsolatedConfig,
  writeIsolatedConfig,
} from './sut-isolated-fixtures.mjs'
import {
  e2eObservationDatabase,
  queryObservedSut,
} from './sut-isolated-target.mjs'

function mysqlInvocation(repoRoot, sql) {
  let invocation
  queryObservedSut(repoRoot, sql, {
    mysqlExecFn(file, args) {
      invocation = { file, args }
      return 'observed\n'
    },
  })
  return invocation
}

function mysqlAuthArgs(args) {
  return args.filter(
    (arg) =>
      arg.startsWith('-u') || (arg.startsWith('-p') && !arg.startsWith('--'))
  )
}

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

test('observation mysql uses E2E SUT doughnut credentials, not passwordless root', () => {
  const checkoutRoot = mkdtempSync(join(tmpdir(), 'e2e-obs-unconfigured-'))
  const { file, args } = mysqlInvocation(checkoutRoot, 'SELECT 1')
  assert.equal(file, 'mysql')
  assert.deepEqual(mysqlAuthArgs(args), ['-udoughnut', '-pdoughnut'])
  assert.ok(args.includes('doughnut_e2e_test'))
})

test('isolated observation mysql still uses the allocated database', () => {
  const checkoutRoot = mkdtempSync(join(tmpdir(), 'e2e-obs-isolated-'))
  writeIsolatedConfig(checkoutRoot, completeIsolatedConfig)
  const { args } = mysqlInvocation(checkoutRoot, 'SELECT 1')
  assert.ok(args.includes('doughnut_e2e_wt_a7c2'))
  assert.equal(args.includes('doughnut_e2e_test'), false)
})
