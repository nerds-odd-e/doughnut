// Recorder behavior across selection gaps, interrupted runs, and files outside
// covered locations.
import assert from 'node:assert/strict'
import { readdirSync } from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import {
  git,
  passedObservation,
  readLatest,
  runNode,
  scratchCheckout,
  seedObservations,
  spawnNode,
  writeTest,
} from './nodeTestProofReporterFixture.mjs'

test('name-pattern exclusion keeps proof for the unexecuted case', (t) => {
  const fixture = scratchCheckout(t)
  writeTest(
    fixture,
    'scripts/pattern.test.mjs',
    `import { test } from 'node:test'
test('selected', () => {})
test('excluded', () => {})
`
  )
  git(fixture, ['add', '.'])
  git(fixture, ['commit', '-m', 'pattern'])
  const commit = git(fixture, ['rev-parse', 'HEAD'])
  const blob = git(fixture, ['hash-object', 'scripts/pattern.test.mjs'])
  seedObservations(fixture, [
    passedObservation('scripts/pattern.test.mjs', ['excluded'], commit, blob),
  ])

  const result = runNode(fixture, ['scripts/pattern.test.mjs'], {
    record: true,
    extraArgs: ['--test-name-pattern', '^selected$'],
  })
  assert.equal(result.status, 0, result.stderr || result.stdout)
  const latest = readLatest(fixture)
  assert.deepEqual(
    latest.observations.map((row) => row.test),
    [['excluded'], ['selected']]
  )
})

test('killed run keeps completed proof updates', async (t) => {
  const fixture = scratchCheckout(t)
  writeTest(
    fixture,
    'scripts/interrupted.test.mjs',
    `import { test } from 'node:test'
test('first finishes', () => {})
test('second sleeps', async () => {
  await new Promise(() => {})
})
`
  )
  git(fixture, ['add', '.'])
  git(fixture, ['commit', '-m', 'interrupted'])

  const child = spawnNode(fixture, 'scripts/interrupted.test.mjs', {
    record: true,
  })

  const deadline = Date.now() + 5_000
  let latest
  while (Date.now() < deadline) {
    latest = readLatest(fixture)
    if (latest?.observations?.some((row) => row.test[0] === 'first finishes')) {
      break
    }
    await new Promise((resolve) => setTimeout(resolve, 20))
  }
  assert.ok(
    latest?.observations?.some((row) => row.test[0] === 'first finishes'),
    'first case should record before kill'
  )
  child.kill('SIGKILL')
  await new Promise((resolve) => child.once('exit', resolve))

  const after = readLatest(fixture)
  assert.ok(
    after?.observations?.some((row) => row.test[0] === 'first finishes')
  )
  assert.equal(
    after.observations.some((row) => row.test[0] === 'second sleeps'),
    false
  )
  assert.deepEqual(
    readdirSync(path.join(fixture, 'scripts/proof')).filter((name) =>
      name.endsWith('.json')
    ),
    ['latest.json']
  )
})

test('a file outside scripts and infra is not recorded', (t) => {
  const fixture = scratchCheckout(t)
  writeTest(
    fixture,
    'scripts/inside.test.mjs',
    `import { test } from 'node:test'
test('inside', () => {})
`
  )
  writeTest(
    fixture,
    'other/outside.test.mjs',
    `import { test } from 'node:test'
test('outside', () => {})
`
  )
  git(fixture, ['add', '.'])
  git(fixture, ['commit', '-m', 'outside'])

  const result = runNode(
    fixture,
    ['scripts/inside.test.mjs', 'other/outside.test.mjs'],
    { record: true }
  )
  assert.equal(result.status, 0, result.stderr || result.stdout)
  const latest = readLatest(fixture)
  assert.deepEqual(
    latest.observations.map((row) => row.file),
    ['scripts/inside.test.mjs']
  )
})
