import assert from 'node:assert/strict'

import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  spawnOwnedTreeStandIn,
  waitForOwnedPids,
} from './sut-isolated-fixtures.mjs'
import { runE2eBatch } from './e2e-runner.mjs'
import { makeCypressChild } from './e2e-runner-cypress-fixtures.mjs'
import {
  trackOwnedTree,
  primaryLifetimeOpts,
} from './e2e-runner-lifetime-fixtures.mjs'

test('primary path: multi-line glob --spec accepted without throwing, joined --spec contains both globs comma-separated', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  let capturedSpecs = null

  const code = await runE2eBatch({
    argv: ['--spec', 'e2e_test/features/foo/**\ne2e_test/features/bar/**'],
    ...primaryLifetimeOpts(checkout.root, standIn),
    spawnCypress: (opts) => {
      capturedSpecs = opts.specs
      return makeCypressChild(0, async () => {
        state.owned = await waitForOwnedPids(standIn.pidsFile)
      })
    },
  })

  assert.equal(code, 0)
  assert.ok(capturedSpecs, 'specs must be forwarded to Cypress')
  assert.equal(capturedSpecs.length, 2, 'multi-line glob splits into two specs')
  assert.deepEqual(
    capturedSpecs,
    ['e2e_test/features/foo/**', 'e2e_test/features/bar/**'],
    'both globs preserved'
  )
  assert.equal(
    capturedSpecs.join(','),
    'e2e_test/features/foo/**,e2e_test/features/bar/**',
    'joined --spec is comma-separated'
  )
})
