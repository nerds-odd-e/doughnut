import assert from 'node:assert/strict'
import { test } from 'node:test'
import { outcomeLines } from './ci-proof-outcomes.mjs'
import { summaryProblems } from './ci-proof-contract.mjs'
import {
  repoRoot,
  summarizeFragments,
  writeFragment,
} from './ci-proof-outcomes-fixtures.mjs'

const vitestFile = (path, status, ...testStatuses) => ({
  name: `${repoRoot}${path}`,
  status,
  assertionResults: testStatuses.map((assertion) =>
    typeof assertion === 'string' ? { status: assertion } : assertion
  ),
})

test('a Vitest report gives one repo-relative line per test', () => {
  const report = {
    testResults: [
      vitestFile('frontend/tests/a.spec.ts', 'passed', 'passed', 'passed'),
      vitestFile('frontend/tests/b.spec.ts', 'failed', 'passed', 'failed'),
    ],
  }

  assert.deepEqual(outcomeLines('vitest', JSON.stringify(report)), [
    'passed\tfrontend/tests/a.spec.ts',
    'passed\tfrontend/tests/a.spec.ts',
    'passed\tfrontend/tests/b.spec.ts',
    'failed\tfrontend/tests/b.spec.ts',
  ])
})

test('skipped, todo and pending Vitest tests count as other', () => {
  const report = {
    testResults: [
      vitestFile(
        'frontend/tests/c.spec.ts',
        'passed',
        'skipped',
        'todo',
        'pending'
      ),
    ],
  }

  assert.deepEqual(outcomeLines('vitest', JSON.stringify(report)), [
    'other\tfrontend/tests/c.spec.ts',
    'other\tfrontend/tests/c.spec.ts',
    'other\tfrontend/tests/c.spec.ts',
  ])
})

test('a Vitest file that failed to load is a file outcome', () => {
  const report = {
    testResults: [vitestFile('frontend/tests/broken.spec.ts', 'failed')],
  }

  assert.deepEqual(outcomeLines('vitest', JSON.stringify(report), 'file'), [
    'failed\tfrontend/tests/broken.spec.ts',
  ])
})

test('the Vitest producer keeps recorded durations and untimed checks', (t) => {
  const path = 'frontend/tests/a.spec.ts'
  const report = {
    testResults: [
      vitestFile(
        path,
        'failed',
        { status: 'failed', duration: 125 },
        { status: 'passed', duration: 0 },
        'skipped'
      ),
    ],
  }
  const { times, fragment } = writeFragment(
    t,
    'vitest',
    'frontend/tests',
    JSON.stringify(report)
  )

  assert.equal(times, `0.125\t${path}\n0\t${path}\n`)
  assert.equal(fragment.unit, 'case')
  assert.equal(fragment.durationMs, 125)
  assert.deepEqual(fragment.files, [
    { path, passed: 1, failed: 1, other: 1, durationMs: 125, untimed: 1 },
  ])
})

test('Vitest publishes cases and failed files with independent timing and empty or missing parts', (t) => {
  const path = 'frontend/tests/a.spec.ts'
  const broken = 'frontend/tests/broken.spec.ts'
  const report = JSON.stringify({
    testResults: [
      vitestFile(
        path,
        'failed',
        { status: 'passed', duration: 10 },
        { status: 'failed' }
      ),
      { ...vitestFile(broken, 'failed'), startTime: 1000, endTime: 1040 },
    ],
  })
  const fragments = ['case', 'file'].map(
    (UNIT) =>
      writeFragment(t, 'vitest', 'frontend/tests', report, { UNIT })
        .fragmentText
  )
  const { summary } = summarizeFragments(
    t,
    fragments,
    'frontend/tests=2:case frontend/tests=2:file'
  )
  assert.deepEqual(summaryProblems(summary), [])
  const [cases, files] = summary.entries[0].evidence
  assert.deepEqual(
    [cases.unit, cases.passed, cases.failed, cases.durationMs, cases.untimed],
    ['case', 1, 1, 10, 1]
  )
  assert.deepEqual(cases.files, [
    { path, passed: 1, failed: 1, other: 0, durationMs: 10, untimed: 1 },
  ])
  assert.deepEqual(files.files, [
    { path: broken, passed: 0, failed: 1, other: 0, durationMs: 40 },
  ])
  for (const evidence of [cases, files])
    assert.deepEqual(
      evidence.parts.map(({ status }) => status),
      ['present', 'missing']
    )

  const onlyCases = JSON.stringify({
    testResults: [vitestFile(path, 'passed', 'passed')],
  })
  const empty = writeFragment(t, 'vitest', 'frontend/tests', onlyCases, {
    UNIT: 'file',
  }).fragment
  assert.deepEqual(
    [empty.unit, empty.passed, empty.failed, empty.other],
    ['file', 0, 0, 0]
  )
  for (const UNIT of ['case', 'file'])
    assert.throws(() =>
      writeFragment(t, 'vitest', 'frontend/tests', undefined, { UNIT })
    )
})

test('a failed Vitest file retains zero reported time or an untimed outcome', (t) => {
  const report = JSON.stringify({
    testResults: [
      {
        ...vitestFile('frontend/tests/zero.spec.ts', 'failed'),
        startTime: 1000,
        endTime: 1000,
      },
      vitestFile('frontend/tests/untimed.spec.ts', 'failed'),
    ],
  })
  const { fragment } = writeFragment(t, 'vitest', 'frontend/tests', report, {
    UNIT: 'file',
  })
  assert.deepEqual(
    [fragment.failed, fragment.durationMs, fragment.untimed],
    [2, 0, 1]
  )
})
