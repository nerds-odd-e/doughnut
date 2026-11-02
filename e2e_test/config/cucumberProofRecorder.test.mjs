import assert from 'node:assert/strict'
import { writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { test } from 'node:test'
import { createCucumberProofRecorder } from './cucumberProofRecorder.mjs'
import { proofProject } from './cucumberProofTestProject.mjs'

const plain =
  'Feature: Observe\n  Scenario: Save a note\n    Given a saved note\n'

test('final observation keeps the run-start commit and compiled source version after later edits', (t) => {
  const project = proofProject(t)
  const commit = project.git('rev-parse', 'HEAD')
  project.recorder.start()
  const [pickle] = project.source(plain)
  const featureBlob = project.git('hash-object', 'features/proof.feature')
  writeFileSync(
    join(project.root, 'features/proof.feature'),
    `${plain}    Then the note changed\n`
  )
  project.git('add', '.')
  project.git(
    '-c',
    'user.name=Proof Fixture',
    '-c',
    'user.email=proof@example.test',
    'commit',
    '--quiet',
    '-m',
    'Later edit'
  )
  project.attempt(pickle)
  assert.deepEqual(project.proof(), [
    {
      scenario: 'Save a note',
      featureBlob,
      feature: 'features/proof.feature',
      outcome: 'passed',
      attempts: 1,
      durationMs: 100,
      finishedAt: '2026-11-02T00:03:10.500Z',
      commit,
    },
  ])
})

test('outline identity uses the Gherkin name and parsed escaped cells under a Rule', (t) => {
  const project = proofProject(t)
  project.recorder.start()
  const pickles = project.source(
    'Feature: Observe\n  Rule: Parse\n    Scenario Outline: Save <text>\n      Given <text>\n      Examples:\n        | text |\n        | a\\|b |\n        | back\\\\slash |\n        | first\\nsecond |\n'
  )
  for (const pickle of pickles) project.attempt(pickle)
  assert.deepEqual(
    project.proof().map(({ scenario, example }) => ({ scenario, example })),
    [
      { scenario: 'Save <text>', example: ['a|b'] },
      { scenario: 'Save <text>', example: ['back\\slash'] },
      { scenario: 'Save <text>', example: ['first\nsecond'] },
    ]
  )
})

test('only the last retry publishes, with all attempt durations and final hook outcome', (t) => {
  const project = proofProject(t)
  project.recorder.start()
  const [pickle] = project.source(plain)
  project.attempt(pickle, { results: ['FAILED'], durations: [80], retry: true })
  assert.equal(project.files().length, 0)
  project.attempt(pickle, {
    index: 1,
    results: ['PASSED', 'FAILED', 'SKIPPED'],
    durations: [10, 20, 0],
  })
  const [observation] = project.proof()
  assert.deepEqual(
    [observation.outcome, observation.attempts, observation.durationMs],
    ['failed', 2, 110]
  )
})

test('later executions of one identity replace the earlier observation in this run', (t) => {
  const project = proofProject(t)
  project.recorder.start()
  const [pickle] = project.source(plain)
  project.attempt(pickle, { results: ['FAILED'] })
  project.attempt(pickle, { seconds: 1793577791 })
  assert.equal(project.proof().length, 1)
  assert.equal(project.proof()[0].outcome, 'passed')
  assert.equal(project.proof()[0].finishedAt, '2026-11-02T00:03:11.500Z')
})

for (const [results, outcome] of [
  [['UNKNOWN', 'PASSED', 'UNDEFINED', 'AMBIGUOUS'], 'ambiguous'],
  [['UNKNOWN', 'PENDING', 'UNDEFINED', 'SKIPPED'], 'undefined'],
  [['SKIPPED', 'PASSED', 'PENDING'], 'pending'],
  [['PASSED', 'SKIPPED'], 'skipped'],
  [['UNKNOWN', 'PASSED'], 'passed'],
  [['UNKNOWN'], 'unknown'],
]) {
  test(`the final attempt's worst result is ${outcome}`, (t) => {
    const project = proofProject(t)
    project.recorder.start()
    const [pickle] = project.source(plain)
    project.attempt(pickle, { results })
    assert.equal(project.proof()[0]?.outcome, outcome)
  })
}

test('only executed scenarios in declaring locations contribute proof', (t) => {
  const project = proofProject(t)
  project.recorder.start()
  let [pickle] = project.source(plain, 'outside/proof.feature')
  project.attempt(pickle)
  ;[pickle] = project.source(plain)
  project.attempt(pickle, { results: ['SKIPPED'] })
  assert.equal(project.files().length, 0)
  project.attempt(pickle)
  assert.equal(project.proof().length, 1)
})

test('proof failures and a missing Git checkout remain silent and harmless', (t) => {
  const project = proofProject(t)
  writeFileSync(join(project.root, 'proof'), 'not a writable directory')
  project.recorder.start()
  const [pickle] = project.source(plain)
  assert.doesNotThrow(() => project.attempt(pickle))
  const unavailable = createCucumberProofRecorder({
    repoRoot: '/not-a-git-checkout',
  })
  assert.equal(unavailable.configured, false)
  assert.doesNotThrow(() => unavailable.start())
})

test('each run keeps its own complete proof file and needs no end-of-run flush', (t) => {
  const project = proofProject(t)
  project.recorder.start()
  const [pickle] = project.source(plain)
  project.attempt(pickle)
  project.recorder.start()
  const [second] = project.source(plain)
  project.attempt(second)
  assert.equal(project.files().length, 2)
  assert.equal(project.proof().length, 2)
})
