import assert from 'node:assert/strict'
import { existsSync } from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import {
  makeLinkedWorktreeCheckout,
  makePrimaryCheckout,
} from './backend-test-worktree-linked-fixtures.mjs'
import { lockPaths } from './backend-test-worktree-lock-fixtures.mjs'
import {
  jdbcUrl,
  readGradleInvocation,
  readGradleInvocations,
} from './backend-test-worktree-stand-in-fixtures.mjs'
import {
  outputOf,
  runWrapper,
} from './backend-test-worktree-launcher-fixtures.mjs'

function assertDidNotPrepare(checkout) {
  assert.equal(existsSync(checkout.mysqlInvocation), false)
  assert.equal(existsSync(lockPaths(checkout).dir), false)
}

test('unconfigured primary migrateTestDB passes through without allocating', (t) => {
  const checkout = makePrimaryCheckout(t)
  const result = runWrapper(checkout, {
    command: 'backend/gradlew',
    args: ['-p', 'backend', 'migrateTestDB'],
  })
  assert.equal(result.error, undefined, result.stderr)
  assert.equal(result.status, 0, outputOf(result))
  assertDidNotPrepare(checkout)
  assert.equal(
    existsSync(path.join(checkout.root, '.worktree.local.json')),
    false
  )
  assert.doesNotMatch(outputOf(result), /Allocated new worktree environment/)
  assert.doesNotMatch(outputOf(result), /Selected database:/)

  const invocation = readGradleInvocation(checkout)
  assert.equal(invocation.url, '')
  assert.equal(invocation.args.includes('migrateTestDB'), true)
  assert.equal(invocation.args.includes('--no-daemon'), false)
})

test('unconfigured primary test does not migrate first or isolate', (t) => {
  const checkout = makePrimaryCheckout(t)
  const result = runWrapper(checkout, {
    command: 'backend/gradlew',
    args: ['-p', 'backend', 'test'],
  })
  assert.equal(result.status, 0, outputOf(result))
  assertDidNotPrepare(checkout)
  const invocations = readGradleInvocations(checkout)
  assert.equal(invocations.length, 1, outputOf(result))
  assert.equal(invocations[0].url, '')
  assert.equal(invocations[0].args.includes('test'), true)
  assert.equal(invocations[0].args.includes('migrateTestDB'), false)
  assert.equal(invocations[0].args.includes('--rerun-tasks'), false)
})

test('unconfigured primary preserves an explicit SPRING_DATASOURCE_URL', (t) => {
  const checkout = makePrimaryCheckout(t)
  const callerUrl = jdbcUrl('doughnut_test')
  const result = runWrapper(checkout, {
    command: 'backend/gradlew',
    args: ['-p', 'backend', 'test'],
    env: { SPRING_DATASOURCE_URL: callerUrl },
  })
  assert.equal(result.status, 0, outputOf(result))
  assertDidNotPrepare(checkout)
  assert.equal(readGradleInvocation(checkout).url, callerUrl)
})

test('configured primary migrateTestDB uses the assigned id', (t) => {
  const checkout = makePrimaryCheckout(t, {
    config: JSON.stringify({ id: 'wt_a7c2' }),
  })
  const result = runWrapper(checkout, {
    command: 'backend/gradlew',
    args: ['-p', 'backend', 'migrateTestDB'],
  })
  assert.equal(result.status, 0, outputOf(result))
  assert.match(result.stdout, /Selected database: doughnut_wt_a7c2_test/)
  assert.equal(
    readGradleInvocation(checkout).url,
    jdbcUrl('doughnut_wt_a7c2_test')
  )
})

test('unrelated task on a configured primary does not prepare or lock', (t) => {
  const checkout = makePrimaryCheckout(t, {
    config: JSON.stringify({ id: 'wt_a7c2' }),
  })
  const result = runWrapper(checkout, {
    command: 'backend/gradlew',
    args: ['-p', 'backend', 'spotlessApply'],
  })
  assert.equal(result.status, 0, outputOf(result))
  assertDidNotPrepare(checkout)
  const invocation = readGradleInvocation(checkout)
  assert.equal(invocation.url, '')
  assert.equal(invocation.args.includes('spotlessApply'), true)
  assert.equal(invocation.args.includes('--no-daemon'), false)
})

test('unrelated task on a linked worktree does not allocate', (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  const result = runWrapper(checkout, {
    command: 'backend/gradlew',
    args: ['-p', 'backend', 'help'],
  })
  assert.equal(result.status, 0, outputOf(result))
  assertDidNotPrepare(checkout)
  assert.equal(
    existsSync(path.join(checkout.root, '.worktree.local.json')),
    false
  )
  assert.equal(readGradleInvocation(checkout).url, '')
})

test('unconfigured primary CI-shaped test does not isolate', (t) => {
  const checkout = makePrimaryCheckout(t)
  const result = runWrapper(checkout, {
    command: 'backend/gradlew',
    args: [
      '-p',
      'backend',
      'test',
      '-Dspring.profiles.active=test',
      '--build-cache',
      '--parallel',
    ],
  })
  assert.equal(result.status, 0, outputOf(result))
  assertDidNotPrepare(checkout)
  const invocations = readGradleInvocations(checkout)
  assert.equal(invocations.length, 1, outputOf(result))
  const { args, url } = invocations[0]
  assert.equal(url, '')
  assert.equal(args.includes('-Dspring.profiles.active=test'), true)
  assert.equal(args.includes('--build-cache'), true)
  assert.equal(args.includes('--parallel'), true)
  assert.equal(args.includes('--rerun-tasks'), false)
  assert.equal(args.includes('--no-build-cache'), false)
  assert.equal(args.includes('--no-daemon'), false)
})
