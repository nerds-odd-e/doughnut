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
