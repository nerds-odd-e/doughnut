import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import { workflow } from './workflow-fixtures.mjs'

test('CI skips documentation-only pushes on every branch', () => {
  const ci = workflow('ci')

  assert.deepEqual(ci.on, {
    push: {
      branches: ['**'],
      'paths-ignore': ['.planning/**', 'docs/**'],
    },
  })
  assert.equal(ci.name, 'donut CI')
})

const stepUsing = (job, action) =>
  job.steps.find((step) => step.uses === `./.github/${action}`)

const producersOf = (ci, entry) =>
  Object.entries(ci.jobs).flatMap(([name, job]) =>
    job.steps
      .filter(
        (step) =>
          step.uses === './.github/ci_proof_fragments' &&
          step.with.entry === entry
      )
      .map((step) => ({ name, job, step }))
  )

const expectationsOf = (ci) =>
  [
    ...stepUsing(ci.jobs['ci-proof'], 'ci_proof_summary').with.expect.matchAll(
      /(\S+)=(\d+)/g
    ),
  ].map(([, entry, parts]) => ({ entry, parts: Number(parts) }))

test('CI proof expects each entry in intent.locations order among proved entries', () => {
  assert.deepEqual(
    expectationsOf(workflow('ci')).map(({ entry }) => entry),
    ['e2e_test/features', 'frontend/tests', 'backend/src/test/java', 'scripts']
  )
})

test('CI proof expects each entry in as many parts as its producers report', () => {
  const ci = workflow('ci')

  for (const { entry, parts } of expectationsOf(ci)) {
    const producers = producersOf(ci, entry)
    const explicit = producers.filter(({ step }) => step.with.part)
    if (explicit.length > 0) {
      assert.deepEqual(
        explicit
          .map(({ step }) => [Number(step.with.part), step.with.of])
          .sort(([a], [b]) => a - b),
        [...Array(parts).keys()].map((index) => [index + 1, parts]),
        entry
      )
    } else {
      const [{ job }] = producers
      assert.equal(Object.values(job.strategy.matrix)[0].length, parts, entry)
      assert.equal(job.strategy['fail-fast'], false, entry)
    }
    for (const { name } of producers) {
      assert.ok(ci.jobs['ci-proof'].needs.includes(name), `${entry}: ${name}`)
    }
  }
})

test('the Bash script runner reports its file checks as the scripts part', () => {
  const ci = workflow('ci')
  const { job, step } = producersOf(ci, 'scripts').find(
    ({ step }) => step.with.format === 'bash'
  )
  const runner = job.steps.find(
    ({ run }) => run === 'bash scripts/test/run_all_script_tests.sh'
  )
  const report = '${{ runner.temp }}/bash-report.tsv'

  assert.deepEqual(runner.env, { CI_PROOF_BASH_REPORT: report })
  assert.equal(
    step.if,
    `\${{ !cancelled() && steps.${runner.id}.outcome != 'skipped' }}`
  )
  assert.deepEqual(step.with, {
    format: 'bash',
    report,
    entry: 'scripts',
    part: 1,
    of: 11,
  })
})

const packageScripts = JSON.parse(
  readFileSync(new URL('../../package.json', import.meta.url), 'utf8')
).scripts

// lint:all's commands that run the Node test wrapper, in lint:all order.
const nodeTestCommands = packageScripts['lint:all']
  .split(' && ')
  .map((command) => command.replace(/^pnpm /, ''))
  .filter((name) =>
    packageScripts[name]?.startsWith('node scripts/ci/node-test.mjs ')
  )

test('each Node test command in lint:all reports its own scripts part after the Bash part', () => {
  const ci = workflow('ci')
  const nodeProducers = producersOf(ci, 'scripts').filter(
    ({ step }) => step.with.format === 'node'
  )
  const [{ name: lintJob, job }] = nodeProducers
  const lintAll = job.steps.find(({ run }) => run === 'pnpm lint:all')
  const reports = '${{ runner.temp }}/node-reports'

  assert.deepEqual(lintAll.env, { CI_PROOF_NODE_REPORT_DIR: reports })
  assert.deepEqual(
    nodeProducers.map(({ name, step }) => [name, step.if, step.with]),
    nodeTestCommands.map((command, index) => [
      lintJob,
      `\${{ !cancelled() && steps.${lintAll.id}.outcome != 'skipped' }}`,
      {
        format: 'node',
        report: `${reports}/${command}.ndjson`,
        entry: 'scripts',
        part: index + 2,
        of: nodeTestCommands.length + 1,
      },
    ])
  )
})
