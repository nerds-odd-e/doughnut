import assert from 'node:assert/strict'
import { readFileSync, rmSync } from 'node:fs'
import { test } from 'node:test'
import { makeLinkedWorktreeCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  jdbcUrl,
  readGradleInvocation,
  readGradleInvocations,
  readMysqlInvocation,
} from './backend-test-worktree-stand-in-fixtures.mjs'
import {
  outputOf,
  runWrapper,
} from './backend-test-worktree-launcher-fixtures.mjs'
import { lockPaths } from './backend-test-worktree-lock-fixtures.mjs'

const allocatedIdPattern =
  /Allocated new worktree environment: (wt_[a-z0-9]{32})/

test('fresh linked worktree migrateTestDB allocates once and migrates without testing', (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)

  const result = runWrapper(checkout, {
    command: 'backend/gradlew',
    args: ['-p', 'backend', 'migrateTestDB'],
  })
  assert.equal(result.error, undefined, result.stderr)
  assert.equal(result.status, 0, outputOf(result))

  const match = outputOf(result).match(allocatedIdPattern)
  assert.ok(match, outputOf(result))
  const [, allocatedId] = match
  const database = `doughnut_${allocatedId}_test`
  assert.match(
    readMysqlInvocation(checkout).args[7],
    new RegExp(`CREATE DATABASE ${database} `)
  )
  assert.deepEqual(
    JSON.parse(readFileSync(`${checkout.root}/.worktree.local.json`, 'utf8')),
    { id: allocatedId }
  )
  assert.match(outputOf(result), new RegExp(`Selected database: ${database}`))

  const invocation = readGradleInvocation(checkout)
  assert.equal(invocation.url, jdbcUrl(database))
  assert.equal(invocation.args.includes('migrateTestDB'), true)
  assert.equal(invocation.args.includes('test'), false)
})

test('fresh linked worktree test allocates then migrates once before tests', (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  const result = runWrapper(checkout, {
    command: 'backend/gradlew',
    args: ['-p', 'backend', 'test'],
  })
  assert.equal(result.status, 0, outputOf(result))

  const match = outputOf(result).match(allocatedIdPattern)
  assert.ok(match, outputOf(result))
  const [, allocatedId] = match
  const expectedUrl = jdbcUrl(`doughnut_${allocatedId}_test`)

  const invocations = readGradleInvocations(checkout)
  assert.equal(invocations.length, 2, outputOf(result))
  assert.equal(invocations[0].url, expectedUrl)
  assert.equal(invocations[1].url, expectedUrl)
  assert.equal(invocations[0].args.includes('migrateTestDB'), true)
  assert.equal(invocations[0].args.includes('test'), false)
  assert.equal(invocations[1].args.includes('test'), true)
  assert.equal(invocations[1].args.includes('migrateTestDB'), false)
})

test('later ordinary command in the same linked worktree reuses the allocated id', (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  const first = runWrapper(checkout, {
    command: 'backend/gradlew',
    args: ['-p', 'backend', 'migrateTestDB'],
  })
  assert.equal(first.status, 0, outputOf(first))

  const configPath = `${checkout.root}/.worktree.local.json`
  const provisionedConfig = readFileSync(configPath, 'utf8')
  const mysqlAfterFirstRun = readMysqlInvocation(checkout)
  const database = `doughnut_${JSON.parse(provisionedConfig).id}_test`
  rmSync(lockPaths(checkout).dir, { recursive: true, force: true })

  const second = runWrapper(checkout, {
    command: 'backend/gradlew',
    args: ['-p', 'backend', 'test'],
  })
  assert.equal(second.status, 0, outputOf(second))
  assert.doesNotMatch(outputOf(second), /Allocated new worktree environment/)
  assert.deepEqual(readMysqlInvocation(checkout), mysqlAfterFirstRun)
  assert.equal(readFileSync(configPath, 'utf8'), provisionedConfig)
  assert.equal(readGradleInvocations(checkout).at(-1).url, jdbcUrl(database))
})
