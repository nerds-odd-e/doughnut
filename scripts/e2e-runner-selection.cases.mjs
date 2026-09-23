import assert from 'node:assert/strict'

import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import { writeIsolatedConfig } from './sut-isolated-fixtures.mjs'
import { runE2eBatch, runE2eInteractive } from './e2e-runner.mjs'
import {
  makeCypressChild,
  makeOpenCypressChild,
} from './e2e-runner-cypress-fixtures.mjs'

test('unsupported spec selection refuses before the stack starts', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  let startCalled = false

  const code = await runE2eBatch({
    argv: ['--spec', 'e2e_test/features/book_reading/epub_book.feature'],
    checkoutRoot: checkout.root,
    startLifetime: async () => {
      startCalled = true
      throw new Error('should not start')
    },
    spawnCypress: () => makeCypressChild(0),
  })

  assert.equal(code, 1)
  assert.equal(startCalled, false, 'must refuse before starting the stack')
})

test('missing explicit --spec refuses before the stack starts', async (t) => {
  const checkout = makePrimaryCheckout(t)
  let startCalled = false

  const code = await runE2eBatch({
    argv: [],
    checkoutRoot: checkout.root,
    startLifetime: async () => {
      startCalled = true
      throw new Error('should not start')
    },
    spawnCypress: () => makeCypressChild(0),
  })

  assert.equal(code, 1)
  assert.equal(startCalled, false)
})

test('interactive session: unsupported preselected spec refuses before the stack starts', async (t) => {
  const checkout = makePrimaryCheckout(t)
  let startCalled = false

  const code = await runE2eInteractive({
    argv: ['--spec', 'e2e_test/features/book_reading/epub_book.feature'],
    checkoutRoot: checkout.root,
    startLifetime: async () => {
      startCalled = true
      throw new Error('should not start')
    },
    spawnCypress: () => makeOpenCypressChild(),
  })

  assert.equal(code, 1)
  assert.equal(startCalled, false, 'must refuse before starting the stack')
})

test('isolated path: non-allowlisted glob still refuses (allowlist enforced for isolated only)', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  let startCalled = false

  const code = await runE2eBatch({
    argv: ['--spec', 'e2e_test/features/foo/**\ne2e_test/features/bar/**'],
    checkoutRoot: checkout.root,
    startLifetime: async () => {
      startCalled = true
      throw new Error('should not start')
    },
    spawnCypress: () => makeCypressChild(0),
  })

  assert.equal(code, 1)
  assert.equal(
    startCalled,
    false,
    'must refuse non-allowlisted glob for isolated path'
  )
})
