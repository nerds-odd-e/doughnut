import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import { parse } from 'yaml'

const workflow = (name) =>
  parse(
    readFileSync(
      new URL(`../../.github/workflows/${name}.yml`, import.meta.url),
      'utf8'
    )
  )

test('main CI remains enabled while application publication is paused', () => {
  const ci = workflow('ci')
  const deploy = workflow('deploy')

  assert.deepEqual(ci.on.push.branches, ['main'])
  assert.equal(ci.name, 'donut CI')
  assert.equal(deploy.jobs['main-head-guard'].if, '${{ false }}')
  assert.equal(deploy.jobs.Deploy.needs, 'main-head-guard')
  assert.equal(
    deploy.jobs.Deploy.if,
    "needs.main-head-guard.outputs.deploy == 'true'"
  )
  assert.equal(deploy.concurrency['cancel-in-progress'], false)
})

test('independent CLI tags retain their release trigger', () => {
  assert.deepEqual(workflow('cli-release').on.push.tags, ['cli-*'])
})
