import assert from 'node:assert/strict'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import { outcomeLines } from './ci-proof-outcomes.mjs'

const repoRoot = fileURLToPath(new URL('../..', import.meta.url))

const vitestFile = (path, status, ...testStatuses) => ({
  name: `${repoRoot}${path}`,
  status,
  assertionResults: testStatuses.map((testStatus) => ({ status: testStatus })),
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

// One scenario's messages: each attempt is a list of step statuses.
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
    ...steps.map((status) => ({
      testStepFinished: {
        testCaseStartedId: `${uri}-${attempt}`,
        testStepResult: { status },
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
