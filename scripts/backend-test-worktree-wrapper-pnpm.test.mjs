import assert from 'node:assert/strict'
import { existsSync } from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  jdbcUrl,
  makeCheckout,
  readGradleInvocations,
} from './backend-test-worktree-stand-in-fixtures.mjs'
import {
  outputOf,
  runWrapper,
} from './backend-test-worktree-launcher-fixtures.mjs'

function runPnpm(checkout, script) {
  return runWrapper(checkout, { command: 'pnpm', args: [script] })
}

function selectedDatabaseCount(result) {
  return (outputOf(result).match(/Selected database:/g) || []).length
}

function assertIsolatedMigrateThenTest(invocations, expectedUrl) {
  assert.equal(invocations.length, 2)
  const [migrate, testRun] = invocations
  assert.equal(migrate.url, expectedUrl)
  assert.equal(testRun.url, expectedUrl)
  assert.equal(migrate.args.includes('migrateTestDB'), true)
  assert.equal(migrate.args.includes('test'), false)
  assert.equal(testRun.args.includes('test'), true)
  assert.equal(testRun.args.includes('migrateTestDB'), false)
}

test('isolated pnpm backend:test formats then owns one migrate and test run', (t) => {
  const checkout = makeCheckout(t, {
    config: JSON.stringify({ id: 'wt_a7c2' }),
  })
  const result = runPnpm(checkout, 'backend:test')
  assert.equal(result.error, undefined, result.stderr)
  assert.equal(result.status, 0, outputOf(result))
  assert.equal(selectedDatabaseCount(result), 1, outputOf(result))

  const invocations = readGradleInvocations(checkout)
  assert.equal(invocations[0].args.includes('spotlessApply'), true)
  assert.equal(invocations[0].url, '')
  assertIsolatedMigrateThenTest(
    invocations.slice(1),
    jdbcUrl('doughnut_wt_a7c2_test')
  )
})

test('isolated pnpm backend:test_only prepares once before testing', (t) => {
  const checkout = makeCheckout(t, {
    config: JSON.stringify({ id: 'wt_a7c2' }),
  })
  const result = runPnpm(checkout, 'backend:test_only')
  assert.equal(result.status, 0, outputOf(result))
  assert.equal(selectedDatabaseCount(result), 1, outputOf(result))

  const invocations = readGradleInvocations(checkout)
  assert.equal(
    invocations.some((invocation) => invocation.args.includes('spotlessApply')),
    false
  )
  assertIsolatedMigrateThenTest(invocations, jdbcUrl('doughnut_wt_a7c2_test'))
})

test('unconfigured primary pnpm backend:test keeps format migrate then test_only', (t) => {
  const checkout = makePrimaryCheckout(t)
  const result = runPnpm(checkout, 'backend:test')
  assert.equal(result.status, 0, outputOf(result))
  assert.equal(selectedDatabaseCount(result), 0, outputOf(result))
  assert.equal(existsSync(checkout.mysqlInvocation), false)
  assert.equal(
    existsSync(path.join(checkout.root, '.worktree.local.json')),
    false
  )

  const invocations = readGradleInvocations(checkout)
  assert.equal(invocations.length, 3, outputOf(result))
  assert.equal(invocations[0].args.includes('spotlessApply'), true)
  assert.equal(invocations[1].args.includes('migrateTestDB'), true)
  assert.equal(invocations[1].args.includes('test'), false)
  assert.equal(invocations[2].args.includes('test'), true)
  assert.equal(invocations[2].args.includes('migrateTestDB'), false)
  for (const invocation of invocations) {
    assert.equal(invocation.url, '')
  }
})

test('unconfigured primary pnpm backend:test_only does not isolate', (t) => {
  const checkout = makePrimaryCheckout(t)
  const result = runPnpm(checkout, 'backend:test_only')
  assert.equal(result.status, 0, outputOf(result))
  assert.equal(selectedDatabaseCount(result), 0, outputOf(result))

  const invocations = readGradleInvocations(checkout)
  assert.equal(invocations.length, 1, outputOf(result))
  assert.equal(invocations[0].url, '')
  assert.equal(invocations[0].args.includes('test'), true)
  assert.equal(invocations[0].args.includes('migrateTestDB'), false)
})
