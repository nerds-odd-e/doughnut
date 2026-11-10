import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import { action, workflow } from './workflow-fixtures.mjs'

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

const ciWithLintSteps = () => {
  const ci = workflow('ci')
  const lint = ci.jobs['Lint-N-Backend-Generated-Types-For-Frontend']
  const execution = stepUsing(lint, 'lint_and_generated_types')
  assert.ok(execution, 'the Lint job calls its execution action')
  lint.steps = lint.steps.flatMap((step) =>
    step === execution ? action('lint_and_generated_types').runs.steps : [step]
  )
  return ci
}

const producersOf = (ci, entry, unit) =>
  Object.entries(ci.jobs).flatMap(([name, job]) =>
    job.steps
      .filter(
        (step) =>
          step.uses === './.github/ci_proof_fragments' &&
          step.with.entry === entry &&
          (!unit || step.with.unit === unit)
      )
      .map((step) => ({ name, job, step }))
  )

const expectationsOf = (ci) =>
  [
    ...stepUsing(ci.jobs['ci-proof'], 'ci_proof_summary').with.expect.matchAll(
      /(\S+)=(\d+)(?::(case|file))?/g
    ),
  ].map(([, entry, parts, unit]) => ({ entry, parts: Number(parts), unit }))

test('CI proof expects each entry in intent.locations order among proved entries', () => {
  assert.deepEqual(expectationsOf(ciWithLintSteps()), [
    { entry: 'e2e_test/features', parts: 6, unit: 'case' },
    { entry: 'frontend/tests', parts: 2, unit: 'case' },
    { entry: 'frontend/tests', parts: 2, unit: 'file' },
    { entry: 'backend/src/test/java', parts: 1, unit: 'case' },
    { entry: 'scripts', parts: 13, unit: 'case' },
    { entry: 'scripts', parts: 14, unit: 'file' },
  ])
})

test('CI proof expects each entry in as many parts as its producers report', () => {
  const ci = ciWithLintSteps()

  for (const { entry, parts, unit } of expectationsOf(ci)) {
    const producers = producersOf(ci, entry, unit)
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
      assert.equal(producers.length, 1, `${entry}:${unit}`)
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
  const ci = ciWithLintSteps()
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
    unit: 'file',
    part: 14,
    of: 14,
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

test('each Node test command in lint:all reports case and file parts in command order', () => {
  const ci = ciWithLintSteps()
  const nodeProducers = producersOf(ci, 'scripts').filter(
    ({ step }) => step.with.format === 'node'
  )
  const [{ name: lintJob, job }] = nodeProducers
  const lintAll = job.steps.find(({ run }) => run === 'pnpm lint:all')
  const reports = '${{ runner.temp }}/node-reports'

  assert.deepEqual(lintAll.env, { CI_PROOF_NODE_REPORT_DIR: reports })
  assert.deepEqual(
    nodeProducers.map(({ name, step }) => [name, step.if, step.with]),
    nodeTestCommands.flatMap((command, index) =>
      ['case', 'file'].map((unit) => [
        lintJob,
        `\${{ !cancelled() && steps.${lintAll.id}.outcome != 'skipped' }}`,
        {
          format: 'node',
          report: `${reports}/${command}.ndjson`,
          entry: 'scripts',
          unit,
          part: index + 1,
          of: nodeTestCommands.length + (unit === 'file' ? 1 : 0),
        },
      ])
    )
  )
})

test('fragment uploads distinguish units for the same job and part', () => {
  const fragments = action('ci_proof_fragments')
  assert.equal(fragments.runs.steps[0].env.UNIT, '${{ inputs.unit }}')
  assert.equal(
    fragments.runs.steps[1].with.name,
    'ci-proof-fragments-${{ github.job }}-${{ inputs.unit }}-${{ steps.fragments.outputs.part }}-attempt-${{ github.run_attempt }}'
  )
})

test('frontend shards publish both units from the same report after the test step', () => {
  const ci = ciWithLintSteps()
  const producers = producersOf(ci, 'frontend/tests')
  assert.deepEqual(
    producers.map(({ step }) => [step.if, step.with]),
    ['case', 'file'].map((unit) => [
      "${{ !cancelled() && steps.frontend-tests.outcome != 'skipped' }}",
      {
        format: 'vitest',
        report: '${{ runner.temp }}/vitest-report.json',
        entry: 'frontend/tests',
        unit,
      },
    ])
  )
})
