import assert from 'node:assert/strict'
import { mkdtempSync, realpathSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { test } from 'node:test'
import { summaryProblems } from './ci-proof-contract.mjs'
import {
  summarizeFragments,
  writeFragment,
} from './ci-proof-outcomes-fixtures.mjs'

const nodeLines = (checks) =>
  checks.map((check) => `${JSON.stringify(check)}\n`).join('')

test('one attempt publishes script cases and files with independent outcomes, times and missing parts', (t) => {
  const runnerTemp = realpathSync(mkdtempSync(join(tmpdir(), 'runner-')))
  t.after(() => rmSync(runnerTemp, { recursive: true, force: true }))
  const routing = 'scripts/test/path-routing/routes.test.mjs'
  const mixed = 'scripts/mixed.test.mjs'
  const broken = 'scripts/broken.test.mjs'
  const bashPath = 'scripts/test/a.sh.test'
  const reports = [
    nodeLines([
      { unit: 'case', status: 'passed', path: routing, seconds: 0.01 },
      { unit: 'case', status: 'passed', path: routing, seconds: 0.02 },
    ]),
    nodeLines([
      { unit: 'case', status: 'passed', path: mixed, seconds: 0.1 },
      { unit: 'case', status: 'failed', path: mixed, seconds: 0.2 },
      { unit: 'case', status: 'failed', path: mixed },
      { unit: 'file', status: 'failed', path: broken, seconds: 0.4 },
      {
        unit: 'case',
        status: 'passed',
        path: 'e2e_test/config/a.test.mjs',
        seconds: 9,
      },
    ]),
    undefined,
  ]
  const fragments = []
  for (const [index, report] of reports.entries()) {
    for (const unit of ['case', 'file']) {
      const identity = {
        UNIT: unit,
        PART: String(index + 1),
        PARTS: unit === 'case' ? '3' : '4',
      }
      if (report === undefined) {
        assert.throws(() =>
          writeFragment(t, 'node', 'scripts', report, identity, runnerTemp)
        )
      } else {
        fragments.push(
          writeFragment(t, 'node', 'scripts', report, identity, runnerTemp)
            .fragmentText
        )
      }
    }
  }
  fragments.push(
    writeFragment(
      t,
      'bash',
      'scripts',
      `passed\t0.500000\t${bashPath}\n`,
      {
        UNIT: 'file',
        PART: '4',
        PARTS: '4',
      },
      runnerTemp
    ).fragmentText
  )
  const { summary } = summarizeFragments(
    t,
    fragments,
    'scripts=3:case scripts=4:file'
  )
  assert.deepEqual(summaryProblems(summary), [])
  const [
    {
      path,
      evidence: [cases, files],
    },
  ] = summary.entries
  assert.equal(path, 'scripts')
  assert.deepEqual(
    [cases.unit, cases.passed, cases.failed, cases.durationMs, cases.untimed],
    ['case', 3, 2, 330, 1]
  )
  assert.deepEqual(
    [files.unit, files.passed, files.failed, files.durationMs, files.untimed],
    ['file', 1, 1, 900, 0]
  )
  assert.deepEqual(cases.files, [
    {
      path: mixed,
      passed: 1,
      failed: 2,
      other: 0,
      durationMs: 300,
      untimed: 1,
    },
    { path: routing, passed: 2, failed: 0, other: 0, durationMs: 30 },
  ])
  assert.deepEqual(files.files, [
    { path: broken, passed: 0, failed: 1, other: 0, durationMs: 400 },
    { path: bashPath, passed: 1, failed: 0, other: 0, durationMs: 500 },
  ])
  assert.deepEqual(
    files.parts.map(({ part, status, passed, failed }) => [
      part,
      status,
      passed,
      failed,
    ]),
    [
      [1, 'present', 0, 0],
      [2, 'present', 0, 1],
      [3, 'missing', undefined, undefined],
      [4, 'present', 1, 0],
    ]
  )
  assert.deepEqual(
    cases.parts.map(({ status }) => status),
    ['present', 'present', 'missing']
  )
})
