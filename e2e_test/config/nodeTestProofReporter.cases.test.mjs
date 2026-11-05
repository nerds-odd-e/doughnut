// Recorder behavior for finished cases: suite paths, subtests, clears, and
// skip/todo retention.
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import {
  git,
  passedObservation,
  readLatest,
  runNode,
  scratchCheckout,
  seedObservations,
  writeTest,
} from './nodeTestProofReporterFixture.mjs'

test('a describe path records the suite name list', (t) => {
  const fixture = scratchCheckout(t)
  writeTest(
    fixture,
    'scripts/describe.test.mjs',
    `import { describe, it } from 'node:test'
describe('guidance', () => {
  it('rejects a missing path', () => {})
})
`
  )
  git(fixture, ['add', '.'])
  git(fixture, ['commit', '-m', 'add describe'])

  const result = runNode(fixture, ['scripts/describe.test.mjs'], {
    record: true,
  })
  assert.equal(result.status, 0, result.stderr || result.stdout)
  const latest = readLatest(fixture)
  assert.ok(latest)
  assert.equal(latest.version, 1)
  assert.equal(latest.observations.length, 1)
  assert.deepEqual(latest.observations[0].test, [
    'guidance',
    'rejects a missing path',
  ])
  assert.equal(latest.observations[0].file, 'scripts/describe.test.mjs')
  assert.equal(latest.observations[0].attempts, 1)
  assert.equal(latest.observations[0].outcome, 'passed')
  assert.equal(
    latest.observations[0].commit,
    git(fixture, ['rev-parse', 'HEAD'])
  )
  assert.equal(
    latest.observations[0].fileBlob,
    git(fixture, ['hash-object', 'scripts/describe.test.mjs'])
  )
  assert.equal(typeof latest.observations[0].durationMs, 'number')
})

test('parent with passing subtests records one observation; no subtest rows', (t) => {
  const fixture = scratchCheckout(t)
  writeTest(
    fixture,
    'scripts/subtests-pass.test.mjs',
    `import { test } from 'node:test'
test('parent', async (t) => {
  await t.test('child a', () => {})
  await t.test('child b', () => {})
})
`
  )
  git(fixture, ['add', '.'])
  git(fixture, ['commit', '-m', 'subtests pass'])

  const result = runNode(fixture, ['scripts/subtests-pass.test.mjs'], {
    record: true,
  })
  assert.equal(result.status, 0, result.stderr || result.stdout)
  const latest = readLatest(fixture)
  assert.equal(latest.observations.length, 1)
  assert.deepEqual(latest.observations[0].test, ['parent'])
  assert.ok(latest.observations[0].durationMs >= 0)
})

test('parent with one failing subtest clears the parent observation', (t) => {
  const fixture = scratchCheckout(t)
  writeTest(
    fixture,
    'scripts/subtests-fail.test.mjs',
    `import assert from 'node:assert/strict'
import { test } from 'node:test'
test('parent', async (t) => {
  await t.test('child ok', () => {})
  await t.test('child fail', () => {
    assert.equal(1, 2)
  })
})
`
  )
  git(fixture, ['add', '.'])
  git(fixture, ['commit', '-m', 'subtests fail'])
  const commit = git(fixture, ['rev-parse', 'HEAD'])
  const blob = git(fixture, ['hash-object', 'scripts/subtests-fail.test.mjs'])
  seedObservations(fixture, [
    passedObservation(
      'scripts/subtests-fail.test.mjs',
      ['parent'],
      commit,
      blob
    ),
  ])

  const result = runNode(fixture, ['scripts/subtests-fail.test.mjs'], {
    record: true,
  })
  assert.notEqual(result.status, 0)
  assert.equal(readLatest(fixture), undefined)
})

test('timeout and assertion failure clear only themselves', (t) => {
  const fixture = scratchCheckout(t)
  writeTest(
    fixture,
    'scripts/clear-self.test.mjs',
    `import assert from 'node:assert/strict'
import { test } from 'node:test'
test('stays', () => {})
test('times out', { timeout: 50 }, async () => {
  await new Promise((resolve) => setTimeout(resolve, 200))
})
test('asserts', () => {
  assert.equal(1, 2)
})
`
  )
  git(fixture, ['add', '.'])
  git(fixture, ['commit', '-m', 'clear self'])
  const commit = git(fixture, ['rev-parse', 'HEAD'])
  const blob = git(fixture, ['hash-object', 'scripts/clear-self.test.mjs'])
  seedObservations(
    fixture,
    ['stays', 'times out', 'asserts'].map((name) =>
      passedObservation('scripts/clear-self.test.mjs', [name], commit, blob)
    )
  )

  const result = runNode(fixture, ['scripts/clear-self.test.mjs'], {
    record: true,
  })
  assert.notEqual(result.status, 0)
  const latest = readLatest(fixture)
  assert.deepEqual(
    latest.observations.map((row) => row.test),
    [['stays']]
  )
})

test('skip and todo keep earlier proof', (t) => {
  const fixture = scratchCheckout(t)
  writeTest(
    fixture,
    'scripts/skip-todo.test.mjs',
    `import { test } from 'node:test'
test('kept skip', { skip: true }, () => {})
test('kept todo', { todo: true }, () => {})
test('kept failing todo', { todo: true }, () => {
  throw new Error('todo failure')
})
`
  )
  git(fixture, ['add', '.'])
  git(fixture, ['commit', '-m', 'skip todo'])
  const commit = git(fixture, ['rev-parse', 'HEAD'])
  const blob = git(fixture, ['hash-object', 'scripts/skip-todo.test.mjs'])
  const proofFile = path.join(fixture, 'scripts/proof/latest.json')
  seedObservations(
    fixture,
    ['kept skip', 'kept todo', 'kept failing todo'].map((name) =>
      passedObservation('scripts/skip-todo.test.mjs', [name], commit, blob)
    )
  )
  const before = readFileSync(proofFile, 'utf8')

  const result = runNode(fixture, ['scripts/skip-todo.test.mjs'], {
    record: true,
  })
  assert.equal(result.status, 0, result.stderr || result.stdout)
  assert.equal(readFileSync(proofFile, 'utf8'), before)
})
