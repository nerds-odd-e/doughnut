import assert from 'node:assert/strict'
import { test } from 'node:test'
import { outcomeLines } from './ci-proof-outcomes.mjs'
import { writeFragment } from './ci-proof-outcomes-fixtures.mjs'

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
  assert.equal(fragment.unit, 'case')
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
