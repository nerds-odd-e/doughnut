import assert from 'node:assert/strict'
import { existsSync, rmSync, writeFileSync } from 'node:fs'
import { basename, join } from 'node:path'
import { test } from 'node:test'
import { stripVTControlCharacters } from 'node:util'
import {
  reporter,
  runCypress,
  writeRunnerFixture,
} from './cucumberProofRunnerFixture.mjs'
import { proofProject } from './cucumberProofTestProject.mjs'

test('Cypress reports invalid features before running even a focused spec', {
  timeout: 60000,
}, async (t) => {
  const { root, git } = proofProject(t)
  writeRunnerFixture(root)
  const broken = join(root, 'features/broken.feature')
  const empty = join(root, 'features/empty.feature')
  writeFileSync(
    broken,
    `Feature: Broken syntax
  Scenario: Broken
    Given an accepted value "x"
    stray text
`
  )
  writeFileSync(
    empty,
    `Feature: Empty examples
  Scenario: Empty
    Given an accepted value "<value>"
    Examples:
`
  )
  writeFileSync(
    join(root, 'features/ignored.feature'),
    `@selected @ignore
Feature: Filtered by tags
  Scenario: Ignored
    Given an accepted value "x"
`
  )
  const env = { ...process.env, PROOF_MODE: 'enabled' }
  delete env.RECORD_CUCUMBER_PROOF
  const selection = [
    '--expose',
    'filterSpecs=true,tags=@selected and not @ignore',
  ]
  const spec = join(
    git('rev-parse', '--show-toplevel'),
    'features/normal.feature'
  )
  const failed = await runCypress(root, [...selection, '--spec', spec], {
    ...env,
    PROOF_MODE: 'report',
  })
  assert.equal(failed.code, 1, failed.output)
  const diagnostic = stripVTControlCharacters(failed.output)
  assert.match(
    diagnostic,
    /features\/broken\.feature[^\n]*\(4:5\)[^\n]*stray text/
  )
  assert.match(diagnostic, /features\/empty\.feature[^\n]*no scenarios/)
  assert.doesNotMatch(diagnostic, /^\s*Running:/m)
  assert.equal(existsSync(join(root, 'existing.ndjson')), false)

  rmSync(broken)
  rmSync(empty)
  const passed = await runCypress(root, selection, env)
  assert.equal(passed.code, 0, passed.output)
  assert.deepEqual(
    reporter(passed.output).specs.map(([spec]) => basename(spec)),
    ['normal.feature']
  )
  assert.doesNotMatch(passed.output, /ignored\.feature/)
})
