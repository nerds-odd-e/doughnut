import assert from 'node:assert/strict'
import { test } from 'node:test'
import { outcomeLines } from './ci-proof-outcomes.mjs'
import { repoRoot, writeFragment } from './ci-proof-outcomes-fixtures.mjs'

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

test('a Vitest file that failed to load counts as one failed check', () => {
  const report = {
    testResults: [vitestFile('frontend/tests/broken.spec.ts', 'failed')],
  }

  assert.deepEqual(outcomeLines('vitest', JSON.stringify(report)), [
    'failed\tfrontend/tests/broken.spec.ts',
  ])
})

test('the Vitest producer keeps recorded durations and untimed checks', (t) => {
  const path = 'frontend/tests/a.spec.ts'
  const broken = 'frontend/tests/broken.spec.ts'
  const report = {
    testResults: [
      vitestFile(
        path,
        'failed',
        { status: 'failed', duration: 125 },
        { status: 'passed', duration: 0 },
        'skipped'
      ),
      vitestFile(broken, 'failed'),
    ],
  }
  const { times, fragment } = writeFragment(
    t,
    'vitest',
    'frontend/tests',
    JSON.stringify(report)
  )

  assert.equal(times, `0.125\t${path}\n0\t${path}\n`)
  assert.equal(fragment.durationMs, 125)
  assert.deepEqual(fragment.files, [
    { path, passed: 1, failed: 1, other: 1, durationMs: 125, untimed: 1 },
    { path: broken, passed: 0, failed: 1, other: 0, untimed: 1 },
  ])
})

// One scenario's messages: each attempt is a list of step results.
const cucumberScenario = (uri, ...attempts) => [
  { pickle: { id: `${uri}-pickle`, uri } },
  { testCase: { id: `${uri}-case`, pickleId: `${uri}-pickle` } },
  ...attempts.flatMap((steps, attempt) => [
    {
      testCaseStarted: {
        id: `${uri}-${attempt}`,
        testCaseId: `${uri}-case`,
        attempt,
      },
    },
    ...steps.map((step) => ({
      testStepFinished: {
        testCaseStartedId: `${uri}-${attempt}`,
        testStepResult: typeof step === 'string' ? { status: step } : step,
      },
    })),
  ]),
]

const cucumberMessages = (...scenarios) =>
  scenarios
    .flat()
    .map((envelope) => `${JSON.stringify(envelope)}\n`)
    .join('')

test('a retried Cucumber scenario counts once, with its final attempt', () => {
  const messages = cucumberMessages(
    cucumberScenario(
      'e2e_test/features/a.feature',
      ['PASSED', 'FAILED'],
      ['PASSED', 'PASSED']
    ),
    cucumberScenario(
      'e2e_test/features/b.feature',
      ['PASSED', 'FAILED'],
      ['FAILED', 'SKIPPED']
    )
  )

  assert.deepEqual(outcomeLines('cucumber', messages), [
    'passed\te2e_test/features/a.feature',
    'failed\te2e_test/features/b.feature',
  ])
})

test('the Cucumber producer sums recorded final-attempt steps for a failed scenario', (t) => {
  const path = 'e2e_test/features/a.feature'
  const messages = cucumberMessages(
    cucumberScenario(
      path,
      [{ status: 'FAILED', duration: { seconds: 8, nanos: 0 } }],
      [
        { status: 'PASSED', duration: { seconds: 1, nanos: 250_000_000 } },
        { status: 'FAILED', duration: { seconds: 0, nanos: 125_000_000 } },
        'SKIPPED',
      ]
    ),
    cucumberScenario('e2e_test/features/b.feature', ['SKIPPED'])
  )
  const { times, fragment } = writeFragment(
    t,
    'cucumber',
    'e2e_test/features',
    messages
  )

  assert.equal(times, `1.375\t${path}\n`)
  assert.equal(fragment.durationMs, 1375)
  assert.deepEqual(fragment.files, [
    { path, passed: 0, failed: 1, other: 0, durationMs: 1375 },
    {
      path: 'e2e_test/features/b.feature',
      passed: 0,
      failed: 0,
      other: 1,
      untimed: 1,
    },
  ])
})

test('an all-skipped Cucumber scenario counts as other', () => {
  const messages = cucumberMessages(
    cucumberScenario('e2e_test/features/c.feature', ['SKIPPED', 'SKIPPED'])
  )

  assert.deepEqual(outcomeLines('cucumber', messages), [
    'other\te2e_test/features/c.feature',
  ])
})

test('undefined, ambiguous and pending Cucumber steps fail the scenario', () => {
  const messages = cucumberMessages(
    cucumberScenario('e2e_test/features/d.feature', ['PASSED', 'UNDEFINED']),
    cucumberScenario('e2e_test/features/e.feature', ['AMBIGUOUS']),
    cucumberScenario('e2e_test/features/f.feature', ['PENDING', 'SKIPPED'])
  )

  assert.deepEqual(outcomeLines('cucumber', messages), [
    'failed\te2e_test/features/d.feature',
    'failed\te2e_test/features/e.feature',
    'failed\te2e_test/features/f.feature',
  ])
})
