import assert from 'node:assert/strict'
import { realpathSync } from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import {
  jdbcUrl,
  makeCheckout,
  readGradleInvocation,
} from './backend-test-worktree-stand-in-fixtures.mjs'
import {
  outputOf,
  runWrapper,
} from './backend-test-worktree-launcher-fixtures.mjs'

test('configured wrapper migrateTestDB from root selects assigned database without testing', (t) => {
  const checkout = makeCheckout(t, {
    config: JSON.stringify({ id: 'wt_a7c2' }),
  })
  const result = runWrapper(checkout, {
    command: 'backend/gradlew',
    args: ['-p', 'backend', 'migrateTestDB'],
  })
  assert.equal(result.error, undefined, result.stderr)
  assert.equal(result.status, 0, outputOf(result))
  assert.match(
    result.stdout,
    /Selected database: doughnut_wt_a7c2_test[\s\S]*GRADLE_STDOUT/
  )
  assert.match(result.stderr, /GRADLE_REACHED/)
  assert.doesNotMatch(outputOf(result), /already running/i)

  const invocation = readGradleInvocation(checkout)
  assert.equal(
    realpathSync(invocation.wrapper),
    realpathSync(path.join(checkout.root, 'backend', 'gradlew'))
  )
  assert.equal(realpathSync(invocation.cwd), realpathSync(checkout.root))
  assert.equal(invocation.url, jdbcUrl('doughnut_wt_a7c2_test'))
  assert.equal(invocation.handoff, '1')
  assert.equal(invocation.args[invocation.args.indexOf('-p') + 1], 'backend')
  assert.equal(invocation.args.includes('migrateTestDB'), true)
  assert.equal(invocation.args.includes('test'), false)
})

test('configured wrapper migrateTestDB from backend/ preserves that cwd', (t) => {
  const checkout = makeCheckout(t, {
    config: JSON.stringify({ id: 'wt_a7c2' }),
  })
  const result = runWrapper(checkout, {
    command: './gradlew',
    cwd: path.join(checkout.root, 'backend'),
    args: ['migrateTestDB'],
  })
  assert.equal(result.status, 0, outputOf(result))
  const invocation = readGradleInvocation(checkout)
  assert.equal(
    realpathSync(invocation.cwd),
    realpathSync(path.join(checkout.root, 'backend'))
  )
  assert.equal(invocation.url, jdbcUrl('doughnut_wt_a7c2_test'))
  assert.equal(invocation.args.includes('migrateTestDB'), true)
})
