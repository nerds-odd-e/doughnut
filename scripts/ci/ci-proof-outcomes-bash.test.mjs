import assert from 'node:assert/strict'
import { test } from 'node:test'
import { outcomeLines } from './ci-proof-outcomes.mjs'
import {
  repoRoot,
  summarizeFragments,
  writeFragment,
} from './ci-proof-outcomes-fixtures.mjs'

const failing = 'scripts/test/a_failing.sh.test'
const passing = 'scripts/test/b_passing.sh.test'
const bashReport = `failed\t1.250000\t${failing}\npassed\t0.500000\t${passing}\n`
const onlyPart = { PART: '1', PARTS: '1' }

const vitestReport = JSON.stringify({
  testResults: [
    {
      name: `${repoRoot}frontend/tests/a.spec.ts`,
      status: 'passed',
      assertionResults: [{ status: 'passed', duration: 10 }],
    },
  ],
})

test('a Bash report gives one check per test file', () => {
  assert.deepEqual(outcomeLines('bash', bashReport), [
    `failed\t${failing}`,
    `passed\t${passing}`,
  ])
})

test('a malformed Bash report line fails the conversion', () => {
  assert.throws(
    () => outcomeLines('bash', `passed\t${passing}\n`),
    /malformed Bash report line/
  )
})

test('a failed Bash check and its time count toward its file and the scripts entry', (t) => {
  const bash = writeFragment(t, 'bash', 'scripts', bashReport, onlyPart)
  const { summary } = summarizeFragments(
    t,
    [bash.fragmentText],
    'scripts=1:file'
  )

  assert.equal(summary.entries[0].path, 'scripts')
  assert.deepEqual(summary.entries[0].evidence, [
    {
      unit: 'file',
      passed: 1,
      failed: 1,
      other: 0,
      durationMs: 1750,
      untimed: 0,
      parts: [
        {
          part: 1,
          status: 'present',
          passed: 1,
          failed: 1,
          other: 0,
          durationMs: 1750,
          untimed: 0,
          breakdown: true,
        },
      ],
      files: [
        { path: failing, passed: 0, failed: 1, other: 0, durationMs: 1250 },
        { path: passing, passed: 1, failed: 0, other: 0, durationMs: 500 },
      ],
    },
  ])
})

test('a missing Bash report fails its conversion and leaves the scripts part missing beside other proof', (t) => {
  assert.throws(() => writeFragment(t, 'bash', 'scripts', undefined, onlyPart))
  const frontend = writeFragment(
    t,
    'vitest',
    'frontend/tests',
    vitestReport,
    onlyPart
  )
  const { summary, log } = summarizeFragments(
    t,
    [frontend.fragmentText],
    'frontend/tests=1:case scripts=1:file'
  )

  const [
    {
      evidence: [frontendEntry],
    },
    {
      evidence: [scripts],
    },
  ] = summary.entries
  assert.equal(frontendEntry.passed, 1)
  assert.deepEqual(
    frontendEntry.parts.map(({ status }) => status),
    ['present']
  )
  assert.deepEqual(scripts.parts, [{ part: 1, status: 'missing' }])
  assert.match(log, /scripts: .*part 1 of 1 missing/)
})
