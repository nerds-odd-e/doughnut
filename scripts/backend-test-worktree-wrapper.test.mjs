import assert from 'node:assert/strict'
import { realpathSync } from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import {
  jdbcUrl,
  makeCheckout,
  readGradleInvocation,
  readGradleInvocations,
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

test('configured wrapper test from root migrates once then runs tests', (t) => {
  const checkout = makeCheckout(t, {
    config: JSON.stringify({ id: 'wt_a7c2' }),
  })
  const result = runWrapper(checkout, {
    command: 'backend/gradlew',
    args: ['-p', 'backend', 'test'],
  })
  assert.equal(result.error, undefined, result.stderr)
  assert.equal(result.status, 0, outputOf(result))
  assert.match(
    result.stdout,
    /Selected database: doughnut_wt_a7c2_test[\s\S]*GRADLE_STDOUT/
  )
  assert.match(result.stderr, /GRADLE_REACHED/)
  assert.doesNotMatch(outputOf(result), /already running/i)

  const invocations = readGradleInvocations(checkout)
  assert.equal(invocations.length, 2, outputOf(result))
  const [migrate, testRun] = invocations
  const expectedUrl = jdbcUrl('doughnut_wt_a7c2_test')
  assert.equal(migrate.url, expectedUrl)
  assert.equal(testRun.url, expectedUrl)
  assert.equal(migrate.handoff, '1')
  assert.equal(testRun.handoff, '1')
  assert.equal(realpathSync(migrate.cwd), realpathSync(checkout.root))
  assert.equal(realpathSync(testRun.cwd), realpathSync(checkout.root))
  assert.equal(
    realpathSync(testRun.wrapper),
    realpathSync(path.join(checkout.root, 'backend', 'gradlew'))
  )
  assert.equal(migrate.args[migrate.args.indexOf('-p') + 1], 'backend')
  assert.equal(migrate.args.includes('migrateTestDB'), true)
  assert.equal(migrate.args.includes('test'), false)
  assert.equal(testRun.args[testRun.args.indexOf('-p') + 1], 'backend')
  assert.equal(testRun.args.includes('test'), true)
  assert.equal(testRun.args.includes('migrateTestDB'), false)
  assert.equal(testRun.args.includes('-Dspring.profiles.active=test'), true)
  assert.equal(testRun.args.includes('--rerun-tasks'), true)
  assert.equal(testRun.args.includes('--no-build-cache'), true)
  assert.equal(testRun.args.includes('--no-daemon'), true)
})

test('configured wrapper test from backend/ preserves that cwd', (t) => {
  const checkout = makeCheckout(t, {
    config: JSON.stringify({ id: 'wt_a7c2' }),
  })
  const result = runWrapper(checkout, {
    command: './gradlew',
    cwd: path.join(checkout.root, 'backend'),
    args: ['test'],
  })
  assert.equal(result.status, 0, outputOf(result))
  const [migrate, testRun] = readGradleInvocations(checkout)
  const backendDir = path.join(checkout.root, 'backend')
  assert.equal(realpathSync(migrate.cwd), realpathSync(backendDir))
  assert.equal(realpathSync(testRun.cwd), realpathSync(backendDir))
  assert.equal(migrate.args.includes('-p'), false)
  assert.equal(migrate.args.includes('migrateTestDB'), true)
  assert.equal(testRun.args.includes('test'), true)
})

test('configured wrapper test preserves the --tests token after test', (t) => {
  const checkout = makeCheckout(t, {
    config: JSON.stringify({ id: 'wt_a7c2' }),
  })
  const result = runWrapper(checkout, {
    command: 'backend/gradlew',
    args: ['-p', 'backend', 'test', '--tests', 'com.odde.donut.controllers.*'],
  })
  assert.equal(result.status, 0, outputOf(result))
  const { args } = readGradleInvocations(checkout)[1]
  const testAt = args.indexOf('test')
  assert.equal(args[testAt + 1], '--tests')
  assert.equal(args[testAt + 2], 'com.odde.donut.controllers.*')
})

test('configured wrapper unmatched --tests filter keeps gradle failure', (t) => {
  const checkout = makeCheckout(t, {
    config: JSON.stringify({ id: 'wt_a7c2' }),
  })
  const result = runWrapper(checkout, {
    command: 'backend/gradlew',
    args: ['-p', 'backend', 'test', '--tests', 'DoesNotMatch'],
    env: { FAKE_GRADLE_FAIL_INVOCATION: '2', FAKE_GRADLE_EXIT: '1' },
  })
  assert.equal(result.status, 1, outputOf(result))
  assert.equal(readGradleInvocations(checkout).length, 2)
})

test('configured wrapper --continue does not run tests after migrate failure', (t) => {
  const checkout = makeCheckout(t, {
    config: JSON.stringify({ id: 'wt_a7c2' }),
  })
  const result = runWrapper(checkout, {
    command: 'backend/gradlew',
    args: ['-p', 'backend', 'test', '--continue'],
    env: { FAKE_GRADLE_EXIT: '1' },
  })
  assert.notEqual(result.status, 0, outputOf(result))
  const invocations = readGradleInvocations(checkout)
  assert.equal(invocations.length, 1, outputOf(result))
  assert.equal(invocations[0].args.includes('migrateTestDB'), true)
  assert.equal(invocations[0].args.includes('test'), false)
  assert.equal(invocations[0].args.includes('--continue'), false)
})
