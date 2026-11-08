import assert from 'node:assert/strict'
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

const producerOf = (ci, entry) =>
  Object.values(ci.jobs).find(
    (job) => stepUsing(job, 'ci_proof_fragments')?.with.entry === entry
  )

test('CI proof expects each entry in as many parts as its producer matrix runs', () => {
  const ci = workflow('ci')
  const { expect } = stepUsing(ci.jobs['ci-proof'], 'ci_proof_summary').with
  const expectations = [...expect.matchAll(/(\S+)=(\d+)/g)]

  assert.deepEqual(
    expectations.map(([, entry]) => entry),
    ['e2e_test/features', 'frontend/tests', 'backend/src/test/java']
  )
  for (const [, entry, parts] of expectations) {
    const producer = producerOf(ci, entry)
    assert.equal(
      Object.values(producer.strategy.matrix)[0].length,
      Number(parts),
      entry
    )
    assert.equal(producer.strategy['fail-fast'], false, entry)
  }
})
