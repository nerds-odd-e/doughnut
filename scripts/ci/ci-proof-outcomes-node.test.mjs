import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import {
  existsSync,
  mkdtempSync,
  readFileSync,
  realpathSync,
  rmSync,
  writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { join, relative } from 'node:path'
import { test } from 'node:test'
import { outcomeLines } from './ci-proof-outcomes.mjs'
import {
  repoRoot,
  summarizeFragments,
  writeFragment,
} from './ci-proof-outcomes-fixtures.mjs'

const nodeTest = join(repoRoot, 'scripts/ci/node-test.mjs')

const fixtures = {
  'outcomes.test.mjs': `import assert from 'node:assert/strict'
import { describe, it, test } from 'node:test'
test('passes', () => {})
test('fails', () => assert.fail('boom'))
test('skipped', { skip: true }, () => {})
test('todo', { todo: true }, () => {})
test('parent fails after its subtest', async (t) => {
  await t.test('subtest passes', () => {})
  assert.fail('parent')
})
test('parent passes', async (t) => {
  await t.test('subtest passes', () => {})
})
describe('suite', () => {
  it('fails inside', () => assert.fail('inside'))
  it('passes inside', () => {})
})
`,
  'broken.test.mjs': `import './missing-module.mjs'\n`,
  'shared.cases.mjs': `import { test } from 'node:test'
export const sharedCases = () => test('shared case passes', () => {})
`,
  'imports.test.mjs': `import { sharedCases } from './shared.cases.mjs'\nsharedCases()\n`,
  'inner.test.mjs': `import { test } from 'node:test'\ntest('inner passes', () => {})\n`,
  'launcher.test.mjs': `import { execFileSync } from 'node:child_process'
import { test } from 'node:test'
test('launches its own test run', () => {
  const { NODE_TEST_CONTEXT, ...env } = process.env
  execFileSync(process.execPath, [${JSON.stringify(nodeTest)}, new URL('inner.test.mjs', import.meta.url).pathname], { env, stdio: 'pipe' })
})
`,
}

const runFixtures = (t, env) => {
  const dir = realpathSync(mkdtempSync(join(tmpdir(), 'node-report-')))
  t.after(() => rmSync(dir, { recursive: true, force: true }))
  for (const [name, content] of Object.entries(fixtures)) {
    writeFileSync(join(dir, name), content)
  }
  const reportDir = join(dir, 'reports')
  const report = join(reportDir, 'fixture-tests.ndjson')
  const { NODE_TEST_CONTEXT, ...parentEnv } = process.env
  const run = spawnSync(
    process.execPath,
    [
      nodeTest,
      ...['outcomes', 'broken', 'imports', 'launcher'].map((name) =>
        join(dir, `${name}.test.mjs`)
      ),
    ],
    {
      cwd: repoRoot,
      env: {
        ...parentEnv,
        npm_lifecycle_event: 'fixture-tests',
        ...env(reportDir),
      },
      encoding: 'utf8',
    }
  )
  const path = (name) => relative(repoRoot, join(dir, `${name}.test.mjs`))
  return { run, report, path }
}

test('a Node report gives one check per test case from the directly selected files, named after the running package script', (t) => {
  const { run, report, path } = runFixtures(t, (reportDir) => ({
    CI_PROOF_NODE_REPORT_DIR: reportDir,
  }))

  assert.equal(run.status, 1)
  assert.match(run.stdout, /✖ fails/)
  // Files report in completion order; cases keep their order within a file.
  const checks = Object.groupBy(
    readFileSync(report, 'utf8')
      .trim()
      .split('\n')
      .map((line) => JSON.parse(line)),
    (check) => check.path
  )
  const outcomes = (name) =>
    checks[path(name)].map(({ status, seconds }) => [
      status,
      typeof seconds === 'number',
    ])
  assert.deepEqual(Object.keys(checks).sort(), [
    path('broken'),
    path('imports'),
    path('launcher'),
    path('outcomes'),
  ])
  assert.deepEqual(outcomes('outcomes'), [
    ['passed', true],
    ['failed', true],
    ['other', true],
    ['other', true],
    ['passed', true],
    ['failed', false],
    ['passed', true],
    ['failed', true],
    ['passed', true],
  ])
  assert.deepEqual(outcomes('broken'), [['failed', true]])
  assert.deepEqual(outcomes('imports'), [['passed', true]])
  assert.deepEqual(outcomes('launcher'), [['passed', true]])
  assert.equal(outcomeLines('node', readFileSync(report, 'utf8')).length, 12)
})

test('without a report request the run keeps its output and exit status', (t) => {
  const { run, report } = runFixtures(t, () => ({}))

  assert.equal(run.status, 1)
  assert.match(run.stdout, /✖ fails/)
  assert.equal(existsSync(report), false)
})

test('a malformed Node report line fails the conversion', () => {
  assert.throws(
    () => outcomeLines('node', '{"status":"ok","path":"scripts/a.test.mjs"}\n'),
    /malformed Node report line/
  )
})

const nodeLines = (checks) =>
  checks.map((check) => `${JSON.stringify(check)}\n`).join('')

test('Bash and Node parts of one job publish once each into the scripts entry, a skipped later part missing', (t) => {
  const runnerTemp = realpathSync(mkdtempSync(join(tmpdir(), 'runner-')))
  t.after(() => rmSync(runnerTemp, { recursive: true, force: true }))
  const part = (n) => ({ PART: String(n), PARTS: '4' })
  const routing = 'scripts/test/path-routing/routes.test.mjs'
  const mixed = 'scripts/mixed.test.mjs'

  const bash = writeFragment(
    t,
    'bash',
    'scripts',
    'passed\t0.500000\tscripts/test/a.sh.test\n',
    part(1)
  )
  const passingNode = writeFragment(
    t,
    'node',
    'scripts',
    nodeLines([
      { status: 'passed', path: routing, seconds: 0.01 },
      { status: 'passed', path: routing, seconds: 0.02 },
    ]),
    part(2),
    runnerTemp
  )
  const failedNode = writeFragment(
    t,
    'node',
    'scripts',
    nodeLines([
      { status: 'passed', path: mixed, seconds: 0.1 },
      { status: 'failed', path: mixed, seconds: 0.2 },
      { status: 'failed', path: mixed },
      { status: 'passed', path: 'e2e_test/config/a.test.mjs', seconds: 9 },
    ]),
    part(3),
    runnerTemp
  )
  // Each part's staging held only its own fragment (writeFragment checks it);
  // a part whose command never ran leaves nothing staged to upload.
  assert.throws(() =>
    writeFragment(t, 'node', 'scripts', undefined, part(4), runnerTemp)
  )
  assert.equal(existsSync(join(runnerTemp, 'ci-proof')), false)

  const { summary, log } = summarizeFragments(
    t,
    [bash, passingNode, failedNode].map(({ fragmentText }) => fragmentText),
    'scripts=4'
  )
  const [
    {
      evidence: [scripts],
    },
  ] = summary.entries
  assert.deepEqual(
    [scripts.passed, scripts.failed, scripts.durationMs, scripts.untimed],
    [4, 2, 830, 1]
  )
  assert.deepEqual(
    scripts.parts.map(({ part, status, failed, durationMs }) => [
      part,
      status,
      failed,
      durationMs,
    ]),
    [
      [1, 'present', 0, 500],
      [2, 'present', 0, 30],
      [3, 'present', 2, 300],
      [4, 'missing', undefined, undefined],
    ]
  )
  assert.deepEqual(scripts.files, [
    {
      path: mixed,
      passed: 1,
      failed: 2,
      other: 0,
      durationMs: 300,
      untimed: 1,
    },
    {
      path: 'scripts/test/a.sh.test',
      passed: 1,
      failed: 0,
      other: 0,
      durationMs: 500,
    },
    { path: routing, passed: 2, failed: 0, other: 0, durationMs: 30 },
  ])
  assert.match(
    log,
    /scripts: 4 passed, 2 failed, 0 other legacy test results \(checks\); part 4 of 4 missing/
  )
})
