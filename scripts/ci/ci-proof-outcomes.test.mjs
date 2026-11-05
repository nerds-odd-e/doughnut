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

  assert.deepEqual(outcomeLines('vitest', report), [
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

  assert.deepEqual(outcomeLines('vitest', report), [
    'other\tfrontend/tests/c.spec.ts',
    'other\tfrontend/tests/c.spec.ts',
    'other\tfrontend/tests/c.spec.ts',
  ])
})

test('a Vitest file that failed to load counts as one failed check', () => {
  const report = {
    testResults: [vitestFile('frontend/tests/broken.spec.ts', 'failed')],
  }

  assert.deepEqual(outcomeLines('vitest', report), [
    'failed\tfrontend/tests/broken.spec.ts',
  ])
})
