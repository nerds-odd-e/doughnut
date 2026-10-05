import assert from 'node:assert/strict'
import { existsSync } from 'node:fs'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  isPidAlive,
  spawnOwnedTreeStandIn,
  waitForOwnedPids,
  writeIsolatedConfig,
} from './sut-isolated-fixtures.mjs'
import { sutOwnerLockDir, verifyLiveSutOwner } from './sut-owner.mjs'
import { runE2eHold } from './e2e-runner.mjs'
import { manualCancel } from './e2e-runner-cypress-fixtures.mjs'
import {
  isolatedLifetimeOpts,
  trackOwnedTree,
} from './e2e-runner-lifetime-fixtures.mjs'

function holdWith(checkout, standIn, cancel, onReady) {
  return runE2eHold({
    ...isolatedLifetimeOpts(checkout.root, standIn),
    cancel,
    log: (line) => {
      if (line.includes('held')) onReady()
    },
  })
}

test('hold: the owned stack stays up until interrupted, then stops, releases ownership, and exits 0', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)
  const cancel = manualCancel()

  const code = await holdWith(checkout, standIn, cancel, async () => {
    state.owned = await waitForOwnedPids(standIn.pidsFile)
    assert.equal(isPidAlive(state.owned.leader), true)
    assert.equal(isPidAlive(state.owned.grandchild), true)
    cancel.trigger()
  })

  assert.equal(code, 0, 'a clean interrupt must return 0')
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})

test('hold: a required service exiting during the hold ends it nonzero and stops the rest', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const standIn = spawnOwnedTreeStandIn(checkout.root)
  const state = { owned: { leader: 0, grandchild: 0 } }
  trackOwnedTree(t, () => state.owned)

  const code = await holdWith(checkout, standIn, manualCancel(), async () => {
    state.owned = await waitForOwnedPids(standIn.pidsFile)
    process.kill(state.owned.leader, 'SIGKILL')
  })

  assert.equal(code, 1, 'service exit must end the hold with visible failure')
  assert.equal(isPidAlive(state.owned.leader), false)
  assert.equal(isPidAlive(state.owned.grandchild), false)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
})
