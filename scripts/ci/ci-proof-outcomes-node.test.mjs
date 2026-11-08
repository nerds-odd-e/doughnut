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
import { repoRoot } from './ci-proof-outcomes-fixtures.mjs'

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
  const report = join(dir, 'report.ndjson')
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
      env: { ...parentEnv, ...env(report) },
      encoding: 'utf8',
    }
  )
  const path = (name) => relative(repoRoot, join(dir, `${name}.test.mjs`))
  return { run, report, path }
}

test('a Node report gives one check per test case from the directly selected files', (t) => {
  const { run, report, path } = runFixtures(t, (report) => ({
    CI_PROOF_NODE_REPORT: report,
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
