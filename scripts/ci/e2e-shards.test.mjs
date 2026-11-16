import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join, relative } from 'node:path'
import { fileURLToPath } from 'node:url'
import { test } from 'node:test'
import { addCucumberPreprocessorPlugin } from '@badeball/cypress-cucumber-preprocessor'
import { cucumberTagExpression } from '../../e2e_test/config/ciTagSelection.mjs'
import {
  discoverE2eSpecs,
  e2eSpecPatterns,
} from '../../e2e_test/config/e2eSpecDiscovery.mjs'
import { resolveSpecs } from '../e2e-invocation-selection.mjs'
import { explicitGroups } from './e2e-shards.mjs'
import { workflow } from './workflow-fixtures.mjs'

const checkout = fileURLToPath(new URL('../../', import.meta.url))
const selector = join(checkout, 'scripts/ci/e2e-shards.mjs')
const slots = workflow('ci').jobs['E2E-tests'].strategy.matrix.shard
const runSelector = (slot, root) =>
  execFileSync(process.execPath, [selector, String(slot), root], {
    encoding: 'utf8',
  })
    .trim()
    .split(',')
const selections = (root) => slots.map((slot) => runSelector(slot, root))

function assertPartition(root, groups) {
  const discovered = discoverE2eSpecs({
    ...e2eSpecPatterns,
    projectRoot: root,
  }).map((path) => relative(root, path))
  assert.deepEqual(groups.flat().sort(), discovered.sort())
  assert.equal(new Set(groups.flat()).size, groups.flat().length)
}

test('workflow slots partition checkout specs and select the learning-session file in the remainder', () => {
  const groups = selections(checkout)
  assertPartition(checkout, groups)
  assert.ok(
    groups
      .at(-1)
      .includes(
        'e2e_test/features/learning_session/commissioned_learning_session.feature'
      )
  )
  for (const group of groups) {
    assert.deepEqual(
      resolveSpecs(['--spec', group.join(',')], checkout, false).specs,
      group
    )
  }
})

function fixture(t) {
  const root = mkdtempSync(join(tmpdir(), 'donut-e2e-shards-'))
  t.after(() => rmSync(root, { recursive: true, force: true }))
  const write = (
    name,
    contents = 'Feature: Example\n  Scenario: Example\n    Given a value\n'
  ) => {
    const path = join(root, name)
    mkdirSync(dirname(path), { recursive: true })
    writeFileSync(path, contents)
    return path
  }
  return { root, write }
}

test('new directories join the remainder while explicit groups and exclusions retain ownership', (t) => {
  const { root, write } = fixture(t)
  const explicit = [
    'assimilation',
    'bazaar',
    'note_creation_and_update',
    'note_topology',
    'recall',
  ].map((folder) => `e2e_test/features/${folder}/example.feature`)
  explicit.forEach((path) => write(path))
  const remainder = 'e2e_test/features/new_capability/example.feature'
  write(remainder)
  write('e2e_test/features/new_capability/__snapshots__/example.feature')
  write('e2e_test/features/new_capability/__image_snapshots__/example.feature')
  write('e2e_test/features/new_capability/example.ts')
  write('e2e_test/features/node_modules/nested/example.feature')
  const groups = selections(root)
  assertPartition(root, groups)
  assert.deepEqual(groups, [...explicit.map((path) => [path]), [remainder]])
  const added = 'e2e_test/features/another_new_directory/added.feature'
  write(added)
  const expanded = selections(root)
  assertPartition(root, expanded)
  assert.ok(expanded.at(-1).includes(added))
})

test('installed Cucumber filtering keeps eligible files and the existing CI tag treatment', async (t) => {
  const { root, write } = fixture(t)
  write(
    'package.json',
    JSON.stringify({
      'cypress-cucumber-preprocessor': {
        filterSpecs: true,
        omitFiltered: true,
      },
    })
  )
  const excludedTags = [
    '@ignore',
    '@wip',
    '@publicationProfile',
    '@publicationProfileRejection',
    '@publicationProfileHttp',
    '@publicationProfileHttpRejection',
    '@publicationProfileHttpUpdate',
  ]
  const eligible = write(
    'e2e_test/features/new_capability/eligible.feature',
    '@usingMockedOpenAiService\nFeature: Eligible\n  Scenario: Eligible\n    Given a value\n'
  )
  const mixed = write(
    'e2e_test/features/new_capability/mixed.feature',
    'Feature: Mixed\n  @ignore\n  Scenario: Excluded\n    Given a value\n  Scenario: Eligible\n    Given a value\n'
  )
  for (const tag of excludedTags) {
    write(
      `e2e_test/features/new_capability/${tag.slice(1)}.feature`,
      `${tag}\nFeature: Filtered\n  Scenario: Filtered\n    Given a value\n`
    )
  }
  const selected = runSelector(slots.at(-1), root)
  const config = {
    ...e2eSpecPatterns,
    projectRoot: root,
    testingType: 'e2e',
    reporter: 'spec',
    isTextTerminal: true,
    env: {},
    expose: { tags: cucumberTagExpression(true) },
    specPattern: selected.map((path) => join(root, path)),
  }
  await addCucumberPreprocessorPlugin(() => undefined, config)
  assert.deepEqual(config.specPattern.sort(), [eligible, mixed].sort())
  // The same configured local expression permits work in progress.
  config.specPattern = selected.map((path) => join(root, path))
  config.expose.tags = cucumberTagExpression(false)
  await addCucumberPreprocessorPlugin(() => undefined, config)
  assert.deepEqual(
    config.specPattern.sort(),
    [
      eligible,
      mixed,
      join(root, 'e2e_test/features/new_capability/wip.feature'),
    ].sort()
  )
})

test('workflow runs every computed selection through the owned wrapper with matching proof parts', () => {
  const ci = workflow('ci')
  const job = ci.jobs['E2E-tests']
  assert.equal(slots.length, explicitGroups.length + 1)
  assert.deepEqual(
    slots,
    slots.map((_, index) => index + 1)
  )
  assert.equal(job.strategy['fail-fast'], false)
  const runner = job.steps.find((step) => step.id === 'e2e-tests')
  assert.equal(
    runner.run,
    'specs="$(node scripts/ci/e2e-shards.mjs \'${{ matrix.shard }}\')"\n' +
      'SUT_RUNTIME_TARGET=\'{"built":true}\' node scripts/e2e-runner.mjs --spec "$specs" --browser chrome\n'
  )
  const fragments = job.steps.find(
    (step) => step.uses === './.github/ci_proof_fragments'
  )
  assert.equal(fragments.with.entry, 'e2e_test/features')
  assert.equal(fragments.with.unit, 'case')
  const expected = ci.jobs['ci-proof'].steps.find(
    (step) => step.uses === './.github/ci_proof_summary'
  )
  assert.ok(
    expected.with.expect.includes(`e2e_test/features=${slots.length}:case`)
  )
})
